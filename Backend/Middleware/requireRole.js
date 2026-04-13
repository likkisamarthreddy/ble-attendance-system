const HttpError = require("../helpers/http-error");

/**
 * Role-based access control middleware factory.
 *
 * Usage:
 *   requireRole("ADMIN")                — single role
 *   requireRole(["ADMIN", "PROFESSOR"]) — multiple roles (OR logic)
 *
 * Must be used AFTER verifyFirebaseToken middleware (which sets req.user.role).
 */
function requireRole(role) {
    const allowedRoles = Array.isArray(role) ? role : [role];
    return (req, res, next) => {
        if (!req.user) {
            return next(
                new HttpError("Authentication required before role check", 401)
            );
        }
        if (!allowedRoles.includes(req.user.role)) {
            return next(
                new HttpError(
                    `Access denied: requires ${allowedRoles.join(" or ")} role`,
                    403
                )
            );
        }
        next();
    };
}

module.exports = { requireRole };
