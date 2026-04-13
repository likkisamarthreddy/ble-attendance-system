require("dotenv").config({ path: __dirname + "/../.env" });
const mongoose = require("mongoose");
const axios = require("axios");
const crypto = require("crypto");
const studentModel = require("../Model/studentModel");
const Course = require("../Model/courseModel");
const Attendance = require("../Model/attendanceModel");

const BASE_URL = "http://localhost:8000/api";
const NUM_STUDENTS = 200; // Simulating 200 concurrent students
const SLOT_DURATION_MS = 7000;
const TOKEN_BYTE_LENGTH = 8;
const BLE_SESSION_SECRET = process.env.BLE_SESSION_SECRET || "fallback_secret";

/**
 * Derives a per-course session secret from the master secret.
 */
function deriveCourseSecret(courseId) {
    const hmac = crypto.createHmac("sha256", BLE_SESSION_SECRET);
    return hmac.update(courseId.toString()).digest("hex");
}

/**
 * Generates an HMAC-SHA256 rolling token.
 */
function generateToken(sessionSecret, timeSlot) {
    const mac = crypto.createHmac("sha256", sessionSecret);
    const slotBytes = Buffer.alloc(8);
    slotBytes.writeBigInt64BE(BigInt(timeSlot));
    const fullHash = mac.update(slotBytes).digest();
    return fullHash.subarray(0, TOKEN_BYTE_LENGTH).toString("hex");
}

// Fixed mock face embedding (128 floats between -1 and 1)
const mockEmbedding = Array.from({ length: 128 }, () => Math.random() * 2 - 1);

async function runStressTest() {
    console.log("🚀 Starting Full System Stress Test...");

    if (!process.env.DATABASE_URL) {
        console.error("❌ DATABASE_URL missing from .env");
        process.exit(1);
    }

    console.log("Connecting to MongoDB to setup test environment...");
    await mongoose.connect(process.env.DATABASE_URL);
    console.log("✅ MongoDB Connected.");

    // 1. Create a mock course
    console.log("📦 Creating mock Course and Session...");
    const course = new Course({
        name: "CS101 Stress Test",
        code: "CS101-STRESS",
        professors: [],
        students: [], // We will fill this
        geofence: { latitude: 37.7749, longitude: -122.4194, radiusMeters: 500 }
    });

    // 2. Create mock students and enroll them
    console.log(`👤 Generating ${NUM_STUDENTS} mock students...`);
    const studentDocs = [];
    for (let i = 0; i < NUM_STUDENTS; i++) {
        studentDocs.push({
            uid: `STRESS_TEST_${i}`,
            email: `student${i}@stresstest.com`,
            name: `Stress Student ${i}`,
            rollNo: `ST${i}`,
            macAddress: `00:11:22:33:44:${i.toString().padStart(2, '0')}`,
            faceEmbeddings: [mockEmbedding]
        });
    }

    // Clean up previous test runs
    await studentModel.deleteMany({ uid: /STRESS_TEST_/ });
    await Course.deleteMany({ code: "CS101-STRESS" });
    await Attendance.deleteMany({ "student.auditLog": /STRESS_TEST/ });

    const insertedStudents = await studentModel.insertMany(studentDocs);
    course.students = insertedStudents.map(s => s._id);
    await course.save();

    // 3. Create active attendance session
    const session = new Attendance({
        course: course._id,
        student: [], // Initially empty
        sessionActive: true,
        sessionStart: new Date(),
        sessionSecret: deriveCourseSecret(course._id)
    });
    await session.save();
    console.log(`✅ Test Environment Ready. Session ID: ${session._id}`);

    // 4. PREPARE THE BOMBARDMENT (Array of Promises)
    console.log(`\n🔥 Initiating ${NUM_STUDENTS} concurrent attendance requests...`);

    const startTime = Date.now();
    const timeSlot = Math.floor(startTime / SLOT_DURATION_MS);
    const validToken = generateToken(session.sessionSecret, timeSlot);

    const requests = insertedStudents.map((student, index) => {
        return axios.post(`${BASE_URL}/student/attendance`, {
            uid: session._id,
            token: validToken,
            latitude: 37.7750, // Within geofence
            longitude: -122.4195,
            faceEmbedding: mockEmbedding,
            deviceId: `device_${index}`,
            integrityToken: "mock_integrity",
            auditLog: [{ event: "STRESS_TEST_FIRE" }]
        }, {
            headers: {
                "Authorization": `Bearer TEST_MOCK_${student.uid}`
            }
        }).then(res => "SUCCESS")
            .catch(err => {
                if (err.response) return `FAIL: ${err.response.status} - ${err.response.data.message || err.response.statusText}`;
                return `FAIL: Network Error - ${err.message}`;
            });
    });

    // 5. FIRE!
    const results = await Promise.all(requests);
    const endTime = Date.now();

    const successes = results.filter(r => r === "SUCCESS").length;
    const failures = results.filter(r => r !== "SUCCESS");

    console.log("\n📊 --- TEST RESULTS ---");
    console.log(`Total Requests: ${NUM_STUDENTS}`);
    console.log(`Time Taken: ${(endTime - startTime) / 1000} seconds`);
    console.log(`Successful Marks: ${successes}`);
    console.log(`Failed Marks: ${failures.length}`);

    if (failures.length > 0) {
        console.log("\nSample Failures:");
        const uniqueFailures = [...new Set(failures)].slice(0, 5);
        uniqueFailures.forEach(f => console.log(f));
    }

    // 6. Verify Database State
    const finalRecord = await Attendance.findById(session._id);
    console.log(`\n💾 Database Validation: ${finalRecord.student.length} students stored in Attendance record.`);

    // Cleanup
    console.log("\n🧹 Cleaning up test data...");
    session.sessionActive = false;
    await session.save();
    process.exit(0);
}

runStressTest().catch(console.error);
