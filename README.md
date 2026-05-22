# 📡 BLE Based Automatic Attendance System

![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/kotlin-%237F52FF.svg?style=for-the-badge&logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-4285F4?style=for-the-badge&logo=android&logoColor=white)
![NodeJS](https://img.shields.io/badge/node.js-6DA55F?style=for-the-badge&logo=node.js&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/postgresql-4169e1?style=for-the-badge&logo=postgresql&logoColor=white)

A modern, fast, and secure Attendance Management System that utilizes **Bluetooth Low Energy (BLE)** to seamlessly mark attendance. Professors can broadcast a secure BLE signature, and students in the vicinity can catch the signal to automatically verify their presence.

---

## ✨ Features

* **BLE Broadcasting & Scanning**: Contactless attendance marking using low-energy Bluetooth.
* **Geofencing & Location Verification**: Ensures students are physically present in the classroom.
* **Real-time Live Sync**: WebSockets (Socket.io) update the professor's dashboard live as students join.
* **Role-Based Access**: Dedicated workflows for **Students**, **Professors**, and **Admins**.
* **SIM Binding**: Maps a student's device to their SIM to prevent proxy attendance using different phones.
* **Modern UI/UX**: Built entirely with Jetpack Compose featuring a sleek "Neon Obsidian" command-center aesthetic, glassmorphism, and fluid animations.
* **Pull-to-Refresh**: Seamless drag-to-refresh course list synchronization.

---

## 🏗️ Architecture Stack

### **Android App (Frontend)**
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose (Material 3)
- **Networking**: Retrofit & OkHttp
- **Local Data**: DataStore Preferences
- **Authentication**: Firebase Auth

### **Backend Server**
- **Runtime**: Node.js & Express.js
- **Database**: PostgreSQL (hosted on Neon.tech)
- **ORM**: Prisma
- **Real-time**: Socket.io
- **Security**: Firebase Admin SDK (JWT Validation), HMAC Tokens

---

## 🚀 Getting Started

Follow these instructions to set up the project locally for development and testing.

### Prerequisites
* [Node.js](https://nodejs.org/en/) (v18+)
* [Android Studio](https://developer.android.com/studio) (Iguana or newer recommended)
* A physical Android device (BLE broadcasting/scanning **does not** work on emulators)
* A Firebase Project (for Authentication)
* A PostgreSQL Database (e.g., Neon.tech)

---

### 1️⃣ Backend Setup

1. Navigate to the backend directory:
   ```bash
   cd Backend
   ```
2. Install dependencies:
   ```bash
   npm install
   ```
3. Environment Configuration:
   Create a `.env` file in the `Backend` directory with the following keys:
   ```env
   PORT=8000
   DATABASE_URL="postgresql://<USER>:<PASSWORD>@<HOST>/<DB>?sslmode=require"
   FIREBASE_KEY_PATH="./serviceAccountKey.json"
   BLE_SESSION_SECRET="your_super_secret_hex_string_here"
   GEOFENCE_RADIUS_METERS=100
   ```
4. Add your Firebase Service Account:
   Download your Firebase Admin SDK `serviceAccountKey.json` from the Firebase Console and place it inside the `Backend/` directory.
5. Sync the Database schema:
   ```bash
   npx prisma db push
   ```
6. Start the server:
   ```bash
   npm run dev
   ```
   *The backend should now be running on `http://localhost:8000`.*

---

### 2️⃣ Android App Setup

1. Open the `Android` folder in **Android Studio**.
2. **Add Firebase config**: Download `google-services.json` from your Firebase project console and place it in the `Android/app/` directory.
3. **Configure the Server URL**:
   Open `Android/app/src/main/java/com/example/practice/api/RetrofitInstance.kt`.
   Change the `BASE_URL` to point to your local machine's IP Address (Make sure your phone and PC are on the same Wi-Fi network).
   ```kotlin
   // Example for local development
   private const val BASE_URL = "http://192.168.x.x:8000/api/" 
   ```
4. **Sync Project**: Let Gradle sync and download all necessary Android dependencies.
5. **Run the App**: Connect your physical Android device via USB or Wireless Debugging and hit **Run** (`Shift + F10`).

> **WARNING**:
> Testing BLE functionality requires physical Android devices. Running the app on an Android Virtual Device (AVD) will cause BLE scans to silently fail or crash.

---

## 📸 Screenshots

| Professor Dashboard | Attendance Session | Student Scanning |
| :---: | :---: | :---: |
| *(Add your screenshot here)* | *(Add your screenshot here)* | *(Add your screenshot here)* |

---

## 🤝 Contributing
Pull requests are welcome. For major changes, please open an issue first to discuss what you would like to change.

## 📝 License
This project is licensed under the MIT License.
