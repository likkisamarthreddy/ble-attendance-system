# Bluetooth Low Energy Based Automatic Attendance System

## Abstract
This paper presents a secure Bluetooth Low Energy (BLE)-based attendance system aimed at preventing proxy attendance and significantly improving efficiency in academic settings. Professors send BLE packets through a dedicated Android app, which students scan to mark their attendance. The backend infrastructure utilizes a high-performance PostgreSQL (Neon Serverless) database managed via Prisma ORM, deployed using clustering to ensure high availability. The system guarantees authenticity through a multi-layered security approach: Firebase Authentication, Play Integrity API for device attestation, and offline-capable Face Recognition powered by ML Kit and TensorFlow Lite (MobileFaceNet). Furthermore, the application integrates Geofencing alongside BLE proximity to enforce strict physical presence constraints. An administrative React/Vite web dashboard provides scalable, real-time insights utilizing Socket.IO. Comprehensive stress, spike, and load tests confirm the system's robustness for institutional deployment.

## I. INTRODUCTION
Traditional attendance systems in educational institutions are often vulnerable to inefficiencies, manipulation, and proxy attendance, compromising both academic integrity and administrative efficiency. This proposal introduces a secure, scalable and technology-driven solution: a Bluetooth Low Energy (BLE)-based attendance system that leverages proximity-based validation, Face Recognition, biometric authentication, and strict device-level binding to ensure authentic presence recording.

At the core of this system is a multi-node architecture designed for professors, students, and administrators. Professors initiate attendance by broadcasting a BLE signal securely tied to a PostgreSQL/Neon database record ID. Students within range receive the broadcast and respond via the app, which communicates with the backend server to validate and mark attendance. Real-time communication is facilitated by Socket.IO, ensuring immediate UI updates across both mobile and web interfaces. 

To mitigate proxy attendance completely, the system utilizes TensorFlow Lite (MobileFaceNet) for on-device face verification, the Google Play Integrity API to confirm the device environment is untampered, and Google Play Services Location for geofencing validation. The localized physical range of BLE, combined with these hardware-backed verifications, eliminates the possibility of remote proxy log-ins.

This solution offers a significant advancement over Biometric Hardware or manual attendance systems by being secure, contactless, real-time, and highly scalable—a fact corroborated by rigorous backend throughput evaluations.

## II. PROBLEM STATEMENT
Attendance tracking is a fundamental aspect of academic administration, but ensuring its accuracy and integrity remains a persistent challenge. In most educational institutions, existing attendance methods are either outdated or prone to manipulation. Proxy attendance undermines both academic accountability and institutional credibility. Traditional systems like manual roll calls, RFID-based card swiping, and biometric fingerprint hardware are commonly used, but heavily flawed.

### A. Manual Roll Call
Manual roll call requires instructors to call out student names and mark attendance based on verbal responses. While simple and cost-effective, it is time-consuming and prone to proxy attendance. This method lacks any form of automation or verification.

### B. RFID Based Cards
RFID systems automate attendance by allowing students to scan ID cards. However, they are vulnerable to misuse since cards can be easily shared among students. The need for dedicated hardware and ongoing maintenance adds to institutional overhead, limiting scalability.

### C. Biometric Hardware
Biometric fingerprint scanners provide identity verification but require expensive hardware and centralized infrastructure. These systems cause classroom delays, raise hygiene concerns, and are impractical for flexible off-site settings. Moreover, they cannot confirm a student’s presence without installing a fixed kiosk in every room.

## III. PROPOSED SYSTEM Architecture
The proposed system is a highly robust, proximity-aware suite that leverages BLE, MobileFaceNet-based Face Recognition, Play Integrity attestation, and a Serverless PostgreSQL backend. 

### A. Technology Stack Overview
- **Mobile Application (Android):** Built natively with Jetpack Compose (Kotlin), architecture utilizes Kotlin Coroutines and the Room Database for offline attendance caching, synchronized asynchronously via Android WorkManager.
- **Backend API:** Built on Node.js and Express. Scaling and worker processing are managed via PM2 clustering. Data operations are securely mediated by Prisma ORM connecting to a Neon Serverless PostgreSQL database. Socket.IO empowers real-time bidirectional event transmission.
- **Admin Dashboard (Web):** A React 19 Single Page Application built on Vite, heavily leveraging Tailwind CSS for layout and Recharts for administrative analytics visualization.
- **Performance:** Extensive load, spike, and stress testing profiles guarantee the infrastructure's ability to handle high concurrency during classroom attendance windows.

### B. System Roles and Authentication Flow
The application operates in three modes: Student, Professor, and Admin. Accounts are created manually or via CSV upload by Administrators.
Rather than relying solely on basic Android IDs or SIM subscriptions, the application now leverages the **Google Play Integrity API**. This validates that the app binary and the Android OS environment are untampered, blocking spoofed locations or modified APKs. 

During student login—if the device is verified as legitimate—the authentication flow triggers an advanced Face Recognition protocol. This is executed fully on-device using a TensorFlow Lite `MobileFaceNet` model integrated via Google ML Kit and CameraX APIs. 

### C. Professor Workflow and BLE Broadcasting
Professors create courses, generating nanoID-based secure identifiers for enrollment. After login, professors can initiate BLE broadcasting. 
The backend responds with a unique record ID, broadcasted over BLE with a predefined UUID, safely isolating it from unrelated BLE signals. The token broadcast time is deliberately reduced to prevent packet sniffing and relay attacks. Professors can actively monitor the class login count updating dynamically via Socket.IO connections.

### D. Student Workflow and BLE Scanning
To mark attendance, students enable Bluetooth and Location services. The application implements an offline-first caching strategy utilizing the **Room Database**. 
If a student scans the professor's BLE broadcast but experiences a sudden network drop, the attendance record is cached securely in Room and queued to Android's **WorkManager**. When the network is restored, WorkManager asynchronously syncs the data back to the server. Furthermore, Play Services Geofencing ensures the student's physical location matches the campus bounds at the exact time of the BLE handshake.

### E. Admin Operations
The React-based frontend dashboard facilitates centralized control over user and course management. It prevents unauthorized registrations, enforces clean data modeling, and guarantees structured institutional control.

Administrators can:
- View all active/archived courses.
- Leverage Recharts visualizations to monitor attendance trends.
- Perform real-time audits on individual historical student attendance.
- Perform high concurrency administrative changes with confidence, backed by Prisma database transactions.

### F. Security and Integrity Measures
This system represents an industry-leading academic tracking standard by applying:
1. **Device Fingerprinting & Attestation:** Play Integrity API prevents rooted devices or emulators from interacting with the backend.
2. **On-Device Artificial Intelligence:** TensorFlow Lite MobileFaceNet prevents biometric-sharing proxies without risking cloud-privacy leaks.
3. **Geofencing & BLE Constraints:** Simultaneous localized distance metrics make remote marking geometrically impossible.
4. **Resiliency Testing:** Automated load and stress tests explicitly verifying that burst traffics (100+ students interacting within 10 seconds) do not cause backend bottlenecking.

## IV. CONCLUSION
This paper presents an advanced, fault-tolerant BLE-based attendance system bridging Jetpack Compose Android engineering with scalable serverless architecture. By deploying Neon PostgreSQL, WorkManager offline-synchronization, and Play Integrity APIs, the system resolves historical loopholes associated with digital proxy markers. Integrated Face Recognition prevents identity spoofing, and the comprehensive web dashboard empowers real-time administrative oversight. The rigorous stress-testing framework guarantees that the system is fully production-ready for large-scale institutional integration.

## ACKNOWLEDGMENT
I would like to express my sincere gratitude to the Indian Institute of Information Technology, Guwahati for providing the necessary guidance and support for this project. 

## REFERENCES
[1] BluetoothLeAdvertiser / BluetoothLeScanner Reference - Android Developers  
[2] Prisma ORM Documentation - Prisma Data Inc.
[3] TensorFlow Lite MobileFaceNet Integration - Google Open Source  
[4] Play Integrity API - Google Developers
[5] node-cron & Socket.IO - npm Registry
 