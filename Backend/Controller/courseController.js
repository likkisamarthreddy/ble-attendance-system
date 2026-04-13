const prisma = require("../prisma/client");
const { nanoid } = require("nanoid");

// Create course
async function handleCourseCreation(req, res, next) {
  const { name, batch, year, courseExpiry } = req.body;
  const uid = req.uid;

  if (!name || !batch || !year) {
    return res.status(422).json({ message: "Invalid Inputs" });
  }

  try {
    const professor = await prisma.professor.findUnique({ where: { uid } });
    if (!professor) return res.status(404).json({ message: "Professor not found" });

    const joiningCode = nanoid(8);

    const course = await prisma.course.create({
      data: {
        name,
        batch,
        year: parseInt(year),
        professorId: professor.id,
        joiningCode,
        courseExpiry: courseExpiry ? new Date(courseExpiry) : null,
      },
    });

    res.status(200).json({ course });
  } catch (err) {
    return res.status(500).json({ message: "Course creation failed: " + err.message });
  }
}

// Update course students (add/remove)
async function handleCourseStudents(req, res, next) {
  const { courseId, students, action } = req.body;

  if (!courseId || !students || !Array.isArray(students)) {
    return res.status(422).json({ message: "Invalid Inputs" });
  }

  try {
    const course = await prisma.course.findUnique({ where: { id: parseInt(courseId) } });
    if (!course) return res.status(404).json({ message: "Course not found" });

    if (action === "remove") {
      await prisma.course.update({
        where: { id: parseInt(courseId) },
        data: {
          students: {
            disconnect: students.map((sid) => ({ id: parseInt(sid) })),
          },
        },
      });
    } else {
      await prisma.course.update({
        where: { id: parseInt(courseId) },
        data: {
          students: {
            connect: students.map((sid) => ({ id: parseInt(sid) })),
          },
        },
      });
    }

    const updatedCourse = await prisma.course.findUnique({
      where: { id: parseInt(courseId) },
      include: { students: true },
    });

    res.status(200).json({ course: updatedCourse });
  } catch (err) {
    return res.status(500).json({ message: "Updating course students failed: " + err.message });
  }
}

// View students in course
async function handleViewStudentsInCourse(req, res, next) {
  const { courseId, joiningCode, courseName, batch } = req.query;

  if (!courseId && !joiningCode && !courseName) {
    return res.status(422).json({ message: "courseId, joiningCode, or courseName required" });
  }

  try {
    let course;
    if (courseId) {
      course = await prisma.course.findUnique({
        where: { id: parseInt(courseId) },
        include: {
          students: {
            select: { id: true, name: true, rollno: true, email: true, batch: true },
          },
        },
      });
    } else if (joiningCode) {
      course = await prisma.course.findFirst({
        where: { joiningCode },
        include: {
          students: {
            select: { id: true, name: true, rollno: true, email: true, batch: true },
          },
        },
      });
    } else {
      course = await prisma.course.findFirst({
        where: { name: courseName, batch: batch || undefined },
        include: {
          students: {
            select: { id: true, name: true, rollno: true, email: true, batch: true },
          },
        },
      });
    }

    if (!course) return res.status(404).json({ message: "Course not found" });

    res.status(200).json({ students: course.students });
  } catch (err) {
    return res.status(500).json({ message: "Fetching course students failed" });
  }
}

// Update geofence settings
async function handleUpdateGeofence(req, res, next) {
  const { courseId, joiningCode, latitude, longitude, radiusMeters } = req.body;

  if (!courseId && !joiningCode) return res.status(422).json({ message: "courseId or joiningCode required" });

  try {
    let course;
    if (courseId) {
      course = await prisma.course.findUnique({ where: { id: parseInt(courseId) } });
    } else {
      course = await prisma.course.findFirst({ where: { joiningCode } });
    }

    if (!course) return res.status(404).json({ message: "Course not found" });

    const updated = await prisma.course.update({
      where: { id: course.id },
      data: {
        geofenceLatitude: latitude ? parseFloat(latitude) : null,
        geofenceLongitude: longitude ? parseFloat(longitude) : null,
        geofenceRadiusMeters: radiusMeters ? parseFloat(radiusMeters) : 100,
      },
    });

    res.status(200).json({
      message: "Geofence updated",
      course: updated,
      geofence: {
        latitude: updated.geofenceLatitude,
        longitude: updated.geofenceLongitude,
        radiusMeters: updated.geofenceRadiusMeters,
      },
    });
  } catch (err) {
    return res.status(500).json({ message: "Updating geofence failed: " + err.message });
  }
}

module.exports = {
  handleCourseCreation,
  handleCourseStudents,
  handleViewStudentsInCourse,
  handleUpdateGeofence,
};
