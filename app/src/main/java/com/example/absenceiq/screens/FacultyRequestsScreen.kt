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
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.Locale

data class FacultyRequestItem(
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
    val facultyStatus: String = "pending",
    val facultyRemarks: String = "",
    val adminStatus: String = "waiting",
    val status: String = "pending",
    val createdAt: Timestamp? = null
)

@Composable
fun FacultyRequestsScreen(
    onBack: () -> Unit
) {

    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    val uid = auth.currentUser?.uid

    var department by remember {
        mutableStateOf("")
    }

    var requests by remember {
        mutableStateOf<List<FacultyRequestItem>>(emptyList())
    }

    var selectedFilter by remember {
        mutableStateOf("All")
    }

    var selectedRequest by remember {
        mutableStateOf<FacultyRequestItem?>(null)
    }

    var reviewAction by remember {
        mutableStateOf("")
    }

    var remarks by remember {
        mutableStateOf("")
    }

    var showReviewDialog by remember {
        mutableStateOf(false)
    }

    var isLoading by remember {
        mutableStateOf(true)
    }

    val teal = Color(0xFF0F7780)
    val background = Color(0xFFF5F7FA)

    /*
     * Load faculty department
     */
    LaunchedEffect(uid) {

        if (uid != null) {

            db.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener { document ->

                    department =
                        document.getString(
                            "department"
                        ) ?: ""
                }
        }
    }

    /*
     * Load all leave requests for this department
     */
    DisposableEffect(department) {

        var listener: ListenerRegistration? = null

        if (department.isNotBlank()) {

            listener =
                db.collection("leave_requests")
                    .whereEqualTo(
                        "department",
                        department
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

                                    FacultyRequestItem(

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
                                            ) ?: 0,

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
                                            ) ?: "pending",

                                        facultyRemarks =
                                            document.getString(
                                                "facultyRemarks"
                                            ) ?: "",

                                        adminStatus =
                                            document.getString(
                                                "adminStatus"
                                            ) ?: "waiting",

                                        status =
                                            document.getString(
                                                "status"
                                            ) ?: "pending",

                                        createdAt =
                                            document.getTimestamp(
                                                "createdAt"
                                            )
                                    )
                                }
                                ?.sortedByDescending {
                                    it.createdAt?.seconds ?: 0
                                }
                                ?: emptyList()

                        isLoading = false
                    }
        }

        onDispose {
            listener?.remove()
        }
    }

    val filteredRequests =
        when (selectedFilter) {

            "Pending" ->
                requests.filter {
                    it.facultyStatus.equals(
                        "pending",
                        ignoreCase = true
                    )
                }

            "Reviewed" ->
                requests.filter {
                    !it.facultyStatus.equals(
                        "pending",
                        ignoreCase = true
                    )
                }

            else ->
                requests
        }

    /*
     * Review dialog
     */
    if (
        showReviewDialog &&
        selectedRequest != null
    ) {

        val request = selectedRequest!!

        AlertDialog(

            onDismissRequest = {
                showReviewDialog = false
                selectedRequest = null
                remarks = ""
            },

            title = {

                Text(
                    if (reviewAction == "approve")
                        "Approve Leave Request"
                    else
                        "Reject Leave Request"
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
                        request.studentName,
                        fontWeight =
                            FontWeight.Bold,
                        fontSize = 18.sp
                    )

                    Spacer(
                        Modifier.height(4.dp)
                    )

                    Text(
                        "${request.studentId} • ${request.department}"
                    )

                    Spacer(
                        Modifier.height(16.dp)
                    )

                    Text(
                        request.leaveType,
                        fontWeight =
                            FontWeight.Bold
                    )

                    Spacer(
                        Modifier.height(8.dp)
                    )

                    Text(
                        "Duration: ${request.duration} day(s)"
                    )

                    Spacer(
                        Modifier.height(12.dp)
                    )

                    Text(
                        "Reason",
                        fontWeight =
                            FontWeight.Bold
                    )

                    Text(
                        request.reason
                    )

                    Spacer(
                        Modifier.height(18.dp)
                    )

                    OutlinedTextField(
                        value = remarks,

                        onValueChange = {
                            remarks = it
                        },

                        label = {
                            Text("Remarks")
                        },

                        placeholder = {
                            Text(
                                if (reviewAction == "reject")
                                    "Reason for rejection"
                                else
                                    "Optional remarks"
                            )
                        },

                        minLines = 3,

                        modifier =
                            Modifier.fillMaxWidth()
                    )
                }
            },

            confirmButton = {

                Button(
                    onClick = {

                        if (
                            reviewAction == "reject" &&
                            remarks.isBlank()
                        ) {

                            Toast.makeText(
                                context,
                                "Please provide a rejection reason",
                                Toast.LENGTH_SHORT
                            ).show()

                            return@Button
                        }

                        val updates =
                            if (reviewAction == "approve") {

                                hashMapOf<String, Any>(
                                    "facultyStatus"
                                            to "approved",

                                    "facultyRemarks"
                                            to remarks.trim(),

                                    "adminStatus"
                                            to "pending",

                                    "status"
                                            to "pending",

                                    "facultyReviewedBy"
                                            to (uid ?: ""),

                                    "facultyReviewedAt"
                                            to FieldValue.serverTimestamp(),

                                    "updatedAt"
                                            to FieldValue.serverTimestamp()
                                )

                            } else {

                                hashMapOf<String, Any>(
                                    "facultyStatus"
                                            to "rejected",

                                    "facultyRemarks"
                                            to remarks.trim(),

                                    "adminStatus"
                                            to "not_required",

                                    "status"
                                            to "rejected",

                                    "facultyReviewedBy"
                                            to (uid ?: ""),

                                    "facultyReviewedAt"
                                            to FieldValue.serverTimestamp(),

                                    "updatedAt"
                                            to FieldValue.serverTimestamp()
                                )
                            }

                        db.collection("leave_requests")
                            .document(request.id)
                            .update(updates)

                            .addOnSuccessListener {

                                val notificationTitle =
                                    if (reviewAction == "approve")
                                        "Application Under Review"
                                    else
                                        "Leave Rejected"

                                val notificationMessage =
                                    if (reviewAction == "approve")
                                        "Your ${request.leaveType} request has been reviewed by Faculty / SSO and forwarded to HOD / Admin."
                                    else
                                        "Your ${request.leaveType} request was rejected by Faculty / SSO. Tap to view remarks."

                                val notificationType =
                                    if (reviewAction == "approve")
                                        "review"
                                    else
                                        "rejected"

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

                                db.collection("notifications")
                                    .add(notification)

                                Toast.makeText(
                                    context,

                                    if (reviewAction == "approve")
                                        "Request forwarded to HOD/Admin"
                                    else
                                        "Request rejected",

                                    Toast.LENGTH_LONG
                                ).show()

                                showReviewDialog =
                                    false

                                selectedRequest =
                                    null

                                remarks = ""
                            }

                            .addOnFailureListener { error ->

                                Toast.makeText(
                                    context,
                                    "Update failed: ${error.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                    }
                ) {

                    Text(
                        if (reviewAction == "approve")
                            "Approve"
                        else
                            "Reject"
                    )
                }
            },

            dismissButton = {

                TextButton(
                    onClick = {
                        showReviewDialog = false
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
                            "← Back",
                            color = Color.White
                        )
                    }

                    Spacer(
                        Modifier.width(8.dp)
                    )

                    Text(
                        text =
                            "Student Leave Requests",

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
                    "Reviewed"
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
                Modifier.height(
                    20.dp
                )
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
                filteredRequests.isEmpty()
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

                        FacultyRequestListCard(

                            request =
                                request,

                            onApprove = {

                                selectedRequest =
                                    request

                                reviewAction =
                                    "approve"

                                remarks = ""

                                showReviewDialog =
                                    true
                            },

                            onReject = {

                                selectedRequest =
                                    request

                                reviewAction =
                                    "reject"

                                remarks = ""

                                showReviewDialog =
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
fun FacultyRequestListCard(
    request: FacultyRequestItem,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {

    val formatter =
        remember {
            SimpleDateFormat(
                "dd MMM yyyy",
                Locale.getDefault()
            )
        }

    val pending =
        request.facultyStatus.equals(
            "pending",
            ignoreCase = true
        )

    val statusColor =
        when {

            pending ->
                Color(0xFFE45817)

            request.facultyStatus.equals(
                "approved",
                true
            ) ->
                Color(0xFF2E9E72)

            else ->
                Color(0xFFD74444)
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
                    Arrangement.SpaceBetween
            ) {

                Column {

                    Text(
                        request.studentName,

                        fontSize =
                            18.sp,

                        fontWeight =
                            FontWeight.Bold
                    )

                    Spacer(
                        Modifier.height(
                            3.dp
                        )
                    )

                    Text(
                        "${request.studentId} • ${request.department}",

                        color =
                            Color.Gray,

                        fontSize =
                            13.sp
                    )
                }

                Text(
                    text =
                        if (pending)
                            "Pending Review"
                        else
                            request.facultyStatus
                                .replaceFirstChar {
                                    it.uppercase()
                                },

                    color =
                        statusColor,

                    fontWeight =
                        FontWeight.Bold
                )
            }

            Spacer(
                Modifier.height(
                    14.dp
                )
            )

            Text(
                request.leaveType,

                fontWeight =
                    FontWeight.Bold
            )

            Spacer(
                Modifier.height(
                    6.dp
                )
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
                Modifier.height(
                    10.dp
                )
            )

            Text(
                "Reason: ${request.reason}"
            )

            if (
                request.facultyRemarks.isNotBlank()
            ) {

                Spacer(
                    Modifier.height(
                        8.dp
                    )
                )

                Text(
                    "Faculty Remarks: ${request.facultyRemarks}",

                    color =
                        Color.DarkGray
                )
            }

            if (pending) {

                Spacer(
                    Modifier.height(
                        18.dp
                    )
                )

                Row(
                    modifier =
                        Modifier.fillMaxWidth(),

                    horizontalArrangement =
                        Arrangement.spacedBy(
                            12.dp
                        )
                ) {

                    OutlinedButton(
                        onClick =
                            onReject,

                        modifier =
                            Modifier.weight(
                                1f
                            )
                    ) {

                        Text("Reject")
                    }

                    Button(
                        onClick =
                            onApprove,

                        modifier =
                            Modifier.weight(
                                1f
                            ),

                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor =
                                    Color(
                                        0xFF0F7780
                                    )
                            )
                    ) {

                        Text("Approve")
                    }
                }
            }
        }
    }
}