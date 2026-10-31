package com.hush.todo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hush.todo.data.entities.ProjectEntity
import com.hush.todo.ui.theme.FragmentMono
import com.hush.todo.ui.theme.HushColors
import com.hush.todo.ui.theme.HushType
import com.hush.todo.ui.theme.Inter

@Composable
fun NavDrawer(
    projects: List<ProjectEntity>,
    onProject: (ProjectEntity) -> Unit,
    onSettings: () -> Unit = {},
    onReview: () -> Unit = {},
    onDismiss: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0x99000000))
            .clickable(onClick = onDismiss),
    ) {
        Column(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.82f)
                .background(Color(0xF20D0D0D))
                .padding(16.dp)
                .clickable(enabled = false, onClick = {}),
        ) {
            Row(
                Modifier.fillMaxWidth().padding(top = 32.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(HushColors.Ink),
                    contentAlignment = Alignment.Center,
                ) { Text("h", color = Color.White, fontStyle = FontStyle.Italic, fontSize = 18.sp) }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("Hush", color = Color.White, fontWeight = FontWeight.SemiBold, fontFamily = Inter)
                    Text("your quiet agent", color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp, fontFamily = FragmentMono)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }
            Spacer(Modifier.height(32.dp))
            Text("PROJECTS", color = Color.White.copy(alpha = 0.35f), fontSize = 10.sp, fontFamily = FragmentMono, modifier = Modifier.padding(bottom = 8.dp))
            projects.forEach { p ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onProject(p); onDismiss() }
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(p.emoji, fontSize = 16.sp)
                    Spacer(Modifier.width(10.dp))
                    Text(p.name, color = Color.White.copy(alpha = 0.85f), fontSize = 14.sp, fontFamily = Inter)
                }
            }
            Spacer(Modifier.weight(1f))
            Text(
                "Review mail",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 14.sp,
                fontFamily = Inter,
                modifier = Modifier.clickable { onReview() }.padding(8.dp),
            )
            Text(
                "Settings",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 14.sp,
                fontFamily = Inter,
                modifier = Modifier.clickable { onSettings() }.padding(8.dp),
            )
            Text("v0.1.0", color = Color.White.copy(alpha = 0.3f), fontSize = 10.sp, fontFamily = FragmentMono)
        }
    }
}
