/**
 * ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
 *  BLE Attendance System — Enterprise k6 Stress Test
 *  Tests: Attendance API, Face Verification, Health, Profile endpoints
 *  Scenarios: Smoke, Load, Stress, Spike, Soak
 * ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
 *
 *  Usage:
 *    k6 run scripts/k6_stress_test.js
 *    k6 run scripts/k6_stress_test.js --out json=results/k6_results.json
 *    k6 run scripts/k6_stress_test.js --env SCENARIO=spike
 */

import http from "k6/http";
import { check, sleep, group } from "k6";
import { Counter, Rate, Trend, Gauge } from "k6/metrics";
import { SharedArray } from "k6/data";
function randomIntBetween(min, max) { return Math.floor(Math.random() * (max - min + 1) + min); }

// ─── Configuration ───────────────────────────────────────────────
const BASE_URL = __ENV.BASE_URL || "http://localhost:8000";
const SCENARIO = __ENV.SCENARIO || "full";

// ─── Load Test Data ──────────────────────────────────────────────
const testData = JSON.parse(open("./k6_data.json"));
const students = new SharedArray("students", () => testData.students);

// ─── Custom Metrics ──────────────────────────────────────────────
const attendanceSuccess = new Counter("attendance_success_total");
const attendanceFailed = new Counter("attendance_failed_total");
const faceVerifyDuration = new Trend("face_verify_duration_ms");
const attendanceDuration = new Trend("attendance_marking_duration_ms");
const healthCheckDuration = new Trend("health_check_duration_ms");
const profileFetchDuration = new Trend("profile_fetch_duration_ms");
const errorRate = new Rate("error_rate");
const activeVUs = new Gauge("active_virtual_users");

// ─── Scenarios ───────────────────────────────────────────────────
const scenarios = {
  // Smoke Test: Verify system works under minimal load
  smoke: {
    smoke_test: {
      executor: "constant-vus",
      vus: 5,
      duration: "30s",
      tags: { scenario: "smoke" },
    },
  },

  // Load Test: Normal expected load
  load: {
    load_test: {
      executor: "ramping-vus",
      startVUs: 0,
      stages: [
        { duration: "10s", target: 50 },   // Ramp up
        { duration: "30s", target: 50 },   // Sustain
        { duration: "10s", target: 0 },    // Ramp down
      ],
      tags: { scenario: "load" },
    },
  },

  // Stress Test: Push beyond normal capacity
  stress: {
    stress_test: {
      executor: "ramping-vus",
      startVUs: 0,
      stages: [
        { duration: "10s", target: 100 },  // Normal load
        { duration: "20s", target: 200 },  // Heavy load
        { duration: "20s", target: 300 },  // Stress zone
        { duration: "15s", target: 0 },    // Recovery
      ],
      tags: { scenario: "stress" },
    },
  },

  // Spike Test: Sudden burst of traffic
  spike: {
    spike_test: {
      executor: "ramping-vus",
      startVUs: 0,
      stages: [
        { duration: "5s", target: 10 },    // Normal
        { duration: "5s", target: 500 },   // SPIKE!
        { duration: "20s", target: 500 },  // Sustain spike
        { duration: "5s", target: 10 },    // Recovery
        { duration: "10s", target: 0 },    // Cool down
      ],
      tags: { scenario: "spike" },
    },
  },

  // Soak Test: Extended duration to detect memory leaks
  soak: {
    soak_test: {
      executor: "constant-vus",
      vus: 30,
      duration: "10m",
      tags: { scenario: "soak" },
    },
  },

  // Full Test: All scenarios combined
  full: {
    smoke: {
      executor: "constant-vus",
      vus: 5,
      duration: "20s",
      tags: { scenario: "smoke" },
      exec: "smokeTest",
    },
    ramp_up: {
      executor: "ramping-vus",
      startVUs: 0,
      stages: [
        { duration: "30s", target: 50 },
        { duration: "1m", target: 100 },
        { duration: "1m", target: 200 },
        { duration: "30s", target: 300 },
        { duration: "30s", target: 0 },
      ],
      startTime: "25s",
      tags: { scenario: "full_ramp" },
      exec: "default",
    },
  },

  // ─── THROUGHPUT TEST: Max marks in 1 minute ───────────────
  // Holds a constant load of 150 VUs (sweet spot before degradation)
  // for exactly 1 minute to measure maximum attendance marks per minute.
  throughput: {
    throughput_test: {
      executor: "constant-vus",
      vus: 150,
      duration: "1m",
      tags: { scenario: "throughput" },
    },
  },

  // ─── BREAKPOINT TEST: Ramp until the server dies ───────────────
  // Continuously increases VUs until 5xx errors appear or connections fail.
  // This finds the exact breaking point of your infrastructure.
  breakpoint: {
    breakpoint_test: {
      executor: "ramping-vus",
      startVUs: 0,
      stages: [
        { duration: "20s", target: 50 },    //  50 VUs — warm-up
        { duration: "20s", target: 100 },   // 100 VUs
        { duration: "20s", target: 200 },   // 200 VUs
        { duration: "20s", target: 300 },   // 300 VUs
        { duration: "20s", target: 400 },   // 400 VUs
        { duration: "20s", target: 500 },   // 500 VUs — heavy
        { duration: "30s", target: 600 },   // 600 VUs — extreme
        { duration: "30s", target: 750 },   // 750 VUs — max destruction
        { duration: "1m", target: 750 },    // Sustain max chaos
        { duration: "20s", target: 0 },     // Recovery (if still alive)
      ],
      tags: { scenario: "breakpoint" },
    },
  },
};

// ─── Options ─────────────────────────────────────────────────────
export const options = {
  scenarios: scenarios[SCENARIO] || scenarios.full,
  thresholds: {
    // API response time SLOs
    http_req_duration: ["p(95)<2000", "p(99)<5000"],
    // Error rate must be below 10%
    error_rate: ["rate<0.10"],
    // Custom metric thresholds
    attendance_marking_duration_ms: ["p(95)<3000"],
    face_verify_duration_ms: ["p(95)<1500"],
    // System must handle at least some requests successfully
    attendance_success_total: ["count>0"],
  },
};

// ─── Utility: Generate fresh token ───────────────────────────────
function generateFreshToken() {
  // For the stress test, we use the pre-generated token from data prep
  // In production the BLE device would broadcast rolling tokens
  return testData.session.token;
}

// ─── Utility: Get random student ─────────────────────────────────
function getRandomStudent() {
  return students[randomIntBetween(0, students.length - 1)];
}

// ─── Utility: Simulate device headers ────────────────────────────
function getDeviceHeaders(student) {
  const ipOctet1 = randomIntBetween(10, 192);
  const ipOctet2 = randomIntBetween(0, 255);
  const ipOctet3 = randomIntBetween(0, 255);
  const ipOctet4 = randomIntBetween(1, 254);
  return {
    "Content-Type": "application/json",
    "Authorization": `Bearer ${student.authToken}`,
    "X-Forwarded-For": `${ipOctet1}.${ipOctet2}.${ipOctet3}.${ipOctet4}`,
    "User-Agent": `BLEAttendance/2.0 (Android ${randomIntBetween(10, 14)}; Samsung Galaxy S${randomIntBetween(21, 24)})`,
    "X-Device-Id": `device_${student.id}_${Date.now()}`,
  };
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
//  TEST SCENARIOS
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

// Smoke test — simple health + profile checks
export function smokeTest() {
  group("🏥 Health Check", () => {
    const start = Date.now();
    const res = http.get(`${BASE_URL}/health`);
    healthCheckDuration.add(Date.now() - start);
    check(res, {
      "health: status 200": (r) => r.status === 200,
      "health: has uptime": (r) => JSON.parse(r.body).uptime > 0,
    });
  });

  group("👤 Student Profile", () => {
    const student = getRandomStudent();
    const start = Date.now();
    const res = http.get(`${BASE_URL}/api/student/profile`, {
      headers: {
        "Authorization": `Bearer ${student.authToken}`,
        "X-Forwarded-For": `10.0.${randomIntBetween(0,255)}.${randomIntBetween(1,254)}`,
      },
    });
    profileFetchDuration.add(Date.now() - start);
    check(res, {
      "profile: status 200": (r) => r.status === 200,
    });
  });

  sleep(1);
}

// Default test — full attendance marking flow
export default function () {
  activeVUs.add(__VU);
  const student = getRandomStudent();
  const headers = getDeviceHeaders(student);

  // 1. Health Check
  group("🏥 Health Check", () => {
    const start = Date.now();
    const res = http.get(`${BASE_URL}/health`);
    healthCheckDuration.add(Date.now() - start);
    check(res, { "health: 200": (r) => r.status === 200 });
  });

  // 2. Student Profile
  group("👤 Fetch Profile", () => {
    const start = Date.now();
    const res = http.get(`${BASE_URL}/api/student/profile`, { headers });
    profileFetchDuration.add(Date.now() - start);
    const ok = check(res, {
      "profile: 200": (r) => r.status === 200,
    });
    errorRate.add(!ok);
  });

  // 3. Face Verification
  group("🧠 Face Verification", () => {
    const start = Date.now();
    const payload = JSON.stringify({
      faceEmbedding: student.faceEmbedding,
    });
    const res = http.post(`${BASE_URL}/api/student/face/verify`, payload, { headers });
    faceVerifyDuration.add(Date.now() - start);
    const ok = check(res, {
      "face: status ok": (r) => r.status === 200 || r.status === 400,
      "face: has similarity": (r) => {
        if (r.status === 200) {
          const body = JSON.parse(r.body);
          return body.similarity !== undefined;
        }
        return true;
      },
    });
    errorRate.add(!ok);
  });

  // 4. Mark Attendance (the core stress target)
  group("🎯 Mark Attendance", () => {
    const start = Date.now();
    // Add slight lat/lng jitter to simulate real students in the geofence
    const latJitter = (Math.random() - 0.5) * 0.001;
    const lngJitter = (Math.random() - 0.5) * 0.001;

    const payload = JSON.stringify({
      uid: testData.session.recordId,
      token: generateFreshToken(),
      latitude: testData.geofence.latitude + latJitter,
      longitude: testData.geofence.longitude + lngJitter,
      faceEmbedding: student.faceEmbedding,
      deviceId: `k6_device_${student.id}_${__VU}`,
      integrityToken: "k6_stress_test",
      auditLog: [{ event: "K6_STRESS_TEST", vu: __VU, iter: __ITER }],
    });

    const res = http.post(`${BASE_URL}/api/student/attendance`, payload, { headers });
    attendanceDuration.add(Date.now() - start);

    const ok = check(res, {
      "attendance: accepted (200/409)": (r) => r.status === 200 || r.status === 409,
      "attendance: not server error": (r) => r.status < 500,
    });

    if (res.status === 200) {
      attendanceSuccess.add(1);
    } else {
      attendanceFailed.add(1);
    }
    errorRate.add(res.status >= 500);
  });

  // 5. Fetch Active Sessions
  group("📋 Active Sessions", () => {
    const res = http.get(`${BASE_URL}/api/student/attendance/active`, { headers });
    check(res, {
      "sessions: status ok": (r) => r.status === 200,
    });
  });

  // 6. Fetch Courses with Attendance
  group("📊 Courses Attendance", () => {
    const res = http.get(`${BASE_URL}/api/student/courses/attendance`, { headers });
    check(res, {
      "courses: status ok": (r) => r.status === 200,
    });
  });

  // Minimize think time for maximum pressure
  sleep(0.5);
}

// ─── Report Generation ──────────────────────────────────────────
export function handleSummary(data) {
  const timestamp = new Date().toISOString().replace(/[:.]/g, "-");
  return {
    [`../Testing/stress_test_results_${timestamp}.json`]: JSON.stringify(data, null, 2),
  };
}
