const prisma = require("../prisma/client");

async function handleAuthRequest(req, res, next) {
  const uid = req.uid;
  const email = req.email;

  if (!uid) {
    return res.status(422).json({ message: "uid not provided" });
  }

  const { androidId } = req.query;

  try {
    // Lookup by email if available (handles multiple Firebase providers giving different UIDs)
    // Fallback to uid if email is not present in the token
    const query = email ? { email } : { uid };
    const student = await prisma.student.findFirst({ where: query });
    const professor = await prisma.professor.findFirst({ where: query });
    const adminUser = await prisma.admin.findFirst({ where: query });

    if (!professor && !adminUser) {
      if (!student) {
        return res.status(404).json({ message: "User with provided uid not found" });
      }

      const studentEmail = student.email;

      if (!androidId) {
        return res.status(400).json({ message: "No android Id provided" });
      }

      let record = await prisma.deviceBinding.findFirst({
        where: { androidId: androidId },
      });

      if (!record) {
        await prisma.deviceBinding.create({
          data: {
            androidId: androidId,
            email: studentEmail,
          },
        });
        return res.status(200).json({ role: "student" });
      } else {
        if (record.email === studentEmail) {
          return res.status(200).json({ role: "student" });
        } else {
          return res.status(401).json({ message: "The device does not belong to requested account" });
        }
      }
    }

    if (!student && !adminUser) {
      return res.status(200).json({ role: "professor" });
    }

    if (!student && !professor) {
      return res.status(200).json({ role: "admin" });
    }

    if (!student && !professor && !adminUser) {
      return res.status(404).json({ message: "user with provided uid not found" });
    }

    return res.status(500).json({ message: "Unhandled role condition" });
  } catch (err) {
    return res.status(500).json({ message: "Server error: " + err.message });
  }
}

async function handleSimBinding(req, res, next) {
  const uid = req.headers.authorization?.split(" ")[1];
  const SimId = req.headers.simid;
  const AndroidId = req.headers.androidid;

  if (!AndroidId) {
    return res.status(422).json({ message: "AndroidId not provided" });
  }
  if (!SimId) {
    return res.status(422).json({ message: "SimId not provided" });
  }

  try {
    const record = await prisma.deviceBinding.findFirst({
      where: { androidId: AndroidId },
    });

    if (!record) {
      return res.status(404).json({ message: "This device is not binded" });
    }

    if (!record.subscriptionId) {
      await prisma.deviceBinding.update({
        where: { id: record.id },
        data: { subscriptionId: parseInt(SimId) },
      });
      return res.status(200).json({ message: "Sim binded successfully" });
    } else {
      if (record.subscriptionId != parseInt(SimId)) {
        return res.status(403).json({ message: "A different sim is binded with this device" });
      } else {
        return res.status(200).json({ message: "Sim bind verification successful" });
      }
    }
  } catch (err) {
    return res.status(500).json({ message: "Server error: " + err.message });
  }
}

module.exports = {
  handleAuthRequest,
  handleSimBinding,
};
