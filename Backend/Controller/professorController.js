const prisma = require("../prisma/client");

// Professor Registration
async function handleProfessorRegistration(req, res, next) {
  const { name, email } = req.body;
  const uid = req.headers.authorization.split(" ")[1];

  if (!name || !email || !uid) {
    return res.status(422).json({ message: "All fields are required" });
  }

  try {
    const professor = await prisma.professor.create({
      data: { name, email, uid },
    });
    res.status(200).json({ user: professor });
  } catch (err) {
    return res.status(500).json({ message: "Could not register professor, please try again later" });
  }
}

// View courses created by professor
async function handleViewCoursesByProfessor(req, res, next) {
  const uid = req.uid;
  if (!uid) return res.status(400).json({ message: "not provided uid" });

  try {
    const professor = await prisma.professor.findUnique({
      where: { uid },
      include: {
        courses: { where: { isArchived: false } },
      },
    });

    if (!professor) return res.status(404).json({ message: "Could not find professor" });

    res.json({ courses: professor.courses });
  } catch (err) {
    return res.status(500).json({ message: "Fetching professor failed, please try again later." });
  }
}

// View archived courses by professor
async function handleViewArchivedCoursesByProfessor(req, res, next) {
  const uid = req.uid;
  if (!uid) return res.status(400).json({ message: "Uid not provided" });

  try {
    const professor = await prisma.professor.findUnique({ where: { uid } });
    if (!professor) return res.status(404).json({ message: "Professor not found" });

    const courses = await prisma.course.findMany({
      where: { professorId: professor.id, isArchived: true },
    });

    res.json({ courses });
  } catch (err) {
    return res.status(500).json({ message: "Fetching archived courses failed, please try later" });
  }
}

// View all students
async function handleViewAllStudents(req, res, next) {
  try {
    const students = await prisma.student.findMany({
      select: {
        id: true, name: true, rollno: true, email: true,
        uid: true, batch: true, isDisabled: true, createdAt: true,
        faceRegisteredAt: true,
      },
    });
    res.status(200).json({ student: students });
  } catch (err) {
    return res.status(500).json({ message: "Cannot fetch Students" });
  }
}

// Fetch all attendance records for a course
async function handleViewAllAttendanceRecords(req, res, next) {
  const { courseId, joiningCode, courseName, batch, year, isArchived } = req.query;

  try {
    let course;
    if (courseId) {
      course = await prisma.course.findUnique({ where: { id: parseInt(courseId) } });
    } else {
      if (!courseName || !batch || !year || !joiningCode) {
        return res.status(422).json({ message: "Invalid Inputs" });
      }
      const isArchivedBool = isArchived === "true";
      course = await prisma.course.findFirst({
        where: {
          name: courseName,
          batch: batch,
          year: parseInt(year),
          joiningCode: joiningCode,
          isArchived: isArchivedBool,
        },
      });
    }

    if (!course) return res.status(404).json({ message: "Could not find course" });

    const records = await prisma.attendance.findMany({
      where: { courseId: course.id },
      include: { students: { select: { id: true } } },
    });

    // Transform to match old API format (student array of IDs)
    const formattedRecords = records.map((r) => ({
      id: r.id,
      _id: r.id.toString(),
      course: r.courseId,
      student: r.students.map((s) => s.id),
      date: r.date,
      batch: r.batch,
      sessionActive: r.sessionActive,
      sessionStart: r.sessionStart,
      sessionEnd: r.sessionEnd,
    }));

    res.json({ records: formattedRecords });
  } catch (err) {
    return res.status(500).json({ message: "Cannot fetch course, try later" });
  }
}

// View record data (per-session attendance)
async function handleViewRecordData(req, res, next) {
  const { recordId } = req.query;
  if (!recordId) return res.status(422).json({ message: "Invalid Inputs" });

  try {
    const record = await prisma.attendance.findUnique({
      where: { id: parseInt(recordId) },
      include: { students: { select: { id: true } } },
    });

    if (!record) return res.status(404).json({ message: "No attendance records found for this course" });

    const course = await prisma.course.findUnique({
      where: { id: record.courseId },
      include: { students: { select: { id: true, rollno: true, name: true } } },
    });

    if (!course) return res.status(404).json({ message: "Could not find course" });

    const presentIds = new Set(record.students.map((s) => s.id));
    const attendanceStatus = course.students.map((student) => ({
      rollno: student.rollno,
      name: student.name,
      status: presentIds.has(student.id) ? "Present" : "Absent",
    }));

    res.json({ attendance: attendanceStatus });
  } catch (err) {
    return res.status(500).json({ message: "Failed to fetch attendance records" });
  }
}

// Complete attendance record for excel
async function handleProfessorViewAttendance(req, res, next) {
  const { courseId, courseName, batch, year, isArchived, joiningCode } = req.query;

  try {
    let course;
    if (courseId) {
      course = await prisma.course.findUnique({
        where: { id: parseInt(courseId) },
        include: { students: true },
      });
    } else {
      if (isArchived === undefined || isArchived === null || joiningCode == null) {
        return res.status(400).json({ message: "Info for archive is not provided" });
      }
      course = await prisma.course.findFirst({
        where: {
          name: courseName,
          batch: batch,
          year: parseInt(year),
          joiningCode: joiningCode,
          isArchived: isArchived === "true",
        },
        include: { students: true },
      });
    }

    if (!course) return res.status(404).json({ message: "Could not find course" });

    const students = course.students;
    if (!students || students.length === 0) {
      return res.status(404).json({ message: "No students found" });
    }

    const attendanceRecords = await prisma.attendance.findMany({
      where: { courseId: course.id },
      include: { students: { select: { id: true } } },
    });

    if (!attendanceRecords || attendanceRecords.length === 0) {
      return res.status(404).json({ message: "No attendance records found for this course" });
    }

    const attendanceSheet = [];
    attendanceRecords.forEach((record) => {
      const dateStr = record.date.toISOString().split("T")[0];
      const presentIds = new Set(record.students.map((s) => s.id));

      let recordEntry = { date: dateStr, attendance: {} };
      students.forEach((student) => {
        recordEntry.attendance[student.rollno] = presentIds.has(student.id) ? "Present" : "Absent";
      });
      attendanceSheet.push(recordEntry);
    });

    res.status(200).json({ attendanceSheet });
  } catch (err) {
    return res.status(500).json({ message: "Cannot fetch course, try later" });
  }
}

// Professor profile
async function handleProfessorProfile(req, res, next) {
  const uid = req.uid;
  if (!uid) return res.status(400).json({ message: "No uid provided" });

  try {
    const professor = await prisma.professor.findUnique({
      where: { uid },
      include: { courses: { where: { isArchived: false } } },
    });

    if (!professor) return res.status(404).json({ message: "No professor found" });

    return res.status(200).json(professor);
  } catch (err) {
    return res.status(500).json({ message: "Server error in finding profile" });
  }
}

module.exports = {
  handleProfessorRegistration,
  handleViewCoursesByProfessor,
  handleProfessorViewAttendance,
  handleViewAllStudents,
  handleViewAllAttendanceRecords,
  handleViewRecordData,
  handleViewArchivedCoursesByProfessor,
  handleProfessorProfile,
};
