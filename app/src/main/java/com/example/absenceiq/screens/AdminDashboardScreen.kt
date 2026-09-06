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
import java.util.Calendar
import java.util.Locale

data class AdminLeaveRequest(
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
    val facultyRemarks: String = "",
    val facultyStatus: String = "",
    val adminStatus: String = "pending",
    val status: String = "pending",
    val createdAt: Timestamp? = null,
    val adminReviewedAt: Timestamp? = null,

    // Prevents leave balance from being deducted twice.
    val balanceDeducted: Boolean = false
)

@Composable
fun AdminDashboardScreen(
    onRequestsClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onProfileClick: () -> Unit,
    onLogout: () -> Unit
) {

    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    val uid = auth.currentUser?.uid

    var adminName by remember {
        mutableStateOf("HOD")
    }

    var requests by remember {
        mutableStateOf<List<AdminLeaveRequest>>(emptyList())
    }

    var selectedFilter by remember {
        mutableStateOf("All")
    }

    var selectedRequest by remember {
        mutableStateOf<AdminLeaveRequest?>(null)
    }

    var showRequestDialog by remember {
        mutableStateOf(false)
    }

    var remarks by remember {
        mutableStateOf("")
    }

    var isLoading by remember {
        mutableStateOf(true)
    }

    val teal = Color(0xFF0B747D)
    val pageBackground = Color(0xFFF5F7FA)

    /*
     * Load logged-in HOD/Admin profile.
     */
    LaunchedEffect(uid) {

        if (uid != null) {

            db.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener { document ->

                    adminName =
                        document.getString("name")
                            ?: "HOD"
                }
        }
    }

    /*
     * Only requests already approved by Faculty / SSO
     * should reach HOD/Admin.
     */
    DisposableEffect(Unit) {

        val listener: ListenerRegistration =
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

                                AdminLeaveRequest(

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

                                    facultyRemarks =
                                        document.getString(
                                            "facultyRemarks"
                                        ) ?: "",

                                    facultyStatus =
                                        document.getString(
                                            "facultyStatus"
                                        ) ?: "",

                                    adminStatus =
                                        document.getString(
                                            "adminStatus"
                                        ) ?: "pending",

                                    status =
                                        document.getString(
                                            "status"
                                        ) ?: "pending",

                                    createdAt =
                                        document.getTimestamp(
                                            "createdAt"
                                        ),

                                    adminReviewedAt =
                                        document.getTimestamp(
                                            "adminReviewedAt"
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
     * Dashboard statistics.
     */
    val pendingCount =
        requests.count {
            it.adminStatus.equals(
                "pending",
                ignoreCase = true
            )
        }

    val rejectedCount =
        requests.count {
            it.adminStatus.equals(
                "rejected",
                ignoreCase = true
            )
        }

    val approvedTodayCount =
        requests.count { request ->

            if (
                !request.adminStatus.equals(
                    "approved",
                    ignoreCase = true
                )
            ) {

                false

            } else {

                val reviewedDate =
                    request.adminReviewedAt
                        ?.toDate()

                if (reviewedDate == null) {

                    false

                } else {

                    val today =
                        Calendar.getInstance()

                    val reviewed =
                        Calendar.getInstance()
                            .apply {
                                time = reviewedDate
                            }

                    today.get(Calendar.YEAR) ==
                            reviewed.get(Calendar.YEAR) &&

                            today.get(
                                Calendar.DAY_OF_YEAR
                            ) ==
                            reviewed.get(
                                Calendar.DAY_OF_YEAR
                            )
                }
            }
        }

    /*
     * Filter requests.
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
     * REQUEST DETAILS DIALOG
     */
    if (
        showRequestDialog &&
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

                showRequestDialog = false
                selectedRequest = null
                remarks = ""
            },

            title = {

                Text(
                    text = "Leave Request",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            },

            text = {

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(
                            rememberScrollState()
                        )
                ) {

                    /*
                     * Student information.
                     */
                    Row(
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Surface(
                            modifier =
                                Modifier.size(52.dp),

                            shape =
                                RoundedCornerShape(26.dp),

                            color =
                                Color(0xFFE1F0F2)
                        ) {

                            Box(
                                contentAlignment =
                                    Alignment.Center
                            ) {

                                Text(
                                    text =
                                        request.studentName
                                            .split(" ")
                                            .filter {
                                                it.isNotBlank()
                                            }
                                            .take(2)
                                            .mapNotNull {
                                                it.firstOrNull()
                                            }
                                            .joinToString("")
                                            .uppercase(),

                                    color = teal,

                                    fontWeight =
                                        FontWeight.Bold
                                )
                            }
                        }

                        Spacer(
                            Modifier.width(12.dp)
                        )

                        Column {

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
                                    "Student ID: ${request.studentId}",

                                color =
                                    Color.Gray
                            )

                            Text(
                                text =
                                    request.department,

                                color =
                                    Color.Gray
                            )
                        }
                    }

                    Spacer(
                        Modifier.height(16.dp)
                    )

                    /*
                     * Status.
                     */
                    Surface(
                        shape =
                            RoundedCornerShape(18.dp),

                        color =
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
                    ) {

                        Text(
                            text =
                                if (
                                    request.adminStatus.equals(
                                        "pending",
                                        true
                                    )
                                ) {
                                    "PENDING REVIEW"
                                } else {
                                    request.adminStatus.uppercase()
                                },

                            color =
                                when (
                                    request.adminStatus.lowercase()
                                ) {

                                    "approved" ->
                                        Color(0xFF2E9E72)

                                    "rejected" ->
                                        Color(0xFFD74444)

                                    else ->
                                        Color(0xFFE49317)
                                },

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

                    Spacer(
                        Modifier.height(22.dp)
                    )

                    /*
                     * Leave details.
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
                        Modifier.height(12.dp)
                    )

                    AdminDetailRow(
                        label =
                            "Leave Type",

                        value =
                            request.leaveType
                    )

                    AdminDetailRow(
                        label =
                            "Start Date",

                        value =
                            startDateText
                    )

                    AdminDetailRow(
                        label =
                            "End Date",

                        value =
                            endDateText
                    )

                    AdminDetailRow(
                        label =
                            "Duration",

                        value =
                            "${request.duration} Days"
                    )

                    Spacer(
                        Modifier.height(16.dp)
                    )

                    Text(
                        text = "Reason:",

                        fontWeight =
                            FontWeight.Bold
                    )

                    Spacer(
                        Modifier.height(5.dp)
                    )

                    Text(
                        text =
                            request.reason,

                        color =
                            Color.DarkGray
                    )

                    /*
                     * Faculty review.
                     */
                    if (
                        request.facultyRemarks
                            .isNotBlank()
                    ) {

                        Spacer(
                            Modifier.height(18.dp)
                        )

                        Text(
                            text =
                                "Faculty Remarks",

                            fontWeight =
                                FontWeight.Bold
                        )

                        Spacer(
                            Modifier.height(5.dp)
                        )

                        Text(
                            text =
                                request.facultyRemarks,

                            color =
                                Color.DarkGray
                        )
                    }

                    Spacer(
                        Modifier.height(20.dp)
                    )

                    /*
                     * HOD/Admin remarks input.
                     */
                    if (
                        request.adminStatus.equals(
                            "pending",
                            true
                        )
                    ) {

                        Text(
                            text = "Remarks",

                            fontWeight =
                                FontWeight.Bold
                        )

                        Spacer(
                            Modifier.height(8.dp)
                        )

                        OutlinedTextField(
                            value = remarks,

                            onValueChange = {
                                remarks = it
                            },

                            placeholder = {
                                Text(
                                    "Add remarks for the student..."
                                )
                            },

                            minLines = 3,

                            modifier =
                                Modifier.fillMaxWidth()
                        )
                    }
                }
            },

            /*
             * APPROVE BUTTON
             */
            confirmButton = {

                if (
                    request.adminStatus.equals(
                        "pending",
                        true
                    )
                ) {

                    Button(
                        onClick = {

                            updateAdminDecision(
                                db = db,
                                request = request,
                                uid = uid,
                                remarks = remarks,
                                action = "approve",
                                context = context
                            ) {

                                showRequestDialog =
                                    false

                                selectedRequest =
                                    null

                                remarks = ""
                            }
                        },

                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = teal
                            )
                    ) {

                        Text("Approve")
                    }

                } else {

                    TextButton(
                        onClick = {

                            showRequestDialog =
                                false

                            selectedRequest =
                                null
                        }
                    ) {

                        Text("Close")
                    }
                }
            },

            /*
             * REJECT BUTTON
             */
            dismissButton = {

                if (
                    request.adminStatus.equals(
                        "pending",
                        true
                    )
                ) {

                    OutlinedButton(
                        onClick = {

                            if (
                                remarks.isBlank()
                            ) {

                                Toast.makeText(
                                    context,
                                    "Please add remarks before rejecting",
                                    Toast.LENGTH_SHORT
                                ).show()

                                return@OutlinedButton
                            }

                            updateAdminDecision(
                                db = db,
                                request = request,
                                uid = uid,
                                remarks = remarks,
                                action = "reject",
                                context = context
                            ) {

                                showRequestDialog =
                                    false

                                selectedRequest =
                                    null

                                remarks = ""
                            }
                        }
                    ) {

                        Text(
                            text = "Reject",
                            color =
                                Color(0xFFC62828)
                        )
                    }
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

                    onClick =
                        onRequestsClick,

                    icon = {
                        Text("≡")
                    },

                    label = {
                        Text("Requests")
                    }
                )

                NavigationBarItem(
                    selected = false,

                    onClick =
                        onHistoryClick,

                    icon = {
                        Text("●")
                    },

                    label = {
                        Text("History")
                    }
                )

                NavigationBarItem(
                    selected = false,

                    onClick =
                        onProfileClick,

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
                        "Leave Management",

                    color =
                        Color.White,

                    fontSize =
                        27.sp,

                    fontWeight =
                        FontWeight.Bold
                )

                Spacer(
                    Modifier.height(5.dp)
                )

                Text(
                    text =
                        "Welcome, $adminName",

                    color =
                        Color.White.copy(
                            alpha = 0.85f
                        ),

                    fontSize =
                        16.sp
                )
            }

            Column(
                modifier =
                    Modifier.padding(18.dp)
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

                    AdminSummaryCard(
                        count =
                            pendingCount,

                        title =
                            "Pending",

                        countColor =
                            Color(0xFFF39C12),

                        modifier =
                            Modifier.weight(1f)
                    )

                    AdminSummaryCard(
                        count =
                            approvedTodayCount,

                        title =
                            "Approved Today",

                        countColor =
                            Color(0xFF2E9E72),

                        modifier =
                            Modifier.weight(1f)
                    )

                    AdminSummaryCard(
                        count =
                            rejectedCount,

                        title =
                            "Rejected",

                        countColor =
                            Color(0xFFD74444),

                        modifier =
                            Modifier.weight(1f)
                    )
                }

                Spacer(
                    Modifier.height(26.dp)
                )

                Text(
                    text =
                        "Pending Leave Requests",

                    fontSize =
                        21.sp,

                    fontWeight =
                        FontWeight.Bold
                )

                Spacer(
                    Modifier.height(12.dp)
                )

                /*
                 * FILTERS
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
                    Modifier.height(18.dp)
                )

                if (isLoading) {

                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(40.dp),

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

                            AdminCompactRequestCard(
                                request =
                                    request,

                                teal =
                                    teal,

                                onViewRequest = {

                                    selectedRequest =
                                        request

                                    remarks = ""

                                    showRequestDialog =
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
                    Modifier.height(12.dp)
                )

                LeaveOverviewCard(
                    requests =
                        requests
                )

                Spacer(
                    Modifier.height(22.dp)
                )

                OutlinedButton(
                    onClick =
                        onLogout,

                    modifier =
                        Modifier.fillMaxWidth()
                ) {

                    Text("Logout")
                }

                Spacer(
                    Modifier.height(20.dp)
                )
            }
        }
    }
}


@Composable
fun AdminSummaryCard(
    count: Int,
    title: String,
    countColor: Color,
    modifier: Modifier = Modifier
) {

    Card(
        modifier =
            modifier,

        shape =
            RoundedCornerShape(16.dp),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    Color.White
            )
    ) {

        Column(
            modifier =
                Modifier
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
                    26.sp,

                fontWeight =
                    FontWeight.Bold,

                color =
                    countColor
            )

            Spacer(
                Modifier.height(4.dp)
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
fun AdminCompactRequestCard(
    request: AdminLeaveRequest,
    teal: Color,
    onViewRequest: () -> Unit
) {

    val formatter =
        remember {
            SimpleDateFormat(
                "dd MMM",
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
            RoundedCornerShape(16.dp),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    Color.White
            )
    ) {

        Column(
            modifier =
                Modifier.padding(18.dp)
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
                        Modifier.weight(1f)
                ) {

                    Text(
                        text =
                            request.studentName,

                        fontSize =
                            17.sp,

                        fontWeight =
                            FontWeight.Bold
                    )

                    Spacer(
                        Modifier.height(5.dp)
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
                Modifier.height(7.dp)
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
                    "$start – $end • ${request.duration} Days",

                color =
                    Color.Gray
            )

            if (
                request.adminStatus.equals(
                    "pending",
                    ignoreCase = true
                )
            ) {

                Spacer(
                    Modifier.height(16.dp)
                )

                Row(
                    modifier =
                        Modifier.fillMaxWidth(),

                    horizontalArrangement =
                        Arrangement.End
                ) {

                    Button(
                        onClick =
                            onViewRequest,

                        colors =
                            ButtonDefaults
                                .buttonColors(
                                    containerColor =
                                        teal
                                ),

                        shape =
                            RoundedCornerShape(
                                14.dp
                            )
                    ) {

                        Text(
                            "View Request"
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun LeaveOverviewCard(
    requests: List<AdminLeaveRequest>
) {

    val approvedRequests =
        requests.filter {
            it.status.equals(
                "approved",
                ignoreCase = true
            )
        }

    val now =
        Calendar.getInstance()

    val todayStart =
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
            .time

    val todayEnd =
        Calendar.getInstance()
            .apply {

                set(
                    Calendar.HOUR_OF_DAY,
                    23
                )

                set(
                    Calendar.MINUTE,
                    59
                )

                set(
                    Calendar.SECOND,
                    59
                )
            }
            .time

    val onLeaveToday =
        approvedRequests.count {

            val start =
                it.startDate?.toDate()

            val end =
                it.endDate?.toDate()

            start != null &&
                    end != null &&
                    !start.after(todayEnd) &&
                    !end.before(todayStart)
        }

    val thisMonth =
        requests.count {

            val created =
                it.createdAt
                    ?.toDate()
                    ?: return@count false

            val calendar =
                Calendar.getInstance()
                    .apply {
                        time = created
                    }

            calendar.get(
                Calendar.MONTH
            ) ==
                    now.get(
                        Calendar.MONTH
                    ) &&

                    calendar.get(
                        Calendar.YEAR
                    ) ==
                    now.get(
                        Calendar.YEAR
                    )
        }

    val decided =
        requests.count {

            it.adminStatus.equals(
                "approved",
                true
            ) ||

                    it.adminStatus.equals(
                        "rejected",
                        true
                    )
        }

    val approved =
        requests.count {
            it.adminStatus.equals(
                "approved",
                true
            )
        }

    val approvalRate =
        if (decided > 0) {

            approved * 100 /
                    decided

        } else {

            0
        }

    Card(
        modifier =
            Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(16.dp),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    Color.White
            )
    ) {

        Column(
            modifier =
                Modifier.padding(20.dp)
        ) {

            Text(
                text =
                    "Leave Overview",

                fontSize =
                    18.sp,

                fontWeight =
                    FontWeight.Bold
            )

            Spacer(
                Modifier.height(16.dp)
            )

            OverviewRow(
                label =
                    "On leave today",

                value =
                    "$onLeaveToday students"
            )

            Spacer(
                Modifier.height(9.dp)
            )

            OverviewRow(
                label =
                    "This month",

                value =
                    "$thisMonth applications"
            )

            Spacer(
                Modifier.height(9.dp)
            )

            OverviewRow(
                label =
                    "Approval rate",

                value =
                    "$approvalRate%"
            )
        }
    }
}


@Composable
fun OverviewRow(
    label: String,
    value: String
) {

    Row(
        modifier =
            Modifier.fillMaxWidth(),

        horizontalArrangement =
            Arrangement.SpaceBetween
    ) {

        Text(
            text =
                label,

            color =
                Color.Gray
        )

        Text(
            text =
                value,

            fontWeight =
                FontWeight.Bold
        )
    }
}


@Composable
fun AdminDetailRow(
    label: String,
    value: String
) {

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    vertical = 5.dp
                ),

        horizontalArrangement =
            Arrangement.SpaceBetween
    ) {

        Text(
            text =
                label,

            color =
                Color.Gray
        )

        Text(
            text =
                value,

            fontWeight =
                FontWeight.Medium
        )
    }
}


/*
 * Final HOD/Admin decision.
 *
 * APPROVE:
 * - verifies request is still pending
 * - reads student's leaveBalance
 * - checks balance
 * - deducts duration
 * - approves request
 * - sets balanceDeducted = true
 *
 * All of that happens in ONE transaction.
 *
 * REJECT:
 * - updates request only
 * - does not deduct leave balance
 */
private fun updateAdminDecision(
    db: FirebaseFirestore,
    request: AdminLeaveRequest,
    uid: String?,
    remarks: String,
    action: String,
    context: android.content.Context,
    onComplete: () -> Unit
) {

    /*
     * APPROVE
     */
    if (action == "approve") {

        if (
            request.studentUid.isBlank()
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
             * Read both documents before writing.
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
             * Make sure Faculty actually approved.
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
             * Prevent duplicate final approval.
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
             * Prevent double balance deduction.
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

            val currentBalance =
                studentSnapshot.getLong(
                    "leaveBalance"
                )
                    ?: throw Exception(
                        "Student leave balance is not assigned"
                    )

            val duration =
                requestSnapshot.getLong(
                    "duration"
                ) ?: request.duration

            if (duration <= 0) {

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
             * Update student balance.
             */
            transaction.update(
                studentRef,
                "leaveBalance",
                newBalance
            )

            /*
             * Final approval.
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

                createAdminNotification(
                    db = db,
                    request = request,
                    action = "approve",
                    context = context
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
         * No leave balance deduction.
         */
        val updates =
            hashMapOf<String, Any>(

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

        db.collection(
            "leave_requests"
        )
            .document(
                request.id
            )
            .update(
                updates
            )
            .addOnSuccessListener {

                createAdminNotification(
                    db = db,
                    request = request,
                    action = "reject",
                    context = context,
                    onComplete = onComplete
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
 * Create notification only AFTER
 * approval/rejection succeeds.
 */
private fun createAdminNotification(
    db: FirebaseFirestore,
    request: AdminLeaveRequest,
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
             * Approval/rejection already succeeded,
             * so don't undo it just because notification failed.
             */
            Toast.makeText(
                context,
                "Decision saved, but notification failed: ${error.message}",
                Toast.LENGTH_LONG
            ).show()

            onComplete()
        }
}