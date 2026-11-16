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
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
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
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
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
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.cos
import kotlin.math.sin

object AppConfig {
    const val PREFS = "AsistanMasterPrefs"
    const val DEFAULT_KEY = "AQ.Ab8RN6KZYPqSekpNWFEnuG42klSAK_TKxGeFNc1YbcduzUm0kA"

    fun isSetupDone(ctx: Context): Boolean =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean("SETUP_DONE", false)

    fun setSetupDone(ctx: Context, done: Boolean) =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean("SETUP_DONE", done).apply()

    fun getName(ctx: Context): String =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("AI_NAME", "")?.takeIf { it.isNotBlank() } ?: "Jarvis"

    fun setName(ctx: Context, name: String) =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString("AI_NAME", name.trim()).apply()

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
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt("VOICE_PROFILE", 2) // 0: Kız 1, 1: Kız 2, 2: Erkek 1, 3: Erkek 2

    fun setVoiceProfile(ctx: Context, idx: Int) =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putInt("VOICE_PROFILE", idx).apply()

    fun getThemeColor(ctx: Context): Int =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt("THEME_COLOR", Color.parseColor("#FBBF24"))

    fun setThemeColor(ctx: Context, color: Int) =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putInt("THEME_COLOR", color).apply()

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

    fun getCustomIconBitmap(ctx: Context): Bitmap? {
        val file = File(ctx.filesDir, "custom_app_icon.png")
        return if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
    }
}

// Fotoğraf 2'deki Akıcı Organik Küre (Fluid Orb) + Jarvis Halkaları Görselleştiricisi
class FluidOrbView(context: Context) : View(context) {
    var audioLevel = 0f
    var isSpeakingOrListening = false
    var themeColor = Color.parseColor("#FBBF24")
    private var phase = 0f
    private val orbPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = android.graphics.Path()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val baseR = (width.coerceAtMost(height) * 0.30f).coerceAtLeast(60f)

        val speed = if (isSpeakingOrListening) 0.09f else 0.025f
        phase += speed

        val dynamicAmp = if (isSpeakingOrListening) (audioLevel * 5.5f).coerceIn(8f, 48f) else 6f

        val r = Color.red(themeColor)
        val g = Color.green(themeColor)
        val b = Color.blue(themeColor)

        // Dış Yumuşak Işıma
        val glowR = baseR * 1.45f + dynamicAmp
        glowPaint.shader = RadialGradient(
            cx, cy, glowR,
            intArrayOf(
                Color.argb(110, r, g, b),
                Color.argb(35, r, g, b),
                Color.TRANSPARENT
            ),
            floatArrayOf(0f, 0.6f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy, glowR, glowPaint)

        // Organik Dalgalanan Küre (Fotoğraf 2'deki tasarım)
        path.reset()
        val steps = 72
        for (i in 0..steps) {
            val angle = (i.toDouble() / steps) * Math.PI * 2.0
            val wave1 = sin(angle * 3 + phase) * (dynamicAmp * 0.55f)
            val wave2 = cos(angle * 2 - phase * 1.3f) * (dynamicAmp * 0.45f)
            val currR = baseR + wave1.toFloat() + wave2.toFloat()
            val x = cx + currR * cos(angle).toFloat()
            val y = cy + currR * sin(angle).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()

        val lightColor = Color.argb(
            255,
            (r + 65).coerceAtMost(255),
            (g + 65).coerceAtMost(255),
            (b + 65).coerceAtMost(255)
        )

        orbPaint.shader = RadialGradient(
            cx - baseR * 0.25f,
            cy - baseR * 0.25f,
            baseR * 1.3f,
            intArrayOf(lightColor, themeColor, Color.argb(230, (r * 0.7).toInt(), (g * 0.7).toInt(), (b * 0.7).toInt())),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawPath(path, orbPaint)

        postInvalidateOnAnimation()
    }
}

class MainActivity : AppCompatActivity() {
    companion object {
        var orbView: FluidOrbView? = null
        var statusLabel: TextView? = null
        var topMoodLabel: TextView? = null
        var topNameLabel: TextView? = null
        private const val REQ_PICK_ICON = 701
    }

    private lateinit var rootContainer: FrameLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 1)
        }

        rootContainer = FrameLayout(this).apply {
            setBackgroundColor(Color.parseColor("#050508"))
        }
        setContentView(rootContainer)

        if (!AppConfig.isSetupDone(this)) {
            showOnboardingScreen()
        } else {
            showMainOrbScreen()
            ensureOverlayServiceRunning()
        }
    }

    private fun ensureOverlayServiceRunning() {
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Sağ alt asistan barı için Ekran Üstü İznini onaylayın", Toast.LENGTH_LONG).show()
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

    // FOTOĞRAF 3: İlk Kurulum, İsim Belirleme ve 5 Dil Seçim Ekranı
    private fun showOnboardingScreen() {
        rootContainer.removeAllViews()
        val scroll = ScrollView(this).apply { isFillViewport = true }
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(60, 90, 60, 70)
            setBackgroundColor(Color.parseColor("#07060E"))
        }

        // Üst Ses Dalgası Çizgileri
        val waveArt = TextView(this).apply {
            text = "ılıı|||||ılıı"
            textSize = 30f
            setTextColor(Color.parseColor("#D946EF"))
            gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 10, 0, 40)
        }

        // Orta Yuvarlak Ses İkonu
        val centerCircle = TextView(this).apply {
            text = "ılı"
            textSize = 28f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(220, 220).apply {
                gravity = Gravity.CENTER
                bottomMargin = 55
            }
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.parseColor("#EC4899"), Color.parseColor("#8B5CF6"))
            ).apply {
                shape = GradientDrawable.OVAL
            }
        }

        val currentName = AppConfig.getName(this)
        val greetingTitle = TextView(this).apply {
            text = "Merhaba, bir ad seçin"
            textSize = 26f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }

        val subDesc = TextView(this).apply {
            text = "Sesinizle sohbet edin — akıllı, anlayan, her zaman yanınızda."
            textSize = 14f
            setTextColor(Color.parseColor("#9CA3AF"))
            gravity = Gravity.CENTER
            setPadding(20, 16, 20, 35)
        }

        // Asistan Adı Seçme Kutusu (Niko zorunluluğu yok, ne yazılırsa o olur)
        val nameInput = EditText(this).apply {
            hint = "Asistanın Adı (Örn: Jarvis, Ahmet, Niko...)"
            setText(if (AppConfig.isSetupDone(this@MainActivity)) currentName else "")
            setHintTextColor(Color.parseColor("#6B7280"))
            setTextColor(Color.WHITE)
            textSize = 15f
            gravity = Gravity.CENTER
            setPadding(40, 32, 40, 32)
            background = GradientDrawable().apply {
                cornerRadius = 40f
                setColor(Color.parseColor("#181824"))
                setStroke(2, Color.parseColor("#A855F7"))
            }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = 40
            }
        }

        // 5 Dil Seçenek Hapları (Türkçe, English, العربية, Русский, کوردی)
        val langs = listOf(
            Triple("🇹🇷 Türkçe", "tr-TR", "Merhaba, ben"),
            Triple("🇬🇧 English", "en-US", "Hello, I am"),
            Triple("🇸🇦 العربية", "ar-SA", "مرحباً، أنا"),
            Triple("🇷🇺 Русский", "ru-RU", "Привет, я"),
            Triple("☀️ کوردی", "ckb-IQ", "سڵاو، من")
        )

        var selectedLang = AppConfig.getLang(this)
        val pillButtons = mutableListOf< Pair<String, TextView> >()

        fun refreshLangPills() {
            for ((code, tv) in pillButtons) {
                val active = (code == selectedLang)
                tv.background = GradientDrawable().apply {
                    cornerRadius = 50f
                    setColor(if (active) Color.parseColor("#2E1065") else Color.parseColor("#14141F"))
                    setStroke(if (active) 3 else 2, if (active) Color.parseColor("#D946EF") else Color.parseColor("#27273A"))
                }
                tv.setTextColor(if (active) Color.WHITE else Color.parseColor("#D1D5DB"))
            }
        }

        val row1 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        val row2 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, 20, 0, 50)
        }

        langs.forEachIndexed { idx, (label, code, prefix) ->
            val chip = TextView(this).apply {
                text = label
                textSize = 13f
                setPadding(36, 22, 36, 22)
                val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(12, 6, 12, 6)
                }
                layoutParams = lp
                setOnClickListener {
                    selectedLang = code
                    AppConfig.setLang(this@MainActivity, code)
                    val typed = nameInput.text.toString().trim().ifEmpty { "Jarvis" }
                    greetingTitle.text = "$prefix $typed"
                    refreshLangPills()
                }
            }
            pillButtons.add(code to chip)
            if (idx < 3) row1.addView(chip) else row2.addView(chip)
        }
        refreshLangPills()

        val continueBtn = Button(this).apply {
            text = "Devam"
            textSize = 16f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(40, 38, 40, 38)
            background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(Color.parseColor("#F43F5E"), Color.parseColor("#D946EF"), Color.parseColor("#8B5CF6"))
            ).apply {
                cornerRadius = 60f
            }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = 30
            }
            setOnClickListener {
                val chosenName = nameInput.text.toString().trim().ifEmpty { "Jarvis" }
                AppConfig.setName(this@MainActivity, chosenName)
                AppConfig.setLang(this@MainActivity, selectedLang)
                AppConfig.setSetupDone(this@MainActivity, true)
                showMainOrbScreen()
                ensureOverlayServiceRunning()
                startAssistantService("RELOAD_CONFIG")
            }
        }

        col.addView(waveArt)
        col.addView(centerCircle)
        col.addView(greetingTitle)
        col.addView(subDesc)
        col.addView(nameInput)
        col.addView(row1)
        col.addView(row2)
        col.addView(continueBtn)

        scroll.addView(col)
        rootContainer.addView(scroll)
    }

    // FOTOĞRAF 2: Ana Küre (Orb) Ekranı, Sağ Üst API/Model Tuşu, Sağ Alt Menü ve Sohbet Girişi
    private fun showMainOrbScreen() {
        rootContainer.removeAllViews()
        val themeColor = AppConfig.getThemeColor(this)
        val aiName = AppConfig.getName(this)

        val mainLayout = FrameLayout(this).apply {
            setBackgroundColor(Color.parseColor("#040406"))
        }

        // ÜST BAR: Sol (● İsim  Şu an: Sakin) | Sağ (🔑 Anahtar, ✨ Model/Renk)
        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(45, 55, 45, 20)
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.TOP)
        }

        // Özel uygulama simgesi varsa sol üstte göster
        val customBmp = AppConfig.getCustomIconBitmap(this)
        val statusDot = ImageView(this).apply {
            val size = 56
            layoutParams = LinearLayout.LayoutParams(size, size).apply { rightMargin = 18 }
            if (customBmp != null) {
                setImageBitmap(getCircularBitmap(customBmp))
            } else {
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(themeColor)
                }
            }
        }

        val nameLabel = TextView(this).apply {
            text = aiName
            textSize = 20f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            setOnClickListener { showRenameDialog() }
        }
        topNameLabel = nameLabel

        val moodLabel = TextView(this).apply {
            text = "  Şu an: Sakin (${AppConfig.getModelChoice(this@MainActivity)})"
            textSize = 13f
            setTextColor(Color.parseColor("#9CA3AF"))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        topMoodLabel = moodLabel

        // İSTEK 7: Sağ Üst Köşe API Anahtarı Değiştirme Butonu
        val keyBtn = TextView(this).apply {
            text = "🔑"
            textSize = 18f
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(95, 95).apply { rightMargin = 18 }
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#1F2937"))
                setStroke(2, themeColor)
            }
            setOnClickListener { showApiKeyDialog() }
        }

        // Sağ Üst Parıltı Butonu (Fotoğraf 2'deki mor/renkli ✨ yuvarlak tuş: Model & Tema Seçici)
        val sparkleBtn = TextView(this).apply {
            text = "✨"
            textSize = 18f
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(95, 95)
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.parseColor("#A855F7"), Color.parseColor("#EC4899"))
            ).apply {
                shape = GradientDrawable.OVAL
            }
            setOnClickListener { showModelAndColorDialog() }
        }

        topBar.addView(statusDot)
        topBar.addView(nameLabel)
        topBar.addView(moodLabel)
        topBar.addView(keyBtn)
        topBar.addView(sparkleBtn)

        // ORTA KÜRE (Fluid Orb)
        val centerBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        }

        val fluidOrb = FluidOrbView(this).apply {
            this.themeColor = themeColor
            layoutParams = LinearLayout.LayoutParams(680, 680)
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
        orbView = fluidOrb

        val statusTv = TextView(this).apply {
            text = "Dinliyorum... (\"Hey $aiName\" de veya küreye dokun)"
            textSize = 15f
            setTextColor(Color.parseColor("#E5E7EB"))
            gravity = Gravity.CENTER
            setPadding(40, 40, 40, 20)
        }
        statusLabel = statusTv

        centerBox.addView(fluidOrb)
        centerBox.addView(statusTv)

        // ALT KONTROL BARI (Fotoğraf 2: Kulak İkonu, Gönder İkonu (➤), 3 Çizgi Menü (☰))
        val bottomContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM)
            setPadding(45, 10, 45, 45)
        }

        val earRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
            setPadding(0, 0, 10, 25)
        }

        val earBtn = TextView(this).apply {
            text = "👂"
            textSize = 22f
            setPadding(20, 15, 20, 15)
            setOnClickListener {
                startAssistantService("WAKE_NOW")
                Toast.makeText(this@MainActivity, "\"Hey $aiName\" dinlemesi aktif!", Toast.LENGTH_SHORT).show()
            }
        }
        earRow.addView(earBtn)

        val navRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val settingsIconBtn = TextView(this).apply {
            text = "⚙️"
            textSize = 22f
            setPadding(25, 15, 25, 15)
            setOnClickListener { showSettingsScreen() }
        }

        val sendChatBtn = TextView(this).apply {
            text = "➤"
            textSize = 26f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener { showGeminiChatScreen() }
        }

        val menuThreeLineBtn = TextView(this).apply {
            text = "☰"
            textSize = 26f
            setTextColor(Color.WHITE)
            setPadding(25, 15, 25, 15)
            setOnClickListener { showBottomModeSheet() }
        }

        navRow.addView(settingsIconBtn)
        navRow.addView(sendChatBtn)
        navRow.addView(menuThreeLineBtn)

        bottomContainer.addView(earRow)
        bottomContainer.addView(navRow)

        mainLayout.addView(centerBox)
        mainLayout.addView(topBar)
        mainLayout.addView(bottomContainer)

        rootContainer.addView(mainLayout)
    }

    // FOTOĞRAF 4: Yeni Sohbet / Mesajlaşma Modu / Geçmiş Sohbetler Menüsü
    private fun showBottomModeSheet() {
        val dialog = AlertDialog.Builder(this).create()
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(45, 45, 45, 45)
            background = GradientDrawable().apply {
                cornerRadius = 45f
                setColor(Color.parseColor("#121218"))
                setStroke(2, Color.parseColor("#27273A"))
            }
        }

        fun buildMenuRow(icon: String, title: String, subtitle: String, onClick: () -> Unit): View {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(25, 30, 25, 30)
                setOnClickListener {
                    dialog.dismiss()
                    onClick()
                }
            }
            val ic = TextView(this).apply {
                text = icon
                textSize = 20f
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(110, 110).apply { rightMargin = 30 }
                background = GradientDrawable().apply {
                    cornerRadius = 30f
                    setColor(Color.parseColor("#1E1B2E"))
                }
            }
            val textCol = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            val t1 = TextView(this).apply {
                text = title
                textSize = 17f
                setTextColor(Color.WHITE)
                typeface = Typeface.DEFAULT_BOLD
            }
            val t2 = TextView(this).apply {
                text = subtitle
                textSize = 13f
                setTextColor(Color.parseColor("#9CA3AF"))
            }
            textCol.addView(t1)
            if (subtitle.isNotEmpty()) textCol.addView(t2)

            val arrow = TextView(this).apply {
                text = "›"
                textSize = 24f
                setTextColor(Color.parseColor("#6B7280"))
            }

            row.addView(ic)
            row.addView(textCol)
            row.addView(arrow)
            return row
        }

        box.addView(buildMenuRow("✏️", "Yeni Sohbet", "Yeni bir konuşma başlat.") {
            AppConfig.clearHistory(this)
            Toast.makeText(this, "Yeni sohbet başlatıldı!", Toast.LENGTH_SHORT).show()
            showMainOrbScreen()
        })
        box.addView(buildMenuRow("💬", "Mesajlaşma Modu", "Gemini gibi yazılı ve sesli sohbet ekranı.") {
            showGeminiChatScreen()
        })
        box.addView(buildMenuRow("🕒", "Geçmiş Sohbetler", "Önceki konuşmalarını görüntüle.") {
            showGeminiChatScreen()
        })
        box.addView(buildMenuRow("⚙️", "Ayarlar & Özelleştirme", "Dil, 4 ses profili, simge, ad ve hız.") {
            showSettingsScreen()
        })

        dialog.setView(box)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    // İSTEK 8: Gemini Tarzı Tam Sohbet (Mesajlaşma) Ekranı
    private fun showGeminiChatScreen() {
        rootContainer.removeAllViews()
        val aiName = AppConfig.getName(this)
        val themeColor = AppConfig.getThemeColor(this)

        val mainCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#07070C"))
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(40, 50, 40, 30)
            setBackgroundColor(Color.parseColor("#0F0F17"))
        }

        val backBtn = TextView(this).apply {
            text = "‹  Küreye Dön"
            textSize = 17f
            setTextColor(themeColor)
            typeface = Typeface.DEFAULT_BOLD
            setOnClickListener { showMainOrbScreen() }
        }

        val title = TextView(this).apply {
            text = "$aiName Sohbet (${AppConfig.getModelChoice(this@MainActivity)})"
            textSize = 17f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val clearBtn = TextView(this).apply {
            text = "🗑️"
            textSize = 18f
            setOnClickListener {
                AppConfig.clearHistory(this@MainActivity)
                showGeminiChatScreen()
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
                    text = "✦ $aiName ile sohbete başlayın.\nİnternet araması, video açma veya her türlü soruyu yazabilirsiniz."
                    textSize = 15f
                    setTextColor(Color.parseColor("#9CA3AF"))
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
                            setColor(if (isUser) themeColor else Color.parseColor("#181824"))
                        }
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            gravity = if (isUser) Gravity.END else Gravity.START
                            topMargin = 14
                            bottomMargin = 14
                        }
                    }
                    messagesList.addView(bubble)
                }
                chatScroll.post { chatScroll.fullScroll(View.FOCUS_DOWN) }
            }
        }
        renderMessages()

        // Alt Gemini Kapsül Giriş Çubuğu
        val inputPill = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(35, 20, 35, 20)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(30, 15, 30, 35)
            }
            background = GradientDrawable().apply {
                cornerRadius = 70f
                setColor(Color.parseColor("#181824"))
                setStroke(3, themeColor)
            }
        }

        val chatInput = EditText(this).apply {
            hint = "+ $aiName'a sorun..."
            setHintTextColor(Color.parseColor("#9CA3AF"))
            setTextColor(Color.WHITE)
            textSize = 15f
            background = null
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val micBtn = TextView(this).apply {
            text = "🎙️"
            textSize = 20f
            setPadding(20, 10, 20, 10)
            setOnClickListener {
                startAssistantService("WAKE_NOW")
            }
        }

        val sendBtn = TextView(this).apply {
            text = "➤"
            textSize = 22f
            setTextColor(themeColor)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(20, 10, 10, 10)
            setOnClickListener {
                val q = chatInput.text.toString().trim()
                if (q.isNotEmpty()) {
                    chatInput.setText("")
                    AppConfig.addHistoryItem(this@MainActivity, "user", q)
                    renderMessages()
                    startAssistantService("TEXT_QUERY", q)
                    Handler(Looper.getMainLooper()).postDelayed({ renderMessages() }, 2500)
                    Handler(Looper.getMainLooper()).postDelayed({ renderMessages() }, 5000)
                }
            }
        }

        inputPill.addView(chatInput)
        inputPill.addView(micBtn)
        inputPill.addView(sendBtn)

        mainCol.addView(header)
        mainCol.addView(chatScroll)
        mainCol.addView(inputPill)

        rootContainer.addView(mainCol)
    }

    // FOTOĞRAF 5: Ayarlar & Yardım (5 Dil, Konuşma Hızı, 2 Kız + 2 Erkek Ses, Simge Değiştirme, Ad Değiştirme)
    private fun showSettingsScreen() {
        rootContainer.removeAllViews()
        val scroll = ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#050508"))
        }
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(45, 55, 45, 80)
        }

        val topRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, 35)
        }
        val backTv = TextView(this).apply {
            text = "‹   Ayarlar & Yardım"
            textSize = 20f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            setOnClickListener {
                startAssistantService("RELOAD_CONFIG")
                showMainOrbScreen()
            }
        }
        topRow.addView(backTv)
        col.addView(topRow)

        fun sectionHeader(title: String): TextView = TextView(this).apply {
            text = title
            textSize = 14f
            setTextColor(Color.parseColor("#9CA3AF"))
            typeface = Typeface.DEFAULT_BOLD
            setPadding(10, 35, 10, 15)
        }

        fun optionCard(label: String, rightBadge: String?, isSelected: Boolean, onClick: () -> Unit): View {
            return LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(40, 34, 40, 34)
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    bottomMargin = 16
                }
                background = GradientDrawable().apply {
                    cornerRadius = 30f
                    setColor(Color.parseColor("#12121A"))
                    setStroke(if (isSelected) 3 else 1, if (isSelected) AppConfig.getThemeColor(this@MainActivity) else Color.parseColor("#222230"))
                }
                val t = TextView(this@MainActivity).apply {
                    text = label
                    textSize = 16f
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
                            cornerRadius = 20f
                            setColor(Color.parseColor("#1F2937"))
                        }
                    }
                    addView(b)
                }
                if (isSelected) {
                    val check = TextView(this@MainActivity).apply {
                        text = "   ✓"
                        textSize = 18f
                        setTextColor(AppConfig.getThemeColor(this@MainActivity))
                        typeface = Typeface.DEFAULT_BOLD
                    }
                    addView(check)
                }
                setOnClickListener { onClick() }
            }
        }

        // 1. Asistan Adı & Uygulama Simgesi
        col.addView(sectionHeader("Asistan Kimliği & Uygulama Simgesi"))
        col.addView(optionCard("🤖 Asistan Adı: ${AppConfig.getName(this)} (Hey ${AppConfig.getName(this)})", "Değiştir", false) {
            showRenameDialog()
        })
        col.addView(optionCard("🖼️ Uygulama Simgesini Değiştir (Galeriden Fotoğraf Yükle)", "Yükle", false) {
            val pick = Intent(Intent.ACTION_GET_CONTENT).apply { type = "image/*" }
            startActivityForResult(Intent.createChooser(pick, "Uygulama Simgesi Seçin"), REQ_PICK_ICON)
        })
        col.addView(optionCard("🔑 API Anahtarını Değiştir", "Sağ Üst", false) {
            showApiKeyDialog()
        })

        // 2. Dil Seçimi (Fotoğraf 5 birebir)
        col.addView(sectionHeader("Dil"))
        val curLang = AppConfig.getLang(this)
        val langOptions = listOf(
            "🇹🇷   Türkçe" to "tr-TR",
            "🇸🇦   Arapça" to "ar-SA",
            "🇺🇸   İngilizce" to "en-US",
            "🇷🇺   Rusça" to "ru-RU",
            "☀️   Kürtçe (Soranî)" to "ckb-IQ"
        )
        langOptions.forEach { (title, code) ->
            col.addView(optionCard(title, null, curLang == code) {
                AppConfig.setLang(this, code)
                startAssistantService("RELOAD_CONFIG")
                showSettingsScreen()
            })
        }

        // 3. Ses Seçimi: Her Dil İçin 2 Kız + 2 Erkek İnsansı Ses Profili
        col.addView(sectionHeader("İnsansı Ses Profili (Her Dil İçin 2 Kız & 2 Erkek)"))
        val curVoice = AppConfig.getVoiceProfile(this)
        val voiceProfiles = listOf(
            0 to "👩  Kız Sesi 1 (Doğal & Yumuşak İnsansı Ton)",
            1 to "👩‍🎤  Kız Sesi 2 (Canlı & Enerjik Asistan Tonu)",
            2 to "👨  Erkek Sesi 1 (Jarvis Karizmatik & Tok Ton)",
            3 to "🧔  Erkek Sesi 2 (Derin & Sakin Stüdyo Tonu)"
        )
        voiceProfiles.forEach { (idx, name) ->
            col.addView(optionCard(name, null, curVoice == idx) {
                AppConfig.setVoiceProfile(this, idx)
                startAssistantService("RELOAD_CONFIG")
                showSettingsScreen()
            })
        }

        // 4. Konuşma Hızı (Fotoğraf 5 birebir: Normal 1x, Hızlı 1.2x, Turbo 1.4x)
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

        // 5. Konuşma Modu (Fotoğraf 5 birebir: Otomatik / Bas-konuş)
        col.addView(sectionHeader("Konuşma Modu"))
        val curMode = AppConfig.getTalkMode(this)
        col.addView(optionCard("Otomatik\nSustuğunda ${AppConfig.getName(this)} kendisi gönderir (Uzun cümle destekli)", null, curMode == "AUTO") {
            AppConfig.setTalkMode(this, "AUTO")
            showSettingsScreen()
        })
        col.addView(optionCard("Bas-konuş\nDokun, konuş, bırakınca gönderilir", null, curMode == "PTT") {
            AppConfig.setTalkMode(this, "PTT")
            showSettingsScreen()
        })

        // 6. Yapay Zeka Modeli Seçimi (3.2 Flash, Hydrogen, Argon)
        col.addView(sectionHeader("Yapay Zeka Motoru Seçimi"))
        val curModel = AppConfig.getModelChoice(this)
        val models = listOf(
            "3.2_FLASH" to "⚡ Gemini 3.2 Flash (Ultra Hızlı Yanıt)",
            "HYDROGEN" to "🔥 Gemini 4 Hydrogen (Derin Zeka & Arama)",
            "ARGON" to "💎 Argon Özel Motoru (Maksimum Analiz & Sohbet)"
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
        val input = EditText(this).apply {
            setText(AppConfig.getName(this@MainActivity))
            hint = "Örn: Jarvis, Ahmet, Niko..."
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
            setPadding(40, 30, 40, 30)
        }
        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("Asistan Adını & Seslenme Komutunu Değiştir")
            .setMessage("Hangi adı yazarsanız 'Hey [Ad]' dediğinizde sağ alttan o isimle açılır:")
            .setView(input)
            .setPositiveButton("Kaydet") { _, _ ->
                val newName = input.text.toString().trim().ifEmpty { "Jarvis" }
                AppConfig.setName(this, newName)
                topNameLabel?.text = newName
                statusLabel?.text = "Dinliyorum... (\"Hey $newName\" de veya küreye dokun)"
                startAssistantService("RELOAD_CONFIG")
                Toast.makeText(this, "Artık 'Hey $newName' diyerek seslenebilirsiniz!", Toast.LENGTH_LONG).show()
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    private fun showApiKeyDialog() {
        val input = EditText(this).apply {
            setText(AppConfig.getKey(this@MainActivity))
            hint = "API Anahtarınızı (AQ. veya AIza...) girin"
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
            setPadding(40, 30, 40, 30)
        }
        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("🔑 API Anahtarı Ayarı")
            .setMessage("Anahtarınızı buradan dilediğiniz zaman değiştirebilirsiniz:")
            .setView(input)
            .setPositiveButton("Kaydet") { _, _ ->
                val k = input.text.toString().trim()
                if (k.isNotEmpty()) {
                    AppConfig.setKey(this, k)
                    startAssistantService("RELOAD_CONFIG")
                    Toast.makeText(this, "API Anahtarı güncellendi!", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    private fun showModelAndColorDialog() {
        val items = arrayOf(
            "⚡ Model: 3.2 Flash",
            "🔥 Model: Hydrogen (Önerilen)",
            "💎 Model: Argon",
            "🟡 Renk: Şeffaf Altın Sarısı (Jarvis)",
            "🔴 Renk: Mercan Kırmızısı (Fotoğraf 2)",
            "🟣 Renk: Neon Mor (Fotoğraf 3)",
            "🔵 Renk: Siber Mavi"
        )
        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("✨ Model & Renk Seçimi")
            .setItems(items) { _, which ->
                when (which) {
                    0 -> AppConfig.setModelChoice(this, "3.2_FLASH")
                    1 -> AppConfig.setModelChoice(this, "HYDROGEN")
                    2 -> AppConfig.setModelChoice(this, "ARGON")
                    3 -> AppConfig.setThemeColor(this, Color.parseColor("#FBBF24"))
                    4 -> AppConfig.setThemeColor(this, Color.parseColor("#F87171"))
                    5 -> AppConfig.setThemeColor(this, Color.parseColor("#C084FC"))
                    6 -> AppConfig.setThemeColor(this, Color.parseColor("#38BDF8"))
                }
                startAssistantService("RELOAD_CONFIG")
                showMainOrbScreen()
            }
            .show()
    }

    // İSTEK 6: Galeriden Seçilen Fotoğrafı Uygulama Simgesi Yapma
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

                    // Ana ekrana seçilen fotoğrafla ve seçilen isimle uygulama simgesi ekle
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

                    Toast.makeText(this, "Yeni simge kaydedildi ve ana ekrana eklendi!", Toast.LENGTH_LONG).show()
                    showMainOrbScreen()
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Fotoğraf yüklenemedi: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
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

// ARKA PLAN SERVİSİ: Sağ Alt Şeffaf Sarı Google Asistan Barı + Canlı Fotoğraf Kartı + 6 Kelime Kesilmeyen Motor
class OverlayService : Service(), TextToSpeech.OnInitListener {
    private lateinit var windowManager: WindowManager
    private lateinit var rootOverlayLayout: LinearLayout
    private lateinit var imageCardView: LinearLayout
    private lateinit var resultImageView: ImageView
    private lateinit var imageCaptionTv: TextView
    private lateinit var assistantPill: LinearLayout
    private lateinit var pillPromptTv: TextView
    private lateinit var waveBarsTv: TextView

    private var speechRecognizer: SpeechRecognizer? = null
    private lateinit var recognizerIntent: Intent
    private var tts: TextToSpeech? = null
    private val client = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(35, TimeUnit.SECONDS)
        .build()
    private val mainHandler = Handler(Looper.getMainLooper())
    private var resolvedModelPath: String? = null

    private var isAwakeForCommand = false
    private var isSpeaking = false
    private var isProcessing = false
    private var isListeningNow = false

    // HATA 1 ÇÖZÜMÜ: 6 Kelimeden Uzun Cümleleri Kesmeyen Birleştirici Tampon (Buffer)
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
        if (!isSpeaking && !isProcessing && !isAwakeForCommand) {
            rootOverlayLayout.visibility = View.GONE
            imageCardView.visibility = View.GONE
            MainActivity.orbView?.isSpeakingOrListening = false
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startAsForeground()
        when (intent?.getStringExtra("ACTION")) {
            "WAKE_NOW" -> {
                sentenceBuffer.clear()
                latestPartialChunk = ""
                wakeUpRightBottomPill("+ ${AppConfig.getName(this)}'a sorun")
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
                resolvedModelPath = null
                applyVoiceAndLanguageConfig()
                updatePillNamingAndColor()
            }
        }
        return START_STICKY
    }

    private fun startAsForeground() {
        val channelId = "ai_master_voice_channel"
        val aiName = AppConfig.getName(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "$aiName Sesli Asistan", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("$aiName Nöbette")
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

        // SAĞ ALT KÖŞE KOMPLE KAPSÜL + ÜSTÜNDE FOTOĞRAF KARTI (Fotoğraf 1 ile birebir boyut, şeffaf sarı)
        rootOverlayLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.END
            visibility = View.GONE
        }

        // İSTEK 3: Konuşulan Konularla İlgili Sağ Alt Köşede Açılan Görsel/Fotoğraf Kartı
        imageCardView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            setPadding(18, 18, 18, 18)
            layoutParams = LinearLayout.LayoutParams(540, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = 18
            }
            background = GradientDrawable().apply {
                cornerRadius = 40f
                setColor(Color.parseColor("#D918181B"))
                setStroke(3, Color.parseColor("#B3FBBF24"))
            }
        }

        resultImageView = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 310)
            scaleType = ImageView.ScaleType.CENTER_CROP
        }

        imageCaptionTv = TextView(this).apply {
            textSize = 12f
            setTextColor(Color.parseColor("#FEF08A"))
            setPadding(12, 10, 12, 4)
        }

        imageCardView.addView(resultImageView)
        imageCardView.addView(imageCaptionTv)

        // FOTOĞRAF 1: "+ [İsim]'a sorun   (🎙️)  ılı" Şeffaf Sarı Google Asistan Hapı
        assistantPill = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(38, 22, 32, 22)
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 90f
                setColor(Color.parseColor("#BF26200A")) // Şeffaf sarımsı koyu cam
                setStroke(3, Color.parseColor("#E6FBBF24")) // Şeffaf Altın Sarısı Kenarlık
            }
        }

        val plusSignTv = TextView(this).apply {
            text = "+  "
            textSize = 22f
            setTextColor(Color.parseColor("#FEF08A"))
            typeface = Typeface.DEFAULT_BOLD
            setOnClickListener {
                val i = Intent(this@OverlayService, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(i)
            }
        }

        pillPromptTv = TextView(this).apply {
            text = "${AppConfig.getName(this@OverlayService)}'a sorun"
            textSize = 16f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(6, 0, 36, 0)
            maxWidth = 520
            setOnClickListener {
                wakeUpRightBottomPill("🎙️ Dinliyorum...")
            }
        }

        // Beyaz/Sarı Yuvarlak Mikrofon İkonu (Fotoğraf 1'deki sağ iç daire)
        val micCircleTv = TextView(this).apply {
            text = "🎙️"
            textSize = 15f
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(76, 76).apply { rightMargin = 20 }
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#E6FDE047"))
            }
            setOnClickListener {
                mainHandler.removeCallbacks(commitSpeechRunnable)
                commitSpeechRunnable.run()
            }
        }

        waveBarsTv = TextView(this).apply {
            text = "ılı"
            textSize = 17f
            setTextColor(Color.parseColor("#FEF08A"))
            typeface = Typeface.DEFAULT_BOLD
            setOnClickListener {
                isAwakeForCommand = false
                rootOverlayLayout.visibility = View.GONE
            }
        }

        assistantPill.addView(plusSignTv)
        assistantPill.addView(pillPromptTv)
        assistantPill.addView(micCircleTv)
        assistantPill.addView(waveBarsTv)

        rootOverlayLayout.addView(imageCardView)
        rootOverlayLayout.addView(assistantPill)

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
            gravity = Gravity.BOTTOM or Gravity.END // Tam Sağ Alt Köşe
            x = 36
            y = 48
        }

        windowManager.addView(rootOverlayLayout, params)

        setupContinuousSpeechRecognizer()
        mainHandler.postDelayed({ startListeningSafely() }, 600)
    }

    private fun updatePillNamingAndColor() {
        mainHandler.post {
            val aiName = AppConfig.getName(this)
            pillPromptTv.text = "$aiName'a sorun"
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
            MainActivity.orbView?.isSpeakingOrListening = true
            MainActivity.statusLabel?.text = text
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
                    MainActivity.orbView?.isSpeakingOrListening = true
                    MainActivity.orbView?.audioLevel = 4.2f
                }
                override fun onDone(utteranceId: String?) {
                    isSpeaking = false
                    isProcessing = false
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

    // İSTEK 3 & 8: 5 Dil ve Her Dil İçin 2 Kız + 2 Erkek İnsansı Ses Profili
    private fun applyVoiceAndLanguageConfig() {
        val langCode = AppConfig.getLang(this)
        val locale = when {
            langCode.startsWith("ar") -> Locale("ar", "SA")
            langCode.startsWith("en") -> Locale("en", "US")
            langCode.startsWith("ru") -> Locale("ru", "RU")
            langCode.startsWith("ckb") || langCode.startsWith("ku") -> Locale("ku", "IQ")
            else -> Locale("tr", "TR")
        }
        tts?.language = locale

        val profile = AppConfig.getVoiceProfile(this) // 0: Kız 1, 1: Kız 2, 2: Erkek 1, 3: Erkek 2
        val baseSpeed = AppConfig.getSpeed(this)

        try {
            val allVoices = tts?.voices?.filter {
                it.locale.language.equals(locale.language, ignoreCase = true)
            }?.sortedByDescending {
                (if (it.name.contains("network", true) || it.name.contains("wavenet", true)) 10 else 0) +
                (if (!it.isNetworkConnectionRequired) 2 else 5)
            } ?: emptyList()

            if (allVoices.isNotEmpty()) {
                val chosenVoice = allVoices[profile % allVoices.size]
                tts?.voice = chosenVoice
            }
        } catch (e: Exception) {}

        // İnsansı Tonlama (Pitch) ve Akıcılık Ayarları
        when (profile) {
            0 -> { // Kız 1: Doğal Sıcak İnsansı
                tts?.setPitch(1.06f)
                tts?.setSpeechRate(baseSpeed * 1.02f)
            }
            1 -> { // Kız 2: Enerjik Net
                tts?.setPitch(1.16f)
                tts?.setSpeechRate(baseSpeed * 1.07f)
            }
            2 -> { // Erkek 1: Jarvis Karizmatik Doğal
                tts?.setPitch(0.88f)
                tts?.setSpeechRate(baseSpeed * 1.03f)
            }
            3 -> { // Erkek 2: Derin Tok Stüdyo
                tts?.setPitch(0.78f)
                tts?.setSpeechRate(baseSpeed * 0.98f)
            }
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
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "MasterAIUtterance")
    }

    // HATA 1 ÇÖZÜMÜ: 6 Kelimede Kesilmeyen, Uzun Konuşmaları Birleştiren Kesintisiz Dinleme Motoru
    private fun setupContinuousSpeechRecognizer() {
        mainHandler.post {
            speechRecognizer?.destroy()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(applicationContext)
            recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, AppConfig.getLang(this@OverlayService))
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                // Uzun cümlelerde erken kesmeyi önlemek için sessizlik toleranslarını maksimuma çıkarıyoruz:
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 4500L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 4000L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 15000L)
            }

            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    isListeningNow = true
                }
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {
                    val lvl = if (rmsdB < 0) 0.5f else rmsdB.coerceIn(0.5f, 10f)
                    MainActivity.orbView?.audioLevel = lvl
                    if (isAwakeForCommand) {
                        waveBarsTv.text = if (lvl > 2.6f) "IlıI" else "ılı"
                    }
                }
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {
                    isListeningNow = false
                }
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
                            // Dinamik isim ("Hey Jarvis", "Hey Ahmet", "Hey Niko" ne seçildiyse) duyuldu!
                            sentenceBuffer.clear()
                            val afterWake = stripWakeWord(partial, aiName)
                            latestPartialChunk = afterWake
                            wakeUpRightBottomPill(if (afterWake.isEmpty()) "🎙️ Dinliyorum..." else afterWake)
                            resetSilenceTimer()
                        } else if (isAwakeForCommand) {
                            latestPartialChunk = stripWakeWord(partial, aiName)
                            val preview = (sentenceBuffer.toString() + " " + latestPartialChunk).trim()
                            pillPromptTv.text = preview.ifEmpty { "🎙️ Dinliyorum..." }
                            MainActivity.statusLabel?.text = preview
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
                        wakeUpRightBottomPill(clean.ifEmpty { "🎙️ Dinliyorum..." })
                        resetSilenceTimer()
                        startListeningSafely()
                    } else if (isAwakeForCommand) {
                        // Android 6 kelimede bir sonucu kesse bile hemen aramaya geçmiyoruz!
                        // Cümleyi tampona ekleyip dinlemeye devam ediyoruz, kullanıcı gerçekten susunca (3.2 sn) gönderiyoruz.
                        val clean = stripWakeWord(spoken, aiName)
                        if (clean.isNotEmpty()) {
                            if (sentenceBuffer.isNotEmpty()) sentenceBuffer.append(" ")
                            sentenceBuffer.append(clean)
                        }
                        latestPartialChunk = ""
                        val combined = sentenceBuffer.toString().trim()
                        pillPromptTv.text = combined.ifEmpty { "🎙️ Dinliyorum..." }
                        MainActivity.statusLabel?.text = combined
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
        // Kullanıcı konuşmaya devam ettiği sürece sayacı sıfırla; sustuktan 3.2 saniye sonra cümleyi bütün olarak gönder
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

    // HATA 2 & İSTEK 4 ÇÖZÜMÜ: Video Araması, Web Araması ve Bankacılık Hariç Sınırsız Kontrol
    private fun processFullCommand(command: String) {
        isProcessing = true
        val lower = command.lowercase(Locale("tr", "TR"))

        AppConfig.addHistoryItem(this, "user", command)
        mainHandler.post {
            rootOverlayLayout.visibility = View.VISIBLE
            pillPromptTv.text = "⏳ $command"
            MainActivity.statusLabel?.text = "⏳ $command"
        }

        // İSTEK 4: Bankacılık Uygulamaları Güvenlik Kilidi
        val bankingKeywords = listOf(
            "banka", "garanti", "ziraat", "iş bankası", "iscep", "akbank", "yapı kredi",
            "qnb", "finansbank", "vakıfbank", "halkbank", "denizbank", "teb", "kuveyttürk",
            "enpara", "papara", "iban", "havale", "eft"
        )
        if (bankingKeywords.any { lower.contains(it) } && (lower.contains("aç") || lower.contains("gönder") || lower.contains("transfer"))) {
            replyAndShow("Güvenlik protokolü gereği bankacılık işlemlerine erişimim kapalıdır.", null)
            return
        }

        // HATA 2 ÇÖZÜMÜ: İnternet ve YouTube Üzerinden Doğrudan Video Araması
        if (lower.contains("video") || lower.contains("izle") || lower.contains("youtube'da") || lower.contains("youtubeda") || lower.contains("şarkı aç") || lower.contains("müzik aç")) {
            val query = lower
                .replace("videosunu aç", "")
                .replace("videosu aç", "")
                .replace("video ara", "")
                .replace("videosunu bul", "")
                .replace("video aç", "")
                .replace("youtube'da", "")
                .replace("youtubeda", "")
                .replace("izle", "")
                .replace("ara", "")
                .replace("aç", "")
                .trim()
                .ifEmpty { command }

            searchAndPlayVideo(query)
            return
        }

        // Doğrudan Google Web Araması Komutu
        if ((lower.contains("google'da") || lower.contains("internette") || lower.contains("webde")) && lower.contains("ara")) {
            val q = lower.replace("google'da", "").replace("internette", "").replace("webde", "").replace("ara", "").trim()
            val url = "https://www.google.com/search?q=${URLEncoder.encode(q, "UTF-8")}"
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(browserIntent)
            replyAndShow("$q için internet araması açılıyor.", q)
            return
        }

        // Cihazdaki Herhangi Bir Uygulamayı Açma (Bankacılık Hariç Sınırsız)
        if (lower.endsWith(" aç") || lower.contains("uygulamasını aç")) {
            val targetApp = lower.replace("uygulamasını aç", "").replace("yi aç", "").replace("yı aç", "").replace("उ aç", "").replace("aç", "").trim()
            if (openAnyInstalledApp(targetApp)) return
        }

        // Gemini + Google Search Grounding + Görsel Kart Sistemi
        askGeminiWithSearchAndImage(command, AppConfig.getKey(this))
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
                // Banka paketlerini engelle
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
            MainActivity.statusLabel?.text = answer
            if (!imageKeyword.isNullOrBlank()) {
                loadTopicPhotoIntoRightBottomCard(imageKeyword)
            } else {
                imageCardView.visibility = View.GONE
            }
            speak(answer)
        }
    }

    // İSTEK 3: Konuşulan Konuyla İlgili Sağ Alt Köşede Fotoğraf Gösterme (Wikipedia & Wikimedia Görsel Motoru)
    private fun loadTopicPhotoIntoRightBottomCard(keyword: String) {
        val cleanKw = keyword.trim()
        val langPrefix = AppConfig.getLang(this).take(2).lowercase(Locale.US)
        val wikiUrl = "https://$langPrefix.wikipedia.org/w/api.php?action=query&prop=pageimages&format=json&piprop=thumbnail&pithumbsize=600&generator=search&gsrsearch=${URLEncoder.encode(cleanKw, "UTF-8")}&gsrlimit=1"

        val req = Request.Builder()
            .url(wikiUrl)
            .header("User-Agent", "MasterAssistant/8.0")
            .build()

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

    private fun askGeminiWithSearchAndImage(prompt: String, apiKey: String) {
        if (resolvedModelPath != null) {
            executeGeminiCall(prompt, apiKey, resolvedModelPath!!)
            return
        }

        val listUrl = "https://generativelanguage.googleapis.com/v1beta/models?key=$apiKey"
        val req = Request.Builder().url(listUrl).addHeader("x-goog-api-key", apiKey).get().build()

        client.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                replyAndShow("İnternet bağlantı hatası oluştu.", null)
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
                            if (methods.contains("generateContent")) available.add(name)
                        }

                        // Kullanıcının seçtiği modele göre (3.2 Flash / Hydrogen / Argon) eşleştirme
                        val choice = AppConfig.getModelChoice(this@OverlayService)
                        val chosen = when (choice) {
                            "3.2_FLASH" -> available.firstOrNull { it.contains("flash", true) && (it.contains("3", true) || it.contains("2.5", true)) }
                            "ARGON" -> available.firstOrNull { it.contains("pro", true) || it.contains("exp", true) || it.contains("2.5", true) }
                            else -> available.firstOrNull { it.contains("hydrogen", true) || it.contains("gemini-4", true) || it.contains("2.5-flash", true) }
                        } ?: available.firstOrNull { it.contains("gemini-2.5-flash", true) }
                          ?: available.firstOrNull { it.contains("gemini", true) }
                          ?: available.firstOrNull()

                        if (chosen != null) {
                            resolvedModelPath = chosen
                            executeGeminiCall(prompt, apiKey, chosen)
                        } else {
                            replyAndShow("Uygun model bulunamadı.", null)
                        }
                    } catch (e: Exception) {
                        replyAndShow("Model listesi okunamadı.", null)
                    }
                } else {
                    replyAndShow("API Anahtarı hatalı (Kod: ${response.code}). Sağ üstteki 🔑 tuşundan kontrol edin.", null)
                }
            }
        })
    }

    private fun executeGeminiCall(prompt: String, cleanKey: String, modelPath: String) {
        val aiName = AppConfig.getName(this)
        val lang = AppConfig.getLang(this)
        val modelBadge = AppConfig.getModelChoice(this)

        val sysPrompt = """
            Senin adın $aiName. Kullanıcının seçtiği ($modelBadge) motoruyla çalışan, son derece zeki, insansı ve doğal konuşan sesli asistansın.
            Dil kodu: $lang. Doğal, akıcı ve net cevap ver (maksimum 2-3 cümle).
            Eğer kullanıcının sorusu bir kişi, şehir, hayvan, eşya, yemek, gezegen, araba veya görseli gösterilebilecek somut bir konu hakkındaysa cevabının en sonuna köşeli parantez içinde [RESIM: Konu Başlığı] ekle (Örn: [RESIM: Eyfel Kulesi] veya [RESIM: Aslan]).
            Eğer kullanıcı video izlemek veya açmak istiyorsa cevabının sonuna [VIDEO: Arama Terimi] ekle.
            Kullanıcı: $prompt
        """.trimIndent()

        val jsonBody = JSONObject().apply {
            put("contents", JSONArray().put(
                JSONObject().put("parts", JSONArray().put(
                    JSONObject().put("text", sysPrompt)
                ))
            ))
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/$modelPath:generateContent?key=$cleanKey"
        val request = Request.Builder()
            .url(url)
            .addHeader("x-goog-api-key", cleanKey)
            .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                replyAndShow("Bağlantı zaman aşımına uğradı.", null)
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

                        // [VIDEO: ...] etiketi varsa videoyu aç
                        val videoRegex = Regex("\\[VIDEO:\\s*(.*?)\\]")
                        val videoMatch = videoRegex.find(rawAnswer)
                        if (videoMatch != null) {
                            val vQuery = videoMatch.groupValues[1].trim()
                            val cleanText = rawAnswer.replace(videoRegex, "").trim()
                            searchAndPlayVideo(vQuery)
                            return
                        }

                        // [RESIM: ...] etiketi varsa sağ alt köşede fotoğrafını göster
                        val imgRegex = Regex("\\[RESIM:\\s*(.*?)\\]")
                        val imgMatch = imgRegex.find(rawAnswer)
                        val imgTopic = imgMatch?.groupValues?.get(1)?.trim()
                        val spokenAnswer = rawAnswer.replace(imgRegex, "").trim()

                        replyAndShow(spokenAnswer, imgTopic)
                    } catch (e: Exception) {
                        replyAndShow("Cevap çözümlenemedi.", null)
                    }
                } else {
                    resolvedModelPath = null
                    replyAndShow("Sunucu hatası (${response.code}).", null)
                }
            }
        })
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
