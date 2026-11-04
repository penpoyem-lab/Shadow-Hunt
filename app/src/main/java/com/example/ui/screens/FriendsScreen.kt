package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AvatarRegistry
import com.example.model.FriendItem
import com.example.ui.components.GlassPanel
import com.example.ui.theme.*

data class FriendRequestItem(
    val id: String,
    val nickname: String,
    val avatarId: String,
    val level: Int,
    val note: String,
    val timeAgo: String
)

@Composable
fun FriendsScreen(
    onInviteToRoom: (String) -> Unit
) {
    val context = LocalContext.current
    var searchId by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Friends, 1: Requests

    val friends = remember {
        mutableStateListOf(
            FriendItem("f1", "RexHunter", "hunter", "In Game - Room SH7KQ2", true, 15),
            FriendItem("f2", "Valkyrie", "phantom", "Online • In Lobby", true, 9),
            FriendItem("f3", "ShadowKing", "reaper", "Online • In Menu", true, 18),
            FriendItem("f4", "Cipher_99", "cyber", "Offline (2h ago)", false, 7),
            FriendItem("f5", "Specter", "demon", "Offline (1d ago)", false, 11)
        )
    }

    val requests = remember {
        mutableStateListOf(
            FriendRequestItem("r1", "NeonPhantom", "phantom", 14, "Played together in Room SH7KQ2", "5m ago"),
            FriendRequestItem("r2", "NightHunter", "hunter", 12, "Wants to join your outdoor squad", "25m ago"),
            FriendRequestItem("r3", "Viper_Zero", "cyber", 8, "Sent from Global Leaderboard", "1h ago")
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .statusBarsPadding()
            .padding(bottom = 90.dp)
    ) {
        // TOP APP BAR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "FRIENDS",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    ),
                    color = Color.White
                )
                Text(
                    text = "LIVE SQUAD & REQUESTS",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = ElectricBlue
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurfaceVariant)
                    .border(1.dp, Color(0xFF1E2840), RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(NeonGreen)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${friends.count { it.isOnline }} LIVE ONLINE",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                        color = NeonGreen
                    )
                }
            }
        }

        // TAB SELECTOR: FRIENDS vs REQUESTS
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF0F1424))
                .border(1.dp, DarkCardBorder, RoundedCornerShape(14.dp))
                .padding(4.dp)
        ) {
            Surface(
                onClick = { selectedTab = 0 },
                shape = RoundedCornerShape(10.dp),
                color = if (selectedTab == 0) ElectricBlue else Color.Transparent,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "FRIENDS (${friends.size})",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    ),
                    color = if (selectedTab == 0) Color.Black else TextSecondary,
                    modifier = Modifier
                        .padding(vertical = 8.dp)
                        .wrapContentWidth(Alignment.CenterHorizontally)
                )
            }

            Surface(
                onClick = { selectedTab = 1 },
                shape = RoundedCornerShape(10.dp),
                color = if (selectedTab == 1) NeonPurple else Color.Transparent,
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier
                        .padding(vertical = 8.dp)
                        .wrapContentWidth(Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "REQUESTS",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        ),
                        color = if (selectedTab == 1) Color.White else TextSecondary
                    )
                    if (requests.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (selectedTab == 1) Color.White else NeonPurple)
                                .padding(horizontal = 6.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "${requests.size}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 10.sp
                                ),
                                color = if (selectedTab == 1) NeonPurple else Color.White
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            // SEARCH & ADD FRIEND BAR
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchId,
                    onValueChange = { searchId = it.uppercase() },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Search by Codename or Player ID", color = TextMuted, fontSize = 12.sp) },
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = ElectricBlue
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricBlue,
                        unfocusedBorderColor = DarkCardBorder,
                        focusedContainerColor = DarkSurfaceGlass,
                        unfocusedContainerColor = DarkSurfaceGlass,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(14.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Button(
                    onClick = {
                        if (searchId.isNotEmpty()) {
                            friends.add(0, FriendItem("new_${System.currentTimeMillis()}", searchId, "cyber", "Online • Added", true, 1))
                            Toast.makeText(context, "Added friend: $searchId!", Toast.LENGTH_SHORT).show()
                            searchId = ""
                        }
                    },
                    modifier = Modifier.height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = "Add Friend",
                        tint = Color.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // TAB 0: LIVE FRIENDS LIST
            if (selectedTab == 0) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(friends) { friend ->
                        val avatarOpt = AvatarRegistry.getAvatar(friend.avatarId)

                        GlassPanel(
                            modifier = Modifier.fillMaxWidth(),
                            borderColor = if (friend.isOnline) DarkCardBorder else Color(0xFF1E2030),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .border(
                                                width = 1.8.dp,
                                                color = if (friend.isOnline) NeonGreen else TextMuted,
                                                shape = CircleShape
                                            )
                                    ) {
                                        Image(
                                            painter = painterResource(id = avatarOpt.drawableRes),
                                            contentDescription = friend.nickname,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = friend.nickname,
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                color = Color.White
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Lv.${friend.level}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = GoldAccent
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .clip(CircleShape)
                                                    .background(if (friend.isOnline) NeonGreen else TextMuted)
                                            )
                                            Spacer(modifier = Modifier.width(5.dp))
                                            Text(
                                                text = friend.status,
                                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                                                color = if (friend.isOnline) NeonGreen else TextMuted
                                            )
                                        }
                                    }
                                }

                                if (friend.isOnline) {
                                    Button(
                                        onClick = {
                                            onInviteToRoom(friend.nickname)
                                            Toast.makeText(context, "Invited ${friend.nickname} to Room SH7KQ2!", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = NeonPurple.copy(alpha = 0.25f)),
                                        border = BorderStroke(1.dp, NeonPurple),
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Send, contentDescription = null, tint = NeonPurple, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Invite",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // TAB 1: PENDING FRIEND REQUESTS
                if (requests.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 60.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("No pending friend requests", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                            Text("You're all caught up!", style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp), color = TextSecondary)
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(requests) { req ->
                            val avatarOpt = AvatarRegistry.getAvatar(req.avatarId)

                            GlassPanel(
                                modifier = Modifier.fillMaxWidth(),
                                borderColor = NeonPurple.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(44.dp)
                                                    .clip(CircleShape)
                                                    .border(1.5.dp, NeonPurple, CircleShape)
                                            ) {
                                                Image(
                                                    painter = painterResource(id = avatarOpt.drawableRes),
                                                    contentDescription = req.nickname,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(text = req.nickname, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(text = "Lv.${req.level}", style = MaterialTheme.typography.labelSmall, color = GoldAccent)
                                                }
                                                Text(text = req.note, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp), color = TextSecondary)
                                            }
                                        }

                                        Text(text = req.timeAgo, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = TextMuted)
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                requests.remove(req)
                                                friends.add(0, FriendItem(req.id, req.nickname, req.avatarId, "Online • In Lobby", true, req.level))
                                                Toast.makeText(context, "${req.nickname} accepted as friend!", Toast.LENGTH_SHORT).show()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("ACCEPT", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black), color = Color.Black)
                                        }

                                        Button(
                                            onClick = {
                                                requests.remove(req)
                                                Toast.makeText(context, "Request declined", Toast.LENGTH_SHORT).show()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant),
                                            border = BorderStroke(1.dp, Color(0xFF323B52)),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("DECLINE", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = TextMuted)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
