package com.niko.asistan

import android.Manifest
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
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

        // Mikrofon izni iste
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 1)
        }

        val prefs = getSharedPreferences("NikoPrefs", Context.MODE_PRIVATE)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#0F172A"))
            setPadding(60, 60, 60, 60)
        }

        val title = TextView(this).apply {
            text = "🤖 NİKO ASİSTAN v2.0"
            textSize = 28f
            setTextColor(Color.parseColor("#38BDF8"))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 20)
        }

        val desc = TextView(this).apply {
            text = "Sesli Jarvis Motoru & Gemini Beyni\n(Ücretsiz API Anahtarını aşağıya yapıştırabilirsin)"
            textSize = 14f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 40)
        }

        val apiInput = EditText(this).apply {
            hint = "Gemini API Anahtarını Buraya Yapıştır"
            setHintTextColor(Color.GRAY)
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#1E293B"))
            setPadding(30, 30, 30, 30)
            setText(prefs.getString("GEMINI_KEY", ""))
        }

        val btnSaveKey = Button(this).apply {
            text = "API ANAHTARINI KAYDET"
            setBackgroundColor(Color.parseColor("#334155"))
            setTextColor(Color.WHITE)
            setOnClickListener {
                prefs.edit().putString("GEMINI_KEY", apiInput.text.toString().trim()).apply()
                Toast.makeText(this@MainActivity, "Beyin Anahtarı Kaydedildi!", Toast.LENGTH_SHORT).show()
            }
        }

        val spacer = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(1, 50)
        }

        val btnStart = Button(this).apply {
            text = "NİKO'YU BAŞLAT (SESLİ ASİSTAN)"
            setBackgroundColor(Color.parseColor("#0284C7"))
            setTextColor(Color.WHITE)
            textSize = 16f
            setPadding(40, 40, 40, 40)
            setOnClickListener {
                if (!Settings.canDrawOverlays(this@MainActivity)) {
                    Toast.makeText(this@MainActivity, "Lütfen Niko Asistan'a ekran izni verin!", Toast.LENGTH_LONG).show()
                    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
                    startActivity(intent)
                } else {
                    prefs.edit().putString("GEMINI_KEY", apiInput.text.toString().trim()).apply()
                    stopService(Intent(this@MainActivity, OverlayService::class.java))
                    startService(Intent(this@MainActivity, OverlayService::class.java))
                    Toast.makeText(this@MainActivity, "Niko Aktif! Balona dokunup konuşabilirsin.", Toast.LENGTH_LONG).show()
                    finish()
                }
            }
        }

        layout.addView(title)
        layout.addView(desc)
        layout.addView(apiInput)
        layout.addView(btnSaveKey)
        layout.addView(spacer)
        layout.addView(btnStart)
        setContentView(layout)
    }
}

class OverlayService : Service(), TextToSpeech.OnInitListener {
    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: View
    private lateinit var statusText: TextView
    private var speechRecognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private val client = OkHttpClient()
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        tts = TextToSpeech(this, this)

        val bubble = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(45, 35, 45, 35)
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 50f
                setColor(Color.parseColor("#F20F172A"))
                setStroke(4, Color.parseColor("#38BDF8"))
            }
        }

        statusText = TextView(this).apply {
            text = "🤖 Niko Hazır\n(Konuşmak için dokun, kapatmak için basılı tut)"
            textSize = 15f
            setTextColor(Color.parseColor("#38BDF8"))
            gravity = Gravity.CENTER
        }

        bubble.addView(statusText)

        // Tıklayınca dinlemeyi başlat
        bubble.setOnClickListener {
            startListening()
        }

        // Basılı tutunca kapat
        bubble.setOnLongClickListener {
            Toast.makeText(this, "Niko Kapatıldı", Toast.LENGTH_SHORT).show()
            stopSelf()
            true
        }

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
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
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
            speak("Niko sistemleri aktif efendim.")
        }
    }

    private fun speak(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "NikoTTS")
    }

    private fun startListening() {
        mainHandler.post {
            speechRecognizer?.destroy()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "tr-TR")
            }

            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    statusText.text = "🎙️ Seni Dinliyorum...\n(Şimdi konuş)"
                    statusText.setTextColor(Color.parseColor("#4ADE80"))
                }
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {
                    statusText.text = "⏳ Düşünüyorum..."
                    statusText.setTextColor(Color.parseColor("#FACC15"))
                }
                override fun onError(error: Int) {
                    statusText.text = "⚠️ Ses algılanamadı. Tekrar dokun."
                    statusText.setTextColor(Color.parseColor("#F87171"))
                }
                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        val command = matches[0]
                        statusText.text = "🗣️ Sen: \"$command\""
                        processCommand(command)
                    }
                }
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })

            speechRecognizer?.startListening(intent)
        }
    }

    private fun processCommand(command: String) {
        val lower = command.lowercase(Locale("tr", "TR"))

        // 1. Doğrudan Cihaz Komutları (Hızlı Aksiyon)
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
        if (lower.contains("kapat kendini") || lower.contains("görüşürüz")) {
            reply("Görüşmek üzere efendim.")
            mainHandler.postDelayed({ stopSelf() }, 2000)
            return
        }

        // 2. Gemini Yapay Zeka Beynine Sor
        val apiKey = getSharedPreferences("NikoPrefs", Context.MODE_PRIVATE).getString("GEMINI_KEY", "") ?: ""
        if (apiKey.isEmpty()) {
            reply("Seni duydum: '$command'. Ama tam zeka için ana ekrandan ücretsiz Gemini API anahtarını girmelisin.")
            return
        }

        askGemini(command, apiKey)
    }

    private fun openApp(packageName: String, appName: String) {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(launchIntent)
            reply("$appName açılıyor efendim.")
        } else {
            reply("$appName cihazda bulunamadı.")
        }
    }

    private fun reply(message: String) {
        mainHandler.post {
            statusText.text = "🤖 Niko: $message\n(Konuşmak için tekrar dokun)"
            statusText.setTextColor(Color.parseColor("#38BDF8"))
            speak(message)
        }
    }

    private fun askGemini(prompt: String, apiKey: String) {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey"

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
            .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                reply("Bağlantı hatası oluştu.")
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
                        reply("Cevabı çözümleyemedim.")
                    }
                } else {
                    reply("API Anahtarı hatalı veya kota doldu.")
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
