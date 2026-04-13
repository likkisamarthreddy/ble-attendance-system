/**
 * Database Performance Indexes
 * 
 * Run once after `npx prisma db push` to add performance indexes
 * that Prisma schema doesn't natively support (partial indexes, etc.)
 * 
 * Usage: node scripts/add-indexes.js
 */
const { PrismaClient } = require("@prisma/client");
require("dotenv").config();

const prisma = new PrismaClient();

const INDEXES = [
    {
        name: "idx_att_course_active",
        sql: `CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_att_course_active 
              ON attendances (course_id) WHERE session_active = true`,
        description: "Fast lookup of active attendance sessions per course",
    },
    {
        name: "idx_student_uid",
        sql: `CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_student_uid 
              ON students (uid)`,
        description: "Fast student lookup by Firebase UID (already unique, but explicit index helps)",
    },
    {
        name: "idx_course_active",
        sql: `CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_course_active 
              ON courses (is_archived, course_status)`,
        description: "Fast lookup of active, non-archived courses",
    },
    {
        name: "idx_att_students_composite",
        sql: `CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_att_students_composite 
              ON "_AttendanceStudents" ("A", "B")`,
        description: "Speed up attendance-student junction table lookups",
    },
];

async function main() {
    console.log("🗄️  Adding performance indexes...\n");

    for (const index of INDEXES) {
        try {
            await prisma.$executeRawUnsafe(index.sql);
            console.log(`  ✅ ${index.name} — ${index.description}`);
        } catch (err) {
            if (err.message.includes("already exists")) {
                console.log(`  ⏭️  ${index.name} — already exists, skipping`);
            } else {
                console.error(`  ❌ ${index.name} — ${err.message}`);
            }
        }
    }

    console.log("\n✨ Done! Indexes applied.");
}

main()
    .catch(console.error)
    .finally(() => prisma.$disconnect());
