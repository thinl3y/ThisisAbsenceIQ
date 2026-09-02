package com.example.absenceiq.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.*

data class StudentLeaveSummary(
    val id: String = "",
    val leaveType: String = "",
    val startDate: Timestamp? = null,
    val endDate: Timestamp? = null,
    val duration: Long = 0,
    val status: String = "pending",
    val createdAt: Timestamp? = null
)

@Composable
fun StudentDashboardScreen(
    onApplyLeave: () -> Unit,
    onHistoryClick: () -> Unit,
    onLogout: () -> Unit
) {

    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    val uid = auth.currentUser?.uid

    var studentName by remember {
        mutableStateOf("Student")
    }

    var studentId by remember {
        mutableStateOf("")
    }

    var department by remember {
        mutableStateOf("")
    }

    var leaveBalance by remember {
        mutableStateOf<Long?>(null)
    }

    var leaveRequests by remember {
        mutableStateOf<List<StudentLeaveSummary>>(emptyList())
    }

    var isLoading by remember {
        mutableStateOf(true)
    }

    /*
     * Read student information
     */
    LaunchedEffect(uid) {

        if (uid != null) {

            db.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener { document ->

                    studentName =
                        document.getString("name")
                            ?: "Student"

                    studentId =
                        document.getString("studentId")
                            ?: ""

                    department =
                        document.getString("department")
                            ?: ""

                    leaveBalance =
                        document.getLong("leaveBalance")
                }
        }
    }

    /*
     * Listen to leave applications in real time.
     */
    DisposableEffect(uid) {

        var listener: ListenerRegistration? = null

        if (uid != null) {

            listener =
                db.collection("leave_requests")
                    .whereEqualTo("studentUid", uid)
                    .addSnapshotListener { snapshot, error ->

                        if (error != null) {

                            isLoading = false
                            return@addSnapshotListener
                        }

                        val requests =
                            snapshot?.documents
                                ?.map { document ->

                                    StudentLeaveSummary(

                                        id = document.id,

                                        leaveType =
                                            document.getString(
                                                "leaveType"
                                            ) ?: "",

                                        startDate =
                                            document.getTimestamp(
                                                "startDate"
                                            ),

                                        endDate =
                                            document.getTimestamp(
                                                "endDate"
                                            ),

                                        duration =
                                            document.getLong(
                                                "duration"
                                            ) ?: 0,

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

                        leaveRequests = requests

                        isLoading = false
                    }
        }

        onDispose {
            listener?.remove()
        }
    }

    /*
     * Live statistics
     */
    val pendingCount =
        leaveRequests.count {
            it.status.equals(
                "pending",
                ignoreCase = true
            )
        }

    val approvedCount =
        leaveRequests.count {
            it.status.equals(
                "approved",
                ignoreCase = true
            )
        }

    val rejectedCount =
        leaveRequests.count {
            it.status.equals(
                "rejected",
                ignoreCase = true
            )
        }

    val recentRequests =
        leaveRequests.take(3)

    val teal =
        Color(0xFF0F6B75)

    val lightBackground =
        Color(0xFFF5F7F8)

    Scaffold(

        bottomBar = {

            NavigationBar {

                NavigationBarItem(
                    selected = true,
                    onClick = onHistoryClick,

                    icon = {
                        Text("⌂")
                    },
                    label = {
                        Text("Home")
                    }
                )

                NavigationBarItem(
                    selected = false,
                    onClick = onHistoryClick,
                    icon = {
                        Text("◷")
                    },
                    label = {
                        Text("History")
                    }
                )

                NavigationBarItem(
                    selected = false,
                    onClick = onHistoryClick,
                    icon = {
                        Text("●")
                    },
                    label = {
                        Text("Notifications")
                    }
                )

                NavigationBarItem(
                    selected = false,
                    onClick = onHistoryClick,
                    icon = {
                        Text("●")
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
                .background(lightBackground)
                .verticalScroll(
                    rememberScrollState()
                )
                .padding(paddingValues)
        ) {

            /*
             * HEADER
             */
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(teal)
                    .padding(
                        start = 24.dp,
                        end = 24.dp,
                        top = 28.dp,
                        bottom = 30.dp
                    )
            ) {

                Text(
                    text = "Welcome back,",
                    color = Color.White.copy(
                        alpha = 0.85f
                    ),
                    fontSize = 15.sp
                )

                Spacer(
                    modifier =
                        Modifier.height(4.dp)
                )

                Text(
                    text = studentName,
                    color = Color.White,
                    fontSize = 27.sp,
                    fontWeight =
                        FontWeight.Bold
                )

                if (studentId.isNotBlank()) {

                    Spacer(
                        modifier =
                            Modifier.height(5.dp)
                    )

                    Text(
                        text =
                            "$studentId • $department",
                        color =
                            Color.White.copy(
                                alpha = 0.85f
                            ),
                        fontSize = 14.sp
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {

                /*
                 * LEAVE BALANCE CARD
                 */
                Card(
                    modifier =
                        Modifier.fillMaxWidth(),

                    shape =
                        RoundedCornerShape(18.dp),

                    colors =
                        CardDefaults.cardColors(
                            containerColor =
                                Color.White
                        )
                ) {

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),

                        verticalAlignment =
                            Alignment.CenterVertically,

                        horizontalArrangement =
                            Arrangement.SpaceBetween
                    ) {

                        Column {

                            Text(
                                text =
                                    "Leave Balance",
                                fontSize = 15.sp,
                                color =
                                    Color.Gray
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(
                                        5.dp
                                    )
                            )

                            if (
                                leaveBalance != null
                            ) {

                                Text(
                                    text =
                                        "$leaveBalance days",
                                    fontSize = 28.sp,
                                    fontWeight =
                                        FontWeight.Bold,
                                    color = teal
                                )

                            } else {

                                Text(
                                    text =
                                        "Not assigned",
                                    fontSize = 20.sp,
                                    fontWeight =
                                        FontWeight.Bold,
                                    color = teal
                                )
                            }
                        }

                        Text(
                            text = "Calendar",
                            color = teal
                        )
                    }
                }

                Spacer(
                    modifier =
                        Modifier.height(18.dp)
                )

                /*
                 * STATUS COUNTERS
                 */
                Row(
                    modifier =
                        Modifier.fillMaxWidth(),

                    horizontalArrangement =
                        Arrangement.spacedBy(
                            10.dp
                        )
                ) {

                    StatusCard(
                        title = "Pending",
                        count = pendingCount,
                        modifier =
                            Modifier.weight(1f)
                    )

                    StatusCard(
                        title = "Approved",
                        count = approvedCount,
                        modifier =
                            Modifier.weight(1f)
                    )

                    StatusCard(
                        title = "Rejected",
                        count = rejectedCount,
                        modifier =
                            Modifier.weight(1f)
                    )
                }

                Spacer(
                    modifier =
                        Modifier.height(22.dp)
                )

                /*
                 * APPLY LEAVE
                 */
                Button(
                    onClick = onApplyLeave,

                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(55.dp),

                    shape =
                        RoundedCornerShape(
                            14.dp
                        ),

                    colors =
                        ButtonDefaults
                            .buttonColors(
                                containerColor =
                                    teal
                            )
                ) {

                    Text(
                        text =
                            "+  Apply for Leave",

                        fontSize = 17.sp,
                        fontWeight =
                            FontWeight.SemiBold
                    )
                }

                Spacer(
                    modifier =
                        Modifier.height(28.dp)
                )

                /*
                 * RECENT APPLICATIONS
                 */
                Row(
                    modifier =
                        Modifier.fillMaxWidth(),

                    horizontalArrangement =
                        Arrangement
                            .SpaceBetween,

                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Text(
                        text =
                            "Recent Applications",

                        fontSize = 20.sp,
                        fontWeight =
                            FontWeight.Bold
                    )
                    TextButton(
                        onClick = onHistoryClick
                    ) {
                        Text(
                            text = "View All",
                            color = teal,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(
                    modifier =
                        Modifier.height(14.dp)
                )

                if (isLoading) {

                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(30.dp),

                        contentAlignment =
                            Alignment.Center
                    ) {

                        CircularProgressIndicator()
                    }

                } else if (
                    recentRequests.isEmpty()
                ) {

                    Card(
                        modifier =
                            Modifier.fillMaxWidth(),

                        colors =
                            CardDefaults.cardColors(
                                containerColor =
                                    Color.White
                            )
                    ) {

                        Text(
                            text =
                                "No leave applications yet.",

                            modifier =
                                Modifier.padding(
                                    24.dp
                                ),

                            color =
                                Color.Gray
                        )
                    }

                } else {

                    recentRequests.forEach {
                            request ->

                        LeaveRequestCard(
                            request =
                                request
                        )

                        Spacer(
                            modifier =
                                Modifier.height(
                                    10.dp
                                )
                        )
                    }
                }

                Spacer(
                    modifier =
                        Modifier.height(24.dp)
                )

                /*
                 * LOGOUT
                 */
                OutlinedButton(
                    onClick =
                        onLogout,

                    modifier =
                        Modifier.fillMaxWidth()
                ) {

                    Text("Logout")
                }

                Spacer(
                    modifier =
                        Modifier.height(20.dp)
                )
            }
        }
    }
}

@Composable
fun StatusCard(
    title: String,
    count: Int,
    modifier: Modifier = Modifier
) {

    Card(
        modifier = modifier,

        shape =
            RoundedCornerShape(14.dp),

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

                fontSize = 25.sp,

                fontWeight =
                    FontWeight.Bold
            )

            Spacer(
                modifier =
                    Modifier.height(4.dp)
            )

            Text(
                text = title,

                color =
                    Color.Gray,

                fontSize =
                    13.sp
            )
        }
    }
}

@Composable
fun LeaveRequestCard(
    request: StudentLeaveSummary
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
            request.status.lowercase()
        ) {

            "approved" ->
                Color(0xFF2E7D32)

            "rejected" ->
                Color(0xFFC62828)

            else ->
                Color(0xFFF57C00)
        }

    Card(
        modifier =
            Modifier.fillMaxWidth(),

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
            modifier =
                Modifier.padding(
                    16.dp
                )
        ) {

            Row(
                modifier =
                    Modifier.fillMaxWidth(),

                horizontalArrangement =
                    Arrangement
                        .SpaceBetween
            ) {

                Text(
                    text =
                        request.leaveType,

                    fontSize =
                        17.sp,

                    fontWeight =
                        FontWeight.Bold
                )

                Text(
                    text =
                        request.status
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
                modifier =
                    Modifier.height(
                        8.dp
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
                    "$start - $end",

                color =
                    Color.DarkGray
            )

            Spacer(
                modifier =
                    Modifier.height(
                        5.dp
                    )
            )

            Text(
                text =
                    "${request.duration} day(s)",

                color =
                    Color.Gray,

                fontSize =
                    13.sp
            )
        }
    }
}