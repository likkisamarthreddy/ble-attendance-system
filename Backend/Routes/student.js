const express = require("express");
const rateLimit = require("express-rate-limit");
const {
    handleStudentViewCourse,
    handleStudentViewAttendance,
    handleStudentRegistration,
    handleStudentJoinCourse,
    handleStudentProfile,
    handleStudentCoursesWithAttendance,
} = require("../Controller/studentController");
const {
    handleMarkStudentAttendance,
    handleGetActiveSessions,
} = require("../Controller/attendanceController");
const { handleSimBinding } = require("../Controller/authController");
const {
    handleFaceRegister,
    handleFaceVerify,
} = require("../Controller/faceController");
const { verifyFirebaseToken } = require("../Middleware/verifyFirebaseToken");
const { requireRole } = require("../Middleware/requireRole");

const studentRouter = express.Router();

// Attendance-specific rate limiter — max 5 per minute per IP
const attendanceLimiter = rateLimit({
    windowMs: 60 * 1000,
    max: 5,
    message: { message: "Too many attendance attempts, please wait" },
});

// View enrolled courses — requires STUDENT role
studentRouter.get(
    "/courses",
    verifyFirebaseToken,
    requireRole("STUDENT"),
    handleStudentViewCourse
);

// View attendance — requires STUDENT role
studentRouter.get(
    "/attendance",
    verifyFirebaseToken,
    requireRole("STUDENT"),
    handleStudentViewAttendance
);

// View courses with attendance percentage — requires STUDENT role
studentRouter.get(
    "/courses/attendance",
    verifyFirebaseToken,
    requireRole("STUDENT"),
    handleStudentCoursesWithAttendance
);

// Student registration (no role check — user is registering)
studentRouter.post("/register", verifyFirebaseToken, handleStudentRegistration);

// Mark attendance via BLE scan — requires STUDENT role + rate limited
studentRouter.post(
    "/attendance",
    verifyFirebaseToken,
    requireRole("STUDENT"),
    attendanceLimiter,
    handleMarkStudentAttendance
);

// Join course — requires STUDENT role
studentRouter.post(
    "/course/join",
    verifyFirebaseToken,
    requireRole("STUDENT"),
    handleStudentJoinCourse
);

// Student profile — requires STUDENT role
studentRouter.get(
    "/profile",
    verifyFirebaseToken,
    requireRole("STUDENT"),
    handleStudentProfile
);

// SIM binding (uses raw authorization header, not Firebase middleware)
studentRouter.get("/sim", handleSimBinding);

// ── Face Recognition Endpoints ──
// Register face embedding — requires STUDENT role
studentRouter.post(
    "/face/register",
    verifyFirebaseToken,
    requireRole("STUDENT"),
    handleFaceRegister
);

// Verify face embedding — requires STUDENT role
studentRouter.post(
    "/face/verify",
    verifyFirebaseToken,
    requireRole("STUDENT"),
    handleFaceVerify
);

// Get active attendance sessions for enrolled courses — requires STUDENT role
studentRouter.get(
    "/attendance/active",
    verifyFirebaseToken,
    requireRole("STUDENT"),
    handleGetActiveSessions
);

module.exports = studentRouter;