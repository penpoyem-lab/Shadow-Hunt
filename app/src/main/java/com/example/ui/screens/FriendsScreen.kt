package com.example.ui.screens

import android.widget.Toast
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

@Composable
fun FriendsScreen(
    onInviteToRoom: (String) -> Unit
) {
    val context = LocalContext.current
    var searchId by remember { mutableStateOf("") }

    val friends = remember {
        mutableStateListOf(
            FriendItem("f1", "RexHunter", "hunter", "In Game - SH7KQ2", true, 15),
            FriendItem("f2", "Valkyrie", "phantom", "Online • Lobby", true, 9),
            FriendItem("f3", "ShadowKing", "reaper", "Online • Home", true, 18),
            FriendItem("f4", "Cipher_99", "cyber", "Offline (2h ago)", false, 7),
            FriendItem("f5", "Specter", "demon", "Offline (1d ago)", false, 11)
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
                    text = "SQUAD & INVITATIONS",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = ElectricBlue
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurfaceVariant)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "${friends.count { it.isOnline }} Online",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = NeonGreen
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            // ADD FRIEND SEARCH BAR
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchId,
                    onValueChange = { searchId = it.uppercase() },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Enter Player ID (e.g. SH-99X)", color = TextMuted) },
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
                            friends.add(0, FriendItem("new_${System.currentTimeMillis()}", searchId, "cyber", "Online", true, 1))
                            Toast.makeText(context, "Friend request sent to $searchId!", Toast.LENGTH_SHORT).show()
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

            Spacer(modifier = Modifier.height(16.dp))

            // FRIENDS LIST
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
                                            width = 1.5.dp,
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
                                    Text(
                                        text = friend.status,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                                        color = if (friend.isOnline) NeonGreen else TextMuted
                                    )
                                }
                            }

                            if (friend.isOnline) {
                                Button(
                                    onClick = {
                                        onInviteToRoom(friend.nickname)
                                        Toast.makeText(context, "Invitation sent to ${friend.nickname}!", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonPurple.copy(alpha = 0.25f)),
                                    border = BorderStroke(1.dp, NeonPurple),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
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
        }
    }
}
