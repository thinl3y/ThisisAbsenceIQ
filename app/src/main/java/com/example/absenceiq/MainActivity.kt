package com.example.absenceiq

import androidx.activity.compose.BackHandler
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.example.absenceiq.screens.*
import com.example.absenceiq.ui.theme.AbsenceIQTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.TIRAMISU
        ) {

            if (
                ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {

                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        Manifest.permission.POST_NOTIFICATIONS
                    ),
                    1001
                )
            }
        }

        setContent {

            AbsenceIQTheme {

                var currentScreen by remember {
                    mutableStateOf("login")
                }
                BackHandler(
                    enabled = currentScreen != "login"
                ) {

                    when (currentScreen) {

                        "register" -> {
                            currentScreen = "login"
                        }

                        "studentDashboard" -> {
                            auth.signOut()
                            currentScreen = "login"
                        }

                        "applyLeave" -> {
                            currentScreen = "studentDashboard"
                        }

                        "leaveHistory" -> {
                            currentScreen = "studentDashboard"
                        }

                        "com/example/absenceiq/notifications" -> {
                            currentScreen = "studentDashboard"
                        }

                        "studentProfile" -> {
                            currentScreen = "studentDashboard"
                        }

                        "facultyDashboard" -> {
                            auth.signOut()
                            currentScreen = "login"
                        }

                        "facultyRequests" -> {
                            currentScreen = "facultyDashboard"
                        }

                        "facultyHistory" -> {
                            currentScreen = "facultyDashboard"
                        }

                        "facultyProfile" -> {
                            currentScreen = "facultyDashboard"
                        }

                        "adminDashboard" -> {
                            auth.signOut()
                            currentScreen = "login"
                        }

                        "adminRequests" -> {
                            currentScreen = "adminDashboard"
                        }

                        "adminHistory" -> {
                            currentScreen = "adminDashboard"
                        }

                        "adminProfile" -> {
                            currentScreen = "adminDashboard"
                        }
                    }
                }

                when (currentScreen) {

                    "login" -> {

                        LoginScreen(

                            onLoginClick = { email, password ->

                                loginUser(
                                    email = email,
                                    password = password,
                                    onRoleFound = { role ->

                                        currentScreen =
                                            when (role) {

                                                "student" ->
                                                    "studentDashboard"

                                                "faculty" ->
                                                    "facultyDashboard"

                                                "admin" ->
                                                    "adminDashboard"

                                                else ->
                                                    "login"
                                            }
                                    }
                                )
                            },

                            onRegisterClick = {
                                currentScreen = "register"
                            }
                        )
                    }
                    "register" -> {

                        RegisterScreen(

                            onRegisterClick = {
                                    name,
                                    email,
                                    password,
                                    studentId,
                                    department ->

                                registerUser(
                                    name = name,
                                    email = email,
                                    password = password,
                                    studentId = studentId,
                                    department = department
                                )
                            },

                            onBackToLogin = {
                                currentScreen = "login"
                            }
                        )
                    }

                    "studentDashboard" -> {

                        StudentDashboardScreen(

                            onApplyLeave = {
                                currentScreen = "applyLeave"
                            },

                            onHistoryClick = {
                                currentScreen = "leaveHistory"
                            },

                            onNotificationsClick = {
                                currentScreen = "com/example/absenceiq/notifications"
                            },

                            onProfileClick = {
                                currentScreen ="studentProfile"// Student profile later
                            },

                            onLogout = {
                                auth.signOut()
                                currentScreen = "login"
                            }
                        )
                    }

                    "applyLeave" -> {

                        ApplyLeaveScreen(

                            onBack = {
                                currentScreen = "studentDashboard"
                            },

                            onLeaveSubmitted = {
                                currentScreen = "studentDashboard"
                            }
                        )
                    }

                    "com/example/absenceiq/notifications" -> {

                        NotificationsScreen(

                            onHomeClick = {
                                currentScreen = "studentDashboard"
                            },

                            onHistoryClick = {
                                currentScreen = "leaveHistory"
                            },

                            onProfileClick = {
                                currentScreen = "studentProfile"
                            }
                        )
                    }

                    "leaveHistory" -> {

                        LeaveHistoryScreen(

                            onBack = {
                                currentScreen = "studentDashboard"
                            }
                        )
                    }

                    "facultyDashboard" -> {

                        FacultyDashboardScreen(

                            onRequestsClick = {
                                currentScreen =
                                    "facultyRequests"
                            },

                            onHistoryClick = {
                                currentScreen =
                                    "facultyHistory"
                            },

                            onProfileClick = {
                                currentScreen =
                                    "facultyProfile"
                            },

                            onLogout = {
                                auth.signOut()
                                currentScreen =
                                    "login"
                            }
                        )

                    }

                    "facultyRequests" -> {

                        FacultyRequestsScreen(

                            onBack = {
                                currentScreen = "facultyDashboard"
                            }
                        )
                    }

                    "facultyHistory" -> {

                        FacultyHistoryScreen(

                            onBack = {
                                currentScreen = "facultyDashboard"
                            }
                        )
                    }

                    "adminDashboard" -> {

                        AdminDashboardScreen(

                            onRequestsClick = {
                                currentScreen =
                                    "adminRequests"
                            },

                            onHistoryClick = {
                                currentScreen =
                                    "adminHistory"
                            },

                            onProfileClick = {
                                currentScreen = "adminProfile"
                            },

                            onLogout = {
                                auth.signOut()
                                currentScreen =
                                    "login"
                            }
                        )
                    }

                    "adminRequests" -> {

                        AdminRequestsScreen(

                            onBack = {
                                currentScreen = "adminDashboard"
                            }
                        )
                    }

                    "adminHistory" -> {

                        AdminHistoryScreen(

                            onBack = {
                                currentScreen = "adminDashboard"
                            }
                        )
                    }
                    "studentProfile" -> {

                        ProfileScreen(

                            onBack = {
                                currentScreen = "studentDashboard"
                            },

                            onLogout = {
                                auth.signOut()
                                currentScreen =
                                    "login"
                            }
                        )
                    }

                    "facultyProfile" -> {

                        ProfileScreen(

                            onBack = {
                                currentScreen = "facultyDashboard"
                            },

                            onLogout = {
                                auth.signOut()
                                currentScreen =
                                    "login"
                            }
                        )
                    }

                    "adminProfile" -> {

                        ProfileScreen(

                            onBack = {
                                currentScreen = "adminDashboard"
                            },

                            onLogout = {
                                auth.signOut()
                                currentScreen =
                                    "login"
                            }
                        )
                    }
                }
            }
        }
    }

    private fun loginUser(
        email: String,
        password: String,
        onRoleFound: (String) -> Unit
    ) {

        if (email.isBlank() || password.isBlank()) {

            Toast.makeText(
                this,
                "Please enter email and password",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        auth.signInWithEmailAndPassword(
            email,
            password
        )
            .addOnSuccessListener {

                val uid = auth.currentUser?.uid

                if (uid == null) {

                    Toast.makeText(
                        this,
                        "Unable to identify user",
                        Toast.LENGTH_LONG
                    ).show()

                    return@addOnSuccessListener
                }

                db.collection("users")
                    .document(uid)
                    .get()
                    .addOnSuccessListener { document ->

                        if (!document.exists()) {

                            Toast.makeText(
                                this,
                                "User profile was not found",
                                Toast.LENGTH_LONG
                            ).show()

                            auth.signOut()

                            return@addOnSuccessListener
                        }

                        val role =
                            document.getString("role")

                        if (role.isNullOrBlank()) {

                            Toast.makeText(
                                this,
                                "User role is missing",
                                Toast.LENGTH_LONG
                            ).show()

                            auth.signOut()

                            return@addOnSuccessListener
                        }

                        onRoleFound(
                            role.lowercase()
                        )
                    }
                    .addOnFailureListener { error ->

                        Toast.makeText(
                            this,
                            "Unable to load profile: ${error.message}",
                            Toast.LENGTH_LONG
                        ).show()

                        auth.signOut()
                    }
            }
            .addOnFailureListener { error ->

                Toast.makeText(
                    this,
                    "Login failed: ${error.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun registerUser(
        name: String,
        email: String,
        password: String,
        studentId: String,
        department: String,
        /*role: String */
    ) {

        if (
            name.isBlank() ||
            email.isBlank() ||
            password.isBlank() ||
            studentId.isBlank() ||
            department.isBlank()
        ) {

            Toast.makeText(
                this,
                "Please fill all fields",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        auth.createUserWithEmailAndPassword(
            email,
            password
        )
            .addOnSuccessListener {

                val uid =
                    auth.currentUser?.uid

                if (uid == null) {

                    Toast.makeText(
                        this,
                        "Unable to create user profile",
                        Toast.LENGTH_LONG
                    ).show()

                    return@addOnSuccessListener
                }

                val user =
                    hashMapOf(
                        "uid" to uid,
                        "name" to name,
                        "email" to email,
                        "studentId" to studentId,
                        "department" to department,
                        "role" to "student",
                        "leaveBalance" to 10L
                    )

                db.collection("users")
                    .document(uid)
                    .set(user)
                    .addOnSuccessListener {

                        Toast.makeText(
                            this,
                            "Registration successful",
                            Toast.LENGTH_LONG
                        ).show()

                        auth.signOut()
                    }
                    .addOnFailureListener { error ->

                        Toast.makeText(
                            this,
                            "Unable to save profile: ${error.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
            }
            .addOnFailureListener { error ->

                Toast.makeText(
                    this,
                    "Registration failed: ${error.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }
}