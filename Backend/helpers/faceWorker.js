/**
 * ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
 *  Face Matching Worker Thread
 *  Runs cosine similarity off the main event loop
 *  Built-in Node.js worker_threads — zero dependencies
 * ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
 */

const { workerData, parentPort } = require("worker_threads");

const { candidate, storedEmbeddings } = workerData;

function normalize(v) {
  const mag = Math.sqrt(v.reduce((sum, x) => sum + x * x, 0));
  if (mag === 0) return v;
  return v.map((x) => x / mag);
}

function cosineSimilarity(a, b) {
  if (a.length !== b.length) return 0;
  const normA = normalize(a);
  const normB = normalize(b);
  let dot = 0;
  for (let i = 0; i < normA.length; i++) {
    dot += normA[i] * normB[i];
  }
  return dot;
}

function bestCosineSimilarity(candidate, storedEmbeddings) {
  if (!storedEmbeddings || storedEmbeddings.length === 0) return 0;

  let bestScore = 0;
  for (const stored of storedEmbeddings) {
    const sim = cosineSimilarity(candidate, stored);
    if (sim > bestScore) bestScore = sim;
  }

  if (storedEmbeddings.length > 1) {
    const dim = storedEmbeddings[0].length;
    const center = new Array(dim).fill(0);
    for (const emb of storedEmbeddings) {
      for (let i = 0; i < dim; i++) center[i] += emb[i];
    }
    for (let i = 0; i < dim; i++) center[i] /= storedEmbeddings.length;
    const centerNorm = normalize(center);
    const centerSim = cosineSimilarity(candidate, centerNorm);
    if (centerSim > bestScore) bestScore = centerSim;
  }

  return bestScore;
}

// Compute and send result back to main thread
const score = bestCosineSimilarity(candidate, storedEmbeddings);
parentPort.postMessage(score);
