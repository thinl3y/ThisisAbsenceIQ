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

data class FacultyLeaveRequest(
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
    val status: String = "pending",
    val createdAt: Timestamp? = null
)

@Composable
fun FacultyDashboardScreen(
    onRequestsClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onProfileClick: () -> Unit,
    onLogout: () -> Unit
) {

    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    val uid = auth.currentUser?.uid

    var facultyName by remember {
        mutableStateOf("Faculty / SSO")
    }

    var department by remember {
        mutableStateOf("")
    }

    var requests by remember {
        mutableStateOf<List<FacultyLeaveRequest>>(emptyList())
    }

    var selectedFilter by remember {
        mutableStateOf("All")
    }

    var selectedRequest by remember {
        mutableStateOf<FacultyLeaveRequest?>(null)
    }

    var showReviewDialog by remember {
        mutableStateOf(false)
    }

    var reviewAction by remember {
        mutableStateOf("")
    }

    var remarks by remember {
        mutableStateOf("")
    }

    var isLoading by remember {
        mutableStateOf(true)
    }

    val teal = Color(0xFF0F7780)
    val pageBackground = Color(0xFFF4F7FB)

    /*
     * Load Faculty / SSO profile
     */
    LaunchedEffect(uid) {

        if (uid != null) {

            db.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener { document ->

                    facultyName =
                        document.getString("name")
                            ?: "Faculty / SSO"

                    department =
                        document.getString("department")
                            ?: ""
                }
        }
    }

    /*
     * Listen to requests in the same department
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

                                    FacultyLeaveRequest(

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

    val pendingCount =
        requests.count {
            it.facultyStatus.equals(
                "pending",
                ignoreCase = true
            )
        }

    val reviewedCount =
        requests.count {
            !it.facultyStatus.equals(
                "pending",
                ignoreCase = true
            )
        }

    val onLeaveTodayCount =
        requests.count {
            it.status.equals(
                "approved",
                ignoreCase = true
            )
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

            else -> requests
        }

    /*
     * Review dialog
     */
    if (
        showReviewDialog &&
        selectedRequest != null
    ) {

        AlertDialog(

            onDismissRequest = {
                showReviewDialog = false
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

                Column {

                    Text(
                        selectedRequest!!.studentName,
                        fontWeight =
                            FontWeight.Bold
                    )

                    Spacer(
                        Modifier.height(6.dp)
                    )

                    Text(
                        selectedRequest!!.leaveType
                    )

                    Spacer(
                        Modifier.height(14.dp)
                    )

                    Text(
                        "Reason: ${selectedRequest!!.reason}"
                    )

                    Spacer(
                        Modifier.height(14.dp)
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
                                if (
                                    reviewAction ==
                                    "reject"
                                )
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

                        val request =
                            selectedRequest
                                ?: return@Button

                        if (
                            reviewAction ==
                            "reject" &&
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
                            if (
                                reviewAction ==
                                "approve"
                            ) {

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

                        db.collection(
                            "leave_requests"
                        )
                            .document(
                                request.id
                            )
                            .update(updates)

                            .addOnSuccessListener {

                                val notificationTitle =
                                    if (
                                        reviewAction ==
                                        "approve"
                                    )
                                        "Application Under Review"
                                    else
                                        "Leave Rejected"

                                val notificationMessage =
                                    if (
                                        reviewAction ==
                                        "approve"
                                    )
                                        "Your ${request.leaveType} request has been reviewed by Faculty / SSO and forwarded to HOD / Admin."
                                    else
                                        "Your ${request.leaveType} request was rejected by Faculty / SSO. Tap to view remarks."

                                val notificationType =
                                    if (
                                        reviewAction ==
                                        "approve"
                                    )
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

                                db.collection(
                                    "com/example/absenceiq/notifications"
                                )
                                    .add(notification)

                                Toast.makeText(
                                    context,

                                    if (
                                        reviewAction ==
                                        "approve"
                                    )
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
                        if (
                            reviewAction ==
                            "approve"
                        )
                            "Approve"
                        else
                            "Reject"
                    )
                }
            },

            dismissButton = {

                TextButton(
                    onClick = {
                        showReviewDialog =
                            false
                    }
                ) {

                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(

        bottomBar = {

            NavigationBar {

                NavigationBarItem(
                    selected = true,
                    onClick = { },
                    icon = {
                        Text("⌂")
                    },
                    label = {
                        Text("Dashboard")
                    }
                )

                NavigationBarItem(
                    selected = false,
                    onClick = onRequestsClick,
                    icon = {
                        Text("≡")
                    },
                    label = {
                        Text("Requests")
                    }
                )

                NavigationBarItem(
                    selected = false,
                    onClick = onHistoryClick,
                    icon = {
                        Text("●")
                    },
                    label = {
                        Text("History")
                    }
                )

                NavigationBarItem(
                    selected = false,
                    onClick = onProfileClick,
                    icon = {
                        Text("○")
                    },
                    label = {
                        Text("Profile")
                    }
                )
            }
        }

    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    pageBackground
                )
                .verticalScroll(
                    rememberScrollState()
                )
                .padding(
                    paddingValues
                )
        ) {

            /*
             * HEADER
             */
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(teal)
                    .padding(
                        horizontal = 24.dp,
                        vertical = 28.dp
                    )
            ) {

                Text(
                    text =
                        "Faculty Dashboard",

                    color =
                        Color.White,

                    fontSize =
                        26.sp,

                    fontWeight =
                        FontWeight.Bold
                )

                Spacer(
                    Modifier.height(5.dp)
                )

                Text(
                    text =
                        "Welcome, Faculty / SSO",

                    color =
                        Color.White.copy(
                            alpha = 0.85f
                        )
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {

                /*
                 * SUMMARY CARDS
                 */
                Row(
                    modifier =
                        Modifier.fillMaxWidth(),

                    horizontalArrangement =
                        Arrangement.spacedBy(
                            10.dp
                        )
                ) {

                    FacultyStatCard(
                        title =
                            "Pending Review",
                        count =
                            pendingCount,
                        countColor =
                            Color(
                                0xFFF39C12
                            ),
                        modifier =
                            Modifier.weight(1f)
                    )

                    FacultyStatCard(
                        title =
                            "Reviewed",
                        count =
                            reviewedCount,
                        countColor =
                            Color(
                                0xFF2E9E72
                            ),
                        modifier =
                            Modifier.weight(1f)
                    )

                    FacultyStatCard(
                        title =
                            "On Leave Today",
                        count =
                            onLeaveTodayCount,
                        countColor =
                            Color(
                                0xFF2D78B7
                            ),
                        modifier =
                            Modifier.weight(1f)
                    )
                }

                Spacer(
                    Modifier.height(
                        26.dp
                    )
                )

                Text(
                    text =
                        "Student Leave Requests",

                    fontSize =
                        21.sp,

                    fontWeight =
                        FontWeight.Bold
                )

                Spacer(
                    Modifier.height(
                        12.dp
                    )
                )

                /*
                 * FILTER CHIPS
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
                        18.dp
                    )
                )

                if (isLoading) {

                    Box(
                        modifier =
                            Modifier.fillMaxWidth()
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

                        Column(
                            modifier =
                                Modifier.padding(
                                    26.dp
                                )
                        ) {

                            Text(
                                "No leave requests found",

                                fontWeight =
                                    FontWeight.Bold
                            )

                            Spacer(
                                Modifier.height(
                                    6.dp
                                )
                            )

                            Text(
                                "New student requests will appear here automatically.",

                                color =
                                    Color.Gray
                            )
                        }
                    }

                } else {

                    filteredRequests
                        .forEach { request ->

                            FacultyRequestCard(
                                request =
                                    request,

                                onReview = {

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

                Spacer(
                    Modifier.height(
                        20.dp
                    )
                )

                OutlinedButton(
                    onClick = onLogout,

                    modifier =
                        Modifier.fillMaxWidth()
                ) {

                    Text("Logout")
                }

                Spacer(
                    Modifier.height(
                        20.dp
                    )
                )
            }
        }
    }
}

@Composable
fun FacultyStatCard(
    title: String,
    count: Int,
    countColor: Color,
    modifier: Modifier = Modifier
) {

    Card(
        modifier = modifier,

        shape =
            RoundedCornerShape(
                15.dp
            ),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    Color.White
            )
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    vertical = 18.dp
                ),

            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            Text(
                text =
                    count.toString(),

                fontSize =
                    24.sp,

                fontWeight =
                    FontWeight.Bold,

                color =
                    countColor
            )

            Spacer(
                Modifier.height(
                    5.dp
                )
            )

            Text(
                text =
                    title,

                color =
                    Color.Gray,

                fontSize =
                    12.sp
            )
        }
    }
}

@Composable
fun FacultyRequestCard(
    request: FacultyLeaveRequest,
    onReview: () -> Unit,
    onReject: () -> Unit
) {

    val formatter =
        remember {
            SimpleDateFormat(
                "dd MMM",
                Locale.getDefault()
            )
        }

    val pending =
        request.facultyStatus
            .equals(
                "pending",
                ignoreCase = true
            )

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

                Text(
                    text =
                        request.studentName,

                    fontSize =
                        17.sp,

                    fontWeight =
                        FontWeight.Bold
                )

                Surface(
                    shape =
                        RoundedCornerShape(
                            20.dp
                        ),

                    color =
                        if (pending)
                            Color(
                                0xFFFFF1DB
                            )
                        else
                            Color(
                                0xFFE4F5EC
                            )
                ) {

                    Text(
                        text =
                            if (pending)
                                "Pending Review"
                            else
                                "Reviewed",

                        color =
                            if (pending)
                                Color(
                                    0xFFE49317
                                )
                            else
                                Color(
                                    0xFF2E9E72
                                ),

                        modifier =
                            Modifier.padding(
                                horizontal =
                                    12.dp,
                                vertical =
                                    6.dp
                            ),

                        fontSize =
                            12.sp,

                        fontWeight =
                            FontWeight.Bold
                    )
                }
            }

            Spacer(
                Modifier.height(
                    8.dp
                )
            )

            Text(
                text =
                    "Student ID: ${request.studentId}",

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
                    request.department,

                color =
                    Color.Gray,

                fontSize =
                    13.sp
            )

            Spacer(
                Modifier.height(
                    14.dp
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
                text =
                    "${request.leaveType} • $start – $end",

                fontWeight =
                    FontWeight.Bold
            )

            if (pending) {

                Spacer(
                    Modifier.height(
                        16.dp
                    )
                )

                Row(
                    modifier =
                        Modifier.fillMaxWidth(),

                    horizontalArrangement =
                        Arrangement.End
                ) {

                    OutlinedButton(
                        onClick =
                            onReject
                    ) {

                        Text("Reject")
                    }

                    Spacer(
                        Modifier.width(
                            10.dp
                        )
                    )

                    Button(
                        onClick =
                            onReview,

                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor =
                                    Color(
                                        0xFF0F7780
                                    )
                            )
                    ) {

                        Text("Review")
                    }
                }
            }
        }
    }
}