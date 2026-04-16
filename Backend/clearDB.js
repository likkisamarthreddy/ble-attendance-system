const { PrismaClient } = require('@prisma/client');
const prisma = new PrismaClient();

async function clearDB() {
  try {
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
    
    console.log("Successfully cleared all testing data (Admins preserved).");
  } catch (err) {
    console.error("Error clearing DB:", err);
  } finally {
    await prisma.$disconnect();
  }
}

clearDB();
