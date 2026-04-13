/**
 * Cluster Mode Entry Point
 * 
 * Spawns one worker per CPU core. Each worker runs the full Express app.
 * This multiplies throughput by the number of cores (typically 4-16x).
 * 
 * Usage:
 *   node cluster.js          (production, multi-core)
 *   node index.js            (development, single process)
 */
const cluster = require("cluster");
const os = require("os");

const NUM_WORKERS = parseInt(process.env.CLUSTER_WORKERS) || os.cpus().length;

if (cluster.isPrimary) {
    console.log(`[CLUSTER] Primary process ${process.pid} starting ${NUM_WORKERS} workers...`);

    for (let i = 0; i < NUM_WORKERS; i++) {
        cluster.fork();
    }

    cluster.on("exit", (worker, code, signal) => {
        console.error(`[CLUSTER] Worker ${worker.process.pid} died (code: ${code}). Restarting...`);
        cluster.fork();
    });

    cluster.on("online", (worker) => {
        console.log(`[CLUSTER] Worker ${worker.process.pid} is online`);
    });
} else {
    // Each worker loads and runs the full Express server
    require("./index.js");
}
