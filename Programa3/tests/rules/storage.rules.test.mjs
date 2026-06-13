/*
 * Traceability to implementation_plan.md §3 / task 12:
 * - Storage reportes/{id}.jpg reads require auth.
 * - Storage writes require auth, image/jpeg contentType, and size < 1.5 MB.
 * - Storage deletes are denied.
 */
import {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment,
} from "@firebase/rules-unit-testing";
import fs from "node:fs";
import { after, beforeEach, describe, test } from "node:test";

const PROJECT_ID = "bachewatch-storage-rules-test";
const MAX_BYTES = 1572864;

const env = await initializeTestEnvironment({
  projectId: PROJECT_ID,
  storage: {
    rules: fs.readFileSync(new URL("../../storage.rules", import.meta.url), "utf8"),
  },
});

after(async () => {
  await env.cleanup();
});

beforeEach(async () => {
  await env.clearStorage();
});

function storageRef(uid, path) {
  return env.authenticatedContext(uid).storage().ref(path);
}

function unauthStorageRef(path) {
  return env.unauthenticatedContext().storage().ref(path);
}

function bytes(size) {
  return new Uint8Array(size).fill(7);
}

describe("storage reporte photos", () => {
  test("small authenticated JPEG write succeeds", async () => {
    await assertSucceeds(
      storageRef("alice", "reportes/reporte-1.jpg").put(bytes(128), {
        contentType: "image/jpeg",
      }),
    );
  });

  test("non-JPEG contentType is denied", async () => {
    await assertFails(
      storageRef("alice", "reportes/reporte-1.jpg").put(bytes(128), {
        contentType: "image/png",
      }),
    );
  });

  test("files over 1.5 MB are denied", async () => {
    await assertFails(
      storageRef("alice", "reportes/reporte-1.jpg").put(bytes(MAX_BYTES + 1), {
        contentType: "image/jpeg",
      }),
    );
  });

  test("unauthenticated reads and deletes are denied", async () => {
    const foto = storageRef("alice", "reportes/reporte-1.jpg");
    await assertSucceeds(foto.put(bytes(128), { contentType: "image/jpeg" }));

    await assertSucceeds(foto.getDownloadURL());
    await assertFails(unauthStorageRef("reportes/reporte-1.jpg").getDownloadURL());
    await assertFails(foto.delete());
  });
});
