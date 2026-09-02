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

data class AdminLeaveRequest(
    val id: String = "",
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
    val createdAt: Timestamp? = null
)

@Composable
fun AdminDashboardScreen(
    onLogout: () -> Unit
) {

    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    val uid = auth.currentUser?.uid

    var adminName by remember {
        mutableStateOf("HOD / Admin")
    }

    var requests by remember {
        mutableStateOf<List<AdminLeaveRequest>>(emptyList())
    }

    var selectedRequest by remember {
        mutableStateOf<AdminLeaveRequest?>(null)
    }

    var showDialog by remember {
        mutableStateOf(false)
    }

    var action by remember {
        mutableStateOf("")
    }

    var remarks by remember {
        mutableStateOf("")
    }

    var filter by remember {
        mutableStateOf("Pending")
    }

    var isLoading by remember {
        mutableStateOf(true)
    }

    val teal = Color(0xFF0F7A83)
    val bg = Color(0xFFF5F7F8)

    LaunchedEffect(uid) {

        if (uid != null) {

            db.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener { document ->

                    adminName =
                        document.getString("name")
                            ?: "HOD / Admin"
                }
        }
    }

    DisposableEffect(Unit) {

        var listener: ListenerRegistration? = null

        listener =
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

                                    id = document.id,

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
                                        )
                                )
                            }
                            ?.sortedByDescending {
                                it.createdAt?.seconds ?: 0
                            }
                            ?: emptyList()

                    isLoading = false
                }

        onDispose {
            listener?.remove()
        }
    }

    val pendingCount =
        requests.count {
            it.adminStatus == "pending"
        }

    val approvedCount =
        requests.count {
            it.adminStatus == "approved"
        }

    val rejectedCount =
        requests.count {
            it.adminStatus == "rejected"
        }

    val filteredRequests =
        when (filter) {

            "Approved" ->
                requests.filter {
                    it.adminStatus == "approved"
                }

            "Rejected" ->
                requests.filter {
                    it.adminStatus == "rejected"
                }

            "All" ->
                requests

            else ->
                requests.filter {
                    it.adminStatus == "pending"
                }
        }

    if (
        showDialog &&
        selectedRequest != null
    ) {

        AlertDialog(

            onDismissRequest = {
                showDialog = false
                remarks = ""
            },

            title = {

                Text(
                    if (action == "approve")
                        "Approve Leave Request"
                    else
                        "Reject Leave Request"
                )
            },

            text = {

                Column {

                    Text(
                        selectedRequest!!.studentName,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(
                        Modifier.height(6.dp)
                    )

                    Text(
                        selectedRequest!!.leaveType
                    )

                    Spacer(
                        Modifier.height(10.dp)
                    )

                    Text(
                        "Faculty remarks: ${selectedRequest!!.facultyRemarks}"
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
                            Text("HOD / Admin Remarks")
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
                            action == "reject" &&
                            remarks.isBlank()
                        ) {

                            Toast.makeText(
                                context,
                                "Please provide rejection remarks",
                                Toast.LENGTH_SHORT
                            ).show()

                            return@Button
                        }

                        val newStatus =
                            if (action == "approve")
                                "approved"
                            else
                                "rejected"

                        val updates =
                            hashMapOf<String, Any>(

                                "adminStatus"
                                        to newStatus,

                                "status"
                                        to newStatus,

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
                            .update(updates)

                            .addOnSuccessListener {

                                Toast.makeText(
                                    context,

                                    if (
                                        action ==
                                        "approve"
                                    )
                                        "Leave request approved"
                                    else
                                        "Leave request rejected",

                                    Toast.LENGTH_LONG
                                ).show()

                                showDialog = false
                                selectedRequest = null
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
                        if (action == "approve")
                            "Approve"
                        else
                            "Reject"
                    )
                }
            },

            dismissButton = {

                TextButton(
                    onClick = {
                        showDialog = false
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
                    onClick = { },
                    icon = {
                        Text("≡")
                    },
                    label = {
                        Text("Requests")
                    }
                )

                NavigationBarItem(
                    selected = false,
                    onClick = { },
                    icon = {
                        Text("●")
                    },
                    label = {
                        Text("History")
                    }
                )

                NavigationBarItem(
                    selected = false,
                    onClick = { },
                    icon = {
                        Text("○")
                    },
                    label = {
                        Text("Profile")
                    }
                )
            }
        }

    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(bg)
                .verticalScroll(
                    rememberScrollState()
                )
                .padding(padding)
        ) {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(teal)
                    .padding(24.dp)
            ) {

                Text(
                    text = "Leave Management",
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    Modifier.height(6.dp)
                )

                Text(
                    text = "Welcome, $adminName",
                    color = Color.White.copy(
                        alpha = 0.85f
                    )
                )
            }

            Column(
                modifier =
                    Modifier.padding(18.dp)
            ) {

                Row(
                    modifier =
                        Modifier.fillMaxWidth(),

                    horizontalArrangement =
                        Arrangement.spacedBy(10.dp)
                ) {

                    AdminStatCard(
                        title = "Pending",
                        count = pendingCount,
                        modifier =
                            Modifier.weight(1f)
                    )

                    AdminStatCard(
                        title = "Approved",
                        count = approvedCount,
                        modifier =
                            Modifier.weight(1f)
                    )

                    AdminStatCard(
                        title = "Rejected",
                        count = rejectedCount,
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

                    fontSize = 21.sp,

                    fontWeight =
                        FontWeight.Bold
                )

                Spacer(
                    Modifier.height(14.dp)
                )

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
                                filter == item,

                            onClick = {
                                filter = item
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

                    CircularProgressIndicator()

                } else if (
                    filteredRequests.isEmpty()
                ) {

                    Text(
                        "No requests found."
                    )

                } else {

                    filteredRequests
                        .forEach { request ->

                            AdminRequestCard(

                                request =
                                    request,

                                onApprove = {

                                    selectedRequest =
                                        request

                                    action =
                                        "approve"

                                    remarks = ""

                                    showDialog =
                                        true
                                },

                                onReject = {

                                    selectedRequest =
                                        request

                                    action =
                                        "reject"

                                    remarks = ""

                                    showDialog =
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
                    Modifier.height(24.dp)
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
                                20.dp
                            )
                    ) {

                        Text(
                            "Leave Overview",

                            fontSize =
                                18.sp,

                            fontWeight =
                                FontWeight.Bold
                        )

                        Spacer(
                            Modifier.height(
                                14.dp
                            )
                        )

                        Text(
                            "Pending requests: $pendingCount"
                        )

                        Text(
                            "Approved requests: $approvedCount"
                        )

                        Text(
                            "Rejected requests: $rejectedCount"
                        )
                    }
                }

                Spacer(
                    Modifier.height(20.dp)
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
                    Modifier.height(25.dp)
                )
            }
        }
    }
}

@Composable
fun AdminStatCard(
    title: String,
    count: Int,
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
                text = count.toString(),

                fontSize = 25.sp,

                fontWeight =
                    FontWeight.Bold
            )

            Spacer(
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
fun AdminRequestCard(
    request: AdminLeaveRequest,
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

                        fontSize = 18.sp,

                        fontWeight =
                            FontWeight.Bold
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
                    request.adminStatus
                        .replaceFirstChar {
                            it.uppercase()
                        },

                    color =
                        Color(0xFFF57C00),

                    fontWeight =
                        FontWeight.Bold
                )
            }

            Spacer(
                Modifier.height(14.dp)
            )

            Text(
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
                "$start - $end • ${request.duration} Days"
            )

            Spacer(
                Modifier.height(12.dp)
            )

            Text(
                "Reason: ${request.reason}"
            )

            if (
                request.facultyRemarks
                    .isNotBlank()
            ) {

                Spacer(
                    Modifier.height(8.dp)
                )

                Text(
                    "Faculty Remarks: ${request.facultyRemarks}",
                    color =
                        Color.DarkGray
                )
            }

            if (
                request.adminStatus ==
                "pending"
            ) {

                Spacer(
                    Modifier.height(18.dp)
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
                            Modifier.weight(1f)
                    ) {

                        Text("Reject")
                    }

                    Button(
                        onClick =
                            onApprove,

                        modifier =
                            Modifier.weight(1f),

                        colors =
                            ButtonDefaults
                                .buttonColors(
                                    containerColor =
                                        Color(
                                            0xFF0F7A83
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