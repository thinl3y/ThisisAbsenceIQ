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
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.Locale

data class FacultyHistoryItem(
    val id: String = "",
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
    val adminStatus: String = "",
    val status: String = "",
    val facultyReviewedAt: Timestamp? = null
)

@Composable
fun FacultyHistoryScreen(
    onBack: () -> Unit
) {

    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    val uid = auth.currentUser?.uid

    var department by remember {
        mutableStateOf("")
    }

    var historyItems by remember {
        mutableStateOf<List<FacultyHistoryItem>>(emptyList())
    }

    var selectedFilter by remember {
        mutableStateOf("All")
    }

    var selectedItem by remember {
        mutableStateOf<FacultyHistoryItem?>(null)
    }

    var showDetailsDialog by remember {
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
                        document.getString("department")
                            ?: ""
                }
        }
    }

    /*
     * Listen to reviewed leave requests
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
                                "Unable to load history: ${error.message}",
                                Toast.LENGTH_LONG
                            ).show()

                            return@addSnapshotListener
                        }

                        historyItems =
                            snapshot?.documents
                                ?.map { document ->

                                    FacultyHistoryItem(

                                        id =
                                            document.id,

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
                                            ) ?: "",

                                        facultyRemarks =
                                            document.getString(
                                                "facultyRemarks"
                                            ) ?: "",

                                        adminStatus =
                                            document.getString(
                                                "adminStatus"
                                            ) ?: "",

                                        status =
                                            document.getString(
                                                "status"
                                            ) ?: "",

                                        facultyReviewedAt =
                                            document.getTimestamp(
                                                "facultyReviewedAt"
                                            )
                                    )
                                }
                                ?.filter {
                                    !it.facultyStatus.equals(
                                        "pending",
                                        ignoreCase = true
                                    ) &&
                                            it.facultyStatus.isNotBlank()
                                }
                                ?.sortedByDescending {
                                    it.facultyReviewedAt?.seconds ?: 0
                                }
                                ?: emptyList()

                        isLoading = false
                    }
        }

        onDispose {
            listener?.remove()
        }
    }

    val filteredItems =
        when (selectedFilter) {

            "Approved" ->
                historyItems.filter {
                    it.facultyStatus.equals(
                        "approved",
                        ignoreCase = true
                    )
                }

            "Rejected" ->
                historyItems.filter {
                    it.facultyStatus.equals(
                        "rejected",
                        ignoreCase = true
                    )
                }

            else ->
                historyItems
        }

    if (
        showDetailsDialog &&
        selectedItem != null
    ) {

        FacultyHistoryDetailsDialog(
            item = selectedItem!!,
            onDismiss = {
                showDetailsDialog = false
                selectedItem = null
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
                        text = "Review History",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(40.dp),

                    contentAlignment =
                        Alignment.Center
                ) {

                    CircularProgressIndicator()
                }

            } else if (
                filteredItems.isEmpty()
            ) {

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

                    Text(
                        text =
                            "No reviewed leave requests found.",

                        modifier =
                            Modifier.padding(24.dp),

                        color =
                            Color.Gray
                    )
                }

            } else {

                filteredItems.forEach { item ->

                    FacultyHistoryCard(
                        item = item,
                        onViewDetails = {
                            selectedItem = item
                            showDetailsDialog = true
                        }
                    )

                    Spacer(
                        Modifier.height(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun FacultyHistoryCard(
    item: FacultyHistoryItem,
    onViewDetails: () -> Unit
) {

    val formatter =
        remember {
            SimpleDateFormat(
                "dd MMM yyyy",
                Locale.getDefault()
            )
        }

    val approved =
        item.facultyStatus.equals(
            "approved",
            ignoreCase = true
        )

    val statusColor =
        if (approved)
            Color(0xFF2E9E72)
        else
            Color(0xFFD74444)

    val statusBackground =
        if (approved)
            Color(0xFFE4F5EC)
        else
            Color(0xFFFDEBEC)

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
                            item.studentName,

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
                            "${item.studentId} • ${item.department}",

                        color =
                            Color.Gray,

                        fontSize =
                            13.sp
                    )
                }

                Surface(
                    shape =
                        RoundedCornerShape(20.dp),

                    color =
                        statusBackground
                ) {

                    Text(
                        text =
                            item.facultyStatus
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
                text = item.leaveType,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                Modifier.height(7.dp)
            )

            val start =
                item.startDate
                    ?.toDate()
                    ?.let {
                        formatter.format(it)
                    }
                    ?: "-"

            val end =
                item.endDate
                    ?.toDate()
                    ?.let {
                        formatter.format(it)
                    }
                    ?: "-"

            Text(
                text =
                    "$start - $end • ${item.duration} day(s)"
            )

            if (
                item.facultyRemarks.isNotBlank()
            ) {

                Spacer(
                    Modifier.height(10.dp)
                )

                Text(
                    text =
                        "Remarks: ${item.facultyRemarks}",

                    color =
                        Color.DarkGray
                )
            }

            Spacer(
                Modifier.height(16.dp)
            )

            OutlinedButton(
                onClick =
                    onViewDetails,

                modifier =
                    Modifier.fillMaxWidth()
            ) {

                Text("View Details")
            }
        }
    }
}

@Composable
fun FacultyHistoryDetailsDialog(
    item: FacultyHistoryItem,
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
        item.startDate
            ?.toDate()
            ?.let {
                formatter.format(it)
            }
            ?: "-"

    val end =
        item.endDate
            ?.toDate()
            ?.let {
                formatter.format(it)
            }
            ?: "-"

    AlertDialog(

        onDismissRequest =
            onDismiss,

        title = {
            Text("Reviewed Leave Request")
        },

        text = {

            Column(
                modifier =
                    Modifier.verticalScroll(
                        rememberScrollState()
                    )
            ) {

                Text(
                    item.studentName,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    "${item.studentId} • ${item.department}",
                    color = Color.Gray
                )

                Spacer(
                    Modifier.height(16.dp)
                )

                Text(
                    item.leaveType,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    "Dates: $start - $end"
                )

                Text(
                    "Duration: ${item.duration} day(s)"
                )

                Spacer(
                    Modifier.height(16.dp)
                )

                Text(
                    "Reason",
                    fontWeight = FontWeight.Bold
                )

                Text(item.reason)

                Spacer(
                    Modifier.height(18.dp)
                )

                Text(
                    "Faculty / SSO Decision",
                    fontWeight = FontWeight.Bold
                )

                Text(
                    "Status: ${
                        item.facultyStatus
                            .replaceFirstChar {
                                it.uppercase()
                            }
                    }"
                )

                if (
                    item.facultyRemarks.isNotBlank()
                ) {

                    Text(
                        "Remarks: ${item.facultyRemarks}"
                    )
                }

                Spacer(
                    Modifier.height(18.dp)
                )

                Text(
                    "HOD / Admin Progress",
                    fontWeight = FontWeight.Bold
                )

                Text(
                    "Status: ${
                        item.adminStatus
                            .ifBlank {
                                "Not reviewed"
                            }
                    }"
                )

                Spacer(
                    Modifier.height(12.dp)
                )

                Text(
                    "Final Status: ${
                        item.status
                            .ifBlank {
                                "Pending"
                            }
                            .replaceFirstChar {
                                it.uppercase()
                            }
                    }",
                    fontWeight = FontWeight.Bold
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