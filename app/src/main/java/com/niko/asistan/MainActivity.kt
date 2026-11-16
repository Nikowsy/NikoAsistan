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
import android.os.PowerManager
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
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
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object AppConfig {
    const val PREFS = "GeminiMasterV10Prefs"
    const val DEFAULT_KEY = "AQ.Ab8RN6KZYPqSekpNWFEnuG42klSAK_TKxGeFNc1YbcduzUm0kA"

    fun isSetupDone(ctx: Context): Boolean =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean("SETUP_DONE", false)

    fun setSetupDone(ctx: Context, done: Boolean) =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean("SETUP_DONE", done).apply()

    fun getName(ctx: Context): String =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("AI_NAME", "")?.takeIf { it.isNotBlank() } ?: "Duman"

    fun setName(ctx: Context, name: String) =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString("AI_NAME", name.trim()).apply()

    fun getUserName(ctx: Context): String =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("USER_NAME", "Abdullah")?.takeIf { it.isNotBlank() } ?: "Abdullah"

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

    fun getConversations(ctx: Context): JSONArray {
        val raw = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("CONVERSATIONS_V10", "[]") ?: "[]"
        return try { JSONArray(raw) } catch (e: Exception) { JSONArray() }
    }

    private fun saveConversations(ctx: Context, arr: JSONArray) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString("CONVERSATIONS_V10", arr.toString()).apply()
    }

    fun getCurrentSessionId(ctx: Context): String {
        val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        var id = prefs.getString("CURRENT_SESSION_ID", null)
        val convs = getConversations(ctx)
        if (id == null || convs.length() == 0) {
            id = createNewConversation(ctx)
        }
        return id
    }

    fun setCurrentSessionId(ctx: Context, id: String) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString("CURRENT_SESSION_ID", id).apply()
    }

    fun createNewConversation(ctx: Context): String {
        val newId = UUID.randomUUID().toString()
        val convs = getConversations(ctx)
        val newObj = JSONObject().apply {
            put("id", newId)
            put("title", "Yeni Sohbet")
            put("timestamp", System.currentTimeMillis())
            put("messages", JSONArray())
        }
        val updated = JSONArray()
        updated.put(newObj)
        for (i in 0 until convs.length().coerceAtMost(29)) {
            updated.put(convs.getJSONObject(i))
        }
        saveConversations(ctx, updated)
        setCurrentSessionId(ctx, newId)
        return newId
    }

    fun getMessagesForSession(ctx: Context, sessionId: String): JSONArray {
        val convs = getConversations(ctx)
        for (i in 0 until convs.length()) {
            val c = convs.getJSONObject(i)
            if (c.getString("id") == sessionId) {
                return c.optJSONArray("messages") ?: JSONArray()
            }
        }
        return JSONArray()
    }

    fun addMessageToCurrentSession(ctx: Context, role: String, text: String) {
        val sessionId = getCurrentSessionId(ctx)
        val convs = getConversations(ctx)
        for (i in 0 until convs.length()) {
            val c = convs.getJSONObject(i)
            if (c.getString("id") == sessionId) {
                val msgs = c.optJSONArray("messages") ?: JSONArray()
                msgs.put(JSONObject().apply {
                    put("role", role)
                    put("text", text)
                    put("time", System.currentTimeMillis())
                })
                c.put("messages", msgs)
                if (role == "user" && (c.optString("title") == "Yeni Sohbet" || msgs.length() <= 2)) {
                    c.put("title", text.take(34))
                }
                c.put("timestamp", System.currentTimeMillis())
                saveConversations(ctx, convs)
                return
            }
        }
    }

    fun deleteConversation(ctx: Context, sessionId: String) {
        val convs = getConversations(ctx)
        val updated = JSONArray()
        for (i in 0 until convs.length()) {
            val c = convs.getJSONObject(i)
            if (c.getString("id") != sessionId) {
                updated.put(c)
            }
        }
        saveConversations(ctx, updated)
        if (updated.length() > 0) {
            setCurrentSessionId(ctx, updated.getJSONObject(0).getString("id"))
        } else {
            createNewConversation(ctx)
        }
    }
}

class ParticleSphereView(context: Context) : View(context) {
    var audioLevel = 0f
    var isActive = false
    private var rotY = 0f
    private val rotX = 0.25f
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    private val points = Array(150) { i ->
        val numPoints = 150
        val goldenAngle = Math.PI * (3.0 - sqrt(5.0))
        val y = 1.0 - (i.toDouble() / (numPoints - 1)) * 2.0
        val radius = sqrt(1.0 - y * y)
        val theta = goldenAngle * i
        floatArrayOf((cos(theta) * radius).toFloat(), y.toFloat(), (sin(theta) * radius).toFloat())
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!isShown || visibility != VISIBLE) return

        val cx = width / 2f
        val cy = height / 2f
        val baseRadius = (width.coerceAtMost(height) * 0.34f).coerceAtLeast(70f)

        rotY += if (isActive) 0.035f else 0.008f
        val pulse = if (isActive) (audioLevel * 4.2f).coerceIn(3f, 34f) else 0f

        val cosY = cos(rotY)
        val sinY = sin(rotY)
        val cosX = cos(rotX)
        val sinX = sin(rotX)

        for (i in points.indices) {
            val p = points[i]
            val wave = if (isActive) sin(rotY * 4f + i * 0.15f) * (pulse * 0.35f) else 0f
            val r = baseRadius + wave

            val x1 = p[0] * cosY - p[2] * sinY
            val z1 = p[2] * cosY + p[0] * sinY
            val y2 = p[1] * cosX - z1 * sinX
            val z2 = z1 * cosX + p[1] * sinX

            val depth = (z2 + 1.4f) / 2.4f
            dotPaint.alpha = (depth * 230f + 25f).toInt().coerceIn(30, 255)
            val dotSize = depth * 4.0f + 1.5f
            canvas.drawCircle(cx + x1 * r, cy + y2 * r, dotSize, dotPaint)
        }
        postInvalidateDelayed(32L)
    }
}

class GeminiMicButtonView(context: Context) : View(context) {
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; style = Paint.Style.FILL }
    private val micFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#111111"); style = Paint.Style.FILL }
    private val micStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#111111")
        style = Paint.Style.STROKE
        strokeWidth = 4.2f
        strokeCap = Paint.Cap.ROUND
    }
    private val rect = RectF()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val r = (width.coerceAtMost(height) / 2f) - 2f
        canvas.drawCircle(cx, cy, r, bgPaint)

        val mw = r * 0.26f
        val mh = r * 0.40f
        rect.set(cx - mw, cy - mh, cx + mw, cy + mh * 0.25f)
        canvas.drawRoundRect(rect, mw, mw, micFillPaint)

        val arcR = r * 0.42f
        rect.set(cx - arcR, cy - mh * 0.35f, cx + arcR, cy + arcR * 0.85f)
        canvas.drawArc(rect, 0f, 180f, false, micStrokePaint)
        canvas.drawLine(cx, cy + arcR * 0.85f, cx, cy + r * 0.55f, micStrokePaint)
    }
}

class GeminiWaveIconView(context: Context) : View(context) {
    var activeWave = false
    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 4.8f
        strokeCap = Paint.Cap.ROUND
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val gap = 11f
        val h1 = if (activeWave) 17f else 9f
        val h2 = if (activeWave) 28f else 20f
        val h3 = if (activeWave) 20f else 12f
        val h4 = if (activeWave) 11f else 6f

        canvas.drawLine(cx - gap * 1.5f, cy - h1, cx - gap * 1.5f, cy + h1, barPaint)
        canvas.drawLine(cx - gap * 0.5f, cy - h2, cx - gap * 0.5f, cy + h2, barPaint)
        canvas.drawLine(cx + gap * 0.5f, cy - h3, cx + gap * 0.5f, cy + h3, barPaint)
        canvas.drawLine(cx + gap * 1.5f, cy - h4, cx + gap * 1.5f, cy + h4, barPaint)
    }
}

class MainActivity : AppCompatActivity() {
    companion object {
        var sphereView: ParticleSphereView? = null
        var centerSubTv: TextView? = null
        var isAppInForeground = false
        var onNewMessageCallback: (() -> Unit)? = null
        private const val REQ_PICK_ICON = 701
    }

    private lateinit var rootContainer: FrameLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        rootContainer = FrameLayout(this).apply { setBackgroundColor(Color.parseColor("#08080A")) }
        setContentView(rootContainer)
        
        checkPermissionsAndProceed()
    }

    override fun onResume() {
        super.onResume()
        isAppInForeground = true
        
        // Uygulamaya dönüldüğünde izinler tamamsa hemen servisi bağla
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED && Settings.canDrawOverlays(this)) {
            if (AppConfig.isSetupDone(this)) {
                if (rootContainer.childCount == 0) showMinimalistHome()
                startAssistantService("APP_FOREGROUND")
            }
        } else {
            checkPermissionsAndProceed()
        }
    }

    override fun onPause() {
        super.onPause()
        isAppInForeground = false
        if (AppConfig.isSetupDone(this)) {
            startAssistantService("APP_BACKGROUND")
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        checkPermissionsAndProceed()
    }

    // YENİ GÜVENLİK DUVARI: İzinler olmadan asla servisi başlatmaz, çöküşü önler!
    private fun checkPermissionsAndProceed() {
        val audioGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        val overlayGranted = Settings.canDrawOverlays(this)

        if (!audioGranted) {
            showPermissionGate("Mikrofon İzni Gerekli", "Asistanın sesinizi duyabilmesi için mikrofon erişimine izin vermelisiniz.") {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 1)
            }
        } else if (!overlayGranted) {
            showPermissionGate("Ekran Üstü İzni Gerekli", "Asistanın diğer uygulamaların üzerinde (sağ altta) görünebilmesi için bu izni vermelisiniz.") {
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            }
        } else {
            if (!AppConfig.isSetupDone(this)) {
                showOnboardingScreen()
            } else {
                if (rootContainer.childCount == 0) showMinimalistHome()
                startAssistantService("START_BG")
            }
        }
    }

    private fun showPermissionGate(title: String, desc: String, onGrant: () -> Unit) {
        rootContainer.removeAllViews()
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(80, 80, 80, 80)
        }
        val t1 = TextView(this).apply {
            text = title
            textSize = 24f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }
        val t2 = TextView(this).apply {
            text = desc
            textSize = 15f
            setTextColor(Color.parseColor("#A1A1AA"))
            gravity = Gravity.CENTER
            setPadding(0, 25, 0, 60)
        }
        val btn = Button(this).apply {
            text = "İzin Ver"
            setTextColor(Color.BLACK)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(50, 30, 50, 30)
            background = GradientDrawable().apply {
                cornerRadius = 50f
                setColor(Color.WHITE)
            }
            setOnClickListener { onGrant() }
        }
        col.addView(t1)
        col.addView(t2)
        col.addView(btn)
        rootContainer.addView(col)
    }

    fun startAssistantService(action: String, extraText: String? = null) {
        val svc = Intent(this, OverlayService::class.java).apply {
            putExtra("ACTION", action)
            if (extraText != null) putExtra("EXTRA_TEXT", extraText)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try { startForegroundService(svc) } catch (e: Exception) {}
        } else {
            startService(svc)
        }
    }

    private fun showOnboardingScreen() {
        rootContainer.removeAllViews()
        val scroll = ScrollView(this).apply { isFillViewport = true }
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(65, 80, 65, 70)
        }

        val miniSphere = ParticleSphereView(this).apply {
            layoutParams = LinearLayout.LayoutParams(300, 300).apply {
                gravity = Gravity.CENTER
                bottomMargin = 40
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
            text = "Adını belirleyin — uygulama kapalıyken bile \"Hey [Ad]\" dediğinizde sağ alttan açılır."
            textSize = 14f
            setTextColor(Color.parseColor("#8E8E93"))
            gravity = Gravity.CENTER
            setPadding(20, 14, 20, 35)
        }

        val aiNameInput = EditText(this).apply {
            hint = "Asistanın Adı (Örn: Duman, Jarvis)"
            setText("Duman")
            setHintTextColor(Color.parseColor("#636366"))
            setTextColor(Color.WHITE)
            textSize = 15f
            gravity = Gravity.CENTER
            setPadding(40, 30, 40, 30)
            background = GradientDrawable().apply {
                cornerRadius = 35f
                setColor(Color.parseColor("#141418"))
                setStroke(2, Color.parseColor("#2C2C34"))
            }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = 22
            }
        }

        val userNameInput = EditText(this).apply {
            hint = "Sizin Adınız (Örn: Abdullah)"
            setText("Abdullah")
            setHintTextColor(Color.parseColor("#636366"))
            setTextColor(Color.WHITE)
            textSize = 15f
            gravity = Gravity.CENTER
            setPadding(40, 30, 40, 30)
            background = GradientDrawable().apply {
                cornerRadius = 35f
                setColor(Color.parseColor("#141418"))
                setStroke(2, Color.parseColor("#2C2C34"))
            }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = 35
            }
        }

        val continueBtn = Button(this).apply {
            text = "Başla"
            textSize = 16f
            setTextColor(Color.BLACK)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(40, 34, 40, 34)
            background = GradientDrawable().apply {
                cornerRadius = 60f
                setColor(Color.WHITE)
            }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            setOnClickListener {
                val chosenAi = aiNameInput.text.toString().trim().ifEmpty { "Duman" }
                val chosenUser = userNameInput.text.toString().trim().ifEmpty { "Abdullah" }
                AppConfig.setName(this@MainActivity, chosenAi)
                AppConfig.setUserName(this@MainActivity, chosenUser)
                AppConfig.setSetupDone(this@MainActivity, true)
                showMinimalistHome()
                startAssistantService("START_BG")
                startAssistantService("RELOAD_CONFIG")
            }
        }

        col.addView(miniSphere)
        col.addView(greetingTitle)
        col.addView(subDesc)
        col.addView(aiNameInput)
        col.addView(userNameInput)
        col.addView(continueBtn)

        scroll.addView(col)
        rootContainer.addView(scroll)
    }

    private fun showMinimalistHome() {
        onNewMessageCallback = null
        rootContainer.removeAllViews()
        val aiName = AppConfig.getName(this)
        val userName = AppConfig.getUserName(this)

        val mainFrame = FrameLayout(this).apply { setBackgroundColor(Color.parseColor("#08080A")) }

        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(50, 50, 50, 20)
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.TOP)
        }

        val historyBtn = TextView(this).apply {
            text = "☰"
            textSize = 22f
            setTextColor(Color.parseColor("#A1A1AA"))
            setPadding(15, 10, 30, 10)
            setOnClickListener { showGeminiChatScreen(focusKeyboard = false) }
        }

        val titleTv = TextView(this).apply {
            text = aiName
            textSize = 22f
            setTextColor(Color.WHITE)
            typeface = Typeface.create("serif", Typeface.NORMAL)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val settingsTopBtn = TextView(this).apply {
            text = "⚙️"
            textSize = 20f
            setTextColor(Color.parseColor("#A1A1AA"))
            setPadding(30, 10, 15, 10)
            setOnClickListener { showSettingsScreen() }
        }

        topBar.addView(historyBtn)
        topBar.addView(titleTv)
        topBar.addView(settingsTopBtn)

        val centerCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
            setPadding(40, 50, 40, 130)
        }

        val pSphere = ParticleSphereView(this).apply {
            layoutParams = LinearLayout.LayoutParams(560, 560)
            setOnClickListener { startAssistantService("WAKE_NOW") }
            setOnTouchListener { _, event ->
                if (AppConfig.getTalkMode(this@MainActivity) == "PTT") {
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> { startAssistantService("WAKE_NOW"); true }
                        MotionEvent.ACTION_UP -> { startAssistantService("FINALIZE_SPEECH"); true }
                        else -> false
                    }
                } else false
            }
        }
        sphereView = pSphere

        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val timeGreeting = when (hour) {
            in 5..11 -> "Günaydın"
            in 12..17 -> "İyi günler"
            in 18..22 -> "İyi akşamlar"
            else -> "İyi geceler"
        }

        val greetingLabel = TextView(this).apply {
            text = "$timeGreeting, $userName."
            textSize = 27f
            setTextColor(Color.WHITE)
            typeface = Typeface.create("serif", Typeface.NORMAL)
            gravity = Gravity.CENTER
            setPadding(30, 30, 30, 10)
        }

        val subLabel = TextView(this).apply {
            text = "Küreye dokun veya \"Hey $aiName\" de"
            textSize = 14f
            setTextColor(Color.parseColor("#71717A"))
            gravity = Gravity.CENTER
            setPadding(40, 4, 40, 10)
        }
        centerSubTv = subLabel

        centerCol.addView(pSphere)
        centerCol.addView(greetingLabel)
        centerCol.addView(subLabel)

        val bottomWrap = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM)
        }

        val writePillBtn = TextView(this).apply {
            text = "⌨️  Yaz"
            textSize = 14f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(52, 22, 52, 22)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = 32
            }
            background = GradientDrawable().apply {
                cornerRadius = 50f
                setColor(Color.parseColor("#16161C"))
                setStroke(1, Color.parseColor("#3F3F46"))
            }
            setOnClickListener { showGeminiChatScreen(focusKeyboard = true) }
        }

        val divider = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1)
            setBackgroundColor(Color.parseColor("#1C1C22"))
        }

        val navBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(20, 20, 20, 28)
            setBackgroundColor(Color.parseColor("#0A0A0E"))
        }

        fun navTab(icon: String, label: String, active: Boolean, onClick: () -> Unit): View {
            return LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                val ic = TextView(this@MainActivity).apply {
                    text = icon
                    textSize = 16f
                    setTextColor(if (active) Color.WHITE else Color.parseColor("#636366"))
                    gravity = Gravity.CENTER
                }
                val lb = TextView(this@MainActivity).apply {
                    text = label
                    textSize = 11f
                    setTextColor(if (active) Color.WHITE else Color.parseColor("#636366"))
                    gravity = Gravity.CENTER
                    setPadding(0, 4, 0, 0)
                }
                addView(ic)
                addView(lb)
                setOnClickListener { onClick() }
            }
        }

        navBar.addView(navTab("❖", aiName, true) { showMinimalistHome() })
        navBar.addView(navTab("💬", "Sohbetler", false) { showGeminiChatScreen(focusKeyboard = false) })
        navBar.addView(navTab("⚙️", "Ayarlar", false) { showSettingsScreen() })

        bottomWrap.addView(writePillBtn)
        bottomWrap.addView(divider)
        bottomWrap.addView(navBar)

        mainFrame.addView(centerCol)
        mainFrame.addView(topBar)
        mainFrame.addView(bottomWrap)

        rootContainer.addView(mainFrame)
    }

    private fun showGeminiChatScreen(focusKeyboard: Boolean) {
        sphereView = null
        startAssistantService("HIDE_PILL")

        rootContainer.removeAllViews()
        val aiName = AppConfig.getName(this)
        val modelBadge = AppConfig.getModelChoice(this)

        val outerFrame = FrameLayout(this).apply { setBackgroundColor(Color.parseColor("#0B0B0F")) }

        val mainCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(35, 45, 35, 25)
            setBackgroundColor(Color.parseColor("#111117"))
        }

        val drawerOverlay = FrameLayout(this).apply {
            visibility = View.GONE
            setBackgroundColor(Color.parseColor("#99000000"))
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
            setOnClickListener { visibility = View.GONE }
        }

        val drawerPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 60, 40, 40)
            setBackgroundColor(Color.parseColor("#14141C"))
            layoutParams = FrameLayout.LayoutParams(680, FrameLayout.LayoutParams.MATCH_PARENT, Gravity.START)
            setOnClickListener { }
        }
        drawerOverlay.addView(drawerPanel)

        val messagesScroll = ScrollView(this).apply { layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f) }
        val messagesList = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(40, 30, 40, 30) }
        messagesScroll.addView(messagesList)

        fun renderCurrentConversation() {
            messagesList.removeAllViews()
            val sessionId = AppConfig.getCurrentSessionId(this)
            val arr = AppConfig.getMessagesForSession(this, sessionId)

            if (arr.length() == 0) {
                val emptyBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setPadding(40, 160, 40, 40) }
                val greet = TextView(this).apply {
                    text = "Merhaba, ${AppConfig.getUserName(this@MainActivity)}"
                    textSize = 28f
                    setTextColor(Color.WHITE)
                    typeface = Typeface.create("serif", Typeface.NORMAL)
                    gravity = Gravity.CENTER
                }
                val sub = TextView(this).apply {
                    text = "Bugün sana nasıl yardımcı olabilirim?"
                    textSize = 16f
                    setTextColor(Color.parseColor("#71717A"))
                    gravity = Gravity.CENTER
                    setPadding(0, 12, 0, 0)
                }
                emptyBox.addView(greet)
                emptyBox.addView(sub)
                messagesList.addView(emptyBox)
            } else {
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val isUser = obj.getString("role") == "user"
                    val msgText = obj.getString("text")

                    val row = LinearLayout(this).apply {
                        orientation = LinearLayout.VERTICAL
                        gravity = if (isUser) Gravity.END else Gravity.START
                        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                            topMargin = 14
                            bottomMargin = 14
                        }
                    }

                    if (!isUser) {
                        val senderLb = TextView(this).apply {
                            text = "✦ $aiName"
                            textSize = 12f
                            setTextColor(Color.parseColor("#A1A1AA"))
                            setPadding(12, 0, 12, 6)
                        }
                        row.addView(senderLb)
                    }

                    val bubble = TextView(this).apply {
                        text = msgText
                        textSize = 15f
                        setTextColor(if (isUser) Color.BLACK else Color.WHITE)
                        setPadding(40, 26, 40, 26)
                        background = GradientDrawable().apply {
                            cornerRadius = 38f
                            setColor(if (isUser) Color.WHITE else Color.parseColor("#1A1A24"))
                            if (!isUser) setStroke(1, Color.parseColor("#2E2E3A"))
                        }
                        maxWidth = (resources.displayMetrics.widthPixels * 0.75).toInt()
                    }
                    row.addView(bubble)
                    messagesList.addView(row)
                }
                messagesScroll.post { messagesScroll.fullScroll(View.FOCUS_DOWN) }
            }
        }

        fun rebuildDrawerList() {
            drawerPanel.removeAllViews()
            val drawerTitle = TextView(this).apply {
                text = "✦  Gemini Sohbet Geçmişi"
                textSize = 18f
                setTextColor(Color.WHITE)
                typeface = Typeface.DEFAULT_BOLD
                setPadding(10, 0, 10, 30)
            }
            drawerPanel.addView(drawerTitle)

            val newChatDrawerBtn = TextView(this).apply {
                text = "＋   Yeni Sohbet Başlat"
                textSize = 15f
                setTextColor(Color.BLACK)
                typeface = Typeface.DEFAULT_BOLD
                setPadding(35, 26, 35, 26)
                background = GradientDrawable().apply {
                    cornerRadius = 40f
                    setColor(Color.WHITE)
                }
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    bottomMargin = 30
                }
                setOnClickListener {
                    AppConfig.createNewConversation(this@MainActivity)
                    drawerOverlay.visibility = View.GONE
                    renderCurrentConversation()
                }
            }
            drawerPanel.addView(newChatDrawerBtn)

            val listScroll = ScrollView(this).apply { layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f) }
            val listCol = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            listScroll.addView(listCol)

            val convs = AppConfig.getConversations(this)
            val currentId = AppConfig.getCurrentSessionId(this)

            for (i in 0 until convs.length()) {
                val c = convs.getJSONObject(i)
                val cid = c.getString("id")
                val cTitle = c.optString("title", "Sohbet")
                val isSelected = (cid == currentId)

                val itemRow = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(28, 24, 20, 24)
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = 10 }
                    background = GradientDrawable().apply {
                        cornerRadius = 25f
                        setColor(if (isSelected) Color.parseColor("#272736") else Color.TRANSPARENT)
                    }
                    setOnClickListener {
                        AppConfig.setCurrentSessionId(this@MainActivity, cid)
                        drawerOverlay.visibility = View.GONE
                        renderCurrentConversation()
                    }
                }

                val tv = TextView(this).apply {
                    text = "💬  $cTitle"
                    textSize = 14f
                    setTextColor(if (isSelected) Color.WHITE else Color.parseColor("#A1A1AA"))
                    setSingleLine(true)
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }

                val delBtn = TextView(this).apply {
                    text = "✕"
                    textSize = 14f
                    setTextColor(Color.parseColor("#71717A"))
                    setPadding(20, 5, 10, 5)
                    setOnClickListener {
                        AppConfig.deleteConversation(this@MainActivity, cid)
                        rebuildDrawerList()
                        renderCurrentConversation()
                    }
                }

                itemRow.addView(tv)
                itemRow.addView(delBtn)
                listCol.addView(itemRow)
            }
            drawerPanel.addView(listScroll)
        }

        val drawerBtn = TextView(this).apply {
            text = "☰  Geçmiş"
            textSize = 15f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(20, 12, 25, 12)
            background = GradientDrawable().apply { cornerRadius = 30f; setColor(Color.parseColor("#1E1E28")) }
            setOnClickListener {
                rebuildDrawerList()
                drawerOverlay.visibility = View.VISIBLE
            }
        }

        val centerHeaderTitle = TextView(this).apply {
            text = "$aiName ($modelBadge)"
            textSize = 16f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val newChatTopBtn = TextView(this).apply {
            text = "＋ Yeni"
            textSize = 14f
            setTextColor(Color.BLACK)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(28, 12, 28, 12)
            background = GradientDrawable().apply { cornerRadius = 30f; setColor(Color.WHITE) }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { rightMargin = 16 }
            setOnClickListener {
                AppConfig.createNewConversation(this@MainActivity)
                renderCurrentConversation()
            }
        }

        val backOrbBtn = TextView(this).apply {
            text = "Küre"
            textSize = 14f
            setTextColor(Color.parseColor("#D4D4D8"))
            setPadding(24, 12, 24, 12)
            background = GradientDrawable().apply { cornerRadius = 30f; setColor(Color.parseColor("#1E1E28")) }
            setOnClickListener { showMinimalistHome() }
        }

        header.addView(drawerBtn)
        header.addView(centerHeaderTitle)
        header.addView(newChatTopBtn)
        header.addView(backOrbBtn)

        renderCurrentConversation()
        onNewMessageCallback = { runOnUiThread { renderCurrentConversation() } }

        val inputPill = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(35, 16, 22, 16)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(30, 12, 30, 30)
            }
            background = GradientDrawable().apply {
                cornerRadius = 70f
                setColor(Color.parseColor("#181822"))
                setStroke(2, Color.parseColor("#3F3F4E"))
            }
        }

        val chatInput = EditText(this).apply {
            hint = "+ $aiName'a sorun..."
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
                Handler(Looper.getMainLooper()).postDelayed({ renderCurrentConversation() }, 200)
            }
        }

        chatInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                submitTypedMessage()
                true
            } else false
        }

        val micBtn = GeminiMicButtonView(this).apply {
            layoutParams = LinearLayout.LayoutParams(76, 76).apply { rightMargin = 16 }
            setOnClickListener { startAssistantService("WAKE_NOW") }
        }

        val sendBtn = TextView(this).apply {
            text = "Gönder"
            textSize = 14f
            setTextColor(Color.BLACK)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(32, 18, 32, 18)
            background = GradientDrawable().apply { cornerRadius = 50f; setColor(Color.WHITE) }
            setOnClickListener { submitTypedMessage() }
        }

        inputPill.addView(chatInput)
        inputPill.addView(micBtn)
        inputPill.addView(sendBtn)

        mainCol.addView(header)
        mainCol.addView(messagesScroll)
        mainCol.addView(inputPill)

        outerFrame.addView(mainCol)
        outerFrame.addView(drawerOverlay)
        rootContainer.addView(outerFrame)

        if (focusKeyboard) {
            chatInput.requestFocus()
        }
    }

    private fun showSettingsScreen() {
        sphereView = null
        onNewMessageCallback = null
        rootContainer.removeAllViews()

        val scroll = ScrollView(this).apply { setBackgroundColor(Color.parseColor("#08080A")) }
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(45, 50, 45, 80) }

        val backTv = TextView(this).apply {
            text = "←   Ayarlar & Kontrol Merkezi"
            textSize = 20f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, 25)
            setOnClickListener {
                startAssistantService("RELOAD_CONFIG")
                showMinimalistHome()
            }
        }
        col.addView(backTv)

        fun sectionHeader(title: String): TextView = TextView(this).apply {
            text = title
            textSize = 13f
            setTextColor(Color.parseColor("#8E8E93"))
            typeface = Typeface.DEFAULT_BOLD
            setPadding(10, 30, 10, 12)
        }

        fun optionCard(label: String, rightBadge: String?, isSelected: Boolean, onClick: () -> Unit): View {
            return LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(38, 30, 38, 30)
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = 14 }
                background = GradientDrawable().apply {
                    cornerRadius = 28f
                    setColor(Color.parseColor("#121217"))
                    setStroke(if (isSelected) 3 else 1, if (isSelected) Color.WHITE else Color.parseColor("#24242E"))
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
                        setTextColor(Color.parseColor("#E4E4E7"))
                        setPadding(22, 8, 22, 8)
                        background = GradientDrawable().apply {
                            cornerRadius = 18f
                            setColor(Color.parseColor("#272732"))
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
        col.addView(optionCard("🤖 Asistan Adı: ${AppConfig.getName(this)}", "Değiştir", false) { showRenameDialog() })
        col.addView(optionCard("👤 Sizin Adınız: ${AppConfig.getUserName(this)}", "Değiştir", false) { showRenameDialog() })
        col.addView(optionCard("🔑 API Anahtarı (AIza... veya AQ...)", "Düzenle", false) { showApiKeyDialog() })
        col.addView(optionCard("🖼️ Uygulama Simgesi (Galeriden)", "Seç", false) {
            val pick = Intent(Intent.ACTION_GET_CONTENT).apply { type = "image/*" }
            startActivityForResult(Intent.createChooser(pick, "Simge Seçin"), REQ_PICK_ICON)
        })

        col.addView(sectionHeader("Yapay Zeka Modeli"))
        val curModel = AppConfig.getModelChoice(this)
        val models = listOf(
            "3.2_FLASH" to "⚡ 3.2 Flash (Hızlı Yanıt)",
            "HYDROGEN" to "🔥 Hydrogen (Doğal Sohbet)",
            "ARGON" to "💎 Argon (Derin Analiz)"
        )
        models.forEach { (mCode, mLabel) ->
            col.addView(optionCard(mLabel, mCode, curModel == mCode) {
                AppConfig.setModelChoice(this, mCode)
                startAssistantService("RELOAD_CONFIG")
                showSettingsScreen()
            })
        }

        col.addView(sectionHeader("Ses Tonu (Test Etmek İçin Seçin)"))
        val curVoice = AppConfig.getVoiceProfile(this)
        val voiceProfiles = listOf(
            0 to "👩  Kız Sesi 1 (Doğal Kadın Sesi)",
            1 to "👩‍🎤  Kız Sesi 2 (Canlı Asistan Sesi)",
            2 to "👨  Erkek Sesi 1 (Doğal Erkek Sesi)",
            3 to "🧔  Erkek Sesi 2 (Derin Stüdyo Sesi)"
        )
        voiceProfiles.forEach { (idx, name) ->
            col.addView(optionCard(name, "Test", curVoice == idx) {
                AppConfig.setVoiceProfile(this, idx)
                startAssistantService("TEST_VOICE")
                showSettingsScreen()
            })
        }

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

        col.addView(sectionHeader("Dil"))
        val curLang = AppConfig.getLang(this)
        val langOptions = listOf(
            "🇹🇷   Türkçe" to "tr-TR",
            "🇺🇸   İngilizce" to "en-US",
            "🇸🇦   Arapça" to "ar-SA",
            "🇷🇺   Rusça" to "ru-RU"
        )
        langOptions.forEach { (title, code) ->
            col.addView(optionCard(title, null, curLang == code) {
                AppConfig.setLang(this, code)
                startAssistantService("RELOAD_CONFIG")
                showSettingsScreen()
            })
        }

        scroll.addView(col)
        rootContainer.addView(scroll)
    }

    private fun showRenameDialog() {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(50, 30, 50, 10) }
        val aiInput = EditText(this).apply {
            setText(AppConfig.getName(this@MainActivity))
            hint = "Asistan Adı (Örn: Duman)"
            setTextColor(Color.WHITE); setHintTextColor(Color.GRAY)
        }
        val userInput = EditText(this).apply {
            setText(AppConfig.getUserName(this@MainActivity))
            hint = "Sizin Adınız (Örn: Abdullah)"
            setTextColor(Color.WHITE); setHintTextColor(Color.GRAY)
        }
        box.addView(aiInput)
        box.addView(userInput)

        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("İsimleri Güncelle")
            .setView(box)
            .setPositiveButton("Kaydet") { _, _ ->
                AppConfig.setName(this, aiInput.text.toString().trim().ifEmpty { "Duman" })
                AppConfig.setUserName(this, userInput.text.toString().trim().ifEmpty { "Abdullah" })
                startAssistantService("RELOAD_CONFIG")
                showSettingsScreen()
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    private fun showApiKeyDialog() {
        val input = EditText(this).apply {
            setText(AppConfig.getKey(this@MainActivity))
            hint = "API Anahtarı (AIza... veya AQ...)"
            setTextColor(Color.WHITE); setHintTextColor(Color.GRAY)
            setPadding(45, 35, 45, 35)
        }
        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("🔑 Gemini API Anahtarı")
            .setView(input)
            .setPositiveButton("Kaydet") { _, _ ->
                val k = input.text.toString().trim()
                if (k.isNotEmpty()) {
                    AppConfig.setKey(this, k)
                    startAssistantService("RELOAD_CONFIG")
                    Toast.makeText(this, "Kaydedildi!", Toast.LENGTH_SHORT).show()
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
                    FileOutputStream(outFile).use { out -> scaled.compress(Bitmap.CompressFormat.PNG, 100, out) }

                    if (ShortcutManagerCompat.isRequestPinShortcutSupported(this)) {
                        val shortcutIntent = Intent(this, MainActivity::class.java).apply { action = Intent.ACTION_MAIN }
                        val pinInfo = ShortcutInfoCompat.Builder(this, "ai_shortcut_${System.currentTimeMillis()}")
                            .setShortLabel(AppConfig.getName(this))
                            .setLongLabel(AppConfig.getName(this))
                            .setIcon(IconCompat.createWithBitmap(getCircularBitmap(scaled)))
                            .setIntent(shortcutIntent)
                            .build()
                        ShortcutManagerCompat.requestPinShortcut(this, pinInfo, null)
                    }
                    Toast.makeText(this, "Simge ana ekrana eklendi!", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {}
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

    private var wakeLock: PowerManager.WakeLock? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private lateinit var recognizerIntent: Intent
    private var tts: TextToSpeech? = null
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
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
        acquireWakeLock()

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
            "HIDE_PILL" -> {
                mainHandler.post {
                    isAwakeForCommand = false
                    rootOverlayLayout.visibility = View.GONE
                    imageCardView.visibility = View.GONE
                }
            }
            "APP_FOREGROUND" -> {
                if (!isAwakeForCommand && !isSpeaking) {
                    mainHandler.post { rootOverlayLayout.visibility = View.GONE }
                }
            }
            "APP_BACKGROUND" -> {
                if (!isListeningNow && !isSpeaking && !isProcessing) {
                    startListeningSafely()
                }
            }
            "TEXT_QUERY" -> {
                val q = intent.getStringExtra("EXTRA_TEXT") ?: ""
                if (q.isNotEmpty()) processFullCommand(q)
            }
            "TEST_VOICE" -> {
                applyVoiceAndLanguageConfig()
                speak("Merhaba ${AppConfig.getUserName(this)}, ben ${AppConfig.getName(this)}. Yeni ses tonum bu şekilde.")
            }
            "RELOAD_CONFIG" -> {
                applyVoiceAndLanguageConfig()
                mainHandler.post { pillPromptTv.text = "${AppConfig.getName(this)}'a sorun" }
            }
        }
        return START_STICKY
    }

    private fun acquireWakeLock() {
        if (wakeLock == null) {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "DumanAsistan::VoiceWakeLock")
            wakeLock?.acquire()
        }
    }

    private fun startAsForeground() {
        val channelId = "duman_voice_v10_channel"
        val aiName = AppConfig.getName(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "$aiName Arka Plan Servisi", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("$aiName Nöbette")
            .setContentText("Uygulama kapalıyken \"Hey $aiName\" diyebilirsiniz")
            .setSmallIcon(android.R.drawable.ic_menu_search)
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

        imageCardView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            setPadding(16, 16, 16, 16)
            layoutParams = LinearLayout.LayoutParams(520, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = 14 }
            background = GradientDrawable().apply {
                cornerRadius = 38f
                setColor(Color.parseColor("#E6121216"))
                setStroke(2, Color.parseColor("#80FFFFFF"))
            }
            setOnClickListener { visibility = View.GONE }
        }

        resultImageView = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 280)
            scaleType = ImageView.ScaleType.CENTER_CROP
        }

        imageCaptionTv = TextView(this).apply {
            textSize = 12f
            setTextColor(Color.WHITE)
            setPadding(12, 8, 12, 4)
        }

        imageCardView.addView(resultImageView)
        imageCardView.addView(imageCaptionTv)

        assistantPill = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(38, 20, 28, 20)
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 100f
                setColor(Color.parseColor("#D9141418"))
                setStroke(2, Color.parseColor("#99FFFFFF"))
            }
        }

        val plusSignTv = TextView(this).apply {
            text = "+"
            textSize = 24f
            setTextColor(Color.WHITE)
            setPadding(4, 0, 24, 0)
            setOnClickListener {
                toggleInlineTyping(true)
            }
        }

        pillPromptTv = TextView(this).apply {
            text = "${AppConfig.getName(this@OverlayService)}'a sorun"
            textSize = 15f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 0, 30, 0)
            maxWidth = 440
            setOnClickListener { toggleInlineTyping(true) }
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
                    if (typed.isNotEmpty()) processFullCommand(typed)
                    true
                } else false
            }
        }

        val geminiMicBtn = GeminiMicButtonView(this).apply {
            layoutParams = LinearLayout.LayoutParams(76, 76).apply { rightMargin = 18 }
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
                    wakeUpRightBottomPill("🎙️ Dinliyorum...")
                }
            }
        }

        waveView = GeminiWaveIconView(this).apply {
            layoutParams = LinearLayout.LayoutParams(58, 58).apply { rightMargin = 14 }
            setOnClickListener {
                toggleInlineTyping(false)
                tts?.stop()
                isSpeaking = false
                isAwakeForCommand = false
                rootOverlayLayout.visibility = View.GONE
            }
        }

        val closePillBtn = TextView(this).apply {
            text = "✕"
            textSize = 16f
            setTextColor(Color.parseColor("#A1A1AA"))
            setPadding(12, 6, 8, 6)
            setOnClickListener {
                tts?.stop()
                isSpeaking = false
                isProcessing = false
                isAwakeForCommand = false
                sentenceBuffer.clear()
                latestPartialChunk = ""
                rootOverlayLayout.visibility = View.GONE
                imageCardView.visibility = View.GONE
            }
        }

        assistantPill.addView(plusSignTv)
        assistantPill.addView(pillPromptTv)
        assistantPill.addView(pillInlineEdit)
        assistantPill.addView(geminiMicBtn)
        assistantPill.addView(waveView)
        assistantPill.addView(closePillBtn)

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
            x = 32
            y = 42
        }

        windowManager.addView(rootOverlayLayout, overlayParams)

        setupContinuousSpeechRecognizer()
        mainHandler.postDelayed({ startListeningSafely() }, 500)
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
            mainHandler.postDelayed(hideOverlayRunnable, 8000L)
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
                    mainHandler.post { waveView.activeWave = true; waveView.invalidate() }
                }
                override fun onDone(utteranceId: String?) {
                    isSpeaking = false
                    isProcessing = false
                    mainHandler.post { waveView.activeWave = false; waveView.invalidate() }
                    mainHandler.postDelayed(hideOverlayRunnable, 2800L)
                    mainHandler.postDelayed({ startListeningSafely() }, 250L)
                }
                override fun onError(utteranceId: String?) {
                    isSpeaking = false
                    isProcessing = false
                    mainHandler.postDelayed(hideOverlayRunnable, 1500L)
                    mainHandler.postDelayed({ startListeningSafely() }, 250L)
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
        val wantFemale = (profile == 0 || profile == 1)

        try {
            val langVoices = tts?.voices?.filter {
                it.locale.language.equals(locale.language, ignoreCase = true)
            } ?: emptyList()

            if (langVoices.isNotEmpty()) {
                val femaleList = langVoices.filter { v ->
                    val n = v.name.lowercase(Locale.US)
                    n.contains("female") || n.contains("cfs") || n.contains("efu") || n.contains("mfm") || n.contains("seda") || n.contains("yelda") || n.contains("wavenet-a") || n.contains("wavenet-c") || n.contains("wavenet-e")
                }.sortedByDescending { it.name.contains("network") }

                val maleList = langVoices.filter { v ->
                    val n = v.name.lowercase(Locale.US)
                    n.contains("male") && !n.contains("female") || n.contains("ama") || n.contains("tmc") || n.contains("iom") || n.contains("wavenet-b") || n.contains("wavenet-d")
                }.sortedByDescending { it.name.contains("network") }

                val selectedVoice: Voice? = if (wantFemale) {
                    if (femaleList.isNotEmpty()) femaleList[profile % femaleList.size] else langVoices.last()
                } else {
                    if (maleList.isNotEmpty()) maleList[(profile - 2).coerceAtLeast(0) % maleList.size] else langVoices.first()
                }

                if (selectedVoice != null) {
                    tts?.voice = selectedVoice
                }
            }
        } catch (e: Exception) {}

        when (profile) {
            0 -> { tts?.setPitch(1.04f); tts?.setSpeechRate(baseSpeed * 1.0f) }
            1 -> { tts?.setPitch(1.12f); tts?.setSpeechRate(baseSpeed * 1.05f) }
            2 -> { tts?.setPitch(0.94f); tts?.setSpeechRate(baseSpeed * 1.01f) }
            3 -> { tts?.setPitch(0.86f); tts?.setSpeechRate(baseSpeed * 0.98f) }
        }
    }

    private fun speak(text: String) {
        try { speechRecognizer?.cancel() } catch (e: Exception) {}
        isListeningNow = false
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "GeminiMasterUtterance")
    }

    private fun setupContinuousSpeechRecognizer() {
        mainHandler.post {
            speechRecognizer?.destroy()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(applicationContext)
            recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, AppConfig.getLang(this@OverlayService))
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1200L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1000L)
            }

            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) { isListeningNow = true }
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {
                    val lvl = if (rmsdB < 0) 0.5f else rmsdB.coerceIn(0.5f, 10f)
                    if (MainActivity.isAppInForeground) {
                        MainActivity.sphereView?.audioLevel = lvl
                    }
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
                        mainHandler.postDelayed({ startListeningSafely() }, 250L)
                    }
                }
                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        val partial = matches[0].trim()
                        if (partial.isEmpty()) return

                        val aiName = AppConfig.getName(this@OverlayService)
                        if (!isAwakeForCommand && containsWakeWordFuzzy(partial, aiName)) {
                            sentenceBuffer.clear()
                            val afterWake = stripWakeWord(partial, aiName)
                            latestPartialChunk = afterWake
                            wakeUpRightBottomPill(afterWake.ifEmpty { "🎙️ Dinliyorum..." })
                            resetFastSilenceTimer()
                        } else if (isAwakeForCommand) {
                            latestPartialChunk = stripWakeWord(partial, aiName)
                            val preview = (sentenceBuffer.toString() + " " + latestPartialChunk).trim()
                            pillPromptTv.text = preview.ifEmpty { "🎙️ Dinliyorum..." }
                            MainActivity.centerSubTv?.text = preview
                            resetFastSilenceTimer()
                        }
                    }
                }
                override fun onResults(results: Bundle?) {
                    isListeningNow = false
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val spoken = matches?.firstOrNull()?.trim() ?: ""
                    val aiName = AppConfig.getName(this@OverlayService)

                    if (!isAwakeForCommand && containsWakeWordFuzzy(spoken, aiName)) {
                        val clean = stripWakeWord(spoken, aiName)
                        sentenceBuffer.clear()
                        latestPartialChunk = ""
                        if (clean.isNotEmpty()) {
                            wakeUpRightBottomPill(clean)
                            processFullCommand(clean)
                        } else {
                            wakeUpRightBottomPill("🎙️ Seni dinliyorum...")
                            startListeningSafely()
                        }
                    } else if (isAwakeForCommand) {
                        val clean = stripWakeWord(spoken, aiName)
                        if (clean.isNotEmpty()) {
                            if (sentenceBuffer.isNotEmpty()) sentenceBuffer.append(" ")
                            sentenceBuffer.append(clean)
                        }
                        latestPartialChunk = ""
                        val combined = sentenceBuffer.toString().trim()
                        if (combined.isNotEmpty()) {
                            pillPromptTv.text = combined
                            MainActivity.centerSubTv?.text = combined
                            resetFastSilenceTimer()
                        } else {
                            startListeningSafely()
                        }
                    } else {
                        startListeningSafely()
                    }
                }
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }
    }

    private fun containsWakeWordFuzzy(spoken: String, aiName: String): Boolean {
        val lower = spoken.lowercase(Locale("tr", "TR"))
        val target = aiName.lowercase(Locale("tr", "TR")).trim()
        if (lower.contains(target)) return true
        if (target == "duman" && (lower.contains("tuman") || lower.contains("duyman") || lower.contains("dumanım") || lower.contains("duman'a") || lower.contains("dumana"))) {
            return true
        }
        return false
    }

    private fun stripWakeWord(text: String, aiName: String): String {
        val t = aiName.trim()
        return text.replace(Regex("(?i)hey\\s+$t"), "")
            .replace(Regex("(?i)hey\\s+tuman"), "")
            .replace(Regex("(?i)hey\\s+duyman"), "")
            .replace(Regex("(?i)$t"), "")
            .trim()
    }

    private fun resetFastSilenceTimer() {
        mainHandler.removeCallbacks(commitSpeechRunnable)
        if (AppConfig.getTalkMode(this) == "AUTO") {
            mainHandler.postDelayed(commitSpeechRunnable, 1250L)
        }
    }

    private fun startListeningSafely() {
        if (isSpeaking || isProcessing || isListeningNow) return
        mainHandler.post {
            try {
                recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, AppConfig.getLang(this))
                speechRecognizer?.startListening(recognizerIntent)
            } catch (e: Exception) {
                setupContinuousSpeechRecognizer()
                mainHandler.postDelayed({ startListeningSafely() }, 350L)
            }
        }
    }

    private fun processFullCommand(command: String) {
        isProcessing = true
        val lower = command.lowercase(Locale("tr", "TR"))

        AppConfig.addMessageToCurrentSession(this, "user", command)
        MainActivity.onNewMessageCallback?.invoke()

        mainHandler.post {
            if (!MainActivity.isAppInForeground) {
                rootOverlayLayout.visibility = View.VISIBLE
            }
            pillPromptTv.text = "⏳ $command"
            MainActivity.centerSubTv?.text = "⏳ $command"
        }

        val bankingKeywords = listOf(
            "banka", "garanti", "ziraat", "iş bankası", "iscep", "akbank", "yapı kredi",
            "qnb", "finansbank", "vakıfbank", "halkbank", "denizbank", "teb", "kuveyttürk",
            "enpara", "papara", "iban", "havale", "eft"
        )
        if (bankingKeywords.any { lower.contains(it) } && (lower.contains("aç") || lower.contains("gönder") || lower.contains("transfer"))) {
            replyAndShow("Güvenlik protokolü gereği bankacılık işlemlerine erişimim kapalıdır.", null)
            return
        }

        if (lower.contains("video") || lower.contains("izle") || lower.contains("youtube'da") || lower.contains("youtubeda") || lower.contains("şarkı aç") || lower.contains("müzik aç")) {
            val query = lower
                .replace("videosunu aç", "").replace("videosu aç", "").replace("video ara", "")
                .replace("videosunu bul", "").replace("video aç", "").replace("youtube'da", "")
                .replace("youtubeda", "").replace("izle", "").replace("ara", "").replace("aç", "")
                .trim().ifEmpty { command }
            searchAndPlayVideo(query)
            return
        }

        if ((lower.contains("google'da") || lower.contains("internette") || lower.contains("webde")) && lower.contains("ara")) {
            val q = lower.replace("google'da", "").replace("internette", "").replace("webde", "").replace("ara", "").trim()
            val url = "https://www.google.com/search?q=${URLEncoder.encode(q, "UTF-8")}"
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            replyAndShow("$q için internet araması açılıyor.", q)
            return
        }

        if (lower.endsWith(" aç") || lower.contains("uygulamasını aç")) {
            val targetApp = lower.replace("uygulamasını aç", "").replace("yi aç", "").replace("yı aç", "").replace("aç", "").trim()
            if (openAnyInstalledApp(targetApp)) return
        }

        askRealGeminiEngine(command, AppConfig.getKey(this))
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
        AppConfig.addMessageToCurrentSession(this, "assistant", answer)
        MainActivity.onNewMessageCallback?.invoke()

        mainHandler.post {
            if (!MainActivity.isAppInForeground) {
                rootOverlayLayout.visibility = View.VISIBLE
            }
            pillPromptTv.text = answer
            MainActivity.centerSubTv?.text = answer
            if (!imageKeyword.isNullOrBlank() && !MainActivity.isAppInForeground) {
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

        val req = Request.Builder().url(wikiUrl).header("User-Agent", "GeminiMaster/10.0").build()
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
                    if (thumbUrl != null) {
                        downloadAndDisplayBitmap(thumbUrl, pageTitle)
                    }
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

    private fun askRealGeminiEngine(prompt: String, rawKey: String) {
        val cleanKey = rawKey.trim()
        val aiName = AppConfig.getName(this)
        val userName = AppConfig.getUserName(this)
        val lang = AppConfig.getLang(this)
        val selectedMode = AppConfig.getModelChoice(this)

        val currentSessionId = AppConfig.getCurrentSessionId(this)
        val historyArr = AppConfig.getMessagesForSession(this, currentSessionId)
        val historyContext = StringBuilder()
        val startIdx = (historyArr.length() - 6).coerceAtLeast(0)
        for (i in startIdx until historyArr.length()) {
            val m = historyArr.getJSONObject(i)
            val r = if (m.getString("role") == "user") userName else aiName
            historyContext.append("$r: ${m.getString("text")}\n")
        }

        val (targetModel, styleInstruction, temp) = when (selectedMode) {
            "3.2_FLASH" -> Triple(
                "gemini-2.5-flash-lite",
                "Sen $aiName (3.2 Flash Motoru). Ultra hızlısın. Maksimum 1 tek cümleyle, anında ve doğrudan cevap ver.",
                0.5
            )
            "ARGON" -> Triple(
                "gemini-2.5-pro",
                "Sen $aiName (Argon Pro Analitik Motoru). Çok derin, zeki, kapsamlı ve bilge bir asistansın. Soruları tüm mantığıyla açıkla (2-4 cümle).",
                0.9
            )
            else -> Triple(
                "gemini-2.5-flash",
                "Sen $aiName (Hydrogen Motoru). Kullanıcın $userName ile çok doğal, samimi, sıcakkanlı bir insan gibi sohbet et (1-2 cümle).",
                0.7
            )
        }

        val fullPrompt = """
            $styleInstruction
            Dil: $lang.
            Eğer somut bir nesne, yer, hayvan veya kişi sorulursa cevabın sonuna [RESIM: Konu] ekle.
            Eğer video açılması istenirse [VIDEO: Konu] ekle.
            
            Önceki Sohbet:
            $historyContext
            $userName: $prompt
        """.trimIndent()

        val jsonBody = JSONObject().apply {
            put("contents", JSONArray().put(
                JSONObject().put("parts", JSONArray().put(
                    JSONObject().put("text", fullPrompt)
                ))
            ))
            put("generationConfig", JSONObject().apply {
                put("temperature", temp)
            })
        }

        executeGeminiWithFallbackModel(jsonBody, cleanKey, listOf(targetModel, "gemini-2.5-flash", "gemini-2.0-flash"), 0)
    }

    private fun executeGeminiWithFallbackModel(jsonBody: JSONObject, cleanKey: String, models: List<String>, idx: Int) {
        if (idx >= models.size) {
            replyAndShow("API anahtarınız geçersiz veya süresi dolmuş. Lütfen Ayarlar'dan AIza ile başlayan Gemini API anahtarınızı girin.", null)
            return
        }

        val modelName = models[idx]
        val isBearer = cleanKey.startsWith("AQ.") && !cleanKey.startsWith("AIza")

        val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$cleanKey"

        val reqBuilder = Request.Builder()
            .url(url)
            .addHeader("x-goog-api-key", cleanKey)
            .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))

        if (isBearer) {
            reqBuilder.addHeader("Authorization", "Bearer $cleanKey")
        }

        client.newCall(reqBuilder.build()).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                replyAndShow("İnternet bağlantınızı kontrol edin.", null)
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
                        executeGeminiWithFallbackModel(jsonBody, cleanKey, models, idx + 1)
                    }
                } else {
                    executeGeminiWithFallbackModel(jsonBody, cleanKey, models, idx + 1)
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
        try {
            if (wakeLock?.isHeld == true) wakeLock?.release()
        } catch (e: Exception) {}
        speechRecognizer?.destroy()
        tts?.shutdown()
        if (::rootOverlayLayout.isInitialized) {
            windowManager.removeView(rootOverlayLayout)
        }
    }
}
