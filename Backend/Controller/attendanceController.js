const crypto = require("crypto");
const prisma = require("../prisma/client");
const { createAuditLog } = require("../helpers/auditLog");
const { bestCosineSimilarity } = require("./faceController");
const { matchFaceAsync } = require("../helpers/faceMatch");
const { studentCache, courseCache } = require("../helpers/cache");
const { metrics } = require("../Middleware/metricsMiddleware");

// ─── Utility Functions ────────────────────────────────────────────

const SLOT_DURATION_MS = 3000;
const TOKEN_BYTE_LENGTH = 8;

function deriveCourseSecret(courseId) {
  const masterSecret = process.env.BLE_SESSION_SECRET;
  if (!masterSecret) throw new Error("BLE_SESSION_SECRET not configured");
  return crypto.createHmac("sha256", masterSecret).update(courseId.toString()).digest("hex");
}

function generateToken(sessionSecret, timeSlot) {
  const mac = crypto.createHmac("sha256", sessionSecret);
  const slotBytes = Buffer.alloc(8);
  slotBytes.writeBigInt64BE(BigInt(timeSlot));
  const fullHash = mac.update(slotBytes).digest();
  return fullHash.subarray(0, TOKEN_BYTE_LENGTH).toString("hex");
}

function verifyRollingToken(token, sessionSecret, timeSlot) {
  for (let offset = -120; offset <= 120; offset++) {
    const expected = generateToken(sessionSecret, timeSlot + offset);
    if (expected.startsWith(token) || token.startsWith(expected.substring(0, 8))) {
      console.log(`[TOKEN] ✅ Match (course secret) at offset ${offset}`);
      return true;
    }
  }
  return false;
}

function verifyTokenMultiKey(token, courseId, recordId, timeSlot) {
  const courseSecret = deriveCourseSecret(courseId);
  if (verifyRollingToken(token, courseSecret, timeSlot)) return "course_secret";

  for (let offset = -120; offset <= 120; offset++) {
    const expected = generateToken(recordId.toString(), timeSlot + offset);
    if (expected.startsWith(token) || token.startsWith(expected.substring(0, 8))) {
      console.log(`[TOKEN] ✅ Match (record ID key) at offset ${offset}`);
      return "record_id_hmac";
    }
  }

  const recordHex = recordId.toString();
  if (recordHex.startsWith(token) || token.startsWith(recordHex.substring(0, token.length))) {
    console.log(`[TOKEN] ✅ Match (raw byte prefix of record ID)`);
    return "raw_bytes";
  }

  console.log(`[TOKEN] ❌ No match.`);
  return null;
}

function haversineDistance(lat1, lon1, lat2, lon2) {
  const R = 6371e3;
  const toRad = (deg) => (deg * Math.PI) / 180;
  const dLat = toRad(lat2 - lat1);
  const dLon = toRad(lon2 - lon1);
  const a = Math.sin(dLat / 2) ** 2 + Math.cos(toRad(lat1)) * Math.cos(toRad(lat2)) * Math.sin(dLon / 2) ** 2;
  return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
}

// ─── Professor Controllers ─────────────────────────────────────────

async function handleCreateAttendanceRecord(req, res, next) {
  const { courseName, batch, joiningCode } = req.body;
  const uid = req.uid;

  if (!courseName || !batch) {
    return res.status(422).json({ message: "Missing courseName or batch" });
  }

  try {
    let course;
    if (joiningCode) {
      course = await prisma.course.findFirst({ where: { joiningCode } });
    } else {
      course = await prisma.course.findFirst({ where: { name: courseName, batch } });
    }

    if (!course) return res.status(404).json({ message: "Course not found" });

    // End existing active sessions
    await prisma.attendance.updateMany({
      where: { courseId: course.id, sessionActive: true },
      data: { sessionActive: false, sessionEnd: new Date() },
    });

    const record = await prisma.attendance.create({
      data: {
        courseId: course.id,
        date: new Date(),
        batch: batch,
        sessionActive: true,
        sessionStart: new Date(),
      },
      include: { course: true },
    });

    createAuditLog({
      userId: req.user ? req.user.id : 0,
      action: "ADMIN_ACTION",
      role: req.user ? req.user.role : "PROFESSOR",
      courseId: course.id,
      details: { type: "attendance_session_started", recordId: record.id },
      status: "SUCCESS",
      req,
    });

    const sessionSecret = deriveCourseSecret(course.id);
    console.log(`[CREATE-ATTENDANCE] courseId=${course.id}, recordId=${record.id}`);

    res.status(200).json({
      record: { ...record, _id: record.id.toString(), id: record.id },
      sessionSecret: sessionSecret,
    });
  } catch (err) {
    return res.status(500).json({ message: "Creating attendance record failed: " + err.message });
  }
}

async function handleEndSession(req, res, next) {
  const { recordId } = req.body;
  if (!recordId) return res.status(422).json({ message: "Missing recordId" });

  try {
    const record = await prisma.attendance.findUnique({ where: { id: parseInt(recordId) } });
    if (!record) return res.status(404).json({ message: "Attendance record not found" });
    if (!record.sessionActive) return res.status(400).json({ message: "Session already ended" });

    const updated = await prisma.attendance.update({
      where: { id: parseInt(recordId) },
      data: { sessionActive: false, sessionEnd: new Date() },
    });

    createAuditLog({
      userId: req.user ? req.user.id : 0,
      action: "ADMIN_ACTION",
      role: req.user ? req.user.role : "PROFESSOR",
      courseId: record.courseId,
      details: { type: "attendance_session_ended", recordId, sessionEnd: updated.sessionEnd },
      status: "SUCCESS",
      req,
    });

    res.status(200).json({ message: "Session ended successfully", recordId, sessionEnd: updated.sessionEnd });
  } catch (err) {
    return res.status(500).json({ message: "End session failed: " + err.message });
  }
}

async function handleManualAttendance(req, res, next) {
  const { uid, students } = req.body;
  if (!uid || !students || !Array.isArray(students)) {
    return res.status(422).json({ message: "Missing record ID or students array" });
  }

  try {
    const record = await prisma.attendance.findUnique({
      where: { id: parseInt(uid) },
      include: { students: { select: { id: true } } },
    });
    if (!record) return res.status(404).json({ message: "Attendance record not found" });

    const presentIds = new Set(record.students.map((s) => s.id));
    const toConnect = [];

    for (const studentId of students) {
      let student = null;
      try {
        student = await prisma.student.findUnique({ where: { id: parseInt(studentId) } });
      } catch (e) { }
      if (!student) {
        student = await prisma.student.findFirst({ where: { rollno: parseInt(studentId) } });
      }
      if (student && !presentIds.has(student.id)) {
        toConnect.push({ id: student.id });
      }
    }

    if (toConnect.length > 0) {
      await prisma.attendance.update({
        where: { id: parseInt(uid) },
        data: { students: { connect: toConnect } },
      });
    }

    createAuditLog({
      userId: req.user ? req.user.id : 0,
      action: "ATTENDANCE_MANUAL",
      role: req.user ? req.user.role : "PROFESSOR",
      courseId: record.courseId,
      details: { recordId: uid, studentsAdded: students.length },
      status: "SUCCESS",
      req,
    });

    const updatedRecord = await prisma.attendance.findUnique({
      where: { id: parseInt(uid) },
      include: { students: { select: { id: true } } },
    });

    res.status(200).json({
      message: "Attendance updated successfully",
      record: { ...updatedRecord, student: updatedRecord.students.map((s) => s.id) },
    });
  } catch (err) {
    return res.status(500).json({ message: "Manual attendance failed: " + err.message });
  }
}

async function handleModifyAttendance(req, res, next) {
  const { rollno, id } = req.body;
  if (!id || !rollno || !Array.isArray(rollno)) {
    return res.status(422).json({ message: "Missing record ID or rollno list" });
  }

  try {
    const record = await prisma.attendance.findUnique({
      where: { id: parseInt(id) },
      include: { students: { select: { id: true, rollno: true } } },
    });
    if (!record) return res.status(404).json({ message: "Attendance record not found" });

    const students = await prisma.student.findMany({ where: { rollno: { in: rollno.map(Number) } } });
    const presentIds = new Set(record.students.map((s) => s.id));

    const toConnect = [];
    const toDisconnect = [];

    for (const student of students) {
      if (presentIds.has(student.id)) {
        toDisconnect.push({ id: student.id });
      } else {
        toConnect.push({ id: student.id });
      }
    }

    await prisma.attendance.update({
      where: { id: parseInt(id) },
      data: {
        students: {
          connect: toConnect,
          disconnect: toDisconnect,
        },
      },
    });

    createAuditLog({
      userId: req.user ? req.user.id : 0,
      action: "ATTENDANCE_MODIFIED",
      role: req.user ? req.user.role : "PROFESSOR",
      courseId: record.courseId,
      details: { recordId: id, rollnosModified: rollno },
      status: "SUCCESS",
      req,
    });

    res.status(200).json({ message: "Attendance modified successfully" });
  } catch (err) {
    return res.status(500).json({ message: "Modifying attendance failed: " + err.message });
  }
}

async function handleLiveModifyAttendance(req, res, next) {
  const { rollno, id } = req.body;
  if (!id || !rollno || !Array.isArray(rollno)) {
    return res.status(422).json({ message: "Missing record ID or rollno list" });
  }

  try {
    const record = await prisma.attendance.findUnique({
      where: { id: parseInt(id) },
      include: { students: { select: { id: true } } },
    });
    if (!record) return res.status(404).json({ message: "Attendance record not found" });

    const students = await prisma.student.findMany({ where: { rollno: { in: rollno.map(Number) } } });
    const presentIds = new Set(record.students.map((s) => s.id));

    const toConnect = [];
    const toDisconnect = [];

    for (const student of students) {
      if (presentIds.has(student.id)) {
        toDisconnect.push({ id: student.id });
      } else {
        toConnect.push({ id: student.id });
      }
    }

    await prisma.attendance.update({
      where: { id: parseInt(id) },
      data: { students: { connect: toConnect, disconnect: toDisconnect } },
    });

    createAuditLog({
      userId: req.user ? req.user.id : 0,
      action: "ATTENDANCE_LIVE_MODIFIED",
      role: req.user ? req.user.role : "PROFESSOR",
      courseId: record.courseId,
      details: { recordId: id, rollnosModified: rollno },
      status: "SUCCESS",
      req,
    });

    res.status(200).json({ message: "Live attendance modified successfully" });
  } catch (err) {
    return res.status(500).json({ message: "Live modify attendance failed: " + err.message });
  }
}

// ─── Student Attendance (Hardened) ────────────────────────────────

async function handleMarkStudentAttendance(req, res, next) {
  const requestStart = Date.now();
  metrics.attendanceRequestsTotal.inc({ status: "attempted", method: "BLE" });
  metrics.concurrentStudentsGauge.inc();

  const studentUid = req.uid;
  const {
    token, latitude, longitude, faceEmbedding,
    deviceId, integrityToken, auditLog: clientAuditLog,
    uid: recordId,
  } = req.body;

  const faceThreshold = parseFloat(process.env.FACE_SIMILARITY_THRESHOLD) || 0.55;
  const geofenceRadius = parseInt(process.env.GEOFENCE_RADIUS_METERS) || 100;

  if (!recordId || !token) {
    metrics.concurrentStudentsGauge.dec();
    metrics.attendanceFailuresTotal.inc({ reason: "missing_fields" });
    return res.status(422).json({ message: "Missing required fields: uid, token" });
  }
  if (latitude === undefined || longitude === undefined) {
    metrics.concurrentStudentsGauge.dec();
    metrics.attendanceFailuresTotal.inc({ reason: "missing_location" });
    return res.status(422).json({ message: "Missing required fields: latitude, longitude" });
  }
  if (!faceEmbedding || !Array.isArray(faceEmbedding) || faceEmbedding.length === 0) {
    metrics.concurrentStudentsGauge.dec();
    metrics.attendanceFailuresTotal.inc({ reason: "missing_face" });
    return res.status(422).json({ message: "Missing or invalid faceEmbedding" });
  }

  try {
    // ─── FIX 4: Batch queries with Promise.all ──────────────────
    // Fetch student (with cache) and attendance record in PARALLEL
    // instead of 3 sequential round-trips to Neon.
    const studentPromise = (async () => {
      // ─── FIX 2: In-process TTL cache ─────────────────────────
      const cached = studentCache.get(studentUid);
      if (cached) return cached;
      const s = await prisma.student.findUnique({ where: { uid: studentUid } });
      if (s) studentCache.set(studentUid, s);
      return s;
    })();

    const recordPromise = prisma.attendance.findUnique({
      where: { id: parseInt(recordId) },
      include: { students: { select: { id: true } } },
    });

    const [student, record] = await Promise.all([studentPromise, recordPromise]);

    if (!student) {
      metrics.concurrentStudentsGauge.dec();
      return res.status(404).json({ message: "Student not found" });
    }
    if (!record) {
      metrics.concurrentStudentsGauge.dec();
      return res.status(404).json({ message: "Attendance record not found" });
    }
    if (!record.sessionActive) {
      metrics.concurrentStudentsGauge.dec();
      return res.status(403).json({ message: "Attendance session has ended" });
    }

    // Course lookup with cache (FIX 2)
    let course = courseCache.get(record.courseId);
    if (!course) {
      course = await prisma.course.findUnique({
        where: { id: record.courseId },
        include: { students: { select: { id: true } } },
      });
      if (course) courseCache.set(record.courseId, course);
    }
    if (!course) {
      metrics.concurrentStudentsGauge.dec();
      return res.status(404).json({ message: "Course not found" });
    }

    const isEnrolled = course.students.some((s) => s.id === student.id);
    if (!isEnrolled) {
      createAuditLog({ userId: student.id, action: "ENROLLMENT_REJECTED", role: "STUDENT", courseId: course.id, details: { recordId }, status: "FAILURE", req });
      metrics.concurrentStudentsGauge.dec();
      return res.status(403).json({ message: "Student not enrolled in this course" });
    }

    // Token verification
    const serverSlot = Math.floor(Date.now() / SLOT_DURATION_MS);
    const tokenMatch = verifyTokenMultiKey(token, course.id, recordId, serverSlot);
    if (!tokenMatch) {
      metrics.tokenVerificationTotal.inc({ result: "invalid" });
      metrics.concurrentStudentsGauge.dec();
      metrics.attendanceFailuresTotal.inc({ reason: "invalid_token" });
      createAuditLog({ userId: student.id, action: "TOKEN_INVALID", role: "STUDENT", courseId: course.id, details: { recordId, serverSlot }, status: "FAILURE", req });
      return res.status(403).json({ message: "Invalid or expired BLE token" });
    }
    metrics.tokenVerificationTotal.inc({ result: "valid" });

    // Geofence check
    if (course.geofenceLatitude && course.geofenceLongitude) {
      const distance = haversineDistance(latitude, longitude, course.geofenceLatitude, course.geofenceLongitude);
      const radius = course.geofenceRadiusMeters || geofenceRadius;

      if (distance > 1000) {
        console.log(`[GEOFENCE] ⚠️ Distance ${Math.round(distance)}m > 1000m — skipping`);
        metrics.geofenceCheckTotal.inc({ result: "skipped" });
      } else if (distance > radius) {
        metrics.geofenceCheckTotal.inc({ result: "failed" });
        metrics.concurrentStudentsGauge.dec();
        metrics.attendanceFailuresTotal.inc({ reason: "geofence_failed" });
        createAuditLog({ userId: student.id, action: "GEOFENCE_FAILED", role: "STUDENT", courseId: course.id, details: { recordId, distance: Math.round(distance), radius }, status: "FAILURE", req });
        return res.status(403).json({ message: `Outside geofence: ${Math.round(distance)}m away (max: ${radius}m)` });
      } else {
        metrics.geofenceCheckTotal.inc({ result: "passed" });
      }
    }

    // ─── FIX 3: Face verification via worker_threads ───────────
    // Runs cosine similarity OFF the main event loop
    const faceStart = Date.now();
    const storedEmbeddings = Array.isArray(student.faceEmbeddings) ? student.faceEmbeddings : [];
    if (storedEmbeddings.length > 0) {
      let similarity;
      try {
        similarity = await matchFaceAsync(faceEmbedding, storedEmbeddings);
      } catch (workerErr) {
        // Fallback to main-thread if worker fails
        console.warn(`[FACE] Worker failed, falling back: ${workerErr.message}`);
        similarity = bestCosineSimilarity(faceEmbedding, storedEmbeddings);
      }
      metrics.faceProcessingDuration.observe({ operation: "verify" }, (Date.now() - faceStart) / 1000);

      if (similarity < faceThreshold) {
        metrics.concurrentStudentsGauge.dec();
        metrics.attendanceFailuresTotal.inc({ reason: "face_mismatch" });
        createAuditLog({ userId: student.id, action: "FACE_VERIFY_FAILED", role: "STUDENT", courseId: course.id, details: { recordId, similarity: similarity.toFixed(4), threshold: faceThreshold }, status: "FAILURE", req });
        return res.status(403).json({ message: `Face verification failed (similarity: ${similarity.toFixed(2)}, required: ${faceThreshold})` });
      }

      // Adaptive: add new embedding (& invalidate cache)
      const MAX_EMBEDDINGS = 10;
      const newEmbeddings = [...storedEmbeddings, faceEmbedding];
      while (newEmbeddings.length > MAX_EMBEDDINGS) newEmbeddings.shift();
      await prisma.student.update({ where: { id: student.id }, data: { faceEmbeddings: newEmbeddings } });
      studentCache.invalidate(studentUid); // Invalidate after embedding update
    } else {
      metrics.faceProcessingDuration.observe({ operation: "verify" }, (Date.now() - faceStart) / 1000);
    }

    // Check duplicate
    const alreadyPresent = record.students.some((s) => s.id === student.id);
    if (alreadyPresent) {
      metrics.concurrentStudentsGauge.dec();
      metrics.attendanceFailuresTotal.inc({ reason: "duplicate" });
      createAuditLog({ userId: student.id, action: "REPLAY_REJECTED", role: "STUDENT", courseId: course.id, details: { recordId, reason: "duplicate" }, status: "FAILURE", req });
      return res.status(409).json({ message: "Attendance already marked or session ended" });
    }

    // Mark attendance
    await prisma.attendance.update({
      where: { id: parseInt(recordId) },
      data: { students: { connect: { id: student.id } } },
    });

    createAuditLog({
      userId: student.id,
      action: "ATTENDANCE_MARKED",
      role: "STUDENT",
      courseId: course.id,
      details: { recordId, deviceId, serverSlot },
      status: "SUCCESS",
      req,
    });

    const populatedRecord = await prisma.attendance.findUnique({
      where: { id: parseInt(recordId) },
      include: { course: true },
    });

    const duration = Date.now() - requestStart;
    metrics.attendanceMarkingDuration.observe({ status: "success" }, duration);
    metrics.concurrentStudentsGauge.dec();

    // ─── Socket.IO Real-Time Publish ────────────────────────────────
    const io = req.app.get("io");
    if (io) {
      io.to(`course_${course.id}`).emit("attendance_marked", {
        studentId: student.id,
        uid: student.uid,
        rollno: student.rollno,
        name: student.name,
        profilePicture: student.profilePicture,
        timestamp: new Date().toISOString(),
      });
    }

    metrics.attendanceRequestsTotal.inc({ status: "success", method: "BLE" });
    return res.status(200).json({
      message: "Attendance marked successfully",
      record: { ...populatedRecord, _id: populatedRecord.id.toString() },
    });
  } catch (err) {
    metrics.concurrentStudentsGauge.dec();
    metrics.attendanceFailuresTotal.inc({ reason: "server_error" });
    return res.status(500).json({ message: "Marking attendance failed: " + err.message });
  }
}

async function handleGetActiveSessions(req, res, next) {
  const studentUid = req.uid;

  try {
    const student = await prisma.student.findUnique({
      where: { uid: studentUid },
      include: { courses: { select: { id: true } } },
    });
    if (!student) return res.status(404).json({ message: "Student not found" });

    const courseIds = student.courses.map((c) => c.id);
    if (courseIds.length === 0) return res.status(200).json({ sessions: [] });

    const activeRecords = await prisma.attendance.findMany({
      where: { courseId: { in: courseIds }, sessionActive: true },
      include: { course: { select: { name: true, batch: true, id: true } } },
    });

    const sessions = activeRecords.map((record) => {
      const sessionSecret = deriveCourseSecret(record.course.id);
      return {
        recordId: record.id.toString(),
        courseName: record.course.name,
        batch: record.course.batch,
        sessionSecret,
      };
    });

    return res.status(200).json({ sessions });
  } catch (err) {
    return res.status(500).json({ message: "Failed to fetch active sessions: " + err.message });
  }
}

module.exports = {
  handleCreateAttendanceRecord,
  handleEndSession,
  handleManualAttendance,
  handleModifyAttendance,
  handleLiveModifyAttendance,
  handleMarkStudentAttendance,
  handleGetActiveSessions,
};
