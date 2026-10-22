package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.FistBackIllustration
import com.example.ui.components.MoveIllustration
import com.example.ui.components.PinchTapIllustration
import com.example.ui.components.ScrollIllustration
import com.example.ui.theme.DarkNavyBackground
import com.example.ui.theme.DarkNavyCard
import com.example.ui.theme.DarkNavyCardBorder
import com.example.ui.theme.DarkNavySurface
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonTeal
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PracticeScreen(
    onBackClick: () -> Unit
) {
    BackHandler { onBackClick() }

    val selectedTab = remember { mutableIntStateOf(0) }
    val tabTitles = listOf("1. Tap", "2. Scroll", "3. Swipe", "4. Back")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Gesture Practice Trainer",
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = NeonCyan
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkNavyBackground)
            )
        },
        containerColor = DarkNavyBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            TabRow(
                selectedTabIndex = selectedTab.intValue,
                containerColor = DarkNavySurface,
                contentColor = NeonCyan,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab.intValue]),
                        color = NeonCyan
                    )
                }
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab.intValue == index,
                        onClick = { selectedTab.intValue = index },
                        text = {
                            Text(
                                text = title,
                                fontSize = 13.sp,
                                fontWeight = if (selectedTab.intValue == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab.intValue == index) NeonCyan else TextSecondary
                            )
                        }
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                when (selectedTab.intValue) {
                    0 -> PracticeTapTask(onCompleted = { selectedTab.intValue = 1 })
                    1 -> PracticeScrollTask(onCompleted = { selectedTab.intValue = 2 })
                    2 -> PracticeSwipeTask(onCompleted = { selectedTab.intValue = 3 })
                    3 -> PracticeBackTask()
                }
            }
        }
    }
}

@Composable
private fun PracticeTapTask(onCompleted: () -> Unit) {
    val tappedTargets = remember { mutableStateListOf(false, false, false) }
    val allCompleted by remember { derivedStateOf { tappedTargets.all { it } } }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, DarkNavyCardBorder, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = DarkNavyCard)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PinchTapIllustration(size = 72.dp)
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Task 1: Click 3 Targets",
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Move your palm to place the ring over each glowing circle. Pinch thumb & index together and release quickly to click.",
                        fontSize = 12.5.sp,
                        color = TextSecondary,
                        lineHeight = 17.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            for (i in 0..2) {
                TargetBubble(
                    index = i + 1,
                    isTapped = tappedTargets[i],
                    onTap = { tappedTargets[i] = true }
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        AnimatedVisibility(visible = allCompleted) {
            SuccessBanner(
                title = "Excellent! You mastered Pinch-to-Click.",
                buttonText = "Next: Scroll Task →",
                onNext = onCompleted
            )
        }
    }
}

@Composable
private fun TargetBubble(
    index: Int,
    isTapped: Boolean,
    onTap: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "bubble")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val bgColor by animateColorAsState(
        targetValue = if (isTapped) NeonTeal else DarkNavySurface,
        label = "color"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isTapped) NeonTeal else NeonCyan,
        label = "border"
    )

    Box(
        modifier = Modifier
            .size(76.dp)
            .scale(if (!isTapped) pulse else 1f)
            .clip(CircleShape)
            .background(bgColor)
            .border(2.dp, borderColor, CircleShape)
            .clickable(enabled = !isTapped) { onTap() },
        contentAlignment = Alignment.Center
    ) {
        if (isTapped) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Completed",
                tint = Color.Black,
                modifier = Modifier.size(36.dp)
            )
        } else {
            Text(
                text = "$index",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = NeonCyan
            )
        }
    }
}

@Composable
private fun PracticeScrollTask(onCompleted: () -> Unit) {
    val listState = rememberLazyListState()
    val isScrolledToBottom by remember {
        derivedStateOf {
            val total = listState.layoutInfo.totalItemsCount
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            total > 0 && lastVisible >= total - 3
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, DarkNavyCardBorder, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = DarkNavyCard)
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ScrollIllustration(size = 72.dp)
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Task 2: Scroll to Bottom",
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Wave your open hand up (flick), OR raise two fingers (V-sign) and move your hand up/down for continuous smooth scroll.",
                        fontSize = 12.5.sp,
                        color = TextSecondary,
                        lineHeight = 17.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(DarkNavySurface)
                .border(1.dp, DarkNavyCardBorder, RoundedCornerShape(14.dp))
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items((1..40).toList()) { itemNum ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (itemNum == 40) NeonTeal.copy(alpha = 0.2f) else DarkNavyCard
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Feed Item #$itemNum",
                            color = if (itemNum == 40) NeonTeal else TextPrimary,
                            fontWeight = if (itemNum == 40) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        if (itemNum == 40) {
                            Text("🏁 FINISH LINE", color = NeonTeal, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        AnimatedVisibility(visible = isScrolledToBottom) {
            SuccessBanner(
                title = "Awesome! Scroll technique mastered.",
                buttonText = "Next: Swipe Task →",
                onNext = onCompleted
            )
        }
    }
}

@Composable
private fun PracticeSwipeTask(onCompleted: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { 4 })
    val isLastPage by remember { derivedStateOf { pagerState.currentPage == 3 } }

    Column(modifier = Modifier.fillMaxSize()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, DarkNavyCardBorder, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = DarkNavyCard)
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MoveIllustration(size = 72.dp)
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Task 3: Swipe Pages (Left / Right)",
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Wave your open palm left/right to flick pages, or pinch-and-hold (350ms) to drag horizontally like a real finger.",
                        fontSize = 12.5.sp,
                        color = TextSecondary,
                        lineHeight = 17.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(DarkNavySurface)
                .border(1.dp, DarkNavyCardBorder, RoundedCornerShape(18.dp))
        ) { page ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Page ${page + 1} of 4",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (page == 3) "🎉 Target reached! You navigated to the last page." else "Swipe left to proceed →",
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        AnimatedVisibility(visible = isLastPage) {
            SuccessBanner(
                title = "Great job! Horizontal page swipe mastered.",
                buttonText = "Next: Fist Back Task →",
                onNext = onCompleted
            )
        }
    }
}

@Composable
private fun PracticeBackTask() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, DarkNavyCardBorder, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = DarkNavyCard)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FistBackIllustration(size = 72.dp)
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Task 4: Fist to Go Back",
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Close your hand into a tight fist for half a second (0.45s - 1.4s) and open it. The green ring indicates Back is ready.",
                        fontSize = 12.5.sp,
                        color = TextSecondary,
                        lineHeight = 17.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(30.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, NeonTeal.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = DarkNavySurface)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Celebration,
                    contentDescription = null,
                    tint = NeonTeal,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "You are Ready for Real-World Control!",
                    fontWeight = FontWeight.Bold,
                    color = NeonTeal,
                    fontSize = 17.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Close a fist now to trigger the system Back action and return to the Home screen, or open other apps on your phone to use Haatix anywhere.",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
private fun SuccessBanner(
    title: String,
    buttonText: String,
    onNext: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, NeonTeal, RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkNavyCard)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = NeonTeal,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onNext,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeonTeal)
            ) {
                Text(
                    text = buttonText,
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
    }
}
