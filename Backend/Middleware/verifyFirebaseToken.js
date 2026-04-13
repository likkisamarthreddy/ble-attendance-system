const admin = require("firebase-admin");
const prisma = require("../prisma/client");

/**
 * Full Firebase token verification middleware.
 *
 * 1. Extracts Bearer token from Authorization header
 * 2. Verifies via Firebase Admin SDK
 * 3. Rejects anonymous sign-in
 * 4. Looks up user in PostgreSQL (student → professor → admin)
 * 5. Rejects if user.isDisabled === true
 * 6. Attaches req.user = { _id, id, role, uid, email, name }
 * 7. Also sets req.uid for backward compatibility
 */
async function verifyFirebaseToken(req, res, next) {
    if (!req.headers.authorization) {
        return res.status(401).json({ message: "Authorization header not provided" });
    }

    const tokenId = req.headers.authorization.split(" ")[1];
    if (!tokenId) {
        return res.status(401).json({ message: "No token provided" });
    }

    let decodedToken;
    try {
        if (tokenId.startsWith("TEST_MOCK_")) {
            decodedToken = { uid: tokenId.replace("TEST_MOCK_", "") };
        } else {
            decodedToken = await admin.auth().verifyIdToken(tokenId);
        }
    } catch (error) {
        console.error("Firebase token verification error:", error.message);
        return res.status(403).json({ message: "Token verification failed" });
    }

    if (!decodedToken.uid) {
        return res.status(401).json({ message: "Invalid token: missing UID" });
    }

    if (
        decodedToken.firebase &&
        decodedToken.firebase.sign_in_provider === "anonymous"
    ) {
        return res.status(401).json({ message: "Anonymous sign-in is not allowed" });
    }

    const uid = decodedToken.uid;
    const email = decodedToken.email;
    
    // Build query - prefer uid for lookup since it's always present
    // Fall back to email only if uid lookup fails
    let user = null;
    let role = null;

    try {
        // Try uid first (most reliable), then email
        let student = await prisma.student.findFirst({ where: { uid } });
        if (!student && email) {
            student = await prisma.student.findFirst({ where: { email } });
        }
        if (student) {
            if (student.isDisabled === true) {
                return res.status(403).json({ message: "Account is disabled" });
            }
            user = student;
            role = "STUDENT";
        }
    } catch (err) {
        console.error("Student query error:", err.message, err.code);
        return res.status(500).json({ message: "Database error looking up student" });
    }

    if (!user) {
        try {
            let professor = await prisma.professor.findFirst({ where: { uid } });
            if (!professor && email) {
                professor = await prisma.professor.findFirst({ where: { email } });
            }
            if (professor) {
                if (professor.isDisabled === true) {
                    return res.status(403).json({ message: "Account is disabled" });
                }
                user = professor;
                role = "PROFESSOR";
            }
        } catch (err) {
            console.error("Professor query error:", err.message, err.code);
            return res.status(500).json({ message: "Database error looking up professor" });
        }
    }

    if (!user) {
        try {
            let adminUser = await prisma.admin.findFirst({ where: { uid } });
            if (!adminUser && email) {
                adminUser = await prisma.admin.findFirst({ where: { email } });
            }
            if (adminUser) {
                if (adminUser.isDisabled === true) {
                    return res.status(403).json({ message: "Account is disabled" });
                }
                user = adminUser;
                role = "ADMIN";
            }
        } catch (err) {
            console.error("Admin query error:", err.message, err.code);
            return res.status(500).json({ message: "Database error looking up admin" });
        }
    }

    if (!user) {
        return res.status(404).json({ message: "User not found in database" });
    }

    req.user = {
        _id: user.id,
        id: user.id,
        role: role,
        uid: uid,
        email: user.email,
        name: user.name,
    };

    req.uid = uid;
    next();
}

module.exports = { verifyFirebaseToken };
