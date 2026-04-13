# 📡 BLE-Based Secure Attendance System

![License](https://img.shields.io/badge/License-MIT-blue.svg)
![Status](https://img.shields.io/badge/Status-Production%20Ready-success.svg)

Welcome to the **BLE-Based Attendance System**! This system is a revolutionary, hardware-free solution for marking student attendance securely in large lecture halls. It uses Bluetooth Low Energy (BLE), precise Geofencing, and AI Face Verification to guarantee that the student marking attendance is actually in the classroom.

---

## 📖 How It Works (For Non-Technical Users)

Imagine a classroom with 150 students. Calling roll numbers takes 15 minutes. Taking attendance on paper leads to proxy attendance (students signing for their absent friends). 

**Our solution solves this in 10 seconds:**
1. **The Professor** opens the app and taps "Start Class". Their phone temporarily becomes a secure "Bluetooth Beacon" broadcasting an invisible, encrypted signal.
2. **The Students** open the app on their phones. Their phones automatically detect the Professor's Bluetooth signal.
3. The system checks the student's **GPS Location** to ensure they aren't standing out in the hallway or in their dorm.
4. The student takes a quick **Selfie**. The AI instantly verifies it mathematically against their registered face.
5. If everything matches (Signal + Location + Face), attendance is pushed to the live web dashboard!

No special hardware required. No proxy attendance. No wasted time.

---

## 🏗️ System Architecture

This project is a complete full-stack enterprise application split into three main parts:

### 1. The Mobile App (Android)
Built using Modern **Kotlin & Jetpack Compose**. It handles the Bluetooth scanning, GPS geofencing, and captures the face data. It has separate, secure logins for both Students and Professors.

### 2. The Brain (Backend Server)
Built using **Node.js, Express, and Prisma**. This server runs on the cloud 24/7. It does the heavy lifting: verifying the facial math (Cosine Similarity), checking if the Bluetooth token corresponds to a live class, and preventing hacking or duplicate button spam. 

### 3. The Monitor (Web Dashboard)
Built using **React & Vite**. This is for College Administrators to log in from a computer, view live statistics, track student attendance percentages, and manage courses visually.

---

## 🚀 Setting Up the Project (For Developers)

If you are a developer looking to run this source code on your local computer, follow these simple steps!

### Prerequisites
- Install **Node.js** (v18+)
- Install **Android Studio**
- Set up a free **Neon.tech** PostgreSQL Database
- Set up a free **Firebase** account (for admin auth)

### Step 1: Database & Backend Setup
1. Open the `Backend` folder in your terminal.
2. Run `npm install` to download all the necessary server packages.
3. Create a file named `.env` in the Backend folder and add your database links:
   ```env
   DATABASE_URL="postgresql://username:password@your-neon-link.neon.tech/neondb?sslmode=require"
   PORT=8000
   ```
4. Run `npx prisma db push`. This automatically reads the `schema.prisma` file and builds all the database tables in your cloud database!
5. Run `npm run dev` to start the server. You should see `Database connected` and `Server is started on port 8000`.

### Step 2: Running the Web Dashboard
1. Open the `WebDashboard` folder.
2. Run `npm install` to download the website packages.
3. Run `npm run dev` and click the `localhost` link to open the beautiful React dashboard in your browser.

### Step 3: Running the Android App
1. Open up **Android Studio**.
2. Click **Open Project** and select the `Android` folder.
3. Let Gradle sync and download the mobile libraries (this might take a few minutes).
4. *Important:* Open `Retrofitinstance.kt` and change the IP address to your computer's local Wi-Fi IP address (e.g., `http://192.168.1.5:8000/`) so the phone can talk to the backend.
5. Plug in your Android phone (or use an emulator) and click the green **Run (▶)** button!

---

## 🛡️ Enterprise Security & Validation

This code isn't just an academic prototype; it is hardened for actual deployment:
* **Idempotency:** Replay-attacks and duplicate button smashing are mathematically blocked at the database level.
* **Large Payloads:** The backend safely ingests up to `50mb` of JSON data without crashing to support ultra-high-resolution mobile selfies.
* **Stress Tested:** The architecture has been rigorously load-tested (via `k6`) to withstand the "09:00 AM Thundering Herd"—the exact moment 150 students simultaneously tap the attendance button.

---
*Built with ❤️ for modern classrooms.*
