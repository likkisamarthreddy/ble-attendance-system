const express = require("express");
const cors = require("cors");
const rateLimit = require("express-rate-limit");
const cron = require("node-cron");
require('dotenv').config();
const admin = require("firebase-admin");

// ─── Prometheus Metrics ──────────────────────────────────────────
const { metricsMiddleware, promClient } = require("./Middleware/metricsMiddleware");

// Initialize Firebase Admin SDK
let serviceAccount;
if (process.env.FIREBASE_KEY) {
  try {
    serviceAccount = typeof process.env.FIREBASE_KEY === 'string' 
      ? JSON.parse(process.env.FIREBASE_KEY) 
      : process.env.FIREBASE_KEY;
  } catch (error) {
    console.error("❌ CRITICAL: Failed to parse FIREBASE_KEY environment variable as JSON. Make sure you pasted the exact JSON object without extra quotes.");
    process.exit(1);
  }
} else if (process.env.FIREBASE_KEY_PATH) {
  try {
    serviceAccount = require(process.env.FIREBASE_KEY_PATH);
  } catch (err) {
    console.error(`❌ CRITICAL: Missing Firebase credentials! Could not find the file at FIREBASE_KEY_PATH (${process.env.FIREBASE_KEY_PATH}).`);
    console.error("👉 FIX: You must either add FIREBASE_KEY in Railway Variables with the JSON content, OR add the serviceAccountKey.json file.");
    process.exit(1);
  }
} else {
  console.error("❌ CRITICAL: No FIREBASE_KEY or FIREBASE_KEY_PATH configured. The backend cannot start.");
  process.exit(1);
}

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
});

const connectDatabase = require("./connect");
const prisma = require("./prisma/client");

const adminRouter = require("./Routes/admin");
const professorRouter = require("./Routes/professor");
const studentRouter = require("./Routes/student");
const authRouter = require("./Routes/auth");

const app = express();
app.set('trust proxy', 1);

connectDatabase().then(() => {
  console.log("Database connected");
});

app.use(cors({
  origin: function (origin, callback) {
    // Allow all origins in production, or restrict to process.env.FRONTEND_URL if you prefer
    callback(null, true);
  },
  credentials: true
}));

app.use(express.json({ limit: '50mb' }));
app.use(express.urlencoded({ extended: false, limit: '50mb' }));

// ─── Prometheus Metrics Middleware (auto-instruments all routes) ──
app.use(metricsMiddleware);

// ─── Health Check and Root Routes ──────────────────────────────────
app.get("/", (req, res) => {
  res.send("BLE Attendance Backend is running 🚀");
});

app.get("/health", (req, res) => {
  res.status(200).json({
    status: "healthy",
    uptime: process.uptime(),
    timestamp: new Date().toISOString(),
    memoryUsage: process.memoryUsage(),
  });
});

// Mount Routes
app.use("/api/auth", authRouter);
app.use("/auth", authRouter); // Fallback for web dashboard missing /api/ prefix
app.use("/api/admin", adminRouter);
app.use("/api/professor", professorRouter);
app.use("/api/student", studentRouter);

app.use((req, res, next) => {
  res.status(404).json({ message: "Could not find this route" });
});

app.use((error, req, res, next) => {
  if (res.headersSent) {
    return next(error);
  }
  res.status(error.code || 500);
  res.json({ message: error.message || "An unknown error occurred!" });
});

// ─── Socket.IO Real-Time WebSockets ────────────────────────────────
const http = require("http");
const { Server } = require("socket.io");

const server = http.createServer(app);
const io = new Server(server, {
  cors: {
    origin: true, // Allow all origins for production ease
    methods: ["GET", "POST"],
    credentials: true
  }
});

// Make io accessible globally in controllers via req.app.get('io')
app.set("io", io);

io.on("connection", (socket) => {
  console.log(`[Socket.io] Client connected: ${socket.id}`);
  
  // Professor joins a specific course room to get live updates
  socket.on("join_course", (courseId) => {
    socket.join(`course_${courseId}`);
    console.log(`[Socket.io] Client ${socket.id} subscribed to course_${courseId}`);
  });

  socket.on("disconnect", () => {
    // console.log(`[Socket.io] Client disconnected: ${socket.id}`);
  });
});

server.listen(process.env.PORT || 5000, "0.0.0.0", () => {
  console.log(`Server is started on port ${process.env.PORT || 5000}`);
});

// ─── Automated Course Archival (Cron Job) ────────────────────────────
// Run at midnight every day
cron.schedule("0 0 * * *", async () => {
  console.log("[CRON] Running automated course archival check...");
  try {
    // Find all active courses whose expiry date has passed
    const expiredCourses = await prisma.course.findMany({
      where: {
        courseExpiry: { lte: new Date() },
        courseStatus: "active",
        isArchived: false,
      },
      include: { attendances: true },
    });

    if (expiredCourses.length === 0) {
      console.log("[CRON] No expired courses found.");
      return;
    }

    console.log(`[CRON] Found ${expiredCourses.length} expired course(s) to archive.`);

    for (const course of expiredCourses) {
      try {
        // Mark course as archived
        await prisma.course.update({
          where: { id: course.id },
          data: { isArchived: true },
        });

        // Mark all attendance records for this course as archived
        await prisma.attendance.updateMany({
          where: { courseId: course.id },
          data: { isArchived: true },
        });

        console.log(`[CRON] ✅ Archived course: "${course.name}" (batch: ${course.batch}, expiry: ${course.courseExpiry})`);
      } catch (err) {
        console.error(`[CRON] ❌ Failed to archive course "${course.name}":`, err.message);
      }
    }

    console.log("[CRON] Archival job complete.");
  } catch (err) {
    console.error("[CRON] Archival job error:", err.message);
  }
});