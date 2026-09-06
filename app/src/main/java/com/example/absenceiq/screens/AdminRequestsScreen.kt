package com.example.absenceiq.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale


data class AdminRequestItem(
    val id: String = "",
    val studentUid: String = "",
    val studentName: String = "",
    val studentId: String = "",
    val department: String = "",
    val leaveType: String = "",
    val reason: String = "",
    val duration: Long = 0,
    val startDate: Timestamp? = null,
    val endDate: Timestamp? = null,
    val facultyStatus: String = "",
    val facultyRemarks: String = "",
    val adminStatus: String = "pending",
    val adminRemarks: String = "",
    val status: String = "pending",
    val createdAt: Timestamp? = null,
    val balanceDeducted: Boolean = false
)


@Composable
fun AdminRequestsScreen(
    onBack: () -> Unit
) {

    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    val uid = auth.currentUser?.uid

    var requests by remember {
        mutableStateOf<List<AdminRequestItem>>(emptyList())
    }

    var selectedFilter by remember {
        mutableStateOf("All")
    }

    var selectedRequest by remember {
        mutableStateOf<AdminRequestItem?>(null)
    }

    var showDetailsDialog by remember {
        mutableStateOf(false)
    }

    var remarks by remember {
        mutableStateOf("")
    }

    var isLoading by remember {
        mutableStateOf(true)
    }

    val teal = Color(0xFF0B747D)
    val background = Color(0xFFF5F7FA)


    /*
     * HOD/Admin only sees requests
     * already approved by Faculty / SSO.
     */
    DisposableEffect(Unit) {

        val listener =
            db.collection("leave_requests")
                .whereEqualTo(
                    "facultyStatus",
                    "approved"
                )
                .addSnapshotListener { snapshot, error ->

                    if (error != null) {

                        isLoading = false

                        Toast.makeText(
                            context,
                            "Unable to load requests: ${error.message}",
                            Toast.LENGTH_LONG
                        ).show()

                        return@addSnapshotListener
                    }

                    requests =
                        snapshot?.documents
                            ?.map { document ->

                                AdminRequestItem(

                                    id =
                                        document.id,

                                    studentUid =
                                        document.getString(
                                            "studentUid"
                                        ) ?: "",

                                    studentName =
                                        document.getString(
                                            "studentName"
                                        ) ?: "",

                                    studentId =
                                        document.getString(
                                            "studentId"
                                        ) ?: "",

                                    department =
                                        document.getString(
                                            "department"
                                        ) ?: "",

                                    leaveType =
                                        document.getString(
                                            "leaveType"
                                        ) ?: "",

                                    reason =
                                        document.getString(
                                            "reason"
                                        ) ?: "",

                                    duration =
                                        document.getLong(
                                            "duration"
                                        ) ?: 0L,

                                    startDate =
                                        document.getTimestamp(
                                            "startDate"
                                        ),

                                    endDate =
                                        document.getTimestamp(
                                            "endDate"
                                        ),

                                    facultyStatus =
                                        document.getString(
                                            "facultyStatus"
                                        ) ?: "",

                                    facultyRemarks =
                                        document.getString(
                                            "facultyRemarks"
                                        ) ?: "",

                                    adminStatus =
                                        document.getString(
                                            "adminStatus"
                                        ) ?: "pending",

                                    adminRemarks =
                                        document.getString(
                                            "adminRemarks"
                                        ) ?: "",

                                    status =
                                        document.getString(
                                            "status"
                                        ) ?: "pending",

                                    createdAt =
                                        document.getTimestamp(
                                            "createdAt"
                                        ),

                                    balanceDeducted =
                                        document.getBoolean(
                                            "balanceDeducted"
                                        ) ?: false
                                )
                            }
                            ?.sortedByDescending {
                                it.createdAt?.seconds ?: 0
                            }
                            ?: emptyList()

                    isLoading = false
                }

        onDispose {
            listener.remove()
        }
    }


    /*
     * Filters
     */
    val filteredRequests =
        when (selectedFilter) {

            "Pending" ->
                requests.filter {
                    it.adminStatus.equals(
                        "pending",
                        ignoreCase = true
                    )
                }

            "Approved" ->
                requests.filter {
                    it.adminStatus.equals(
                        "approved",
                        ignoreCase = true
                    )
                }

            "Rejected" ->
                requests.filter {
                    it.adminStatus.equals(
                        "rejected",
                        ignoreCase = true
                    )
                }

            else ->
                requests
        }


    /*
     * View request / final review dialog
     */
    if (
        showDetailsDialog &&
        selectedRequest != null
    ) {

        val request =
            selectedRequest!!

        val formatter =
            remember {
                SimpleDateFormat(
                    "dd MMM yyyy",
                    Locale.getDefault()
                )
            }

        val startDateText =
            request.startDate
                ?.toDate()
                ?.let {
                    formatter.format(it)
                }
                ?: "-"

        val endDateText =
            request.endDate
                ?.toDate()
                ?.let {
                    formatter.format(it)
                }
                ?: "-"


        AlertDialog(

            onDismissRequest = {

                showDetailsDialog = false
                selectedRequest = null
                remarks = ""
            },

            title = {

                Text(
                    text = "Leave Request",
                    fontWeight = FontWeight.Bold
                )
            },

            text = {

                Column(
                    modifier =
                        Modifier.verticalScroll(
                            rememberScrollState()
                        )
                ) {

                    Text(
                        text =
                            request.studentName,

                        fontSize =
                            19.sp,

                        fontWeight =
                            FontWeight.Bold
                    )

                    Text(
                        text =
                            "${request.studentId} • ${request.department}",

                        color =
                            Color.Gray
                    )

                    Spacer(
                        Modifier.height(14.dp)
                    )


                    /*
                     * Leave details
                     */
                    Text(
                        text =
                            "Leave Details",

                        fontSize =
                            18.sp,

                        fontWeight =
                            FontWeight.Bold
                    )

                    Spacer(
                        Modifier.height(8.dp)
                    )

                    Text(
                        "Leave Type: ${request.leaveType}"
                    )

                    Text(
                        "Start Date: $startDateText"
                    )

                    Text(
                        "End Date: $endDateText"
                    )

                    Text(
                        "Duration: ${request.duration} Days"
                    )

                    Spacer(
                        Modifier.height(14.dp)
                    )

                    Text(
                        text = "Reason",
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        request.reason
                    )


                    /*
                     * Faculty review
                     */
                    Spacer(
                        Modifier.height(16.dp)
                    )

                    Text(
                        text =
                            "Faculty / SSO Review",

                        fontWeight =
                            FontWeight.Bold
                    )

                    Text(
                        text =
                            "Status: ${
                                request.facultyStatus
                                    .replaceFirstChar {
                                        it.uppercase()
                                    }
                            }"
                    )

                    if (
                        request.facultyRemarks
                            .isNotBlank()
                    ) {

                        Text(
                            "Remarks: ${request.facultyRemarks}"
                        )
                    }


                    Spacer(
                        Modifier.height(18.dp)
                    )


                    /*
                     * HOD/Admin section
                     */
                    if (
                        request.adminStatus.equals(
                            "pending",
                            ignoreCase = true
                        )
                    ) {

                        Text(
                            text =
                                "HOD / Admin Remarks",

                            fontWeight =
                                FontWeight.Bold
                        )

                        Spacer(
                            Modifier.height(8.dp)
                        )

                        OutlinedTextField(
                            value =
                                remarks,

                            onValueChange = {
                                remarks = it
                            },

                            placeholder = {
                                Text(
                                    "Add remarks for the student..."
                                )
                            },

                            minLines =
                                3,

                            modifier =
                                Modifier.fillMaxWidth()
                        )

                    } else {

                        Text(
                            text =
                                "HOD / Admin Decision",

                            fontWeight =
                                FontWeight.Bold
                        )

                        Text(
                            text =
                                "Status: ${
                                    request.adminStatus
                                        .replaceFirstChar {
                                            it.uppercase()
                                        }
                                }"
                        )

                        if (
                            request.adminRemarks
                                .isNotBlank()
                        ) {

                            Text(
                                "Remarks: ${request.adminRemarks}"
                            )
                        }
                    }
                }
            },


            /*
             * APPROVE
             */
            confirmButton = {

                if (
                    request.adminStatus.equals(
                        "pending",
                        ignoreCase = true
                    )
                ) {

                    Button(

                        onClick = {

                            updateAdminRequestDecision(
                                db = db,
                                request = request,
                                uid = uid,
                                remarks = remarks,
                                action = "approve",
                                context = context
                            ) {

                                showDetailsDialog =
                                    false

                                selectedRequest =
                                    null

                                remarks =
                                    ""
                            }
                        },

                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor =
                                    teal
                            )
                    ) {

                        Text("Approve")
                    }

                } else {

                    TextButton(
                        onClick = {

                            showDetailsDialog =
                                false

                            selectedRequest =
                                null

                            remarks =
                                ""
                        }
                    ) {

                        Text("Close")
                    }
                }
            },


            /*
             * REJECT
             */
            dismissButton = {

                if (
                    request.adminStatus.equals(
                        "pending",
                        ignoreCase = true
                    )
                ) {

                    OutlinedButton(

                        onClick = {

                            if (
                                remarks.isBlank()
                            ) {

                                Toast.makeText(
                                    context,
                                    "Please provide rejection remarks",
                                    Toast.LENGTH_SHORT
                                ).show()

                                return@OutlinedButton
                            }

                            updateAdminRequestDecision(
                                db = db,
                                request = request,
                                uid = uid,
                                remarks = remarks,
                                action = "reject",
                                context = context
                            ) {

                                showDetailsDialog =
                                    false

                                selectedRequest =
                                    null

                                remarks =
                                    ""
                            }
                        }
                    ) {

                        Text(
                            text =
                                "Reject",

                            color =
                                Color(0xFFC62828)
                        )
                    }
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
                            text =
                                "← Back",

                            color =
                                Color.White
                        )
                    }

                    Spacer(
                        Modifier.width(8.dp)
                    )

                    Text(
                        text =
                            "Leave Requests",

                        color =
                            Color.White,

                        fontSize =
                            22.sp,

                        fontWeight =
                            FontWeight.Bold
                    )
                }
            }
        }

    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    background
                )
                .verticalScroll(
                    rememberScrollState()
                )
                .padding(
                    padding
                )
                .padding(
                    18.dp
                )
        ) {

            /*
             * Filters
             */
            Row(
                horizontalArrangement =
                    Arrangement.spacedBy(
                        8.dp
                    )
            ) {

                listOf(
                    "All",
                    "Pending",
                    "Approved",
                    "Rejected"
                ).forEach { item ->

                    FilterChip(
                        selected =
                            selectedFilter ==
                                    item,

                        onClick = {
                            selectedFilter =
                                item
                        },

                        label = {
                            Text(item)
                        }
                    )
                }
            }


            Spacer(
                Modifier.height(20.dp)
            )


            if (isLoading) {

                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                40.dp
                            ),

                    contentAlignment =
                        Alignment.Center
                ) {

                    CircularProgressIndicator()
                }

            } else if (
                filteredRequests
                    .isEmpty()
            ) {

                Card(
                    modifier =
                        Modifier.fillMaxWidth(),

                    shape =
                        RoundedCornerShape(
                            16.dp
                        ),

                    colors =
                        CardDefaults.cardColors(
                            containerColor =
                                Color.White
                        )
                ) {

                    Text(
                        text =
                            "No leave requests found.",

                        modifier =
                            Modifier.padding(
                                24.dp
                            ),

                        color =
                            Color.Gray
                    )
                }

            } else {

                filteredRequests
                    .forEach { request ->

                        AdminRequestListCard(
                            request =
                                request,

                            onViewRequest = {

                                selectedRequest =
                                    request

                                remarks =
                                    ""

                                showDetailsDialog =
                                    true
                            }
                        )

                        Spacer(
                            Modifier.height(
                                14.dp
                            )
                        )
                    }
            }
        }
    }
}


@Composable
fun AdminRequestListCard(
    request: AdminRequestItem,
    onViewRequest: () -> Unit
) {

    val formatter =
        remember {
            SimpleDateFormat(
                "dd MMM yyyy",
                Locale.getDefault()
            )
        }

    val statusColor =
        when (
            request.adminStatus.lowercase()
        ) {

            "approved" ->
                Color(0xFF2E9E72)

            "rejected" ->
                Color(0xFFD74444)

            else ->
                Color(0xFFE49317)
        }


    val statusBackground =
        when (
            request.adminStatus.lowercase()
        ) {

            "approved" ->
                Color(0xFFE4F5EC)

            "rejected" ->
                Color(0xFFFDEBEC)

            else ->
                Color(0xFFFFF1DB)
        }


    Card(
        modifier =
            Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(
                16.dp
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
                    18.dp
                )
        ) {

            Row(
                modifier =
                    Modifier.fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.SpaceBetween,

                verticalAlignment =
                    Alignment.Top
            ) {

                Column(
                    modifier =
                        Modifier.weight(
                            1f
                        )
                ) {

                    Text(
                        text =
                            request.studentName,

                        fontSize =
                            18.sp,

                        fontWeight =
                            FontWeight.Bold
                    )

                    Spacer(
                        Modifier.height(4.dp)
                    )

                    Text(
                        text =
                            "${request.studentId} • ${request.department}",

                        color =
                            Color.Gray,

                        fontSize =
                            13.sp
                    )
                }


                Surface(
                    shape =
                        RoundedCornerShape(
                            20.dp
                        ),

                    color =
                        statusBackground
                ) {

                    Text(
                        text =
                            request.adminStatus
                                .replaceFirstChar {
                                    it.uppercase()
                                },

                        color =
                            statusColor,

                        fontWeight =
                            FontWeight.Bold,

                        fontSize =
                            12.sp,

                        modifier =
                            Modifier.padding(
                                horizontal = 12.dp,
                                vertical = 6.dp
                            )
                    )
                }
            }


            Spacer(
                Modifier.height(14.dp)
            )


            Text(
                text =
                    request.leaveType,

                fontWeight =
                    FontWeight.Bold
            )


            Spacer(
                Modifier.height(6.dp)
            )


            val start =
                request.startDate
                    ?.toDate()
                    ?.let {
                        formatter.format(it)
                    }
                    ?: "-"


            val end =
                request.endDate
                    ?.toDate()
                    ?.let {
                        formatter.format(it)
                    }
                    ?: "-"


            Text(
                "$start - $end • ${request.duration} day(s)"
            )


            Spacer(
                Modifier.height(10.dp)
            )


            Text(
                "Reason: ${request.reason}"
            )


            Spacer(
                Modifier.height(16.dp)
            )


            Button(
                onClick =
                    onViewRequest,

                modifier =
                    Modifier.fillMaxWidth(),

                colors =
                    ButtonDefaults.buttonColors(
                        containerColor =
                            Color(0xFF0B747D)
                    )
            ) {

                Text(
                    "View Request"
                )
            }
        }
    }
}


/*
 * Final HOD/Admin decision from
 * AdminRequestsScreen.
 */
private fun updateAdminRequestDecision(
    db: FirebaseFirestore,
    request: AdminRequestItem,
    uid: String?,
    remarks: String,
    action: String,
    context: android.content.Context,
    onComplete: () -> Unit
) {

    /*
     * APPROVE
     */
    if (
        action == "approve"
    ) {

        if (
            request.studentUid
                .isBlank()
        ) {

            Toast.makeText(
                context,
                "Student account could not be identified",
                Toast.LENGTH_LONG
            ).show()

            return
        }


        val requestRef =
            db.collection(
                "leave_requests"
            )
                .document(
                    request.id
                )


        val studentRef =
            db.collection(
                "users"
            )
                .document(
                    request.studentUid
                )


        db.runTransaction { transaction ->

            /*
             * Read before writing.
             */
            val requestSnapshot =
                transaction.get(
                    requestRef
                )

            val studentSnapshot =
                transaction.get(
                    studentRef
                )


            /*
             * Faculty must have approved first.
             */
            val facultyStatus =
                requestSnapshot.getString(
                    "facultyStatus"
                ) ?: ""


            if (
                !facultyStatus.equals(
                    "approved",
                    ignoreCase = true
                )
            ) {

                throw Exception(
                    "Faculty approval is required"
                )
            }


            /*
             * Prevent second Admin decision.
             */
            val currentAdminStatus =
                requestSnapshot.getString(
                    "adminStatus"
                ) ?: "pending"


            if (
                !currentAdminStatus.equals(
                    "pending",
                    ignoreCase = true
                )
            ) {

                throw Exception(
                    "This request has already been reviewed"
                )
            }


            /*
             * Prevent balance deduction twice.
             */
            val alreadyDeducted =
                requestSnapshot.getBoolean(
                    "balanceDeducted"
                ) ?: false


            if (alreadyDeducted) {

                throw Exception(
                    "Leave balance has already been deducted"
                )
            }


            /*
             * Read student's balance.
             */
            val currentBalance =
                studentSnapshot.getLong(
                    "leaveBalance"
                )
                    ?: throw Exception(
                        "Student leave balance is not assigned"
                    )


            /*
             * Read actual duration from Firestore.
             */
            val duration =
                requestSnapshot.getLong(
                    "duration"
                )
                    ?: request.duration


            if (
                duration <= 0
            ) {

                throw Exception(
                    "Invalid leave duration"
                )
            }


            if (
                currentBalance <
                duration
            ) {

                throw Exception(
                    "Insufficient leave balance. Available: $currentBalance day(s)"
                )
            }


            val newBalance =
                currentBalance -
                        duration


            /*
             * Deduct student's leave balance.
             */
            transaction.update(
                studentRef,
                "leaveBalance",
                newBalance
            )


            /*
             * Approve the leave request and
             * mark balance as already deducted.
             */
            transaction.update(
                requestRef,

                mapOf(

                    "adminStatus"
                            to "approved",

                    "status"
                            to "approved",

                    "adminRemarks"
                            to remarks.trim(),

                    "adminReviewedBy"
                            to (uid ?: ""),

                    "adminReviewedAt"
                            to FieldValue.serverTimestamp(),

                    "updatedAt"
                            to FieldValue.serverTimestamp(),

                    "balanceDeducted"
                            to true
                )
            )


            newBalance
        }

            .addOnSuccessListener { newBalance ->

                createAdminRequestNotification(
                    db =
                        db,

                    request =
                        request,

                    action =
                        "approve",

                    context =
                        context
                ) {

                    Toast.makeText(
                        context,
                        "Leave approved. Remaining balance: $newBalance day(s)",
                        Toast.LENGTH_LONG
                    ).show()

                    onComplete()
                }
            }

            .addOnFailureListener { error ->

                Toast.makeText(
                    context,
                    "Approval failed: ${error.message}",
                    Toast.LENGTH_LONG
                ).show()
            }


    } else {

        /*
         * REJECT
         *
         * Rejection does NOT deduct
         * leave balance.
         */

        val requestRef =
            db.collection(
                "leave_requests"
            )
                .document(
                    request.id
                )


        db.runTransaction { transaction ->

            val snapshot =
                transaction.get(
                    requestRef
                )


            val currentStatus =
                snapshot.getString(
                    "adminStatus"
                ) ?: "pending"


            if (
                !currentStatus.equals(
                    "pending",
                    ignoreCase = true
                )
            ) {

                throw Exception(
                    "This request has already been reviewed"
                )
            }


            transaction.update(
                requestRef,

                mapOf(

                    "adminStatus"
                            to "rejected",

                    "status"
                            to "rejected",

                    "adminRemarks"
                            to remarks.trim(),

                    "adminReviewedBy"
                            to (uid ?: ""),

                    "adminReviewedAt"
                            to FieldValue.serverTimestamp(),

                    "updatedAt"
                            to FieldValue.serverTimestamp()
                )
            )
        }

            .addOnSuccessListener {

                createAdminRequestNotification(
                    db =
                        db,

                    request =
                        request,

                    action =
                        "reject",

                    context =
                        context,

                    onComplete =
                        onComplete
                )
            }

            .addOnFailureListener { error ->

                Toast.makeText(
                    context,
                    "Rejection failed: ${error.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }
}


/*
 * Create the student's notification
 * after the final decision succeeds.
 */
private fun createAdminRequestNotification(
    db: FirebaseFirestore,
    request: AdminRequestItem,
    action: String,
    context: android.content.Context,
    onComplete: () -> Unit
) {

    val notificationTitle =
        if (
            action == "approve"
        ) {

            "Leave Approved"

        } else {

            "Leave Rejected"
        }


    val notificationMessage =
        if (
            action == "approve"
        ) {

            "Your ${request.leaveType} request has been approved."

        } else {

            "Your ${request.leaveType} request has been rejected. Tap to view remarks."
        }


    val notificationType =
        if (
            action == "approve"
        ) {

            "approved"

        } else {

            "rejected"
        }


    val notification =
        hashMapOf(

            "userUid"
                    to request.studentUid,

            "title"
                    to notificationTitle,

            "message"
                    to notificationMessage,

            "type"
                    to notificationType,

            "leaveRequestId"
                    to request.id,

            "isRead"
                    to false,

            "createdAt"
                    to FieldValue.serverTimestamp()
        )


    db.collection(
        "com/example/absenceiq/notifications"
    )
        .add(
            notification
        )

        .addOnSuccessListener {

            onComplete()
        }

        .addOnFailureListener { error ->

            /*
             * Final decision was already saved,
             * so do not reverse it just because
             * notification creation failed.
             */
            Toast.makeText(
                context,
                "Decision saved, but notification failed: ${error.message}",
                Toast.LENGTH_LONG
            ).show()

            onComplete()
        }
}