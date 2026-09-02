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
    val createdAt: Timestamp? = null
)

@Composable
fun FacultyDashboardScreen(
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

    var leaveRequests by remember {
        mutableStateOf<List<FacultyLeaveRequest>>(emptyList())
    }

    var isLoading by remember {
        mutableStateOf(true)
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

    val teal = Color(0xFF0F7A83)
    val background = Color(0xFFF5F7F8)

    /*
     * Read logged-in Faculty / SSO information.
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
     * Listen for leave requests belonging
     * to the faculty's department.
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
                                "Unable to load leave requests: ${error.message}",
                                Toast.LENGTH_LONG
                            ).show()

                            return@addSnapshotListener
                        }

                        val requests =
                            snapshot?.documents
                                ?.map { document ->

                                    FacultyLeaveRequest(

                                        id = document.id,

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

                                        createdAt =
                                            document.getTimestamp(
                                                "createdAt"
                                            )
                                    )
                                }

                                /*
                                 * Faculty dashboard should show
                                 * only requests awaiting faculty review.
                                 */
                                ?.filter {

                                    it.facultyStatus.equals(
                                        "pending",
                                        ignoreCase = true
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
     * Review dialog.
     */
    if (showReviewDialog && selectedRequest != null) {

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
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(
                        modifier = Modifier.height(4.dp)
                    )

                    Text(
                        selectedRequest!!.leaveType
                    )

                    Spacer(
                        modifier = Modifier.height(16.dp)
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

                        val request =
                            selectedRequest
                                ?: return@Button

                        /*
                         * Rejection requires a reason.
                         */
                        if (
                            reviewAction == "reject" &&
                            remarks.isBlank()
                        ) {

                            Toast.makeText(
                                context,
                                "Please provide a reason for rejection",
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

                                    /*
                                     * Overall status remains pending
                                     * until final HOD/Admin approval.
                                     */
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

                                Toast.makeText(
                                    context,

                                    if (
                                        reviewAction ==
                                        "approve"
                                    )
                                        "Leave request forwarded to HOD/Admin"
                                    else
                                        "Leave request rejected",

                                    Toast.LENGTH_LONG
                                ).show()

                                showReviewDialog = false
                                selectedRequest = null
                                remarks = ""
                            }

                            .addOnFailureListener { error ->

                                Toast.makeText(
                                    context,
                                    "Unable to update request: ${error.message}",
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
                        remarks = ""
                    }
                ) {

                    Text("Cancel")
                }
            }
        )
    }

    Scaffold {

            paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(background)
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
                        horizontal = 24.dp,
                        vertical = 28.dp
                    )
            ) {

                Text(
                    text = "Faculty / SSO",
                    color =
                        Color.White.copy(
                            alpha = 0.8f
                        ),
                    fontSize = 15.sp
                )

                Spacer(
                    modifier =
                        Modifier.height(5.dp)
                )

                Text(
                    text = facultyName,

                    color = Color.White,

                    fontSize = 27.sp,

                    fontWeight =
                        FontWeight.Bold
                )

                if (department.isNotBlank()) {

                    Spacer(
                        modifier =
                            Modifier.height(5.dp)
                    )

                    Text(
                        text =
                            "$department Department",

                        color =
                            Color.White.copy(
                                alpha = 0.85f
                            )
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {

                /*
                 * SUMMARY CARD
                 */
                Card(
                    modifier =
                        Modifier.fillMaxWidth(),

                    shape =
                        RoundedCornerShape(
                            18.dp
                        ),

                    colors =
                        CardDefaults.cardColors(
                            containerColor =
                                Color.White
                        )
                ) {

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(22.dp),

                        verticalAlignment =
                            Alignment.CenterVertically,

                        horizontalArrangement =
                            Arrangement.SpaceBetween
                    ) {

                        Column {

                            Text(
                                "Pending Requests",
                                color = Color.Gray
                            )

                            Spacer(
                                Modifier.height(4.dp)
                            )

                            Text(
                                leaveRequests.size
                                    .toString(),

                                fontSize = 32.sp,

                                fontWeight =
                                    FontWeight.Bold,

                                color = teal
                            )
                        }

                        Text(
                            "Awaiting Review",
                            color = teal,
                            fontWeight =
                                FontWeight.SemiBold
                        )
                    }
                }

                Spacer(
                    modifier =
                        Modifier.height(26.dp)
                )

                Text(
                    text =
                        "Leave Requests",

                    fontSize = 22.sp,

                    fontWeight =
                        FontWeight.Bold
                )

                Spacer(
                    modifier =
                        Modifier.height(14.dp)
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
                    leaveRequests.isEmpty()
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

                        Column(
                            modifier =
                                Modifier.padding(
                                    28.dp
                                ),

                            horizontalAlignment =
                                Alignment.CenterHorizontally
                        ) {

                            Text(
                                "No pending leave requests",
                                fontWeight =
                                    FontWeight.Bold
                            )

                            Spacer(
                                Modifier.height(6.dp)
                            )

                            Text(
                                "New student requests will appear here automatically.",
                                color = Color.Gray
                            )
                        }
                    }

                } else {

                    leaveRequests.forEach {
                            request ->

                        FacultyLeaveRequestCard(

                            request = request,

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
                            modifier =
                                Modifier.height(
                                    14.dp
                                )
                        )
                    }
                }

                Spacer(
                    modifier =
                        Modifier.height(30.dp)
                )

                OutlinedButton(
                    onClick = onLogout,

                    modifier =
                        Modifier.fillMaxWidth()
                ) {

                    Text("Logout")
                }

                Spacer(
                    modifier =
                        Modifier.height(30.dp)
                )
            }
        }
    }
}


@Composable
fun FacultyLeaveRequestCard(

    request: FacultyLeaveRequest,

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

            Text(
                text =
                    request.studentName,

                fontSize = 19.sp,

                fontWeight =
                    FontWeight.Bold
            )

            Spacer(
                modifier =
                    Modifier.height(
                        3.dp
                    )
            )

            Text(
                text =
                    "${request.studentId} • ${request.department}",

                color = Color.Gray,

                fontSize = 13.sp
            )

            Spacer(
                modifier =
                    Modifier.height(
                        16.dp
                    )
            )

            Text(
                text =
                    request.leaveType,

                fontWeight =
                    FontWeight.Bold,

                fontSize =
                    17.sp
            )

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
                    "$start - $end"
            )

            Spacer(
                modifier =
                    Modifier.height(
                        6.dp
                    )
            )

            Text(
                text =
                    "${request.duration} day(s)",

                color =
                    Color.DarkGray
            )

            Spacer(
                modifier =
                    Modifier.height(
                        16.dp
                    )
            )

            Text(
                text = "Reason",

                fontWeight =
                    FontWeight.SemiBold
            )

            Spacer(
                modifier =
                    Modifier.height(
                        5.dp
                    )
            )

            Text(
                text =
                    request.reason,

                color =
                    Color.DarkGray
            )

            Spacer(
                modifier =
                    Modifier.height(
                        20.dp
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
                        Modifier.weight(1f),

                    colors =
                        ButtonDefaults
                            .outlinedButtonColors(
                                contentColor =
                                    Color(
                                        0xFFC62828
                                    )
                            )
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