package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import com.example.R
import com.example.data.Player
import com.example.ui.theme.*
import com.example.ui.viewmodel.PadelViewModel
import java.util.Calendar
import java.util.concurrent.TimeUnit

enum class TabScreen(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    DASHBOARD("Missa", Icons.Default.Home),
    LISTA("Inscrições", Icons.Default.List),
    FAME("Hall of Fame", Icons.Default.Star),
    REGRAS("Regras", Icons.Default.Info),
    PAGAMENTOS("Pagamento", Icons.Default.Check)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PadelTeamScreen(
    viewModel: PadelViewModel,
    modifier: Modifier = Modifier
) {
    val players by viewModel.allPlayers.collectAsStateWithLifecycle()
    val winners by viewModel.allWinners.collectAsStateWithLifecycle()
    val isBypassed by viewModel.isScheduleBypassed.collectAsStateWithLifecycle()
    val isOpen = viewModel.areInscriptionsOpen() || isBypassed

    var currentTab by remember { mutableStateOf(TabScreen.DASHBOARD) }
    val context = LocalContext.current

    // Dialog state for cancellation rule details
    var playerToDelete by remember { mutableStateOf<Player?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // Dialog state for admin group controls
    var showAdminMenu by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                containerColor = DarkSurface,
                tonalElevation = 8.dp,
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                TabScreen.values().forEach { tab ->
                    val isSelected = currentTab == tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentTab = tab },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.label,
                                tint = if (isSelected) PadelLime else SoftGray
                            )
                        },
                        label = {
                            Text(
                                text = tab.label,
                                style = Typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) PadelLime else SoftGray
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = DarkCardSurface
                        ),
                        modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground)
                .padding(innerPadding)
        ) {
            // Elegant Sport Header
            HeaderView(
                onAdminClick = { showAdminMenu = true }
            )

            // Dynamic view based on tab
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                AnimatedContent(
                    targetState = currentTab,
                    transitionSpec = {
                        fadeIn(animationSpec = spring()) togetherWith fadeOut(animationSpec = spring())
                    },
                    label = "TabTransition"
                ) { tab ->
                    when (tab) {
                        TabScreen.DASHBOARD -> DashboardTab(
                            players = players,
                            viewModel = viewModel,
                            isOpen = isOpen,
                            onGoToRegister = { currentTab = TabScreen.LISTA },
                            onGoToRules = { currentTab = TabScreen.REGRAS }
                        )
                        TabScreen.LISTA -> InscriptionsTab(
                            players = players,
                            viewModel = viewModel,
                            isOpen = isOpen,
                            onRemovePlayerTriggered = { player ->
                                playerToDelete = player
                                showDeleteConfirm = true
                            }
                        )
                        TabScreen.FAME -> FameTab(
                            winners = winners,
                            viewModel = viewModel
                        )
                        TabScreen.REGRAS -> RulesTab()
                        TabScreen.PAGAMENTOS -> PaymentsTab()
                    }
                }
            }
        }
    }

    // Modal Confirmation Dialog with cancellation fines structures
    if (showDeleteConfirm && playerToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteConfirm = false
                playerToDelete = null
            },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Alerta de Cancelamento",
                        tint = AlertGold
                    )
                    Text(
                        text = "Cancelar Inscrição?",
                        style = Typography.titleLarge,
                        color = WarmWhite
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Tens a certeza que desejas cancelar a inscrição de ${playerToDelete?.name}?",
                        style = Typography.bodyLarge,
                        color = WarmWhite
                    )
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(CoralFine.copy(alpha = 0.15f))
                            .padding(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "⚠️ LEMBRETE DE MULTA:",
                                style = Typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = CoralFine
                            )
                            Text(
                                text = "📅 Até 24h antes dos jogos: multa de 4.000 Kz.",
                                style = Typography.bodyMedium,
                                color = WarmWhite
                            )
                            Text(
                                text = "⏱️ Após lançamento da lista / antes do jogo: multa de 14.500 Kz.",
                                style = Typography.bodyMedium,
                                color = WarmWhite
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        playerToDelete?.let {
                            viewModel.removePlayer(it.id)
                            Toast.makeText(context, "${it.name} removido da lista.", Toast.LENGTH_SHORT).show()
                        }
                        showDeleteConfirm = false
                        playerToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CoralFine),
                    modifier = Modifier.testTag("confirm_delete_button")
                ) {
                    Text("Confirmar Remoção", color = WarmWhite)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        playerToDelete = null
                    }
                ) {
                    Text("Manter Inscrito", color = SoftGray)
                }
            },
            containerColor = DarkSurface
        )
    }

    // Admin dialog / quick prepopulate for demo
    if (showAdminMenu) {
        AlertDialog(
            onDismissRequest = { showAdminMenu = false },
            title = {
                Text("Funções de Administração", style = Typography.titleLarge, color = PadelLime)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "Gerencia as regras de inscrições abertas temporariamente às Quintas 15:00 ou força a abertura para testar e povoar as listas.",
                        style = Typography.bodyMedium,
                        color = SoftGray
                    )
                    
                    // Toggle Scheduler constraint for testing
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkCardSurface)
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Forçar Abertura", style = Typography.titleLarge.copy(fontSize = 14.sp), color = WarmWhite)
                            Text("Bypass horário (Quintas 15:00)", style = Typography.bodyMedium, color = SoftGray)
                        }
                        Switch(
                            checked = isBypassed,
                            onCheckedChange = { viewModel.isScheduleBypassed.value = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = PadelLime, checkedTrackColor = PadelLime.copy(alpha = 0.5f))
                        )
                    }
                    
                    Button(
                        onClick = {
                            viewModel.loadSamplePlayers()
                            showAdminMenu = false
                            Toast.makeText(context, "Lista de exemplo carregada (35 jogadores)", Toast.LENGTH_LONG).show()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("load_sample_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricTeal, contentColor = DarkBackground)
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Simular 35 Jogadores", fontWeight = FontWeight.Bold)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.clearAll()
                                showAdminMenu = false
                                Toast.makeText(context, "Lista de inscritos limpa.", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("clear_players_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = CoralFine, contentColor = WarmWhite)
                        ) {
                            Text("Limpar Inscritos")
                        }

                        Button(
                            onClick = {
                                viewModel.clearWinners()
                                showAdminMenu = false
                                Toast.makeText(context, "Hall of Fame redefinido.", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = CoralFine.copy(alpha = 0.5f), contentColor = WarmWhite)
                        ) {
                            Text("Limpar Fama")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAdminMenu = false }) {
                    Text("Fechar", color = PadelLime)
                }
            },
            containerColor = DarkSurface
        )
    }
}

@Composable
fun HeaderView(onAdminClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                // Add a beautiful neon green bottom line visual highlight
                val strokeWidth = 1.dp.toPx()
                val y = size.height - strokeWidth / 2
                drawLine(
                    color = PadelLime.copy(alpha = 0.3f),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = strokeWidth
                )
            }
            .background(
                Brush.verticalGradient(
                    colors = listOf(DarkSurface, DarkBackground)
                )
            )
            .padding(horizontal = 16.dp, vertical = 20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AsyncImage(
                        model = R.drawable.img_logo,
                        contentDescription = "Tá Cuiar Padel Team Logo",
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Text(
                        text = "TÁ CUIAR PADEL TEAM",
                        style = Typography.titleLarge.copy(
                            letterSpacing = 1.5.sp,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black
                        ),
                        color = PadelLime
                    )
                }
                Text(
                    text = "Missa de Sábado 🎾",
                    style = Typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = WarmWhite
                )
            }
            
            IconButton(
                onClick = onAdminClick,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(DarkCardSurface)
                    .size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Admin Settings",
                    tint = PadelLime
                )
            }
        }
    }
}

@Composable
fun DashboardTab(
    players: List<Player>,
    viewModel: PadelViewModel,
    isOpen: Boolean,
    onGoToRegister: () -> Unit,
    onGoToRules: () -> Unit
) {
    // Keep internal clock reference state that triggers every minute to count down
    var countdownText by remember { mutableStateOf(getCountdownString()) }
    val nextSaturdayStr = remember { getNextSaturdayDate() }

    LaunchedEffect(Unit) {
        while (true) {
            countdownText = getCountdownString()
            kotlinx.coroutines.delay(60000)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome / Summary Banner
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "PRÓXIMO ENCONTRO",
                        style = Typography.labelMedium,
                        color = ElectricTeal
                    )
                    Text(
                        text = nextSaturdayStr,
                        style = Typography.displayLarge.copy(fontSize = 24.sp),
                        color = WarmWhite
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Início: 10:30 (Concentração 10:15)",
                            style = Typography.bodyLarge,
                            color = SoftGray
                        )
                    }
                }
            }
        }

        // Live Countdown Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkCardSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "CONTAGEM DECRESCENTE",
                        style = Typography.labelMedium,
                        color = PadelLime,
                        letterSpacing = 1.sp
                    )
                    
                    Text(
                        text = countdownText,
                        style = Typography.displayLarge.copy(
                            fontSize = 32.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        ),
                        color = WarmWhite,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    Text(
                        text = "Jogos disputados todos os Sábados",
                        style = Typography.bodyMedium,
                        color = SoftGray
                    )
                }
            }
        }

        // Scheduler rules banner card indicating open status
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isOpen) DarkSurface else DarkSurface.copy(alpha = 0.9f)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isOpen) PadelLime.copy(alpha = 0.15f) else CoralFine.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isOpen) Icons.Default.LockOpen else Icons.Default.Lock,
                            contentDescription = null,
                            tint = if (isOpen) PadelLime else CoralFine
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isOpen) "Inscrições Prontas! ✅" else "Inscrições Fechadas! 🔑",
                            style = Typography.titleLarge.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
                            color = if (isOpen) PadelLime else CoralFine
                        )
                        Text(
                            text = if (isOpen) "Podes registar o teu nome para a missa." else "Inscrições abrem às Quintas-feiras às 15:00.",
                            style = Typography.bodyMedium,
                            color = SoftGray
                        )
                    }
                }
            }
        }

        // Inscription stats card with M3 Lime progress meter
        item {
            val confirmedCount = players.take(32).size
            val reserveCount = if (players.size > 32) players.size - 32 else 0
            val vacantSlots = 32 - confirmedCount

            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "ESTADO DAS INSCRIÇÕES",
                        style = Typography.labelMedium,
                        color = ElectricTeal
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "$confirmedCount / 32",
                                style = Typography.displayLarge.copy(fontSize = 28.sp),
                                color = PadelLime
                            )
                            Text(
                                text = "Confirmados",
                                style = Typography.bodyMedium,
                                color = SoftGray
                            )
                        }

                        if (reserveCount > 0) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "+$reserveCount",
                                    style = Typography.displayLarge.copy(fontSize = 28.sp),
                                    color = AlertGold
                                )
                                Text(
                                    text = "Na Fila de Espera",
                                    style = Typography.bodyMedium,
                                    color = SoftGray
                                )
                            }
                        } else {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "$vacantSlots",
                                    style = Typography.displayLarge.copy(fontSize = 28.sp),
                                    color = SoftGray
                                )
                                Text(
                                    text = "Vagas Restantes",
                                    style = Typography.bodyMedium,
                                    color = SoftGray
                                )
                            }
                        }
                    }

                    // Progress bar matching maximum 32 slots limit
                    LinearProgressIndicator(
                        progress = { confirmedCount / 32f },
                        color = PadelLime,
                        trackColor = DarkCardSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    if (players.size >= 32) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(AlertGold.copy(alpha = 0.15f))
                                .padding(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = AlertGold,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "LOTAÇÃO COMPLETA: Novas inscrições entram automaticamente como Reservas.",
                                style = Typography.bodyMedium,
                                color = AlertGold,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (isOpen) {
                            Button(
                                onClick = onGoToRegister,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PadelLime,
                                    contentColor = DarkBackground
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("dash_register_btn")
                            ) {
                                Text("Inscrever-me para Sábado", fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Button(
                                onClick = onGoToRegister,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = DarkCardSurface,
                                    contentColor = SoftGray
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) {
                                Text("Lista Fechada (Ver Inscrições)", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Quick Rules Info teaser
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onGoToRules() }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "REGULAMENTO & MULTAS",
                            style = Typography.labelMedium,
                            color = CoralFine
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Atrasos e cancelamentos tardios estão sujeitos a penalizações. Lê as regras.",
                            style = Typography.bodyMedium,
                            color = SoftGray
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Ver Regras",
                        tint = PadelLime,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun InscriptionsTab(
    players: List<Player>,
    viewModel: PadelViewModel,
    isOpen: Boolean,
    onRemovePlayerTriggered: (Player) -> Unit
) {
    var playerName by remember { mutableStateOf("") }
    val context = LocalContext.current

    val isQueueFull = players.size >= 32

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Enrolling Register Form Card
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "NOVA INSCRIÇÃO",
                    style = Typography.labelMedium,
                    color = PadelLime
                )

                if (isOpen) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextField(
                            value = playerName,
                            onValueChange = { playerName = it },
                            placeholder = { Text("Nome Completo do Jogador", color = SoftGray) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("player_name_input"),
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedTextColor = WarmWhite,
                                unfocusedTextColor = WarmWhite,
                                focusedContainerColor = DarkCardSurface,
                                unfocusedContainerColor = DarkCardSurface,
                                focusedIndicatorColor = PadelLime,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )

                        Button(
                            onClick = {
                                viewModel.addPlayer(
                                    name = playerName,
                                    onSuccess = {
                                        Toast.makeText(context, "$playerName inscrito!", Toast.LENGTH_SHORT).show()
                                        playerName = ""
                                    },
                                    onError = { error ->
                                        Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                                    }
                                )
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isQueueFull) AlertGold else PadelLime,
                                contentColor = DarkBackground
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .height(56.dp)
                                .testTag("inscribe_submit_btn")
                        ) {
                            Text(
                                text = if (isQueueFull) "+ Reserva" else "Inscrever",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Dynamic alert text informing entry category
                    val enrollmentDestText = if (isQueueFull) {
                        "Nota: Lotação de 32 atingida. Esta inscrição entrará na lista de RESERVAS."
                    } else {
                        "Disponível: Inscrição entra na LISTA PRINCIPAL (Vaga #${players.size + 1}/32)."
                    }
                    
                    Text(
                        text = enrollmentDestText,
                        style = Typography.bodyMedium,
                        color = if (isQueueFull) AlertGold else ElectricTeal,
                        fontWeight = FontWeight.Medium
                    )
                } else {
                    // Closed card indicating opening Thursday schedule
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(CoralFine.copy(alpha = 0.12f))
                            .padding(14.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = CoralFine, modifier = Modifier.size(18.dp))
                                Text(
                                    text = "INSCRIÇÕES FECHADAS temporariamente",
                                    style = Typography.titleLarge.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold),
                                    color = CoralFine
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Para garantir a igualdade de oportunidades, as inscrições abrem automaticamente todas as quintas-feiras a partir das 15h00. Prepara as tuas raquetes!",
                                style = Typography.bodyMedium,
                                color = WarmWhite
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Próxima Abertura: ${viewModel.getNextOpeningDateTime()}",
                                style = Typography.labelMedium,
                                color = PadelLime,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Two Section LazyList for Confirmed and Reserves
        Text(
            text = "LISTA DE JOGADORES (Total: ${players.size})",
            style = Typography.labelMedium,
            color = SoftGray,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (players.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkSurface)
                    .padding(32.dp),
                    contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.List,
                        contentDescription = null,
                        tint = SoftGray,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "Lista Inicial Vazia",
                        style = Typography.titleLarge,
                        color = WarmWhite
                    )
                    Text(
                        text = "Não há inscrições no momento. Introduz o teu nome para abrir a lista de sábado ou abre o menu de opções para simular a lista completa!",
                        style = Typography.bodyMedium,
                        color = SoftGray,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val confirmedPlayers = players.take(32)
                val reservePlayers = if (players.size > 32) players.drop(32) else emptyList()

                // Confirmed Group Header
                item {
                    Text(
                        text = "LISTA PRINCIPAL (MÁX 32)",
                        style = Typography.labelMedium,
                        color = PadelLime,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                itemsIndexed(confirmedPlayers) { idx, player ->
                    PlayerRowItem(
                        numberDisplay = "${idx + 1}",
                        player = player,
                        isReserve = false,
                        onCancelClick = { onRemovePlayerTriggered(player) },
                        modifier = Modifier.testTag("confirmed_item_${idx}")
                    )
                }

                // Reserves Group Header
                if (reservePlayers.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "FILA DE SUPLENTES / RESERVAS",
                            style = Typography.labelMedium,
                            color = AlertGold,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }

                    itemsIndexed(reservePlayers) { idx, player ->
                        PlayerRowItem(
                            numberDisplay = "R${idx + 1}",
                            player = player,
                            isReserve = true,
                            onCancelClick = { onRemovePlayerTriggered(player) },
                            modifier = Modifier.testTag("reserve_item_${idx}")
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PlayerRowItem(
    numberDisplay: String,
    player: Player,
    isReserve: Boolean,
    onCancelClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Number Index Bubble
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isReserve) AlertGold.copy(alpha = 0.2f) else PadelLime.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = numberDisplay,
                        style = Typography.labelMedium,
                        color = if (isReserve) AlertGold else PadelLime,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = player.name,
                    style = Typography.bodyLarge,
                    color = WarmWhite,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Cancellation cross Button
            IconButton(
                onClick = onCancelClick,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(DarkCardSurface)
                    .size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cancelar inscrição",
                    modifier = Modifier.size(16.dp),
                    tint = CoralFine
                )
            }
        }
    }
}

@Composable
fun RulesTab() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "REGRAS JORNADA - MISSA DE SÁBADO",
                style = Typography.headlineMedium.copy(fontSize = 20.sp),
                color = PadelLime
            )
        }

        // Rule Item: Horário
        item {
            RuleSectionCard(
                title = "1. HORÁRIO E CONCENTRAÇÃO",
                color = ElectricTeal,
                content = buildAnnotatedString {
                    append("O horário oficial dos jogos é a partir das ")
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = PadelLime)) {
                        append("10:30")
                    }
                    append(".\n\nA concentração dos atletas e verificação de presenças deve ser feita obrigatoriamente até às ")
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = PadelLime)) {
                        append("10:15")
                    }
                    append(".")
                }
            )
        }

        // Rule Item: Atrasos
        item {
            RuleSectionCard(
                title = "2. ATRASOS DE GRUPO",
                color = AlertGold,
                content = buildAnnotatedString {
                    append("Atrasos no dia do jogo deverão ser comunicados exclusivamente via ")
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = WarmWhite)) {
                        append("TELEFONE")
                    }
                    append(".\n\nNota importante: a partir das ")
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = AlertGold)) {
                        append("10:15")
                    }
                    append(" já não estaremos atentos ao feed de conversa ou mensagens no Grupo de conversação.")
                }
            )
        }

        // Rule Item: Cancelamentos com multas reais
        item {
            RuleSectionCard(
                title = "3. POLÍTICA DE CANCELAMENTO & MULTAS",
                color = CoralFine,
                content = buildAnnotatedString {
                    append("O cancelamento da tua inscrição está estritamente sujeito a multas financeiras:\n\n")
                    
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = WarmWhite)) {
                        append("❌ Cancelamento até 24 Horas antes ")
                    }
                    append("do horário dos jogos:\n")
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = CoralFine)) {
                        append("Multa de 4.000 Kz.\n\n")
                    }

                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = WarmWhite)) {
                        append("❌ Cancelamento após o lançamento da lista ")
                    }
                    append("e até antes do início dos jogos:\n")
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = CoralFine)) {
                        append("Multa de 14.500 Kz.")
                    }
                }
            )
        }

        // Rule Item: Pós jogos
        item {
            RuleSectionCard(
                title = "4. JOGOS \"PÓS\" TÁ CUIAR",
                color = ElectricTeal,
                content = buildAnnotatedString {
                    append("Após a conclusão da ronda dos 5 jogos de aferição dos super finalistas:\n\n")
                    
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = PadelLime)) {
                        append("Campo 4: ")
                    }
                    append("Estará reservado aos que fizeram os jogos normais e queiram fazer mais uns jogos. Os jogos devem ser sempre à melhor de 3 para que todos possam jogar.\n\n")

                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = PadelLime)) {
                        append("Campo 5: ")
                    }
                    append("Para aqueles que vão jogar somente às 12:30, apenas poderão jogar após a finalíssima. No caso de se terem comprado bolas, terão de pagar para o fundo de reserva o montante equivalente.\n\n")

                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = PadelLime)) {
                        append("Campo 6: ")
                    }
                    append("O campo 6 fica permanentemente reservado às meninas que queiram jogar após as partidas oficiais.")
                }
            )
        }
    }
}

@Composable
fun RuleSectionCard(
    title: String,
    color: Color,
    content: AnnotatedString
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = Typography.titleLarge.copy(fontSize = 14.sp),
                color = color,
                fontWeight = FontWeight.Black
            )
            HorizontalDivider(color = DarkCardSurface, thickness = 1.dp)
            Text(
                text = content,
                style = Typography.bodyLarge,
                color = WarmWhite
            )
        }
    }
}

@Composable
fun PaymentsTab() {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    val ibanStr = "AO06 0060 0148 0100 3690 7428 8"
    val cleanedIban = ibanStr.replace(" ", "")

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "DADOS DE PAGAMENTO",
                style = Typography.headlineMedium.copy(fontSize = 20.sp),
                color = PadelLime
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Utiliza as informações abaixo para regularizar as tuas quotas, inscrições e multas regulamentares para o fundo oficial do Tá Cuiar Padel Team.",
                style = Typography.bodyLarge,
                color = SoftGray
            )
        }

        // Standard Bank Official Details Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(ElectricTeal)
                        )
                        Text(
                            text = "OFFICIAL BANCO EMISSOR",
                            style = Typography.labelMedium,
                            color = ElectricTeal
                        )
                    }

                    Text(
                        text = "STANDARD BANK",
                        style = Typography.headlineMedium.copy(fontSize = 24.sp, fontWeight = FontWeight.Black),
                        color = WarmWhite
                    )

                    HorizontalDivider(color = DarkCardSurface, thickness = 1.dp)

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "TITULAR DA CONTA:",
                            style = Typography.labelMedium,
                            color = SoftGray
                        )
                        Text(
                            text = "Sérgio Vânio Guerreiro Miranda",
                            style = Typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = WarmWhite
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "IBAN DE TRANSFERÊNCIA:",
                            style = Typography.labelMedium,
                            color = SoftGray
                        )
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(DarkCardSurface)
                                .padding(12.dp)
                        ) {
                            Text(
                                text = ibanStr,
                                style = Typography.labelMedium.copy(fontSize = 14.sp),
                                color = PadelLime,
                                modifier = Modifier.align(Alignment.CenterStart)
                            )
                        }
                    }

                    Button(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(cleanedIban))
                            Toast.makeText(context, "IBAN Copiado!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PadelLime,
                            contentColor = DarkBackground
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("copy_iban_btn")
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Copiar IBAN Completo", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Express checkout alert fine guidelines
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(ElectricTeal.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = ElectricTeal
                            )
                        }

                        Column {
                            Text(
                                text = "MULTICAIXA EXPRESS",
                                style = Typography.titleLarge.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
                                color = WarmWhite
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Telemóvel: 923 423 061",
                                style = Typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                color = PadelLime
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Aceita transferências diretas via Express.",
                                style = Typography.bodyMedium,
                                color = SoftGray
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString("923423061"))
                            Toast.makeText(context, "Telemóvel Copiado!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkCardSurface)
                            .size(36.dp)
                            .testTag("copy_phone_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copiar Telemóvel",
                            tint = WarmWhite,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

// --- Hall of Fame Winner Celebration Section ---

@Composable
fun FameTab(
    winners: List<com.example.data.Winner>,
    viewModel: PadelViewModel
) {
    var title by remember { mutableStateOf("") }
    var p1 by remember { mutableStateOf("") }
    var p2 by remember { mutableStateOf("") }
    var score by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<String?>(null) }
    var isFormExpanded by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    
    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                // Done on non-documents or sandboxed
            }
            selectedImageUri = uri.toString()
            Toast.makeText(context, "Foto selecionada com sucesso!", Toast.LENGTH_SHORT).show()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "HALL OF FAME 🏆",
                        style = Typography.headlineMedium.copy(fontSize = 20.sp),
                        color = PadelLime
                    )
                    Text(
                        text = "Celebração das lendas e vencedores da Missa.",
                        style = Typography.bodyMedium,
                        color = SoftGray
                    )
                }

                Button(
                    onClick = { isFormExpanded = !isFormExpanded },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isFormExpanded) CoralFine else ElectricTeal,
                        contentColor = DarkBackground
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("toggle_fame_form_btn")
                ) {
                    Text(if (isFormExpanded) "Fechar" else "Subir Campeão", fontWeight = FontWeight.Bold)
                }
            }
        }

        if (isFormExpanded) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "REGISTAR NOVOS VENCEDORES",
                            style = Typography.labelMedium,
                            color = PadelLime
                        )

                        TextField(
                            value = title,
                            onValueChange = { title = it },
                            placeholder = { Text("Nome da Final (ex: Missa A - Campo 4)", color = SoftGray) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("winner_title_input"),
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedTextColor = WarmWhite,
                                unfocusedTextColor = WarmWhite,
                                focusedContainerColor = DarkCardSurface,
                                unfocusedContainerColor = DarkCardSurface,
                                focusedIndicatorColor = PadelLime,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TextField(
                                value = p1,
                                onValueChange = { p1 = it },
                                placeholder = { Text("Jogador 1", color = SoftGray) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("winner_p1_input"),
                                singleLine = true,
                                colors = TextFieldDefaults.colors(
                                    focusedTextColor = WarmWhite,
                                    unfocusedTextColor = WarmWhite,
                                    focusedContainerColor = DarkCardSurface,
                                    unfocusedContainerColor = DarkCardSurface,
                                    focusedIndicatorColor = PadelLime,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                shape = RoundedCornerShape(8.dp)
                            )

                            TextField(
                                value = p2,
                                onValueChange = { p2 = it },
                                placeholder = { Text("Jogador 2", color = SoftGray) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("winner_p2_input"),
                                singleLine = true,
                                colors = TextFieldDefaults.colors(
                                    focusedTextColor = WarmWhite,
                                    unfocusedTextColor = WarmWhite,
                                    focusedContainerColor = DarkCardSurface,
                                    unfocusedContainerColor = DarkCardSurface,
                                    focusedIndicatorColor = PadelLime,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextField(
                                value = score,
                                onValueChange = { score = it },
                                placeholder = { Text("Resultado (ex: 6-4)", color = SoftGray) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("winner_score_input"),
                                singleLine = true,
                                colors = TextFieldDefaults.colors(
                                    focusedTextColor = WarmWhite,
                                    unfocusedTextColor = WarmWhite,
                                    focusedContainerColor = DarkCardSurface,
                                    unfocusedContainerColor = DarkCardSurface,
                                    focusedIndicatorColor = PadelLime,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                shape = RoundedCornerShape(8.dp)
                            )

                            Button(
                                onClick = { pickerLauncher.launch("image/*") },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ElectricTeal,
                                    contentColor = DarkBackground
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp)
                            ) {
                                Icon(Icons.Default.AddAPhoto, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (selectedImageUri != null) "Com Foto" else "Foto",
                                    maxLines = 1,
                                    style = Typography.bodyMedium.copy(fontSize = 12.sp)
                                )
                            }
                        }

                        Button(
                            onClick = {
                                if (p1.trim().isEmpty() || p2.trim().isEmpty()) {
                                    Toast.makeText(context, "Por favor introduz o nome de ambos os jogadores!", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                val displayTitle = if (title.trim().isEmpty()) "Super Campeões" else title.trim()
                                val displayScore = if (score.trim().isEmpty()) "Vencedores" else score.trim()
                                
                                viewModel.addWinner(
                                    title = displayTitle,
                                    date = "Lançado no Sábado",
                                    p1 = p1.trim(),
                                    p2 = p2.trim(),
                                    score = displayScore,
                                    imageUri = selectedImageUri
                                )
                                
                                Toast.makeText(context, "Campeões gravados com sucesso!", Toast.LENGTH_SHORT).show()
                                p1 = ""
                                p2 = ""
                                score = ""
                                title = ""
                                selectedImageUri = null
                                isFormExpanded = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PadelLime, contentColor = DarkBackground),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("save_winner_btn")
                        ) {
                            Text("Adicionar Lendas", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        if (winners.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(DarkSurface)
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Sem registros no Hall of Fame.",
                        color = SoftGray,
                        style = Typography.bodyLarge
                    )
                }
            }
        } else {
            items(winners.size) { index ->
                val winner = winners[index]
                WinnerCardItem(winner = winner, onDelete = { viewModel.removeWinner(winner.id) })
            }
        }
    }
}

@Composable
fun WinnerCardItem(
    winner: com.example.data.Winner,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            // Photo Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(ElectricTeal.copy(alpha = 0.4f), DarkCardSurface)
                        )
                    )
            ) {
                if (winner.imageUri != null) {
                    AsyncImage(
                        model = winner.imageUri,
                        contentDescription = "Foto dos Vencedores",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = PadelLime,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "TÁ CUIAR PADEL TEAM",
                                style = Typography.labelMedium,
                                color = ElectricTeal,
                                letterSpacing = 2.sp
                            )
                        }
                    }
                }

                // Delete Cross
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkBackground.copy(alpha = 0.7f))
                        .size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Remover do Hall of Fame",
                        tint = CoralFine,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Title Tag Overlay at bottom left
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(DarkBackground.copy(alpha = 0.85f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = winner.score,
                        style = Typography.labelMedium,
                        color = PadelLime,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            // Info Section
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = winner.title,
                        style = Typography.titleLarge,
                        color = WarmWhite,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = winner.date,
                        style = Typography.bodyMedium,
                        color = SoftGray
                    )
                }

                HorizontalDivider(color = DarkCardSurface, thickness = 1.dp)

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = PadelLime,
                        modifier = Modifier.size(20.dp)
                    )
                    
                    Text(
                        text = "${winner.player1}  &  ${winner.player2}",
                        style = Typography.bodyLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            letterSpacing = 0.5.sp
                        ),
                        color = WarmWhite
                    )
                }
            }
        }
    }
}

// Private Date and countdown calculations matching server-provided time anchoring

fun getNextSaturdayDate(): String {
    val cal = Calendar.getInstance()
    // Align with today's metadata date: 2026-05-21 (Thursday)
    val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
    
    // Find upcoming Saturday
    val daysToSaturday = (Calendar.SATURDAY - dayOfWeek + 7) % 7
    val correctedDays = if (daysToSaturday == 0 && (cal.get(Calendar.HOUR_OF_DAY) > 10 || (cal.get(Calendar.HOUR_OF_DAY) == 10 && cal.get(Calendar.MINUTE) >= 30))) {
        7
    } else {
        daysToSaturday
    }
    
    cal.add(Calendar.DAY_OF_MONTH, correctedDays)
    
    val weekDays = arrayOf("", "Domingo", "Segunda-feira", "Terça-feira", "Quarta-feira", "Quinta-feira", "Sexta-feira", "Sábado")
    val monthNames = arrayOf("Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho", "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro")
    
    return "Sábado, ${cal.get(Calendar.DAY_OF_MONTH)} de ${monthNames[cal.get(Calendar.MONTH)]}"
}

fun getCountdownString(): String {
    val current = Calendar.getInstance()
    val target = Calendar.getInstance()
    val daysToSaturday = (Calendar.SATURDAY - current.get(Calendar.DAY_OF_WEEK) + 7) % 7
    val correctedDays = if (daysToSaturday == 0 && (current.get(Calendar.HOUR_OF_DAY) > 10 || (current.get(Calendar.HOUR_OF_DAY) == 10 && current.get(Calendar.MINUTE) >= 30))) {
        7
    } else {
        daysToSaturday
    }
    
    target.add(Calendar.DAY_OF_MONTH, correctedDays)
    target.set(Calendar.HOUR_OF_DAY, 10)
    target.set(Calendar.MINUTE, 30)
    target.set(Calendar.SECOND, 0)
    target.set(Calendar.MILLISECOND, 0)

    val diffMs = target.timeInMillis - current.timeInMillis
    if (diffMs <= 0) return "MISSA EM ANDAMENTO! 🎾"

    val days = TimeUnit.MILLISECONDS.toDays(diffMs)
    val hours = TimeUnit.MILLISECONDS.toHours(diffMs) % 24
    val minutes = TimeUnit.MILLISECONDS.toMinutes(diffMs) % 60
    
    return when {
        days > 0 -> "${days}d ${hours}h ${minutes}m"
        else -> "${hours}h ${minutes}m"
    }
}
