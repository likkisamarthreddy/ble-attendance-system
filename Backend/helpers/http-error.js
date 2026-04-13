/**
 * Simple HTTP Error class (kept for backward compatibility with requireRole.js)
 */
class HttpError extends Error {
    constructor(message, errorCode) {
        super(message);
        this.code = errorCode;
    }
}

module.exports = HttpError;
