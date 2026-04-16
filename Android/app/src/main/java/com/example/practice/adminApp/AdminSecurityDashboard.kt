package com.example.practice.adminApp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.practice.ui.theme.*

@Composable
fun AdminSecurityDashboard(
    modifier: Modifier = Modifier,
    navController: NavController,
    adminViewModel: AdminViewModel
) {
    val securityState by adminViewModel.securityStats.observeAsState()

    LaunchedEffect(Unit) {
        adminViewModel.fetchSecurityStats()
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
                Icon(Icons.Default.Security, contentDescription = "Security", tint = Neon_Cyan)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Security Dashboard", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Text_Primary)
            }
            
            IconButton(onClick = { adminViewModel.fetchSecurityStats() }) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Neon_Cyan)
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Monitor anti-spoofing and fraud detection events across all sessions.", 
            color = Text_Secondary, 
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.height(24.dp))

        when (val state = securityState) {
            is AdminViewModel.SecurityStatsState.Loading -> {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Neon_Cyan)
                }
            }
            is AdminViewModel.SecurityStatsState.Success -> {
                val stats = state.data
                
                // Threat Stats Grid
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        DashboardStatCard(
                            title = "Face Mismatch",
                            value = "${stats.faceMismatchCount}",
                            icon = Icons.Default.Warning,
                            color = Neon_Red,
                            modifier = Modifier.weight(1f)
                        )
                        DashboardStatCard(
                            title = "Replay Attacks",
                            value = "${stats.replayAttempts}",
                            icon = Icons.Default.Refresh,
                            color = Neon_Orange,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        DashboardStatCard(
                            title = "Geofence Fails",
                            value = "${stats.geofenceFailures}",
                            icon = Icons.Default.Warning,
                            color = Neon_Purple,
                            modifier = Modifier.weight(1f)
                        )
                        DashboardStatCard(
                            title = "Token Invalid",
                            value = "${stats.tokenInvalid}",
                            icon = Icons.Default.Warning,
                            color = Neon_Blue,
                            modifier = Modifier.weight(1f)
                        )
                        DashboardStatCard(
                            title = "Timing Reject",
                            value = "${stats.timingRejected}",
                            icon = Icons.Default.Warning,
                            color = Neon_Cyan,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                Text("Recent Security Events", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Text_Primary)
                Spacer(modifier = Modifier.height(12.dp))

                if (stats.recentEvents.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = Neon_Green, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("System Secure", color = Neon_Green, fontWeight = FontWeight.Bold)
                            Text("No recent security events", color = Text_Secondary, fontSize = 14.sp)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(stats.recentEvents) { event ->
                            AuditLogItem(
                                log = event, 
                                isExpanded = false, // Simplified for recent events
                                onExpandToggle = { }
                            )
                        }
                    }
                }
            }
            is AdminViewModel.SecurityStatsState.Error -> {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(state.message, color = Neon_Red, fontSize = 14.sp)
                }
            }
            else -> {}
        }
    }
}
