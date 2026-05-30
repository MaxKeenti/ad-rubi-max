import {
  initializeTestEnvironment,
  assertSucceeds,
  assertFails,
} from "@firebase/rules-unit-testing";
import fs from "node:fs";
import { after, beforeEach, describe, test } from "node:test";
import {
  deleteDoc,
  doc,
  getDoc,
  serverTimestamp,
  setDoc,
  Timestamp,
  updateDoc,
} from "firebase/firestore";

const env = await initializeTestEnvironment({
  projectId: "mangos-test",
  firestore: {
    rules: fs.readFileSync(new URL("../../firestore.rules", import.meta.url), "utf8"),
  },
});

after(async () => {
  await env.cleanup();
});

beforeEach(async () => {
  await env.clearFirestore();
});

async function seed(path, data) {
  await env.withSecurityRulesDisabled(async (context) => {
    await setDoc(doc(context.firestore(), path), data);
  });
}

async function seedUser(uid, role, extra = {}) {
  await seed(`users/${uid}`, {
    displayName: uid,
    role,
    ...extra,
  });
}

async function db(uid, role) {
  await seedUser(uid, role);
  return env.authenticatedContext(uid).firestore();
}

function purchaseData(overrides = {}) {
  return {
    supplierId: "supplier-1",
    supplierNoteFreeform: null,
    quantityTons: 2,
    pricePerTonCentavos: 1250000,
    date: "2026-05-26",
    dateKey: "2026-05-26",
    createdBy: "op1",
    enteredAt: Timestamp.fromMillis(Date.now()),
    serverWrittenAt: Timestamp.fromMillis(Date.now()),
    deletedAt: null,
    deletedBy: null,
    ...overrides,
  };
}

describe("users", () => {
  test("operator cannot promote self to admin", async () => {
    const operatorDb = await db("op1", "operator");

    await assertFails(
      updateDoc(doc(operatorDb, "users/op1"), {
        role: "admin",
      }),
    );
  });

  test("operator can update own displayName", async () => {
    const operatorDb = await db("op1", "operator");

    await assertSucceeds(
      updateDoc(doc(operatorDb, "users/op1"), {
        displayName: "Operador Uno",
      }),
    );
  });
});

describe("suppliers", () => {
  test("operator cannot write suppliers", async () => {
    const operatorDb = await db("op1", "operator");

    await assertFails(
      setDoc(doc(operatorDb, "suppliers/supplier-1"), {
        name: "Mangos del Sur",
        isActive: true,
      }),
    );
  });

  test("operator cannot edit suppliers", async () => {
    const operatorDb = await db("op1", "operator");
    await seed("suppliers/supplier-1", {
      name: "Mangos del Sur",
      isActive: true,
    });

    await assertFails(
      updateDoc(doc(operatorDb, "suppliers/supplier-1"), {
        isActive: false,
      }),
    );
  });

  test("admin can write suppliers", async () => {
    const adminDb = await db("admin1", "admin");

    await assertSucceeds(
      setDoc(doc(adminDb, "suppliers/supplier-1"), {
        name: "Mangos del Sur",
        isActive: true,
      }),
    );
  });

  test("admin can edit suppliers", async () => {
    const adminDb = await db("admin1", "admin");
    await seed("suppliers/supplier-1", {
      name: "Mangos del Sur",
      isActive: true,
    });

    await assertSucceeds(
      updateDoc(doc(adminDb, "suppliers/supplier-1"), {
        isActive: false,
      }),
    );
  });
});

describe("purchases", () => {
  test("operator can create own purchase", async () => {
    const operatorDb = await db("op1", "operator");

    await assertSucceeds(
      setDoc(doc(operatorDb, "purchases/purchase-1"), {
        ...purchaseData(),
        serverWrittenAt: serverTimestamp(),
      }),
    );
  });

  test("operator cannot create purchase with createdBy = otherUid", async () => {
    const operatorDb = await db("op1", "operator");

    await assertFails(
      setDoc(doc(operatorDb, "purchases/purchase-1"), {
        ...purchaseData({ createdBy: "op2" }),
        serverWrittenAt: serverTimestamp(),
      }),
    );
  });

  test("operator can edit own purchase within 24h", async () => {
    const operatorDb = await db("op1", "operator");
    await seed(
      "purchases/purchase-1",
      purchaseData({
        serverWrittenAt: Timestamp.fromMillis(Date.now() - 23 * 3600 * 1000),
      }),
    );

    await assertSucceeds(
      updateDoc(doc(operatorDb, "purchases/purchase-1"), {
        quantityTons: 3,
      }),
    );
  });

  test("operator cannot edit own purchase after 24h", async () => {
    const operatorDb = await db("op1", "operator");
    await seed(
      "purchases/purchase-1",
      purchaseData({
        serverWrittenAt: Timestamp.fromMillis(Date.now() - 25 * 3600 * 1000),
      }),
    );

    await assertFails(
      updateDoc(doc(operatorDb, "purchases/purchase-1"), {
        quantityTons: 3,
      }),
    );
  });

  test("operator cannot rewrite createdBy on own purchase within 24h", async () => {
    const operatorDb = await db("op1", "operator");
    await seed(
      "purchases/purchase-1",
      purchaseData({
        serverWrittenAt: Timestamp.fromMillis(Date.now() - 23 * 3600 * 1000),
      }),
    );

    await assertFails(
      updateDoc(doc(operatorDb, "purchases/purchase-1"), {
        createdBy: "op2",
      }),
    );
  });

  test("operator cannot rewrite enteredAt on own purchase within 24h", async () => {
    const operatorDb = await db("op1", "operator");
    await seed(
      "purchases/purchase-1",
      purchaseData({
        serverWrittenAt: Timestamp.fromMillis(Date.now() - 23 * 3600 * 1000),
      }),
    );

    await assertFails(
      updateDoc(doc(operatorDb, "purchases/purchase-1"), {
        enteredAt: Timestamp.fromMillis(Date.now()),
      }),
    );
  });

  test("admin can edit any purchase anytime", async () => {
    const adminDb = await db("admin1", "admin");
    await seed(
      "purchases/purchase-1",
      purchaseData({
        createdBy: "op1",
        serverWrittenAt: Timestamp.fromMillis(Date.now() - 25 * 3600 * 1000),
      }),
    );

    await assertSucceeds(
      updateDoc(doc(adminDb, "purchases/purchase-1"), {
        quantityTons: 4,
      }),
    );
  });

  test("signed in users can read purchases", async () => {
    const operatorDb = await db("op1", "operator");
    await seed("purchases/purchase-1", purchaseData());

    await assertSucceeds(getDoc(doc(operatorDb, "purchases/purchase-1")));
  });

  test("anonymous users cannot read purchases", async () => {
    const anonymousDb = env.unauthenticatedContext().firestore();
    await seed("purchases/purchase-1", purchaseData());

    await assertFails(getDoc(doc(anonymousDb, "purchases/purchase-1")));
  });

  test("operator can soft delete own purchase within 24h", async () => {
    const operatorDb = await db("op1", "operator");
    await seed(
      "purchases/purchase-1",
      purchaseData({
        serverWrittenAt: Timestamp.fromMillis(Date.now() - 23 * 3600 * 1000),
      }),
    );

    await assertSucceeds(
      updateDoc(doc(operatorDb, "purchases/purchase-1"), {
        deletedAt: serverTimestamp(),
        deletedBy: "op1",
      }),
    );
  });

  test("operator cannot hard delete purchase after 24h", async () => {
    const operatorDb = await db("op1", "operator");
    await seed(
      "purchases/purchase-1",
      purchaseData({
        serverWrittenAt: Timestamp.fromMillis(Date.now() - 25 * 3600 * 1000),
      }),
    );

    await assertFails(deleteDoc(doc(operatorDb, "purchases/purchase-1")));
  });
});
