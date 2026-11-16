package com.niko.asistan

import android.Manifest
import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.accessibilityservice.GestureDescription
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
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
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
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
import android.text.method.ScrollingMovementMethod
import android.util.Base64
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.inputmethod.EditorInfo
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
import java.security.MessageDigest
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
    const val PREFS = "AssistantBixbyV17Prefs"
    const val DEFAULT_KEY = "AQ.Ab8RN6KZYPqSekpNWFEnuG42klSAK_TKxGeFNc1YbcduzUm0kA"

    val VOICE_LIST = listOf(
        Triple(0, "alloy", "👩  Claude Alloy — Sıcak & Doğal Kadın"),
        Triple(1, "coral", "👩‍💼  Claude Coral — Akıcı & Samimi Kadın"),
        Triple(2, "nova", "👩  Claude Nova — Enerjik & Net Kadın"),
        Triple(3, "shimmer", "👩‍🎤  Claude Shimmer — Berrak Stüdyo Kadın"),
        Triple(4, "ballad", "🎭  Claude Ballad — Duygusal & Hikayeci Ton"),
        Triple(5, "ash", "👨  Claude Ash — Doğal & Zeki Erkek"),
        Triple(6, "sage", "🧔  Claude Sage — Bilge & Sakin Jarvis Erkek"),
        Triple(7, "verse", "🎙️  Claude Verse — Dinamik Spiker Erkek"),
        Triple(8, "onyx", "👨  Claude Onyx — Karizmatik & Tok Erkek"),
        Triple(9, "echo", "🧔  Claude Echo — Yumuşak Stüdyo Erkek")
    )

    val LANG_LIST = listOf(
        "tr-TR" to "🇹🇷  Türkçe",
        "en-US" to "🇺🇸  English (İngilizce)",
        "ar-SA" to "🇸🇦  العربية (Arapça)",
        "ru-RU" to "🇷🇺  Русский (Rusça)"
    )

    val MODEL_LIST = listOf(
        Triple("HYDROGEN", "🔥 Hydrogen Ultra (GPT-4o & Gemini 2.5)", "En yüksek zeka, canlı web araştırması ve tam cihaz kontrolü"),
        Triple("ARGON", "💎 Argon Pro (Claude & DeepSeek R1 Mantık)", "Derin analitik düşünme, kodlama ve detaylı problem çözme"),
        Triple("3.2_FLASH", "⚡ 3.2 Flash Hız Motoru", "0.4 saniye ultra hızlı yanıt ve anında komut yürütme")
    )

    val SPEED_LIST = listOf(
        1.0f to "Normal (1.0x Doğal Akış)",
        1.2f to "Hızlı (1.2x Seri Konuşma)",
        1.4f to "Turbo (1.4x Maksimum Hız)"
    )

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

    fun getVoiceProfile(ctx: Context): Int =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt("VOICE_PROFILE", 5)

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

    fun isDarkTheme(ctx: Context): Boolean =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean("DARK_THEME", true)

    fun setDarkTheme(ctx: Context, dark: Boolean) =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean("DARK_THEME", dark).apply()

    fun getSphereColor(ctx: Context): Int =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt("SPHERE_COLOR", Color.parseColor("#38BDF8"))

    fun setSphereColor(ctx: Context, color: Int) =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putInt("SPHERE_COLOR", color).apply()

    // BULUT HESAP EŞİTLEME (Cloud Account Sync)
    fun getAccountEmail(ctx: Context): String =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("SYNC_ACCOUNT", "")?.trim() ?: ""

    fun setAccountEmail(ctx: Context, email: String) =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString("SYNC_ACCOUNT", email.trim().lowercase(Locale.US)).apply()

    fun getConversations(ctx: Context): JSONArray {
        val raw = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("CONVERSATIONS_V17", "[]") ?: "[]"
        return try { JSONArray(raw) } catch (e: Exception) { JSONArray() }
    }

    fun saveConversations(ctx: Context, arr: JSONArray) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString("CONVERSATIONS_V17", arr.toString()).apply()
        CloudSyncManager.pushToCloudIfConnected(ctx, arr)
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

    fun addMessageToCurrentSession(ctx: Context, role: String, text: String, imageUrl: String? = null) {
        val sessionId = getCurrentSessionId(ctx)
        val convs = getConversations(ctx)
        for (i in 0 until convs.length()) {
            val c = convs.getJSONObject(i)
            if (c.getString("id") == sessionId) {
                val msgs = c.optJSONArray("messages") ?: JSONArray()
                msgs.put(JSONObject().apply {
                    put("role", role)
                    put("text", text)
                    if (!imageUrl.isNullOrBlank()) put("imageUrl", imageUrl)
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

// TELEFON VE TABLET ARASINDA SOHBET GEÇMİŞİNİ EŞİTLEYEN BULUT SENKRONİZASYON MOTORU
object CloudSyncManager {
    private val client = OkHttpClient.Builder().connectTimeout(8, TimeUnit.SECONDS).build()

    private fun getBucketUrl(accountKey: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val hash = md.digest("AsistanSyncV17_$accountKey".toByteArray())
            .joinToString("") { "%02x".format(it) }.take(28)
        return "https://kvdb.io/A2x9kL5mP8qR3vW7yB1n/$hash"
    }

    fun pushToCloudIfConnected(ctx: Context, conversations: JSONArray) {
        val acc = AppConfig.getAccountEmail(ctx)
        if (acc.isEmpty()) return
        val url = getBucketUrl(acc)
        val req = Request.Builder()
            .url(url)
            .post(conversations.toString().toRequestBody("application/json".toMediaType()))
            .build()
        client.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {}
            override fun onResponse(call: Call, response: Response) { response.close() }
        })
    }

    fun pullFromCloud(ctx: Context, onComplete: (Boolean) -> Unit) {
        val acc = AppConfig.getAccountEmail(ctx)
        if (acc.isEmpty()) {
            onComplete(false)
            return
        }
        val url = getBucketUrl(acc)
        val req = Request.Builder().url(url).get().build()
        client.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Handler(Looper.getMainLooper()).post { onComplete(false) }
            }
            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string() ?: ""
                if (response.isSuccessful && body.trim().startsWith("[")) {
                    try {
                        val cloudArr = JSONArray(body)
                        if (cloudArr.length() > 0) {
                            ctx.getSharedPreferences(AppConfig.PREFS, Context.MODE_PRIVATE)
                                .edit().putString("CONVERSATIONS_V17", cloudArr.toString()).apply()
                            AppConfig.setCurrentSessionId(ctx, cloudArr.getJSONObject(0).getString("id"))
                            Handler(Looper.getMainLooper()).post { onComplete(true) }
                            return
                        }
                    } catch (e: Exception) {}
                }
                // Eğer bulutta henüz veri yoksa mevcut cihazın verisini buluta yükle
                pushToCloudIfConnected(ctx, AppConfig.getConversations(ctx))
                Handler(Looper.getMainLooper()).post { onComplete(true) }
            }
        })
    }
}

class AsistanOtonomService : AccessibilityService() {
    companion object {
        var instance: AsistanOtonomService? = null
        @Volatile var pendingAutoCapturePhoto = false
        @Volatile var pendingWhatsAppAutoSend = false
        @Volatile var pendingAutoTypeText: String? = null
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
        if (!pendingAutoCapturePhoto && !pendingWhatsAppAutoSend && pendingAutoTypeText == null) return
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

        val textToType = pendingAutoTypeText
        if (textToType != null && !pkg.contains("com.niko.asistan")) {
            pendingAutoTypeText = null
            handler.postDelayed({
                typeIntoFocusedOrSearchField(textToType)
            }, 1100L)
        }
    }

    private fun typeIntoFocusedOrSearchField(text: String) {
        val root = rootInActiveWindow ?: return
        val editNode = findEditableNodeRecursive(root, 0)
        if (editNode != null) {
            val args = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
            }
            editNode.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
            editNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                editNode.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_IME_ENTER.id)
            }
        }
    }

    private fun findEditableNodeRecursive(node: AccessibilityNodeInfo?, depth: Int): AccessibilityNodeInfo? {
        if (node == null || depth > 12) return null
        if (node.isEditable || node.className?.toString()?.contains("EditText", true) == true) {
            return node
        }
        for (i in 0 until node.childCount) {
            val found = findEditableNodeRecursive(node.getChild(i), depth + 1)
            if (found != null) return found
        }
        return null
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

class VectorIconView(context: Context, private val iconType: String, var iconColor: Int = Color.WHITE) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = iconColor
        style = Paint.Style.STROKE
        strokeWidth = 4.5f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = iconColor
        style = Paint.Style.FILL
    }
    private val rect = RectF()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        paint.color = iconColor
        fillPaint.color = iconColor
        val cx = width / 2f
        val cy = height / 2f
        val r = width.coerceAtMost(height) * 0.34f

        when (iconType) {
            "MENU" -> {
                canvas.drawLine(cx - r, cy - r * 0.65f, cx + r, cy - r * 0.65f, paint)
                canvas.drawLine(cx - r, cy, cx + r * 0.6f, cy, paint)
                canvas.drawLine(cx - r, cy + r * 0.65f, cx + r, cy + r * 0.65f, paint)
            }
            "SETTINGS" -> {
                canvas.drawCircle(cx, cy, r * 0.42f, paint)
                for (i in 0 until 8) {
                    val ang = Math.toRadians((i * 45).toDouble())
                    val x1 = cx + (r * 0.68f) * cos(ang).toFloat()
                    val y1 = cy + (r * 0.68f) * sin(ang).toFloat()
                    val x2 = cx + (r * 0.98f) * cos(ang).toFloat()
                    val y2 = cy + (r * 0.98f) * sin(ang).toFloat()
                    canvas.drawLine(x1, y1, x2, y2, paint)
                }
            }
            "KEY" -> {
                canvas.drawCircle(cx - r * 0.35f, cy, r * 0.42f, paint)
                canvas.drawLine(cx + r * 0.05f, cy, cx + r * 0.95f, cy, paint)
                canvas.drawLine(cx + r * 0.55f, cy, cx + r * 0.55f, cy + r * 0.38f, paint)
                canvas.drawLine(cx + r * 0.88f, cy, cx + r * 0.88f, cy + r * 0.38f, paint)
            }
            "CHAT" -> {
                rect.set(cx - r, cy - r * 0.8f, cx + r, cy + r * 0.6f)
                canvas.drawRoundRect(rect, 14f, 14f, paint)
                canvas.drawLine(cx - r * 0.4f, cy + r * 0.6f, cx - r * 0.7f, cy + r * 0.98f, paint)
                canvas.drawLine(cx - r * 0.7f, cy + r * 0.98f, cx + r * 0.1f, cy + r * 0.6f, paint)
            }
            "ORB" -> {
                canvas.drawCircle(cx, cy, r * 0.85f, paint)
                canvas.drawCircle(cx, cy, r * 0.35f, fillPaint)
            }
            "SEND" -> {
                val path = Path().apply {
                    moveTo(cx - r * 0.7f, cy - r * 0.7f)
                    lineTo(cx + r * 0.85f, cy)
                    lineTo(cx - r * 0.7f, cy + r * 0.7f)
                    lineTo(cx - r * 0.3f, cy)
                    close()
                }
                canvas.drawPath(path, fillPaint)
            }
        }
    }
}

// SAMSUNG BIXBY TARZI CANLI RENKLİ AKIŞKAN KÜRE İKONU (Sağ Alt Kapsülün Solundaki Canlı Çekirdek)
class BixbyOrbIconView(context: Context, var mainColor: Int = Color.parseColor("#38BDF8")) : View(context) {
    var isAnimating = false
    var audioAmp = 0f
    private var phase = 0f
    private val orbPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3.5f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val maxR = (width.coerceAtMost(height) / 2f) - 4f

        phase += if (isAnimating) 0.14f else 0.04f
        val pulse = if (isAnimating) (sin(phase) * 4f + (audioAmp * 1.2f)).coerceIn(-3f, 8f) else 0f
        val r = (maxR * 0.76f + pulse).coerceAtMost(maxR)

        val red = Color.red(mainColor)
        val green = Color.green(mainColor)
        val blue = Color.blue(mainColor)
        val lightCol = Color.argb(255, min(255, red + 75), min(255, green + 75), min(255, blue + 75))
        val darkCol = Color.argb(255, (red * 0.45f).toInt(), (green * 0.45f).toInt(), (blue * 0.45f).toInt())

        orbPaint.shader = RadialGradient(
            cx - r * 0.3f, cy - r * 0.3f, r * 1.3f,
            intArrayOf(Color.WHITE, lightCol, mainColor, darkCol),
            floatArrayOf(0f, 0.25f, 0.7f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy, r, orbPaint)

        ringPaint.color = Color.argb(140, red, green, blue)
        canvas.drawCircle(cx, cy, (r + 5f).coerceAtMost(maxR), ringPaint)

        if (isShown) postInvalidateDelayed(40L)
    }
}

class ParticleSphereView(context: Context) : View(context) {
    var audioLevel = 0f
    var isActive = false
    var sphereColor: Int = Color.WHITE
    private var rotY = 0f
    private val rotX = 0.25f
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    private val points = Array(135) { i ->
        val numPoints = 135
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

        val red = Color.red(sphereColor)
        val green = Color.green(sphereColor)
        val blue = Color.blue(sphereColor)

        for (i in points.indices) {
            val p = points[i]
            val wave = if (isActive) sin(rotY * 4f + i * 0.15f) * (pulse * 0.35f) else 0f
            val r = baseRadius + wave

            val x1 = p[0] * cosY - p[2] * sinY
            val z1 = p[2] * cosY + p[0] * sinY
            val y2 = p[1] * cosX - z1 * sinX
            val z2 = z1 * cosX + p[1] * sinX

            val depth = (z2 + 1.4f) / 2.4f
            val alpha = (depth * 230f + 25f).toInt().coerceIn(30, 255)
            dotPaint.color = Color.argb(alpha, red, green, blue)
            val dotSize = depth * 3.9f + 1.4f
            canvas.drawCircle(cx + x1 * r, cy + y2 * r, dotSize, dotPaint)
        }
        postInvalidateDelayed(34L)
    }
}

class GeminiMicButtonView(context: Context, var accentColor: Int = Color.WHITE) : View(context) {
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accentColor; style = Paint.Style.FILL }
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
        bgPaint.color = accentColor
        val iconCol = if (Color.luminance(accentColor) > 0.5f) Color.parseColor("#111111") else Color.WHITE
        micFillPaint.color = iconCol
        micStrokePaint.color = iconCol

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
    var barColor = Color.WHITE
    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 4.8f
        strokeCap = Paint.Cap.ROUND
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        barPaint.color = barColor
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
        var isChatScreenOpen = false
        var onNewMessageCallback: (() -> Unit)? = null
        private const val REQ_PICK_ICON = 701

        val COLOR_PALETTE = listOf(
            "Bixby Mavi" to Color.parseColor("#38BDF8"),
            "Saf Beyaz" to Color.parseColor("#FFFFFF"),
            "Altın Sarısı" to Color.parseColor("#FBBF24"),
            "Zümrüt Yeşili" to Color.parseColor("#34D399"),
            "Claude Turuncu" to Color.parseColor("#D97757"),
            "Neon Mor" to Color.parseColor("#A855F7"),
            "Mercan Kırmızı" to Color.parseColor("#F87171"),
            "Pembe Kuvars" to Color.parseColor("#F472B6"),
            "Turkuaz" to Color.parseColor("#2DD4BF"),
            "Elektrik İndigo" to Color.parseColor("#818CF8"),
            "Limon Yeşili" to Color.parseColor("#A3E635"),
            "Gümüş Gri" to Color.parseColor("#94A3B8")
        )
    }

    private lateinit var rootContainer: FrameLayout
    private val imageHttpClient = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        rootContainer = FrameLayout(this)
        applyThemeBackground()
        setContentView(rootContainer)
        requestBatteryOptimizationExemption()
        checkPermissionsAndProceed()
    }

    private fun applyThemeBackground() {
        val isDark = AppConfig.isDarkTheme(this)
        rootContainer.setBackgroundColor(if (isDark) Color.parseColor("#08080C") else Color.parseColor("#F8FAFC"))
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
        isChatScreenOpen = false
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
            showPermissionGate("Ekran Üstü İzni Gerekli", "Asistanın diğer uygulamaların üzerinde (Bixby kapsülü olarak) görünebilmesi için bu izni vermelisiniz.") {
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
        isChatScreenOpen = false
        rootContainer.removeAllViews()
        val scroll = ScrollView(this).apply { isFillViewport = true }
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(65, 80, 65, 70)
        }

        val miniSphere = ParticleSphereView(this).apply {
            sphereColor = AppConfig.getSphereColor(this@MainActivity)
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
        isChatScreenOpen = false
        onNewMessageCallback = null
        applyThemeBackground()
        rootContainer.removeAllViews()

        val isDark = AppConfig.isDarkTheme(this)
        val bgColor = if (isDark) Color.parseColor("#08080C") else Color.parseColor("#F8FAFC")
        val primaryText = if (isDark) Color.WHITE else Color.parseColor("#0F172A")
        val secondaryText = if (isDark) Color.parseColor("#71717A") else Color.parseColor("#64748B")
        val cardBg = if (isDark) Color.parseColor("#14141B") else Color.parseColor("#FFFFFF")
        val cardBorder = if (isDark) Color.parseColor("#272732") else Color.parseColor("#E2E8F0")
        val accentColor = AppConfig.getSphereColor(this)
        val effectiveSphereColor = if (!isDark && accentColor == Color.WHITE) Color.parseColor("#0F172A") else accentColor

        val aiName = AppConfig.getName(this).ifEmpty { "Asistan" }
        val userName = AppConfig.getUserName(this).ifEmpty { "Dostum" }

        val mainFrame = FrameLayout(this).apply { setBackgroundColor(bgColor) }

        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(50, 48, 50, 20)
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.TOP)
        }

        val menuIconBtn = VectorIconView(this, "MENU", primaryText).apply {
            layoutParams = LinearLayout.LayoutParams(75, 75)
            setOnClickListener { showGeminiChatScreen(focusKeyboard = false) }
        }

        val titleTv = TextView(this).apply {
            text = aiName
            textSize = 22f
            setTextColor(primaryText)
            typeface = Typeface.create("serif", Typeface.BOLD)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener { showRenameDialog() }
        }

        val keyIconBtn = VectorIconView(this, "KEY", primaryText).apply {
            layoutParams = LinearLayout.LayoutParams(72, 72).apply { rightMargin = 20 }
            setOnClickListener { showApiKeyDialog() }
        }

        val settingsIconBtn = VectorIconView(this, "SETTINGS", primaryText).apply {
            layoutParams = LinearLayout.LayoutParams(75, 75)
            setOnClickListener { showSettingsScreen() }
        }

        topBar.addView(menuIconBtn)
        topBar.addView(titleTv)
        topBar.addView(keyIconBtn)
        topBar.addView(settingsIconBtn)

        val centerCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
            setPadding(40, 50, 40, 140)
        }

        val pSphere = ParticleSphereView(this).apply {
            sphereColor = effectiveSphereColor
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
            textSize = 28f
            setTextColor(primaryText)
            typeface = Typeface.create("serif", Typeface.NORMAL)
            gravity = Gravity.CENTER
            setPadding(30, 28, 30, 10)
        }

        val subLabel = TextView(this).apply {
            text = "Küreye dokun veya \"Hey $aiName\" de"
            textSize = 14f
            setTextColor(secondaryText)
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
            text = "Sohbet & Görsel Üretim Ekranını Aç..."
            textSize = 14f
            setTextColor(primaryText)
            gravity = Gravity.CENTER
            setPadding(64, 24, 64, 24)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = 30
            }
            background = GradientDrawable().apply {
                cornerRadius = 60f
                setColor(cardBg)
                setStroke(2, cardBorder)
            }
            setOnClickListener { showGeminiChatScreen(focusKeyboard = true) }
        }

        val divider = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1)
            setBackgroundColor(cardBorder)
        }

        val navBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(20, 18, 20, 26)
            setBackgroundColor(if (isDark) Color.parseColor("#0B0B10") else Color.parseColor("#F1F5F9"))
        }

        fun navTab(iconType: String, label: String, active: Boolean, onClick: () -> Unit): View {
            val tabColor = if (active) effectiveSphereColor else secondaryText
            return LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                val ic = VectorIconView(this@MainActivity, iconType, tabColor).apply {
                    layoutParams = LinearLayout.LayoutParams(54, 54)
                }
                val lb = TextView(this@MainActivity).apply {
                    text = label
                    textSize = 11f
                    typeface = if (active) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                    setTextColor(tabColor)
                    gravity = Gravity.CENTER
                    setPadding(0, 6, 0, 0)
                }
                addView(ic)
                addView(lb)
                setOnClickListener { onClick() }
            }
        }

        navBar.addView(navTab("ORB", aiName, true) { showMinimalistHome() })
        navBar.addView(navTab("CHAT", "Sohbet", false) { showGeminiChatScreen(focusKeyboard = false) })
        navBar.addView(navTab("SETTINGS", "Ayarlar", false) { showSettingsScreen() })

        bottomWrap.addView(writePillBtn)
        bottomWrap.addView(divider)
        bottomWrap.addView(navBar)

        mainFrame.addView(centerCol)
        mainFrame.addView(topBar)
        mainFrame.addView(bottomWrap)

        rootContainer.addView(mainFrame)
    }

    // SOHBET EKRANI: Sağ Alttaki Bar Burada ASLA Açılmaz + Üretilen Resimler Sohbet İçinde Görünür ve Tıklanınca Tam Ekran Olur!
    private fun showGeminiChatScreen(focusKeyboard: Boolean) {
        isChatScreenOpen = true
        sphereView = null
        startAssistantService("HIDE_PILL")
        applyThemeBackground()
        rootContainer.removeAllViews()

        val isDark = AppConfig.isDarkTheme(this)
        val bgColor = if (isDark) Color.parseColor("#0B0B0F") else Color.parseColor("#F8FAFC")
        val headerBg = if (isDark) Color.parseColor("#121218") else Color.parseColor("#FFFFFF")
        val primaryText = if (isDark) Color.WHITE else Color.parseColor("#0F172A")
        val secondaryText = if (isDark) Color.parseColor("#94A3B8") else Color.parseColor("#64748B")
        val aiBubbleBg = if (isDark) Color.parseColor("#181822") else Color.parseColor("#FFFFFF")
        val borderCol = if (isDark) Color.parseColor("#2A2A38") else Color.parseColor("#E2E8F0")
        val accentColor = AppConfig.getSphereColor(this)
        val userBubbleBg = if (accentColor == Color.WHITE && !isDark) Color.parseColor("#0F172A") else accentColor
        val userBubbleText = if (Color.luminance(userBubbleBg) > 0.5f) Color.BLACK else Color.WHITE

        val aiName = AppConfig.getName(this).ifEmpty { "Asistan" }
        val modelBadge = AppConfig.getModelChoice(this)

        val outerFrame = FrameLayout(this).apply { setBackgroundColor(bgColor) }

        val mainCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(35, 45, 35, 25)
            setBackgroundColor(headerBg)
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
            setBackgroundColor(headerBg)
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
                val emptyBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setPadding(30, 100, 30, 40) }
                val greet = TextView(this).apply {
                    text = "Merhaba, ${AppConfig.getUserName(this@MainActivity).ifEmpty { "Dostum" }}"
                    textSize = 28f
                    setTextColor(primaryText)
                    typeface = Typeface.create("serif", Typeface.BOLD)
                    gravity = Gravity.CENTER
                }
                val sub = TextView(this).apply {
                    text = "Sohbet edebilir, gerçekçi görsel üretebilir veya navigasyon başlatabilirsin."
                    textSize = 15f
                    setTextColor(secondaryText)
                    gravity = Gravity.CENTER
                    setPadding(0, 10, 0, 40)
                }
                emptyBox.addView(greet)
                emptyBox.addView(sub)

                val suggestions = listOf(
                    "🗺️ Gaziantep'e yol tarifi al",
                    "🎨 Gerçekçi bir spor araba resmi çiz",
                    "🌐 Google'ı açıp EBA yaz",
                    "🎬 YouTube'da MrBeast videosu aç"
                )
                for (sug in suggestions) {
                    val chip = TextView(this).apply {
                        text = sug
                        textSize = 14f
                        setTextColor(primaryText)
                        setPadding(42, 26, 42, 26)
                        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                            bottomMargin = 16
                        }
                        background = GradientDrawable().apply {
                            cornerRadius = 40f
                            setColor(aiBubbleBg)
                            setStroke(2, borderCol)
                        }
                        setOnClickListener {
                            val cleanCmd = sug.substringAfter(" ").trim()
                            startAssistantService("TEXT_QUERY", cleanCmd)
                        }
                    }
                    emptyBox.addView(chip)
                }
                messagesList.addView(emptyBox)
            } else {
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val isUser = obj.getString("role") == "user"
                    val msgText = obj.getString("text")
                    val imgUrl = obj.optString("imageUrl", "")

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
                            typeface = Typeface.DEFAULT_BOLD
                            setTextColor(secondaryText)
                            setPadding(12, 0, 12, 6)
                        }
                        row.addView(senderLb)
                    }

                    val bubbleBox = LinearLayout(this).apply {
                        orientation = LinearLayout.VERTICAL
                        setPadding(40, 26, 40, 26)
                        background = GradientDrawable().apply {
                            cornerRadius = 38f
                            setColor(if (isUser) userBubbleBg else aiBubbleBg)
                            if (!isUser) setStroke(2, borderCol)
                        }
                    }

                    val bubbleTv = TextView(this).apply {
                        text = msgText
                        textSize = 15f
                        setTextColor(if (isUser) userBubbleText else primaryText)
                        maxWidth = (resources.displayMetrics.widthPixels * 0.76).toInt()
                        setOnLongClickListener {
                            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cm.setPrimaryClip(ClipData.newPlainText("Mesaj", msgText))
                            Toast.makeText(this@MainActivity, "Metin kopyalandı", Toast.LENGTH_SHORT).show()
                            true
                        }
                    }
                    bubbleBox.addView(bubbleTv)

                    // SOHBET İÇİNDE RESİM GÖSTERME & TIKLAYINCA TAM EKRAN AÇMA!
                    if (imgUrl.isNotEmpty()) {
                        val chatImgView = ImageView(this).apply {
                            layoutParams = LinearLayout.LayoutParams(620, 420).apply {
                                topMargin = 20
                            }
                            scaleType = ImageView.ScaleType.CENTER_CROP
                            background = GradientDrawable().apply {
                                cornerRadius = 28f
                                setColor(Color.parseColor("#22222A"))
                            }
                        }
                        val tapHint = TextView(this).apply {
                            text = "🔍 Tam ekran görmek ve indirmek için resme dokunun"
                            textSize = 11f
                            setTextColor(secondaryText)
                            setPadding(4, 10, 4, 0)
                        }
                        bubbleBox.addView(chatImgView)
                        bubbleBox.addView(tapHint)

                        loadBitmapAsync(imgUrl) { bmp ->
                            if (bmp != null) {
                                chatImgView.setImageBitmap(bmp)
                                chatImgView.setOnClickListener {
                                    showFullScreenImageDialog(bmp, msgText)
                                }
                            }
                        }
                    }

                    row.addView(bubbleBox)
                    messagesList.addView(row)
                }
                messagesScroll.post { messagesScroll.fullScroll(View.FOCUS_DOWN) }
            }
        }

        fun rebuildDrawerList() {
            drawerPanel.removeAllViews()
            val drawerTitle = TextView(this).apply {
                text = "Sohbet Geçmişi"
                textSize = 20f
                setTextColor(primaryText)
                typeface = Typeface.DEFAULT_BOLD
                setPadding(10, 0, 10, 10)
            }
            drawerPanel.addView(drawerTitle)

            val syncAcc = AppConfig.getAccountEmail(this)
            val syncBadge = TextView(this).apply {
                text = if (syncAcc.isNotEmpty()) "☁️ Eşitlenen Hesap: $syncAcc" else "☁️ Cihazlar arası eşitleme kapalı (Ayarlar'dan açın)"
                textSize = 12f
                setTextColor(secondaryText)
                setPadding(10, 0, 10, 25)
            }
            drawerPanel.addView(syncBadge)

            val newChatDrawerBtn = TextView(this).apply {
                text = "＋   Yeni Sohbet Başlat"
                textSize = 15f
                setTextColor(userBubbleText)
                typeface = Typeface.DEFAULT_BOLD
                setPadding(35, 26, 35, 26)
                background = GradientDrawable().apply {
                    cornerRadius = 40f
                    setColor(userBubbleBg)
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
                        setColor(if (isSelected) borderCol else Color.TRANSPARENT)
                    }
                    setOnClickListener {
                        AppConfig.setCurrentSessionId(this@MainActivity, cid)
                        drawerOverlay.visibility = View.GONE
                        renderCurrentConversation()
                    }
                }

                val tv = TextView(this).apply {
                    text = cTitle
                    textSize = 14f
                    typeface = if (isSelected) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                    setTextColor(primaryText)
                    setSingleLine(true)
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }

                val delBtn = TextView(this).apply {
                    text = "Sil"
                    textSize = 12f
                    setTextColor(secondaryText)
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

        val drawerBtn = VectorIconView(this, "MENU", primaryText).apply {
            layoutParams = LinearLayout.LayoutParams(72, 72)
            setOnClickListener {
                rebuildDrawerList()
                drawerOverlay.visibility = View.VISIBLE
            }
        }

        val centerHeaderTitle = TextView(this).apply {
            text = "$aiName  ·  $modelBadge"
            textSize = 16f
            setTextColor(primaryText)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val newChatTopBtn = TextView(this).apply {
            text = "＋ Yeni"
            textSize = 13f
            setTextColor(userBubbleText)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(28, 14, 28, 14)
            background = GradientDrawable().apply { cornerRadius = 30f; setColor(userBubbleBg) }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { rightMargin = 16 }
            setOnClickListener {
                AppConfig.createNewConversation(this@MainActivity)
                renderCurrentConversation()
            }
        }

        val backOrbBtn = TextView(this).apply {
            text = "Küre"
            textSize = 13f
            setTextColor(primaryText)
            setPadding(26, 14, 26, 14)
            background = GradientDrawable().apply { cornerRadius = 30f; setColor(aiBubbleBg); setStroke(2, borderCol) }
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
                setColor(aiBubbleBg)
                setStroke(2, borderCol)
            }
        }

        val chatInput = EditText(this).apply {
            hint = "$aiName'a mesaj yazın veya 'resim çiz' deyin..."
            setHintTextColor(secondaryText)
            setTextColor(primaryText)
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

        val micBtn = GeminiMicButtonView(this, userBubbleBg).apply {
            layoutParams = LinearLayout.LayoutParams(76, 76).apply { rightMargin = 16 }
            setOnClickListener {
                Toast.makeText(this@MainActivity, "Dinliyorum, konuşabilirsiniz...", Toast.LENGTH_SHORT).show()
                startAssistantService("WAKE_SILENT_IN_CHAT")
            }
        }

        val sendBtn = VectorIconView(this, "SEND", primaryText).apply {
            layoutParams = LinearLayout.LayoutParams(76, 76)
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

    // RESME TIKLAYINCA TAM EKRAN AÇAN VE GALERİYE KAYDEDEN PENCERE
    private fun showFullScreenImageDialog(bmp: Bitmap, title: String) {
        val dialog = Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        val box = FrameLayout(this).apply {
            setBackgroundColor(Color.parseColor("#F2050508"))
        }
        val fullImg = ImageView(this).apply {
            setImageBitmap(bmp)
            scaleType = ImageView.ScaleType.FIT_CENTER
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        }
        val topControls = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
            setPadding(40, 50, 40, 30)
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.TOP)
        }
        val saveBtn = TextView(this).apply {
            text = "📥 Galeriye Kaydet"
            textSize = 14f
            setTextColor(Color.BLACK)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(36, 20, 36, 20)
            background = GradientDrawable().apply { cornerRadius = 40f; setColor(Color.WHITE) }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { rightMargin = 20 }
            setOnClickListener {
                saveBitmapToGallery(bmp, title)
            }
        }
        val closeBtn = TextView(this).apply {
            text = "✕ Kapat"
            textSize = 14f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(36, 20, 36, 20)
            background = GradientDrawable().apply { cornerRadius = 40f; setColor(Color.parseColor("#33FFFFFF")) }
            setOnClickListener { dialog.dismiss() }
        }
        topControls.addView(saveBtn)
        topControls.addView(closeBtn)
        box.addView(fullImg)
        box.addView(topControls)
        dialog.setContentView(box)
        dialog.show()
    }

    private fun saveBitmapToGallery(bmp: Bitmap, title: String) {
        try {
            val filename = "AI_Gorsel_${System.currentTimeMillis()}.png"
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                }
            }
            val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            if (uri != null) {
                contentResolver.openOutputStream(uri)?.use { out ->
                    bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                Toast.makeText(this, "Görsel galeriye kaydedildi!", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Kaydedilemedi.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadBitmapAsync(url: String, onLoaded: (Bitmap?) -> Unit) {
        val req = Request.Builder().url(url).header("User-Agent", "Mozilla/5.0").build()
        imageHttpClient.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread { onLoaded(null) }
            }
            override fun onResponse(call: Call, response: Response) {
                val bytes = response.body?.bytes()
                val bmp = if (bytes != null && bytes.size > 500) BitmapFactory.decodeByteArray(bytes, 0, bytes.size) else null
                runOnUiThread { onLoaded(bmp) }
            }
        })
    }

    // YENİ AÇILIR MENÜLÜ (POP-UP DIALOG) KOMPAKT VE ŞIK AYARLAR EKRANI
    // (Dil, Ses, Model, Hız ve Hesap Bağlama kendi üzerine basınca açılır!)
    private fun showSettingsScreen() {
        isChatScreenOpen = false
        sphereView = null
        onNewMessageCallback = null
        applyThemeBackground()
        rootContainer.removeAllViews()

        val isDark = AppConfig.isDarkTheme(this)
        val bgColor = if (isDark) Color.parseColor("#08080C") else Color.parseColor("#F8FAFC")
        val cardBg = if (isDark) Color.parseColor("#121218") else Color.parseColor("#FFFFFF")
        val borderCol = if (isDark) Color.parseColor("#242432") else Color.parseColor("#E2E8F0")
        val primaryText = if (isDark) Color.WHITE else Color.parseColor("#0F172A")
        val secondaryText = if (isDark) Color.parseColor("#8E8E93") else Color.parseColor("#64748B")
        val accentColor = AppConfig.getSphereColor(this)
        val activeBorder = if (!isDark && accentColor == Color.WHITE) Color.parseColor("#0F172A") else accentColor

        val scroll = ScrollView(this).apply { setBackgroundColor(bgColor) }
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(45, 50, 45, 80) }

        val backTv = TextView(this).apply {
            text = "←   Ayarlar & Kontrol Merkezi"
            textSize = 22f
            setTextColor(primaryText)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, 25)
            setOnClickListener {
                startAssistantService("RELOAD_CONFIG")
                showMinimalistHome()
            }
        }
        col.addView(backTv)

        fun sectionHeader(title: String): TextView = TextView(this).apply {
            text = title.uppercase(Locale("tr", "TR"))
            textSize = 12f
            setTextColor(secondaryText)
            typeface = Typeface.DEFAULT_BOLD
            letterSpacing = 0.08f
            setPadding(12, 32, 12, 14)
        }

        fun dropdownCard(title: String, currentValue: String, badgeText: String, onClick: () -> Unit): View {
            return LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(40, 32, 40, 32)
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = 16 }
                background = GradientDrawable().apply {
                    cornerRadius = 32f
                    setColor(cardBg)
                    setStroke(2, borderCol)
                }
                val textCol = LinearLayout(this@MainActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }
                val t1 = TextView(this@MainActivity).apply {
                    text = title
                    textSize = 16f
                    setTextColor(primaryText)
                    typeface = Typeface.DEFAULT_BOLD
                }
                val t2 = TextView(this@MainActivity).apply {
                    text = currentValue
                    textSize = 13f
                    setTextColor(activeBorder)
                    setPadding(0, 6, 0, 0)
                }
                textCol.addView(t1)
                textCol.addView(t2)
                addView(textCol)

                val badge = TextView(this@MainActivity).apply {
                    text = "$badgeText  ▾"
                    textSize = 12f
                    setTextColor(primaryText)
                    typeface = Typeface.DEFAULT_BOLD
                    setPadding(26, 12, 26, 12)
                    background = GradientDrawable().apply {
                        cornerRadius = 22f
                        setColor(borderCol)
                    }
                }
                addView(badge)
                setOnClickListener { onClick() }
            }
        }

        // 1. TEMA & BIXBY RENK PALETİ
        col.addView(sectionHeader("Tema & Bixby Renk Paleti"))
        val themeRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = 18 }
        }
        val darkBtn = TextView(this).apply {
            text = "🌙  Karanlık Tema"
            textSize = 14f
            gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(if (isDark) Color.BLACK else primaryText)
            setPadding(30, 28, 30, 28)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { rightMargin = 10 }
            background = GradientDrawable().apply {
                cornerRadius = 28f
                setColor(if (isDark) Color.WHITE else cardBg)
                setStroke(2, borderCol)
            }
            setOnClickListener {
                AppConfig.setDarkTheme(this@MainActivity, true)
                showSettingsScreen()
            }
        }
        val lightBtn = TextView(this).apply {
            text = "☀️  Açık Tema"
            textSize = 14f
            gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(if (!isDark) Color.WHITE else primaryText)
            setPadding(30, 28, 30, 28)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { leftMargin = 10 }
            background = GradientDrawable().apply {
                cornerRadius = 28f
                setColor(if (!isDark) Color.parseColor("#0F172A") else cardBg)
                setStroke(2, borderCol)
            }
            setOnClickListener {
                AppConfig.setDarkTheme(this@MainActivity, false)
                showSettingsScreen()
            }
        }
        themeRow.addView(darkBtn)
        themeRow.addView(lightBtn)
        col.addView(themeRow)

        val paletteScroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = 16 }
        }
        val paletteRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(10, 14, 10, 14)
        }
        for ((cName, cVal) in COLOR_PALETTE) {
            val isChosen = (accentColor == cVal)
            val swatchWrap = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(16, 8, 16, 8)
                setOnClickListener {
                    AppConfig.setSphereColor(this@MainActivity, cVal)
                    startAssistantService("RELOAD_CONFIG")
                    showSettingsScreen()
                }
            }
            val circle = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(84, 84)
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(cVal)
                    setStroke(if (isChosen) 6 else 2, if (isChosen) primaryText else borderCol)
                }
            }
            val lb = TextView(this).apply {
                text = cName
                textSize = 11f
                setTextColor(if (isChosen) primaryText else secondaryText)
                typeface = if (isChosen) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                setPadding(0, 8, 0, 0)
            }
            swatchWrap.addView(circle)
            swatchWrap.addView(lb)
            paletteRow.addView(swatchWrap)
        }
        paletteScroll.addView(paletteRow)
        col.addView(paletteScroll)

        // 2. AÇILIR PENCERELİ SES, DİL, MODEL VE HIZ SEÇİCİLERİ (Üzerine basınca açılır!)
        col.addView(sectionHeader("Ses, Dil & Yapay Zeka Seçenekleri (Seçmek İçin Dokunun)"))

        val curVoiceIdx = AppConfig.getVoiceProfile(this)
        val curVoiceLabel = AppConfig.VOICE_LIST.firstOrNull { it.first == curVoiceIdx }?.third ?: "Claude Ash"
        col.addView(dropdownCard("🎙️ Ses Profili (10 Claude & Stüdyo Sesi)", curVoiceLabel, "Sesi Seç") {
            val labels = AppConfig.VOICE_LIST.map { it.third }.toTypedArray()
            AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("Ses Profili Seçin")
                .setSingleChoiceItems(labels, curVoiceIdx) { d, which ->
                    AppConfig.setVoiceProfile(this, which)
                    startAssistantService("TEST_VOICE")
                    d.dismiss()
                    showSettingsScreen()
                }
                .show()
        })

        val curLangCode = AppConfig.getLang(this)
        val curLangLabel = AppConfig.LANG_LIST.firstOrNull { it.first == curLangCode }?.second ?: "🇹🇷 Türkçe"
        col.addView(dropdownCard("🌍 Konuşma ve Algılama Dili", curLangLabel, "Dili Seç") {
            val labels = AppConfig.LANG_LIST.map { it.second }.toTypedArray()
            val curIdx = AppConfig.LANG_LIST.indexOfFirst { it.first == curLangCode }.coerceAtLeast(0)
            AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("Dil Seçin")
                .setSingleChoiceItems(labels, curIdx) { d, which ->
                    AppConfig.setLang(this, AppConfig.LANG_LIST[which].first)
                    startAssistantService("RELOAD_CONFIG")
                    d.dismiss()
                    showSettingsScreen()
                }
                .show()
        })

        val curModelCode = AppConfig.getModelChoice(this)
        val curModelLabel = AppConfig.MODEL_LIST.firstOrNull { it.first == curModelCode }?.second ?: "Hydrogen Ultra"
        col.addView(dropdownCard("🧠 Yapay Zeka Beyin Motoru", curModelLabel, "Modeli Seç") {
            val labels = AppConfig.MODEL_LIST.map { "${it.second}\n${it.third}" }.toTypedArray()
            val curIdx = AppConfig.MODEL_LIST.indexOfFirst { it.first == curModelCode }.coerceAtLeast(0)
            AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("Yapay Zeka Motoru Seçin")
                .setSingleChoiceItems(labels, curIdx) { d, which ->
                    AppConfig.setModelChoice(this, AppConfig.MODEL_LIST[which].first)
                    startAssistantService("RELOAD_CONFIG")
                    d.dismiss()
                    showSettingsScreen()
                }
                .show()
        })

        val curSpeed = AppConfig.getSpeed(this)
        val curSpeedLabel = AppConfig.SPEED_LIST.firstOrNull { kotlin.math.abs(it.first - curSpeed) < 0.05f }?.second ?: "Normal (1.0x)"
        col.addView(dropdownCard("⚡ Konuşma Hızı", curSpeedLabel, "Hızı Seç") {
            val labels = AppConfig.SPEED_LIST.map { it.second }.toTypedArray()
            val curIdx = AppConfig.SPEED_LIST.indexOfFirst { kotlin.math.abs(it.first - curSpeed) < 0.05f }.coerceAtLeast(0)
            AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("Konuşma Hızı Seçin")
                .setSingleChoiceItems(labels, curIdx) { d, which ->
                    AppConfig.setSpeed(this, AppConfig.SPEED_LIST[which].first)
                    startAssistantService("RELOAD_CONFIG")
                    d.dismiss()
                    showSettingsScreen()
                }
                .show()
        })

        // 3. BULUT HESAP BAĞLAMA (Telefon & Tablet Ortak Sohbet Geçmişi)
        col.addView(sectionHeader("Cihazlar Arası Bulut Hesap Eşitleme"))
        val connectedAcc = AppConfig.getAccountEmail(this)
        col.addView(dropdownCard(
            "☁️ Google / E-Posta ile Hesap Bağla",
            if (connectedAcc.isNotEmpty()) "Bağlı Hesap: $connectedAcc (Telefon & Tablet Eşitleniyor)" else "Henüz bağlı değil — Tablet ve telefonda aynı geçmişi görmek için dokunun",
            if (connectedAcc.isNotEmpty()) "Eşitle" else "Bağla"
        ) {
            showCloudSyncDialog()
        })

        // 4. KİMLİK, ANAHTAR, SİMGE & OTONOM KONTROL
        col.addView(sectionHeader("Kimlik, Simge & Cihaz Yetkileri"))
        col.addView(dropdownCard("🤖 Asistan & Kullanıcı Adı", "Asistan: ${AppConfig.getName(this)}  ·  Siz: ${AppConfig.getUserName(this)}", "Düzenle") {
            showRenameDialog()
        })
        col.addView(dropdownCard("🔑 Gemini API Anahtarı", "Özel AIza... veya AQ... anahtarınızı girin", "Anahtar") {
            showApiKeyDialog()
        })
        col.addView(dropdownCard("🖼️ Uygulama Simgesini Değiştir", "Galeriden seçtiğiniz fotoğrafı ana ekran simgesi yapar", "Yükle") {
            val pick = Intent(Intent.ACTION_GET_CONTENT).apply { type = "image/*" }
            startActivityForResult(Intent.createChooser(pick, "Simge Seçin"), REQ_PICK_ICON)
        })

        val isAutoEnabled = AsistanOtonomService.instance != null
        col.addView(dropdownCard(
            "⚙️ Otonom Uygulama Kontrolü",
            if (isAutoEnabled) "Aktif (Otomatik yazı yazma, fotoğraf çekme ve mesaj gönderme)" else "Kapalı — Etkinleştirmek için dokunun",
            if (isAutoEnabled) "Açık" else "Aç"
        ) {
            try { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) } catch (e: Exception) {}
        })

        scroll.addView(col)
        rootContainer.addView(scroll)
    }

    private fun showCloudSyncDialog() {
        val input = EditText(this).apply {
            setText(AppConfig.getAccountEmail(this@MainActivity))
            hint = "Gmail adresiniz veya ortak bir şifre yazın..."
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
            setPadding(45, 35, 45, 35)
        }
        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("☁️ Telefon & Tablet Hesap Eşitleme")
            .setMessage("Hem telefonunuzda hem tabletinizde aynı Gmail adresini (veya belirlediğiniz özel bir kodu) girin. İki cihazdaki sohbet geçmişiniz anında eşitlenir:")
            .setView(input)
            .setPositiveButton("Bağla ve Eşitle") { _, _ ->
                val acc = input.text.toString().trim()
                AppConfig.setAccountEmail(this, acc)
                if (acc.isNotEmpty()) {
                    Toast.makeText(this, "Bulut hesabı eşitleniyor...", Toast.LENGTH_SHORT).show()
                    CloudSyncManager.pullFromCloud(this) { ok ->
                        Toast.makeText(this, if (ok) "✅ Sohbet geçmişi eşitlendi!" else "☁️ Yeni bulut hesabı oluşturuldu!", Toast.LENGTH_LONG).show()
                        showSettingsScreen()
                    }
                } else {
                    showSettingsScreen()
                }
            }
            .setNegativeButton("İptal", null)
            .show()
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
            .setTitle("Gemini API Anahtarı")
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

// ARKA PLAN SERVİSİ v17.0:
// - Samsung Bixby Tasarımlı & Dinamik Renkli Sağ Alt Kapsül
// - Sohbet ekranı açıkken (`MainActivity.isChatScreenOpen`) sağ alt bar ASLA açılmaz!
// - "Gaziantep'e yol tarifi al" dediğinde doğrudan Google Maps Navigasyonu (`google.navigation:q=...`) başlatır!
// - Sadece "Duman" / "Fındık" denildiğinde gereksiz Twitter/Web araması yapmaz!
// - Flux Realism Görsel Üretim Motoru (Hem sohbette hem sağ altta tam ekran açılabilir)
class OverlayService : Service(), TextToSpeech.OnInitListener {
    private lateinit var windowManager: WindowManager
    private lateinit var overlayParams: WindowManager.LayoutParams
    private lateinit var rootOverlayLayout: LinearLayout
    private lateinit var imageCardView: LinearLayout
    private lateinit var resultImageView: ImageView
    private lateinit var imageCaptionTv: TextView
    private lateinit var bixbyPill: LinearLayout
    private lateinit var bixbyOrbIcon: BixbyOrbIconView
    private lateinit var pillScrollWrapper: ScrollView
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
        .connectTimeout(8, TimeUnit.SECONDS)
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

    private val micWatchdogRunnable = object : Runnable {
        override fun run() {
            val now = System.currentTimeMillis()
            if (!isSpeaking && !isProcessing && (!isListeningNow || (now - lastMicStartTime > 6500L))) {
                freshRestartMic()
            }
            mainHandler.postDelayed(this, 3200L)
        }
    }

    private val hideOverlayRunnable = Runnable {
        if (!isSpeaking && !isProcessing && !isAwakeForCommand && pillInlineEdit.visibility != View.VISIBLE) {
            rootOverlayLayout.visibility = View.GONE
            imageCardView.visibility = View.GONE
            MainActivity.sphereView?.isActive = false
            bixbyOrbIcon.isAnimating = false
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startAsForeground()
        acquireWakeLock()

        when (intent?.getStringExtra("ACTION")) {
            "WAKE_NOW" -> {
                isProcessing = false
                isSpeaking = false
                sentenceBuffer.clear()
                latestPartialChunk = ""
                imageCardView.visibility = View.GONE
                wakeUpBixbyPill("Dinliyorum...", showVisualPill = !MainActivity.isChatScreenOpen)
            }
            "WAKE_SILENT_IN_CHAT" -> {
                // Sohbet ekranındayken sağ alttan bar çıkarma, doğrudan dinle ve sohbete yaz!
                isProcessing = false
                isSpeaking = false
                sentenceBuffer.clear()
                latestPartialChunk = ""
                rootOverlayLayout.visibility = View.GONE
                wakeUpBixbyPill("Dinliyorum...", showVisualPill = false)
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
                if (MainActivity.isChatScreenOpen || (!isAwakeForCommand && !isSpeaking)) {
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
                val vLabel = AppConfig.VOICE_LIST.firstOrNull { it.first == p }?.third ?: "Claude Sesim"
                speakHighRatedVoice("Merhaba ${AppConfig.getUserName(this)}, ben ${AppConfig.getName(this)}. Şu an $vLabel ile konuşuyorum.")
            }
            "RELOAD_CONFIG" -> {
                applyHighestRatedVoiceToTts()
                mainHandler.post {
                    applyBixbyDynamicColor()
                    pillPromptTv.text = AppConfig.getName(this).ifEmpty { "Asistan" }
                    startAsForeground()
                }
            }
        }
        return START_STICKY
    }

    private fun acquireWakeLock() {
        if (wakeLock == null) {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MasterAI::AlwaysOnMicV17")
            wakeLock?.acquire()
        }
    }

    private fun startAsForeground() {
        val channelId = "master_ai_v17_channel"
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

    // SAMSUNG BIXBY TARZI DİNAMİK RENKLİ YÜZEN KAPSÜL TASARIMI
    private fun applyBixbyDynamicColor() {
        val accent = AppConfig.getSphereColor(this)
        val r = Color.red(accent)
        val g = Color.green(accent)
        val b = Color.blue(accent)

        bixbyOrbIcon.mainColor = accent
        bixbyOrbIcon.invalidate()

        waveView.barColor = accent
        waveView.invalidate()

        bixbyPill.background = GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            intArrayOf(
                Color.argb(242, (r * 0.22f).toInt().coerceAtLeast(14), (g * 0.22f).toInt().coerceAtLeast(14), (b * 0.22f).toInt().coerceAtLeast(20)),
                Color.parseColor("#F2101018"),
                Color.argb(242, (r * 0.16f).toInt().coerceAtLeast(12), (g * 0.16f).toInt().coerceAtLeast(12), (b * 0.16f).toInt().coerceAtLeast(18))
            )
        ).apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 110f
            setStroke(3, Color.argb(210, r, g, b))
        }

        imageCardView.background = GradientDrawable().apply {
            cornerRadius = 42f
            setColor(Color.parseColor("#F2101018"))
            setStroke(3, Color.argb(200, r, g, b))
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

        val accent = AppConfig.getSphereColor(this)

        rootOverlayLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.END
            visibility = View.GONE
        }

        imageCardView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            setPadding(18, 18, 18, 18)
            layoutParams = LinearLayout.LayoutParams(560, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = 16 }
        }

        resultImageView = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 340)
            scaleType = ImageView.ScaleType.CENTER_CROP
        }

        imageCaptionTv = TextView(this).apply {
            textSize = 12f
            setTextColor(Color.WHITE)
            setPadding(12, 10, 12, 4)
        }

        imageCardView.addView(resultImageView)
        imageCardView.addView(imageCaptionTv)

        // SAMSUNG BIXBY KAPSÜLÜ: Sol (Bixby Canlı Küre İkonu) | Orta (Kaydırılabilir Yanıt) | Sağ (Ses Dalgası & Kapat)
        bixbyPill = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(28, 20, 28, 20)
        }

        bixbyOrbIcon = BixbyOrbIconView(this, accent).apply {
            layoutParams = LinearLayout.LayoutParams(82, 82).apply { rightMargin = 22 }
            setOnClickListener {
                if (sentenceBuffer.isNotEmpty() || latestPartialChunk.isNotEmpty()) {
                    mainHandler.removeCallbacks(commitSpeechRunnable)
                    commitSpeechRunnable.run()
                } else {
                    wakeUpBixbyPill("Dinliyorum...", showVisualPill = true)
                }
            }
        }

        pillScrollWrapper = ScrollView(this).apply {
            isVerticalScrollBarEnabled = true
            layoutParams = LinearLayout.LayoutParams(450, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                rightMargin = 18
            }
        }

        pillPromptTv = TextView(this).apply {
            text = AppConfig.getName(this@OverlayService).ifEmpty { "Asistan" }
            textSize = 15f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            maxLines = 4
            movementMethod = ScrollingMovementMethod.getInstance()
            setOnClickListener {
                toggleInlineTyping(true)
            }
        }
        pillScrollWrapper.addView(pillPromptTv)

        pillInlineEdit = EditText(this).apply {
            hint = "${AppConfig.getName(this@OverlayService).ifEmpty { "Asistan" }}'a yazın..."
            setHintTextColor(Color.parseColor("#A1A1AA"))
            setTextColor(Color.WHITE)
            textSize = 15f
            background = null
            visibility = View.GONE
            minWidth = 340
            maxWidth = 450
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

        waveView = GeminiWaveIconView(this).apply {
            barColor = accent
            layoutParams = LinearLayout.LayoutParams(56, 56).apply { rightMargin = 14 }
            setOnClickListener {
                wakeUpBixbyPill("Dinliyorum...", showVisualPill = true)
            }
        }

        val closePillBtn = TextView(this).apply {
            text = "✕"
            textSize = 16f
            setTextColor(Color.parseColor("#CBD5E1"))
            setPadding(12, 6, 8, 6)
            setOnClickListener {
                stopAllAudioAndReleaseFocus()
                isProcessing = false
                isAwakeForCommand = false
                sentenceBuffer.clear()
                latestPartialChunk = ""
                rootOverlayLayout.visibility = View.GONE
                imageCardView.visibility = View.GONE
                bixbyOrbIcon.isAnimating = false
                freshRestartMic()
            }
        }

        bixbyPill.addView(bixbyOrbIcon)
        bixbyPill.addView(pillScrollWrapper)
        bixbyPill.addView(pillInlineEdit)
        bixbyPill.addView(waveView)
        bixbyPill.addView(closePillBtn)

        applyBixbyDynamicColor()

        rootOverlayLayout.addView(imageCardView)
        rootOverlayLayout.addView(bixbyPill)

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

        mainHandler.postDelayed({ freshRestartMic() }, 250)
        mainHandler.postDelayed(micWatchdogRunnable, 3200L)
    }

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
                pillScrollWrapper.visibility = View.GONE
                pillInlineEdit.visibility = View.VISIBLE
                overlayParams.flags = WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
                windowManager.updateViewLayout(rootOverlayLayout, overlayParams)
                pillInlineEdit.requestFocus()
            } else {
                pillInlineEdit.visibility = View.GONE
                pillScrollWrapper.visibility = View.VISIBLE
                overlayParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
                windowManager.updateViewLayout(rootOverlayLayout, overlayParams)
            }
        }
    }

    private fun wakeUpBixbyPill(text: String, showVisualPill: Boolean = !MainActivity.isChatScreenOpen) {
        mainHandler.post {
            mainHandler.removeCallbacks(hideOverlayRunnable)
            mainHandler.removeCallbacks(commitSpeechRunnable)
            stopAllAudioAndReleaseFocus()
            isProcessing = false
            isSpeaking = false
            isAwakeForCommand = true
            applyBixbyDynamicColor()
            bixbyOrbIcon.isAnimating = true
            bixbyOrbIcon.invalidate()

            // SOHBET EKRANINDAYKEN SAĞ ALTTAN BAR ÇIKARMA!
            if (showVisualPill && !MainActivity.isChatScreenOpen) {
                rootOverlayLayout.visibility = View.VISIBLE
            } else {
                rootOverlayLayout.visibility = View.GONE
            }

            pillPromptTv.text = text
            pillPromptTv.scrollTo(0, 0)
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
            bixbyOrbIcon.isAnimating = true
            bixbyOrbIcon.audioAmp = 4.5f
            waveView.activeWave = true
            waveView.invalidate()
        }
    }

    private fun onSpeechDone() {
        mainHandler.post {
            stopAllAudioAndReleaseFocus()
            isProcessing = false
            bixbyOrbIcon.audioAmp = 0f
            waveView.activeWave = false
            waveView.invalidate()

            val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val isMediaPlayingOnTablet = am.isMusicActive

            if (isMediaPlayingOnTablet || MainActivity.isChatScreenOpen) {
                isAwakeForCommand = false
                mainHandler.postDelayed(hideOverlayRunnable, 3000L)
            } else {
                isAwakeForCommand = true
                mainHandler.removeCallbacks(hideOverlayRunnable)
                mainHandler.postDelayed(hideOverlayRunnable, 6500L)
            }
            freshRestartMic()
        }
    }

    // 10 CLAUDE & OPENAI STÜDYO SES MOTORU
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

        val profile = AppConfig.getVoiceProfile(this)
        val speed = AppConfig.getSpeed(this)

        tryClaudeOpenAiStudioVoice(clean, profile) { openAiSuccess ->
            if (!openAiSuccess) {
                speakPure500QualityWavenet(clean, profile, speed)
            }
        }
    }

    private fun tryClaudeOpenAiStudioVoice(text: String, profile: Int, callback: (Boolean) -> Unit) {
        val voiceCode = AppConfig.VOICE_LIST.firstOrNull { it.first == profile }?.second ?: "ash"

        val payload = JSONObject().apply {
            put("model", "openai-audio")
            put("modalities", JSONArray().put("text").put("audio"))
            put("audio", JSONObject().apply {
                put("voice", voiceCode)
                put("format", "mp3")
            })
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", "Verilen metni hiçbir kelime eklemeden, olduğu gibi çok doğal, akıcı ve sıcak bir Türkçe insan sesiyle oku.")
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", text.take(280))
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
                            val mp3File = File(cacheDir, "claude_studio_v17.mp3")
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

        val profile = AppConfig.getVoiceProfile(this) // 0..4: Kadın, 5..9: Erkek
        val speed = AppConfig.getSpeed(this)
        val wantFemale = (profile in 0..4)

        try {
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
                    if (maleVoices.isNotEmpty()) maleVoices[(profile - 5).coerceAtLeast(0) % maleVoices.size] else allVoices.lastOrNull()
                }
                if (selected != null) tts?.voice = selected
            }
        } catch (e: Exception) {}

        val pitchVal = if (wantFemale) 1.0f + (profile * 0.02f) else 0.96f - ((profile - 5) * 0.03f)
        tts?.setPitch(pitchVal.coerceIn(0.84f, 1.08f))
        tts?.setSpeechRate(speed)
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
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 850L)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 700L)
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
                            bixbyOrbIcon.audioAmp = lvl
                            waveView.activeWave = lvl > 2.2f
                            waveView.invalidate()
                        }
                    }
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() { isListeningNow = false }
                    override fun onError(error: Int) {
                        isListeningNow = false
                        if (!isSpeaking && !isProcessing) {
                            mainHandler.postDelayed({ freshRestartMic() }, 90L)
                        }
                    }
                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) ?: return
                        if (matches.isEmpty()) return

                        val aiName = AppConfig.getName(this@OverlayService)
                        if (aiName.isEmpty()) return

                        val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
                        val isMediaPlaying = am.isMusicActive

                        val matchedCandidate = matches.firstOrNull { containsWakeWordSmart(it, aiName, strictForMedia = isMediaPlaying) }

                        if (!isAwakeForCommand && matchedCandidate != null) {
                            sentenceBuffer.clear()
                            val afterWake = stripWakeWordSmart(matchedCandidate, aiName)
                            latestPartialChunk = afterWake
                            isAwakeForCommand = true
                            if (!MainActivity.isChatScreenOpen) {
                                rootOverlayLayout.visibility = View.VISIBLE
                            }
                            bixbyOrbIcon.isAnimating = true
                            pillPromptTv.text = afterWake.ifEmpty { "Dinliyorum..." }
                            MainActivity.centerSubTv?.text = afterWake.ifEmpty { "Dinliyorum..." }
                            resetFastSilenceTimer()
                        } else if (isAwakeForCommand) {
                            val partial = matches[0].trim()
                            if (partial.isNotEmpty()) {
                                latestPartialChunk = stripWakeWordSmart(partial, aiName)
                                val preview = (sentenceBuffer.toString() + " " + latestPartialChunk).trim()
                                pillPromptTv.text = preview.ifEmpty { "Dinliyorum..." }
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

                        val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
                        val isMediaPlaying = am.isMusicActive

                        val matchedWake = matches.firstOrNull { containsWakeWordSmart(it, aiName, strictForMedia = isMediaPlaying) }

                        if (!isAwakeForCommand && matchedWake != null) {
                            val clean = stripWakeWordSmart(matchedWake, aiName)
                            sentenceBuffer.clear()
                            latestPartialChunk = ""
                            if (clean.isNotEmpty()) {
                                isAwakeForCommand = false
                                processFullCommand(clean)
                            } else {
                                // SADECE "DUMAN" / "FINDIK" DENİLDİĞİNDE GEREKSİZ TWITTER VB. ARAŞTIRMASI YAPMAZ!
                                wakeUpBixbyPill("Efendim, seni dinliyorum...", showVisualPill = !MainActivity.isChatScreenOpen)
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
                mainHandler.postDelayed({ freshRestartMic() }, 350L)
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

    private fun containsWakeWordSmart(spoken: String, aiName: String, strictForMedia: Boolean = false): Boolean {
        val normSpoken = normalizeTr(spoken).trim()
        val normTarget = normalizeTr(aiName.trim())
        if (normTarget.isEmpty()) return false

        val words = normSpoken.split(" ").filter { it.isNotBlank() }
        if (words.isEmpty()) return false

        if (strictForMedia) {
            val firstTwo = words.take(2)
            return firstTwo.any { it == normTarget || levenshteinSimilarity(it, normTarget) >= 0.80f }
        }

        if (normSpoken.contains(normTarget)) return true
        for (w in words.take(3)) {
            if (w.startsWith(normTarget) || levenshteinSimilarity(w, normTarget) >= 0.65f) {
                return true
            }
            if (w.length > normTarget.length && levenshteinSimilarity(w.take(normTarget.length), normTarget) >= 0.74f) {
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
            if (w.contains(normTarget) || levenshteinSimilarity(w.take(normTarget.length.coerceAtLeast(1)), normTarget) >= 0.65f) {
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
            mainHandler.postDelayed(commitSpeechRunnable, 720L)
        }
    }

    private fun didUserExplicitlyAskForPhoto(lowerCmd: String): Boolean {
        val photoTriggers = listOf(
            "fotoğraf", "fotograf", "foto ", "fotosu", "resim", "resmi", "görsel", "gorsel",
            "çiz", "ciz", "fotoğrafıyla", "resmiyle", "görseliyle"
        )
        return photoTriggers.any { lowerCmd.contains(it) }
    }

    // SÜPER AKILLI KOMUT İŞLEYİCİ v17.0:
    // - Sadece "Duman" / "Fındık" denildiğinde saçma araştırma yapmaz!
    // - "Google Haritaları açıp Gaziantep'e yol tarifi al" dediğinde gerçek Google Maps Navigasyonu başlatır!
    private fun processFullCommand(command: String) {
        val trimmedCmd = command.trim()
        val lower = trimmedCmd.lowercase(Locale("tr", "TR"))
        val aiName = AppConfig.getName(this).ifEmpty { "Asistan" }

        // Sadece asistanın adı söylendiyse ("duman", "fındık", "hey duman") Twitter vb. arama yapma!
        val strippedCheck = stripWakeWordSmart(lower, aiName)
        if (strippedCheck.isEmpty() || strippedCheck.length <= 2) {
            wakeUpBixbyPill("Efendim ${AppConfig.getUserName(this)}, seni dinliyorum?", showVisualPill = !MainActivity.isChatScreenOpen)
            speakHighRatedVoice("Efendim ${AppConfig.getUserName(this)}, seni dinliyorum?")
            return
        }

        isProcessing = true
        AppConfig.addMessageToCurrentSession(this, "user", trimmedCmd)
        MainActivity.onNewMessageCallback?.invoke()

        mainHandler.post {
            imageCardView.visibility = View.GONE
            if (!MainActivity.isChatScreenOpen) {
                rootOverlayLayout.visibility = View.VISIBLE
            } else {
                rootOverlayLayout.visibility = View.GONE
            }
            bixbyOrbIcon.isAnimating = true
            pillPromptTv.text = "⏳ $trimmedCmd"
            MainActivity.centerSubTv?.text = "⏳ $trimmedCmd"
        }

        // 1. Bankacılık Güvenlik Kilidi
        val bankingKeywords = listOf(
            "banka", "garanti", "ziraat", "iş bankası", "iscep", "akbank", "yapı kredi",
            "qnb", "finansbank", "vakıfbank", "halkbank", "denizbank", "teb", "kuveyttürk",
            "enpara", "papara", "iban", "havale", "eft"
        )
        if (bankingKeywords.any { lower.contains(it) } && (lower.contains("aç") || lower.contains("gönder") || lower.contains("transfer"))) {
            replyAndShow("Güvenlik protokolü gereği bankacılık işlemlerine erişimim kapalıdır.", null)
            return
        }

        // 2. GERÇEK GOOGLE HARİTALAR NAVİGASYON MOTORU ("Gaziantep'e yol tarifi al", "Haritaları açıp Ankara'ya rota oluştur" vb.)
        if (lower.contains("yol tarifi") || lower.contains("rota ") || lower.contains("navigasyon") || (lower.contains("harita") && (lower.contains("git") || lower.contains("tarif") || lower.contains("nerede")))) {
            val dest = extractDestinationFromMapCommand(lower)
            startGoogleMapsNavigation(dest)
            return
        }

        // 3. BİRLEŞİK KOMUT YAKALAYICI ("Google açıp EBA yaz", "YouTube açıp ... aç")
        val compoundRegex = Regex("(.+?)\\s+(?:açıp|acip|aç ve|ac ve|girip)\\s+(.+?)\\s*(?:yaz|ara|arat|aç|ac)?$")
        val compoundMatch = compoundRegex.find(lower)
        if (compoundMatch != null && !lower.contains("whatsapp") && !lower.contains("mesaj") && !lower.contains("harita")) {
            val rawAppPart = compoundMatch.groupValues[1]
                .replace("'ı", "").replace("'i", "").replace("'u", "").replace("'ü", "")
                .replace("yi", "").replace("yı", "").replace("yu", "").replace("yü", "").trim()
            val queryToType = compoundMatch.groupValues[2].trim()

            if (rawAppPart.contains("google") || rawAppPart.contains("chrome") || rawAppPart.contains("internet") || rawAppPart.contains("tarayıcı")) {
                openGoogleAndSearch(queryToType)
                return
            } else if (rawAppPart.contains("youtube")) {
                findAndPlayYouTubeVideoDirectly(queryToType)
                return
            } else if (rawAppPart.isNotEmpty() && queryToType.isNotEmpty()) {
                AsistanOtonomService.pendingAutoTypeText = queryToType
                if (openAnyInstalledApp(rawAppPart, customReply = "$rawAppPart açılıyor ve \"$queryToType\" yazılıyor.")) {
                    return
                }
            }
        }

        // 4. DOĞRUDAN GOOGLE WEB ARAMASI ("Google'a EBA yaz")
        if ((lower.contains("google") || lower.contains("chrome") || lower.contains("internette")) && (lower.contains("yaz") || lower.contains("ara") || lower.contains("bak")) && !lower.contains("harita")) {
            val q = lower
                .replace("google'ı açıp", "").replace("google açıp", "")
                .replace("google'da", "").replace("googleda", "").replace("google'a", "").replace("googlea", "").replace("google", "")
                .replace("chrome'da", "").replace("internette", "")
                .replace("diye yaz", "").replace("yazıp ara", "").replace("yaz", "").replace("ara", "").replace("bak", "").replace("aç", "")
                .trim()
            if (q.isNotEmpty()) {
                openGoogleAndSearch(q)
                return
            }
        }

        // 5. KAMERAYI AÇIP FOTOĞRAF ÇEKME
        if (lower.contains("fotoğraf çek") || lower.contains("foto çek") || lower.contains("resim çek") || lower.contains("selfie çek") || lower.contains("özçekim")) {
            capturePhotoAutomatically(isSelfie = lower.contains("selfie") || lower.contains("özçekim") || lower.contains("ön kamera"))
            return
        }

        // 6. YÜKSEK KALİTELİ GERÇEKÇİ GÖRSEL ÜRETİM (Flux AI Motoru - Hem Sohbette Hem Sağ Altta)
        if (lower.contains("resmi çiz") || lower.contains("resim çiz") || lower.contains("görsel oluştur") || lower.contains("görseli oluştur") || lower.contains("resmi oluştur") || lower.contains("resim üret") || lower.contains("görsel üret") || lower.contains("fotoğrafını göster") || lower.contains("resmini göster")) {
            val imgPrompt = lower
                .replace("bana", "").replace("resmi çiz", "").replace("resim çiz", "")
                .replace("görsel oluştur", "").replace("görseli oluştur", "").replace("resmi oluştur", "")
                .replace("resim üret", "").replace("görsel üret", "")
                .replace("fotoğrafını göster", "").replace("resmini göster", "").trim()
                .ifEmpty { trimmedCmd }
            generateHighQualityFluxImage(imgPrompt)
            return
        }

        // 7. WHATSAPP MESAJ GÖNDERME
        if (lower.contains("whatsapp") || lower.contains("mesaj at") || lower.contains("mesaj gönder") || lower.contains("diye yaz")) {
            if (handleWhatsAppMessageCommand(trimmedCmd, lower)) return
        }

        // 8. TELEFONLA ARAMA
        if (lower.endsWith(" ara") && !lower.contains("internette") && !lower.contains("google") && !lower.contains("youtube") && !lower.contains("araştır")) {
            val targetPerson = lower.removeSuffix(" ara").replace("yi", "").replace("yı", "").replace("i", "").replace("ı", "").trim()
            if (targetPerson.isNotEmpty() && dialContactByName(targetPerson)) return
        }

        // 9. ALARM KURMA
        if (lower.contains("alarm kur") || lower.contains("alarmı kur")) {
            if (handleSetAlarm(lower)) return
        }

        // 10. YOUTUBE DİREKT VİDEO OYNATMA
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

        // 11. EVRENSEL UYGULAMA AÇICI
        if (lower.contains("aç") || lower.endsWith("gir")) {
            if (lower.contains("kamera")) {
                try {
                    startActivity(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    replyAndShow("Kamera açılıyor.", null)
                    return
                } catch (e: Exception) {}
            }
            val cleanedApp = extractCleanAppName(lower)
            if (cleanedApp.isNotEmpty() && openAnyInstalledApp(cleanedApp)) return
        }

        // 12. AKILLI NİYET ANALİZİ: Gereksiz web araması yapmadan doğrudan en güçlü modele sor
        val allowPhoto = didUserExplicitlyAskForPhoto(lower)
        val needsWebSearch = lower.contains("kaç") || lower.contains("kimdir") || lower.contains("nedir") ||
                lower.contains("nerede") || lower.contains("ne zaman") || lower.contains("haber") ||
                lower.contains("hava durumu") || lower.contains("dolar") || lower.contains("altın") ||
                lower.contains("maç") || lower.contains("araştır")

        if (needsWebSearch) {
            fetchLiveWebContext(trimmedCmd) { webContext ->
                askMultiEngineAI(trimmedCmd, webContext, allowPhoto)
            }
        } else {
            askMultiEngineAI(trimmedCmd, "", allowPhoto)
        }
    }

    // "Google Haritaları açıp Gaziantep'e yol tarifi al" cümlesinden saf hedefi ("Gaziantep") ayıklar ve gerçek navigasyonu başlatır!
    private fun extractDestinationFromMapCommand(lower: String): String {
        var s = lower
            .replace("google haritaları açıp", "")
            .replace("google haritalar'ı açıp", "")
            .replace("google haritalari acip", "")
            .replace("haritaları açıp", "")
            .replace("haritalari acip", "")
            .replace("haritayı açıp", "")
            .replace("google haritalarda", "")
            .replace("google haritalar", "")
            .replace("haritalarda", "")
            .replace("haritada", "")
            .replace("haritalar", "")
            .replace("yol tarifi al", "")
            .replace("yol tarifi aç", "")
            .replace("yol tarifi ver", "")
            .replace("yol tarifi", "")
            .replace("navigasyonu başlat", "")
            .replace("navigasyon aç", "")
            .replace("rota oluştur", "")
            .replace("rotası oluştur", "")
            .replace("bana", "")
            .replace("lütfen", "")
            .replace("aç", "")
            .trim()

        // Türkçe yönelme eklerini temizle: "gaziantep'e" -> "gaziantep", "ankara'ya" -> "ankara", "istanbula" -> "istanbul"
        s = s.replace(Regex("'(?:e|a|ye|ya)$"), "")
        val words = s.split(" ").filter { it.isNotBlank() }.toMutableList()
        if (words.isNotEmpty()) {
            val last = words.last()
            val cleanedLast = when {
                last.endsWith("ye") || last.endsWith("ya") -> last.dropLast(2)
                last.endsWith("e") || last.endsWith("a") -> last.dropLast(1)
                else -> last
            }
            if (cleanedLast.length >= 3) {
                words[words.lastIndex] = cleanedLast
            }
        }
        return words.joinToString(" ").trim().ifEmpty { "Gaziantep" }
    }

    private fun startGoogleMapsNavigation(destination: String) {
        try {
            val encodedDest = URLEncoder.encode(destination, "UTF-8")
            // Doğrudan adım adım Google Maps Navigasyonunu başlatır!
            val navUri = Uri.parse("google.navigation:q=$encodedDest")
            val navIntent = Intent(Intent.ACTION_VIEW, navUri).apply {
                setPackage("com.google.android.apps.maps")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (navIntent.resolveActivity(packageManager) != null) {
                startActivity(navIntent)
            } else {
                val webMaps = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$encodedDest")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(webMaps)
            }
            replyAndShow("$destination için Google Haritalar yol tarifi ve navigasyon başlatılıyor.", null)
        } catch (e: Exception) {
            replyAndShow("Harita navigasyonu başlatılamadı.", null)
        }
    }

    // YÜKSEK KALİTELİ GERÇEKÇİ RESİM ÜRETİCİ (Flux Model + Otomatik İngilizce Çeviri + Sohbet İçi Tam Ekran Gösterim)
    private fun generateHighQualityFluxImage(turkishPrompt: String) {
        // Önce Türkçe isteği zengin bir İngilizce Flux promptuna çevir ki saçma resimler üretmesin!
        val translatePrompt = "Translate this image description into a detailed, photorealistic English prompt for Flux image generator (only output the English prompt, nothing else): $turkishPrompt"
        val url = "https://text.pollinations.ai/${URLEncoder.encode(translatePrompt, "UTF-8")}?model=openai"

        client.newCall(Request.Builder().url(url).get().build()).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                deliverFluxImage(turkishPrompt, "$turkishPrompt, masterpiece, photorealistic, 8k, highly detailed")
            }
            override fun onResponse(call: Call, response: Response) {
                val eng = response.body?.string()?.trim()?.takeIf { it.isNotEmpty() && !it.contains("limit", true) }
                    ?: "$turkishPrompt, photorealistic, 8k resolution, cinematic lighting"
                deliverFluxImage(turkishPrompt, eng)
            }
        })
    }

    private fun deliverFluxImage(originalTrTitle: String, englishFluxPrompt: String) {
        val encoded = URLEncoder.encode("$englishFluxPrompt, ultra detailed, high quality, 8k", "UTF-8")
        val fluxUrl = "https://image.pollinations.ai/prompt/$encoded?model=flux&width=768&height=512&nologo=true&enhance=true&seed=${System.currentTimeMillis() % 100000}"

        val answerText = "🎨 \"$originalTrTitle\" görseli yüksek kalitede hazırlandı."
        // Hem sohbet geçmişine resimli olarak kaydet hem de sağ alt kartta göster!
        AppConfig.addMessageToCurrentSession(this, "assistant", answerText, imageUrl = fluxUrl)
        MainActivity.onNewMessageCallback?.invoke()

        mainHandler.post {
            if (!MainActivity.isChatScreenOpen) {
                rootOverlayLayout.visibility = View.VISIBLE
                downloadAndDisplayBitmap(fluxUrl, originalTrTitle) {}
            } else {
                rootOverlayLayout.visibility = View.GONE
            }
            pillPromptTv.text = answerText
            MainActivity.centerSubTv?.text = answerText
            speakHighRatedVoice(answerText)
        }
    }

    private fun openGoogleAndSearch(query: String) {
        try {
            val url = "https://www.google.com/search?q=${URLEncoder.encode(query, "UTF-8")}"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
            replyAndShow("Google açıldı ve \"$query\" aratıldı.", null)
        } catch (e: Exception) {
            replyAndShow("Google araması başlatılamadı.", null)
        }
    }

    private fun extractCleanAppName(lower: String): String {
        var s = lower
            .replace("uygulamasını aç", "")
            .replace("uygulamasini ac", "")
            .replace("uygulamasını", "")
            .replace("programını aç", "")
            .replace("oyununu aç", "")
            .replace("lütfen", "")
            .replace("hemen", "")
            .replace("bana", "")
            .replace("aç", "")
            .replace("gir", "")
            .trim()

        val suffixes = listOf("'ı", "'i", "'u", "'ü", "'a", "'e", "'ya", "'ye", "yı", "yi", "yu", "yü")
        for (suf in suffixes) {
            if (s.endsWith(suf) && s.length > suf.length + 2) {
                s = s.dropLast(suf.length).trim()
                break
            }
        }
        if (s.length > 4 && (s.endsWith("ı") || s.endsWith("i") || s.endsWith("u") || s.endsWith("ü"))) {
            val withoutLast = s.dropLast(1)
            if (s != "galeri") {
                s = withoutLast
            }
        }
        return s.trim()
    }

    private fun openAnyInstalledApp(targetName: String, customReply: String? = null): Boolean {
        val normTarget = normalizeTr(targetName).trim()
        if (normTarget.isEmpty()) return false

        if (normTarget.contains("ayarlar")) {
            startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            replyAndShow(customReply ?: "Ayarlar açılıyor.", null)
            return true
        }

        if (normTarget == "google" || normTarget == "chrome" || normTarget == "internet" || normTarget == "tarayici") {
            val gLaunch = packageManager.getLaunchIntentForPackage("com.google.android.googlequicksearchbox")
                ?: packageManager.getLaunchIntentForPackage("com.android.chrome")
                ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com"))
            gLaunch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(gLaunch)
            replyAndShow(customReply ?: "Google açılıyor.", null)
            return true
        }

        val aliasPackages = mapOf(
            "whatsapp" to listOf("com.whatsapp", "com.whatsapp.w4b"),
            "vatsap" to listOf("com.whatsapp"),
            "youtube" to listOf("com.google.android.youtube"),
            "yutup" to listOf("com.google.android.youtube"),
            "instagram" to listOf("com.instagram.android"),
            "insta" to listOf("com.instagram.android"),
            "eba" to listOf("tr.gov.eba.hesap", "air.tr.gov.eba.ekitap", "tr.gov.eba.ders"),
            "brawl stars" to listOf("com.supercell.brawlstars"),
            "clash royale" to listOf("com.supercell.clashroyale"),
            "satranc" to listOf("com.chess"),
            "chess" to listOf("com.chess"),
            "minecraft" to listOf("com.mojang.minecraftpe"),
            "spotify" to listOf("com.spotify.music"),
            "tiktok" to listOf("com.zhiliaoapp.musically", "com.ss.android.ugc.trill"),
            "telegram" to listOf("org.telegram.messenger"),
            "harita" to listOf("com.google.android.apps.maps"),
            "haritalar" to listOf("com.google.android.apps.maps"),
            "google haritalar" to listOf("com.google.android.apps.maps"),
            "maps" to listOf("com.google.android.apps.maps"),
            "galeri" to listOf("com.sec.android.gallery3d", "com.google.android.apps.photos", "com.miui.gallery"),
            "saat" to listOf("com.sec.android.app.clockpackage", "com.google.android.deskclock"),
            "hesap makinesi" to listOf("com.sec.android.app.popupcalculator", "com.google.android.calculator"),
            "notlar" to listOf("com.samsung.android.app.notes", "com.google.android.keep"),
            "play store" to listOf("com.android.vending"),
            "dosyalar" to listOf("com.sec.android.app.myfiles", "com.google.android.apps.nbu.files"),
            "takvim" to listOf("com.samsung.android.calendar", "com.google.android.calendar")
        )

        for ((alias, pkgList) in aliasPackages) {
            if (normTarget.contains(alias) || levenshteinSimilarity(normTarget, alias) >= 0.75f) {
                for (pkg in pkgList) {
                    val launch = packageManager.getLaunchIntentForPackage(pkg)
                    if (launch != null) {
                        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(launch)
                        replyAndShow(customReply ?: "$targetName açılıyor.", null)
                        return true
                    }
                }
            }
        }

        val pm = packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = pm.queryIntentActivities(mainIntent, 0)

        var bestMatchPkg: String? = null
        var bestMatchLabel = ""
        var bestScore = 0f

        for (info in resolveInfos) {
            val pkg = info.activityInfo.packageName.lowercase(Locale.US)
            val rawLabel = info.loadLabel(pm).toString()
            val normLabel = normalizeTr(rawLabel).trim()

            if (normLabel == normTarget || pkg.endsWith(".$normTarget")) {
                bestMatchPkg = info.activityInfo.packageName
                bestMatchLabel = rawLabel
                bestScore = 1.0f
                break
            }

            if (normLabel.contains(normTarget) || normTarget.contains(normLabel)) {
                val score = 0.88f
                if (score > bestScore) {
                    bestScore = score
                    bestMatchPkg = info.activityInfo.packageName
                    bestMatchLabel = rawLabel
                }
            } else {
                val sim = levenshteinSimilarity(normLabel, normTarget)
                if (sim > bestScore) {
                    bestScore = sim
                    bestMatchPkg = info.activityInfo.packageName
                    bestMatchLabel = rawLabel
                }
            }
        }

        if (bestMatchPkg != null && bestScore >= 0.58f) {
            val lowPkg = bestMatchPkg.lowercase(Locale.US)
            if (lowPkg.contains("bank") || lowPkg.contains("garanti") || lowPkg.contains("ziraat") || lowPkg.contains("akbank") || lowPkg.contains("isbank") || lowPkg.contains("yapikredi")) {
                replyAndShow("Bankacılık uygulamalarına erişimim kısıtlıdır.", null)
                return true
            }
            val launch = pm.getLaunchIntentForPackage(bestMatchPkg)
            if (launch != null) {
                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(launch)
                replyAndShow(customReply ?: "$bestMatchLabel açılıyor.", null)
                return true
            }
        }

        if (normTarget == "eba") {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.eba.gov.tr")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            replyAndShow("EBA açılıyor.", null)
            return true
        }

        return false
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

    private fun replyAndShow(answer: String, imageKeyword: String?) {
        val resolvedImgUrl = if (!imageKeyword.isNullOrBlank()) {
            "https://image.pollinations.ai/prompt/${URLEncoder.encode("$imageKeyword, photorealistic, 8k", "UTF-8")}?model=flux&width=768&height=512&nologo=true"
        } else null

        AppConfig.addMessageToCurrentSession(this, "assistant", answer, imageUrl = resolvedImgUrl)
        MainActivity.onNewMessageCallback?.invoke()

        mainHandler.post {
            applyBixbyDynamicColor()
            // SOHBET EKRANI AÇIKSA SAĞ ALTTAN BAR ÇIKARMA!
            if (!MainActivity.isChatScreenOpen) {
                rootOverlayLayout.visibility = View.VISIBLE
            } else {
                rootOverlayLayout.visibility = View.GONE
            }
            pillPromptTv.text = answer
            pillPromptTv.scrollTo(0, 0)
            MainActivity.centerSubTv?.text = answer
            if (!imageKeyword.isNullOrBlank() && !MainActivity.isChatScreenOpen) {
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
        val wikiUrl = "https://$langPrefix.wikipedia.org/w/api.php?action=query&prop=pageimages&format=json&piprop=thumbnail&pithumbsize=600&generator=search&gsrsearch=${URLEncoder.encode(cleanKw, "UTF-8")}&gsrlimit=1"

        val req = Request.Builder().url(wikiUrl).header("User-Agent", "GeminiMaster/17.0").build()
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
                    val finalUrl = thumbUrl ?: "https://image.pollinations.ai/prompt/${URLEncoder.encode("$cleanKw, photorealistic", "UTF-8")}?model=flux&width=560&height=340&nologo=true"
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
                    if (!MainActivity.isChatScreenOpen) {
                        rootOverlayLayout.visibility = View.VISIBLE
                        resultImageView.setImageBitmap(bmp)
                        imageCaptionTv.text = "📸 $caption (Büyütmek için dokunun)"
                        imageCardView.visibility = View.VISIBLE
                        imageCardView.setOnClickListener {
                            // Sağ alttaki resme dokununca Sohbet ekranını açıp resmi tam boy gösterir!
                            imageCardView.visibility = View.GONE
                            val i = Intent(this@OverlayService, MainActivity::class.java).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            startActivity(i)
                        }
                    }
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
        client.newCall(Request.Builder().url(wikiUrl).header("User-Agent", "MasterAI/17.0").build()).enqueue(object : Callback {
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
        val startIdx = (historyArr.length() - 10).coerceAtLeast(0)
        for (i in startIdx until historyArr.length()) {
            val m = historyArr.getJSONObject(i)
            val r = if (m.getString("role") == "user") userName else aiName
            historyContext.append("$r: ${m.getString("text")}\n")
        }

        val photoRule = if (allowPhoto) {
            "- Kullanıcı görsel/fotoğraf istediği için cevabın sonuna [RESIM: İngilizceDetaylıGörselTanımı] ekle."
        } else {
            "- KESİNLİKLE [RESIM: ...] etiketi EKLEME! Sadece konuşarak cevap ver."
        }

        val systemRules = """
            Senin adın $aiName ($selectedMode motoru). Kullanıcının adı $userName.
            Üstün zekalı, mantıklı, esprili ve her konuya hakim kişisel yapay zeka asistanısın.
            Doğal, akıcı ve zeki Türkçe cevap ver (1-3 cümle).
            ${if (liveWebInfo.isNotEmpty()) "Güncel Canlı İnternet Verisi: $liveWebInfo" else ""}
            
            OTONOM EYLEM ETİKETLERİ (Eğer kullanıcı bir uygulama/cihaz işlemi isterse cevabın en sonuna ekle):
            - Haritada bir yere yol tarifi / navigasyon başlatmak için: [HARITA_ROTA: HedefKonum]
            - Google'da bir şey yazıp aratmak için: [GOOGLE_YAZ: AranacakMetin]
            - YouTube'da doğrudan video oynatmak için: [VIDEO: AramaTerimi]
            - WhatsApp mesajı atmak için: [WHATSAPP: KişiAdı | MesajMetni]
            - Cihazdaki herhangi bir uygulamayı açmak için: [UYGULAMA_AC: UygulamaAdı]
            $photoRule
            
            Önceki Konuşma Hafızası:
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
        val modelName = when (AppConfig.getModelChoice(this)) {
            "ARGON" -> "deepseek-reasoner"
            else -> "openai-large"
        }
        val messagesArr = JSONArray().apply {
            put(JSONObject().apply { put("role", "system"); put("content", systemRules) })
            put(JSONObject().apply { put("role", "user"); put("content", userPrompt) })
        }
        val payload = JSONObject().apply {
            put("model", modelName)
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
                        val clean = content.replace(Regex("<think>[\\s\\S]*?</think>"), "").replace("*", "").trim()
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
        val url = "https://text.pollinations.ai/${URLEncoder.encode(shortPrompt.take(900), "UTF-8")}?model=openai&seed=${System.currentTimeMillis() % 9999}"

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

        val mapRouteRegex = Regex("\\[HARITA_ROTA:\\s*(.*?)\\]")
        val mapMatch = mapRouteRegex.find(rawAnswer)
        if (mapMatch != null) {
            val dest = mapMatch.groupValues[1].trim()
            startGoogleMapsNavigation(dest)
            return
        }

        val gSearchRegex = Regex("\\[GOOGLE_YAZ:\\s*(.*?)\\]")
        val gMatch = gSearchRegex.find(rawAnswer)
        if (gMatch != null) {
            val q = gMatch.groupValues[1].trim()
            openGoogleAndSearch(q)
            return
        }

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
