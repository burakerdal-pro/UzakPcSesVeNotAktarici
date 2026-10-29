package com.example.sat

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.sat.ui.theme.SatışTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SatışTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    NoteSenderScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

class AudioRecorderManager(private val context: Context) {
    private var mediaRecorder: MediaRecorder? = null
    var outputFile: File? = null
        private set

    fun startRecording(): File {
        val file = File(context.cacheDir, "temp_rec_${System.currentTimeMillis()}.m4a")
        outputFile = file

        mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(file.absolutePath)
            prepare()
            start()
        }
        return file
    }

    fun stopRecording() {
        try {
            mediaRecorder?.stop()
            mediaRecorder?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            mediaRecorder = null
        }
    }
}

@Composable
fun NoteSenderScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val audioRecorder = remember { AudioRecorderManager(context) }

    var ipAddress by remember { mutableStateOf("192.168.1.100") }
    var port by remember { mutableStateOf("5000") }

    // Note State
    var fileName by remember { mutableStateOf("notlar.txt") }
    var noteContent by remember { mutableStateOf("") }
    var appendMode by remember { mutableStateOf(true) }
    var openNotepad by remember { mutableStateOf(true) }

    // Audio State
    var isRecording by remember { mutableStateOf(false) }
    var recordDurationSeconds by remember { mutableStateOf(0) }
    var audioFileName by remember { mutableStateOf("ses_kaydi.m4a") }
    var recordedAudioFile by remember { mutableStateOf<File?>(null) }

    var isLoading by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isSuccess by remember { mutableStateOf(true) }

    // Permission launcher for RECORD_AUDIO
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            isRecording = true
            recordDurationSeconds = 0
            recordedAudioFile = audioRecorder.startRecording()
        } else {
            Toast.makeText(context, "Ses kaydı için mikrofona izin verilmeli.", Toast.LENGTH_SHORT).show()
        }
    }

    // Timer effect for audio recording
    LaunchedEffect(isRecording) {
        if (isRecording) {
            while (isRecording) {
                delay(1000L)
                recordDurationSeconds++
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "PC Not & Ses Aktarıcı",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "Yazılı notlarınızı ve ses kayıtlarınızı bilgisayarınıza anında aktarın.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
        }

        // Connection Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "1. Bilgisayar Bağlantı Bilgileri",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = ipAddress,
                        onValueChange = { ipAddress = it },
                        label = { Text("Bilgisayar IP Adresi") },
                        placeholder = { Text("örn: 192.168.1.50") },
                        modifier = Modifier.weight(2f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    OutlinedTextField(
                        value = port,
                        onValueChange = { port = it },
                        label = { Text("Port") },
                        placeholder = { Text("5000") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                OutlinedButton(
                    onClick = {
                        coroutineScope.launch {
                            isLoading = true
                            statusMessage = null
                            val result = testConnection(ipAddress, port)
                            isLoading = false
                            result.fold(
                                onSuccess = {
                                    isSuccess = true
                                    statusMessage = it
                                },
                                onFailure = {
                                    isSuccess = false
                                    statusMessage = it.message ?: "Bağlantı hatası"
                                }
                            )
                        }
                    },
                    modifier = Modifier.align(Alignment.End),
                    enabled = !isLoading && ipAddress.isNotBlank() && port.isNotBlank()
                ) {
                    Text("Bağlantıyı Test Et")
                }
            }
        }

        // Text Note Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "2. Yazılı Not Gönder",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                OutlinedTextField(
                    value = fileName,
                    onValueChange = { fileName = it },
                    label = { Text("Kaydedilecek Dosya Adı") },
                    placeholder = { Text("notlar.txt") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = noteContent,
                    onValueChange = { noteContent = it },
                    label = { Text("Notunuzu Buraya Yazın") },
                    placeholder = { Text("Bilgisayardaki not defterine yazılacak metin...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp),
                    maxLines = 8
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Ekleme Modu", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = if (appendMode) "Mevcut notların altına ekler" else "Dosya içeriğini tamamen değiştirir",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                    Switch(
                        checked = appendMode,
                        onCheckedChange = { appendMode = it }
                    )
                }

                HorizontalDivider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Not Defteri'nde (Notepad) Aç", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = "Bilgisayarda otomatik Not Defteri penceresini açar",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                    Switch(
                        checked = openNotepad,
                        onCheckedChange = { openNotepad = it }
                    )
                }

                Button(
                    onClick = {
                        coroutineScope.launch {
                            isLoading = true
                            statusMessage = null
                            val result = sendNoteToPc(
                                ip = ipAddress.trim(),
                                port = port.trim(),
                                filename = fileName.trim(),
                                content = noteContent,
                                append = appendMode,
                                openNotepad = openNotepad
                            )
                            isLoading = false
                            result.fold(
                                onSuccess = {
                                    isSuccess = true
                                    statusMessage = it
                                },
                                onFailure = {
                                    isSuccess = false
                                    statusMessage = it.message ?: "Gönderme hatası"
                                }
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    enabled = !isLoading && noteContent.isNotBlank() && ipAddress.isNotBlank()
                ) {
                    Text("Yazılı Notu Gönder", fontSize = 15.sp)
                }
            }
        }

        // Audio Record Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "3. Ses Kaydı Al ve Gönder",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                OutlinedTextField(
                    value = audioFileName,
                    onValueChange = { audioFileName = it },
                    label = { Text("Ses Dosyası Adı") },
                    placeholder = { Text("ses_kaydi.m4a") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Record Button & Timer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (isRecording) {
                        Button(
                            onClick = {
                                audioRecorder.stopRecording()
                                isRecording = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                        ) {
                            Text("⏹ Kaydı Durdur")
                        }

                        val mins = recordDurationSeconds / 60
                        val secs = recordDurationSeconds % 60
                        Text(
                            text = String.format("🔴 Kaydediliyor... %02d:%02d", mins, secs),
                            color = Color(0xFFD32F2F),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    } else {
                        Button(
                            onClick = {
                                val hasPermission = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.RECORD_AUDIO
                                ) == PackageManager.PERMISSION_GRANTED

                                if (hasPermission) {
                                    isRecording = true
                                    recordDurationSeconds = 0
                                    recordedAudioFile = audioRecorder.startRecording()
                                } else {
                                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                        ) {
                            Text("🎙 Ses Kaydı Başlat")
                        }

                        recordedAudioFile?.let {
                            if (it.exists() && it.length() > 0) {
                                Text(
                                    text = "✓ Kayıt Hazır (${it.length() / 1024} KB)",
                                    color = Color(0xFF2E7D32),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }

                // Send Audio Button
                Button(
                    onClick = {
                        val file = recordedAudioFile
                        if (file != null && file.exists()) {
                            coroutineScope.launch {
                                isLoading = true
                                statusMessage = null
                                val result = sendAudioToPc(
                                    ip = ipAddress.trim(),
                                    port = port.trim(),
                                    filename = audioFileName.trim(),
                                    audioFile = file
                                )
                                isLoading = false
                                result.fold(
                                    onSuccess = {
                                        isSuccess = true
                                        statusMessage = it
                                    },
                                    onFailure = {
                                        isSuccess = false
                                        statusMessage = it.message ?: "Ses gönderme hatası"
                                    }
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    enabled = !isLoading && !isRecording && recordedAudioFile != null && ipAddress.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onSecondary,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Ses Gönderiliyor...")
                    } else {
                        Text("Ses Kaydını Bilgisayara Gönder", fontSize = 15.sp)
                    }
                }
            }
        }

        // Status Message Banner
        statusMessage?.let { message ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSuccess) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isSuccess) "✓ " else "⚠ ",
                        fontWeight = FontWeight.Bold,
                        color = if (isSuccess) Color(0xFF2E7D32) else Color(0xFFC62828),
                        fontSize = 18.sp
                    )
                    Text(
                        text = message,
                        color = if (isSuccess) Color(0xFF1B5E20) else Color(0xFFB71C1C),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

suspend fun sendNoteToPc(
    ip: String,
    port: String,
    filename: String,
    content: String,
    append: Boolean,
    openNotepad: Boolean
): Result<String> = withContext(Dispatchers.IO) {
    try {
        val url = URL("http://$ip:$port/note")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
        conn.setRequestProperty("Accept", "application/json")
        conn.doOutput = true
        conn.connectTimeout = 5000
        conn.readTimeout = 5000

        val jsonInput = JSONObject().apply {
            put("filename", filename.ifBlank { "notlar.txt" })
            put("content", content)
            put("append", append)
            put("open_notepad", openNotepad)
        }.toString()

        OutputStreamWriter(conn.outputStream, StandardCharsets.UTF_8).use { writer ->
            writer.write(jsonInput)
            writer.flush()
        }

        val statusCode = conn.responseCode
        if (statusCode in 200..299) {
            val responseText = conn.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
            val responseJson = JSONObject(responseText)
            val msg = responseJson.optString("message", "Not bilgisayara gönderildi.")
            Result.success(msg)
        } else {
            val errorText = conn.errorStream?.bufferedReader(StandardCharsets.UTF_8)?.use { it.readText() } ?: ""
            Result.failure(Exception("Sunucu hatası ($statusCode): $errorText"))
        }
    } catch (e: Exception) {
        Result.failure(Exception("Bağlantı kurulamadı: ${e.localizedMessage}"))
    }
}

suspend fun sendAudioToPc(
    ip: String,
    port: String,
    filename: String,
    audioFile: File
): Result<String> = withContext(Dispatchers.IO) {
    try {
        if (!audioFile.exists() || audioFile.length() == 0L) {
            return@withContext Result.failure(Exception("Ses kaydı bulunamadı veya boş."))
        }

        val audioBytes = audioFile.readBytes()
        val base64Audio = android.util.Base64.encodeToString(audioBytes, android.util.Base64.NO_WRAP)

        val url = URL("http://$ip:$port/audio")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
        conn.setRequestProperty("Accept", "application/json")
        conn.doOutput = true
        conn.connectTimeout = 15000
        conn.readTimeout = 15000

        val jsonInput = JSONObject().apply {
            put("filename", filename.ifBlank { "ses_kaydi.m4a" })
            put("audio_data", base64Audio)
        }.toString()

        OutputStreamWriter(conn.outputStream, StandardCharsets.UTF_8).use { writer ->
            writer.write(jsonInput)
            writer.flush()
        }

        val statusCode = conn.responseCode
        if (statusCode in 200..299) {
            val responseText = conn.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
            val responseJson = JSONObject(responseText)
            val msg = responseJson.optString("message", "Ses kaydı bilgisayara gönderildi.")
            Result.success(msg)
        } else {
            val errorText = conn.errorStream?.bufferedReader(StandardCharsets.UTF_8)?.use { it.readText() } ?: ""
            Result.failure(Exception("Sunucu hatası ($statusCode): $errorText"))
        }
    } catch (e: Exception) {
        Result.failure(Exception("Ses gönderilemedi: ${e.localizedMessage}"))
    }
}

suspend fun testConnection(ip: String, port: String): Result<String> = withContext(Dispatchers.IO) {
    try {
        val url = URL("http://$ip:$port/ping")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.connectTimeout = 3000
        conn.readTimeout = 3000
        val code = conn.responseCode
        if (code in 200..299) {
            Result.success("PC Bağlantısı Başarılı!")
        } else {
            Result.failure(Exception("Sunucu yanıt verdi ama kod: $code"))
        }
    } catch (e: Exception) {
        Result.failure(Exception("PC'ye ulaşılamadı: ${e.localizedMessage}"))
    }
}
