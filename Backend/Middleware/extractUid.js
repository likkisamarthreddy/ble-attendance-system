const admin = require("firebase-admin");
const path = require("path");

// Load Firebase service account key from env-configured path
const keyPath = process.env.FIREBASE_KEY_PATH || "./serviceAccountKey.json";
const serviceAccount = require(path.resolve(__dirname, "..", keyPath));

if (!admin.apps.length) {
    admin.initializeApp({
        credential: admin.credential.cert(serviceAccount),
    });
}

/**
 * Lightweight token extraction middleware.
 * Verifies Firebase ID token and sets req.uid.
 */
async function extractToken(req, res, next) {
    if (!req.headers.authorization) {
        return res.status(422).json({ message: "Firebase token not provided" });
    }
    const tokenid = req.headers.authorization.split(" ")[1];

    if (!tokenid) {
        return res.status(401).json({ message: "No tokenId provided" });
    }

    try {
        const decodedToken = await admin.auth().verifyIdToken(tokenid);

        if (
            decodedToken.firebase &&
            decodedToken.firebase.sign_in_provider === "anonymous"
        ) {
            return res.status(401).json({ message: "Anonymous sign-in is not allowed" });
        }

        req.uid = decodedToken.uid;
        req.email = decodedToken.email; // Extract email to allow multi-provider login
        next();
    } catch (error) {
        console.error("Token extraction error:", error);
        return res.status(403).json({ message: "Token verification failed" });
    }
}

module.exports = { extractToken };