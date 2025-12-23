package com.ggz.smsbridge

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Patterns
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Http
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import co.yml.charts.axis.AxisData
import co.yml.charts.common.model.Point
import co.yml.charts.ui.linechart.model.GridLines
import co.yml.charts.ui.linechart.model.IntersectionPoint
import co.yml.charts.ui.linechart.model.Line
import co.yml.charts.ui.linechart.model.LineChartData
import co.yml.charts.ui.linechart.model.LinePlotData
import co.yml.charts.ui.linechart.model.LineStyle
import co.yml.charts.ui.linechart.model.SelectionHighlightPoint
import co.yml.charts.ui.linechart.model.SelectionHighlightPopUp
import co.yml.charts.ui.linechart.model.ShadowUnderLine
import com.ggz.smsbridge.ui.theme.SmsBridgeTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Toast.makeText(this, "短信权限已授予", Toast.LENGTH_SHORT).show()
            setReceiverState(this, true)
        } else {
            Toast.makeText(this, "短信权限被拒绝", Toast.LENGTH_SHORT).show()
            setReceiverState(this, false)
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        init()

        setContent {
            SmsBridgeTheme {
                val navController = rememberNavController()
                val currentBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = currentBackStackEntry?.destination?.route ?: "home"

                Scaffold(
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = { Text(text = "飞鸿") },
                            navigationIcon = {
                                if (currentRoute != "home") {
                                    IconButton(onClick = { navController.navigateUp() }) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                                    }
                                }
                            }
                        )
                    },
                    bottomBar = {
                        NavigationBar {
                            val items = listOf(
                                "home" to "主页",
                                "logs" to "日志",
                                "settings" to "设置"
                            )
                            items.forEach { (route, label) ->
                                NavigationBarItem(
                                    icon = {
                                        when (route) {
                                            "home" -> Icon(Icons.Default.Home, contentDescription = label)
                                            "logs" -> Icon(Icons.AutoMirrored.Filled.Notes, contentDescription = label)
                                            "settings" -> Icon(Icons.Default.Settings, contentDescription = label)
                                        }
                                    },
                                    label = { Text(label) },
                                    selected = currentRoute == route,
                                    onClick = { navController.navigate(route) }
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "home",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("home") { HomeScreen(navController, LocalContext.current) }
                        composable("logs") { LogScreen(navController) }
                        composable("settings") { SettingsScreen(navController) }
                    }
                }
            }
        }
    }

    private fun init() {
        checkSmsPermission()
        MonitorRepository.init(this)
    }

    private fun checkSmsPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED) {
            setReceiverState(this, true)
        } else {
            requestPermissionLauncher.launch( Manifest.permission.RECEIVE_SMS)
        }
    }
}

@Composable
fun HomeScreen(navController: NavHostController, context: Context) {
    val viewModel: SettingsViewModel = hiltViewModel()
    val appSettings by viewModel.appSettings.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Button(onClick = {
            val newListeningState = !appSettings.isListening
            setReceiverState(context, newListeningState)
            coroutineScope.launch {
                viewModel.saveSettings(appSettings.copy(isListening = newListeningState))
            }
        }) {
            Text(if (appSettings.isListening) "停止监控" else "开始监控")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(if (appSettings.isListening) "状态: 正在监控" else "状态: 已停止")
    }
}

@Composable
fun ExpandableCard(title: String, icon: @Composable () -> Unit, content: @Composable () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val rotationAngle by animateFloatAsState(targetValue = if (expanded) 180f else 0f)

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .clickable { expanded = !expanded }
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    icon()
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = title, style = MaterialTheme.typography.titleLarge)
                }
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = "展开/折叠",
                    modifier = Modifier.rotate(rotationAngle)
                )
            }
        }
        AnimatedVisibility(visible = expanded) {
            Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                content()
            }
        }
    }
}

@Composable
fun TestRuleDialog(regex: String, onDismiss: () -> Unit) {
    var textToTest by remember { mutableStateOf("") }
    var matchResult by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("测试匹配规则") },
        text = {
            Column {
                TextField(
                    value = textToTest,
                    onValueChange = { textToTest = it },
                    label = { Text("在此处粘贴短信原文") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = {
                    val result = regex.toRegex().find(textToTest)
                    matchResult = result?.groupValues?.getOrNull(1) ?: "未匹配到"
                }) {
                    Text("测试")
                }
                Spacer(modifier = Modifier.height(16.dp))
                if (matchResult != null) {
                    Text("匹配结果: $matchResult")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}


@Composable
fun SettingsScreen(navController: NavHostController) {
    val viewModel: SettingsViewModel = hiltViewModel()
    val appSettings by viewModel.appSettings.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    var showTestRuleDialog by remember { mutableStateOf(false) }

    var smsNumber by remember(appSettings.smsNumber) { mutableStateOf(appSettings.smsNumber) }
    var smsRegex by remember(appSettings.smsRegex) { mutableStateOf(appSettings.smsRegex) }
    var uploadMethod by remember(appSettings.uploadMethod) { mutableStateOf(appSettings.uploadMethod) }
    var recipientEmail by remember(appSettings.recipientEmail) { mutableStateOf(appSettings.recipientEmail) }
    var emailUser by remember(appSettings.emailUser) { mutableStateOf(appSettings.emailUser) }
    var emailPass by remember(appSettings.emailPass) { mutableStateOf(appSettings.emailPass) }
    var emailServer by remember(appSettings.emailServer) { mutableStateOf(appSettings.emailServer) }
    var emailPort by remember(appSettings.emailPort) { mutableStateOf(appSettings.emailPort) }
    var emailSsl by remember(appSettings.emailSsl) { mutableStateOf(appSettings.emailSsl) }
    var apiUrl by remember(appSettings.apiUrl) { mutableStateOf(appSettings.apiUrl) }
    var publicKey by remember(appSettings.publicKey) { mutableStateOf(appSettings.publicKey) }
    var enableEncryption by remember(appSettings.enableEncryption) { mutableStateOf(appSettings.enableEncryption) }
    var notifyOnNewCode by remember(appSettings.notifyOnNewCode) { mutableStateOf(appSettings.notifyOnNewCode) }

    if (showTestRuleDialog) {
        TestRuleDialog(regex = smsRegex, onDismiss = { showTestRuleDialog = false })
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "monitor_management") {
            ExpandableCard("监控管理", icon = { Icon(Icons.Default.Policy, contentDescription = null) }) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextField(
                        value = smsNumber,
                        onValueChange = { smsNumber = it },
                        label = { Text("短信发送号码") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextField(
                            value = smsRegex,
                            onValueChange = { smsRegex = it },
                            label = { Text("短信匹配规则") },
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = { showTestRuleDialog = true }) {
                            Text("测试")
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("通知方法: ")
                        RadioButton(selected = uploadMethod == "email", onClick = { uploadMethod = "email" })
                        Text("Email")
                        Spacer(Modifier.width(16.dp))
                        RadioButton(selected = uploadMethod == "http", onClick = { uploadMethod = "http" })
                        Text("HTTP")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(checked = enableEncryption, onCheckedChange = { enableEncryption = it })
                        Text("是否加密")
                    }
                    TextField(value = publicKey, onValueChange = { publicKey = it }, label = { Text("公钥") }, modifier = Modifier.fillMaxWidth())
                }
            }
        }

        item(key = "notification_channel") {
            ExpandableCard("通知渠道", icon = { Icon(Icons.Default.Email, contentDescription = null) }) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("邮箱设置", style = MaterialTheme.typography.titleMedium)
                    TextField(value = recipientEmail, onValueChange = { recipientEmail = it }, label = { Text("收件人邮箱") }, modifier = Modifier.fillMaxWidth())
                    TextField(value = emailUser, onValueChange = { emailUser = it }, label = { Text("用户名") }, modifier = Modifier.fillMaxWidth())
                    TextField(value = emailPass, onValueChange = { emailPass = it }, label = { Text("密码") }, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), modifier = Modifier.fillMaxWidth())

                    Text("SMTP 服务器", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
                    TextField(value = emailServer, onValueChange = { emailServer = it }, label = { Text("服务器") }, modifier = Modifier.fillMaxWidth())
                    TextField(
                        value = emailPort,
                        onValueChange = { emailPort = it },
                        label = { Text("端口") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = emailPort.isNotEmpty() && emailPort.toIntOrNull() == null,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(checked = emailSsl, onCheckedChange = { emailSsl = it })
                        Text("SSL")
                    }
                    Button(onClick = {
                        coroutineScope.launch {
                            val currentTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                            try {
                                withContext(Dispatchers.IO) {
                                    UploadHelper.sendEmail(emailUser, emailPass, recipientEmail, emailServer, emailPort, emailSsl, "Test Email", "This is a test email sent at $currentTime")
                                }
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "测试邮件已发送", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "发送失败: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    }) {
                        Text("发送测试邮件")
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Text("HTTP 设置", style = MaterialTheme.typography.titleMedium)
                    TextField(
                        value = apiUrl,
                        onValueChange = { apiUrl = it },
                        label = { Text("API URL") },
                        isError = apiUrl.isNotEmpty() && !Patterns.WEB_URL.matcher(apiUrl).matches(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        item(key = "system_settings") {
            ExpandableCard("系统设置", icon = { Icon(Icons.Default.Settings, contentDescription = null) }) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(checked = notifyOnNewCode, onCheckedChange = { notifyOnNewCode = it })
                        Text("接收到新验证码时通知")
                    }
                    Button(onClick = {
                        if (Build.MANUFACTURER.equals("Xiaomi", ignoreCase = true)) {
                            try {
                                val intent = Intent("miui.intent.action.APP_PERM_EDITOR").apply {
                                    setClassName("com.miui.securitycenter", "com.miui.permcenter.permissions.PermissionsEditorActivity")
                                    putExtra("extra_pkgname", context.packageName)
                                }
                                context.startActivity(intent)
                                Toast.makeText(context, "请找到“其他权限-通知类短信”并设为允许", Toast.LENGTH_LONG).show();
                            } catch (e: Exception) {
                                context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                            }
                        } else {
                            context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                        }
                    }) {
                        Text(if (Build.MANUFACTURER.equals("Xiaomi", ignoreCase = true)) "开启通知类短信权限" else "开启读取通知权限")
                    }
                    if (Build.MANUFACTURER.equals("Xiaomi", ignoreCase = true)) {
                        Button(onClick = {
                            try {
                                val intent = Intent().apply {
                                    component = ComponentName("com.miui.securitycenter", "com.miui.appmanager.ApplicationsDetailsActivity")
                                    putExtra("package_name", context.packageName)
                                    putExtra("package_label", context.applicationInfo.loadLabel(context.packageManager))
                                }
                                context.startActivity(intent)
                                Toast.makeText(context, "请找到“省电策略”并设为无限制", Toast.LENGTH_LONG).show();
                            } catch (e: Exception) {
                                context.startActivity(Intent(Settings.ACTION_SETTINGS))
                            }
                        }) {
                            Text("关闭省电策略")
                        }
                    }
                }
            }
        }
        item(key = "save_button") {
            Button(
                onClick = {
                    coroutineScope.launch {
                        viewModel.saveSettings(
                            appSettings.copy(
                                smsNumber = smsNumber,
                                smsRegex = smsRegex,
                                uploadMethod = uploadMethod,
                                recipientEmail = recipientEmail,
                                emailUser = emailUser,
                                emailPass = emailPass,
                                emailServer = emailServer,
                                emailPort = emailPort,
                                emailSsl = emailSsl,
                                apiUrl = apiUrl,
                                publicKey = publicKey,
                                enableEncryption = enableEncryption,
                                notifyOnNewCode = notifyOnNewCode
                            )
                        )
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "设置已保存", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) {
                Text("保存")
            }
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(text = title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 8.dp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogScreen(navController: NavHostController) {
    val allLogs by MonitorRepository.getTodayLogs().collectAsState(initial = emptyList())

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("转发日志", style = MaterialTheme.typography.titleLarge)
            Button(onClick = { MonitorRepository.clearAllLogs() }) {
                Text("清空全部日志")
            }
        }

        HorizontalDivider()

        if (allLogs.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Notes,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "暂无日志",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Text(
                    text = "开始监控后，日志会在这里显示",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        } else {
            val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) }
            LazyColumn(contentPadding = PaddingValues(16.dp)) {
                items(allLogs) { log ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            LogDetailRow("发件号码:", log.sender)
                            LogDetailRow("短信内容:", log.smsBody)
                            LogDetailRow("解析结果:", log.code, isBold = true)
                            LogDetailRow("接收时间:", dateFormat.format(Date(log.timestamp)))
                            LogDetailRow("下发方式:", log.uploadMethod)
                            LogDetailRow("下发状态:", log.uploadStatus, highlight = true)
                            if (log.failureReason != null) {
                                LogDetailRow("备注:", log.failureReason)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LogDetailRow(label: String, value: String, isBold: Boolean = false, highlight: Boolean = false) {
    Row(modifier = Modifier.padding(vertical = 2.dp)) {
        Text("$label ", fontWeight = FontWeight.Bold)
        Text(
            text = value,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            color = if (highlight) {
                if (value == "成功") Color(0xFF4CAF50) else Color.Red
            } else {
                LocalContentColor.current
            }
        )
    }
}

@Composable
fun getChartData(logs: List<VerificationCodeLog>, days: Int): LineChartData {
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.DAY_OF_YEAR, -days)
    val startDate = calendar.time

    val filteredLogs = logs.filter { it.timestamp >= startDate.time }

    val dataByDay = filteredLogs
        .groupBy { log ->
            val cal = Calendar.getInstance().apply { timeInMillis = log.timestamp }
            cal.get(Calendar.DAY_OF_YEAR)
        }
        .mapValues { it.value.size }

    val points = (0 until days).map { i ->
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -(days - 1 - i))
        val dayOfYear = cal.get(Calendar.DAY_OF_YEAR)
        val count = dataByDay[dayOfYear] ?: 0
        Point(i.toFloat(), count.toFloat())
    }

    val xAxisData = AxisData.Builder()
        .axisStepSize(100.dp)
        .backgroundColor(Color.Transparent)
        .steps(points.size - 1)
        .labelData { i -> 
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -(days - 1 - i))
            SimpleDateFormat("MM/dd", Locale.getDefault()).format(cal.time)
        }
        .labelAndAxisLinePadding(15.dp)
        .build()

    val yMax = (points.maxOfOrNull { it.y }?.toInt() ?: 0).coerceAtLeast(5)
    val yAxisData = AxisData.Builder()
        .steps(yMax)
        .backgroundColor(Color.Transparent)
        .labelAndAxisLinePadding(20.dp)
        .labelData { i ->
            val scale = yMax / 5
            (i * scale).toString()
        }.build()

    return LineChartData(
        linePlotData = LinePlotData(
            lines = listOf(
                Line(
                    dataPoints = points,
                    lineStyle = LineStyle(color = MaterialTheme.colorScheme.primary),
                    intersectionPoint = IntersectionPoint(color = MaterialTheme.colorScheme.primary),
                    selectionHighlightPoint = SelectionHighlightPoint(),
                    shadowUnderLine = ShadowUnderLine(alpha = 0.5f),
                    selectionHighlightPopUp = SelectionHighlightPopUp()
                )
            ),
        ),
        xAxisData = xAxisData,
        yAxisData = yAxisData,
        gridLines = GridLines(),
        backgroundColor = MaterialTheme.colorScheme.surface
    )
}

fun setReceiverState(context: Context, enabled: Boolean) {
    val componentName = ComponentName(context, SmsReceiver::class.java)
    val newState = if (enabled) {
        PackageManager.COMPONENT_ENABLED_STATE_ENABLED
    } else {
        PackageManager.COMPONENT_ENABLED_STATE_DISABLED
    }
    context.packageManager.setComponentEnabledSetting(componentName, newState, PackageManager.DONT_KILL_APP)
}
