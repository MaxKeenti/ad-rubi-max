/*
 * Traceability to implementation_plan.md §3 / task 12:
 * - reportes create: signed-in caller, createdBy == uid, confirmCount == 0,
 *   server timestamp, geo/photo fields, severidad enum, descripcion <= 200.
 * - reportes update: exactly confirmCount +1, or creator soft-delete within 24 h.
 * - reportes delete: denied; unauthenticated reads: denied.
 * - confirmaciones/{uid}: uid must equal auth.uid, confirmedAt is server time,
 *   update/delete denied.
 */
import {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment,
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

const HOUR_MS = 60 * 60 * 1000;
const PROJECT_ID = "bachewatch-firestore-rules-test";

const env = await initializeTestEnvironment({
  projectId: PROJECT_ID,
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

function db(uid) {
  return env.authenticatedContext(uid).firestore();
}

function validCreate(overrides = {}) {
  return {
    lat: 19.3962,
    lng: -99.0907,
    geohash: "9g3w81",
    accuracyMeters: 9.5,
    severidad: "severo",
    descripcion: "Hundimiento grande frente a UPIICSA",
    fotoPath: "reportes/reporte-1.jpg",
    fotoUrl: "https://example.test/reporte-1.jpg",
    createdBy: "alice",
    confirmCount: 0,
    serverWrittenAt: serverTimestamp(),
    ...overrides,
  };
}

function validSeed(overrides = {}) {
  return {
    lat: 19.3962,
    lng: -99.0907,
    geohash: "9g3w81",
    accuracyMeters: 9.5,
    severidad: "moderado",
    descripcion: "Bache junto al tope",
    fotoPath: "reportes/reporte-1.jpg",
    fotoUrl: "https://example.test/reporte-1.jpg",
    createdBy: "alice",
    confirmCount: 3,
    serverWrittenAt: Timestamp.fromMillis(Date.now() - HOUR_MS),
    ...overrides,
  };
}

async function seed(path, data) {
  await env.withSecurityRulesDisabled(async (context) => {
    await setDoc(doc(context.firestore(), path), data);
  });
}

describe("reportes create shape", () => {
  test("caller can create own valid reporte", async () => {
    await assertSucceeds(
      setDoc(doc(db("alice"), "reportes/reporte-1"), validCreate()),
    );
  });

  test("createdBy must match auth uid", async () => {
    await assertFails(
      setDoc(
        doc(db("alice"), "reportes/reporte-1"),
        validCreate({ createdBy: "bob" }),
      ),
    );
  });

  test("shape violations are denied", async () => {
    const missingGeo = validCreate();
    delete missingGeo.lat;

    await assertFails(
      setDoc(
        doc(db("alice"), "reportes/bad-confirm-count"),
        validCreate({ confirmCount: 1 }),
      ),
    );
    await assertFails(
      setDoc(
        doc(db("alice"), "reportes/bad-severidad"),
        validCreate({ severidad: "profundo" }),
      ),
    );
    await assertFails(
      setDoc(
        doc(db("alice"), "reportes/long-descripcion"),
        validCreate({ descripcion: "x".repeat(201) }),
      ),
    );
    await assertFails(
      setDoc(doc(db("alice"), "reportes/missing-geo"), missingGeo),
    );
    await assertFails(
      setDoc(
        doc(db("alice"), "reportes/soft-delete-on-create"),
        validCreate({ deletedAt: null }),
      ),
    );
  });
});

describe("confirmaciones", () => {
  test("uid-keyed confirmacion create succeeds only for that uid", async () => {
    await seed("reportes/reporte-1", validSeed());

    await assertSucceeds(
      setDoc(
        doc(db("alice"), "reportes/reporte-1/confirmaciones/alice"),
        { confirmedAt: serverTimestamp() },
      ),
    );
    await assertFails(
      setDoc(
        doc(db("alice"), "reportes/reporte-1/confirmaciones/bob"),
        { confirmedAt: serverTimestamp() },
      ),
    );
  });

  test("confirmacion update and delete are denied", async () => {
    await seed("reportes/reporte-1", validSeed());
    await seed("reportes/reporte-1/confirmaciones/alice", {
      confirmedAt: Timestamp.fromMillis(Date.now() - HOUR_MS),
    });

    await assertFails(
      updateDoc(
        doc(db("alice"), "reportes/reporte-1/confirmaciones/alice"),
        { confirmedAt: serverTimestamp() },
      ),
    );
    await assertFails(
      deleteDoc(doc(db("alice"), "reportes/reporte-1/confirmaciones/alice")),
    );
  });
});

describe("confirm count updates", () => {
  test("increment by one is allowed; increment by two is denied", async () => {
    await seed("reportes/reporte-1", validSeed({ confirmCount: 3 }));
    await seed("reportes/reporte-2", validSeed({ confirmCount: 3 }));

    await assertSucceeds(
      updateDoc(doc(db("bob"), "reportes/reporte-1"), { confirmCount: 4 }),
    );
    await assertFails(
      updateDoc(doc(db("bob"), "reportes/reporte-2"), { confirmCount: 5 }),
    );
  });
});

describe("soft delete", () => {
  test("creator can soft-delete within 24 hours", async () => {
    await seed(
      "reportes/reporte-1",
      validSeed({ serverWrittenAt: Timestamp.fromMillis(Date.now() - HOUR_MS) }),
    );

    await assertSucceeds(
      updateDoc(doc(db("alice"), "reportes/reporte-1"), {
        deletedAt: serverTimestamp(),
        deletedBy: "alice",
      }),
    );
  });

  test("non-creator cannot soft-delete", async () => {
    await seed("reportes/reporte-1", validSeed());

    await assertFails(
      updateDoc(doc(db("bob"), "reportes/reporte-1"), {
        deletedAt: serverTimestamp(),
        deletedBy: "bob",
      }),
    );
  });

  test("creator cannot soft-delete after 25 hours", async () => {
    await seed(
      "reportes/reporte-1",
      validSeed({ serverWrittenAt: Timestamp.fromMillis(Date.now() - 25 * HOUR_MS) }),
    );

    await assertFails(
      updateDoc(doc(db("alice"), "reportes/reporte-1"), {
        deletedAt: serverTimestamp(),
        deletedBy: "alice",
      }),
    );
  });

  test("soft-delete cannot touch extra fields", async () => {
    await seed("reportes/reporte-1", validSeed());

    await assertFails(
      updateDoc(doc(db("alice"), "reportes/reporte-1"), {
        deletedAt: serverTimestamp(),
        deletedBy: "alice",
        descripcion: "texto cambiado",
      }),
    );
  });
});

describe("reads and deletes", () => {
  test("signed-in read succeeds and unauthenticated read is denied", async () => {
    await seed("reportes/reporte-1", validSeed());

    await assertSucceeds(getDoc(doc(db("alice"), "reportes/reporte-1")));
    await assertFails(
      getDoc(doc(env.unauthenticatedContext().firestore(), "reportes/reporte-1")),
    );
  });

  test("client hard-delete is denied", async () => {
    await seed("reportes/reporte-1", validSeed());

    await assertFails(deleteDoc(doc(db("alice"), "reportes/reporte-1")));
  });
});
