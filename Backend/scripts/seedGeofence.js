/**
 * One-time seed script to set geofence on all existing courses.
 * Center: (26.081154, 91.561969) — computed from 6 boundary points.
 * Radius: 80 meters (covers ~62m max distance + safety buffer).
 *
 * Usage: node scripts/seedGeofence.js
 */

require("dotenv").config({ path: require("path").join(__dirname, "..", ".env") });
const mongoose = require("mongoose");
const { Course } = require("../Model/courseModel");

const GEOFENCE = {
    latitude: 26.081154,
    longitude: 91.561969,
    radiusMeters: 80,
};

async function seed() {
    const mongoUri = process.env.MONGODB_URL || process.env.MONGO_URI;
    if (!mongoUri) {
        console.error("ERROR: No MONGODB_URL or MONGO_URI in .env");
        process.exit(1);
    }

    await mongoose.connect(mongoUri);
    console.log("Connected to MongoDB");

    const result = await Course.updateMany(
        { $or: [{ "geofence.latitude": { $exists: false } }, { "geofence.latitude": null }] },
        { $set: { geofence: GEOFENCE } }
    );

    console.log(`Updated ${result.modifiedCount} courses with geofence:`);
    console.log(`  Center: (${GEOFENCE.latitude}, ${GEOFENCE.longitude})`);
    console.log(`  Radius: ${GEOFENCE.radiusMeters}m`);

    // Show all courses with their geofence
    const courses = await Course.find({}, "name batch joiningCode geofence");
    for (const c of courses) {
        console.log(`  [${c.joiningCode}] ${c.name} (${c.batch}) => lat=${c.geofence?.latitude}, lng=${c.geofence?.longitude}, r=${c.geofence?.radiusMeters}m`);
    }

    await mongoose.disconnect();
    console.log("Done!");
}

seed().catch((err) => {
    console.error("Seed failed:", err);
    process.exit(1);
});
