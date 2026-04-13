/**
 * ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
 *  Prometheus Metrics Middleware for BLE Attendance System
 *  Enterprise-grade observability layer
 * ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
 */

const promBundle = require("express-prom-bundle");
const promClient = require("prom-client");

// ─── Collect Default Node.js Metrics ──────────────────────────────
// CPU, memory, event loop lag, active handles, GC pauses, etc.
promClient.collectDefaultMetrics({
  prefix: "ble_attendance_",
  gcDurationBuckets: [0.001, 0.01, 0.1, 1, 2, 5],
});

// ─── Custom Business Metrics ──────────────────────────────────────

// Counter: Total attendance requests
const attendanceRequestsTotal = new promClient.Counter({
  name: "ble_attendance_requests_total",
  help: "Total number of attendance marking requests",
  labelNames: ["status", "method"],
});

// Counter: Attendance failures
const attendanceFailuresTotal = new promClient.Counter({
  name: "ble_attendance_failures_total",
  help: "Total number of failed attendance marking requests",
  labelNames: ["reason"],
});

// Histogram: Face processing duration
const faceProcessingDuration = new promClient.Histogram({
  name: "ble_face_processing_duration_seconds",
  help: "Time spent on face verification/recognition in seconds",
  labelNames: ["operation"],
  buckets: [0.01, 0.025, 0.05, 0.1, 0.25, 0.5, 1, 2.5, 5, 10],
});

// Histogram: Database query duration
const dbQueryDuration = new promClient.Histogram({
  name: "ble_db_query_duration_seconds",
  help: "Duration of database queries in seconds",
  labelNames: ["operation", "model"],
  buckets: [0.005, 0.01, 0.025, 0.05, 0.1, 0.25, 0.5, 1, 2.5],
});

// Gauge: Active attendance sessions
const activeSessionsGauge = new promClient.Gauge({
  name: "ble_active_attendance_sessions",
  help: "Number of currently active attendance sessions",
});

// Gauge: Connected students (concurrent)
const concurrentStudentsGauge = new promClient.Gauge({
  name: "ble_concurrent_students",
  help: "Number of students concurrently marking attendance",
});

// Counter: Token verification results
const tokenVerificationTotal = new promClient.Counter({
  name: "ble_token_verification_total",
  help: "Token verification outcomes",
  labelNames: ["result"],
});

// Counter: Geofence check results
const geofenceCheckTotal = new promClient.Counter({
  name: "ble_geofence_check_total",
  help: "Geofence verification outcomes",
  labelNames: ["result"],
});

// Histogram: Attendance marking end-to-end duration
const attendanceMarkingDuration = new promClient.Histogram({
  name: "ble_attendance_marking_duration_ms",
  help: "End-to-end time for marking attendance in milliseconds",
  labelNames: ["status"],
  buckets: [50, 100, 250, 500, 1000, 2500, 5000, 10000],
});

// Histogram: API response size
const responseSizeHistogram = new promClient.Histogram({
  name: "ble_response_size_bytes",
  help: "HTTP response size in bytes",
  labelNames: ["route", "method"],
  buckets: [100, 500, 1000, 5000, 10000, 50000, 100000],
});

// ─── Express Middleware (auto-instruments all routes) ─────────────
const metricsMiddleware = promBundle({
  includeMethod: true,
  includePath: true,
  includeStatusCode: true,
  includeUp: true,
  customLabels: { app: "ble-attendance-backend" },
  promClient: {
    collectDefaultMetrics: {},
  },
  normalizePath: [
    // Normalize dynamic route params
    [/\/api\/admin\/student\/\d+\/face/, "/api/admin/student/#id/face"],
    [/\/api\/admin\/student\/\d+/, "/api/admin/student/#id"],
  ],
  formatStatusCode: (res) => {
    if (res.statusCode < 200) return "1xx";
    if (res.statusCode < 300) return "2xx";
    if (res.statusCode < 400) return "3xx";
    if (res.statusCode < 500) return "4xx";
    return "5xx";
  },
});

// ─── Export everything ────────────────────────────────────────────
module.exports = {
  metricsMiddleware,
  promClient,
  metrics: {
    attendanceRequestsTotal,
    attendanceFailuresTotal,
    faceProcessingDuration,
    dbQueryDuration,
    activeSessionsGauge,
    concurrentStudentsGauge,
    tokenVerificationTotal,
    geofenceCheckTotal,
    attendanceMarkingDuration,
    responseSizeHistogram,
  },
};
