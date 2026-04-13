const express = require("express");
const {
  handleProfessorRegistration,
  handleViewCoursesByProfessor,
  handleProfessorViewAttendance,
  handleViewAllStudents,
  handleViewAllAttendanceRecords,
  handleViewRecordData,
  handleViewArchivedCoursesByProfessor,
  handleProfessorProfile,
} = require("../Controller/professorController");
const {
  handleCourseCreation,
  handleCourseStudents,
  handleViewStudentsInCourse,
  handleUpdateGeofence,
} = require("../Controller/courseController");
const {
  handleCreateAttendanceRecord,
  handleEndSession,
  handleManualAttendance,
  handleModifyAttendance,
  handleLiveModifyAttendance,
} = require("../Controller/attendanceController");
const { verifyFirebaseToken } = require("../Middleware/verifyFirebaseToken");
const { requireRole } = require("../Middleware/requireRole");
const router = express.Router();

// Professor registration (no role check — user is registering)
router.post("/register", handleProfessorRegistration);

// View current courses — requires PROFESSOR role
router.get(
  "/course/current",
  verifyFirebaseToken,
  requireRole("PROFESSOR"),
  handleViewCoursesByProfessor
);

// View archived courses — requires PROFESSOR role
router.get(
  "/course/archived",
  verifyFirebaseToken,
  requireRole("PROFESSOR"),
  handleViewArchivedCoursesByProfessor
);

// View all students in a course — requires PROFESSOR or ADMIN role
router.get(
  "/course/student",
  verifyFirebaseToken,
  requireRole(["PROFESSOR", "ADMIN"]),
  handleViewStudentsInCourse
);

// Create course — requires PROFESSOR role
router.post(
  "/course",
  verifyFirebaseToken,
  requireRole("PROFESSOR"),
  handleCourseCreation
);

// View all students — requires PROFESSOR or ADMIN role
router.get(
  "/students",
  verifyFirebaseToken,
  requireRole(["PROFESSOR", "ADMIN"]),
  handleViewAllStudents
);

// Update course students — requires PROFESSOR role
router.patch(
  "/course",
  verifyFirebaseToken,
  requireRole("PROFESSOR"),
  handleCourseStudents
);

// Update geofence settings for a course — requires PROFESSOR role
router.patch(
  "/course/geofence",
  verifyFirebaseToken,
  requireRole("PROFESSOR"),
  handleUpdateGeofence
);

// Create attendance record (start session) — requires PROFESSOR role
router.post(
  "/attendance",
  verifyFirebaseToken,
  requireRole("PROFESSOR"),
  handleCreateAttendanceRecord
);

// End attendance session — requires PROFESSOR role
router.patch(
  "/attendance/session/end",
  verifyFirebaseToken,
  requireRole("PROFESSOR"),
  handleEndSession
);

// View attendance records for a course — requires PROFESSOR role
router.get(
  "/attendance/course",
  verifyFirebaseToken,
  requireRole("PROFESSOR"),
  handleViewAllAttendanceRecords
);

// View record data — requires PROFESSOR role
router.get(
  "/attendance/record",
  verifyFirebaseToken,
  requireRole("PROFESSOR"),
  handleViewRecordData
);

// Manual attendance — requires PROFESSOR role
router.patch(
  "/attendance/manual",
  verifyFirebaseToken,
  requireRole("PROFESSOR"),
  handleManualAttendance
);

// Modify attendance — requires PROFESSOR role
router.patch(
  "/attendance/modify",
  verifyFirebaseToken,
  requireRole("PROFESSOR"),
  handleModifyAttendance
);

// Live modify attendance — requires PROFESSOR role
router.patch(
  "/attendance/live/modify",
  verifyFirebaseToken,
  requireRole("PROFESSOR"),
  handleLiveModifyAttendance
);

// Full attendance for excel — requires PROFESSOR role
router.get(
  "/fullattendance",
  verifyFirebaseToken,
  requireRole("PROFESSOR"),
  handleProfessorViewAttendance
);

// Professor profile — requires PROFESSOR or ADMIN role
router.get(
  "/profile",
  verifyFirebaseToken,
  requireRole(["PROFESSOR", "ADMIN"]),
  handleProfessorProfile
);

module.exports = router;
