# Crime Detection & Violence Analysis App

An AI-powered Android application designed to detect violence and criminal activities in real-time using a mobile camera or uploaded video files. The app communicates with a FastAPI-based Machine Learning backend to analyze video frames and provide instant results.

## 🚀 Features
*   **Live Recording**: Capture video directly within the app using CameraX.
*   **Media Upload**: Select existing videos from the gallery for analysis.
*   **AI Analysis**: Real-time violence detection (Violent vs. Non-Violent) with confidence scores.
*   **Local History**: Saves all detection results locally using a Room Database.
*   
* **Emergency Integration**: Automated SMS alerts and manual WhatsApp location sharing.
*   **Report Generation**: Generate comprehensive PDF reports of criminal activities and statistical analysis.
*   **Stealth Mode**: Background recording and notification-based alerts.

## 🛠 Tech Stack
*   **Android**: Java, CameraX, Retrofit 2, Room DB, ViewModel & LiveData, ViewBinding.
*   **Backend**: Python, FastAPI, Uvicorn (Requires the Violence Detection ML Model).
*   **Networking**: REST API with JSON responses.

---

## 📋 Prerequisites
1.  **Android Studio** Jellyfish or newer.
2.  **Physical Android Device** (Recommended for camera and network testing).
3.  **Python 3.9+** (For the backend server).
4.  **USB Cable** for debugging.

---

## ⚙️ Setup & Installation

### 1. Backend Setup
Before running the app, ensure your FastAPI backend is running and accessible:
1.  Install dependencies: `pip install fastapi uvicorn opencv-python tensorflow` (add other model-specific libs).
2.  **CRITICAL**: Start the server using the `0.0.0.0` host to allow connections from your phone:
    ```bash
    uvicorn main:app --host 0.0.0.0 --port 8000
    ```
3.  Find your computer's **Local IP Address**:
    *   **Windows**: Open CMD, type `ipconfig`, look for `IPv4 Address` (e.g., `192.168.1.15`).
    *   **Mac/Linux**: Type `ifconfig` in terminal.

### 2. Android App Setup
1.  Clone this repository.
2.  Open the project in Android Studio.
3.  Enable **USB Debugging** on your phone (Settings > Developer Options).
4.  Connect your phone and click **Run**.

---

## 🔗 Connecting the App to the Backend
To make the app talk to your computer, follow these steps:

1.  **Same Network**: Ensure your phone and computer are connected to the **same Wi-Fi**.
2.  **Server Settings**:
    *   Open the App.
    *   Go to the **Settings** (Top right menu).
    *   Enter your computer's IP and port: `http://192.168.1.XX:8000`
    *   Click **Save**.
3.  **Check Connection**: The status bar in the app should change to `✅ Server Connected`.

### Troubleshooting Connection:
*   **Firewall**: If it won't connect, disable Windows Firewall or add an Inbound Rule for Port `8000`.
*   **Host**: Ensure you didn't use `127.0.0.1` in the app; that only works for emulators. Physical phones need the real IP.

---

## 📂 Project Structure
*   `activities/`: UI Controllers (Main, Login, Dashboard).
*   `viewmodel/`: Business logic and UI state management.
*   `network/`: Retrofit API clients and Response models.
*   `repository/`: Data handling between Room DB and Network.
*   `models/`: Room Database entities.
*   `managers/`: Emergency response and automated alert management.
*   `utils/`: Helper classes for Time, Notifications, PDF generation, and Preferences.

## 🛡 Security & Privacy
This app requires Camera, Audio, and Location permissions to function correctly. All video data processed is intended for analysis purposes only.

---
**Developed by Krishna**
