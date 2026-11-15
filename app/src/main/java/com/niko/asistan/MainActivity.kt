package com.niko.asistan

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.AudioAttributes
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

// N · I · K · O Canlı Sarı Hologram Çekirdeği
class NikoVisualizerView(context: Context) : View(context) {
    var audioLevel = 0f
    var stateText = "NÖBETTE"
    var isSpeakingOrListening = false
    private var angle1 = 0f
    private var angle2 = 0f
    private var angle3 = 0f

    private val corePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ringPaint1 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 6f
        color = Color.parseColor("#F59E0B")
        pathEffect = DashPathEffect(floatArrayOf(70f, 35f, 25f, 35f), 0f)
    }
    private val ringPaint2 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        color = Color.parseColor("#FBBF24")
        pathEffect = DashPathEffect(floatArrayOf(110f, 50f), 0f)
    }
    private val ringPaint3 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        color = Color.parseColor("#99F59E0B")
        pathEffect = DashPathEffect(floatArrayOf(30f, 60f), 0f)
    }
    private val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        color = Color.parseColor("#80FBBF24")
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FBBF24")
        textSize = 26f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        letterSpacing = 0.25f
    }
    private val rect = RectF()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val maxR = (width.coerceAtMost(height) / 2f) * 0.85f

        val speed = if (isSpeakingOrListening) 2.8f else 0.7f
        angle1 = (angle1 + 0.9f * speed) % 360f
        angle2 = (angle2 - 1.3f * speed) % 360f
        angle3 = (angle3 + 0.5f * speed) % 360f

        val pulse = if (isSpeakingOrListening) (audioLevel * 15f).coerceIn(6f, 100f) else 8f

        val outerR = maxR * 0.95f
        for (i in 0 until 60) {
            val rad = Math.toRadians((i * 6).toDouble())
            val len = if (i % 5 == 0) 20f else 8f
            val x1 = cx + (outerR - len) * cos(rad).toFloat()
            val y1 = cy + (outerR - len) * sin(rad).toFloat()
            val x2 = cx + outerR * cos(rad).toFloat()
            val y2 = cy + outerR * sin(rad).toFloat()
            canvas.drawLine(x1, y1, x2, y2, tickPaint)
        }

        canvas.save()
        canvas.rotate(angle1, cx, cy)
        val r1 = maxR * 0.76f + pulse * 0.2f
        rect.set(cx - r1, cy - r1, cx + r1, cy + r1)
        canvas.drawArc(rect, 0f, 360f, false, ringPaint1)
        canvas.restore()

        canvas.save()
        canvas.rotate(angle2, cx, cy)
        val r2 = maxR * 0.61f + pulse * 0.35f
        rect.set(cx - r2, cy - r2, cx + r2, cy + r2)
        canvas.drawArc(rect, 0f, 360f, false, ringPaint2)
        canvas.restore()

        canvas.save()
        canvas.rotate(angle3, cx, cy)
        val r3 = maxR * 0.47f + pulse * 0.25f
        rect.set(cx - r3, cy - r3, cx + r3, cy + r3)
        canvas.drawArc(rect, 0f, 360f, false, ringPaint3)
        canvas.restore()

        val coreR = (maxR * 0.30f + pulse * 0.6f).coerceAtLeast(25f)
        corePaint.shader = RadialGradient(
            cx, cy, coreR * 1.4f,
            intArrayOf(
                Color.parseColor("#FFFFFF"),
                Color.parseColor("#FDE047"),
                Color.parseColor("#F59E0B"),
                Color.TRANSPARENT
            ),
            floatArrayOf(0f, 0.3f, 0.7f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy, coreR * 1.4f, corePaint)

        canvas.drawText(stateText, cx, cy + maxR * 0.68f, textPaint)
        postInvalidateOnAnimation()
    }
}

class MainActivity : AppCompatActivity() {
    companion object {
        var visualizer: NikoVisualizerView? = null
        var subtitleView: TextView? = null
        // API Anahtarı doğrudan kodun içine gömüldü[span_2](start_span)[span_2](end_span)[span_3](start_span)[span_3](end_span)
        const val EMBEDDED_KEY = "AQ.Ab8RN6KZYPqSekpNWFEnuG42klSAK_TKxGeFNc1YbcduzUm0kA"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 1)
        }

        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.parseColor("#050505"))
        }

        val mainCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(40, 70, 40, 50)
        }

        val topHeader = TextView(this).apply {
            text = "N  ·  I  ·  K  ·  O"
            textSize = 26f
            setTextColor(Color.parseColor("#F59E0B"))
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, 10, 0, 20)
        }

        val nikoView = NikoVisualizerView(this).apply {
            layoutParams = LinearLayout.LayoutParams(720, 720).apply {
                gravity = Gravity.CENTER
            }
            setOnClickListener {
                startNikoService("WAKE_NOW")
            }
        }
        visualizer = nikoView

        val subText = TextView(this).apply {
            text = "Arka planda aktif. \"Hey Niko\" de ve sağ alttan asistanı çağır."
            textSize = 15f
            setTextColor(Color.parseColor("#FEF08A"))
            gravity = Gravity.CENTER
            setPadding(40, 20, 40, 40)
        }
        subtitleView = subText

        val btnActivate = Button(this).apply {
            text = "NÖBETİ BAŞLAT & ÇIK"
            textSize = 14f
            setTextColor(Color.BLACK)
            setPadding(50, 40, 50, 40)
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 40f
                setColor(Color.parseColor("#FBBF24"))
            }
            setOnClickListener {
                if (!Settings.canDrawOverlays(this@MainActivity)) {
                    Toast.makeText(this@MainActivity, "Lütfen Ekran Üstü İznini Açın", Toast.LENGTH_LONG).show()
                    startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
                } else {
                    startNikoService("START_BG")
                    Toast.makeText(this@MainActivity, "Niko Aktif! Uygulamadan çıkabilirsin.", Toast.LENGTH_LONG).show()
                    finish()
                }
            }
        }

        mainCol.addView(topHeader)
        mainCol.addView(nikoView, LinearLayout.LayoutParams(720, 720))
        mainCol.addView(subText)
        mainCol.addView(btnActivate)

        root.addView(mainCol)
        setContentView(root)

        if (Settings.canDrawOverlays(this)) {
            startNikoService("START_BG")
        }
    }

    private fun startNikoService(action: String) {
        val svc = Intent(this, OverlayService::class.java).apply {
            putExtra("ACTION", action)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(svc)
        } else {
            startService(svc)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        visualizer = null
        subtitleView = null
    }
}

class OverlayService : Service(), TextToSpeech.OnInitListener {
    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: View
    private lateinit var pillText: TextView
    private lateinit var waveIndicator: TextView
    private var speechRecognizer: SpeechRecognizer? = null
    private lateinit var recognizerIntent: Intent
    private var tts: TextToSpeech? = null
    private val client = OkHttpClient()
    private val mainHandler = Handler(Looper.getMainLooper())
    private var activeModelName: String? = null

    private var isAwakeForCommand = false
    private var isSpeaking = false
    private var isProcessing = false
    private var isListeningNow = false

    private val hideRunnable = Runnable {
        if (!isSpeaking && !isProcessing) {
            isAwakeForCommand = false
            overlayView.visibility = View.GONE
            MainActivity.visualizer?.stateText = "NÖBETTE"
            MainActivity.visualizer?.isSpeakingOrListening = false
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startAsForeground()
        if (intent?.getStringExtra("ACTION") == "WAKE_NOW") {
            wakeUpAssistant("🎙️ Seni dinliyorum...")
        }
        return START_STICKY
    }

    private fun startAsForeground() {
        val channelId = "niko_voice_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Niko Sesli Asistan", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Niko Asistan Nöbette")
            .setContentText("'Hey Niko' dediğinde sağ alttan açılır")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        } else {
            startForeground(1, notification)
        }
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        tts = TextToSpeech(this, this)

        val geminiPill = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(45, 28, 45, 28)
            visibility = View.GONE
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 80f
                setColor(Color.parseColor("#D918181B"))
                setStroke(3, Color.parseColor("#FBBF24"))
            }
        }

        val starIcon = TextView(this).apply {
            text = "✦  "
            textSize = 18f
            setTextColor(Color.parseColor("#FBBF24"))
        }

        pillText = TextView(this).apply {
            text = "Niko dinliyor..."
            textSize = 15f
            setTextColor(Color.parseColor("#FEF08A"))
            setPadding(8, 0, 35, 0)
        }

        waveIndicator = TextView(this).apply {
            text = "ılı"
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#F59E0B"))
        }

        geminiPill.addView(starIcon)
        geminiPill.addView(pillText)
        geminiPill.addView(waveIndicator)

        overlayView = geminiPill

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.END
            x = 45
            y = 75
        }

        windowManager.addView(overlayView, params)

        setupPersistentRecognizer()
        mainHandler.postDelayed({ startListeningSafely() }, 600)
    }

    private fun wakeUpAssistant(message: String) {
        mainHandler.post {
            mainHandler.removeCallbacks(hideRunnable)
            isAwakeForCommand = true
            overlayView.visibility = View.VISIBLE
            pillText.text = message.take(44)
            MainActivity.visualizer?.stateText = "DİNLİYOR"
            MainActivity.visualizer?.isSpeakingOrListening = true
            MainActivity.subtitleView?.text = message
            if (!isListeningNow && !isSpeaking && !isProcessing) {
                startListeningSafely()
            }
            mainHandler.postDelayed(hideRunnable, 10000)
        }
    }

    private fun updateUI(state: String, subtitle: String, active: Boolean) {
        mainHandler.post {
            overlayView.visibility = View.VISIBLE
            pillText.text = subtitle.take(44)
            MainActivity.visualizer?.stateText = state
            MainActivity.visualizer?.isSpeakingOrListening = active
            MainActivity.subtitleView?.text = subtitle
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale("tr", "TR")
            try {
                val voices = tts?.voices
                if (voices != null) {
                    val bestVoice = voices.firstOrNull {
                        it.locale.language == "tr" && (it.name.contains("male", true) || it.name.contains("iom", true) || it.name.contains("network", true) || it.name.contains("wavenet", true))
                    }
                    if (bestVoice != null) tts?.voice = bestVoice
                }
            } catch (e: Exception) {}

            tts?.setPitch(0.84f)
            tts?.setSpeechRate(1.06f)

            val audioAttrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
            tts?.setAudioAttributes(audioAttrs)

            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    isSpeaking = true
                    MainActivity.visualizer?.isSpeakingOrListening = true
                    MainActivity.visualizer?.audioLevel = 4.0f
                }
                override fun onDone(utteranceId: String?) {
                    isSpeaking = false
                    isProcessing = false
                    mainHandler.postDelayed(hideRunnable, 3500)
                    mainHandler.postDelayed({ startListeningSafely() }, 300)
                }
                override fun onError(utteranceId: String?) {
                    isSpeaking = false
                    isProcessing = false
                    mainHandler.postDelayed(hideRunnable, 2000)
                    mainHandler.postDelayed({ startListeningSafely() }, 300)
                }
            })
        }
    }

    private fun speak(text: String) {
        try { speechRecognizer?.cancel() } catch (e: Exception) {}
        isListeningNow = false

        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.mode = AudioManager.MODE_NORMAL
        audioManager.isSpeakerphoneOn = true
        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVol, 0)

        val params = Bundle()
        params.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "NikoUtterance")
    }

    private fun setupPersistentRecognizer() {
        mainHandler.post {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(applicationContext)
            recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "tr-TR")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
            }

            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    isListeningNow = true
                }
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {
                    val level = if (rmsdB < 0) 0.5f else rmsdB.coerceIn(0.5f, 10f)
                    if (isAwakeForCommand) {
                        MainActivity.visualizer?.audioLevel = level
                        waveIndicator.text = if (level > 2.5f) "IlıI" else "ılı"
                    }
                }
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {
                    isListeningNow = false
                }
                override fun onError(error: Int) {
                    isListeningNow = false
                    if (!isSpeaking && !isProcessing) {
                        mainHandler.postDelayed({ startListeningSafely() }, 350)
                    }
                }
                override fun onResults(results: Bundle?) {
                    isListeningNow = false
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        handleSpokenText(matches[0])
                    } else {
                        startListeningSafely()
                    }
                }
                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        val partial = matches[0]
                        val lower = partial.lowercase(Locale("tr", "TR"))

                        if (!isAwakeForCommand && (lower.contains("niko") || lower.contains("nico"))) {
                            wakeUpAssistant("🎙️ $partial")
                        } else if (isAwakeForCommand) {
                            mainHandler.removeCallbacks(hideRunnable)
                            pillText.text = partial.take(44)
                            MainActivity.subtitleView?.text = partial
                        }
                    }
                }
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }
    }

    private fun startListeningSafely() {
        if (isSpeaking || isProcessing || isListeningNow) return
        mainHandler.post {
            try {
                speechRecognizer?.startListening(recognizerIntent)
            } catch (e: Exception) {
                setupPersistentRecognizer()
                mainHandler.postDelayed({ startListeningSafely() }, 500)
            }
        }
    }

    private fun handleSpokenText(spoken: String) {
        val lower = spoken.lowercase(Locale("tr", "TR"))
        val hasWakeWord = lower.contains("niko") || lower.contains("nico")

        if (isAwakeForCommand || hasWakeWord) {
            mainHandler.removeCallbacks(hideRunnable)
            val cleanCmd = lower
                .replace("hey niko", "")
                .replace("hey nico", "")
                .replace("niko", "")
                .replace("nico", "")
                .trim()

            if (cleanCmd.isEmpty()) {
                wakeUpAssistant("🎙️ Seni dinliyorum...")
                startListeningSafely()
            } else {
                isAwakeForCommand = false
                processCommand(cleanCmd)
            }
        } else {
            startListeningSafely()
        }
    }

    private fun processCommand(command: String) {
        isProcessing = true
        val lower = command.lowercase(Locale("tr", "TR"))

        if (lower.contains("whatsapp") && lower.contains("aç")) {
            openApp("com.whatsapp", "WhatsApp")
            return
        }
        if (lower.contains("youtube") && lower.contains("aç")) {
            openApp("com.google.android.youtube", "YouTube")
            return
        }
        if (lower.contains("ayarlar") && lower.contains("aç")) {
            val intent = Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            reply("Ayarları açıyorum efendim.")
            return
        }

        updateUI("DÜŞÜNÜYOR", "⏳ $command", true)
        askGeminiSmart(command, MainActivity.EMBEDDED_KEY)
    }

    private fun openApp(packageName: String, appName: String) {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(launchIntent)
            reply("$appName açılıyor efendim.")
        } else {
            reply("$appName cihazda bulunamadı efendim.")
        }
    }

    private fun reply(message: String) {
        updateUI("KONUŞUYOR", message, true)
        speak(message)
    }

    private fun askGeminiSmart(prompt: String, cleanKey: String) {
        if (activeModelName != null) {
            sendGenerateRequest(prompt, cleanKey, activeModelName!!)
            return
        }

        val listUrl = "https://generativelanguage.googleapis.com/v1beta/models?key=$cleanKey"
        val req = Request.Builder()
            .url(listUrl)
            .addHeader("x-goog-api-key", cleanKey)
            .get()
            .build()

        client.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                reply("Bağlantı hatası oluştu efendim.")
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                if (response.isSuccessful && body != null) {
                    try {
                        val models = JSONObject(body).getJSONArray("models")
                        val available = mutableListOf<String>()

                        for (i in 0 until models.length()) {
                            val m = models.getJSONObject(i)
                            val name = m.getString("name")
                            val methods = m.optJSONArray("supportedGenerationMethods")?.toString() ?: ""
                            if (methods.contains("generateContent")) {
                                available.add(name)
                            }
                        }

                        val selectedModel = available.firstOrNull { it.contains("hydrogen", ignoreCase = true) }
                            ?: available.firstOrNull { it.contains("gemini-4", ignoreCase = true) }
                            ?: available.firstOrNull { it.centerOrNull("gemini-3", ignoreCase = true) == true }
                            ?: available.firstOrNull { it.contains("gemini-2.5", ignoreCase = true) }
                            ?: available.firstOrNull { it.contains("gemini-2.0", ignoreCase = true) }
                            ?: available.firstOrNull { it.contains("gemini", ignoreCase = true) }
                            ?: available.firstOrNull()

                        if (selectedModel != null) {
                            activeModelName = selectedModel
                            sendGenerateRequest(prompt, cleanKey, selectedModel)
                        } else {
                            reply("Aktif model bulunamadı.")
                        }
                    } catch (e: Exception) {
                        reply("Model tarama hatası.")
                    }
                } else {
                    reply("Anahtar doğrulanamadı.")
                }
            }
        })
    }

    private fun sendGenerateRequest(prompt: String, cleanKey: String, modelPath: String) {
        val url = "https://generativelanguage.googleapis.com/v1beta/$modelPath:generateContent?key=$cleanKey"
        val systemInstruction = "Sen Tony Stark'ın JARVIS'i tarzında çalışan, karizmatik, zeki ve sadık sesli asistan Niko'sun. Kullanıcıya gerektiğinde 'efendim' diye hitap et. Çok kısa, net ve doğal konuş (maksimum 2 cümle). Kullanıcı: $prompt"

        val jsonBody = JSONObject().apply {
            put("contents", JSONArray().put(
                JSONObject().put("parts", JSONArray().put(
                    JSONObject().put("text", systemInstruction)
                ))
            ))
        }

        val request = Request.Builder()
            .url(url)
            .addHeader("x-goog-api-key", cleanKey)
            .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                reply("İnternet bağlantı hatası efendim.")
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                if (response.isSuccessful && body != null) {
                    try {
                        val json = JSONObject(body)
                        val answer = json.getJSONArray("candidates")
                            .getJSONObject(0)
                            .getJSONObject("content")
                            .getJSONArray("parts")
                            .getJSONObject(0)
                            .getString("text")
                            .replace("*", "")
                            .trim()
                        reply(answer)
                    } catch (e: Exception) {
                        reply("Cevap çözümlenemedi efendim.")
                    }
                } else {
                    activeModelName = null
                    reply("Hata kodu: ${response.code}")
                }
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer?.destroy()
        tts?.shutdown()
        if (::overlayView.isInitialized) {
            windowManager.removeView(overlayView)
        }
    }
}
