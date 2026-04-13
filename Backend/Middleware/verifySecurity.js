const HttpError = require("../Model/http-error");

/**
 * Pre-flight validation middleware for attendance marking.
 *
 * Validates that the required security fields exist in the request body
 * before reaching the controller's full validation chain.
 *
 * This replaces the previous passthrough stub.
 */
const verifySecurityMessages = async (req, res, next) => {
    const { token, timeSlot, uid } = req.body;

    // uid (recordId) is mandatory
    if (!uid) {
        return next(new HttpError("Missing attendance record ID (uid)", 422));
    }

    // Token and timeSlot should be present for BLE-based attendance
    if (!token) {
        console.warn(
            "Security Warning: BLE token not provided in attendance request"
        );
    }

    if (!timeSlot) {
        console.warn(
            "Security Warning: timeSlot not provided in attendance request"
        );
    }

    next();
};

module.exports = { verifySecurityMessages };
