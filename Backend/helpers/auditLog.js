const prisma = require("../prisma/client");

/**
 * Helper to create an audit log entry.
 */
async function createAuditLog({ userId, action, role, courseId, details, status, req }) {
    try {
        await prisma.auditLog.create({
            data: {
                userId: typeof userId === "number" ? userId : 0,
                action: action,
                role: role,
                courseId: courseId || undefined,
                details: details || {},
                ipAddress: req
                    ? req.headers["x-forwarded-for"] || req.socket?.remoteAddress
                    : undefined,
                userAgent: req ? req.headers["user-agent"] : undefined,
                status: status,
            },
        });
    } catch (err) {
        console.error("Audit log write failed:", err.message);
    }
}

module.exports = { createAuditLog };
