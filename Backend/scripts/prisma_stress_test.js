require("dotenv").config({ path: __dirname + "/../.env" });
const prisma = require("../prisma/client");
const crypto = require("crypto");

const BASE_URL = "http://localhost:8000/api";
const NUM_STUDENTS = 200; // Simulating 200 concurrent students
const SLOT_DURATION_MS = 3000;
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
    console.log("🚀 Starting Full System Stress Test with Prisma...");

    if (!process.env.DATABASE_URL) {
        console.error("❌ DATABASE_URL missing from .env");
        process.exit(1);
    }

    console.log("✅ Database URL found.");

    // 1. Create a mock course & professor
    console.log("📦 Creating mock Professor, Course and Session...");
    
    // Clean up previous test runs
    await prisma.attendance.deleteMany({ where: { batch: "STRESS-BATCH" } });
    await prisma.course.deleteMany({ where: { code: "CS101-STRESS" } }).catch(()=>null); // if code doesn't exist use joiningCode
    await prisma.course.deleteMany({ where: { joiningCode: "CS101-STRESS" } });
    await prisma.student.deleteMany({ where: { uid: { startsWith: "STRESS_TEST_" } } });
    await prisma.professor.deleteMany({ where: { uid: "STRESS_PROF" } });
    
    const professor = await prisma.professor.create({
        data: {
            name: "Stress Prof",
            email: "prof@stresstest.com",
            uid: "STRESS_PROF",
            isDisabled: false
        }
    });

    const course = await prisma.course.create({
        data: {
            name: "CS101 Stress Test",
            joiningCode: "CS101-STRESS",
            batch: "STRESS-BATCH",
            year: 1,
            professorId: professor.id,
            geofenceLatitude: 37.7749,
            geofenceLongitude: -122.4194,
            geofenceRadiusMeters: 500,
        }
    });

    // 2. Create mock students and enroll them
    console.log(`👤 Generating ${NUM_STUDENTS} mock students...`);
    const studentDocs = [];
    for (let i = 0; i < NUM_STUDENTS; i++) {
        studentDocs.push({
            uid: `STRESS_TEST_${i}`,
            email: `student${i}@stresstest.com`,
            name: `Stress Student ${i}`,
            rollno: 900000 + i, // prevent unique constraint violation
            faceEmbeddings: [mockEmbedding],
            isDisabled: false
        });
    }

    const insertedStudents = await prisma.student.createManyAndReturn({
        data: studentDocs
    });

    // Enroll students to course
    await prisma.course.update({
        where: { id: course.id },
        data: {
            students: {
                connect: insertedStudents.map(s => ({ id: s.id }))
            }
        }
    });

    // 3. Create active attendance session
    const session = await prisma.attendance.create({
        data: {
            courseId: course.id,
            batch: "STRESS-BATCH",
            sessionActive: true,
            sessionStart: new Date(),
        }
    });
    
    const sessionSecret = deriveCourseSecret(course.id);
    console.log(`✅ Test Environment Ready. Session ID: ${session.id}`);

    // 4. PREPARE THE BOMBARDMENT (Array of Promises)
    console.log(`\n🔥 Initiating ${NUM_STUDENTS} concurrent attendance requests...`);

    const startTime = Date.now();
    const serverSlot = Math.floor(startTime / SLOT_DURATION_MS);
    const validToken = generateToken(sessionSecret, serverSlot);

    const requests = insertedStudents.map((student, index) => {
        return fetch(`${BASE_URL}/student/attendance`, {
            method: 'POST',
            headers: {
                "Content-Type": "application/json",
                "Authorization": `Bearer TEST_MOCK_${student.uid}`,
                "X-Forwarded-For": `192.168.1.${(index % 250) + 1}` // Randomish IP to avoid rate limit
            },
            body: JSON.stringify({
                uid: session.id, // Record ID for attendance
                token: validToken,
                latitude: 37.7750, // Within geofence
                longitude: -122.4195,
                faceEmbedding: mockEmbedding,
                deviceId: `device_${index}`,
                integrityToken: "mock_integrity",
                auditLog: [{ event: "STRESS_TEST_FIRE" }]
            })
        }).then(async res => {
            if (!res.ok) {
                const text = await res.text();
                return `FAIL: ${res.status} - ${text}`;
            }
            return "SUCCESS";
        }).catch(err => {
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
    const finalRecord = await prisma.attendance.findUnique({
        where: { id: session.id },
        include: { students: true }
    });
    console.log(`\n💾 Database Validation: ${finalRecord.students.length} students stored in Attendance record.`);

    // Cleanup
    console.log("\n🧹 Cleaning up test data...");
    await prisma.attendance.update({
        where: { id: session.id },
        data: { sessionActive: false, sessionEnd: new Date() }
    });
    
    // uncomment to delete data after run
    // await prisma.attendance.deleteMany({ where: { batch: "STRESS-BATCH" } });
    // await prisma.course.deleteMany({ where: { joiningCode: "CS101-STRESS" } });
    // await prisma.student.deleteMany({ where: { uid: { startsWith: "STRESS_TEST_" } } });
    // await prisma.professor.deleteMany({ where: { uid: "STRESS_PROF" } });

    process.exit(0);
}

runStressTest().catch(console.error);
