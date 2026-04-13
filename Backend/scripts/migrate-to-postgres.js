/**
 * MongoDB → PostgreSQL Data Migration Script
 * 
 * Run this ONCE after setting up PostgreSQL with Prisma:
 *   1. Set DATABASE_URL in .env to your PostgreSQL connection string
 *   2. Run: npx prisma migrate dev --name init
 *   3. Run: node scripts/migrate-to-postgres.js
 * 
 * Prerequisites: npm install mongoose (temporarily, just for migration)
 * 
 * This script reads from MongoDB (MONGODB_URL) and writes to PostgreSQL (DATABASE_URL).
 */

require("dotenv").config();
const mongoose = require("mongoose");
const { PrismaClient } = require("@prisma/client");

const prisma = new PrismaClient();

// ─── MongoDB Schemas (minimal, just for reading) ─────────────────

const adminSchema = new mongoose.Schema({
    name: String, email: String, college: String, uid: String, isDisabled: Boolean,
});
const professorSchema = new mongoose.Schema({
    name: String, email: String, uid: String, isDisabled: Boolean,
    courses: [{ type: mongoose.Schema.Types.ObjectId }],
});
const studentSchema = new mongoose.Schema({
    name: String, rollno: Number, email: String, uid: String,
    batch: [String], faceEmbeddings: [[Number]], faceRegisteredAt: Date, isDisabled: Boolean,
    courses: [{ type: mongoose.Schema.Types.ObjectId }],
});
const courseSchema = new mongoose.Schema({
    name: String, batch: String, year: Number,
    professor: { type: mongoose.Schema.Types.ObjectId },
    students: [{ type: mongoose.Schema.Types.ObjectId }],
    courseStatus: String, courseExpiry: Date, joiningCode: String, sessionSecret: String,
    geofence: { latitude: Number, longitude: Number, radiusMeters: Number },
});
const attendanceSchema = new mongoose.Schema({
    course: { type: mongoose.Schema.Types.ObjectId },
    student: [{ type: mongoose.Schema.Types.ObjectId }],
    date: Date, batch: String, sessionActive: Boolean, sessionStart: Date, sessionEnd: Date,
}, { timestamps: true });
const auditSchema = new mongoose.Schema({
    userId: mongoose.Schema.Types.ObjectId, action: String, role: String,
    courseId: mongoose.Schema.Types.ObjectId, details: mongoose.Schema.Types.Mixed,
    ipAddress: String, userAgent: String, timestamp: Date, status: String,
});
const deviceBindingSchema = new mongoose.Schema({
    androidID: String, email: String, subscriptionId: Number,
});

const MongoAdmin = mongoose.model("adminModel", adminSchema, "admins");
const MongoProfessor = mongoose.model("professorModel", professorSchema, "professors");
const MongoStudent = mongoose.model("studentModel", studentSchema, "students");
const MongoCourse = mongoose.model("courseModel", courseSchema, "courses");
const MongoArchivedCourse = mongoose.model("archieveCourseModel", courseSchema, "archived_courses");
const MongoAttendance = mongoose.model("Attendance", attendanceSchema, "attendances");
const MongoArchivedAttendance = mongoose.model("ArchivedAttendance", attendanceSchema, "archived_attendances");
const MongoAuditLog = mongoose.model("AuditLog", auditSchema, "audit_logs");
const MongoDeviceBinding = mongoose.model("deviceBinding", deviceBindingSchema, "bindings");

// Maps from MongoDB ObjectId → PostgreSQL int ID
const profIdMap = {};
const studentIdMap = {};
const courseIdMap = {};
const attendanceIdMap = {};

async function migrate() {
    console.log("🚀 Starting MongoDB → PostgreSQL migration...\n");

    // Connect to MongoDB
    const mongoUrl = process.env.MONGODB_URL;
    if (!mongoUrl) {
        console.error("❌ MONGODB_URL not set in .env");
        process.exit(1);
    }
    await mongoose.connect(mongoUrl);
    console.log("✅ Connected to MongoDB");

    // Connect to PostgreSQL
    await prisma.$connect();
    console.log("✅ Connected to PostgreSQL\n");

    // ── 1. Migrate Admins ──
    console.log("📦 Migrating admins...");
    const admins = await MongoAdmin.find();
    for (const a of admins) {
        try {
            await prisma.admin.create({
                data: {
                    name: a.name, email: a.email,
                    college: a.college || "Unknown",
                    uid: a.uid, isDisabled: a.isDisabled || false,
                },
            });
            console.log(`  ✅ Admin: ${a.email}`);
        } catch (e) {
            console.log(`  ⚠️ Skip admin ${a.email}: ${e.message}`);
        }
    }

    // ── 2. Migrate Professors ──
    console.log("\n📦 Migrating professors...");
    const professors = await MongoProfessor.find();
    for (const p of professors) {
        try {
            const created = await prisma.professor.create({
                data: {
                    name: p.name, email: p.email,
                    uid: p.uid, isDisabled: p.isDisabled || false,
                },
            });
            profIdMap[p._id.toString()] = created.id;
            console.log(`  ✅ Professor: ${p.email} (${p._id} → ${created.id})`);
        } catch (e) {
            console.log(`  ⚠️ Skip professor ${p.email}: ${e.message}`);
        }
    }

    // ── 3. Migrate Students ──
    console.log("\n📦 Migrating students...");
    const students = await MongoStudent.find();
    for (const s of students) {
        try {
            const created = await prisma.student.create({
                data: {
                    name: s.name, rollno: s.rollno || 0, email: s.email,
                    uid: s.uid, batch: s.batch || [],
                    faceEmbeddings: s.faceEmbeddings && s.faceEmbeddings.length > 0 ? s.faceEmbeddings : [],
                    faceRegisteredAt: s.faceRegisteredAt || null,
                    isDisabled: s.isDisabled || false,
                },
            });
            studentIdMap[s._id.toString()] = created.id;
            console.log(`  ✅ Student: ${s.email} (${s._id} → ${created.id})`);
        } catch (e) {
            console.log(`  ⚠️ Skip student ${s.email}: ${e.message}`);
        }
    }

    // ── 4. Migrate Courses (active + archived) ──
    console.log("\n📦 Migrating courses...");
    const activeCourses = await MongoCourse.find();
    const archivedCourses = await MongoArchivedCourse.find();

    for (const c of [...activeCourses, ...archivedCourses]) {
        const isArchived = archivedCourses.some((ac) => ac._id.toString() === c._id.toString());
        const professorId = profIdMap[c.professor?.toString()];

        if (!professorId) {
            console.log(`  ⚠️ Skip course "${c.name}" — professor not found in map`);
            continue;
        }

        try {
            const studentIds = (c.students || [])
                .map((sid) => studentIdMap[sid.toString()])
                .filter(Boolean)
                .map((id) => ({ id }));

            const created = await prisma.course.create({
                data: {
                    name: c.name, batch: c.batch, year: c.year || 1,
                    professorId: professorId,
                    courseStatus: c.courseStatus || "active",
                    courseExpiry: c.courseExpiry || null,
                    joiningCode: c.joiningCode || "",
                    sessionSecret: c.sessionSecret || null,
                    geofenceLatitude: c.geofence?.latitude || null,
                    geofenceLongitude: c.geofence?.longitude || null,
                    geofenceRadiusMeters: c.geofence?.radiusMeters || 100,
                    isArchived: isArchived,
                    students: { connect: studentIds },
                },
            });
            courseIdMap[c._id.toString()] = created.id;
            console.log(`  ✅ Course: "${c.name}" ${isArchived ? "(archived)" : ""} (${c._id} → ${created.id})`);
        } catch (e) {
            console.log(`  ⚠️ Skip course "${c.name}": ${e.message}`);
        }
    }

    // ── 5. Migrate Attendances (active + archived) ──
    console.log("\n📦 Migrating attendances...");
    const activeAttendances = await MongoAttendance.find();
    const archivedAttendances = await MongoArchivedAttendance.find();

    for (const a of [...activeAttendances, ...archivedAttendances]) {
        const isArchived = archivedAttendances.some((aa) => aa._id.toString() === a._id.toString());
        const courseId = courseIdMap[a.course?.toString()];

        if (!courseId) {
            console.log(`  ⚠️ Skip attendance ${a._id} — course not found in map`);
            continue;
        }

        try {
            const presentStudents = (a.student || [])
                .map((sid) => studentIdMap[sid.toString()])
                .filter(Boolean)
                .map((id) => ({ id }));

            const created = await prisma.attendance.create({
                data: {
                    courseId: courseId,
                    date: a.date || new Date(),
                    batch: a.batch || "",
                    sessionActive: a.sessionActive || false,
                    sessionStart: a.sessionStart || a.date || new Date(),
                    sessionEnd: a.sessionEnd || null,
                    isArchived: isArchived,
                    students: { connect: presentStudents },
                },
            });
            attendanceIdMap[a._id.toString()] = created.id;
            console.log(`  ✅ Attendance: ${a._id} → ${created.id} (${presentStudents.length} students)`);
        } catch (e) {
            console.log(`  ⚠️ Skip attendance ${a._id}: ${e.message}`);
        }
    }

    // ── 6. Migrate Audit Logs ──
    console.log("\n📦 Migrating audit logs...");
    const auditLogs = await MongoAuditLog.find().sort({ timestamp: 1 });
    let auditCount = 0;
    const VALID_ACTIONS = [
        "ATTENDANCE_MARKED", "ATTENDANCE_MANUAL", "ATTENDANCE_MODIFIED",
        "ATTENDANCE_LIVE_MODIFIED", "FACE_REGISTERED", "FACE_VERIFY_SUCCESS",
        "FACE_VERIFY_FAILED", "TOKEN_INVALID", "REPLAY_REJECTED",
        "GEOFENCE_FAILED", "GEOFENCE_MISSING", "TIMING_REJECTED",
        "ENROLLMENT_REJECTED", "CSV_UPLOAD", "STUDENT_CREATED",
        "PROFESSOR_CREATED", "ADMIN_ACTION",
    ];
    const VALID_ROLES = ["STUDENT", "PROFESSOR", "ADMIN"];
    const VALID_STATUSES = ["SUCCESS", "FAILURE", "WARNING"];

    for (const log of auditLogs) {
        if (!VALID_ACTIONS.includes(log.action)) continue;
        if (!VALID_ROLES.includes(log.role)) continue;
        if (!VALID_STATUSES.includes(log.status)) continue;

        const userId = studentIdMap[log.userId?.toString()] || profIdMap[log.userId?.toString()] || 0;
        const courseId = courseIdMap[log.courseId?.toString()] || null;

        try {
            await prisma.auditLog.create({
                data: {
                    userId, action: log.action, role: log.role,
                    courseId, details: log.details || {},
                    ipAddress: log.ipAddress || null,
                    userAgent: log.userAgent || null,
                    timestamp: log.timestamp || new Date(),
                    status: log.status,
                },
            });
            auditCount++;
        } catch (e) {
            // Skip invalid audit logs silently
        }
    }
    console.log(`  ✅ Migrated ${auditCount} audit logs`);

    // ── 7. Migrate Device Bindings ──
    console.log("\n📦 Migrating device bindings...");
    const bindings = await MongoDeviceBinding.find();
    for (const b of bindings) {
        try {
            await prisma.deviceBinding.create({
                data: {
                    androidId: b.androidID,
                    email: b.email,
                    subscriptionId: b.subscriptionId || null,
                },
            });
        } catch (e) {
            // Skip duplicates
        }
    }
    console.log(`  ✅ Migrated ${bindings.length} device bindings`);

    // ── Done ──
    console.log("\n🎉 Migration complete!");
    console.log(`   Admins: ${admins.length}`);
    console.log(`   Professors: ${professors.length}`);
    console.log(`   Students: ${students.length}`);
    console.log(`   Courses: ${Object.keys(courseIdMap).length}`);
    console.log(`   Attendances: ${Object.keys(attendanceIdMap).length}`);
    console.log(`   Audit logs: ${auditCount}`);
    console.log(`   Device bindings: ${bindings.length}`);

    await mongoose.disconnect();
    await prisma.$disconnect();
    process.exit(0);
}

migrate().catch((err) => {
    console.error("❌ Migration failed:", err);
    process.exit(1);
});
