const prisma = require("../prisma/client");
const { createAuditLog } = require("../helpers/auditLog");

const MAX_EMBEDDINGS = 10;

function normalize(v) {
    const mag = Math.sqrt(v.reduce((sum, x) => sum + x * x, 0));
    if (mag === 0) return v;
    return v.map((x) => x / mag);
}

function cosineSimilarity(a, b) {
    if (a.length !== b.length) return 0;
    const normA = normalize(a);
    const normB = normalize(b);
    let dot = 0;
    for (let i = 0; i < normA.length; i++) {
        dot += normA[i] * normB[i];
    }
    return dot;
}

function bestCosineSimilarity(candidate, storedEmbeddings) {
    if (!storedEmbeddings || storedEmbeddings.length === 0) return 0;

    let bestScore = 0;
    for (const stored of storedEmbeddings) {
        const sim = cosineSimilarity(candidate, stored);
        if (sim > bestScore) bestScore = sim;
    }

    if (storedEmbeddings.length > 1) {
        const dim = storedEmbeddings[0].length;
        const center = new Array(dim).fill(0);
        for (const emb of storedEmbeddings) {
            for (let i = 0; i < dim; i++) center[i] += emb[i];
        }
        for (let i = 0; i < dim; i++) center[i] /= storedEmbeddings.length;
        const centerNorm = normalize(center);
        const centerSim = cosineSimilarity(candidate, centerNorm);
        if (centerSim > bestScore) bestScore = centerSim;
    }

    return bestScore;
}

async function handleFaceRegister(req, res, next) {
    const uid = req.uid;
    let { faceEmbeddings, faceEmbedding, profilePicture } = req.body;

    if (!faceEmbeddings && faceEmbedding && Array.isArray(faceEmbedding)) {
        faceEmbeddings = [faceEmbedding];
    }

    if (!faceEmbeddings || !Array.isArray(faceEmbeddings) || faceEmbeddings.length === 0) {
        return res.status(422).json({ message: "faceEmbeddings array is required" });
    }

    const VALID_SIZES = [128, 192, 512];
    for (let e = 0; e < faceEmbeddings.length; e++) {
        const emb = faceEmbeddings[e];
        if (!Array.isArray(emb) || !VALID_SIZES.includes(emb.length)) {
            return res.status(422).json({ message: `faceEmbeddings[${e}] must be 128, 192, or 512 dimensions` });
        }
        for (let i = 0; i < emb.length; i++) {
            if (typeof emb[i] !== "number" || isNaN(emb[i])) {
                return res.status(422).json({ message: `faceEmbeddings[${e}][${i}] is not a valid number` });
            }
        }
    }

    try {
        const student = await prisma.student.findUnique({ where: { uid } });
        if (!student) return res.status(404).json({ message: "Student not found" });

        // Sanitize embeddings: replace NaN/Infinity with 0, then round-trip
        // through JSON to ensure Prisma's Json field gets clean data
        const sanitizedEmbeddings = faceEmbeddings.map(emb =>
            emb.map(v => (typeof v === "number" && isFinite(v)) ? parseFloat(v.toFixed(8)) : 0)
        );
        const cleanEmbeddings = JSON.parse(JSON.stringify(sanitizedEmbeddings));

        const hasProfilePic = profilePicture && typeof profilePicture === "string" && profilePicture.length > 0;
        console.log(`[FACE-REGISTER] uid=${uid}, embeddings=${cleanEmbeddings.length}, dim=${cleanEmbeddings[0]?.length}, hasProfilePic=${hasProfilePic}, picSize=${hasProfilePic ? profilePicture.length : 0}`);

        // Step 1: Update face embeddings + timestamp
        await prisma.student.update({
            where: { uid: uid },
            data: {
                faceEmbeddings: cleanEmbeddings,
                faceRegisteredAt: new Date(),
            },
        });

        // Step 2: Update profile picture separately (if provided)
        if (hasProfilePic) {
            try {
                await prisma.student.update({
                    where: { uid: uid },
                    data: {
                        profilePicture: profilePicture,
                    },
                });
                console.log(`[FACE-REGISTER] Profile picture saved for uid=${uid}`);
            } catch (picErr) {
                console.error(`[FACE-REGISTER] Profile picture save failed (non-fatal):`, picErr.message);
                // Don't fail the whole registration if just the picture fails
            }
        }

        createAuditLog({
            userId: student.id,
            action: "FACE_REGISTERED",
            role: "STUDENT",
            details: { embeddingCount: cleanEmbeddings.length, embeddingDim: cleanEmbeddings[0].length },
            status: "SUCCESS",
            req,
        });

        res.status(200).json({
            message: `Face registered with ${cleanEmbeddings.length} embeddings`,
            registeredAt: new Date(),
            embeddingCount: cleanEmbeddings.length,
        });
    } catch (err) {
        console.error(`[FACE-REGISTER] FULL ERROR for uid=${uid}:`, err);
        return res.status(500).json({ message: "Face registration failed: " + err.message });
    }
}

async function handleFaceVerify(req, res, next) {
    const uid = req.uid;
    const { faceEmbedding } = req.body;
    const threshold = parseFloat(process.env.FACE_SIMILARITY_THRESHOLD) || 0.55;

    if (!faceEmbedding || !Array.isArray(faceEmbedding)) {
        return res.status(422).json({ message: "faceEmbedding array is required" });
    }

    const VALID_SIZES = [128, 192, 512];
    if (!VALID_SIZES.includes(faceEmbedding.length)) {
        return res.status(422).json({ message: `faceEmbedding must be 128, 192, or 512 dimensions` });
    }

    try {
        const student = await prisma.student.findUnique({ where: { uid } });
        if (!student) return res.status(404).json({ message: "Student not found" });

        const storedEmbeddings = Array.isArray(student.faceEmbeddings) ? student.faceEmbeddings : [];
        if (storedEmbeddings.length === 0) {
            return res.status(400).json({ message: "No face embeddings registered. Please register first." });
        }

        const similarity = bestCosineSimilarity(faceEmbedding, storedEmbeddings);
        const verified = similarity >= threshold;

        if (verified) {
            const embeddings = [...storedEmbeddings, faceEmbedding];
            while (embeddings.length > MAX_EMBEDDINGS) embeddings.shift();

            await prisma.student.update({
                where: { uid },
                data: { faceEmbeddings: embeddings },
            });
        }

        createAuditLog({
            userId: student.id,
            action: verified ? "FACE_VERIFY_SUCCESS" : "FACE_VERIFY_FAILED",
            role: "STUDENT",
            details: { similarity: similarity.toFixed(4), threshold, storedCount: storedEmbeddings.length },
            status: verified ? "SUCCESS" : "FAILURE",
            req,
        });

        res.status(200).json({
            verified,
            similarity: parseFloat(similarity.toFixed(4)),
            threshold,
            storedEmbeddings: storedEmbeddings.length,
        });
    } catch (err) {
        return res.status(500).json({ message: "Face verification failed: " + err.message });
    }
}

module.exports = {
    handleFaceRegister,
    handleFaceVerify,
    cosineSimilarity,
    bestCosineSimilarity,
    normalize,
};
