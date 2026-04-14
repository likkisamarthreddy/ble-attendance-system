const fs = require("fs");
const csv = require("csv-parser");
const prisma = require("../prisma/client");
const { createAuditLog } = require("../helpers/auditLog");
const firebaseadmin = require("firebase-admin");

// Register students (or professors) in bulk from a CSV file
async function handleStudentRegistrationUsingCsv(req, res, next) {
  const isProfessor = req.headers.isprofessor === "true";

  if (!req.file) {
    return res.status(400).json({ message: "No CSV file uploaded" });
  }

  const errors = [];

  const rows = await new Promise((resolve, reject) => {
    const data = [];
    fs.createReadStream(req.file.path)
      .pipe(csv())
      .on("data", (row) => data.push(row))
      .on("end", () => resolve(data))
      .on("error", (err) => reject(err));
  });

  let successCount = 0;

  for (const row of rows) {
    const { name, email, rollno, password } = row;

    if (!name || !email) {
      errors.push({ email: email || "unknown", error: "Missing name or email" });
      continue;
    }

    try {
      let userRecord;
      try {
        userRecord = await firebaseadmin.auth().createUser({
          email: email,
          password: password || "Default@123",
          displayName: name,
        });
      } catch (firebaseErr) {
        if (firebaseErr.code === "auth/email-already-exists") {
          try {
            userRecord = await firebaseadmin.auth().getUserByEmail(email);
          } catch (fetchErr) {
            errors.push({ email, error: `Firebase error: ${firebaseErr.message}` });
            continue;
          }
        } else {
          errors.push({ email, error: `Firebase error: ${firebaseErr.message}` });
          continue;
        }
      }

      if (isProfessor) {
        const existing = await prisma.professor.findUnique({ where: { email } });
        if (existing) {
          errors.push({ email, error: "Professor already registered" });
          continue;
        }
        await prisma.professor.create({
          data: { name, email, uid: userRecord.uid },
        });
      } else {
        const existing = await prisma.student.findUnique({ where: { email } });
        if (existing) {
          errors.push({ email, error: "Student already registered" });
          continue;
        }
        await prisma.student.create({
          data: {
            name,
            email,
            rollno: parseInt(rollno) || 0,
            uid: userRecord.uid,
          },
        });
      }
      successCount++;
    } catch (err) {
      errors.push({ email, error: err.message });
    }
  }

  try { fs.unlinkSync(req.file.path); } catch (e) { }

  createAuditLog({
    userId: req.user ? req.user.id : 0,
    action: "CSV_UPLOAD",
    role: "ADMIN",
    details: { type: isProfessor ? "professor" : "student", total: rows.length, success: successCount, errorCount: errors.length },
    status: successCount > 0 ? "SUCCESS" : "FAILURE",
    req,
  });

  res.status(200).json({
    message: `${successCount} ${isProfessor ? "professors" : "students"} registered successfully`,
    total: rows.length,
    success: successCount,
    errors,
  });
}

// Create a single student or professor account manually
async function handleCreateStudentAccount(req, res, next) {
  const { name, rollno, email, password, isProfessor } = req.body;

  if (!name || !email || !password) {
    return res.status(422).json({ message: "Missing required fields (name, email, password)" });
  }

  try {
    // 1. Check if user already exists in DB to prevent Firebase orphans
    if (isProfessor) {
      const existing = await prisma.professor.findUnique({ where: { email } });
      if (existing) return res.status(409).json({ message: "Professor already registered with this email" });
    } else {
      const existing = await prisma.student.findUnique({ where: { email } });
      if (existing) return res.status(409).json({ message: "Student already registered with this email" });
    }

    let userRecord;
    try {
      userRecord = await firebaseadmin.auth().createUser({
        email: email,
        password: password,
        displayName: name,
      });
    } catch (firebaseErr) {
      if (firebaseErr.code === "auth/email-already-exists") {
        try {
          userRecord = await firebaseadmin.auth().getUserByEmail(email);
        } catch (fetchErr) {
          return res.status(500).json({ message: `Firebase error: ${firebaseErr.message}` });
        }
      } else {
        return res.status(500).json({ message: `Firebase error: ${firebaseErr.message}` });
      }
    }

    const uid = userRecord.uid;

    if (isProfessor) {
      const prof = await prisma.professor.create({
        data: { name, email, uid },
      });
      createAuditLog({
        userId: req.user ? req.user.id : 0,
        action: "PROFESSOR_CREATED",
        role: "ADMIN",
        details: { createdUid: uid, email, method: "Manual" },
        status: "SUCCESS",
        req,
      });
      return res.status(200).json(prof);
    }

    const student = await prisma.student.create({
      data: { name, email, rollno: parseInt(rollno) || 0, uid },
    });

    createAuditLog({
      userId: req.user ? req.user.id : 0,
      action: "STUDENT_CREATED",
      role: "ADMIN",
      details: { createdUid: uid, email, method: "Manual" },
      status: "SUCCESS",
      req,
    });

    return res.status(200).json(student);
  } catch (err) {
    return res.status(500).json({ message: "Registration failed: " + err.message });
  }
}

// View all current (active) courses
async function handleViewCurrentCoursesByAdmin(req, res, next) {
  try {
    const courses = await prisma.course.findMany({
      where: { isArchived: false },
      include: { professor: true },
    });
    // Flatten professor object to email string for Android compatibility
    const flatCourses = courses.map(c => ({
      ...c,
      professor: c.professor ? c.professor.email : "Unknown",
    }));
    res.status(200).json({ courses: flatCourses });
  } catch (err) {
    return res.status(500).json({ message: "Fetching courses failed" });
  }
}

// View all archived courses
async function handleViewArchiveCoursesByAdmin(req, res, next) {
  try {
    const courses = await prisma.course.findMany({
      where: { isArchived: true },
      include: { professor: true },
    });
    // Flatten professor object to email string for Android compatibility
    const flatCourses = courses.map(c => ({
      ...c,
      professor: c.professor ? c.professor.email : "Unknown",
    }));
    res.status(200).json({ courses: flatCourses });
  } catch (err) {
    return res.status(500).json({ message: "Fetching archived courses failed" });
  }
}

// View all professors
async function handleViewAllProfessor(req, res, next) {
  try {
    const professors = await prisma.professor.findMany();
    res.status(200).json({ professor: professors });
  } catch (err) {
    return res.status(500).json({ message: "Fetching professors failed" });
  }
}

// View a student's attendance across all courses
async function handleViewStudentAttendance(req, res, next) {
  const { name, rollno } = req.query;

  if (!name && !rollno) {
    return res.status(422).json({ message: "Provide student name or rollno" });
  }

  try {
    const where = {};
    if (name) where.name = { contains: name, mode: "insensitive" };
    if (rollno) where.rollno = parseInt(rollno);

    const student = await prisma.student.findFirst({
      where,
      include: { courses: true },
    });

    if (!student) {
      return res.status(404).json({ message: "Student not found" });
    }

    const attendanceData = [];

    for (const course of student.courses) {
      const allRecords = await prisma.attendance.findMany({
        where: { courseId: course.id },
        include: { students: { select: { id: true } } },
      });

      const totalTaken = allRecords.length;
      let presentCount = 0;

      for (const record of allRecords) {
        if (record.students.some((s) => s.id === student.id)) {
          presentCount++;
        }
      }

      attendanceData.push({
        course: course.name,
        batch: course.batch,
        courseYear: course.year,
        totalTaken,
        presentCount,
      });
    }

    res.status(200).json({ attendanceData });
  } catch (err) {
    return res.status(500).json({ message: "Fetching student attendance failed: " + err.message });
  }
}

// Remove a student's registered face
async function handleRemoveStudentFace(req, res, next) {
  const { id } = req.params;

  if (!id) {
    return res.status(400).json({ message: "Student ID is required" });
  }

  try {
    const student = await prisma.student.findUnique({ where: { id: parseInt(id) } });
    if (!student) {
      return res.status(404).json({ message: "Student not found" });
    }

    await prisma.student.update({
      where: { id: parseInt(id) },
      data: {
        faceEmbeddings: "[]",
        faceRegisteredAt: null
      }
    });

    createAuditLog({
      userId: req.user ? req.user.id : 0,
      action: "ADMIN_ACTION",
      role: "ADMIN",
      details: { targetStudent: student.email, action: "Remove Face" },
      status: "SUCCESS",
      req,
    });

    return res.status(200).json({ message: "Face profile removed successfully" });
  } catch (err) {
    return res.status(500).json({ message: "Failed to remove face profile: " + err.message });
  }
}

// Delete a student entirely (DB + Firebase Auth)
async function handleDeleteStudent(req, res, next) {
  const { id } = req.params;

  if (!id) {
    return res.status(400).json({ message: "Student ID is required" });
  }

  try {
    const student = await prisma.student.findUnique({
      where: { id: parseInt(id) },
      include: { courses: { select: { id: true } }, attendances: { select: { id: true } } },
    });
    if (!student) {
      return res.status(404).json({ message: "Student not found" });
    }

    // Disconnect student from all courses
    if (student.courses.length > 0) {
      await prisma.student.update({
        where: { id: parseInt(id) },
        data: {
          courses: { disconnect: student.courses.map((c) => ({ id: c.id })) },
          attendances: { disconnect: student.attendances.map((a) => ({ id: a.id })) },
        },
      });
    }

    // Delete from DB
    await prisma.student.delete({ where: { id: parseInt(id) } });

    // Delete from Firebase Auth
    try {
      await firebaseadmin.auth().deleteUser(student.uid);
    } catch (fbErr) {
      console.error("Firebase deletion failed (non-blocking):", fbErr.message);
    }

    createAuditLog({
      userId: req.user ? req.user.id : 0,
      action: "ADMIN_ACTION",
      role: "ADMIN",
      details: { targetStudent: student.email, action: "Delete Student" },
      status: "SUCCESS",
      req,
    });

    return res.status(200).json({ message: "Student deleted successfully" });
  } catch (err) {
    return res.status(500).json({ message: "Failed to delete student: " + err.message });
  }
}

// Get all students with overall attendance percentage
async function handleGetAllStudentsDetailed(req, res, next) {
  try {
    const students = await prisma.student.findMany({
      include: {
        courses: {
          where: { isArchived: false },
          select: { id: true },
        },
      },
      orderBy: { rollno: "asc" },
    });

    // Gather all active attendance records
    const allRecords = await prisma.attendance.findMany({
      where: { isArchived: false },
      include: { students: { select: { id: true } } },
    });

    const studentsDetailed = students.map((student) => {
      // Count how many records this student was present in (across all their courses)
      const courseIds = student.courses.map((c) => c.id);
      const relevantRecords = allRecords.filter((r) => courseIds.includes(r.courseId));
      const totalClasses = relevantRecords.length;
      let present = 0;
      for (const record of relevantRecords) {
        if (record.students.some((s) => s.id === student.id)) {
          present++;
        }
      }
      const attendancePercentage = totalClasses > 0 ? Math.round((present / totalClasses) * 100) : null;

      const hasRegisteredFace = Array.isArray(student.faceEmbeddings) && student.faceEmbeddings.length > 0;

      // Strip faceEmbeddings from response (too large)
      const { faceEmbeddings, ...studentObj } = student;

      return {
        ...studentObj,
        attendancePercentage,
        totalClasses,
        presentCount: present,
        hasRegisteredFace,
      };
    });

    res.status(200).json({ students: studentsDetailed });
  } catch (err) {
    return res.status(500).json({ message: "Failed to fetch students: " + err.message });
  }
}


module.exports = {
  handleStudentRegistrationUsingCsv,
  handleCreateStudentAccount,
  handleViewCurrentCoursesByAdmin,
  handleViewArchiveCoursesByAdmin,
  handleViewAllProfessor,
  handleViewStudentAttendance,
  handleRemoveStudentFace,
  handleDeleteStudent,
  handleGetAllStudentsDetailed,
};
