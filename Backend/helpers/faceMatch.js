/**
 * ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
 *  Face Match — Worker Thread Dispatcher
 *  Sends face matching work to a background thread
 *  so the main event loop stays unblocked
 * ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
 */

const { Worker } = require("worker_threads");
const path = require("path");

const WORKER_PATH = path.join(__dirname, "faceWorker.js");
const TIMEOUT_MS = 5000; // 5 second timeout for face matching

/**
 * Run face matching in a worker thread.
 * Returns a Promise that resolves to the best cosine similarity score.
 *
 * @param {number[]} candidate - The incoming face embedding
 * @param {number[][]} storedEmbeddings - Array of stored face embeddings
 * @returns {Promise<number>} - Best cosine similarity score
 */
function matchFaceAsync(candidate, storedEmbeddings) {
  return new Promise((resolve, reject) => {
    const worker = new Worker(WORKER_PATH, {
      workerData: { candidate, storedEmbeddings },
    });

    const timer = setTimeout(() => {
      worker.terminate();
      reject(new Error("Face matching worker timed out"));
    }, TIMEOUT_MS);

    worker.on("message", (score) => {
      clearTimeout(timer);
      resolve(score);
    });

    worker.on("error", (err) => {
      clearTimeout(timer);
      reject(err);
    });

    worker.on("exit", (code) => {
      clearTimeout(timer);
      if (code !== 0) {
        reject(new Error(`Face worker exited with code ${code}`));
      }
    });
  });
}

module.exports = { matchFaceAsync };
