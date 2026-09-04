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
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.Locale

data class AdminHistoryItem(
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
    val adminRemarks: String = "",
    val status: String = "",
    val adminReviewedAt: Timestamp? = null
)

@Composable
fun AdminHistoryScreen(
    onBack: () -> Unit
) {

    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()

    var historyItems by remember {
        mutableStateOf<List<AdminHistoryItem>>(emptyList())
    }

    var selectedFilter by remember {
        mutableStateOf("All")
    }

    var selectedItem by remember {
        mutableStateOf<AdminHistoryItem?>(null)
    }

    var showDetailsDialog by remember {
        mutableStateOf(false)
    }

    var isLoading by remember {
        mutableStateOf(true)
    }

    val teal = Color(0xFF0B747D)
    val background = Color(0xFFF5F7FA)

    /*
     * Listen to requests already reviewed by HOD/Admin
     */
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
                            "Unable to load history: ${error.message}",
                            Toast.LENGTH_LONG
                        ).show()

                        return@addSnapshotListener
                    }

                    historyItems =
                        snapshot?.documents
                            ?.map { document ->

                                AdminHistoryItem(

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

                                    adminRemarks =
                                        document.getString(
                                            "adminRemarks"
                                        ) ?: "",

                                    status =
                                        document.getString(
                                            "status"
                                        ) ?: "",

                                    adminReviewedAt =
                                        document.getTimestamp(
                                            "adminReviewedAt"
                                        )
                                )
                            }

                            /*
                             * History contains only requests
                             * already decided by HOD/Admin
                             */
                            ?.filter {

                                it.adminStatus.equals(
                                    "approved",
                                    ignoreCase = true
                                ) ||

                                        it.adminStatus.equals(
                                            "rejected",
                                            ignoreCase = true
                                        )
                            }

                            ?.sortedByDescending {
                                it.adminReviewedAt?.seconds ?: 0
                            }

                            ?: emptyList()

                    isLoading = false
                }

        onDispose {
            listener?.remove()
        }
    }

    val filteredItems =
        when (selectedFilter) {

            "Approved" ->
                historyItems.filter {
                    it.adminStatus.equals(
                        "approved",
                        ignoreCase = true
                    )
                }

            "Rejected" ->
                historyItems.filter {
                    it.adminStatus.equals(
                        "rejected",
                        ignoreCase = true
                    )
                }

            else ->
                historyItems
        }

    /*
     * History Details dialog
     */
    if (
        showDetailsDialog &&
        selectedItem != null
    ) {

        AdminHistoryDetailsDialog(
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
                        text = "Decision History",

                        color = Color.White,

                        fontSize = 22.sp,

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

            /*
             * FILTERS
             */
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
                            "No completed leave decisions found.",

                        modifier =
                            Modifier.padding(
                                24.dp
                            ),

                        color =
                            Color.Gray
                    )
                }

            } else {

                filteredItems.forEach { item ->

                    AdminHistoryCard(
                        item = item,

                        onViewDetails = {

                            selectedItem =
                                item

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
fun AdminHistoryCard(
    item: AdminHistoryItem,
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
        item.adminStatus.equals(
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
                        Modifier.height(
                            4.dp
                        )
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
                        RoundedCornerShape(
                            20.dp
                        ),

                    color =
                        statusBackground
                ) {

                    Text(
                        text =
                            item.adminStatus
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
                Modifier.height(
                    14.dp
                )
            )

            Text(
                text =
                    item.leaveType,

                fontWeight =
                    FontWeight.Bold
            )

            Spacer(
                Modifier.height(
                    7.dp
                )
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
                item.adminRemarks
                    .isNotBlank()
            ) {

                Spacer(
                    Modifier.height(
                        10.dp
                    )
                )

                Text(
                    text =
                        "Admin Remarks: ${item.adminRemarks}",

                    color =
                        Color.DarkGray
                )
            }

            Spacer(
                Modifier.height(
                    16.dp
                )
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
fun AdminHistoryDetailsDialog(
    item: AdminHistoryItem,
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

    val finalStatusColor =
        if (
            item.adminStatus.equals(
                "approved",
                ignoreCase = true
            )
        ) {

            Color(0xFF2E9E72)

        } else {

            Color(0xFFD74444)
        }

    AlertDialog(

        onDismissRequest =
            onDismiss,

        title = {

            Text(
                "Leave Decision Details"
            )
        },

        text = {

            Column(
                modifier =
                    Modifier.verticalScroll(
                        rememberScrollState()
                    )
            ) {

                /*
                 * STUDENT
                 */
                Text(
                    text =
                        item.studentName,

                    fontSize =
                        18.sp,

                    fontWeight =
                        FontWeight.Bold
                )

                Text(
                    text =
                        "${item.studentId} • ${item.department}",

                    color =
                        Color.Gray
                )

                Spacer(
                    Modifier.height(
                        16.dp
                    )
                )

                /*
                 * FINAL STATUS
                 */
                Surface(
                    shape =
                        RoundedCornerShape(
                            20.dp
                        ),

                    color =
                        finalStatusColor.copy(
                            alpha =
                                0.12f
                        )
                ) {

                    Text(
                        text =
                            item.adminStatus
                                .uppercase(),

                        color =
                            finalStatusColor,

                        fontWeight =
                            FontWeight.Bold,

                        modifier =
                            Modifier.padding(
                                horizontal =
                                    12.dp,

                                vertical =
                                    6.dp
                            )
                    )
                }

                Spacer(
                    Modifier.height(
                        18.dp
                    )
                )

                /*
                 * LEAVE DETAILS
                 */
                Text(
                    text =
                        "Leave Details",

                    fontWeight =
                        FontWeight.Bold
                )

                Spacer(
                    Modifier.height(
                        7.dp
                    )
                )

                Text(
                    "Leave Type: ${item.leaveType}"
                )

                Text(
                    "Start Date: $start"
                )

                Text(
                    "End Date: $end"
                )

                Text(
                    "Duration: ${item.duration} day(s)"
                )

                Spacer(
                    Modifier.height(
                        16.dp
                    )
                )

                Text(
                    text = "Reason",

                    fontWeight =
                        FontWeight.Bold
                )

                Text(
                    text =
                        item.reason
                )

                Spacer(
                    Modifier.height(
                        20.dp
                    )
                )

                /*
                 * FACULTY REVIEW
                 */
                Text(
                    text =
                        "Faculty / SSO Review",

                    fontWeight =
                        FontWeight.Bold
                )

                Text(
                    text =
                        "Status: ${
                            item.facultyStatus
                                .ifBlank {
                                    "Not reviewed"
                                }
                        }"
                )

                if (
                    item.facultyRemarks
                        .isNotBlank()
                ) {

                    Text(
                        text =
                            "Remarks: ${item.facultyRemarks}"
                    )
                }

                Spacer(
                    Modifier.height(
                        20.dp
                    )
                )

                /*
                 * ADMIN REVIEW
                 */
                Text(
                    text =
                        "HOD / Admin Decision",

                    fontWeight =
                        FontWeight.Bold
                )

                Text(
                    text =
                        "Status: ${
                            item.adminStatus
                                .replaceFirstChar {
                                    it.uppercase()
                                }
                        }"
                )

                if (
                    item.adminRemarks
                        .isNotBlank()
                ) {

                    Text(
                        text =
                            "Remarks: ${item.adminRemarks}"
                    )
                }

                Spacer(
                    Modifier.height(
                        20.dp
                    )
                )

                Text(
                    text =
                        "Final Status: ${
                            item.status
                                .ifBlank {
                                    item.adminStatus
                                }
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
                onClick =
                    onDismiss
            ) {

                Text("Close")
            }
        }
    )
}