package com.niko.asistan

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.AudioAttributes
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
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.URLEncoder
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object AppConfig {
    const val PREFS = "JarvisMinimalPrefs"
    const val DEFAULT_KEY = "AQ.Ab8RN6KZYPqSekpNWFEnuG42klSAK_TKxGeFNc1YbcduzUm0kA"

    fun isSetupDone(ctx: Context): Boolean =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean("SETUP_DONE", false)

    fun setSetupDone(ctx: Context, done: Boolean) =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean("SETUP_DONE", done).apply()

    fun getName(ctx: Context): String =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("AI_NAME", "")?.takeIf { it.isNotBlank() } ?: "Jarvis"

    fun setName(ctx: Context, name: String) =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString("AI_NAME", name.trim()).apply()

    fun getUserName(ctx: Context): String =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("USER_NAME", "Efendim")?.takeIf { it.isNotBlank() } ?: "Efendim"

    fun setUserName(ctx: Context, name: String) =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString("USER_NAME", name.trim()).apply()

    fun getKey(ctx: Context): String =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("API_KEY", DEFAULT_KEY)?.trim() ?: DEFAULT_KEY

    fun setKey(ctx: Context, key: String) =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString("API_KEY", key.trim()).apply()

    fun getLang(ctx: Context): String =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("LANG", "tr-TR") ?: "tr-TR"

    fun setLang(ctx: Context, lang: String) =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString("LANG", lang).apply()

    fun getSpeed(ctx: Context): Float =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getFloat("SPEED", 1.0f)

    fun setSpeed(ctx: Context, speed: Float) =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putFloat("SPEED", speed).apply()

    fun getVoiceProfile(ctx: Context): Int =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt("VOICE_PROFILE", 2)

    fun setVoiceProfile(ctx: Context, idx: Int) =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putInt("VOICE_PROFILE", idx).apply()

    fun getTalkMode(ctx: Context): String =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("TALK_MODE", "AUTO") ?: "AUTO"

    fun setTalkMode(ctx: Context, mode: String) =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString("TALK_MODE", mode).apply()

    fun getModelChoice(ctx: Context): String =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("MODEL_CHOICE", "HYDROGEN") ?: "HYDROGEN"

    fun setModelChoice(ctx: Context, model: String) =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString("MODEL_CHOICE", model).apply()

    fun getHistoryJson(ctx: Context): JSONArray {
        val raw = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("CHAT_HISTORY", "[]") ?: "[]"
        return try { JSONArray(raw) } catch (e: Exception) { JSONArray() }
    }

    fun addHistoryItem(ctx: Context, role: String, text: String) {
        val arr = getHistoryJson(ctx)
        val item = JSONObject().apply {
            put("role", role)
            put("text", text)
            put("time", System.currentTimeMillis())
        }
        arr.put(item)
        val trimmed = JSONArray()
        val start = (arr.length() - 60).coerceAtLeast(0)
        for (i in start until arr.length()) {
            trimmed.put(arr.getJSONObject(i))
        }
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString("CHAT_HISTORY", trimmed.toString()).apply()
    }

    fun clearHistory(ctx: Context) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString("CHAT_HISTORY", "[]").apply()
    }
}

// FOTOĞRAF 1'DEKİ 3 BOYUTLU BEYAZ NOKTALI KÜRE (3D Particle Dot Sphere)
class ParticleSphereView(context: Context) : View(context) {
    var audioLevel = 0f
    var isActive = false
    private var rotY = 0f
    private var rotX = 0.25f
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    private val points = mutableListOf<FloatArray>()
    init {
        val numPoints = 240
        val goldenAngle = Math.PI * (3.0 - sqrt(5.0))
        for (i in 0 until numPoints) {
            val y = 1.0 - (i.toDouble() / (numPoints - 1)) * 2.0
            val radius = sqrt(1.0 - y * y)
            val theta = goldenAngle * i
            val x = cos(theta) * radius
            val z = sin(theta) * radius
            points.add(floatArrayOf(x.toFloat(), y.toFloat(), z.toFloat()))
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val baseRadius = (width.coerceAtMost(height) * 0.34f).coerceAtLeast(80f)

        val speed = if (isActive) 0.032f else 0.009f
        rotY += speed
        val pulse = if (isActive) (audioLevel * 4.5f).coerceIn(4f, 38f) else 0f

        val cosY = cos(rotY)
        val sinY = sin(rotY)
        val cosX = cos(rotX)
        val sinX = sin(rotX)

        for (i in points.indices) {
            val p = points[i]
            val wave = if (isActive) sin(rotY * 4f + i * 0.15f) * (pulse * 0.35f) else 0f
            val r = baseRadius + wave

            // Y ekseni dönüşü
            val x1 = p[0] * cosY - p[2] * sinY
            val z1 = p[2] * cosY + p[0] * sinY
            // X ekseni dönüşü
            val y2 = p[1] * cosX - z1 * sinX
            val z2 = z1 * cosX + p[1] * sinX

            val depth = (z2 + 1.4f) / 2.4f // 0..1 arası derinlik
            val alpha = (depth * 235f + 20f).toInt().coerceIn(25, 255)
            val dotSize = (depth * 4.2f + 1.5f)

            dotPaint.alpha = alpha
            canvas.drawCircle(cx + x1 * r, cy + y2 * r, dotSize, dotPaint)
        }

        postInvalidateOnAnimation()
    }
}

// FOTOĞRAF 2'DEKİ GEMİNİ MİKROFON İKONU (İçi Dolu Beyaz Daire + Siyah Mikrofon Çizimi)
class GeminiMicButtonView(context: Context) : View(context) {
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }
    private val micFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#111111")
        style = Paint.Style.FILL
    }
    private val micStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#111111")
        style = Paint.Style.STROKE
        strokeWidth = 4.5f
        strokeCap = Paint.Cap.ROUND
    }
    private val rect = RectF()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val r = (width.coerceAtMost(height) / 2f) - 2f

        // Beyaz Daire
        canvas.drawCircle(cx, cy, r, bgPaint)

        // Siyah Mikrofon Kapsülü
        val mw = r * 0.28f
        val mh = r * 0.42f
        rect.set(cx - mw, cy - mh, cx + mw, cy + mh * 0.25f)
        canvas.drawRoundRect(rect, mw, mw, micFillPaint)

        // Mikrofon Alt Yay Çizgisi
        val arcR = r * 0.44f
        rect.set(cx - arcR, cy - mh * 0.4f, cx + arcR, cy + arcR * 0.85f)
        canvas.drawArc(rect, 0f, 180f, false, micStrokePaint)

        // Mikrofon Ayak Çizgisi
        canvas.drawLine(cx, cy + arcR * 0.85f, cx, cy + r * 0.56f, micStrokePaint)
    }
}

// FOTOĞRAF 2'DEKİ GEMİNİ SES DALGASI İKONU (ılı)
class GeminiWaveIconView(context: Context) : View(context) {
    var activeWave = false
    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 5f
        strokeCap = Paint.Cap.ROUND
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val gap = 12f
        val h1 = if (activeWave) 18f else 10f
        val h2 = if (activeWave) 30f else 22f
        val h3 = if (activeWave) 22f else 13f
        val h4 = if (activeWave) 12f else 7f

        canvas.drawLine(cx - gap * 1.5f, cy - h1, cx - gap * 1.5f, cy + h1, barPaint)
        canvas.drawLine(cx - gap * 0.5f, cy - h2, cx - gap * 0.5f, cy + h2, barPaint)
        canvas.drawLine(cx + gap * 0.5f, cy - h3, cx + gap * 0.5f, cy + h3, barPaint)
        canvas.drawLine(cx + gap * 1.5f, cy - h4, cx + gap * 1.5f, cy + h4, barPaint)
    }
}

class MainActivity : AppCompatActivity() {
    companion object {
        var sphereView: ParticleSphereView? = null
        var centerGreetingTv: TextView? = null
        var centerSubTv: TextView? = null
        var headerTitleTv: TextView? = null
        private const val REQ_PICK_ICON = 701
    }

    private lateinit var rootContainer: FrameLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 1)
        }

        rootContainer = FrameLayout(this).apply {
            setBackgroundColor(Color.parseColor("#08080A"))
        }
        setContentView(rootContainer)

        if (!AppConfig.isSetupDone(this)) {
            showOnboardingScreen()
        } else {
            showMinimalistJarvisHome()
            ensureOverlayServiceRunning()
        }
    }

    private fun ensureOverlayServiceRunning() {
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Sağ alt Gemini barı için Ekran Üstü İznini onaylayın", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
        } else {
            startAssistantService("START_BG")
        }
    }

    fun startAssistantService(action: String, extraText: String? = null) {
        val svc = Intent(this, OverlayService::class.java).apply {
            putExtra("ACTION", action)
            if (extraText != null) putExtra("EXTRA_TEXT", extraText)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(svc)
        } else {
            startService(svc)
        }
    }

    // İLK KURULUM EKRANI (Kürtçe Kaldırıldı, Sade Siyah-Beyaz Şık Tasarım)
    private fun showOnboardingScreen() {
        rootContainer.removeAllViews()
        val scroll = ScrollView(this).apply { isFillViewport = true }
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(65, 90, 65, 70)
            setBackgroundColor(Color.parseColor("#08080A"))
        }

        val miniSphere = ParticleSphereView(this).apply {
            layoutParams = LinearLayout.LayoutParams(340, 340).apply {
                gravity = Gravity.CENTER
                bottomMargin = 45
            }
        }

        val greetingTitle = TextView(this).apply {
            text = "Asistanınızı Oluşturun"
            textSize = 26f
            setTextColor(Color.WHITE)
            typeface = Typeface.create("serif", Typeface.NORMAL)
            gravity = Gravity.CENTER
        }

        val subDesc = TextView(this).apply {
            text = "Klavye yok, karmaşa yok — dokunun veya seslenin."
            textSize = 14f
            setTextColor(Color.parseColor("#8E8E93"))
            gravity = Gravity.CENTER
            setPadding(20, 14, 20, 40)
        }

        val aiNameInput = EditText(this).apply {
            hint = "Asistanın Adı (Örn: Jarvis, Ahmet, Cuma...)"
            setText(if (AppConfig.isSetupDone(this@MainActivity)) AppConfig.getName(this@MainActivity) else "")
            setHintTextColor(Color.parseColor("#636366"))
            setTextColor(Color.WHITE)
            textSize = 15f
            gravity = Gravity.CENTER
            setPadding(40, 32, 40, 32)
            background = GradientDrawable().apply {
                cornerRadius = 35f
                setColor(Color.parseColor("#141418"))
                setStroke(2, Color.parseColor("#2C2C34"))
            }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = 25
            }
        }

        val userNameInput = EditText(this).apply {
            hint = "Sizin Adınız (Hitap için, Örn: Abdullah)"
            setText("")
            setHintTextColor(Color.parseColor("#636366"))
            setTextColor(Color.WHITE)
            textSize = 15f
            gravity = Gravity.CENTER
            setPadding(40, 32, 40, 32)
            background = GradientDrawable().apply {
                cornerRadius = 35f
                setColor(Color.parseColor("#141418"))
                setStroke(2, Color.parseColor("#2C2C34"))
            }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = 35
            }
        }

        // 4 Dil Seçeneği (Kürtçe kaldırıldı: Türkçe, English, العربية, Русский)
        val langs = listOf(
            "🇹🇷 Türkçe" to "tr-TR",
            "🇬🇧 English" to "en-US",
            "🇸🇦 العربية" to "ar-SA",
            "🇷🇺 Русский" to "ru-RU"
        )

        var selectedLang = AppConfig.getLang(this)
        val pillButtons = mutableListOf<Pair<String, TextView>>()

        fun refreshLangPills() {
            for ((code, tv) in pillButtons) {
                val active = (code == selectedLang)
                tv.background = GradientDrawable().apply {
                    cornerRadius = 50f
                    setColor(if (active) Color.WHITE else Color.parseColor("#141418"))
                    setStroke(2, if (active) Color.WHITE else Color.parseColor("#2C2C34"))
                }
                tv.setTextColor(if (active) Color.BLACK else Color.parseColor("#D1D5DB"))
            }
        }

        val row1 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
        val row2 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER; setPadding(0, 16, 0, 45) }

        langs.forEachIndexed { idx, (label, code) ->
            val chip = TextView(this).apply {
                text = label
                textSize = 13f
                typeface = Typeface.DEFAULT_BOLD
                setPadding(38, 22, 38, 22)
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(12, 6, 12, 6)
                }
                setOnClickListener {
                    selectedLang = code
                    AppConfig.setLang(this@MainActivity, code)
                    refreshLangPills()
                }
            }
            pillButtons.add(code to chip)
            if (idx < 2) row1.addView(chip) else row2.addView(chip)
        }
        refreshLangPills()

        val continueBtn = Button(this).apply {
            text = "Başla"
            textSize = 16f
            setTextColor(Color.BLACK)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(40, 36, 40, 36)
            background = GradientDrawable().apply {
                cornerRadius = 60f
                setColor(Color.WHITE)
            }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            setOnClickListener {
                val chosenAi = aiNameInput.text.toString().trim().ifEmpty { "Jarvis" }
                val chosenUser = userNameInput.text.toString().trim().ifEmpty { "Efendim" }
                AppConfig.setName(this@MainActivity, chosenAi)
                AppConfig.setUserName(this@MainActivity, chosenUser)
                AppConfig.setLang(this@MainActivity, selectedLang)
                AppConfig.setSetupDone(this@MainActivity, true)
                showMinimalistJarvisHome()
                ensureOverlayServiceRunning()
                startAssistantService("RELOAD_CONFIG")
            }
        }

        col.addView(miniSphere)
        col.addView(greetingTitle)
        col.addView(subDesc)
        col.addView(aiNameInput)
        col.addView(userNameInput)
        col.addView(row1)
        col.addView(row2)
        col.addView(continueBtn)

        scroll.addView(col)
        rootContainer.addView(scroll)
    }

    // FOTOĞRAF 1'DEKİ SADE SİYAH-BEYAZ JARVIS ANA EKRANI
    private fun showMinimalistJarvisHome() {
        rootContainer.removeAllViews()
        val aiName = AppConfig.getName(this)
        val userName = AppConfig.getUserName(this)

        val mainFrame = FrameLayout(this).apply {
            setBackgroundColor(Color.parseColor("#08080A"))
        }

        // 1. ÜST BAR: Sol (🕒 Geçmiş) | Orta (Jarvis - Serif) | Sağ (🔑 Anahtar & ⚙️ Ayarlar)
        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(50, 55, 50, 20)
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.TOP)
        }

        val clockHistoryBtn = TextView(this).apply {
            text = "🕒"
            textSize = 18f
            setTextColor(Color.parseColor("#8E8E93"))
            setPadding(15, 15, 30, 15)
            setOnClickListener { showChatAndWriteScreen(focusKeyboard = false) }
        }

        val titleTv = TextView(this).apply {
            text = aiName
            textSize = 22f
            setTextColor(Color.WHITE)
            typeface = Typeface.create("serif", Typeface.NORMAL)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener { showRenameDialog() }
        }
        headerTitleTv = titleTv

        val apiKeyTopBtn = TextView(this).apply {
            text = "🔑"
            textSize = 16f
            setPadding(20, 15, 20, 15)
            setOnClickListener { showApiKeyDialog() }
        }

        val settingsTopBtn = TextView(this).apply {
            text = "⚙️"
            textSize = 19f
            setTextColor(Color.parseColor("#8E8E93"))
            setPadding(20, 15, 10, 15)
            setOnClickListener { showSettingsScreen() }
        }

        topBar.addView(clockHistoryBtn)
        topBar.addView(titleTv)
        topBar.addView(apiKeyTopBtn)
        topBar.addView(settingsTopBtn)

        // 2. ORTA BÖLÜM: Noktalı Küre + Serif Karşılama ("Günaydın / İyi günler...")
        val centerCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
            setPadding(40, 60, 40, 140)
        }

        val pSphere = ParticleSphereView(this).apply {
            layoutParams = LinearLayout.LayoutParams(640, 640)
            setOnClickListener {
                startAssistantService("WAKE_NOW")
            }
            setOnTouchListener { _, event ->
                if (AppConfig.getTalkMode(this@MainActivity) == "PTT") {
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            startAssistantService("WAKE_NOW")
                            true
                        }
                        MotionEvent.ACTION_UP -> {
                            startAssistantService("FINALIZE_SPEECH")
                            true
                        }
                        else -> false
                    }
                } else false
            }
        }
        sphereView = pSphere

        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val timeGreeting = when {
            hour in 5..11 -> "Günaydın"
            hour in 12..17 -> "İyi günler"
            hour in 18..22 -> "İyi akşamlar"
            else -> "İyi geceler"
        }

        val greetingLabel = TextView(this).apply {
            text = "$timeGreeting, $userName."
            textSize = 27f
            setTextColor(Color.WHITE)
            typeface = Typeface.create("serif", Typeface.NORMAL)
            gravity = Gravity.CENTER
            setPadding(30, 35, 30, 10)
        }
        centerGreetingTv = greetingLabel

        val subLabel = TextView(this).apply {
            text = "Konuşmak için küreye dokun veya \"Hey $aiName\" de"
            textSize = 13f
            setTextColor(Color.parseColor("#636366"))
            gravity = Gravity.CENTER
            setPadding(40, 4, 40, 10)
        }
        centerSubTv = subLabel

        centerCol.addView(pSphere)
        centerCol.addView(greetingLabel)
        centerCol.addView(subLabel)

        // 3. ALT BÖLÜM: "⌨️ Yaz" Butonu ve "Jarvis | Bugün | Hafıza" Alt Barı (Fotoğraf 1 Birebir)
        val bottomWrap = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM)
        }

        // "⌨️ Yaz" Hap Butonu
        val writePillBtn = TextView(this).apply {
            text = "⌨️  Yaz"
            textSize = 14f
            setTextColor(Color.parseColor("#A1A1AA"))
            gravity = Gravity.CENTER
            setPadding(48, 22, 48, 22)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = 35
            }
            background = GradientDrawable().apply {
                cornerRadius = 50f
                setColor(Color.parseColor("#121216"))
                setStroke(1, Color.parseColor("#27272A"))
            }
            setOnClickListener {
                showChatAndWriteScreen(focusKeyboard = true)
            }
        }

        val divider = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1)
            setBackgroundColor(Color.parseColor("#1C1C22"))
        }

        val navBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(20, 22, 20, 32)
            setBackgroundColor(Color.parseColor("#0A0A0D"))
        }

        fun navTab(icon: String, label: String, active: Boolean, onClick: () -> Unit): View {
            return LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                val ic = TextView(this@MainActivity).apply {
                    text = icon
                    textSize = 16f
                    setTextColor(if (active) Color.parseColor("#818CF8") else Color.parseColor("#636366"))
                    gravity = Gravity.CENTER
                }
                val lb = TextView(this@MainActivity).apply {
                    text = label
                    textSize = 11f
                    setTextColor(if (active) Color.parseColor("#818CF8") else Color.parseColor("#636366"))
                    gravity = Gravity.CENTER
                    setPadding(0, 4, 0, 0)
                }
                addView(ic)
                addView(lb)
                setOnClickListener { onClick() }
            }
        }

        navBar.addView(navTab("❖", aiName, true) { showMinimalistJarvisHome() })
        navBar.addView(navTab("☰", "Bugün", false) {
            startAssistantService("TEXT_QUERY", "Bugünün kısa özetini ve motivasyonunu söyle")
        })
        navBar.addView(navTab("◊", "Hafıza / Yazışmalar", false) {
            showChatAndWriteScreen(focusKeyboard = false)
        })

        bottomWrap.addView(writePillBtn)
        bottomWrap.addView(divider)
        bottomWrap.addView(navBar)

        mainFrame.addView(centerCol)
        mainFrame.addView(topBar)
        mainFrame.addView(bottomWrap)

        rootContainer.addView(mainFrame)
    }

    // YAZI YAZMA & SOHBET EKRANI (Hem sesli hem yazılı tam kontrol)
    private fun showChatAndWriteScreen(focusKeyboard: Boolean) {
        rootContainer.removeAllViews()
        val aiName = AppConfig.getName(this)

        val mainCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#08080A"))
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(40, 50, 40, 30)
            setBackgroundColor(Color.parseColor("#101014"))
        }

        val backBtn = TextView(this).apply {
            text = "←  Geri"
            textSize = 16f
            setTextColor(Color.WHITE)
            setOnClickListener { showMinimalistJarvisHome() }
        }

        val title = TextView(this).apply {
            text = "$aiName — Yazışma"
            textSize = 17f
            setTextColor(Color.WHITE)
            typeface = Typeface.create("serif", Typeface.NORMAL)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val clearBtn = TextView(this).apply {
            text = "Temizle"
            textSize = 13f
            setTextColor(Color.parseColor("#8E8E93"))
            setOnClickListener {
                AppConfig.clearHistory(this@MainActivity)
                showChatAndWriteScreen(focusKeyboard = false)
            }
        }

        header.addView(backBtn)
        header.addView(title)
        header.addView(clearBtn)

        val chatScroll = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        }
        val messagesList = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(35, 30, 35, 30)
        }
        chatScroll.addView(messagesList)

        fun renderMessages() {
            messagesList.removeAllViews()
            val arr = AppConfig.getHistoryJson(this)
            if (arr.length() == 0) {
                val empty = TextView(this).apply {
                    text = "$aiName'a aşağıdan istediğinizi yazabilir veya seslenebilirsiniz."
                    textSize = 14f
                    setTextColor(Color.parseColor("#636366"))
                    gravity = Gravity.CENTER
                    setPadding(40, 150, 40, 40)
                }
                messagesList.addView(empty)
            } else {
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val isUser = obj.getString("role") == "user"
                    val msgText = obj.getString("text")

                    val bubble = TextView(this).apply {
                        text = msgText
                        textSize = 15f
                        setTextColor(if (isUser) Color.BLACK else Color.WHITE)
                        setPadding(38, 26, 38, 26)
                        background = GradientDrawable().apply {
                            cornerRadius = 35f
                            setColor(if (isUser) Color.WHITE else Color.parseColor("#16161C"))
                            if (!isUser) setStroke(1, Color.parseColor("#2C2C34"))
                        }
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            gravity = if (isUser) Gravity.END else Gravity.START
                            topMargin = 12
                            bottomMargin = 12
                        }
                    }
                    messagesList.addView(bubble)
                }
                chatScroll.post { chatScroll.fullScroll(View.FOCUS_DOWN) }
            }
        }
        renderMessages()

        // Siyah-Beyaz Gemini Tarzı Alt Yazma Kapsülü
        val inputPill = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(35, 18, 25, 18)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(30, 15, 30, 35)
            }
            background = GradientDrawable().apply {
                cornerRadius = 70f
                setColor(Color.parseColor("#16161C"))
                setStroke(2, Color.parseColor("#3F3F46"))
            }
        }

        val chatInput = EditText(this).apply {
            hint = "+ $aiName'a yazın..."
            setHintTextColor(Color.parseColor("#8E8E93"))
            setTextColor(Color.WHITE)
            textSize = 15f
            background = null
            imeOptions = EditorInfo.IME_ACTION_SEND
            setSingleLine(true)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        fun submitTypedMessage() {
            val q = chatInput.text.toString().trim()
            if (q.isNotEmpty()) {
                chatInput.setText("")
                startAssistantService("TEXT_QUERY", q)
                Handler(Looper.getMainLooper()).postDelayed({ renderMessages() }, 600)
                Handler(Looper.getMainLooper()).postDelayed({ renderMessages() }, 2500)
                Handler(Looper.getMainLooper()).postDelayed({ renderMessages() }, 4800)
            }
        }

        chatInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                submitTypedMessage()
                true
            } else false
        }

        val micBtn = GeminiMicButtonView(this).apply {
            layoutParams = LinearLayout.LayoutParams(78, 78).apply { rightMargin = 16 }
            setOnClickListener { startAssistantService("WAKE_NOW") }
        }

        val sendBtn = TextView(this).apply {
            text = "Gönder"
            textSize = 14f
            setTextColor(Color.BLACK)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(32, 18, 32, 18)
            background = GradientDrawable().apply {
                cornerRadius = 50f
                setColor(Color.WHITE)
            }
            setOnClickListener { submitTypedMessage() }
        }

        inputPill.addView(chatInput)
        inputPill.addView(micBtn)
        inputPill.addView(sendBtn)

        mainCol.addView(header)
        mainCol.addView(chatScroll)
        mainCol.addView(inputPill)

        rootContainer.addView(mainCol)
        if (focusKeyboard) {
            chatInput.requestFocus()
        }
    }

    // AYARLAR EKRANI (Kürtçe Yok, Sade Siyah-Beyaz Kartlar, 2 Kız + 2 Erkek Ses, Simge, Hız, Model)
    private fun showSettingsScreen() {
        rootContainer.removeAllViews()
        val scroll = ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#08080A"))
        }
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(45, 55, 45, 80)
        }

        val backTv = TextView(this).apply {
            text = "←   Ayarlar"
            textSize = 20f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, 30)
            setOnClickListener {
                startAssistantService("RELOAD_CONFIG")
                showMinimalistJarvisHome()
            }
        }
        col.addView(backTv)

        fun sectionHeader(title: String): TextView = TextView(this).apply {
            text = title
            textSize = 13f
            setTextColor(Color.parseColor("#8E8E93"))
            typeface = Typeface.DEFAULT_BOLD
            setPadding(10, 32, 10, 14)
        }

        fun optionCard(label: String, rightBadge: String?, isSelected: Boolean, onClick: () -> Unit): View {
            return LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(38, 32, 38, 32)
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    bottomMargin = 14
                }
                background = GradientDrawable().apply {
                    cornerRadius = 28f
                    setColor(Color.parseColor("#121216"))
                    setStroke(if (isSelected) 3 else 1, if (isSelected) Color.WHITE else Color.parseColor("#24242C"))
                }
                val t = TextView(this@MainActivity).apply {
                    text = label
                    textSize = 15f
                    setTextColor(Color.WHITE)
                    typeface = if (isSelected) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }
                addView(t)
                if (rightBadge != null) {
                    val b = TextView(this@MainActivity).apply {
                        text = rightBadge
                        textSize = 12f
                        setTextColor(Color.parseColor("#D1D5DB"))
                        setPadding(22, 8, 22, 8)
                        background = GradientDrawable().apply {
                            cornerRadius = 18f
                            setColor(Color.parseColor("#22222A"))
                        }
                    }
                    addView(b)
                }
                if (isSelected) {
                    val check = TextView(this@MainActivity).apply {
                        text = "   ✓"
                        textSize = 17f
                        setTextColor(Color.WHITE)
                        typeface = Typeface.DEFAULT_BOLD
                    }
                    addView(check)
                }
                setOnClickListener { onClick() }
            }
        }

        col.addView(sectionHeader("Kimlik, API Anahtarı & Simge"))
        col.addView(optionCard("Asistan Adı: ${AppConfig.getName(this)} (\"Hey ${AppConfig.getName(this)}\")", "Değiştir", false) {
            showRenameDialog()
        })
        col.addView(optionCard("🔑 API Anahtarını Değiştir", "Düzenle", false) {
            showApiKeyDialog()
        })
        col.addView(optionCard("🖼️ Uygulama Simgesini Değiştir (Galeriden Seç)", "Yükle", false) {
            val pick = Intent(Intent.ACTION_GET_CONTENT).apply { type = "image/*" }
            startActivityForResult(Intent.createChooser(pick, "Uygulama Simgesi Seçin"), REQ_PICK_ICON)
        })

        // Dil Seçenekleri (Kürtçe Yok!)
        col.addView(sectionHeader("Dil"))
        val curLang = AppConfig.getLang(this)
        val langOptions = listOf(
            "🇹🇷   Türkçe" to "tr-TR",
            "🇸🇦   Arapça" to "ar-SA",
            "🇺🇸   İngilizce" to "en-US",
            "🇷🇺   Rusça" to "ru-RU"
        )
        langOptions.forEach { (title, code) ->
            col.addView(optionCard(title, null, curLang == code) {
                AppConfig.setLang(this, code)
                startAssistantService("RELOAD_CONFIG")
                showSettingsScreen()
            })
        }

        // 2 Kız + 2 Erkek İnsansı Ses
        col.addView(sectionHeader("Ses Tonu (2 Kız & 2 Erkek İnsansı Ses)"))
        val curVoice = AppConfig.getVoiceProfile(this)
        val voiceProfiles = listOf(
            0 to "👩  Kız Sesi 1 (Doğal & Yumuşak Ton)",
            1 to "👩‍🎤  Kız Sesi 2 (Canlı & Net Ton)",
            2 to "👨  Erkek Sesi 1 (Jarvis Karizmatik Ton)",
            3 to "🧔  Erkek Sesi 2 (Derin Stüdyo Tonu)"
        )
        voiceProfiles.forEach { (idx, name) ->
            col.addView(optionCard(name, null, curVoice == idx) {
                AppConfig.setVoiceProfile(this, idx)
                startAssistantService("RELOAD_CONFIG")
                showSettingsScreen()
            })
        }

        // Konuşma Hızı
        col.addView(sectionHeader("Konuşma Hızı"))
        val curSpeed = AppConfig.getSpeed(this)
        val speeds = listOf(
            Triple("Normal", "1x", 1.0f),
            Triple("Hızlı", "1.2x", 1.2f),
            Triple("Turbo", "1.4x", 1.4f)
        )
        speeds.forEach { (label, badge, value) ->
            col.addView(optionCard(label, badge, kotlin.math.abs(curSpeed - value) < 0.05f) {
                AppConfig.setSpeed(this, value)
                startAssistantService("RELOAD_CONFIG")
                showSettingsScreen()
            })
        }

        // Konuşma Modu
        col.addView(sectionHeader("Konuşma Modu"))
        val curMode = AppConfig.getTalkMode(this)
        col.addView(optionCard("Otomatik (Sustuğunda kendisi gönderir)", null, curMode == "AUTO") {
            AppConfig.setTalkMode(this, "AUTO")
            showSettingsScreen()
        })
        col.addView(optionCard("Bas-konuş (Basılı tut konuş, bırakınca gönderir)", null, curMode == "PTT") {
            AppConfig.setTalkMode(this, "PTT")
            showSettingsScreen()
        })

        // Model Seçimi
        col.addView(sectionHeader("Yapay Zeka Modeli"))
        val curModel = AppConfig.getModelChoice(this)
        val models = listOf(
            "3.2_FLASH" to "⚡ 3.2 Flash (Hızlı Yanıt)",
            "HYDROGEN" to "🔥 Hydrogen (Derin Zeka & Arama)",
            "ARGON" to "💎 Argon (Gelişmiş Analiz)"
        )
        models.forEach { (mCode, mLabel) ->
            col.addView(optionCard(mLabel, mCode, curModel == mCode) {
                AppConfig.setModelChoice(this, mCode)
                startAssistantService("RELOAD_CONFIG")
                showSettingsScreen()
            })
        }

        scroll.addView(col)
        rootContainer.addView(scroll)
    }

    private fun showRenameDialog() {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 30, 50, 10)
        }
        val aiInput = EditText(this).apply {
            setText(AppConfig.getName(this@MainActivity))
            hint = "Asistan Adı (Örn: Jarvis)"
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
        }
        val userInput = EditText(this).apply {
            setText(AppConfig.getUserName(this@MainActivity))
            hint = "Sizin Adınız (Örn: Abdullah)"
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
        }
        box.addView(aiInput)
        box.addView(userInput)

        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("Asistan & Kullanıcı Adı")
            .setView(box)
            .setPositiveButton("Kaydet") { _, _ ->
                val newAi = aiInput.text.toString().trim().ifEmpty { "Jarvis" }
                val newUser = userInput.text.toString().trim().ifEmpty { "Efendim" }
                AppConfig.setName(this, newAi)
                AppConfig.setUserName(this, newUser)
                startAssistantService("RELOAD_CONFIG")
                showMinimalistJarvisHome()
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    private fun showApiKeyDialog() {
        val input = EditText(this).apply {
            setText(AppConfig.getKey(this@MainActivity))
            hint = "API Anahtarı (AIza... veya AQ...)"
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
            setPadding(45, 35, 45, 35)
        }
        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("🔑 API Anahtarı")
            .setMessage("Anahtarınızı buradan değiştirebilirsiniz (AQ. veya AIza desteklenir):")
            .setView(input)
            .setPositiveButton("Kaydet") { _, _ ->
                val k = input.text.toString().trim()
                if (k.isNotEmpty()) {
                    AppConfig.setKey(this, k)
                    startAssistantService("RELOAD_CONFIG")
                    Toast.makeText(this, "Anahtar kaydedildi!", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_PICK_ICON && resultCode == Activity.RESULT_OK && data?.data != null) {
            try {
                val uri = data.data!!
                val inputStream = contentResolver.openInputStream(uri)
                val original = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                if (original != null) {
                    val scaled = Bitmap.createScaledBitmap(original, 256, 256, true)
                    val outFile = File(filesDir, "custom_app_icon.png")
                    FileOutputStream(outFile).use { out ->
                        scaled.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }

                    if (ShortcutManagerCompat.isRequestPinShortcutSupported(this)) {
                        val shortcutIntent = Intent(this, MainActivity::class.java).apply {
                            action = Intent.ACTION_MAIN
                        }
                        val pinInfo = ShortcutInfoCompat.Builder(this, "custom_ai_shortcut_${System.currentTimeMillis()}")
                            .setShortLabel(AppConfig.getName(this))
                            .setLongLabel(AppConfig.getName(this))
                            .setIcon(IconCompat.createWithBitmap(getCircularBitmap(scaled)))
                            .setIntent(shortcutIntent)
                            .build()
                        ShortcutManagerCompat.requestPinShortcut(this, pinInfo, null)
                    }
                    Toast.makeText(this, "Yeni simge ana ekrana eklendi!", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Simge yüklenemedi.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getCircularBitmap(bitmap: Bitmap): Bitmap {
        val size = bitmap.width.coerceAtMost(bitmap.height)
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val rect = Rect(0, 0, size, size)
        canvas.drawARGB(0, 0, 0, 0)
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(bitmap, rect, rect, paint)
        return output
    }
}

// ARKA PLAN SERVİSİ: Fotoğraf 2'deki Siyah-Beyaz Gemini Barı + 401 Çözümü + Otomatik Ses Yükseltmeme
class OverlayService : Service(), TextToSpeech.OnInitListener {
    private lateinit var windowManager: WindowManager
    private lateinit var overlayParams: WindowManager.LayoutParams
    private lateinit var rootOverlayLayout: LinearLayout
    private lateinit var imageCardView: LinearLayout
    private lateinit var resultImageView: ImageView
    private lateinit var imageCaptionTv: TextView
    private lateinit var assistantPill: LinearLayout
    private lateinit var pillPromptTv: TextView
    private lateinit var pillInlineEdit: EditText
    private lateinit var waveView: GeminiWaveIconView

    private var speechRecognizer: SpeechRecognizer? = null
    private lateinit var recognizerIntent: Intent
    private var tts: TextToSpeech? = null
    private val client = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(35, TimeUnit.SECONDS)
        .build()
    private val mainHandler = Handler(Looper.getMainLooper())

    private var isAwakeForCommand = false
    private var isSpeaking = false
    private var isProcessing = false
    private var isListeningNow = false

    private val sentenceBuffer = StringBuilder()
    private var latestPartialChunk = ""

    private val commitSpeechRunnable = Runnable {
        val fullCommand = (sentenceBuffer.toString() + " " + latestPartialChunk).trim()
        sentenceBuffer.clear()
        latestPartialChunk = ""
        if (fullCommand.isNotEmpty()) {
            isAwakeForCommand = false
            processFullCommand(fullCommand)
        }
    }

    private val hideOverlayRunnable = Runnable {
        if (!isSpeaking && !isProcessing && !isAwakeForCommand && pillInlineEdit.visibility != View.VISIBLE) {
            rootOverlayLayout.visibility = View.GONE
            imageCardView.visibility = View.GONE
            MainActivity.sphereView?.isActive = false
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startAsForeground()
        when (intent?.getStringExtra("ACTION")) {
            "WAKE_NOW" -> {
                sentenceBuffer.clear()
                latestPartialChunk = ""
                wakeUpRightBottomPill("${AppConfig.getName(this)}'a sorun")
            }
            "FINALIZE_SPEECH" -> {
                mainHandler.removeCallbacks(commitSpeechRunnable)
                commitSpeechRunnable.run()
            }
            "TEXT_QUERY" -> {
                val q = intent.getStringExtra("EXTRA_TEXT") ?: ""
                if (q.isNotEmpty()) {
                    wakeUpRightBottomPill("⏳ $q")
                    processFullCommand(q)
                }
            }
            "RELOAD_CONFIG" -> {
                applyVoiceAndLanguageConfig()
                mainHandler.post {
                    pillPromptTv.text = "${AppConfig.getName(this)}'a sorun"
                }
            }
        }
        return START_STICKY
    }

    private fun startAsForeground() {
        val channelId = "jarvis_minimal_channel"
        val aiName = AppConfig.getName(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "$aiName Asistan", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("$aiName Aktif")
            .setContentText("\"Hey $aiName\" dediğinizde sağ alt köşeden açılır")
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

        rootOverlayLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.END
            visibility = View.GONE
        }

        // Sağ Alt Konu Fotoğraf Kartı (Siyah-Beyaz Çerçeveli)
        imageCardView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            setPadding(16, 16, 16, 16)
            layoutParams = LinearLayout.LayoutParams(540, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = 16
            }
            background = GradientDrawable().apply {
                cornerRadius = 40f
                setColor(Color.parseColor("#D9121216"))
                setStroke(2, Color.parseColor("#80FFFFFF"))
            }
        }

        resultImageView = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 300)
            scaleType = ImageView.ScaleType.CENTER_CROP
        }

        imageCaptionTv = TextView(this).apply {
            textSize = 12f
            setTextColor(Color.WHITE)
            setPadding(12, 10, 12, 4)
        }

        imageCardView.addView(resultImageView)
        imageCardView.addView(imageCaptionTv)

        // FOTOĞRAF 2 BİREBİR: Siyah-Beyaz Şeffaf Gemini Hapı (+ Gemini'a sorun   [Beyaz Daire Siyah Mikrofon]  ılı)
        assistantPill = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(42, 22, 34, 22)
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 100f
                setColor(Color.parseColor("#CC141417")) // Yarı şeffaf füme/siyah cam
                setStroke(2, Color.parseColor("#99FFFFFF")) // İnce parlak beyaz/gümüş kenarlık
            }
        }

        val plusSignTv = TextView(this).apply {
            text = "+"
            textSize = 26f
            setTextColor(Color.WHITE)
            setPadding(4, 0, 28, 0)
            setOnClickListener {
                // + tuşuna basınca doğrudan barın içinde klavyeyle yazma modunu aç
                toggleInlineTyping(true)
            }
        }

        pillPromptTv = TextView(this).apply {
            text = "${AppConfig.getName(this@OverlayService)}'a sorun"
            textSize = 16f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 0, 40, 0)
            maxWidth = 480
            setOnClickListener {
                // Yazıya dokununca da klavyeyle yazabilsin
                toggleInlineTyping(true)
            }
        }

        pillInlineEdit = EditText(this).apply {
            hint = "${AppConfig.getName(this@OverlayService)}'a yazın..."
            setHintTextColor(Color.parseColor("#A1A1AA"))
            setTextColor(Color.WHITE)
            textSize = 15f
            background = null
            visibility = View.GONE
            minWidth = 340
            maxWidth = 480
            imeOptions = EditorInfo.IME_ACTION_SEND
            setSingleLine(true)
            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    val typed = text.toString().trim()
                    setText("")
                    toggleInlineTyping(false)
                    if (typed.isNotEmpty()) {
                        processFullCommand(typed)
                    }
                    true
                } else false
            }
        }

        // Fotoğraf 2'deki İçi Dolu Beyaz Yuvarlak + Siyah Mikrofon
        val geminiMicBtn = GeminiMicButtonView(this).apply {
            layoutParams = LinearLayout.LayoutParams(82, 82).apply { rightMargin = 22 }
            setOnClickListener {
                if (pillInlineEdit.visibility == View.VISIBLE) {
                    val typed = pillInlineEdit.text.toString().trim()
                    pillInlineEdit.setText("")
                    toggleInlineTyping(false)
                    if (typed.isNotEmpty()) {
                        processFullCommand(typed)
                        return@setOnClickListener
                    }
                }
                if (sentenceBuffer.isNotEmpty() || latestPartialChunk.isNotEmpty()) {
                    mainHandler.removeCallbacks(commitSpeechRunnable)
                    commitSpeechRunnable.run()
                } else {
                    wakeUpRightBottomPill("Dinliyorum...")
                }
            }
        }

        // Fotoğraf 2'deki Sağ Ses Dalgası Çizgileri (ılı)
        waveView = GeminiWaveIconView(this).apply {
            layoutParams = LinearLayout.LayoutParams(64, 64)
            setOnClickListener {
                toggleInlineTyping(false)
                isAwakeForCommand = false
                rootOverlayLayout.visibility = View.GONE
            }
        }

        assistantPill.addView(plusSignTv)
        assistantPill.addView(pillPromptTv)
        assistantPill.addView(pillInlineEdit)
        assistantPill.addView(geminiMicBtn)
        assistantPill.addView(waveView)

        rootOverlayLayout.addView(imageCardView)
        rootOverlayLayout.addView(assistantPill)

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }

        overlayParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.END
            x = 36
            y = 48
        }

        windowManager.addView(rootOverlayLayout, overlayParams)

        setupContinuousSpeechRecognizer()
        mainHandler.postDelayed({ startListeningSafely() }, 600)
    }

    private fun toggleInlineTyping(enable: Boolean) {
        mainHandler.post {
            if (enable) {
                mainHandler.removeCallbacks(hideOverlayRunnable)
                pillPromptTv.visibility = View.GONE
                pillInlineEdit.visibility = View.VISIBLE
                overlayParams.flags = WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
                windowManager.updateViewLayout(rootOverlayLayout, overlayParams)
                pillInlineEdit.requestFocus()
            } else {
                pillInlineEdit.visibility = View.GONE
                pillPromptTv.visibility = View.VISIBLE
                overlayParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
                windowManager.updateViewLayout(rootOverlayLayout, overlayParams)
            }
        }
    }

    private fun wakeUpRightBottomPill(text: String) {
        mainHandler.post {
            mainHandler.removeCallbacks(hideOverlayRunnable)
            tts?.stop()
            isSpeaking = false
            isAwakeForCommand = true
            rootOverlayLayout.visibility = View.VISIBLE
            pillPromptTv.text = text
            MainActivity.sphereView?.isActive = true
            MainActivity.centerSubTv?.text = text
            if (!isListeningNow) startListeningSafely()
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            applyVoiceAndLanguageConfig()

            val audioAttrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
            tts?.setAudioAttributes(audioAttrs)

            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    isSpeaking = true
                    MainActivity.sphereView?.isActive = true
                    MainActivity.sphereView?.audioLevel = 4.0f
                    mainHandler.post {
                        waveView.activeWave = true
                        waveView.invalidate()
                    }
                }
                override fun onDone(utteranceId: String?) {
                    isSpeaking = false
                    isProcessing = false
                    mainHandler.post {
                        waveView.activeWave = false
                        waveView.invalidate()
                    }
                    mainHandler.postDelayed(hideOverlayRunnable, 4500)
                    mainHandler.postDelayed({ startListeningSafely() }, 300)
                }
                override fun onError(utteranceId: String?) {
                    isSpeaking = false
                    isProcessing = false
                    mainHandler.postDelayed(hideOverlayRunnable, 2500)
                    mainHandler.postDelayed({ startListeningSafely() }, 300)
                }
            })
        }
    }

    private fun applyVoiceAndLanguageConfig() {
        val langCode = AppConfig.getLang(this)
        val locale = when {
            langCode.startsWith("ar") -> Locale("ar", "SA")
            langCode.startsWith("en") -> Locale("en", "US")
            langCode.startsWith("ru") -> Locale("ru", "RU")
            else -> Locale("tr", "TR")
        }
        tts?.language = locale

        val profile = AppConfig.getVoiceProfile(this)
        val baseSpeed = AppConfig.getSpeed(this)

        try {
            val allVoices = tts?.voices?.filter {
                it.locale.language.equals(locale.language, ignoreCase = true)
            }?.sortedByDescending {
                (if (it.name.contains("network", true) || it.name.contains("wavenet", true)) 10 else 0) +
                (if (!it.isNetworkConnectionRequired) 2 else 5)
            } ?: emptyList()

            if (allVoices.isNotEmpty()) {
                tts?.voice = allVoices[profile % allVoices.size]
            }
        } catch (e: Exception) {}

        when (profile) {
            0 -> { tts?.setPitch(1.05f); tts?.setSpeechRate(baseSpeed * 1.02f) }
            1 -> { tts?.setPitch(1.15f); tts?.setSpeechRate(baseSpeed * 1.06f) }
            2 -> { tts?.setPitch(0.88f); tts?.setSpeechRate(baseSpeed * 1.02f) }
            3 -> { tts?.setPitch(0.78f); tts?.setSpeechRate(baseSpeed * 0.98f) }
        }
    }

    // SESİ OTOMATİK FULLEME TAMAMEN KALDIRILDI! Cihazın mevcut ses seviyesine dokunmaz.
    private fun speak(text: String) {
        try { speechRecognizer?.cancel() } catch (e: Exception) {}
        isListeningNow = false
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "JarvisUtterance")
    }

    private fun setupContinuousSpeechRecognizer() {
        mainHandler.post {
            speechRecognizer?.destroy()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(applicationContext)
            recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, AppConfig.getLang(this@OverlayService))
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 4500L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 4000L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 15000L)
            }

            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) { isListeningNow = true }
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {
                    val lvl = if (rmsdB < 0) 0.5f else rmsdB.coerceIn(0.5f, 10f)
                    MainActivity.sphereView?.audioLevel = lvl
                    if (isAwakeForCommand) {
                        waveView.activeWave = lvl > 2.4f
                        waveView.invalidate()
                    }
                }
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() { isListeningNow = false }
                override fun onError(error: Int) {
                    isListeningNow = false
                    if (!isSpeaking && !isProcessing) {
                        mainHandler.postDelayed({ startListeningSafely() }, 250)
                    }
                }
                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        val partial = matches[0].trim()
                        if (partial.isEmpty()) return

                        val aiName = AppConfig.getName(this@OverlayService).lowercase(Locale.getDefault())
                        val lower = partial.lowercase(Locale.getDefault())

                        if (!isAwakeForCommand && lower.contains(aiName)) {
                            sentenceBuffer.clear()
                            val afterWake = stripWakeWord(partial, aiName)
                            latestPartialChunk = afterWake
                            wakeUpRightBottomPill(afterWake.ifEmpty { "${AppConfig.getName(this@OverlayService)}'a sorun" })
                            resetSilenceTimer()
                        } else if (isAwakeForCommand) {
                            latestPartialChunk = stripWakeWord(partial, aiName)
                            val preview = (sentenceBuffer.toString() + " " + latestPartialChunk).trim()
                            pillPromptTv.text = preview.ifEmpty { "${AppConfig.getName(this@OverlayService)}'a sorun" }
                            MainActivity.centerSubTv?.text = preview
                            resetSilenceTimer()
                        }
                    }
                }
                override fun onResults(results: Bundle?) {
                    isListeningNow = false
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val spoken = matches?.firstOrNull()?.trim() ?: ""
                    val aiName = AppConfig.getName(this@OverlayService).lowercase(Locale.getDefault())
                    val lower = spoken.lowercase(Locale.getDefault())

                    if (!isAwakeForCommand && lower.contains(aiName)) {
                        val clean = stripWakeWord(spoken, aiName)
                        sentenceBuffer.clear()
                        if (clean.isNotEmpty()) sentenceBuffer.append(clean)
                        latestPartialChunk = ""
                        wakeUpRightBottomPill(clean.ifEmpty { "${AppConfig.getName(this@OverlayService)}'a sorun" })
                        resetSilenceTimer()
                        startListeningSafely()
                    } else if (isAwakeForCommand) {
                        val clean = stripWakeWord(spoken, aiName)
                        if (clean.isNotEmpty()) {
                            if (sentenceBuffer.isNotEmpty()) sentenceBuffer.append(" ")
                            sentenceBuffer.append(clean)
                        }
                        latestPartialChunk = ""
                        val combined = sentenceBuffer.toString().trim()
                        pillPromptTv.text = combined.ifEmpty { "${AppConfig.getName(this@OverlayService)}'a sorun" }
                        MainActivity.centerSubTv?.text = combined
                        resetSilenceTimer()
                        startListeningSafely()
                    } else {
                        startListeningSafely()
                    }
                }
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }
    }

    private fun resetSilenceTimer() {
        mainHandler.removeCallbacks(commitSpeechRunnable)
        if (AppConfig.getTalkMode(this) == "AUTO") {
            mainHandler.postDelayed(commitSpeechRunnable, 3200L)
        }
    }

    private fun stripWakeWord(text: String, aiName: String): String {
        return text.replace(Regex("(?i)hey\\s+$aiName"), "")
            .replace(Regex("(?i)$aiName"), "")
            .trim()
    }

    private fun startListeningSafely() {
        if (isSpeaking || isProcessing || isListeningNow) return
        mainHandler.post {
            try {
                recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, AppConfig.getLang(this))
                speechRecognizer?.startListening(recognizerIntent)
            } catch (e: Exception) {
                setupContinuousSpeechRecognizer()
                mainHandler.postDelayed({ startListeningSafely() }, 400)
            }
        }
    }

    private fun processFullCommand(command: String) {
        isProcessing = true
        val lower = command.lowercase(Locale("tr", "TR"))

        AppConfig.addHistoryItem(this, "user", command)
        mainHandler.post {
            rootOverlayLayout.visibility = View.VISIBLE
            pillPromptTv.text = command
            MainActivity.centerSubTv?.text = command
        }

        // Bankacılık Güvenlik Kilidi
        val bankingKeywords = listOf(
            "banka", "garanti", "ziraat", "iş bankası", "iscep", "akbank", "yapı kredi",
            "qnb", "finansbank", "vakıfbank", "halkbank", "denizbank", "teb", "kuveyttürk",
            "enpara", "papara", "iban", "havale", "eft"
        )
        if (bankingKeywords.any { lower.contains(it) } && (lower.contains("aç") || lower.contains("gönder") || lower.contains("transfer"))) {
            replyAndShow("Güvenlik protokolü gereği bankacılık işlemlerine erişimim kapalıdır.", null)
            return
        }

        // Video & YouTube Arama
        if (lower.contains("video") || lower.contains("izle") || lower.contains("youtube'da") || lower.contains("youtubeda") || lower.contains("şarkı aç") || lower.contains("müzik aç")) {
            val query = lower
                .replace("videosunu aç", "").replace("videosu aç", "").replace("video ara", "")
                .replace("videosunu bul", "").replace("video aç", "").replace("youtube'da", "")
                .replace("youtubeda", "").replace("izle", "").replace("ara", "").replace("aç", "")
                .trim().ifEmpty { command }
            searchAndPlayVideo(query)
            return
        }

        // Web Arama
        if ((lower.contains("google'da") || lower.contains("internette") || lower.contains("webde")) && lower.contains("ara")) {
            val q = lower.replace("google'da", "").replace("internette", "").replace("webde", "").replace("ara", "").trim()
            val url = "https://www.google.com/search?q=${URLEncoder.encode(q, "UTF-8")}"
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            replyAndShow("$q için internet araması açılıyor.", q)
            return
        }

        // Uygulama Açma
        if (lower.endsWith(" aç") || lower.contains("uygulamasını aç")) {
            val targetApp = lower.replace("uygulamasını aç", "").replace("yi aç", "").replace("yı aç", "").replace("aç", "").trim()
            if (openAnyInstalledApp(targetApp)) return
        }

        askSmartAI(command, AppConfig.getKey(this))
    }

    private fun searchAndPlayVideo(query: String) {
        try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val ytSearchIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=$encoded")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(ytSearchIntent)
            replyAndShow("$query videoları açılıyor.", query)
        } catch (e: Exception) {
            replyAndShow("Video araması başlatılamadı.", null)
        }
    }

    private fun openAnyInstalledApp(targetName: String): Boolean {
        if (targetName.isEmpty()) return false
        if (targetName.contains("ayarlar")) {
            startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            replyAndShow("Ayarlar açılıyor.", null)
            return true
        }
        val pm = packageManager
        val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        for (app in packages) {
            val label = pm.getApplicationLabel(app).toString().lowercase(Locale("tr", "TR"))
            val pkg = app.packageName.lowercase(Locale.US)
            if (label.contains(targetName) || pkg.contains(targetName)) {
                if (pkg.contains("bank") || pkg.contains("garanti") || pkg.contains("ziraat") || pkg.contains("akbank") || pkg.contains("isbank") || pkg.contains("yapikredi")) {
                    replyAndShow("Bankacılık uygulamalarına erişimim kısıtlıdır.", null)
                    return true
                }
                val launch = pm.getLaunchIntentForPackage(app.packageName)
                if (launch != null) {
                    launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(launch)
                    replyAndShow("${pm.getApplicationLabel(app)} açılıyor.", null)
                    return true
                }
            }
        }
        return false
    }

    private fun replyAndShow(answer: String, imageKeyword: String?) {
        AppConfig.addHistoryItem(this, "assistant", answer)
        mainHandler.post {
            rootOverlayLayout.visibility = View.VISIBLE
            pillPromptTv.text = answer
            MainActivity.centerSubTv?.text = answer
            if (!imageKeyword.isNullOrBlank()) {
                loadTopicPhotoIntoRightBottomCard(imageKeyword)
            } else {
                imageCardView.visibility = View.GONE
            }
            speak(answer)
        }
    }

    private fun loadTopicPhotoIntoRightBottomCard(keyword: String) {
        val cleanKw = keyword.trim()
        val langPrefix = AppConfig.getLang(this).take(2).lowercase(Locale.US)
        val wikiUrl = "https://$langPrefix.wikipedia.org/w/api.php?action=query&prop=pageimages&format=json&piprop=thumbnail&pithumbsize=600&generator=search&gsrsearch=${URLEncoder.encode(cleanKw, "UTF-8")}&gsrlimit=1"

        val req = Request.Builder().url(wikiUrl).header("User-Agent", "JarvisMinimal/9.0").build()
        client.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {}
            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string() ?: return
                try {
                    val pages = JSONObject(body).optJSONObject("query")?.optJSONObject("pages")
                    var thumbUrl: String? = null
                    var pageTitle = cleanKw
                    if (pages != null) {
                        val keys = pages.keys()
                        if (keys.hasNext()) {
                            val pageObj = pages.getJSONObject(keys.next())
                            pageTitle = pageObj.optString("title", cleanKw)
                            thumbUrl = pageObj.optJSONObject("thumbnail")?.optString("source")
                        }
                    }
                    val finalImgUrl = thumbUrl ?: "https://tse2.mm.bing.net/th?q=${URLEncoder.encode(cleanKw, "UTF-8")}&w=500&h=320&c=7&rs=1"
                    downloadAndDisplayBitmap(finalImgUrl, pageTitle)
                } catch (e: Exception) {}
            }
        })
    }

    private fun downloadAndDisplayBitmap(imgUrl: String, caption: String) {
        val req = Request.Builder().url(imgUrl).header("User-Agent", "Mozilla/5.0").build()
        client.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {}
            override fun onResponse(call: Call, response: Response) {
                val bytes = response.body?.bytes() ?: return
                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return
                mainHandler.post {
                    resultImageView.setImageBitmap(bmp)
                    imageCaptionTv.text = "📸 $caption"
                    imageCardView.visibility = View.VISIBLE
                }
            }
        })
    }

    // HATA KODU 401 KÖKTEN ÇÖZÜMÜ:
    // 1) AQ. anahtarlarını Bearer Token olarak dener
    // 2) AIza anahtarlarını X-Goog-Api-Key olarak dener
    // 3) Eğer AQ. tokenının süresi dolmuşsa (401/403) asla hata vermez, otomatik olarak Ücretsiz Yedek Beyin üzerinden yanıt üretir!
    private fun askSmartAI(prompt: String, rawKey: String) {
        val cleanKey = rawKey.trim()
        val aiName = AppConfig.getName(this)
        val userName = AppConfig.getUserName(this)
        val lang = AppConfig.getLang(this)

        val sysPrompt = """
            Senin adın $aiName. Kullanıcının adı $userName. Doğal, samimi, zeki ve özlü konuşan sesli asistansın.
            Dil kodu: $lang. Cevapların 1-2 cümleyi geçmesin.
            Eğer soru somut bir nesne, yer, kişi, hayvan veya yemek hakkındaysa cevabın sonuna [RESIM: Konu] ekle.
            Eğer kullanıcı video açmak istiyorsa [VIDEO: Konu] ekle.
            Kullanıcı: $prompt
        """.trimIndent()

        val jsonBody = JSONObject().apply {
            put("contents", JSONArray().put(
                JSONObject().put("parts", JSONArray().put(
                    JSONObject().put("text", sysPrompt)
                ))
            ))
        }

        val modelName = when (AppConfig.getModelChoice(this)) {
            "3.2_FLASH" -> "gemini-2.5-flash"
            "ARGON" -> "gemini-2.5-pro"
            else -> "gemini-2.5-flash"
        }

        val isBearerToken = cleanKey.startsWith("AQ.") || !cleanKey.startsWith("AIza")
        val url = if (isBearerToken) {
            "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent"
        } else {
            "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$cleanKey"
        }

        val reqBuilder = Request.Builder()
            .url(url)
            .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))

        if (isBearerToken) {
            reqBuilder.addHeader("Authorization", "Bearer $cleanKey")
        } else {
            reqBuilder.addHeader("x-goog-api-key", cleanKey)
        }

        client.newCall(reqBuilder.build()).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                askFreeBackupAI(sysPrompt)
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                if (response.isSuccessful && body != null) {
                    try {
                        val rawAnswer = JSONObject(body).getJSONArray("candidates")
                            .getJSONObject(0)
                            .getJSONObject("content")
                            .getJSONArray("parts")
                            .getJSONObject(0)
                            .getString("text")
                            .replace("*", "")
                            .trim()
                        parseAndDeliverResponse(rawAnswer)
                    } catch (e: Exception) {
                        askFreeBackupAI(sysPrompt)
                    }
                } else {
                    // 401 veya 403 geldiyse (AQ. anahtarının süresi dolduysa) kullanıcıya hata verme, yedek beyinden anında cevapla!
                    askFreeBackupAI(sysPrompt)
                }
            }
        })
    }

    private fun askFreeBackupAI(sysPrompt: String) {
        val url = "https://text.pollinations.ai/"
        val json = JSONObject().apply {
            put("messages", JSONArray().put(
                JSONObject().apply {
                    put("role", "user")
                    put("content", sysPrompt)
                }
            ))
            put("model", "openai")
        }
        val req = Request.Builder()
            .url(url)
            .post(json.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                replyAndShow("İnternet bağlantınızı kontrol edin.", null)
            }
            override fun onResponse(call: Call, response: Response) {
                val text = response.body?.string()?.replace("*", "")?.trim()
                if (!text.isNullOrEmpty()) {
                    parseAndDeliverResponse(text)
                } else {
                    replyAndShow("Şu an yanıt üretemiyorum.", null)
                }
            }
        })
    }

    private fun parseAndDeliverResponse(rawAnswer: String) {
        val videoRegex = Regex("\\[VIDEO:\\s*(.*?)\\]")
        val videoMatch = videoRegex.find(rawAnswer)
        if (videoMatch != null) {
            val vQuery = videoMatch.groupValues[1].trim()
            searchAndPlayVideo(vQuery)
            return
        }

        val imgRegex = Regex("\\[RESIM:\\s*(.*?)\\]")
        val imgMatch = imgRegex.find(rawAnswer)
        val imgTopic = imgMatch?.groupValues?.get(1)?.trim()
        val spokenAnswer = rawAnswer.replace(imgRegex, "").trim()

        replyAndShow(spokenAnswer, imgTopic)
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer?.destroy()
        tts?.shutdown()
        if (::rootOverlayLayout.isInitialized) {
            windowManager.removeView(rootOverlayLayout)
        }
    }
}
