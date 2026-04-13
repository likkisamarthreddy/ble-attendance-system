const prisma = require("../prisma/client");

/**
 * GET /api/admin/dashboard/stats
 */
async function handleDashboardStats(req, res, next) {
    try {
        const [totalStudents, totalProfessors, totalCourses] = await Promise.all([
            prisma.student.count(),
            prisma.professor.count(),
            prisma.course.count({ where: { isArchived: false } }),
        ]);

        // Overall attendance percentage
        const allRecords = await prisma.attendance.findMany({
            where: { isArchived: false },
            include: { students: { select: { id: true } } },
        });

        let totalPresent = 0;
        let totalRecords = allRecords.length;
        for (const record of allRecords) {
            totalPresent += record.students.length;
        }

        let overallAttendancePercentage = 0;
        if (totalRecords > 0 && totalStudents > 0) {
            const totalPossible = totalRecords * totalStudents;
            overallAttendancePercentage = totalPossible > 0
                ? parseFloat(((totalPresent / totalPossible) * 100).toFixed(1))
                : 0;
        }

        // Students with attendance below thresholds
        const courses = await prisma.course.findMany({
            where: { isArchived: false },
            include: { students: { select: { id: true } } },
        });

        let criticalCount = 0;
        let warningCount = 0;

        for (const course of courses) {
            const records = await prisma.attendance.findMany({
                where: { courseId: course.id },
                include: { students: { select: { id: true } } },
            });
            if (records.length === 0) continue;

            for (const student of course.students) {
                let present = 0;
                for (const record of records) {
                    if (record.students.some((s) => s.id === student.id)) {
                        present++;
                    }
                }
                const pct = (present / records.length) * 100;
                if (pct < 60) criticalCount++;
                else if (pct < 75) warningCount++;
            }
        }

        res.status(200).json({
            totalStudents,
            totalProfessors,
            totalCourses,
            overallAttendancePercentage,
            criticalCount,
            warningCount,
        });
    } catch (err) {
        return res.status(500).json({ message: "Failed to fetch dashboard stats: " + err.message });
    }
}

/**
 * GET /api/admin/dashboard/security
 */
async function handleSecurityStats(req, res, next) {
    try {
        const securityActions = [
            "FACE_VERIFY_FAILED",
            "REPLAY_REJECTED",
            "GEOFENCE_FAILED",
            "TOKEN_INVALID",
            "TIMING_REJECTED",
        ];

        const securityAgg = await prisma.auditLog.groupBy({
            by: ["action"],
            where: { action: { in: securityActions } },
            _count: { action: true },
        });

        const securityMap = {};
        for (const item of securityAgg) {
            securityMap[item.action] = item._count.action;
        }

        const recentEvents = await prisma.auditLog.findMany({
            where: { action: { in: securityActions } },
            orderBy: { timestamp: "desc" },
            take: 50,
        });

        res.status(200).json({
            faceMismatchCount: securityMap["FACE_VERIFY_FAILED"] || 0,
            replayAttempts: securityMap["REPLAY_REJECTED"] || 0,
            geofenceFailures: securityMap["GEOFENCE_FAILED"] || 0,
            tokenInvalid: securityMap["TOKEN_INVALID"] || 0,
            timingRejected: securityMap["TIMING_REJECTED"] || 0,
            recentEvents,
        });
    } catch (err) {
        return res.status(500).json({ message: "Failed to fetch security stats: " + err.message });
    }
}

/**
 * GET /api/admin/audit-logs
 */
async function handleAuditLogs(req, res, next) {
    try {
        const page = parseInt(req.query.page) || 1;
        const limit = Math.min(parseInt(req.query.limit) || 50, 100);
        const skip = (page - 1) * limit;

        const where = {};
        if (req.query.action) {
            where.action = req.query.action;
        }

        const [logs, total] = await Promise.all([
            prisma.auditLog.findMany({
                where,
                orderBy: { timestamp: "desc" },
                skip,
                take: limit,
            }),
            prisma.auditLog.count({ where }),
        ]);

        res.status(200).json({
            logs,
            total,
            page,
            totalPages: Math.ceil(total / limit),
        });
    } catch (err) {
        return res.status(500).json({ message: "Failed to fetch audit logs: " + err.message });
    }
}

module.exports = {
    handleDashboardStats,
    handleSecurityStats,
    handleAuditLogs,
};
