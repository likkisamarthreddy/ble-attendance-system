const { PrismaClient } = require('@prisma/client');
const prisma = new PrismaClient({
  datasources: {
    db: {
      url: 'postgresql://neondb_owner:npg_eonl5DUd7kVN@ep-ancient-fog-a1s5pere.ap-southeast-1.aws.neon.tech/neondb?sslmode=require'
    }
  }
});

async function clearDb() {
  console.log('Clearing database via DIRECT URL...');
  await prisma.auditLog.deleteMany();
  await prisma.deviceBinding.deleteMany();
  await prisma.attendance.deleteMany();
  await prisma.course.deleteMany();
  await prisma.student.deleteMany();
  await prisma.professor.deleteMany();
  await prisma.admin.deleteMany();
  console.log('Database completely cleared!');
  await prisma.$disconnect();
}

clearDb().catch(e => {
  console.error(e);
  process.exit(1);
});
