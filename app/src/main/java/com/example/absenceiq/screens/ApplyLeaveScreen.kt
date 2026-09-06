package com.example.absenceiq.screens

import android.app.DatePickerDialog
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit


@Composable
fun ApplyLeaveScreen(
    onBack: () -> Unit,
    onLeaveSubmitted: () -> Unit
) {

    val context = LocalContext.current

    val auth =
        FirebaseAuth.getInstance()

    val db =
        FirebaseFirestore.getInstance()


    /*
     * FORM STATE
     */
    var leaveType by remember {
        mutableStateOf("Casual Leave")
    }

    var expanded by remember {
        mutableStateOf(false)
    }

    var startDate by remember {
        mutableStateOf<Date?>(null)
    }

    var endDate by remember {
        mutableStateOf<Date?>(null)
    }

    var reason by remember {
        mutableStateOf("")
    }


    /*
     * Supporting document.
     *
     * Because Firebase Storage requires billing,
     * we only store the selected filename for now.
     */
    var selectedDocumentUri by remember {
        mutableStateOf<Uri?>(null)
    }

    var selectedDocumentName by remember {
        mutableStateOf("")
    }


    /*
     * Loading / error state
     */
    var isSubmitting by remember {
        mutableStateOf(false)
    }

    var errorMessage by remember {
        mutableStateOf("")
    }


    val formatter =
        remember {

            SimpleDateFormat(
                "dd MMM yyyy",
                Locale.getDefault()
            )
        }


    /*
     * Calculate duration.
     *
     * +1 because a leave from 5 Sep to 5 Sep
     * is one day, not zero days.
     */
    val duration =
        if (
            startDate != null &&
            endDate != null
        ) {

            val difference =
                endDate!!.time -
                        startDate!!.time

            TimeUnit.MILLISECONDS
                .toDays(
                    difference
                )
                .toInt() + 1

        } else {

            0
        }


    /*
     * LOCAL FILE PICKER
     *
     * No Firebase Storage upload.
     */
    val documentLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts.OpenDocument()
        ) { uri ->

            if (uri != null) {

                selectedDocumentUri =
                    uri

                /*
                 * Obtain readable filename.
                 */
                val cursor =
                    context.contentResolver
                        .query(
                            uri,
                            null,
                            null,
                            null,
                            null
                        )

                selectedDocumentName =
                    cursor?.use {

                        val nameIndex =
                            it.getColumnIndex(
                                OpenableColumns.DISPLAY_NAME
                            )

                        if (
                            nameIndex >= 0 &&
                            it.moveToFirst()
                        ) {

                            it.getString(
                                nameIndex
                            )

                        } else {

                            "Selected document"
                        }

                    } ?: "Selected document"
            }
        }


    /*
     * DATE PICKER
     */
    fun showDatePicker(
        onDateSelected: (Date) -> Unit
    ) {

        val calendar =
            Calendar.getInstance()

        val dialog =
            DatePickerDialog(

                context,

                { _, year, month, day ->

                    val selected =
                        Calendar.getInstance()

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

                    onDateSelected(
                        selected.time
                    )
                },

                calendar.get(
                    Calendar.YEAR
                ),

                calendar.get(
                    Calendar.MONTH
                ),

                calendar.get(
                    Calendar.DAY_OF_MONTH
                )
            )

        /*
         * Do not allow past dates.
         */
        dialog.datePicker.minDate =
            Calendar.getInstance()
                .apply {

                    set(
                        Calendar.HOUR_OF_DAY,
                        0
                    )

                    set(
                        Calendar.MINUTE,
                        0
                    )

                    set(
                        Calendar.SECOND,
                        0
                    )

                    set(
                        Calendar.MILLISECOND,
                        0
                    )
                }
                .timeInMillis

        dialog.show()
    }


    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(
                    rememberScrollState()
                )
                .padding(
                    24.dp
                )
    ) {

        /*
         * BACK
         */
        TextButton(
            enabled =
                !isSubmitting,

            onClick =
                onBack
        ) {

            Text("← Back")
        }


        Text(
            text =
                "Apply for Leave",

            style =
                MaterialTheme
                    .typography
                    .headlineMedium
        )


        Spacer(
            Modifier.height(
                28.dp
            )
        )


        /*
         * LEAVE TYPE
         */
        Text("Leave Type")


        Spacer(
            Modifier.height(
                8.dp
            )
        )


        Box(
            modifier =
                Modifier.fillMaxWidth()
        ) {

            OutlinedButton(
                enabled =
                    !isSubmitting,

                onClick = {
                    expanded = true
                },

                modifier =
                    Modifier.fillMaxWidth()
            ) {

                Text(
                    leaveType
                )
            }


            DropdownMenu(
                expanded =
                    expanded,

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

                            leaveType =
                                type

                            expanded =
                                false

                            errorMessage =
                                ""
                        }
                    )
                }
            }
        }


        Spacer(
            Modifier.height(
                20.dp
            )
        )


        /*
         * DATES
         */
        Row(
            modifier =
                Modifier.fillMaxWidth(),

            horizontalArrangement =
                Arrangement.spacedBy(
                    12.dp
                )
        ) {

            /*
             * START DATE
             */
            Column(
                modifier =
                    Modifier.weight(
                        1f
                    )
            ) {

                Text(
                    "Start Date"
                )


                OutlinedButton(
                    enabled =
                        !isSubmitting,

                    onClick = {

                        showDatePicker {

                            startDate =
                                it

                            /*
                             * Reset end date if
                             * it becomes invalid.
                             */
                            if (
                                endDate != null &&
                                endDate!!.before(it)
                            ) {

                                endDate =
                                    null
                            }

                            errorMessage =
                                ""
                        }
                    },

                    modifier =
                        Modifier.fillMaxWidth()
                ) {

                    Text(
                        startDate
                            ?.let {
                                formatter.format(
                                    it
                                )
                            }
                            ?: "Select"
                    )
                }
            }


            /*
             * END DATE
             */
            Column(
                modifier =
                    Modifier.weight(
                        1f
                    )
            ) {

                Text(
                    "End Date"
                )


                OutlinedButton(
                    enabled =
                        !isSubmitting,

                    onClick = {

                        showDatePicker {

                            endDate =
                                it

                            errorMessage =
                                ""
                        }
                    },

                    modifier =
                        Modifier.fillMaxWidth()
                ) {

                    Text(
                        endDate
                            ?.let {
                                formatter.format(
                                    it
                                )
                            }
                            ?: "Select"
                    )
                }
            }
        }


        /*
         * DURATION
         */
        if (
            duration > 0
        ) {

            Spacer(
                Modifier.height(
                    14.dp
                )
            )

            AssistChip(
                onClick = { },

                label = {

                    Text(
                        "Duration: $duration Day${
                            if (
                                duration == 1
                            ) {
                                ""
                            } else {
                                "s"
                            }
                        }"
                    )
                }
            )
        }


        Spacer(
            Modifier.height(
                24.dp
            )
        )


        /*
         * REASON
         */
        Text(
            "Reason for Leave"
        )


        Spacer(
            Modifier.height(
                8.dp
            )
        )


        OutlinedTextField(
            value =
                reason,

            enabled =
                !isSubmitting,

            onValueChange = {

                /*
                 * Prevent unnecessarily huge text.
                 */
                if (
                    it.length <= 500
                ) {

                    reason =
                        it

                    errorMessage =
                        ""
                }
            },

            placeholder = {

                Text(
                    "Briefly explain your reason for requesting leave..."
                )
            },

            supportingText = {

                Text(
                    "${reason.length}/500"
                )
            },

            minLines =
                5,

            modifier =
                Modifier.fillMaxWidth()
        )


        Spacer(
            Modifier.height(
                24.dp
            )
        )


        /*
         * SUPPORTING DOCUMENT
         */
        Text(
            "Supporting Document"
        )


        Spacer(
            Modifier.height(
                8.dp
            )
        )


        OutlinedButton(
            enabled =
                !isSubmitting,

            onClick = {

                documentLauncher.launch(
                    arrayOf(
                        "application/pdf",
                        "image/jpeg",
                        "image/png"
                    )
                )
            },

            modifier =
                Modifier.fillMaxWidth()
        ) {

            Text(
                if (
                    selectedDocumentName
                        .isBlank()
                ) {

                    "📎 Attach Supporting Document"

                } else {

                    "📎 Change Document"
                }
            )
        }


        if (
            selectedDocumentName
                .isNotBlank()
        ) {

            Spacer(
                Modifier.height(
                    6.dp
                )
            )

            Text(
                text =
                    "Selected: $selectedDocumentName",

                style =
                    MaterialTheme
                        .typography
                        .bodyMedium
            )
        }


        Text(
            text =
                "PDF, JPG or PNG",

            style =
                MaterialTheme
                    .typography
                    .bodySmall,

            color =
                Color.Gray
        )


        Spacer(
            Modifier.height(
                24.dp
            )
        )


        /*
         * ERROR MESSAGE
         */
        if (
            errorMessage
                .isNotBlank()
        ) {

            Card(
                colors =
                    CardDefaults.cardColors(
                        containerColor =
                            MaterialTheme
                                .colorScheme
                                .errorContainer
                    ),

                modifier =
                    Modifier.fillMaxWidth()
            ) {

                Text(
                    text =
                        errorMessage,

                    color =
                        MaterialTheme
                            .colorScheme
                            .onErrorContainer,

                    modifier =
                        Modifier.padding(
                            12.dp
                        )
                )
            }


            Spacer(
                Modifier.height(
                    16.dp
                )
            )
        }


        /*
         * SUBMIT
         */
        Button(

            enabled =
                !isSubmitting,

            modifier =
                Modifier.fillMaxWidth(),

            onClick = {

                /*
                 * Clear previous error.
                 */
                errorMessage =
                    ""


                /*
                 * AUTH VALIDATION
                 */
                val uid =
                    auth.currentUser
                        ?.uid


                if (
                    uid == null
                ) {

                    errorMessage =
                        "Your login session has expired. Please log in again."

                    return@Button
                }


                /*
                 * LEAVE TYPE VALIDATION
                 */
                if (
                    leaveType.isBlank()
                ) {

                    errorMessage =
                        "Please select a leave type."

                    return@Button
                }


                /*
                 * DATE VALIDATION
                 */
                if (
                    startDate == null
                ) {

                    errorMessage =
                        "Please select a start date."

                    return@Button
                }


                if (
                    endDate == null
                ) {

                    errorMessage =
                        "Please select an end date."

                    return@Button
                }


                if (
                    endDate!!.before(
                        startDate!!
                    )
                ) {

                    errorMessage =
                        "End date cannot be before the start date."

                    return@Button
                }


                /*
                 * DURATION VALIDATION
                 */
                if (
                    duration <= 0
                ) {

                    errorMessage =
                        "Leave duration is invalid."

                    return@Button
                }


                /*
                 * REASON VALIDATION
                 */
                if (
                    reason.trim()
                        .length < 5
                ) {

                    errorMessage =
                        "Please provide a meaningful reason for leave."

                    return@Button
                }


                /*
                 * Begin submission.
                 */
                isSubmitting =
                    true


                /*
                 * Load user profile and balance first.
                 */
                db.collection(
                    "users"
                )
                    .document(
                        uid
                    )
                    .get()

                    .addOnSuccessListener {
                            userDocument ->


                        /*
                         * Make sure profile exists.
                         */
                        if (
                            !userDocument.exists()
                        ) {

                            isSubmitting =
                                false

                            errorMessage =
                                "Student profile could not be found."

                            return@addOnSuccessListener
                        }


                        val studentName =
                            userDocument
                                .getString(
                                    "name"
                                ) ?: ""


                        val studentId =
                            userDocument
                                .getString(
                                    "studentId"
                                ) ?: ""


                        val department =
                            userDocument
                                .getString(
                                    "department"
                                ) ?: ""


                        /*
                         * Validate profile.
                         */
                        if (
                            studentName.isBlank() ||
                            studentId.isBlank() ||
                            department.isBlank()
                        ) {

                            isSubmitting =
                                false

                            errorMessage =
                                "Your student profile is incomplete."

                            return@addOnSuccessListener
                        }


                        /*
                         * LEAVE BALANCE VALIDATION
                         */
                        val leaveBalance =
                            userDocument
                                .getLong(
                                    "leaveBalance"
                                )


                        if (
                            leaveBalance == null
                        ) {

                            isSubmitting =
                                false

                            errorMessage =
                                "Your leave balance has not been assigned. Please contact the administrator."

                            return@addOnSuccessListener
                        }


                        if (
                            duration.toLong() >
                            leaveBalance
                        ) {

                            isSubmitting =
                                false

                            errorMessage =
                                "Insufficient leave balance. You have $leaveBalance day(s) remaining."

                            return@addOnSuccessListener
                        }


                        /*
                         * CREATE LEAVE REQUEST
                         */
                        val request =
                            hashMapOf<String, Any>(

                                "studentUid"
                                        to uid,

                                "studentName"
                                        to studentName,

                                "studentId"
                                        to studentId,

                                "department"
                                        to department,

                                "leaveType"
                                        to leaveType,

                                "startDate"
                                        to startDate!!,

                                "endDate"
                                        to endDate!!,

                                "duration"
                                        to duration.toLong(),

                                "reason"
                                        to reason.trim(),

                                /*
                                 * Workflow status
                                 */
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

                                /*
                                 * Leave-balance safety.
                                 */
                                "balanceDeducted"
                                        to false,

                                /*
                                 * Local supporting-document information.
                                 *
                                 * No Storage URL because
                                 * Firebase Storage is not enabled.
                                 */
                                "supportingDocumentName"
                                        to selectedDocumentName,

                                "supportingDocumentUrl"
                                        to "",

                                /*
                                 * Timestamps
                                 */
                                "createdAt"
                                        to FieldValue
                                    .serverTimestamp(),

                                "updatedAt"
                                        to FieldValue
                                    .serverTimestamp()
                            )


                        db.collection(
                            "leave_requests"
                        )
                            .add(
                                request
                            )

                            .addOnSuccessListener {

                                isSubmitting =
                                    false

                                Toast.makeText(
                                    context,
                                    "Leave request submitted successfully",
                                    Toast.LENGTH_LONG
                                ).show()

                                onLeaveSubmitted()
                            }

                            .addOnFailureListener {
                                    error ->

                                isSubmitting =
                                    false

                                errorMessage =
                                    "Unable to submit leave request: ${
                                        error.message
                                            ?: "Unknown error"
                                    }"
                            }
                    }

                    .addOnFailureListener {
                            error ->

                        isSubmitting =
                            false

                        errorMessage =
                            "Unable to load student information: ${
                                error.message
                                    ?: "Unknown error"
                            }"
                    }
            }
        ) {

            if (
                isSubmitting
            ) {

                Row(
                    verticalAlignment =
                        Alignment.CenterVertically,

                    horizontalArrangement =
                        Arrangement.Center
                ) {

                    CircularProgressIndicator(
                        modifier =
                            Modifier.size(
                                20.dp
                            ),

                        strokeWidth =
                            2.dp,

                        color =
                            Color.White
                    )


                    Spacer(
                        Modifier.width(
                            10.dp
                        )
                    )


                    Text(
                        "Submitting..."
                    )
                }

            } else {

                Text(
                    "Submit Leave Request"
                )
            }
        }


        Spacer(
            Modifier.height(
                12.dp
            )
        )


        Text(
            text =
                "Faculty / SSO reviews your request before final HOD / Admin approval.",

            style =
                MaterialTheme
                    .typography
                    .bodyMedium
        )


        Spacer(
            Modifier.height(
                40.dp
            )
        )
    }
}