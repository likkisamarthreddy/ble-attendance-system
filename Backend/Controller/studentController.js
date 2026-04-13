const prisma = require("../prisma/client");

// Register the student
async function handleStudentRegistration(req, res, next) {
  const { name, rollno, email } = req.body;
  const uid = req.firebaseUser?.uid || req.headers.authorization?.split(" ")[1];

  if (!name || !rollno || !email || !uid) {
    return res.status(422).json({ message: "Invalid inputs" });
  }

  try {
    const student = await prisma.student.create({
      data: { name, rollno: parseInt(rollno), email, uid },
    });
    return res.status(200).json({ user: student });
  } catch (err) {
    return res.status(500).json({ message: "Registration failed, please try again." });
  }
}

// Get the Student Profile
async function handleStudentProfile(req, res, next) {
  const uid = req.uid;

  try {
    const student = await prisma.student.findUnique({
      where: { uid },
      include: { courses: { where: { isArchived: false } } },
    });

    if (!student) return res.status(404).json({ message: "No student Found" });

    const hasRegisteredFace = Array.isArray(student.faceEmbeddings) && student.faceEmbeddings.length > 0;

    const { faceEmbeddings, ...studentObj } = student;

    console.log(`[PROFILE DEBUG] uid=${uid}, faceEmbeddings length=${Array.isArray(faceEmbeddings) ? faceEmbeddings.length : 0}, hasRegisteredFace=${hasRegisteredFace}`);

    res.status(200).json({ ...studentObj, hasRegisteredFace });
  } catch (err) {
    return res.status(500).json({ message: "Server error in finding student profile" });
  }
}

// Find all courses a student is enrolled in
async function handleStudentViewCourse(req, res, next) {
  const uid = req.uid;

  try {
    const student = await prisma.student.findUnique({
      where: { uid },
      include: { courses: { where: { isArchived: false } } },
    });

    if (!student) return res.status(404).json({ message: "Could not find the provided student" });

    res.json({ courses: student.courses });
  } catch (err) {
    return res.status(500).json({ message: "Fetching student failed, please try again later." });
  }
}

// View attendance of student
async function handleStudentViewAttendance(req, res, next) {
  const { courseName, batch } = req.query;
  const uid = req.uid;

  if (!batch || !courseName) {
    return res.status(422).json({ message: "Invalid Inputs" });
  }

  try {
    const student = await prisma.student.findUnique({ where: { uid } });
    if (!student) return res.status(404).json({ message: "Could not find student" });

    const course = await prisma.course.findFirst({
      where: { name: courseName, batch },
    });
    if (!course) return res.status(404).json({ message: "Could not find course" });

    const attendanceRecords = await prisma.attendance.findMany({
      where: { courseId: course.id },
      include: { students: { select: { id: true } } },
    });

    const attendanceStatus = attendanceRecords.map((record) => ({
      date: record.date.toISOString().split("T")[0],
      status: record.students.some((s) => s.id === student.id) ? "Present" : "Absent",
    }));

    res.status(200).json({ attendance: attendanceStatus });
  } catch (err) {
    return res.status(500).json({ message: "Failed to fetch attendance records" });
  }
}

// Join course
async function handleStudentJoinCourse(req, res, next) {
  const uid = req.uid;
  const { joiningCode } = req.body;

  try {
    const course = await prisma.course.findFirst({
      where: { joiningCode },
      include: { students: { select: { id: true } } },
    });
    if (!course) return res.status(404).json({ message: "Course not found for provided joining Code" });

    const student = await prisma.student.findUnique({ where: { uid } });
    if (!student) return res.status(404).json({ message: "Student not found" });

    if (course.students.some((s) => s.id === student.id)) {
      return res.status(208).json({ message: "Student is already part of the course" });
    }

    await prisma.course.update({
      where: { id: course.id },
      data: {
        students: { connect: { id: student.id } },
      },
    });

    // Also update student's courses
    await prisma.student.update({
      where: { id: student.id },
      data: {
        courses: { connect: { id: course.id } },
      },
    });

    res.status(200).json({ message: "Successfully added student to course" });
  } catch (err) {
    return res.status(500).json({ message: "Server error Adding student to course, please try again later" });
  }
}

// Get all courses with attendance percentage per course
async function handleStudentCoursesWithAttendance(req, res, next) {
  const uid = req.uid;

  try {
    const student = await prisma.student.findUnique({
      where: { uid },
      include: { courses: { where: { isArchived: false } } },
    });

    if (!student) return res.status(404).json({ message: "Could not find student" });

    const coursesWithAttendance = [];

    for (const course of student.courses) {
      try {
        const records = await prisma.attendance.findMany({
          where: { courseId: course.id },
          include: { students: { select: { id: true } } },
        });

        const totalClasses = records.length;
        let presentCount = 0;

        for (const record of records) {
          if (record.students.some((s) => s.id === student.id)) {
            presentCount++;
          }
        }

        const absentCount = totalClasses - presentCount;
        const percentage = totalClasses > 0 ? Math.round((presentCount / totalClasses) * 100) : 0;

        coursesWithAttendance.push({
          name: course.name,
          batch: course.batch,
          _id: course.id,
          id: course.id,
          totalClasses,
          presentCount,
          absentCount,
          percentage,
        });
      } catch (err) {
        console.error(`Error fetching attendance for course ${course.name}:`, err.message);
      }
    }

    res.status(200).json({ courses: coursesWithAttendance });
  } catch (err) {
    return res.status(500).json({ message: "Fetching student failed" });
  }
}

module.exports = {
  handleStudentViewCourse,
  handleStudentViewAttendance,
  handleStudentRegistration,
  handleStudentJoinCourse,
  handleStudentProfile,
  handleStudentCoursesWithAttendance,
};
