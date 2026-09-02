package com.example.absenceiq.screens

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@Composable
fun ApplyLeaveScreen(
    onBack: () -> Unit,
    onLeaveSubmitted: () -> Unit
) {

    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    var leaveType by remember { mutableStateOf("Casual Leave") }
    var expanded by remember { mutableStateOf(false) }

    var startDate by remember { mutableStateOf<Date?>(null) }
    var endDate by remember { mutableStateOf<Date?>(null) }

    var reason by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }

    val formatter = remember {
        SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    }

    val duration = if (startDate != null && endDate != null) {
        val difference =
            endDate!!.time - startDate!!.time

        TimeUnit.MILLISECONDS.toDays(difference).toInt() + 1
    } else {
        0
    }

    fun showDatePicker(
        onDateSelected: (Date) -> Unit
    ) {

        val calendar = Calendar.getInstance()

        DatePickerDialog(
            context,
            { _, year, month, day ->

                val selected = Calendar.getInstance()

                selected.set(
                    year,
                    month,
                    day,
                    0,
                    0,
                    0
                )

                selected.set(
                    Calendar.MILLISECOND,
                    0
                )

                onDateSelected(selected.time)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {

        TextButton(
            onClick = onBack
        ) {
            Text("← Back")
        }

        Text(
            text = "Apply for Leave",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(
            modifier = Modifier.height(28.dp)
        )

        Text("Leave Type")

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Box(
            modifier = Modifier.fillMaxWidth()
        ) {

            OutlinedButton(
                onClick = {
                    expanded = true
                },
                modifier = Modifier.fillMaxWidth()
            ) {

                Text(leaveType)
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = {
                    expanded = false
                }
            ) {

                listOf(
                    "Casual Leave",
                    "Medical Leave",
                    "Emergency Leave",
                    "Other"
                ).forEach { type ->

                    DropdownMenuItem(
                        text = {
                            Text(type)
                        },
                        onClick = {
                            leaveType = type
                            expanded = false
                        }
                    )
                }
            }
        }

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.spacedBy(12.dp)
        ) {

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text("Start Date")

                OutlinedButton(
                    onClick = {

                        showDatePicker {
                            startDate = it
                        }

                    },
                    modifier =
                        Modifier.fillMaxWidth()
                ) {

                    Text(
                        startDate?.let {
                            formatter.format(it)
                        } ?: "Select"
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text("End Date")

                OutlinedButton(
                    onClick = {

                        showDatePicker {
                            endDate = it
                        }

                    },
                    modifier =
                        Modifier.fillMaxWidth()
                ) {

                    Text(
                        endDate?.let {
                            formatter.format(it)
                        } ?: "Select"
                    )
                }
            }
        }

        if (duration > 0) {

            Spacer(
                modifier = Modifier.height(14.dp)
            )

            AssistChip(
                onClick = {},
                label = {
                    Text(
                        "Duration: $duration Days"
                    )
                }
            )
        }

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        Text("Reason for Leave")

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        OutlinedTextField(
            value = reason,
            onValueChange = {
                reason = it
            },
            placeholder = {
                Text(
                    "Briefly explain your reason for requesting leave..."
                )
            },
            minLines = 5,
            modifier =
                Modifier.fillMaxWidth()
        )

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        Text("Supporting Document")

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        OutlinedButton(
            onClick = {

                Toast.makeText(
                    context,
                    "Document upload will be connected next",
                    Toast.LENGTH_SHORT
                ).show()

            },
            modifier =
                Modifier.fillMaxWidth()
        ) {

            Text(
                "📎 Attach Supporting Document"
            )
        }

        Text(
            text = "PDF, JPG or PNG",
            style =
                MaterialTheme.typography.bodySmall
        )

        Spacer(
            modifier = Modifier.height(32.dp)
        )

        Button(
            onClick = {

                val uid =
                    auth.currentUser?.uid

                if (uid == null) {

                    Toast.makeText(
                        context,
                        "Please login again",
                        Toast.LENGTH_LONG
                    ).show()

                    return@Button
                }

                if (
                    startDate == null ||
                    endDate == null
                ) {

                    Toast.makeText(
                        context,
                        "Select start and end date",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@Button
                }

                if (
                    endDate!!.before(startDate)
                ) {

                    Toast.makeText(
                        context,
                        "End date cannot be before start date",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@Button
                }

                if (reason.isBlank()) {

                    Toast.makeText(
                        context,
                        "Please enter the reason for leave",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@Button
                }

                isSubmitting = true

                db.collection("users")
                    .document(uid)
                    .get()
                    .addOnSuccessListener { userDocument ->

                        val studentName =
                            userDocument.getString("name")
                                ?: ""

                        val studentId =
                            userDocument.getString("studentId")
                                ?: ""

                        val department =
                            userDocument.getString("department")
                                ?: ""

                        val request =
                            hashMapOf(

                                "studentUid" to uid,

                                "studentName"
                                        to studentName,

                                "studentId"
                                        to studentId,

                                "department"
                                        to department,

                                "leaveType"
                                        to leaveType,

                                "startDate"
                                        to startDate,

                                "endDate"
                                        to endDate,

                                "duration"
                                        to duration,

                                "reason"
                                        to reason.trim(),

                                "status"
                                        to "pending",

                                "facultyStatus"
                                        to "pending",

                                "adminStatus"
                                        to "waiting",

                                "facultyRemarks"
                                        to "",

                                "adminRemarks"
                                        to "",

                                "supportingDocumentUrl"
                                        to "",

                                "createdAt"
                                        to FieldValue.serverTimestamp(),

                                "updatedAt"
                                        to FieldValue.serverTimestamp()
                            )

                        db.collection(
                            "leave_requests"
                        )
                            .add(request)

                            .addOnSuccessListener {

                                isSubmitting = false

                                Toast.makeText(
                                    context,
                                    "Leave request submitted successfully",
                                    Toast.LENGTH_LONG
                                ).show()

                                onLeaveSubmitted()
                            }

                            .addOnFailureListener { error ->

                                isSubmitting = false

                                Toast.makeText(
                                    context,
                                    "Failed: ${error.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                    }

                    .addOnFailureListener {

                        isSubmitting = false

                        Toast.makeText(
                            context,
                            "Unable to load student information",
                            Toast.LENGTH_LONG
                        ).show()
                    }
            },

            enabled = !isSubmitting,

            modifier =
                Modifier.fillMaxWidth()
        ) {

            if (isSubmitting) {

                CircularProgressIndicator(
                    modifier =
                        Modifier.size(20.dp)
                )

            } else {

                Text(
                    "Submit Leave Request"
                )
            }
        }

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        Text(
            text =
                "Faculty / SSO reviews before final HOD approval.",
            style =
                MaterialTheme.typography.bodyMedium
        )

        Spacer(
            modifier = Modifier.height(40.dp)
        )
    }
}