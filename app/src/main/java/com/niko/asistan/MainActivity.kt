package com.niko.asistan

import android.Manifest
import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.accessibilityservice.GestureDescription
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
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.provider.AlarmClock
import android.provider.ContactsContract
import android.provider.MediaStore
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Base64
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
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
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

object AppConfig {
    const val PREFS = "AssistantFinalV15Prefs"
    const val DEFAULT_KEY = "AQ.Ab8RN6KZYPqSekpNWFEnuG42klSAK_TKxGeFNc1YbcduzUm0kA"

    fun isSetupDone(ctx: Context): Boolean =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean("SETUP_DONE", false)

    fun setSetupDone(ctx: Context, done: Boolean) =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean("SETUP_DONE", done).apply()

    fun getName(ctx: Context): String =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("AI_NAME", "")?.trim() ?: ""

    fun setName(ctx: Context, name: String) =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString("AI_NAME", name.trim()).apply()

    fun getUserName(ctx: Context): String =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("USER_NAME", "")?.trim() ?: ""

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

    // 0: Nova (Kadın 1 - 5.0★), 1: Shimmer (Kadın 2 - 4.9★), 2: Onyx (Erkek 1 - 5.0★), 3: Echo (Erkek 2 - 4.9★)
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
        val raw = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("CONVERSATIONS_V15", "[]") ?: "[]"
        return try { JSONArray(raw) } catch (e: Exception) { JSONArray() }
    }

    private fun saveConversations(ctx: Context, arr: JSONArray) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString("CONVERSATIONS_V15", arr.toString()).apply()
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

class AsistanOtonomService : AccessibilityService() {
    companion object {
        var instance: AsistanOtonomService? = null
        @Volatile var pendingAutoCapturePhoto = false
        @Volatile var pendingWhatsAppAutoSend = false
    }

    private val handler = Handler(Looper.getMainLooper())

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
            notificationTimeout = 250
        }
        serviceInfo = info
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!pendingAutoCapturePhoto && !pendingWhatsAppAutoSend) return
        val pkg = event?.packageName?.toString()?.lowercase(Locale.US) ?: return

        if (pendingWhatsAppAutoSend && pkg.contains("whatsapp")) {
            handler.postDelayed({
                if (pendingWhatsAppAutoSend && clickNodeByKeywords(listOf("send", "gönder", "com.whatsapp:id/send"))) {
                    pendingWhatsAppAutoSend = false
                }
            }, 850L)
        }

        if (pendingAutoCapturePhoto && (pkg.contains("camera") || pkg.contains("kamera"))) {
            pendingAutoCapturePhoto = false
            triggerCameraShutterNow()
        }
    }

    fun triggerCameraShutterNow() {
        handler.postDelayed({
            val clicked = clickNodeByKeywords(
                listOf("shutter", "capture", "çek", "take photo", "camera_shutter", "btn_shutter")
            )
            if (!clicked) {
                performScreenTapOnShutter()
            }
            pendingAutoCapturePhoto = false
        }, 2200L)
    }

    private fun clickNodeByKeywords(keywords: List<String>): Boolean {
        val root = rootInActiveWindow ?: return false
        return try {
            searchAndClickRecursive(root, keywords, 0)
        } catch (e: Exception) {
            false
        }
    }

    private fun searchAndClickRecursive(node: AccessibilityNodeInfo?, keywords: List<String>, depth: Int): Boolean {
        if (node == null || depth > 12) return false
        val desc = (node.contentDescription?.toString() ?: "").lowercase(Locale("tr", "TR"))
        val text = (node.text?.toString() ?: "").lowercase(Locale("tr", "TR"))
        val viewId = (node.viewIdResourceName ?: "").lowercase(Locale.US)

        for (kw in keywords) {
            if (desc.contains(kw) || text.contains(kw) || viewId.contains(kw)) {
                if (node.isClickable) {
                    node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    return true
                } else if (node.parent?.isClickable == true) {
                    node.parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    return true
                }
            }
        }
        for (i in 0 until node.childCount) {
            if (searchAndClickRecursive(node.getChild(i), keywords, depth + 1)) return true
        }
        return false
    }

    private fun performScreenTapOnShutter() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val dm = resources.displayMetrics
            val x = dm.widthPixels / 2f
            val y = dm.heightPixels * 0.88f
            val path = Path().apply { moveTo(x, y) }
            val stroke = GestureDescription.StrokeDescription(path, 0, 100)
            dispatchGesture(GestureDescription.Builder().addStroke(stroke).build(), null, null)
        }
    }

    override fun onInterrupt() {}
    override fun onDestroy() {
        super.onDestroy()
        instance = null
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

    private val points = Array(130) { i ->
        val numPoints = 130
        val goldenAngle = Math.PI * (3.0 - sqrt(5.0))
        val y = 1.0 - (i.toDouble() / (numPoints - 1)) * 2.0
        val radius = sqrt(1.0 - y * y)
        val theta = goldenAngle * i
        floatArrayOf((cos(theta) * radius).toFloat(), y.toFloat(), (sin(theta) * radius).toFloat())
    }

    init {
        setLayerType(LAYER_TYPE_HARDWARE, null)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!isShown || visibility != VISIBLE) return

        val cx = width / 2f
        val cy = height / 2f
        val baseRadius = (width.coerceAtMost(height) * 0.34f).coerceAtLeast(70f)

        rotY += if (isActive) 0.038f else 0.009f
        val pulse = if (isActive) (audioLevel * 4.0f).coerceIn(3f, 32f) else 0f

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
            val dotSize = depth * 3.8f + 1.4f
            canvas.drawCircle(cx + x1 * r, cy + y2 * r, dotSize, dotPaint)
        }
        postInvalidateDelayed(36L)
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
        requestBatteryOptimizationExemption()
        checkPermissionsAndProceed()
    }

    private fun requestBatteryOptimizationExemption() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
                if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    startActivity(intent)
                }
            } catch (e: Exception) {}
        }
    }

    override fun onResume() {
        super.onResume()
        isAppInForeground = true
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED && Settings.canDrawOverlays(this)) {
            if (AppConfig.isSetupDone(this) && AppConfig.getName(this).isNotEmpty()) {
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

    private fun checkPermissionsAndProceed() {
        val audioGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        val overlayGranted = Settings.canDrawOverlays(this)

        if (!audioGranted) {
            showPermissionGate("Mikrofon ve Rehber İzni", "Asistanın sesinizi duyması ve WhatsApp'tan kişilerinize mesaj atabilmesi için izinleri onaylayın.") {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_CONTACTS, Manifest.permission.CALL_PHONE),
                    1
                )
            }
        } else if (!overlayGranted) {
            showPermissionGate("Ekran Üstü İzni Gerekli", "Asistanın diğer uygulamaların üzerinde (sağ altta) görünebilmesi için bu izni vermelisiniz.") {
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_CONTACTS, Manifest.permission.CALL_PHONE), 2)
            }
            if (!AppConfig.isSetupDone(this) || AppConfig.getName(this).isEmpty()) {
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
            text = "Asistanınızın adını ve kendi adınızı belirleyin."
            textSize = 14f
            setTextColor(Color.parseColor("#8E8E93"))
            gravity = Gravity.CENTER
            setPadding(20, 14, 20, 35)
        }

        val aiNameInput = EditText(this).apply {
            hint = "Asistanın Adını Yazın..."
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
                bottomMargin = 22
            }
        }

        val userNameInput = EditText(this).apply {
            hint = "Kendi Adınızı Yazın..."
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
                val chosenAi = aiNameInput.text.toString().trim()
                val chosenUser = userNameInput.text.toString().trim()
                if (chosenAi.isEmpty()) {
                    Toast.makeText(this@MainActivity, "Lütfen asistana bir ad verin!", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                AppConfig.setName(this@MainActivity, chosenAi)
                AppConfig.setUserName(this@MainActivity, chosenUser.ifEmpty { "Dostum" })
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
        val aiName = AppConfig.getName(this).ifEmpty { "Asistan" }
        val userName = AppConfig.getUserName(this).ifEmpty { "Dostum" }

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
            setOnClickListener { showRenameDialog() }
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
            layoutParams = LinearLayout.LayoutParams(540, 540)
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
        val aiName = AppConfig.getName(this).ifEmpty { "Asistan" }
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
                    text = "Merhaba, ${AppConfig.getUserName(this@MainActivity).ifEmpty { "Dostum" }}"
                    textSize = 28f
                    setTextColor(Color.WHITE)
                    typeface = Typeface.create("serif", Typeface.NORMAL)
                    gravity = Gravity.CENTER
                }
                val sub = TextView(this).apply {
                    text = "İnternet araştırması yapabilir, YouTube videosu açabilir veya mesaj gönderebilirsin."
                    textSize = 15f
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
                text = "✦  Sohbet Geçmişi"
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
                Handler(Looper.getMainLooper()).postDelayed({ renderCurrentConversation() }, 150)
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

        col.addView(sectionHeader("Otonom Kontrol Kilidi Açma & Kimlik"))
        val isAutoEnabled = AsistanOtonomService.instance != null

        // "Erişim Reddedildi" (Kısıtlı Ayar) kilidini tek tuşla açmak için Uygulama Bilgileri kısayolu
        col.addView(optionCard(
            "🔓 1. Adım: Kısıtlı Ayar Kilidini Aç (Sağ üstteki ⋮ noktaya basıp 'Kısıtlı ayarlara izin ver' deyin)",
            "Kilidi Aç",
            false
        ) {
            try {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
                Toast.makeText(this, "Sağ üstteki 3 noktaya (⋮) basıp 'Kısıtlı ayarlara izin ver' seçeneğine dokunun", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {}
        })

        col.addView(optionCard(
            if (isAutoEnabled) "✅ 2. Adım: Otonom Kontrol Aktif (Fotoğraf Çekme & WhatsApp Mesaj)"
            else "⚡ 2. Adım: Otonom Kontrolü Etkinleştir (Erişilebilirlik)",
            if (isAutoEnabled) "Açık" else "Etkinleştir",
            isAutoEnabled
        ) {
            try {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            } catch (e: Exception) {}
        })

        col.addView(optionCard("🤖 Asistan Adı: ${AppConfig.getName(this)} (Hey ${AppConfig.getName(this)})", "Değiştir", false) { showRenameDialog() })
        col.addView(optionCard("👤 Sizin Adınız: ${AppConfig.getUserName(this)}", "Değiştir", false) { showRenameDialog() })
        col.addView(optionCard("🔑 API Anahtarı (AIza... veya AQ...)", "Düzenle", false) { showApiKeyDialog() })
        col.addView(optionCard("🖼️ Uygulama Simgesi (Galeriden)", "Seç", false) {
            val pick = Intent(Intent.ACTION_GET_CONTENT).apply { type = "image/*" }
            startActivityForResult(Intent.createChooser(pick, "Simge Seçin"), REQ_PICK_ICON)
        })

        col.addView(sectionHeader("En Yüksek Puanlı Stüdyo İnsan Sesleri (5.0★ HD Neural)"))
        val curVoice = AppConfig.getVoiceProfile(this)
        val voiceProfiles = listOf(
            0 to "👩  Nova / Emel HD (★ 5.0) — En Doğal & Sıcak Kadın Sesi",
            1 to "👩‍🎤  Shimmer / Seda HD (★ 4.9) — Berrak & Akıcı Kadın Sesi",
            2 to "👨  Onyx / Ahmet HD (★ 5.0) — Karizmatik & Net Erkek Sesi",
            3 to "🧔  Echo / Jarvis HD (★ 4.9) — Derin & Tok Stüdyo Erkek Sesi"
        )
        voiceProfiles.forEach { (idx, name) ->
            col.addView(optionCard(name, "Dinle", curVoice == idx) {
                AppConfig.setVoiceProfile(this, idx)
                startAssistantService("TEST_VOICE")
                showSettingsScreen()
            })
        }

        col.addView(optionCard("🎙️ Cihazdaki 500 Puanlı (Çok Yüksek Kalite) Ses Paketini İndir", "Google HD", false) {
            try {
                startActivity(Intent("com.android.settings.TTS_SETTINGS"))
                Toast.makeText(this, "⚙️ Ayarlar -> Ses verilerini yükle -> Türkçe kısmından HD sesleri indirin", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {}
        })

        col.addView(sectionHeader("Yapay Zeka Modeli"))
        val curModel = AppConfig.getModelChoice(this)
        val models = listOf(
            "3.2_FLASH" to "⚡ 3.2 Flash (Ultra Hızlı + Canlı Web Arama)",
            "HYDROGEN" to "🔥 Hydrogen (Dengeli Zeka + Görsel Üretim + YouTube)",
            "ARGON" to "💎 Argon (Derin Analiz + Kapsamlı Araştırma)"
        )
        models.forEach { (mCode, mLabel) ->
            col.addView(optionCard(mLabel, mCode, curModel == mCode) {
                AppConfig.setModelChoice(this, mCode)
                startAssistantService("RELOAD_CONFIG")
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
            hint = "Asistanın Adını Yazın..."
            setTextColor(Color.WHITE); setHintTextColor(Color.GRAY)
        }
        val userInput = EditText(this).apply {
            setText(AppConfig.getUserName(this@MainActivity))
            hint = "Kendi Adınızı Yazın..."
            setTextColor(Color.WHITE); setHintTextColor(Color.GRAY)
        }
        box.addView(aiInput)
        box.addView(userInput)

        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("İsimleri Güncelle")
            .setView(box)
            .setPositiveButton("Kaydet") { _, _ ->
                val newAi = aiInput.text.toString().trim()
                val newUser = userInput.text.toString().trim()
                if (newAi.isNotEmpty()) AppConfig.setName(this, newAi)
                if (newUser.isNotEmpty()) AppConfig.setUserName(this, newUser)
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
                            .setShortLabel(AppConfig.getName(this).ifEmpty { "Asistan" })
                            .setLongLabel(AppConfig.getName(this).ifEmpty { "Asistan" })
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

// ARKA PLAN SERVİSİ v15.0:
// 1) İkinci soruda "Dinliyorum..." yazıp algılamama hatasını %100 çözen Taze Mikrofon Oturumu (freshRestartMic) + AudioFocus Serbest Bırakıcı
// 2) Sesi robotikleştiren yapay PlaybackParams kaldırıldı -> OpenAI Audio (Nova/Shimmer/Onyx/Echo) + Google 500 Puanlı (QUALITY_VERY_HIGH) Saf Sesler
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
    private var audioFocusRequest: AudioFocusRequest? = null
    private var tts: TextToSpeech? = null
    private var mediaPlayer: MediaPlayer? = null
    private var isTtsReady = false
    private var pendingSpeechText: String? = null

    private val client = OkHttpClient.Builder()
        .connectTimeout(9, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()
    private val mainHandler = Handler(Looper.getMainLooper())

    private var isAwakeForCommand = false
    private var isSpeaking = false
    private var isProcessing = false
    private var isListeningNow = false
    private var lastMicStartTime = 0L

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

    // Her 3.5 saniyede bir mikrofonun durumunu kontrol eder; eğer 2. soruda takılmışsa anında taze oturum açar!
    private val micWatchdogRunnable = object : Runnable {
        override fun run() {
            val now = System.currentTimeMillis()
            if (!isSpeaking && !isProcessing && (!isListeningNow || (now - lastMicStartTime > 7000L))) {
                freshRestartMic()
            }
            mainHandler.postDelayed(this, 3500L)
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
                // Kullanıcı küreye veya mikrofona bastığında her şeyi sıfırla ve mikrofonu taze oturumla aç!
                isProcessing = false
                isSpeaking = false
                sentenceBuffer.clear()
                latestPartialChunk = ""
                imageCardView.visibility = View.GONE
                wakeUpRightBottomPill("🎙️ Dinliyorum...")
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
                if (!isSpeaking && !isProcessing) freshRestartMic()
            }
            "TEXT_QUERY" -> {
                val q = intent.getStringExtra("EXTRA_TEXT") ?: ""
                if (q.isNotEmpty()) processFullCommand(q)
            }
            "TEST_VOICE" -> {
                val p = AppConfig.getVoiceProfile(this)
                val vName = when (p) {
                    0 -> "Nova beş yıldızlı doğal kadın sesim"
                    1 -> "Shimmer berrak kadın sesim"
                    2 -> "Onyx beş yıldızlı karizmatik erkek sesim"
                    else -> "Echo derin stüdyo erkek sesim"
                }
                speakHighRatedVoice("Merhaba ${AppConfig.getUserName(this)}, ben ${AppConfig.getName(this)}. Şu an $vName ile konuşuyorum.")
            }
            "RELOAD_CONFIG" -> {
                applyHighestRatedVoiceToTts()
                mainHandler.post {
                    pillPromptTv.text = "${AppConfig.getName(this)}'a sorun"
                    startAsForeground()
                }
            }
        }
        return START_STICKY
    }

    private fun acquireWakeLock() {
        if (wakeLock == null) {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MasterAI::AlwaysOnMicV15")
            wakeLock?.acquire()
        }
    }

    private fun startAsForeground() {
        val channelId = "master_ai_v15_channel"
        val aiName = AppConfig.getName(this).ifEmpty { "Asistan" }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "$aiName Kesintisiz Dinleme", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("$aiName Aktif")
            .setContentText("\"Hey $aiName\" dediğinizde tek seferde açılır")
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
        tts = try {
            TextToSpeech(applicationContext, this, "com.google.android.tts")
        } catch (e: Exception) {
            TextToSpeech(applicationContext, this)
        }

        rootOverlayLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.END
            visibility = View.GONE
        }

        imageCardView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            setPadding(16, 16, 16, 16)
            layoutParams = LinearLayout.LayoutParams(540, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = 14 }
            background = GradientDrawable().apply {
                cornerRadius = 38f
                setColor(Color.parseColor("#E6121216"))
                setStroke(2, Color.parseColor("#80FFFFFF"))
            }
            setOnClickListener { visibility = View.GONE }
        }

        resultImageView = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 310)
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
            setOnClickListener { toggleInlineTyping(true) }
        }

        pillPromptTv = TextView(this).apply {
            text = "${AppConfig.getName(this@OverlayService).ifEmpty { "Asistan" }}'a sorun"
            textSize = 15f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 0, 30, 0)
            maxWidth = 440
            setOnClickListener {
                // Yazıya dokunulduğunda konuşmayı sustur ve hemen 2. soruyu dinlemeye başla!
                wakeUpRightBottomPill("🎙️ Dinliyorum...")
            }
        }

        pillInlineEdit = EditText(this).apply {
            hint = "${AppConfig.getName(this@OverlayService).ifEmpty { "Asistan" }}'a yazın..."
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
                wakeUpRightBottomPill("🎙️ Dinliyorum...")
            }
        }

        val closePillBtn = TextView(this).apply {
            text = "✕"
            textSize = 16f
            setTextColor(Color.parseColor("#A1A1AA"))
            setPadding(12, 6, 8, 6)
            setOnClickListener {
                stopAllAudioAndReleaseFocus()
                isProcessing = false
                isAwakeForCommand = false
                sentenceBuffer.clear()
                latestPartialChunk = ""
                rootOverlayLayout.visibility = View.GONE
                imageCardView.visibility = View.GONE
                freshRestartMic()
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

        mainHandler.postDelayed({ freshRestartMic() }, 300)
        mainHandler.postDelayed(micWatchdogRunnable, 3500L)
    }

    // HATA ÇÖZÜMÜ: Konuşma durduğunda ses odağını (AudioFocus) serbest bırakır ki mikrofon tekrar ses alabilsin!
    private fun stopAllAudioAndReleaseFocus() {
        try { tts?.stop() } catch (e: Exception) {}
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {}
        mediaPlayer = null
        isSpeaking = false

        try {
            val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest?.let { am.abandonAudioFocusRequest(it) }
            } else {
                @Suppress("DEPRECATION")
                am.abandonAudioFocus(null)
            }
        } catch (e: Exception) {}
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

    // İKİNCİ SORUDAKİ KİLİTLENMEYİ %100 ÇÖZEN UYANDIRICI:
    // Ses çıkışını kapatır, kilitli bayrakları (`isProcessing`, `isSpeaking`) sıfırlar ve mikrofonu sıfırdan taze açar!
    private fun wakeUpRightBottomPill(text: String) {
        mainHandler.post {
            mainHandler.removeCallbacks(hideOverlayRunnable)
            mainHandler.removeCallbacks(commitSpeechRunnable)
            stopAllAudioAndReleaseFocus()
            isProcessing = false
            isSpeaking = false
            isAwakeForCommand = true
            rootOverlayLayout.visibility = View.VISIBLE
            pillPromptTv.text = text
            MainActivity.sphereView?.isActive = true
            MainActivity.centerSubTv?.text = text
            freshRestartMic()
            mainHandler.postDelayed(hideOverlayRunnable, 12000L)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isTtsReady = true
            val audioAttrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
            tts?.setAudioAttributes(audioAttrs)
            applyHighestRatedVoiceToTts()

            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) { onSpeechStarted() }
                override fun onDone(utteranceId: String?) { onSpeechDone() }
                override fun onError(utteranceId: String?) { onSpeechDone() }
            })

            pendingSpeechText?.let {
                pendingSpeechText = null
                speakHighRatedVoice(it)
            }
        }
    }

    private fun onSpeechStarted() {
        isSpeaking = true
        MainActivity.sphereView?.isActive = true
        MainActivity.sphereView?.audioLevel = 4.2f
        mainHandler.post {
            waveView.activeWave = true
            waveView.invalidate()
        }
    }

    // Konuşma bittiği an ses odağını bırakır ve 2. soruyu hemen dinlemek için mikrofonu taze oturumla açar!
    private fun onSpeechDone() {
        mainHandler.post {
            stopAllAudioAndReleaseFocus()
            isProcessing = false
            waveView.activeWave = false
            waveView.invalidate()
            // Cevap bittikten sonra 6 saniye boyunca kullanıcı "Hey ..." demeden bile doğrudan 2. sorusunu sorabilsin!
            isAwakeForCommand = true
            pillPromptTv.text = "🎙️ Dinliyorum..."
            MainActivity.centerSubTv?.text = "🎙️ Dinliyorum..."
            freshRestartMic()
            mainHandler.removeCallbacks(hideOverlayRunnable)
            mainHandler.postDelayed(hideOverlayRunnable, 7000L)
        }
    }

    // EN YÜKSEK PUANLI GERÇEK İNSAN SES MOTORU:
    // 1. Öncelik: OpenAI Audio (gpt-4o-mini-audio-preview -> Nova, Shimmer, Onyx, Echo 5.0★ İnsan Sesleri)
    // 2. Öncelik: Cihazdaki 500 Tam Puanlı (Voice.QUALITY_VERY_HIGH) Saf Google Wavenet/Network Sesleri (Sesi bozan yapay filtreler kaldırıldı!)
    private fun speakHighRatedVoice(text: String) {
        val clean = text.replace(Regex("[*#_~`]"), "").trim()
        if (clean.isEmpty()) {
            onSpeechDone()
            return
        }

        mainHandler.post {
            try {
                speechRecognizer?.cancel()
                speechRecognizer?.destroy()
                speechRecognizer = null
            } catch (e: Exception) {}
            isListeningNow = false
        }
        stopAllAudioAndReleaseFocus()

        val profile = AppConfig.getVoiceProfile(this) // 0: Nova, 1: Shimmer, 2: Onyx, 3: Echo
        val speed = AppConfig.getSpeed(this)

        tryOpenAiHighRatedAudio(clean, profile) { openAiSuccess ->
            if (!openAiSuccess) {
                speakPure500QualityWavenet(clean, profile, speed)
            }
        }
    }

    // OpenAI gpt-4o-mini-audio-preview (Nova, Shimmer, Onyx, Echo - 5.0★ Gerçek İnsan Stüdyo Sesleri)
    private fun tryOpenAiHighRatedAudio(text: String, profile: Int, callback: (Boolean) -> Unit) {
        val voiceName = when (profile) {
            0 -> "nova"    // Kadın 1: Çok sıcak ve gerçekçi insan kadın sesi
            1 -> "shimmer" // Kadın 2: Berrak ve akıcı kadın sesi
            2 -> "onyx"    // Erkek 1: Tok ve karizmatik erkek sesi
            else -> "echo" // Erkek 2: Yumuşak ve doğal stüdyo erkek sesi
        }

        val payload = JSONObject().apply {
            put("model", "openai-audio")
            put("modalities", JSONArray().put("text").put("audio"))
            put("audio", JSONObject().apply {
                put("voice", voiceName)
                put("format", "mp3")
            })
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", "Sen bir seslendirme sanatçısısın. Kullanıcının verdiği metni hiçbir kelime eklemeden ve değiştirmeden, olduğu gibi çok doğal bir Türkçe tonlamayla seslendir.")
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", text.take(260))
                })
            })
        }

        val req = Request.Builder()
            .url("https://text.pollinations.ai/openai")
            .header("User-Agent", "Mozilla/5.0")
            .post(payload.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { callback(false) }
            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string() ?: ""
                if (response.isSuccessful && body.isNotEmpty()) {
                    try {
                        val audioObj = JSONObject(body)
                            .getJSONArray("choices").getJSONObject(0)
                            .getJSONObject("message").optJSONObject("audio")
                        val b64 = audioObj?.optString("data", "") ?: ""
                        if (b64.length > 500) {
                            val mp3Bytes = Base64.decode(b64, Base64.DEFAULT)
                            val mp3File = File(cacheDir, "high_rated_voice.mp3")
                            FileOutputStream(mp3File).use { it.write(mp3Bytes) }
                            playPureMp3File(mp3File)
                            callback(true)
                            return
                        }
                    } catch (e: Exception) {}
                }
                callback(false)
            }
        })
    }

    // Sesi bozan PlaybackParams olmadan saf MP3 oynatıcı
    private fun playPureMp3File(mp3File: File) {
        mainHandler.post {
            try {
                stopAllAudioAndReleaseFocus()
                val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK).build()
                    audioFocusRequest = req
                    am.requestAudioFocus(req)
                }
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(mp3File.absolutePath)
                    setOnPreparedListener { mp ->
                        onSpeechStarted()
                        mp.start()
                    }
                    setOnCompletionListener { onSpeechDone() }
                    setOnErrorListener { _, _, _ ->
                        onSpeechDone()
                        true
                    }
                    prepareAsync()
                }
            } catch (e: Exception) {
                onSpeechDone()
            }
        }
    }

    // Voice.QUALITY_VERY_HIGH (500 Puan) Filtreli Saf Google Stüdyo Sesi
    private fun applyHighestRatedVoiceToTts() {
        if (!isTtsReady) return
        val langCode = AppConfig.getLang(this)
        val locale = when {
            langCode.startsWith("ar") -> Locale("ar", "SA")
            langCode.startsWith("en") -> Locale("en", "US")
            langCode.startsWith("ru") -> Locale("ru", "RU")
            else -> Locale("tr", "TR")
        }
        tts?.language = locale

        val profile = AppConfig.getVoiceProfile(this)
        val speed = AppConfig.getSpeed(this)
        val wantFemale = (profile <= 1)

        try {
            // Tüm sesleri kalite puanına göre (QUALITY_VERY_HIGH = 500, QUALITY_HIGH = 400) sırala!
            val allVoices = tts?.voices?.filter {
                it.locale.language.equals(locale.language, ignoreCase = true)
            }?.sortedWith(
                compareByDescending<Voice> { it.quality }
                    .thenByDescending { it.name.contains("network", true) || it.name.contains("wavenet", true) }
                    .thenBy { it.latency }
            ) ?: emptyList()

            if (allVoices.isNotEmpty()) {
                val femaleVoices = allVoices.filter { v ->
                    val n = v.name.lowercase(Locale.US)
                    n.contains("female") || n.contains("cfs") || n.contains("efu") || n.contains("mfm") || n.contains("wavenet-a") || n.contains("wavenet-c") || n.contains("wavenet-e")
                }

                val maleVoices = allVoices.filter { v ->
                    val n = v.name.lowercase(Locale.US)
                    (n.contains("male") && !n.contains("female")) || n.contains("ama") || n.contains("iom") || n.contains("tmc") || n.contains("wavenet-b") || n.contains("wavenet-d")
                }

                val selected: Voice? = if (wantFemale) {
                    if (femaleVoices.isNotEmpty()) femaleVoices[profile % femaleVoices.size] else allVoices.firstOrNull()
                } else {
                    if (maleVoices.isNotEmpty()) maleVoices[(profile - 2).coerceAtLeast(0) % maleVoices.size] else allVoices.lastOrNull()
                }
                if (selected != null) tts?.voice = selected
            }
        } catch (e: Exception) {}

        // Sesi robotikleştiren aşırı Pitch oynamaları kaldırıldı! Saf 1.0f doğal insan frekansı:
        when (profile) {
            0 -> { tts?.setPitch(1.0f); tts?.setSpeechRate(speed * 1.0f) }
            1 -> { tts?.setPitch(1.04f); tts?.setSpeechRate(speed * 1.03f) }
            2 -> { tts?.setPitch(0.96f); tts?.setSpeechRate(speed * 1.0f) }
            else -> { tts?.setPitch(0.90f); tts?.setSpeechRate(speed * 0.98f) }
        }
    }

    private fun speakPure500QualityWavenet(clean: String, profile: Int, speed: Float) {
        mainHandler.post {
            if (!isTtsReady) {
                pendingSpeechText = clean
                return@post
            }
            val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK).build()
                audioFocusRequest = req
                am.requestAudioFocus(req)
            }
            applyHighestRatedVoiceToTts()
            val params = Bundle().apply { putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f) }
            tts?.speak(clean, TextToSpeech.QUEUE_FLUSH, params, "HighRatedUtterance_${System.currentTimeMillis()}")
        }
    }

    // HER YANIT SONRASI MİKROFONU TERTEMİZ SIFIRDAN AÇAN MOTOR (2. soruda takılma sorununu %100 çözer!)
    private fun freshRestartMic() {
        if (isSpeaking || isProcessing) return
        mainHandler.post {
            try {
                speechRecognizer?.cancel()
                speechRecognizer?.destroy()
            } catch (e: Exception) {}
            speechRecognizer = null

            try {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(applicationContext)
                val recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, AppConfig.getLang(this@OverlayService))
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1000L)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 850L)
                }

                speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        isListeningNow = true
                        lastMicStartTime = System.currentTimeMillis()
                    }
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {
                        val lvl = if (rmsdB < 0) 0.5f else rmsdB.coerceIn(0.5f, 10f)
                        if (MainActivity.isAppInForeground) {
                            MainActivity.sphereView?.audioLevel = lvl
                        }
                        if (isAwakeForCommand) {
                            waveView.activeWave = lvl > 2.2f
                            waveView.invalidate()
                        }
                    }
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() { isListeningNow = false }
                    override fun onError(error: Int) {
                        isListeningNow = false
                        if (!isSpeaking && !isProcessing) {
                            mainHandler.postDelayed({ freshRestartMic() }, 120L)
                        }
                    }
                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) ?: return
                        if (matches.isEmpty()) return

                        val aiName = AppConfig.getName(this@OverlayService)
                        if (aiName.isEmpty()) return
                        val matchedCandidate = matches.firstOrNull { containsWakeWordSmart(it, aiName) }

                        if (!isAwakeForCommand && matchedCandidate != null) {
                            sentenceBuffer.clear()
                            val afterWake = stripWakeWordSmart(matchedCandidate, aiName)
                            latestPartialChunk = afterWake
                            isAwakeForCommand = true
                            rootOverlayLayout.visibility = View.VISIBLE
                            pillPromptTv.text = afterWake.ifEmpty { "🎙️ Dinliyorum..." }
                            MainActivity.centerSubTv?.text = afterWake.ifEmpty { "🎙️ Dinliyorum..." }
                            resetFastSilenceTimer()
                        } else if (isAwakeForCommand) {
                            val partial = matches[0].trim()
                            if (partial.isNotEmpty()) {
                                latestPartialChunk = stripWakeWordSmart(partial, aiName)
                                val preview = (sentenceBuffer.toString() + " " + latestPartialChunk).trim()
                                pillPromptTv.text = preview.ifEmpty { "🎙️ Dinliyorum..." }
                                MainActivity.centerSubTv?.text = preview
                                resetFastSilenceTimer()
                            }
                        }
                    }
                    override fun onResults(results: Bundle?) {
                        isListeningNow = false
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) ?: arrayListOf()
                        val aiName = AppConfig.getName(this@OverlayService)
                        if (aiName.isEmpty()) {
                            freshRestartMic()
                            return
                        }

                        val matchedWake = matches.firstOrNull { containsWakeWordSmart(it, aiName) }

                        if (!isAwakeForCommand && matchedWake != null) {
                            val clean = stripWakeWordSmart(matchedWake, aiName)
                            sentenceBuffer.clear()
                            latestPartialChunk = ""
                            if (clean.isNotEmpty()) {
                                isAwakeForCommand = false
                                processFullCommand(clean)
                            } else {
                                wakeUpRightBottomPill("🎙️ Seni dinliyorum...")
                            }
                        } else if (isAwakeForCommand) {
                            val spoken = matches.firstOrNull()?.trim() ?: ""
                            val clean = stripWakeWordSmart(spoken, aiName)
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
                                freshRestartMic()
                            }
                        } else {
                            freshRestartMic()
                        }
                    }
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })

                lastMicStartTime = System.currentTimeMillis()
                speechRecognizer?.startListening(recognizerIntent)
            } catch (e: Exception) {
                mainHandler.postDelayed({ freshRestartMic() }, 400L)
            }
        }
    }

    private fun normalizeTr(input: String): String {
        return input.lowercase(Locale("tr", "TR"))
            .replace('ı', 'i')
            .replace('ğ', 'g')
            .replace('ü', 'u')
            .replace('ş', 's')
            .replace('ö', 'o')
            .replace('ç', 'c')
            .replace(Regex("[^a-z0-9\\s]"), "")
    }

    private fun levenshteinSimilarity(s1: String, s2: String): Float {
        if (s1 == s2) return 1.0f
        if (s1.isEmpty() || s2.isEmpty()) return 0.0f
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j
        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = min(min(dp[i - 1][j] + 1, dp[i][j - 1] + 1), dp[i - 1][j - 1] + cost)
            }
        }
        val maxLen = max(s1.length, s2.length).toFloat()
        return 1.0f - (dp[s1.length][s2.length] / maxLen)
    }

    private fun containsWakeWordSmart(spoken: String, aiName: String): Boolean {
        val normSpoken = normalizeTr(spoken)
        val normTarget = normalizeTr(aiName.trim())
        if (normTarget.isEmpty()) return false
        if (normSpoken.contains(normTarget)) return true

        val words = normSpoken.split(" ").filter { it.isNotBlank() }
        for (w in words) {
            if (w.startsWith(normTarget) || levenshteinSimilarity(w, normTarget) >= 0.64f) {
                return true
            }
            if (w.length > normTarget.length && levenshteinSimilarity(w.take(normTarget.length), normTarget) >= 0.72f) {
                return true
            }
        }
        return false
    }

    private fun stripWakeWordSmart(text: String, aiName: String): String {
        val normTarget = normalizeTr(aiName.trim())
        if (normTarget.isEmpty()) return text.trim()
        val rawWords = text.trim().split(Regex("\\s+")).toMutableList()
        while (rawWords.isNotEmpty()) {
            val w = normalizeTr(rawWords[0])
            if (w == "hey" || w == "hi" || w == "ey" || w == "alo") {
                rawWords.removeAt(0)
                continue
            }
            if (w.contains(normTarget) || levenshteinSimilarity(w.take(normTarget.length.coerceAtLeast(1)), normTarget) >= 0.64f) {
                rawWords.removeAt(0)
                break
            }
            break
        }
        return rawWords.joinToString(" ").trim()
    }

    private fun resetFastSilenceTimer() {
        mainHandler.removeCallbacks(commitSpeechRunnable)
        if (AppConfig.getTalkMode(this) == "AUTO") {
            mainHandler.postDelayed(commitSpeechRunnable, 850L)
        }
    }

    private fun didUserExplicitlyAskForPhoto(lowerCmd: String): Boolean {
        val photoTriggers = listOf(
            "fotoğraf", "fotograf", "foto ", "fotosu", "resim", "resmi", "görsel", "gorsel",
            "çiz", "ciz", "fotoğrafıyla", "resmiyle", "görseliyle"
        )
        return photoTriggers.any { lowerCmd.contains(it) }
    }

    private fun processFullCommand(command: String) {
        isProcessing = true
        val lower = command.lowercase(Locale("tr", "TR")).trim()

        AppConfig.addMessageToCurrentSession(this, "user", command)
        MainActivity.onNewMessageCallback?.invoke()

        mainHandler.post {
            imageCardView.visibility = View.GONE
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

        if (lower.contains("fotoğraf çek") || lower.contains("foto çek") || lower.contains("resim çek") || lower.contains("selfie çek") || lower.contains("özçekim")) {
            capturePhotoAutomatically(isSelfie = lower.contains("selfie") || lower.contains("özçekim") || lower.contains("ön kamera"))
            return
        }

        if (lower.contains("resmi çiz") || lower.contains("resim çiz") || lower.contains("görsel oluştur") || lower.contains("görseli oluştur") || lower.contains("resmi oluştur") || lower.contains("fotoğrafını göster") || lower.contains("resmini göster")) {
            val imgPrompt = lower
                .replace("bana", "").replace("resmi çiz", "").replace("resim çiz", "")
                .replace("görsel oluştur", "").replace("görseli oluştur", "").replace("resmi oluştur", "")
                .replace("fotoğrafını göster", "").replace("resmini göster", "").trim()
                .ifEmpty { command }
            generateOrFetchImageInRightBottom(imgPrompt)
            return
        }

        if (lower.contains("whatsapp") || lower.contains("mesaj at") || lower.contains("mesaj gönder") || lower.contains("diye yaz")) {
            if (handleWhatsAppMessageCommand(command, lower)) return
        }

        if (lower.endsWith(" ara") && !lower.contains("internette") && !lower.contains("google") && !lower.contains("youtube") && !lower.contains("araştır")) {
            val targetPerson = lower.removeSuffix(" ara").replace("yi", "").replace("yı", "").replace("i", "").replace("ı", "").trim()
            if (targetPerson.isNotEmpty() && dialContactByName(targetPerson)) return
        }

        if (lower.contains("alarm kur") || lower.contains("alarmı kur")) {
            if (handleSetAlarm(lower)) return
        }

        if (lower.contains("video") || lower.contains("izle") || lower.contains("youtube") || lower.contains("şarkı aç") || lower.contains("müzik aç") || lower.contains("klip aç")) {
            val query = lower
                .replace("youtube'u açıp", "").replace("youtube açıp", "").replace("youtube'da", "").replace("youtubeda", "").replace("youtube", "")
                .replace("videosunu aç", "").replace("videosu aç", "").replace("video aç", "")
                .replace("şarkısını aç", "").replace("şarkı aç", "").replace("müzik aç", "")
                .replace("izle", "").replace("ara", "").replace("aç", "")
                .trim()
            if (query.isEmpty()) {
                openAnyInstalledApp("youtube")
            } else {
                findAndPlayYouTubeVideoDirectly(query)
            }
            return
        }

        if (lower.endsWith(" aç") || lower.contains("uygulamasını aç")) {
            if (lower.contains("kamera")) {
                try {
                    startActivity(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    replyAndShow("Kamera açılıyor.", null)
                    return
                } catch (e: Exception) {}
            }
            val cleanedApp = lower
                .replace("uygulamasını aç", "").replace("uygulamasini ac", "")
                .replace("'ı aç", "").replace("'i aç", "").replace("'u aç", "").replace("'ü aç", "")
                .replace("yi aç", "").replace("yı aç", "").replace("yu aç", "").replace("yü aç", "")
                .replace("ı aç", "").replace("i aç", "").replace("u aç", "").replace("ü aç", "")
                .replace("aç", "").trim()
            if (cleanedApp.isNotEmpty() && openAnyInstalledApp(cleanedApp)) return
        }

        val allowPhoto = didUserExplicitlyAskForPhoto(lower)
        val isSimpleGreeting = listOf("merhaba", "selam", "nasılsın", "naber", "günaydın", "iyi akşamlar", "iyi geceler", "adın ne", "kimsin", "saat kaç").any { lower == it || lower.startsWith("$it ") }

        if (isSimpleGreeting) {
            askMultiEngineAI(command, "", allowPhoto)
        } else {
            fetchLiveWebContext(command) { webContext ->
                askMultiEngineAI(command, webContext, allowPhoto)
            }
        }
    }

    private fun capturePhotoAutomatically(isSelfie: Boolean) {
        try {
            AsistanOtonomService.pendingAutoCapturePhoto = true
            val camIntent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (isSelfie) {
                    putExtra("android.intent.extras.CAMERA_FACING", 1)
                    putExtra("android.intent.extras.LENS_FACING_FRONT", 1)
                    putExtra("android.intent.extra.USE_FRONT_CAMERA", true)
                }
            }
            startActivity(camIntent)
            replyAndShow("Kamera açılıyor, 2 saniye içinde fotoğrafı çekiyorum.", null)
            AsistanOtonomService.instance?.triggerCameraShutterNow()
        } catch (e: Exception) {
            replyAndShow("Kamera başlatılamadı.", null)
        }
    }

    private fun findAndPlayYouTubeVideoDirectly(rawQuery: String) {
        var searchQuery = rawQuery
        val lowerQ = rawQuery.lowercase(Locale("tr", "TR"))
        if (lowerQ.contains("en çok izlenen") || lowerQ.contains("en popüler") || lowerQ.contains("edok izlenen")) {
            searchQuery = "$rawQuery most viewed popular video"
        }

        replyAndShow("\"$rawQuery\" videosu doğrudan açılıyor...", null)

        val encoded = URLEncoder.encode(searchQuery, "UTF-8")
        val ytUrl = "https://www.youtube.com/results?search_query=$encoded"
        val req = Request.Builder()
            .url(ytUrl)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36")
            .header("Accept-Language", "tr-TR,tr;q=0.9,en-US;q=0.8")
            .build()

        client.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                openYouTubeFallbackSearch(rawQuery)
            }
            override fun onResponse(call: Call, response: Response) {
                val html = response.body?.string() ?: ""
                val match = Regex("\"videoId\":\"([a-zA-Z0-9_-]{11})\"").find(html)
                val videoId = match?.groupValues?.get(1)

                if (!videoId.isNullOrEmpty()) {
                    mainHandler.post {
                        try {
                            val watchIntent = Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:$videoId")).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            startActivity(watchIntent)
                        } catch (e: Exception) {
                            val webWatch = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=$videoId")).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            startActivity(webWatch)
                        }
                    }
                } else {
                    openYouTubeFallbackSearch(rawQuery)
                }
            }
        })
    }

    private fun openYouTubeFallbackSearch(query: String) {
        mainHandler.post {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=$encoded")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        }
    }

    private fun generateOrFetchImageInRightBottom(prompt: String) {
        replyAndShow("\"$prompt\" görseli hazırlanıyor.", null)
        val encoded = URLEncoder.encode(prompt, "UTF-8")
        val aiImageUrl = "https://image.pollinations.ai/prompt/$encoded?width=540&height=340&nologo=true&seed=${System.currentTimeMillis() % 10000}"

        downloadAndDisplayBitmap(aiImageUrl, prompt) { success ->
            if (!success) {
                loadTopicPhotoIntoRightBottomCard(prompt)
            }
        }
    }

    private fun handleWhatsAppMessageCommand(originalCmd: String, lower: String): Boolean {
        val stripped = lower.replace("whatsapp'ı", "").replace("whatsappı", "").replace("whatsapp", "").replace("aç", "").trim()
        if (stripped.isEmpty()) {
            return openAnyInstalledApp("whatsapp")
        }

        var cleaned = lower
            .replace("whatsapp'tan", "").replace("whatsapptan", "").replace("whatsapp üzerinden", "")
            .replace("whatsapp açıp", "").replace("whatsapp'ı açıp", "").replace("whatsapp", "")
            .replace("mesaj at", "").replace("mesaj gönder", "").replace("mesajı at", "")
            .replace("diye yaz", "").replace("yaz", "").replace("söyle", "").trim()

        val words = cleaned.split(" ").filter { it.isNotBlank() }
        var contactQuery = ""
        var messageBody = ""

        if (words.isNotEmpty()) {
            val firstWord = words[0]
            contactQuery = when {
                firstWord.startsWith("annem") || firstWord.startsWith("anne") -> "anne"
                firstWord.startsWith("babam") || firstWord.startsWith("baba") -> "baba"
                firstWord.startsWith("ablam") || firstWord.startsWith("abla") -> "abla"
                firstWord.startsWith("abim") || firstWord.startsWith("abi") -> "abi"
                firstWord.startsWith("kardeşim") -> "kardeş"
                firstWord.endsWith("ye") || firstWord.endsWith("ya") -> firstWord.dropLast(2)
                firstWord.endsWith("e") || firstWord.endsWith("a") -> firstWord.dropLast(1)
                else -> firstWord
            }
            messageBody = words.drop(1).joinToString(" ").replace("diye", "").trim()
        }
        if (messageBody.isEmpty()) messageBody = "Merhaba"

        val phoneNumber = findPhoneNumberInContacts(contactQuery)
        return sendWhatsAppDirect(phoneNumber, contactQuery, messageBody)
    }

    private fun findPhoneNumberInContacts(nameQuery: String): String? {
        if (nameQuery.isEmpty()) return null
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            return null
        }
        try {
            val normQuery = normalizeTr(nameQuery)
            val cursor = contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER),
                null, null, null
            )
            cursor?.use {
                while (it.moveToNext()) {
                    val displayName = it.getString(0) ?: ""
                    val number = it.getString(1) ?: ""
                    if (normalizeTr(displayName).contains(normQuery)) {
                        var digits = number.replace(Regex("[^0-9+]"), "")
                        if (digits.startsWith("05")) digits = "9$digits"
                        else if (digits.startsWith("5") && digits.length == 10) digits = "90$digits"
                        return digits
                    }
                }
            }
        } catch (e: Exception) {}
        return null
    }

    private fun sendWhatsAppDirect(phoneNumber: String?, personName: String, message: String): Boolean {
        return try {
            AsistanOtonomService.pendingWhatsAppAutoSend = true
            if (!phoneNumber.isNullOrEmpty()) {
                val uri = Uri.parse("https://api.whatsapp.com/send?phone=$phoneNumber&text=${URLEncoder.encode(message, "UTF-8")}")
                val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                    setPackage("com.whatsapp")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(intent)
                replyAndShow("$personName kişisine \"$message\" mesajı gönderiliyor.", null)
            } else {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    setPackage("com.whatsapp")
                    putExtra(Intent.EXTRA_TEXT, message)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(intent)
                replyAndShow("WhatsApp açıldı, \"$message\" mesajı için kişiyi seçebilirsin.", null)
            }
            true
        } catch (e: Exception) {
            openAnyInstalledApp("whatsapp")
        }
    }

    private fun dialContactByName(personName: String): Boolean {
        val phone = findPhoneNumberInContacts(personName)
        return if (!phone.isNullOrEmpty()) {
            try {
                val callIntent = if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                    Intent(Intent.ACTION_CALL, Uri.parse("tel:$phone"))
                } else {
                    Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                }
                callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(callIntent)
                replyAndShow("$personName aranıyor.", null)
                true
            } catch (e: Exception) { false }
        } else false
    }

    private fun handleSetAlarm(lower: String): Boolean {
        val nums = Regex("(\\d{1,2})").findAll(lower).map { it.value.toInt() }.toList()
        if (nums.isNotEmpty()) {
            val hour = nums[0].coerceIn(0, 23)
            val min = if (nums.size > 1) nums[1].coerceIn(0, 59) else 0
            try {
                val alarmIntent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                    putExtra(AlarmClock.EXTRA_HOUR, hour)
                    putExtra(AlarmClock.EXTRA_MINUTES, min)
                    putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(alarmIntent)
                replyAndShow("Saat ${String.format("%02d:%02d", hour, min)} için alarm kuruldu.", null)
                return true
            } catch (e: Exception) {}
        }
        return false
    }

    private fun openAnyInstalledApp(targetName: String): Boolean {
        val normTarget = normalizeTr(targetName)
        if (normTarget.isEmpty()) return false

        if (normTarget.contains("ayarlar")) {
            startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            replyAndShow("Ayarlar açılıyor.", null)
            return true
        }

        val knownPackages = mapOf(
            "whatsapp" to "com.whatsapp",
            "youtube" to "com.google.android.youtube",
            "instagram" to "com.instagram.android",
            "spotify" to "com.spotify.music",
            "chrome" to "com.android.chrome",
            "harita" to "com.google.android.apps.maps",
            "galeri" to "com.sec.android.gallery3d",
            "saat" to "com.sec.android.app.clockpackage",
            "hesap makinesi" to "com.sec.android.app.popupcalculator"
        )
        for ((k, pkgName) in knownPackages) {
            if (normTarget.contains(normalizeTr(k))) {
                val launch = packageManager.getLaunchIntentForPackage(pkgName)
                if (launch != null) {
                    launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(launch)
                    replyAndShow("$targetName açılıyor.", null)
                    return true
                }
            }
        }

        val pm = packageManager
        val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        for (app in packages) {
            val label = normalizeTr(pm.getApplicationLabel(app).toString())
            val pkg = app.packageName.lowercase(Locale.US)
            if (label.contains(normTarget) || pkg.contains(normTarget)) {
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
            rootOverlayLayout.visibility = View.VISIBLE
            pillPromptTv.text = answer
            MainActivity.centerSubTv?.text = answer
            if (!imageKeyword.isNullOrBlank()) {
                loadTopicPhotoIntoRightBottomCard(imageKeyword)
            } else {
                imageCardView.visibility = View.GONE
            }
            speakHighRatedVoice(answer)
        }
    }

    private fun loadTopicPhotoIntoRightBottomCard(keyword: String) {
        val cleanKw = keyword.trim()
        val langPrefix = AppConfig.getLang(this).take(2).lowercase(Locale.US)
        val wikiUrl = "https://$langPrefix.wikipedia.org/w/api.php?action=query&prop=pageimages&format=json&piprop=thumbnail&pithumbsize=500&generator=search&gsrsearch=${URLEncoder.encode(cleanKw, "UTF-8")}&gsrlimit=1"

        val req = Request.Builder().url(wikiUrl).header("User-Agent", "GeminiMaster/15.0").build()
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
                    val finalUrl = thumbUrl ?: "https://tse2.mm.bing.net/th?q=${URLEncoder.encode(cleanKw, "UTF-8")}&w=480&h=300&c=7&rs=1"
                    downloadAndDisplayBitmap(finalUrl, pageTitle) {}
                } catch (e: Exception) {}
            }
        })
    }

    private fun downloadAndDisplayBitmap(imgUrl: String, caption: String, onDone: (Boolean) -> Unit) {
        val req = Request.Builder().url(imgUrl).header("User-Agent", "Mozilla/5.0").build()
        client.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { onDone(false) }
            override fun onResponse(call: Call, response: Response) {
                val bytes = response.body?.bytes()
                if (bytes == null || bytes.size < 500) {
                    onDone(false)
                    return
                }
                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                if (bmp == null) {
                    onDone(false)
                    return
                }
                mainHandler.post {
                    rootOverlayLayout.visibility = View.VISIBLE
                    resultImageView.setImageBitmap(bmp)
                    imageCaptionTv.text = "📸 $caption"
                    imageCardView.visibility = View.VISIBLE
                }
                onDone(true)
            }
        })
    }

    private fun fetchLiveWebContext(query: String, callback: (String) -> Unit) {
        val cleanQ = query
            .replace("araştır", "").replace("internette", "").replace("google'da", "")
            .replace("nedir", "").replace("kimdir", "").replace("ne kadar", "").trim()
            .ifEmpty { query }

        val ddgUrl = "https://html.duckduckgo.com/html/?q=${URLEncoder.encode(cleanQ, "UTF-8")}"
        val req = Request.Builder()
            .url(ddgUrl)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .build()

        client.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                fetchWikipediaSnippet(cleanQ, callback)
            }
            override fun onResponse(call: Call, response: Response) {
                val html = response.body?.string() ?: ""
                val snippets = Regex("class=\"result__snippet\"[^>]*>(.*?)</a>", RegexOption.DOT_MATCHES_ALL)
                    .findAll(html)
                    .map { it.groupValues[1].replace(Regex("<[^>]*>"), "").replace("&quot;", "\"").replace("&#x27;", "'").replace("&amp;", "&").trim() }
                    .filter { it.length > 20 }
                    .take(3)
                    .toList()

                if (snippets.isNotEmpty()) {
                    callback(snippets.joinToString(" | "))
                } else {
                    fetchWikipediaSnippet(cleanQ, callback)
                }
            }
        })
    }

    private fun fetchWikipediaSnippet(cleanQ: String, callback: (String) -> Unit) {
        val wikiUrl = "https://tr.wikipedia.org/w/api.php?action=query&list=search&srsearch=${URLEncoder.encode(cleanQ, "UTF-8")}&utf8=&format=json&srlimit=2"
        client.newCall(Request.Builder().url(wikiUrl).header("User-Agent", "MasterAI/15.0").build()).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { callback("") }
            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string() ?: ""
                try {
                    val searchArr = JSONObject(body).optJSONObject("query")?.optJSONArray("search")
                    if (searchArr != null && searchArr.length() > 0) {
                        val sb = StringBuilder()
                        for (i in 0 until searchArr.length()) {
                            val item = searchArr.getJSONObject(i)
                            val snip = item.optString("snippet", "").replace(Regex("<[^>]*>"), "")
                            sb.append(item.optString("title")).append(": ").append(snip).append(". ")
                        }
                        callback(sb.toString().trim())
                        return
                    }
                } catch (e: Exception) {}
                callback("")
            }
        })
    }

    private fun askMultiEngineAI(prompt: String, liveWebInfo: String, allowPhoto: Boolean) {
        val rawKey = AppConfig.getKey(this).trim()
        val aiName = AppConfig.getName(this).ifEmpty { "Asistan" }
        val userName = AppConfig.getUserName(this).ifEmpty { "Dostum" }
        val selectedMode = AppConfig.getModelChoice(this)

        val currentSessionId = AppConfig.getCurrentSessionId(this)
        val historyArr = AppConfig.getMessagesForSession(this, currentSessionId)
        val historyContext = StringBuilder()
        val startIdx = (historyArr.length() - 5).coerceAtLeast(0)
        for (i in startIdx until historyArr.length()) {
            val m = historyArr.getJSONObject(i)
            val r = if (m.getString("role") == "user") userName else aiName
            historyContext.append("$r: ${m.getString("text")}\n")
        }

        val photoRule = if (allowPhoto) {
            "- Kullanıcı görsel/fotoğraf istediği için cevabın sonuna [RESIM: KonuBaşlığı] ekle."
        } else {
            "- KESİNLİKLE [RESIM: ...] etiketi EKLEME! Sadece konuşarak cevap ver."
        }

        val systemRules = """
            Senin adın $aiName ($selectedMode motoru). Kullanıcının adı $userName.
            Çok zeki, samimi, her konuyu bilen sesli asistansın. Doğal ve net Türkçe cevap ver (1-3 cümle).
            ${if (liveWebInfo.isNotEmpty()) "Canlı İnternet Bilgisi: $liveWebInfo" else ""}
            
            KURALLAR:
            - YouTube'da video açmak istenirse: [VIDEO: AramaTerimi]
            - WhatsApp mesajı atmak istenirse: [WHATSAPP: KişiAdı | MesajMetni]
            - Uygulama açmak istenirse: [UYGULAMA_AC: UygulamaAdı]
            $photoRule
            
            Geçmiş:
            $historyContext
            $userName: $prompt
        """.trimIndent()

        tryGeminiOfficialAPI(systemRules, rawKey, allowPhoto) { geminiSuccess ->
            if (!geminiSuccess) {
                tryCloudAIEnginePost(systemRules, prompt, allowPhoto) { postSuccess ->
                    if (!postSuccess) {
                        tryCloudAIEngineGet(systemRules, prompt, liveWebInfo, allowPhoto)
                    }
                }
            }
        }
    }

    private fun tryGeminiOfficialAPI(fullPrompt: String, rawKey: String, allowPhoto: Boolean, callback: (Boolean) -> Unit) {
        if (rawKey.isEmpty()) {
            callback(false)
            return
        }
        val jsonBody = JSONObject().apply {
            put("contents", JSONArray().put(
                JSONObject().put("parts", JSONArray().put(JSONObject().put("text", fullPrompt)))
            ))
        }
        val isAiZa = rawKey.startsWith("AIza")
        val url = if (isAiZa) {
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$rawKey"
        } else {
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent"
        }

        val reqBuilder = Request.Builder()
            .url(url)
            .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))

        if (isAiZa) {
            reqBuilder.addHeader("x-goog-api-key", rawKey)
        } else {
            reqBuilder.addHeader("Authorization", "Bearer $rawKey")
        }

        client.newCall(reqBuilder.build()).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { callback(false) }
            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                if (response.isSuccessful && body != null) {
                    try {
                        val ans = JSONObject(body).getJSONArray("candidates").getJSONObject(0)
                            .getJSONObject("content").getJSONArray("parts").getJSONObject(0)
                            .getString("text").replace("*", "").trim()
                        if (ans.isNotEmpty()) {
                            executeActionTagsAndReply(ans, allowPhoto)
                            callback(true)
                            return
                        }
                    } catch (e: Exception) {}
                }
                callback(false)
            }
        })
    }

    private fun tryCloudAIEnginePost(systemRules: String, userPrompt: String, allowPhoto: Boolean, callback: (Boolean) -> Unit) {
        val messagesArr = JSONArray().apply {
            put(JSONObject().apply { put("role", "system"); put("content", systemRules) })
            put(JSONObject().apply { put("role", "user"); put("content", userPrompt) })
        }
        val payload = JSONObject().apply {
            put("model", "openai-large")
            put("messages", messagesArr)
            put("seed", (System.currentTimeMillis() % 100000).toInt())
        }

        val req = Request.Builder()
            .url("https://text.pollinations.ai/openai")
            .header("User-Agent", "Mozilla/5.0")
            .post(payload.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { callback(false) }
            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string() ?: ""
                if (response.isSuccessful && body.isNotEmpty()) {
                    try {
                        val content = if (body.trim().startsWith("{")) {
                            JSONObject(body).getJSONArray("choices").getJSONObject(0)
                                .getJSONObject("message").getString("content")
                        } else {
                            body
                        }
                        val clean = content.replace("*", "").trim()
                        if (clean.isNotEmpty() && !clean.contains("Free limit", true) && !clean.contains("rate limit", true)) {
                            executeActionTagsAndReply(clean, allowPhoto)
                            callback(true)
                            return
                        }
                    } catch (e: Exception) {}
                }
                callback(false)
            }
        })
    }

    private fun tryCloudAIEngineGet(systemRules: String, userPrompt: String, liveWebInfo: String, allowPhoto: Boolean) {
        val shortPrompt = "$systemRules\nCevap:"
        val url = "https://text.pollinations.ai/${URLEncoder.encode(shortPrompt.take(900), "UTF-8")}?model=mistral&seed=${System.currentTimeMillis() % 9999}"

        client.newCall(Request.Builder().url(url).header("User-Agent", "Mozilla/5.0").get().build()).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                deliverGuaranteedFallbackAnswer(userPrompt, liveWebInfo, allowPhoto)
            }
            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()?.replace("*", "")?.trim() ?: ""
                if (response.isSuccessful && body.isNotEmpty() && !body.contains("Free limit", true) && !body.contains("rate limit", true)) {
                    executeActionTagsAndReply(body, allowPhoto)
                } else {
                    deliverGuaranteedFallbackAnswer(userPrompt, liveWebInfo, allowPhoto)
                }
            }
        })
    }

    private fun deliverGuaranteedFallbackAnswer(userPrompt: String, liveWebInfo: String, allowPhoto: Boolean) {
        val lower = userPrompt.lowercase(Locale("tr", "TR"))
        val userName = AppConfig.getUserName(this).ifEmpty { "Dostum" }
        val aiName = AppConfig.getName(this).ifEmpty { "Asistan" }

        if (lower.contains("merhaba") || lower.contains("selam") || lower.contains("nasılsın") || lower.contains("naber")) {
            replyAndShow("Merhaba $userName! Ben $aiName, seni dinliyorum. Nasıl yardımcı olabilirim?", null)
            return
        }
        if (lower.contains("saat kaç")) {
            val cal = Calendar.getInstance()
            replyAndShow("Şu an saat ${String.format("%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))}.", null)
            return
        }
        if (liveWebInfo.isNotBlank()) {
            val cleanInfo = liveWebInfo.split("|").firstOrNull()?.trim() ?: liveWebInfo.take(240)
            replyAndShow(cleanInfo, if (allowPhoto) userPrompt else null)
        } else {
            replyAndShow("Seni dinliyorum $userName, başka bir soru sorabilirsin.", null)
        }
    }

    private fun executeActionTagsAndReply(rawAnswer: String, allowPhoto: Boolean) {
        var cleanSpeech = rawAnswer

        val waRegex = Regex("\\[WHATSAPP:\\s*(.*?)\\]")
        val waMatch = waRegex.find(rawAnswer)
        if (waMatch != null) {
            val inside = waMatch.groupValues[1]
            val parts = inside.split("|")
            val person = parts.getOrNull(0)?.trim() ?: ""
            val msg = parts.getOrNull(1)?.trim() ?: "Merhaba"
            val phone = findPhoneNumberInContacts(person)
            sendWhatsAppDirect(phone, person, msg)
            return
        }

        val appRegex = Regex("\\[UYGULAMA_AC:\\s*(.*?)\\]")
        val appMatch = appRegex.find(rawAnswer)
        if (appMatch != null) {
            val appName = appMatch.groupValues[1].trim()
            if (openAnyInstalledApp(appName)) return
        }

        val videoRegex = Regex("\\[VIDEO:\\s*(.*?)\\]")
        val videoMatch = videoRegex.find(rawAnswer)
        if (videoMatch != null) {
            val vQuery = videoMatch.groupValues[1].trim()
            findAndPlayYouTubeVideoDirectly(vQuery)
            return
        }

        val imgRegex = Regex("\\[RESIM:\\s*(.*?)\\]")
        val imgMatch = imgRegex.find(cleanSpeech)
        val imgTopic = if (allowPhoto) imgMatch?.groupValues?.get(1)?.trim() else null
        cleanSpeech = cleanSpeech.replace(imgRegex, "").trim()

        replyAndShow(cleanSpeech, imgTopic)
    }

    override fun onDestroy() {
        super.onDestroy()
        mainHandler.removeCallbacks(micWatchdogRunnable)
        stopAllAudioAndReleaseFocus()
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
