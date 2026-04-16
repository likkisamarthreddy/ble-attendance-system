package com.example.practice.adminApp

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.practice.ResponsesModel.AuditLog
import com.example.practice.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminAuditLogs(
    modifier: Modifier = Modifier,
    navController: NavController,
    adminViewModel: AdminViewModel
) {
    val auditLogsState by adminViewModel.auditLogs.observeAsState()
    var currentPage by remember { mutableStateOf(1) }
    var selectedAction by remember { mutableStateOf<String?>("All Actions") }
    var expanded by remember { mutableStateOf(false) }
    var expandedLogId by remember { mutableStateOf<Int?>(null) }

    val actionOptions = listOf(
        "All Actions",
        "ATTENDANCE_MARKED",
        "ATTENDANCE_MANUAL",
        "FACE_VERIFY_FAILED",
        "FACE_REGISTERED",
        "TOKEN_INVALID",
        "GEOFENCE_FAILED",
        "REPLAY_REJECTED",
        "TIMING_REJECTED",
        "CSV_UPLOAD",
        "ADMIN_ACTION"
    )

    LaunchedEffect(currentPage, selectedAction) {
        val filterAction = if (selectedAction == "All Actions") null else selectedAction
        adminViewModel.fetchAuditLogs(page = currentPage, limit = 20, action = filterAction)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Background_Deep)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.List, contentDescription = "Audit Logs", tint = Neon_Blue)
                Spacer(modifier = Modifier.width(8.dp))
                Text("System Audit Logs", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Text_Primary)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        // Filter Dropdown
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = selectedAction ?: "All Actions",
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Neon_Cyan,
                    unfocusedBorderColor = Surface_Elevated,
                    focusedContainerColor = Surface_Elevated,
                    unfocusedContainerColor = Surface_Elevated,
                    focusedTextColor = Text_Primary,
                    unfocusedTextColor = Text_Primary
                ),
                shape = RoundedCornerShape(12.dp)
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(Surface_Elevated)
            ) {
                actionOptions.forEach { selectionOption ->
                    DropdownMenuItem(
                        text = { Text(selectionOption, color = Text_Primary) },
                        onClick = {
                            selectedAction = selectionOption
                            currentPage = 1
                            expanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (val state = auditLogsState) {
            is AdminViewModel.AuditLogsState.Loading -> {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Neon_Cyan)
                }
            }
            is AdminViewModel.AuditLogsState.Success -> {
                val logsList = state.data.logs
                val totalPages = state.data.totalPages

                if (logsList.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("No logs found.", color = Text_Secondary, fontSize = 16.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(logsList) { log ->
                            AuditLogItem(
                                log = log, 
                                isExpanded = expandedLogId == log.id,
                                onExpandToggle = {
                                    expandedLogId = if (expandedLogId == log.id) null else log.id
                                }
                            )
                        }
                    }

                    // Pagination
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { if (currentPage > 1) currentPage-- },
                            enabled = currentPage > 1,
                            colors = ButtonDefaults.buttonColors(containerColor = Surface_Elevated)
                        ) {
                            Text("Prev", color = if (currentPage > 1) Neon_Cyan else Color.Gray)
                        }
                        
                        Text("Page $currentPage of $totalPages", color = Text_Secondary)
                        
                        Button(
                            onClick = { if (currentPage < totalPages) currentPage++ },
                            enabled = currentPage < totalPages,
                            colors = ButtonDefaults.buttonColors(containerColor = Surface_Elevated)
                        ) {
                            Text("Next", color = if (currentPage < totalPages) Neon_Cyan else Color.Gray)
                        }
                    }
                }
            }
            is AdminViewModel.AuditLogsState.Error -> {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(state.message, color = Neon_Red, fontSize = 14.sp)
                }
            }
            else -> {}
        }
    }
}

@Composable
fun AuditLogItem(log: AuditLog, isExpanded: Boolean, onExpandToggle: () -> Unit) {
    val statusColor = when(log.status) {
        "SUCCESS" -> Neon_Green
        "FAILURE" -> Neon_Red
        "WARNING" -> Neon_Orange
        else -> Text_Secondary
    }

    val dateFormat = SimpleDateFormat("MMM d, HH:mm:ss", Locale.getDefault())
    val formattedDate = try {
        val date = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.parse(log.timestamp)
        date?.let { dateFormat.format(it) } ?: log.timestamp
    } catch (e: Exception) { log.timestamp }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onExpandToggle() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Surface_Elevated),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = log.action.replace("_", " "), 
                    color = Text_Primary, 
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(text = formattedDate, color = Text_Secondary, fontSize = 12.sp)
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "${log.role} #${log.userId}", color = Text_Secondary, fontSize = 12.sp)
                
                Box(
                    modifier = Modifier
                        .background(statusColor.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                        .border(1.dp, statusColor.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(text = log.status, color = statusColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
            
            if (isExpanded) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(8.dp))
                
                Text("IP: ${log.ipAddress ?: "N/A"}", color = Text_Secondary, fontSize = 12.sp)
                
                if (log.details != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = log.details.toString(),
                            color = Text_Secondary,
                            fontSize = 12.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}
