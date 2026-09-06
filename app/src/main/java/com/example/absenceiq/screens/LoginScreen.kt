package com.example.absenceiq.screens

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.example.absenceiq.R
import com.google.firebase.auth.FirebaseAuth

@Composable
fun LoginScreen(
    onLoginClick: (String, String) -> Unit,
    onRegisterClick: () -> Unit
) {

    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()

    var email by remember {
        mutableStateOf("")
    }

    var password by remember {
        mutableStateOf("")
    }

    var selectedRole by remember {
        mutableStateOf("Student")
    }

    var passwordVisible by remember {
        mutableStateOf(false)
    }

    var isResettingPassword by remember {
        mutableStateOf(false)
    }

    val teal = Color(0xFF0F7780)
    val pageBackground = Color(0xFFF5F7FA)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(pageBackground)
            .verticalScroll(
                rememberScrollState()
            )
    ) {

        /*
         * =========================
         * HEADER
         * =========================
         */
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(teal)
                .padding(
                    top = 38.dp,
                    bottom = 28.dp
                ),
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            Image(
                painter = painterResource(
                    id = R.drawable.cst_logo
                ),
                contentDescription = "CST Logo",
                modifier = Modifier.size(78.dp)
            )

            Spacer(
                Modifier.height(14.dp)
            )

            Text(
                text = "AbsenceIQ",
                color = Color.White,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                Modifier.height(4.dp)
            )

            Text(
                text = "Student Leave Management",
                color = Color.White.copy(
                    alpha = 0.9f
                ),
                fontSize = 15.sp
            )
        }


        /*
         * =========================
         * FORM
         * =========================
         */
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 24.dp,
                    vertical = 24.dp
                )
        ) {

            /*
             * ROLE SELECTOR
             */
            Text(
                text = "Choose your role",
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp
            )

            Spacer(
                Modifier.height(10.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {

                RoleButton(
                    text = "Student",
                    selected =
                        selectedRole == "Student",
                    teal = teal,
                    modifier =
                        Modifier.weight(1f)
                ) {
                    selectedRole = "Student"
                }

                RoleButton(
                    text = "Faculty / SSO",
                    selected =
                        selectedRole == "Faculty / SSO",
                    teal = teal,
                    modifier =
                        Modifier.weight(1f)
                ) {
                    selectedRole = "Faculty / SSO"
                }

                RoleButton(
                    text = "HOD / Admin",
                    selected =
                        selectedRole == "HOD / Admin",
                    teal = teal,
                    modifier =
                        Modifier.weight(1f)
                ) {
                    selectedRole = "HOD / Admin"
                }
            }

            Spacer(
                Modifier.height(24.dp)
            )


            /*
             * EMAIL
             */
            Text(
                text = "Email",
                fontWeight = FontWeight.Medium
            )

            Spacer(
                Modifier.height(8.dp)
            )

            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                },
                placeholder = {
                    Text(
                        "Enter your registered email"
                    )
                },
                modifier =
                    Modifier.fillMaxWidth(),
                keyboardOptions =
                    KeyboardOptions(
                        keyboardType =
                            KeyboardType.Email
                    ),
                singleLine = true,
                shape =
                    RoundedCornerShape(14.dp)
            )


            Spacer(
                Modifier.height(18.dp)
            )


            /*
             * PASSWORD
             */
            Text(
                text = "Password",
                fontWeight = FontWeight.Medium
            )

            Spacer(
                Modifier.height(8.dp)
            )

            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                },
                placeholder = {
                    Text("Enter password")
                },
                modifier =
                    Modifier.fillMaxWidth(),
                visualTransformation =
                    if (passwordVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                trailingIcon = {

                    TextButton(
                        onClick = {
                            passwordVisible =
                                !passwordVisible
                        }
                    ) {

                        Text(
                            text =
                                if (passwordVisible)
                                    "Hide"
                                else
                                    "Show",
                            color = teal
                        )
                    }
                },
                keyboardOptions =
                    KeyboardOptions(
                        keyboardType =
                            KeyboardType.Password
                    ),
                singleLine = true,
                shape =
                    RoundedCornerShape(14.dp)
            )


            /*
             * FORGOT PASSWORD
             */
            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.End
            ) {

                TextButton(
                    enabled =
                        !isResettingPassword,
                    onClick = {

                        val trimmedEmail =
                            email.trim()

                        if (
                            trimmedEmail.isBlank()
                        ) {

                            Toast.makeText(
                                context,
                                "Enter your email first",
                                Toast.LENGTH_SHORT
                            ).show()

                            return@TextButton
                        }

                        isResettingPassword =
                            true

                        auth.sendPasswordResetEmail(
                            trimmedEmail
                        )
                            .addOnSuccessListener {

                                isResettingPassword =
                                    false

                                Toast.makeText(
                                    context,
                                    "Password reset email sent",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                            .addOnFailureListener {
                                    error ->

                                isResettingPassword =
                                    false

                                Toast.makeText(
                                    context,
                                    "Unable to send reset email: ${error.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                    }
                ) {

                    Text(
                        text =
                            if (isResettingPassword)
                                "Sending..."
                            else
                                "Forgot Password?",
                        color = teal
                    )
                }
            }


            Spacer(
                Modifier.height(8.dp)
            )


            /*
             * LOGIN
             */
            Button(
                onClick = {

                    if (email.isBlank()) {

                        Toast.makeText(
                            context,
                            "Please enter your email",
                            Toast.LENGTH_SHORT
                        ).show()

                        return@Button
                    }

                    if (password.isBlank()) {

                        Toast.makeText(
                            context,
                            "Please enter your password",
                            Toast.LENGTH_SHORT
                        ).show()

                        return@Button
                    }

                    onLoginClick(
                        email.trim(),
                        password
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape =
                    RoundedCornerShape(14.dp),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = teal
                    )
            ) {

                Text(
                    text = "LOGIN",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }


            Spacer(
                Modifier.height(12.dp)
            )


            /*
             * STUDENT REGISTRATION
             */
            TextButton(
                onClick =
                    onRegisterClick,
                modifier =
                    Modifier.align(
                        Alignment.CenterHorizontally
                    )
            ) {

                Text(
                    text =
                        "New student? Create account",
                    color = teal
                )
            }


            /*
             * FOOTER
             */
            Spacer(
                Modifier.height(70.dp)
            )

            Text(
                text =
                    "College Leave Management System",
                color =
                    Color.Gray,
                fontSize =
                    13.sp,
                modifier =
                    Modifier.align(
                        Alignment.CenterHorizontally
                    )
            )

            Spacer(
                Modifier.height(20.dp)
            )
        }
    }
}


@Composable
private fun RoleButton(
    text: String,
    selected: Boolean,
    teal: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {

    if (selected) {

        Button(
            onClick = onClick,
            modifier =
                modifier.height(52.dp),
            shape =
                RoundedCornerShape(14.dp),
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = teal
                ),
            contentPadding =
                PaddingValues(
                    horizontal = 4.dp
                )
        ) {

            Text(
                text = text,
                fontSize = 12.sp,
                maxLines = 1
            )
        }

    } else {

        OutlinedButton(
            onClick = onClick,
            modifier =
                modifier.height(52.dp),
            shape =
                RoundedCornerShape(14.dp),
            contentPadding =
                PaddingValues(
                    horizontal = 4.dp
                )
        ) {

            Text(
                text = text,
                fontSize = 12.sp,
                maxLines = 1
            )
        }
    }
}