const express = require("express");
const {
    handleStudentRegistrationUsingCsv,
    handleCreateStudentAccount,
    handleViewCurrentCoursesByAdmin,
    handleViewArchiveCoursesByAdmin,
    handleViewAllProfessor,
    handleViewStudentAttendance,
    handleRemoveStudentFace,
    handleDeleteStudent,
    handleGetAllStudentsDetailed,
} = require("../Controller/adminController");
const {
    handleDashboardStats,
    handleSecurityStats,
    handleAuditLogs,
} = require("../Controller/dashboardController");
const { verifyFirebaseToken } = require("../Middleware/verifyFirebaseToken");
const { requireRole } = require("../Middleware/requireRole");
const upload = require("../Middleware/multerConfig");

const adminRouter = express.Router();

// --- Student Face Management ---
adminRouter.delete(
    "/student/:id/face",
    verifyFirebaseToken,
    requireRole("ADMIN"),
    handleRemoveStudentFace
);

// --- Student Deletion ---
adminRouter.delete(
    "/student/:id",
    verifyFirebaseToken,
    requireRole("ADMIN"),
    handleDeleteStudent
);

// --- Enhanced Student Listing ---
adminRouter.get(
    "/students/detailed",
    verifyFirebaseToken,
    requireRole("ADMIN"),
    handleGetAllStudentsDetailed
);

// ─── Dashboard Analytics ─────────────────────────────────────────
adminRouter.get(
    "/dashboard/stats",
    verifyFirebaseToken,
    requireRole("ADMIN"),
    handleDashboardStats
);
adminRouter.get(
    "/dashboard/security",
    verifyFirebaseToken,
    requireRole("ADMIN"),
    handleSecurityStats
);

// ─── Audit Logs ───────────────────────────────────────────────────
adminRouter.get(
    "/dashboard/audit-logs",
    verifyFirebaseToken,
    requireRole("ADMIN"),
    handleAuditLogs
);

// ─── CSV Bulk Registration ────────────────────────────────────────
// Field name is "csvFile" to match the Web Dashboard form upload
adminRouter.post(
    "/register/csv",
    verifyFirebaseToken,
    requireRole("ADMIN"),
    upload.single("csvFile"),
    handleStudentRegistrationUsingCsv
);

// Create single student/professor account
adminRouter.post(
    "/create/student",
    verifyFirebaseToken,
    requireRole("ADMIN"),
    handleCreateStudentAccount
);

// ─── Course Management ────────────────────────────────────────────
adminRouter.get(
    "/course/viewCurrent",
    verifyFirebaseToken,
    requireRole("ADMIN"),
    handleViewCurrentCoursesByAdmin
);
adminRouter.get(
    "/course/viewArchive",
    verifyFirebaseToken,
    requireRole("ADMIN"),
    handleViewArchiveCoursesByAdmin
);

// ─── User Management ─────────────────────────────────────────────
adminRouter.get(
    "/professor/viewAll",
    verifyFirebaseToken,
    requireRole("ADMIN"),
    handleViewAllProfessor
);
adminRouter.get(
    "/student/attendance",
    verifyFirebaseToken,
    requireRole("ADMIN"),
    handleViewStudentAttendance
);

module.exports = adminRouter;
