package com.example.absenceiq.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore


@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit
) {

    val context = LocalContext.current

    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    val uid = auth.currentUser?.uid

    var name by remember {
        mutableStateOf("")
    }

    var email by remember {
        mutableStateOf("")
    }

    var role by remember {
        mutableStateOf("")
    }

    var studentId by remember {
        mutableStateOf("")
    }

    var department by remember {
        mutableStateOf("")
    }

    var isLoading by remember {
        mutableStateOf(true)
    }

    var showPasswordDialog by remember {
        mutableStateOf(false)
    }

    var currentPassword by remember {
        mutableStateOf("")
    }

    var newPassword by remember {
        mutableStateOf("")
    }

    var confirmPassword by remember {
        mutableStateOf("")
    }

    var isChangingPassword by remember {
        mutableStateOf(false)
    }

    val teal = Color(0xFF0F7780)
    val background = Color(0xFFF5F7FA)

    /*
     * Load user profile from Firestore
     */
    LaunchedEffect(uid) {

        if (uid == null) {

            isLoading = false
            return@LaunchedEffect
        }

        db.collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener { document ->

                name =
                    document.getString("name")
                        ?: ""

                email =
                    document.getString("email")
                        ?: auth.currentUser?.email
                                ?: ""

                role =
                    document.getString("role")
                        ?: ""

                studentId =
                    document.getString("studentId")
                        ?: ""

                department =
                    document.getString("department")
                        ?: ""

                isLoading = false
            }
            .addOnFailureListener { error ->

                isLoading = false

                Toast.makeText(
                    context,
                    "Unable to load profile: ${error.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    /*
     * CHANGE PASSWORD DIALOG
     *
     * Important:
     * This must be OUTSIDE LaunchedEffect.
     */
    if (showPasswordDialog) {

        AlertDialog(

            onDismissRequest = {

                if (!isChangingPassword) {
                    showPasswordDialog = false
                }
            },

            title = {
                Text("Change Password")
            },

            text = {

                Column {

                    Text(
                        text =
                            "Enter your current password and choose a new password."
                    )

                    Spacer(
                        Modifier.height(16.dp)
                    )

                    OutlinedTextField(
                        value = currentPassword,

                        onValueChange = {
                            currentPassword = it
                        },

                        label = {
                            Text("Current Password")
                        },

                        visualTransformation =
                            PasswordVisualTransformation(),

                        singleLine = true,

                        enabled =
                            !isChangingPassword,

                        modifier =
                            Modifier.fillMaxWidth()
                    )

                    Spacer(
                        Modifier.height(12.dp)
                    )

                    OutlinedTextField(
                        value = newPassword,

                        onValueChange = {
                            newPassword = it
                        },

                        label = {
                            Text("New Password")
                        },

                        visualTransformation =
                            PasswordVisualTransformation(),

                        singleLine = true,

                        enabled =
                            !isChangingPassword,

                        modifier =
                            Modifier.fillMaxWidth()
                    )

                    Spacer(
                        Modifier.height(12.dp)
                    )

                    OutlinedTextField(
                        value = confirmPassword,

                        onValueChange = {
                            confirmPassword = it
                        },

                        label = {
                            Text("Confirm New Password")
                        },

                        visualTransformation =
                            PasswordVisualTransformation(),

                        singleLine = true,

                        enabled =
                            !isChangingPassword,

                        modifier =
                            Modifier.fillMaxWidth()
                    )

                    if (isChangingPassword) {

                        Spacer(
                            Modifier.height(18.dp)
                        )

                        Row(
                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {

                            CircularProgressIndicator(
                                modifier =
                                    Modifier.size(22.dp)
                            )

                            Spacer(
                                Modifier.width(10.dp)
                            )

                            Text(
                                "Updating password..."
                            )
                        }
                    }
                }
            },

            confirmButton = {

                Button(
                    enabled =
                        !isChangingPassword,

                    onClick = {

                        val user =
                            auth.currentUser

                        val userEmail =
                            user?.email

                        if (
                            user == null ||
                            userEmail.isNullOrBlank()
                        ) {

                            Toast.makeText(
                                context,
                                "Unable to identify the current user",
                                Toast.LENGTH_LONG
                            ).show()

                            return@Button
                        }

                        if (
                            currentPassword.isBlank() ||
                            newPassword.isBlank() ||
                            confirmPassword.isBlank()
                        ) {

                            Toast.makeText(
                                context,
                                "Please fill all password fields",
                                Toast.LENGTH_SHORT
                            ).show()

                            return@Button
                        }

                        if (
                            newPassword.length < 6
                        ) {

                            Toast.makeText(
                                context,
                                "New password must be at least 6 characters",
                                Toast.LENGTH_SHORT
                            ).show()

                            return@Button
                        }

                        if (
                            newPassword != confirmPassword
                        ) {

                            Toast.makeText(
                                context,
                                "New passwords do not match",
                                Toast.LENGTH_SHORT
                            ).show()

                            return@Button
                        }

                        if (
                            currentPassword == newPassword
                        ) {

                            Toast.makeText(
                                context,
                                "New password must be different from the current password",
                                Toast.LENGTH_SHORT
                            ).show()

                            return@Button
                        }

                        isChangingPassword = true

                        /*
                         * Re-authenticate using current password
                         */
                        val credential =
                            EmailAuthProvider
                                .getCredential(
                                    userEmail,
                                    currentPassword
                                )

                        user.reauthenticate(
                            credential
                        )
                            .addOnSuccessListener {

                                /*
                                 * Current password is correct.
                                 * Update to the new password.
                                 */
                                user.updatePassword(
                                    newPassword
                                )
                                    .addOnSuccessListener {

                                        isChangingPassword =
                                            false

                                        showPasswordDialog =
                                            false

                                        currentPassword =
                                            ""

                                        newPassword =
                                            ""

                                        confirmPassword =
                                            ""

                                        Toast.makeText(
                                            context,
                                            "Password changed successfully",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                    .addOnFailureListener { error ->

                                        isChangingPassword =
                                            false

                                        Toast.makeText(
                                            context,
                                            "Unable to update password: ${error.message}",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                            }
                            .addOnFailureListener {

                                isChangingPassword =
                                    false

                                Toast.makeText(
                                    context,
                                    "Current password is incorrect",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                    }
                ) {

                    Text("Update")
                }
            },

            dismissButton = {

                TextButton(
                    enabled =
                        !isChangingPassword,

                    onClick = {

                        showPasswordDialog =
                            false

                        currentPassword =
                            ""

                        newPassword =
                            ""

                        confirmPassword =
                            ""
                    }
                ) {

                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(

        topBar = {

            Surface(
                color = teal
            ) {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 16.dp,
                            vertical = 18.dp
                        ),

                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    TextButton(
                        onClick = onBack
                    ) {

                        Text(
                            text = "← Back",
                            color = Color.White
                        )
                    }

                    Spacer(
                        Modifier.width(8.dp)
                    )

                    Text(
                        text = "Profile",
                        color = Color.White,
                        fontSize = 23.sp,
                        fontWeight =
                            FontWeight.Bold
                    )
                }
            }
        }

    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(background)
                .verticalScroll(
                    rememberScrollState()
                )
                .padding(paddingValues)
                .padding(20.dp),

            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            if (isLoading) {

                CircularProgressIndicator()

            } else {

                /*
                 * PROFILE INITIALS
                 */
                Surface(
                    modifier =
                        Modifier.size(90.dp),

                    shape =
                        CircleShape,

                    color =
                        Color(0xFFD9EFF1)
                ) {

                    Box(
                        contentAlignment =
                            Alignment.Center
                    ) {

                        Text(
                            text =
                                name
                                    .split(" ")
                                    .filter {
                                        it.isNotBlank()
                                    }
                                    .take(2)
                                    .mapNotNull {
                                        it.firstOrNull()
                                    }
                                    .joinToString("")
                                    .uppercase()
                                    .ifBlank {
                                        "U"
                                    },

                            fontSize =
                                28.sp,

                            fontWeight =
                                FontWeight.Bold,

                            color =
                                teal
                        )
                    }
                }

                Spacer(
                    Modifier.height(16.dp)
                )

                Text(
                    text =
                        name.ifBlank {
                            "User"
                        },

                    fontSize =
                        24.sp,

                    fontWeight =
                        FontWeight.Bold
                )

                Spacer(
                    Modifier.height(5.dp)
                )

                Text(
                    text =
                        role
                            .replaceFirstChar {
                                if (
                                    it.isLowerCase()
                                ) {
                                    it.titlecase()
                                } else {
                                    it.toString()
                                }
                            },

                    color =
                        Color.Gray
                )

                Spacer(
                    Modifier.height(28.dp)
                )

                /*
                 * USER INFORMATION
                 */
                Card(
                    modifier =
                        Modifier.fillMaxWidth(),

                    shape =
                        RoundedCornerShape(
                            18.dp
                        ),

                    colors =
                        CardDefaults.cardColors(
                            containerColor =
                                Color.White
                        )
                ) {

                    Column(
                        modifier =
                            Modifier.padding(
                                20.dp
                            )
                    ) {

                        ProfileInfoRow(
                            label =
                                "Full Name",

                            value =
                                name.ifBlank {
                                    "-"
                                }
                        )

                        HorizontalDivider()

                        ProfileInfoRow(
                            label =
                                "Email",

                            value =
                                email.ifBlank {
                                    "-"
                                }
                        )

                        HorizontalDivider()

                        ProfileInfoRow(
                            label =
                                "Role",

                            value =
                                role
                                    .replaceFirstChar {
                                        if (
                                            it.isLowerCase()
                                        ) {
                                            it.titlecase()
                                        } else {
                                            it.toString()
                                        }
                                    }
                                    .ifBlank {
                                        "-"
                                    }
                        )

                        if (
                            studentId.isNotBlank()
                        ) {

                            HorizontalDivider()

                            ProfileInfoRow(
                                label =
                                    if (
                                        role.equals(
                                            "student",
                                            ignoreCase =
                                                true
                                        )
                                    )
                                        "Student ID"
                                    else
                                        "Staff ID",

                                value =
                                    studentId
                            )
                        }

                        if (
                            department.isNotBlank()
                        ) {

                            HorizontalDivider()

                            ProfileInfoRow(
                                label =
                                    "Department",

                                value =
                                    department
                            )
                        }
                    }
                }

                Spacer(
                    Modifier.height(24.dp)
                )

                /*
                 * CHANGE PASSWORD
                 */
                OutlinedButton(
                    onClick = {

                        currentPassword =
                            ""

                        newPassword =
                            ""

                        confirmPassword =
                            ""

                        showPasswordDialog =
                            true
                    },

                    modifier =
                        Modifier.fillMaxWidth()
                ) {

                    Text(
                        "Change Password"
                    )
                }

                Spacer(
                    Modifier.height(12.dp)
                )

                /*
                 * LOGOUT
                 */
                Button(
                    onClick =
                        onLogout,

                    modifier =
                        Modifier.fillMaxWidth(),

                    colors =
                        ButtonDefaults
                            .buttonColors(
                                containerColor =
                                    teal
                            )
                ) {

                    Text("Logout")
                }
            }
        }
    }
}


@Composable
fun ProfileInfoRow(
    label: String,
    value: String
) {

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    vertical =
                        14.dp
                )
    ) {

        Text(
            text =
                label,

            color =
                Color.Gray,

            fontSize =
                13.sp
        )

        Spacer(
            Modifier.height(
                4.dp
            )
        )

        Text(
            text =
                value,

            fontWeight =
                FontWeight.Medium,

            fontSize =
                16.sp
        )
    }
}