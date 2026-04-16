require('dotenv').config();
const { PrismaClient } = require('@prisma/client');

// Use the explicit DIRECT_URL from .env to bypass connection pooler timeouts
const directUrl = process.env.DIRECT_URL;

if (!directUrl) {
  console.error("No DIRECT_URL found in .env file. Please check your .env");
  process.exit(1);
}

const prisma = new PrismaClient({
  datasources: {
    db: {
      url: directUrl,
    },
  },
});

async function clearDB() {
  try {
    console.log("Connecting directly to bypass pooler timeouts...");
    
    console.log("Clearing DeviceBinding...");
    await prisma.deviceBinding.deleteMany({});
    
    console.log("Clearing AuditLog...");
    await prisma.auditLog.deleteMany({});
    
    console.log("Clearing Attendance...");
    await prisma.attendance.deleteMany({});
    
    console.log("Clearing Course...");
    await prisma.course.deleteMany({});
    
    console.log("Clearing Student...");
    await prisma.student.deleteMany({});
    
    console.log("Clearing Professor...");
    await prisma.professor.deleteMany({});
    
    console.log("\nSuccess! Cleared all testing data.");
  } catch (err) {
    console.error("\nError clearing DB:", err);
  } finally {
    await prisma.$disconnect();
  }
}

clearDB();
