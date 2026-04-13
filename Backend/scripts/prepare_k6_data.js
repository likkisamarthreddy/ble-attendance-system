/**
 * ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
 *  k6 Data Preparation Script
 *  Seeds test students + creates active session
 *  Outputs k6_data.json for the k6 stress test
 * ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
 */

require("dotenv").config({ path: __dirname + "/../.env" });
const prisma = require("../prisma/client");
const crypto = require("crypto");
const fs = require("fs");
const path = require("path");

const NUM_STUDENTS = 500;
const SLOT_DURATION_MS = 3000;
const TOKEN_BYTE_LENGTH = 8;
const BLE_SESSION_SECRET = process.env.BLE_SESSION_SECRET || "fallback_secret";

function deriveCourseSecret(courseId) {
  return crypto.createHmac("sha256", BLE_SESSION_SECRET).update(courseId.toString()).digest("hex");
}

function generateToken(sessionSecret, timeSlot) {
  const mac = crypto.createHmac("sha256", sessionSecret);
  const slotBytes = Buffer.alloc(8);
  slotBytes.writeBigInt64BE(BigInt(timeSlot));
  return mac.update(slotBytes).digest().subarray(0, TOKEN_BYTE_LENGTH).toString("hex");
}

// Generate a realistic-looking 128-float face embedding
function generateFaceEmbedding() {
  return Array.from({ length: 128 }, () => Math.random() * 2 - 1);
}

async function prepareData() {
  console.log("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
  console.log("  🔧 k6 Data Preparation Script");
  console.log("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");

  if (!process.env.DATABASE_URL) {
    console.error("❌ DATABASE_URL missing from .env");
    process.exit(1);
  }

  // --- 1. Cleanup previous test data ---
  console.log("🧹 Cleaning up previous test data...");
  try {
    // Must disconnect attendance-student relations first
    const oldAttendances = await prisma.attendance.findMany({
      where: { batch: "K6-STRESS-BATCH" },
      select: { id: true }
    });
    for (const att of oldAttendances) {
      await prisma.attendance.update({
        where: { id: att.id },
        data: { students: { set: [] } }
      });
    }
    await prisma.attendance.deleteMany({ where: { batch: "K6-STRESS-BATCH" } });

    // Disconnect students from courses
    const oldCourses = await prisma.course.findMany({
      where: { joiningCode: "K6-STRESS-CODE" },
      select: { id: true }
    });
    for (const c of oldCourses) {
      await prisma.course.update({
        where: { id: c.id },
        data: { students: { set: [] } }
      });
    }
    await prisma.course.deleteMany({ where: { joiningCode: "K6-STRESS-CODE" } });
    await prisma.student.deleteMany({ where: { uid: { startsWith: "K6_STUDENT_" } } });
    await prisma.professor.deleteMany({ where: { uid: "K6_STRESS_PROF" } });
    console.log("✅ Cleanup complete.\n");
  } catch (err) {
    console.log("⚠️  Cleanup partial (first run?): ", err.message, "\n");
  }

  // --- 2. Create professor ---
  console.log("👨‍🏫 Creating test professor...");
  const professor = await prisma.professor.create({
    data: {
      name: "k6 Stress Professor",
      email: "k6prof@stresstest.com",
      uid: "K6_STRESS_PROF",
      isDisabled: false,
    }
  });

  // --- 3. Create course ---
  console.log("📚 Creating test course...");
  const course = await prisma.course.create({
    data: {
      name: "Advanced k6 Stress Test",
      joiningCode: "K6-STRESS-CODE",
      batch: "K6-STRESS-BATCH",
      year: 2026,
      professorId: professor.id,
      geofenceLatitude: 17.3850,
      geofenceLongitude: 78.4867,
      geofenceRadiusMeters: 500,
    }
  });

  // --- 4. Create students ---
  console.log(`👤 Creating ${NUM_STUDENTS} test students...`);
  const studentData = [];
  for (let i = 0; i < NUM_STUDENTS; i++) {
    const embedding = generateFaceEmbedding();
    studentData.push({
      uid: `K6_STUDENT_${i}`,
      email: `k6student${i}@stresstest.com`,
      name: `K6 Student ${i}`,
      rollno: 800000 + i,
      faceEmbeddings: [embedding],
      isDisabled: false,
    });
  }
  const insertedStudents = await prisma.student.createManyAndReturn({ data: studentData });
  console.log(`✅ Created ${insertedStudents.length} students.\n`);

  // --- 5. Enroll students in course ---
  console.log("📝 Enrolling students in course...");
  await prisma.course.update({
    where: { id: course.id },
    data: { students: { connect: insertedStudents.map(s => ({ id: s.id })) } }
  });

  // --- 6. Create active attendance session ---
  console.log("🎯 Creating active attendance session...");
  const session = await prisma.attendance.create({
    data: {
      courseId: course.id,
      batch: "K6-STRESS-BATCH",
      sessionActive: true,
      sessionStart: new Date(),
    }
  });

  // --- 7. Generate tokens and export ---
  const sessionSecret = deriveCourseSecret(course.id);
  const serverSlot = Math.floor(Date.now() / SLOT_DURATION_MS);
  const token = generateToken(sessionSecret, serverSlot);

  // Build the k6 data file
  const k6Data = {
    metadata: {
      generatedAt: new Date().toISOString(),
      numStudents: NUM_STUDENTS,
      courseId: course.id,
      courseName: course.name,
      sessionId: session.id,
      sessionSecret: sessionSecret,
      professorUid: professor.uid,
    },
    session: {
      recordId: session.id,
      token: token,
      tokenSlot: serverSlot,
      slotDurationMs: SLOT_DURATION_MS,
    },
    geofence: {
      latitude: course.geofenceLatitude,
      longitude: course.geofenceLongitude,
      radiusMeters: course.geofenceRadiusMeters,
    },
    students: insertedStudents.map((s, i) => ({
      uid: s.uid,
      id: s.id,
      rollno: s.rollno,
      authToken: `TEST_MOCK_${s.uid}`,
      faceEmbedding: studentData[i].faceEmbeddings[0],
    })),
  };

  const outputPath = path.join(__dirname, "k6_data.json");
  fs.writeFileSync(outputPath, JSON.stringify(k6Data, null, 2));
  console.log(`\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━`);
  console.log(`  ✅ Data prepared and written to: ${outputPath}`);
  console.log(`  📊 Students: ${NUM_STUDENTS}`);
  console.log(`  🎯 Session ID: ${session.id}`);
  console.log(`  🔑 Token: ${token}`);
  console.log(`━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n`);

  await prisma.$disconnect();
  process.exit(0);
}

prepareData().catch(err => {
  console.error("❌ Fatal error:", err);
  process.exit(1);
});
