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
import java.util.Locale

data class LeaveHistoryItem(
    val id: String = "",
    val leaveType: String = "",
    val reason: String = "",
    val duration: Long = 0,
    val startDate: Timestamp? = null,
    val endDate: Timestamp? = null,
    val status: String = "pending",
    val facultyStatus: String = "",
    val facultyRemarks: String = "",
    val adminStatus: String = "",
    val adminRemarks: String = "",
    val createdAt: Timestamp? = null
)

@Composable
fun LeaveHistoryScreen(
    onBack: () -> Unit
) {

    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    val uid = auth.currentUser?.uid

    var requests by remember {
        mutableStateOf<List<LeaveHistoryItem>>(emptyList())
    }

    var selectedFilter by remember {
        mutableStateOf("All")
    }

    var selectedRequest by remember {
        mutableStateOf<LeaveHistoryItem?>(null)
    }

    var showDetails by remember {
        mutableStateOf(false)
    }

    var isLoading by remember {
        mutableStateOf(true)
    }

    val teal = Color(0xFF0F7A83)
    val background = Color(0xFFF5F7F8)

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

                        requests =
                            snapshot?.documents
                                ?.map { document ->

                                    LeaveHistoryItem(

                                        id = document.id,

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

                                        status =
                                            document.getString(
                                                "status"
                                            ) ?: "pending",

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
                                            ) ?: "",

                                        adminRemarks =
                                            document.getString(
                                                "adminRemarks"
                                            ) ?: "",

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
                    it.status.equals(
                        "pending",
                        ignoreCase = true
                    )
                }

            "Approved" ->
                requests.filter {
                    it.status.equals(
                        "approved",
                        ignoreCase = true
                    )
                }

            "Rejected" ->
                requests.filter {
                    it.status.equals(
                        "rejected",
                        ignoreCase = true
                    )
                }

            else -> requests
        }

    if (
        showDetails &&
        selectedRequest != null
    ) {

        LeaveDetailsDialog(
            request = selectedRequest!!,
            onDismiss = {
                showDetails = false
                selectedRequest = null
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
                        Modifier.width(10.dp)
                    )

                    Text(
                        text = "Leave History",
                        color = Color.White,
                        fontSize = 23.sp,
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
                .background(background)
                .verticalScroll(
                    rememberScrollState()
                )
                .padding(padding)
                .padding(18.dp)
        ) {

            Row(
                horizontalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {

                listOf(
                    "All",
                    "Pending",
                    "Approved",
                    "Rejected"
                ).forEach { item ->

                    FilterChip(
                        selected =
                            selectedFilter == item,

                        onClick = {
                            selectedFilter = item
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
                        Modifier.fillMaxWidth(),
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
                    colors =
                        CardDefaults.cardColors(
                            containerColor =
                                Color.White
                        )
                ) {

                    Text(
                        text =
                            "No leave applications found.",
                        modifier =
                            Modifier.padding(24.dp),
                        color = Color.Gray
                    )
                }

            } else {

                filteredRequests.forEach {
                        request ->

                    LeaveHistoryCard(
                        request = request,
                        onViewDetails = {
                            selectedRequest =
                                request
                            showDetails = true
                        }
                    )

                    Spacer(
                        Modifier.height(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun LeaveHistoryCard(
    request: LeaveHistoryItem,
    onViewDetails: () -> Unit
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
                    Arrangement.SpaceBetween
            ) {

                Text(
                    request.leaveType,
                    fontSize = 18.sp,
                    fontWeight =
                        FontWeight.Bold
                )

                Text(
                    request.status
                        .replaceFirstChar {
                            it.uppercase()
                        },
                    color = statusColor,
                    fontWeight =
                        FontWeight.Bold
                )
            }

            Spacer(
                Modifier.height(8.dp)
            )

            val start =
                request.startDate
                    ?.toDate()
                    ?.let {
                        formatter.format(it)
                    } ?: "-"

            val end =
                request.endDate
                    ?.toDate()
                    ?.let {
                        formatter.format(it)
                    } ?: "-"

            Text(
                "$start - $end"
            )

            Spacer(
                Modifier.height(4.dp)
            )

            Text(
                "${request.duration} day(s)",
                color = Color.Gray
            )

            Spacer(
                Modifier.height(14.dp)
            )

            OutlinedButton(
                onClick = onViewDetails,
                modifier =
                    Modifier.fillMaxWidth()
            ) {

                Text("View Details")
            }
        }
    }
}

@Composable
fun LeaveDetailsDialog(
    request: LeaveHistoryItem,
    onDismiss: () -> Unit
) {

    val formatter =
        remember {
            SimpleDateFormat(
                "dd MMM yyyy",
                Locale.getDefault()
            )
        }

    val start =
        request.startDate
            ?.toDate()
            ?.let {
                formatter.format(it)
            } ?: "-"

    val end =
        request.endDate
            ?.toDate()
            ?.let {
                formatter.format(it)
            } ?: "-"

    AlertDialog(

        onDismissRequest =
            onDismiss,

        title = {
            Text("Leave Request Details")
        },

        text = {

            Column(
                modifier =
                    Modifier.verticalScroll(
                        rememberScrollState()
                    )
            ) {

                Text(
                    request.leaveType,
                    fontWeight =
                        FontWeight.Bold,
                    fontSize = 18.sp
                )

                Spacer(
                    Modifier.height(10.dp)
                )

                Text(
                    "Dates: $start - $end"
                )

                Text(
                    "Duration: ${request.duration} day(s)"
                )

                Spacer(
                    Modifier.height(14.dp)
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

                Text(
                    "Faculty / SSO Review",
                    fontWeight =
                        FontWeight.Bold
                )

                Text(
                    "Status: ${
                        request.facultyStatus
                            .ifBlank { "Not reviewed" }
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

                Text(
                    "HOD / Admin Review",
                    fontWeight =
                        FontWeight.Bold
                )

                Text(
                    "Status: ${
                        request.adminStatus
                            .ifBlank { "Not reviewed" }
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

                Spacer(
                    Modifier.height(18.dp)
                )

                Text(
                    "Final Status: ${
                        request.status
                            .replaceFirstChar {
                                it.uppercase()
                            }
                    }",
                    fontWeight =
                        FontWeight.Bold
                )
            }
        },

        confirmButton = {

            TextButton(
                onClick = onDismiss
            ) {
                Text("Close")
            }
        }
    )
}