/**
 * One-time migration: Clear all face embeddings so students
 * re-register with the new multi-embedding system.
 */
require('dotenv').config({ path: require('path').join(__dirname, '..', '.env') });
const mongoose = require('mongoose');
const Student = require('../Model/studentModel');

async function main() {
    const mongoUri = process.env.DATABASE_URL;
    if (!mongoUri) {
        console.error('DATABASE_URL not set');
        process.exit(1);
    }

    await mongoose.connect(mongoUri);
    console.log('Connected to database');

    const result = await Student.updateMany(
        {},
        { $set: { faceEmbeddings: [], faceEmbedding: undefined }, $unset: { faceEmbedding: "" } }
    );

    console.log(`Cleared face embeddings for ${result.modifiedCount} student(s)`);
    await mongoose.disconnect();
    process.exit(0);
}

main().catch(e => { console.error(e); process.exit(1); });
