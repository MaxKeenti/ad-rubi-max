import { initializeApp } from "firebase-admin/app";
import { getAuth, UserRecord } from "firebase-admin/auth";
import { FieldValue, Timestamp, getFirestore } from "firebase-admin/firestore";
import { logger } from "firebase-functions";
import { CallableRequest, HttpsError, onCall } from "firebase-functions/v2/https";

initializeApp();

const USERS = "users";
const ADMIN_ROLE = "admin" as const;
const OPERATOR_ROLE = "operator" as const;

const auth = getAuth();
const db = getFirestore();

type Payload = Record<string, unknown>;

type OperatorResponse = {
  id: string;
  email: string;
  displayName: string;
  role: "operator";
  accountCreatedAtMillis: number | null;
  disabledAtMillis: number | null;
  retiredAtMillis: number | null;
  promotedToUid: string | null;
};

export const createOperatorAccount = onCall(async (request) => {
  await requireAdmin(request);
  const payload = payloadFrom(request);
  const email = readEmail(payload, "email");
  const displayName = readTrimmedString(payload, "displayName", "Ingresa el nombre.");
  const password = readPassword(payload, "password");

  return createAccount({
    email,
    displayName,
    password,
    role: OPERATOR_ROLE,
  });
});

export const createAdminAccount = onCall(async (request) => {
  const actingAdmin = await requireAdmin(request);
  const payload = payloadFrom(request);
  const email = readEmail(payload, "email");
  const displayName = readTrimmedString(payload, "displayName", "Ingresa el nombre.");
  const password = readPassword(payload, "password");
  const adminPassword = readPassword(payload, "adminPasswordConfirmation");

  await verifyAdminPassword(actingAdmin, adminPassword);

  return createAccount({
    email,
    displayName,
    password,
    role: ADMIN_ROLE,
  });
});

export const listOperators = onCall(async (request) => {
  await requireAdmin(request);

  const snap = await db
    .collection(USERS)
    .where("role", "==", OPERATOR_ROLE)
    .get();

  const operators: OperatorResponse[] = snap.docs
    .map((doc) => {
      const data = doc.data();
      return {
        id: doc.id,
        email: stringOrEmpty(data.email),
        displayName: stringOrEmpty(data.displayName),
        role: OPERATOR_ROLE,
        accountCreatedAtMillis: timestampMillis(data.accountCreatedAt),
        disabledAtMillis: timestampMillis(data.disabledAt),
        retiredAtMillis: timestampMillis(data.retiredAt),
        promotedToUid: stringOrNull(data.promotedToUid),
      };
    })
    .filter((user) => user.disabledAtMillis == null && user.retiredAtMillis == null)
    .sort((a, b) => {
      const nameCompare = a.displayName.localeCompare(b.displayName, "es");
      return nameCompare !== 0 ? nameCompare : a.email.localeCompare(b.email, "es");
    });

  return { operators };
});

export const promoteOperatorToAdmin = onCall(async (request) => {
  const actingAdmin = await requireAdmin(request);
  const payload = payloadFrom(request);
  const operatorUserId = readTrimmedString(
    payload,
    "operatorUserId",
    "Selecciona un operador.",
  );
  const operatorPassword = readPassword(payload, "operatorPasswordConfirmation");
  const adminPassword = readPassword(payload, "adminPasswordConfirmation");

  const operatorRef = db.collection(USERS).doc(operatorUserId);
  const operatorSnap = await operatorRef.get();
  if (!operatorSnap.exists) {
    throw new HttpsError("not-found", "No se encontro el operador.");
  }

  const operatorData = operatorSnap.data() ?? {};
  if (operatorData.role !== OPERATOR_ROLE) {
    throw new HttpsError("failed-precondition", "El usuario seleccionado no es operador.");
  }
  if (operatorData.disabledAt != null || operatorData.retiredAt != null) {
    throw new HttpsError("failed-precondition", "El operador ya esta retirado.");
  }

  const operatorRecord = await auth.getUser(operatorUserId);
  const originalEmail = operatorRecord.email ?? stringOrEmpty(operatorData.email);
  if (originalEmail.length === 0) {
    throw new HttpsError("failed-precondition", "El operador no tiene correo registrado.");
  }

  await verifyEmailPassword(originalEmail, operatorPassword, "La contrasena del operador no coincide.");
  await verifyAdminPassword(actingAdmin, adminPassword);

  const displayName = stringOrEmpty(operatorData.displayName) ||
    operatorRecord.displayName ||
    originalEmail;
  const retiredEmail = retiredEmailFor(operatorUserId);
  let newAdmin: UserRecord | null = null;

  try {
    await auth.updateUser(operatorUserId, {
      disabled: true,
      email: retiredEmail,
      emailVerified: false,
    });

    newAdmin = await auth.createUser({
      email: originalEmail,
      password: operatorPassword,
      displayName,
      emailVerified: operatorRecord.emailVerified,
      disabled: false,
    });

    const batch = db.batch();
    batch.update(operatorRef, {
      disabledAt: FieldValue.serverTimestamp(),
      retiredAt: FieldValue.serverTimestamp(),
      promotedToUid: newAdmin.uid,
      authEmailRetiredTo: retiredEmail,
    });
    batch.create(db.collection(USERS).doc(newAdmin.uid), {
      email: originalEmail,
      displayName,
      role: ADMIN_ROLE,
      accountCreatedAt: FieldValue.serverTimestamp(),
      disabledAt: null,
      retiredAt: null,
      promotedFromUid: operatorUserId,
    });
    await batch.commit();

    return { uid: newAdmin.uid };
  } catch (error) {
    await compensatePromotionFailure({
      operatorUserId,
      originalEmail,
      newAdminUid: newAdmin?.uid ?? null,
    });
    throw toHttpsError(error, "No se pudo promover el operador.");
  }
});

async function createAccount(input: {
  email: string;
  displayName: string;
  password: string;
  role: "admin" | "operator";
}) {
  try {
    await assertEmailAvailable(input.email);
    const user = await auth.createUser({
      email: input.email,
      password: input.password,
      displayName: input.displayName,
      disabled: false,
    });

    try {
      await db.collection(USERS).doc(user.uid).create({
        email: input.email,
        displayName: input.displayName,
        role: input.role,
        accountCreatedAt: FieldValue.serverTimestamp(),
        disabledAt: null,
        retiredAt: null,
      });
    } catch (error) {
      await auth.deleteUser(user.uid).catch((rollbackError) => {
        logger.error("Failed to roll back auth user after Firestore create failure", {
          uid: user.uid,
          rollbackError,
        });
      });
      throw error;
    }

    return { uid: user.uid };
  } catch (error) {
    throw toHttpsError(error, "No se pudo crear la cuenta.");
  }
}

async function requireAdmin(request: CallableRequest<unknown>): Promise<UserRecord> {
  const uid = request.auth?.uid;
  if (uid == null) {
    throw new HttpsError("unauthenticated", "Inicia sesion para continuar.");
  }

  const userSnap = await db.collection(USERS).doc(uid).get();
  const data = userSnap.data();
  if (!userSnap.exists || data?.role !== ADMIN_ROLE || data.disabledAt != null || data.retiredAt != null) {
    throw new HttpsError("permission-denied", "Solo administradores pueden administrar usuarios.");
  }

  return auth.getUser(uid);
}

async function assertEmailAvailable(email: string) {
  try {
    await auth.getUserByEmail(email);
    throw new HttpsError("already-exists", "Ya existe una cuenta con ese correo.");
  } catch (error) {
    if (authErrorCode(error) === "auth/user-not-found") {
      return;
    }
    throw error;
  }
}

async function verifyAdminPassword(admin: UserRecord, password: string) {
  if (admin.email == null) {
    throw new HttpsError("failed-precondition", "El administrador no tiene correo registrado.");
  }
  await verifyEmailPassword(admin.email, password, "Las credenciales del administrador no coinciden.");
}

async function verifyEmailPassword(email: string, password: string, message: string) {
  const apiKey = process.env.FIREBASE_WEB_API_KEY;
  if (apiKey == null || apiKey.trim().length === 0) {
    throw new HttpsError(
      "failed-precondition",
      "Falta configurar FIREBASE_WEB_API_KEY en Functions.",
    );
  }

  const response = await fetch(
    `https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=${apiKey}`,
    {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        email,
        password,
        returnSecureToken: true,
      }),
    },
  );

  if (response.ok) {
    return;
  }

  const errorPayload = await response.json().catch(() => null) as {
    error?: { message?: string };
  } | null;
  const code = errorPayload?.error?.message;
  if (
    code === "EMAIL_NOT_FOUND" ||
    code === "INVALID_PASSWORD" ||
    code === "INVALID_LOGIN_CREDENTIALS" ||
    code === "USER_DISABLED"
  ) {
    throw new HttpsError("permission-denied", message);
  }

  logger.error("Identity Toolkit password verification failed", { code });
  throw new HttpsError("internal", "No se pudo verificar la contrasena.");
}

async function compensatePromotionFailure(input: {
  operatorUserId: string;
  originalEmail: string;
  newAdminUid: string | null;
}) {
  if (input.newAdminUid != null) {
    await auth.deleteUser(input.newAdminUid).catch((error) => {
      logger.error("Failed to delete partially created promoted admin", {
        uid: input.newAdminUid,
        error,
      });
    });
  }

  await auth.updateUser(input.operatorUserId, {
    email: input.originalEmail,
    disabled: false,
  }).catch((error) => {
    logger.error("Failed to restore operator auth account after promotion failure", {
      uid: input.operatorUserId,
      error,
    });
  });
}

function payloadFrom(request: CallableRequest<unknown>): Payload {
  if (typeof request.data !== "object" || request.data == null || Array.isArray(request.data)) {
    throw new HttpsError("invalid-argument", "Solicitud invalida.");
  }
  return request.data as Payload;
}

function readEmail(payload: Payload, key: string): string {
  const value = readTrimmedString(payload, key, "Ingresa un correo valido.").toLowerCase();
  if (!/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(value)) {
    throw new HttpsError("invalid-argument", "Ingresa un correo valido.");
  }
  return value;
}

function readTrimmedString(payload: Payload, key: string, message: string): string {
  const value = payload[key];
  if (typeof value !== "string" || value.trim().length === 0) {
    throw new HttpsError("invalid-argument", message);
  }
  return value.trim();
}

function readPassword(payload: Payload, key: string): string {
  const value = payload[key];
  if (typeof value !== "string" || value.length < 6) {
    throw new HttpsError("invalid-argument", "La contrasena debe tener al menos 6 caracteres.");
  }
  return value;
}

function stringOrEmpty(value: unknown): string {
  return typeof value === "string" ? value : "";
}

function stringOrNull(value: unknown): string | null {
  return typeof value === "string" && value.length > 0 ? value : null;
}

function timestampMillis(value: unknown): number | null {
  return value instanceof Timestamp ? value.toMillis() : null;
}

function retiredEmailFor(uid: string): string {
  return `retired-${uid}-${Date.now()}@mangos.invalid`.toLowerCase();
}

function authErrorCode(error: unknown): string | null {
  if (typeof error !== "object" || error == null || !("code" in error)) {
    return null;
  }
  const code = (error as { code?: unknown }).code;
  return typeof code === "string" ? code : null;
}

function toHttpsError(error: unknown, fallbackMessage: string): HttpsError {
  if (error instanceof HttpsError) {
    return error;
  }

  const code = authErrorCode(error);
  if (code === "auth/email-already-exists") {
    return new HttpsError("already-exists", "Ya existe una cuenta con ese correo.");
  }
  if (code === "auth/invalid-password") {
    return new HttpsError("invalid-argument", "La contrasena debe tener al menos 6 caracteres.");
  }
  if (code === "auth/invalid-email") {
    return new HttpsError("invalid-argument", "Ingresa un correo valido.");
  }

  logger.error(fallbackMessage, error);
  return new HttpsError("internal", fallbackMessage);
}
