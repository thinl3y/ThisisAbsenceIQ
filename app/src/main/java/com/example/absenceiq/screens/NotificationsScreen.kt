package com.example.absenceiq.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import java.util.concurrent.TimeUnit

data class NotificationItem(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val type: String = "",
    val leaveRequestId: String = "",
    val isRead: Boolean = false,
    val createdAt: Timestamp? = null
)

data class NotificationLeaveDetails(
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
    val adminRemarks: String = ""
)

@Composable
fun NotificationsScreen(
    onHomeClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onProfileClick: () -> Unit
) {

    val context = LocalContext.current

    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    val uid = auth.currentUser?.uid

    var notifications by remember {
        mutableStateOf<List<NotificationItem>>(emptyList())
    }

    var selectedLeave by remember {
        mutableStateOf<NotificationLeaveDetails?>(null)
    }

    var showLeaveDialog by remember {
        mutableStateOf(false)
    }

    var isLoading by remember {
        mutableStateOf(true)
    }

    val background =
        Color(0xFFF5F7FA)

    val teal =
        Color(0xFF0F7A83)

    /*
     * Real-time notifications
     */
    DisposableEffect(uid) {

        var listener: ListenerRegistration? = null

        if (uid != null) {

            listener =
                db.collection("notifications")
                    .whereEqualTo(
                        "userUid",
                        uid
                    )
                    .addSnapshotListener { snapshot, error ->

                        if (error != null) {

                            isLoading = false
                            return@addSnapshotListener
                        }

                        notifications =
                            snapshot?.documents
                                ?.map { document ->

                                    NotificationItem(

                                        id =
                                            document.id,

                                        title =
                                            document.getString(
                                                "title"
                                            ) ?: "",

                                        message =
                                            document.getString(
                                                "message"
                                            ) ?: "",

                                        type =
                                            document.getString(
                                                "type"
                                            ) ?: "",

                                        leaveRequestId =
                                            document.getString(
                                                "leaveRequestId"
                                            ) ?: "",

                                        isRead =
                                            document.getBoolean(
                                                "isRead"
                                            ) ?: false,

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

    val unreadCount =
        notifications.count {
            !it.isRead
        }

    /*
     * Leave details dialog
     */
    if (
        showLeaveDialog &&
        selectedLeave != null
    ) {

        NotificationLeaveDetailsDialog(
            request =
                selectedLeave!!,

            onDismiss = {

                showLeaveDialog =
                    false

                selectedLeave =
                    null
            }
        )
    }

    Scaffold(

        bottomBar = {

            NavigationBar {

                NavigationBarItem(
                    selected = false,

                    onClick = onHomeClick,

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
                        Text("≡")
                    },

                    label = {
                        Text("History")
                    }
                )

                NavigationBarItem(
                    selected = true,

                    onClick = { },

                    icon = {
                        Text("●")
                    },

                    label = {
                        Text("Notifications")
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
                    background
                )
                .verticalScroll(
                    rememberScrollState()
                )
                .padding(
                    paddingValues
                )
                .padding(
                    horizontal =
                        18.dp,

                    vertical =
                        24.dp
                )
        ) {

            /*
             * HEADER
             */
            Row(
                modifier =
                    Modifier.fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.SpaceBetween,

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Text(
                    text =
                        "Notifications",

                    fontSize =
                        28.sp,

                    fontWeight =
                        FontWeight.Bold
                )

                Text(
                    text =
                        "$unreadCount updates",

                    color =
                        teal,

                    fontWeight =
                        FontWeight.Bold
                )
            }

            Spacer(
                Modifier.height(
                    24.dp
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
                notifications.isEmpty()
            ) {

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

                    Column(
                        modifier =
                            Modifier.padding(
                                30.dp
                            ),

                        horizontalAlignment =
                            Alignment.CenterHorizontally
                    ) {

                        Text(
                            text =
                                "No notifications yet",

                            fontWeight =
                                FontWeight.Bold
                        )

                        Spacer(
                            Modifier.height(
                                6.dp
                            )
                        )

                        Text(
                            text =
                                "Updates about your leave requests will appear here.",

                            color =
                                Color.Gray
                        )
                    }
                }

            } else {

                notifications.forEach {
                        notification ->

                    NotificationCard(

                        notification =
                            notification,

                        onClick = {

                            /*
                             * STEP 1:
                             * mark as read
                             */
                            if (
                                !notification.isRead
                            ) {

                                db.collection(
                                    "notifications"
                                )
                                    .document(
                                        notification.id
                                    )
                                    .update(
                                        "isRead",
                                        true
                                    )
                            }

                            /*
                             * STEP 2:
                             * load associated leave request
                             */
                            if (
                                notification.leaveRequestId
                                    .isBlank()
                            ) {

                                Toast.makeText(
                                    context,
                                    "Leave request information is unavailable",
                                    Toast.LENGTH_SHORT
                                ).show()

                                return@NotificationCard
                            }

                            db.collection(
                                "leave_requests"
                            )
                                .document(
                                    notification.leaveRequestId
                                )
                                .get()

                                .addOnSuccessListener {
                                        document ->

                                    if (
                                        !document.exists()
                                    ) {

                                        Toast.makeText(
                                            context,
                                            "Leave request was not found",
                                            Toast.LENGTH_SHORT
                                        ).show()

                                        return@addOnSuccessListener
                                    }

                                    selectedLeave =
                                        NotificationLeaveDetails(

                                            id =
                                                document.id,

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
                                                ) ?: ""
                                        )

                                    showLeaveDialog =
                                        true
                                }

                                .addOnFailureListener {
                                        error ->

                                    Toast.makeText(
                                        context,
                                        "Unable to load leave request: ${error.message}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
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
                    30.dp
                )
            )
        }
    }
}


@Composable
fun NotificationCard(
    notification: NotificationItem,
    onClick: () -> Unit
) {

    val iconText =
        when (
            notification.type.lowercase()
        ) {

            "approved" ->
                "✓"

            "rejected" ->
                "×"

            "review" ->
                "⌛"

            else ->
                "●"
        }

    val iconBackground =
        when (
            notification.type.lowercase()
        ) {

            "approved" ->
                Color(
                    0xFFE6F6EF
                )

            "rejected" ->
                Color(
                    0xFFFDEBEC
                )

            "review" ->
                Color(
                    0xFFFFF2DE
                )

            else ->
                Color(
                    0xFFECEFF4
                )
        }

    val iconColor =
        when (
            notification.type.lowercase()
        ) {

            "approved" ->
                Color(
                    0xFF2E9D72
                )

            "rejected" ->
                Color(
                    0xFFD74444
                )

            "review" ->
                Color(
                    0xFF9A6A2F
                )

            else ->
                Color.Gray
        }

    /*
     * Unread notifications have
     * a very light teal background.
     */
    val cardColor =
        if (
            notification.isRead
        ) {

            Color.White

        } else {

            Color(
                0xFFF0FAFA
            )
        }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onClick()
            },

        shape =
            RoundedCornerShape(
                18.dp
            ),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    cardColor
            )
    ) {

        Row(
            modifier =
                Modifier.padding(
                    18.dp
                ),

            verticalAlignment =
                Alignment.Top
        ) {

            /*
             * STATUS ICON
             */
            Box(
                modifier =
                    Modifier
                        .size(
                            46.dp
                        )
                        .background(
                            color =
                                iconBackground,

                            shape =
                                CircleShape
                        ),

                contentAlignment =
                    Alignment.Center
            ) {

                Text(
                    text =
                        iconText,

                    color =
                        iconColor,

                    fontWeight =
                        FontWeight.Bold,

                    fontSize =
                        19.sp
                )
            }

            Spacer(
                Modifier.width(
                    14.dp
                )
            )

            Column(
                modifier =
                    Modifier.weight(
                        1f
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
                            notification.title,

                        fontSize =
                            17.sp,

                        fontWeight =
                            if (
                                notification.isRead
                            )
                                FontWeight.SemiBold
                            else
                                FontWeight.Bold
                    )

                    /*
                     * Unread dot
                     */
                    if (
                        !notification.isRead
                    ) {

                        Box(
                            modifier =
                                Modifier
                                    .size(
                                        8.dp
                                    )
                                    .background(
                                        color =
                                            Color(
                                                0xFF0F7A83
                                            ),

                                        shape =
                                            CircleShape
                                    )
                        )
                    }
                }

                Spacer(
                    Modifier.height(
                        4.dp
                    )
                )

                Text(
                    text =
                        notification.message,

                    color =
                        Color.DarkGray
                )

                Spacer(
                    Modifier.height(
                        18.dp
                    )
                )

                Text(
                    text =
                        formatRelativeTime(
                            notification.createdAt
                        ),

                    color =
                        Color.Gray,

                    fontSize =
                        13.sp
                )
            }
        }
    }
}


@Composable
fun NotificationLeaveDetailsDialog(
    request: NotificationLeaveDetails,
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
            }
            ?: "-"

    val end =
        request.endDate
            ?.toDate()
            ?.let {
                formatter.format(it)
            }
            ?: "-"

    val statusColor =
        when (
            request.status.lowercase()
        ) {

            "approved" ->
                Color(
                    0xFF2E9E72
                )

            "rejected" ->
                Color(
                    0xFFD74444
                )

            else ->
                Color(
                    0xFFE49317
                )
        }

    AlertDialog(

        onDismissRequest =
            onDismiss,

        title = {

            Text(
                "Leave Request Details"
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
                 * FINAL STATUS
                 */
                Surface(
                    shape =
                        RoundedCornerShape(
                            20.dp
                        ),

                    color =
                        statusColor.copy(
                            alpha = 0.12f
                        )
                ) {

                    Text(
                        text =
                            request.status
                                .uppercase(),

                        color =
                            statusColor,

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

                Text(
                    text =
                        request.leaveType,

                    fontSize =
                        19.sp,

                    fontWeight =
                        FontWeight.Bold
                )

                Spacer(
                    Modifier.height(
                        10.dp
                    )
                )

                Text(
                    text =
                        "Dates: $start - $end"
                )

                Text(
                    text =
                        "Duration: ${request.duration} day(s)"
                )

                Spacer(
                    Modifier.height(
                        16.dp
                    )
                )

                Text(
                    text =
                        "Reason",

                    fontWeight =
                        FontWeight.Bold
                )

                Text(
                    text =
                        request.reason
                )

                Spacer(
                    Modifier.height(
                        20.dp
                    )
                )

                /*
                 * FACULTY
                 */
                Text(
                    text =
                        "Faculty / SSO Review",

                    fontWeight =
                        FontWeight.Bold
                )

                Spacer(
                    Modifier.height(
                        5.dp
                    )
                )

                Text(
                    text =
                        "Status: ${
                            request.facultyStatus
                                .ifBlank {
                                    "Not reviewed"
                                }
                        }"
                )

                if (
                    request.facultyRemarks
                        .isNotBlank()
                ) {

                    Text(
                        text =
                            "Remarks: ${request.facultyRemarks}"
                    )
                }

                Spacer(
                    Modifier.height(
                        20.dp
                    )
                )

                /*
                 * ADMIN
                 */
                Text(
                    text =
                        "HOD / Admin Review",

                    fontWeight =
                        FontWeight.Bold
                )

                Spacer(
                    Modifier.height(
                        5.dp
                    )
                )

                Text(
                    text =
                        "Status: ${
                            request.adminStatus
                                .ifBlank {
                                    "Not reviewed"
                                }
                        }"
                )

                if (
                    request.adminRemarks
                        .isNotBlank()
                ) {

                    Text(
                        text =
                            "Remarks: ${request.adminRemarks}"
                    )
                }
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


fun formatRelativeTime(
    timestamp: Timestamp?
): String {

    if (timestamp == null) {
        return ""
    }

    val now =
        System.currentTimeMillis()

    val time =
        timestamp.toDate().time

    val difference =
        now - time

    val minutes =
        TimeUnit.MILLISECONDS
            .toMinutes(
                difference
            )

    val hours =
        TimeUnit.MILLISECONDS
            .toHours(
                difference
            )

    val days =
        TimeUnit.MILLISECONDS
            .toDays(
                difference
            )

    return when {

        minutes < 1 ->
            "Just now"

        minutes < 60 ->
            "$minutes minute${
                if (
                    minutes == 1L
                )
                    ""
                else
                    "s"
            } ago"

        hours < 24 ->
            "$hours hour${
                if (
                    hours == 1L
                )
                    ""
                else
                    "s"
            } ago"

        else ->
            "$days day${
                if (
                    days == 1L
                )
                    ""
                else
                    "s"
            } ago"
    }
}