# 📱 PDA Android App (Kotlin)

A PDA-style Android application built with **Kotlin**, featuring **Firebase Authentication** and a base structure for managing products and categories.

This project has been cleaned so every developer can connect **their own Firebase project** and insert **their own data**.

---

## 🚀 Features

* 🔐 **Login / Register with Firebase Authentication**
* 🗂️ Structure for **Products & Categories**
* 🧩 Clean Kotlin-based architecture
* 🎨 XML layouts ready for extension
* 🧪 Example data models (no automatic sample data insertion)

---

## 🛠️ What’s Included in This Repository

Only the essential source and UI files are included:

```
✔ Kotlin source files (.kt)
✔ XML layout files
✔ Gradle dependencies (libs)
✔ Base structure for Activities & Models
```

Intentionally **removed**:

```
❌ google-services.json
❌ Firebase sample database data
❌ addSampleData() automatic product/category insertion
❌ Personal or test data
```

---

## 🔥 Firebase Setup (REQUIRED)

The application **will NOT run** unless you connect your own Firebase project.

### Steps:

1. Go to **Firebase Console**
   👉 [https://console.firebase.google.com/](https://console.firebase.google.com/)

2. Create a **New Project**

3. Add an **Android App** using:

   * The **same package name** as the project

4. Download the file:

```
google-services.json
```

5. Place it inside:

```
app/google-services.json
```

6. In Firebase Console enable:

   * **Authentication → Sign-in method → Email/Password**

---

## 👤 Authentication System

The app uses:

* **Firebase Authentication**
* Email & Password login
* Persistent user session (user stays logged in)

If Email/Password sign-in is not enabled in Firebase, login will not work.

---

## 🛒 Products & Categories

The structure for products and categories exists, but:

* Initial data has been removed
* `addSampleData()` has been removed
* Only **example models** remain to demonstrate structure

👉 Each developer should insert **their own data** into the database.

---

## 🧱 Project Structure (Example)

```
java/
 ├── activities/
 ├── adapters/
 ├── models/
 └── utils/

res/
 ├── layout/
 ├── values/
 └── drawable/
```

---

## ▶️ How to Run the App

1. Clone the repository
2. Open it in **Android Studio**
3. Complete the **Firebase setup** (see above)
4. Let Gradle sync
5. Press **Run ▶️** on an emulator or physical device

---

## ⚠️ Important Notes

* This project is a **template / baseline**, not a production-ready app
* No real data is included
* Each user must configure:

  * Firebase
  * Products
  * Categories

---

## 👨‍💻 Author

Developed in Kotlin for Android PDA-style usage.
Feel free to modify and extend it to fit your needs.
