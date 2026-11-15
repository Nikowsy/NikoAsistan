package com.niko.asistan

import android.Manifest
import android.app.Activity
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.PixelFormat
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
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
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

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Sesli komut tetikleyici modunda açıldıysa doğrudan Google mikrofonunu başlat
        if (intent?.getBooleanExtra("VOICE_TRIGGER", false) == true) {
            window.setBackgroundDrawableResource(android.R.color.transparent)
            startVoiceDialog()
            return
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 1)
        }

        val prefs = getSharedPreferences("NikoPrefs", Context.MODE_PRIVATE)

        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#030712"))
            setPadding(80, 60, 80, 60)
        }

        val titleCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(40, 40, 40, 40)
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 30f
                setColor(Color.parseColor("#0F172A"))
                setStroke(3, Color.parseColor("#06B6D4"))
            }
        }

        val title = TextView(this).apply {
            text = "⚡ NİKO ASİSTAN v5.0"
            textSize = 24f
            setTextColor(Color.parseColor("#22D3EE"))
            gravity = Gravity.CENTER
        }

        val subtitle = TextView(this).apply {
            text = "KESİNTİSİZ SES ALGILAMA & HEY NİKO MODU"
            textSize = 11f
            setTextColor(Color.parseColor("#94A3B8"))
            gravity = Gravity.CENTER
            setPadding(0, 8, 0, 0)
        }

        titleCard.addView(title)
        titleCard.addView(subtitle)

        val spacer1 = View(this).apply { layoutParams = LinearLayout.LayoutParams(1, 50) }

        val apiInput = EditText(this).apply {
            hint = "API Anahtarını Buraya Yapıştır"
            setHintTextColor(Color.parseColor("#64748B"))
            setTextColor(Color.WHITE)
            textSize = 14f
            setPadding(40, 30, 40, 30)
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 20f
                setColor(Color.parseColor("#1E293B"))
                setStroke(2, Color.parseColor("#334155"))
            }
            setText(prefs.getString("GEMINI_KEY", ""))
        }

        val spacer2 = View(this).apply { layoutParams = LinearLayout.LayoutParams(1, 30) }

        val btnSaveKey = Button(this).apply {
            text = "BEYİN ANAHTARINI KAYDET"
            textSize = 13f
            setTextColor(Color.WHITE)
            setPadding(40, 30, 40, 30)
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 20f
                setColor(Color.parseColor("#334155"))
            }
            setOnClickListener {
                prefs.edit().putString("GEMINI_KEY", apiInput.text.toString().trim()).apply()
                Toast.makeText(this@MainActivity, "✨ Anahtar Kaydedildi!", Toast.LENGTH_SHORT).show()
            }
        }

        val spacer3 = View(this).apply { layoutParams = LinearLayout.LayoutParams(1, 40) }

        val btnStart = Button(this).apply {
            text = "🚀 NİKO'YU BAŞLAT (SESSİZ AÇILIŞ)"
            textSize = 15f
            setTextColor(Color.WHITE)
            setPadding(50, 45, 50, 45)
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 25f
                setColor(Color.parseColor("#0284C7"))
            }
            setOnClickListener {
                if (!Settings.canDrawOverlays(this@MainActivity)) {
                    Toast.makeText(this@MainActivity, "Lütfen ekran üstü iznini onaylayın!", Toast.LENGTH_LONG).show()
                    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
                    startActivity(intent)
                } else {
                    prefs.edit().putString("GEMINI_KEY", apiInput.text.toString().trim()).apply()
                    stopService(Intent(this@MainActivity, OverlayService::class.java))
                    startService(Intent(this@MainActivity, OverlayService::class.java))
                    finish()
                }
            }
        }

        rootLayout.addView(titleCard)
        rootLayout.addView(spacer1)
        rootLayout.addView(apiInput)
        rootLayout.addView(spacer2)
        rootLayout.addView(btnSaveKey)
        rootLayout.addView(spacer3)
        rootLayout.addView(btnStart)
        setContentView(rootLayout)
    }

    private fun startVoiceDialog() {
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "tr-TR")
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Niko Dinliyor...")
            }
            startActivityForResult(intent, 999)
        } catch (e: Exception) {
            Toast.makeText(this, "Cihazda ses motoru bulunamadı.", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 999 && resultCode == Activity.RESULT_OK && data != null) {
            val matches = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!matches.isNullOrEmpty()) {
                val svcIntent = Intent(this, OverlayService::class.java).apply {
                    putExtra("VOICE_COMMAND", matches[0])
                }
                startService(svcIntent)
            }
        }
        finish()
    }
}

class OverlayService : Service(), TextToSpeech.OnInitListener {
    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: View
    private lateinit var statusText: TextView
    private lateinit var wakeBtn: TextView
    private var speechRecognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private val client = OkHttpClient()
    private val mainHandler = Handler(Looper.getMainLooper())
    private var activeModelName: String? = null
    private var alwaysListening = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val cmd = intent?.getStringExtra("VOICE_COMMAND")
        if (!cmd.isNullOrEmpty()) {
            statusText.text = "🗣️ \"$cmd\""
            processCommand(cmd, fromWakeWord = false)
        }
        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        tts = TextToSpeech(this, this)

        val bubble = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(35, 25, 35, 25)
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 60f
                setColor(Color.parseColor("#E60F172A"))
                setStroke(4, Color.parseColor("#22D3EE"))
            }
        }

        statusText = TextView(this).apply {
            text = "🤖 Niko Hazır (Konuşmak için dokun)"
            textSize = 14f
            setTextColor(Color.parseColor("#22D3EE"))
            setPadding(10, 0, 20, 0)
            setOnClickListener {
                // Garantili ses algılama penceresini aç
                tts?.stop()
                val dialogIntent = Intent(this@OverlayService, MainActivity::class.java).apply {
                    putExtra("VOICE_TRIGGER", true)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
                startActivity(dialogIntent)
            }
        }

        // "Hey Niko" Sürekli Dinleme Aç/Kapat Tuşu
        wakeBtn = TextView(this).apply {
            text = " 👂 "
            textSize = 16f
            setTextColor(Color.parseColor("#94A3B8"))
            setOnClickListener {
                alwaysListening = !alwaysListening
                if (alwaysListening) {
                    setTextColor(Color.parseColor("#4ADE80"))
                    statusText.text = "👂 'Hey Niko' bekleniyor..."
                    startWakeWordLoop()
                } else {
                    setTextColor(Color.parseColor("#94A3B8"))
                    speechRecognizer?.destroy()
                    statusText.text = "🤖 Niko Hazır (Konuşmak için dokun)"
                }
            }
        }

        val closeBtn = TextView(this).apply {
            text = " ✖ "
            textSize = 16f
            setTextColor(Color.parseColor("#F87171"))
            setOnClickListener {
                alwaysListening = false
                stopSelf()
            }
        }

        bubble.addView(statusText)
        bubble.addView(wakeBtn)
        bubble.addView(closeBtn)

        overlayView = bubble

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
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            y = 140
        }

        windowManager.addView(overlayView, params)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale("tr", "TR")
            val audioAttrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
            tts?.setAudioAttributes(audioAttrs)
            // İstediğin gibi açılış konuşması tamamen kaldırıldı!
        }
    }

    private fun speak(text: String) {
        speechRecognizer?.destroy()
        speechRecognizer = null

        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.mode = AudioManager.MODE_NORMAL
        audioManager.isSpeakerphoneOn = true
        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVol, 0)

        val params = Bundle()
        params.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "NikoTTS")

        // Konuşma bitince "Hey Niko" modu açıksa tekrar dinlemeye dön
        if (alwaysListening) {
            mainHandler.postDelayed({ startWakeWordLoop() }, 4000)
        }
    }

    private fun startWakeWordLoop() {
        if (!alwaysListening) return
        mainHandler.post {
            try {
                speechRecognizer?.destroy()
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "tr-TR")
                }

                speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {}
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {}
                    override fun onError(error: Int) {
                        if (alwaysListening) {
                            mainHandler.postDelayed({ startWakeWordLoop() }, 1200)
                        }
                    }
                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            val spoken = matches[0]
                            val lower = spoken.lowercase(Locale("tr", "TR"))
                            if (lower.contains("niko") || lower.contains("nico")) {
                                val cleanCmd = lower.replace("hey niko", "")
                                    .replace("niko", "")
                                    .replace("nico", "")
                                    .trim()
                                if (cleanCmd.isNotEmpty()) {
                                    statusText.text = "🗣️ \"$cleanCmd\""
                                    processCommand(cleanCmd, fromWakeWord = true)
                                } else {
                                    // Sadece "Hey Niko" dediyse garantili mikrofon penceresini aç
                                    val dialogIntent = Intent(this@OverlayService, MainActivity::class.java).apply {
                                        putExtra("VOICE_TRIGGER", true)
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                    }
                                    startActivity(dialogIntent)
                                }
                                return
                            }
                        }
                        if (alwaysListening) {
                            mainHandler.postDelayed({ startWakeWordLoop() }, 800)
                        }
                    }
                    override fun onPartialResults(partialResults: Bundle?) {}
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })

                speechRecognizer?.startListening(intent)
            } catch (e: Exception) {}
        }
    }

    private fun processCommand(command: String, fromWakeWord: Boolean) {
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
            reply("Ayarları açıyorum.")
            return
        }
        if (lower.contains("kapat kendini") || lower.contains("görüşürüz")) {
            alwaysListening = false
            reply("Görüşmek üzere.")
            mainHandler.postDelayed({ stopSelf() }, 2000)
            return
        }

        val apiKey = getSharedPreferences("NikoPrefs", Context.MODE_PRIVATE).getString("GEMINI_KEY", "") ?: ""
        if (apiKey.isEmpty()) {
            reply("Önce ana ekrandan anahtarı kaydetmelisin.")
            return
        }

        statusText.text = "⏳ Düşünüyorum..."
        statusText.setTextColor(Color.parseColor("#FACC15"))
        askGeminiSmart(command, apiKey.trim())
    }

    private fun openApp(packageName: String, appName: String) {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(launchIntent)
            reply("$appName açılıyor.")
        } else {
            reply("$appName cihazda bulunamadı.")
        }
    }

    private fun reply(message: String) {
        mainHandler.post {
            statusText.text = "🤖 $message"
            statusText.setTextColor(Color.parseColor("#22D3EE"))
            speak(message)
        }
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
                reply("Bağlantı hatası.")
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
                            ?: available.firstOrNull { it.contains("gemini-3", ignoreCase = true) }
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
                    reply("Anahtar doğrulanamadı (Kod: ${response.code})")
                }
            }
        })
    }

    private fun sendGenerateRequest(prompt: String, cleanKey: String, modelPath: String) {
        val url = "https://generativelanguage.googleapis.com/v1beta/$modelPath:generateContent?key=$cleanKey"
        val systemInstruction = "Sen kullanıcının Android cihazında çalışan Jarvis benzeri sesli asistanı Niko'sun. Çok kısa, net, samimi ve doğrudan cevap ver (maksimum 2 cümle). Kullanıcı: $prompt"

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
                reply("İnternet hatası.")
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
                        reply("Cevap okunamadı.")
                    }
                } else {
                    activeModelName = null
                    reply("Hata Kodu: ${response.code}")
                }
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        alwaysListening = false
        speechRecognizer?.destroy()
        tts?.shutdown()
        if (::overlayView.isInitialized) {
            windowManager.removeView(overlayView)
        }
    }
}
