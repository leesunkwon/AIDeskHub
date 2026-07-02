package com.kotlinsun.aideskhub

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.kotlinsun.aideskhub.BuildConfig
import com.kotlinsun.aideskhub.ai.GeminiRestClient
import com.kotlinsun.aideskhub.model.DeskHubScreenState
import com.kotlinsun.aideskhub.voice.VoiceInteractionController
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private val handler = Handler(Looper.getMainLooper())
    private val geminiClient = GeminiRestClient(BuildConfig.GEMINI_API_KEY)
    private val aiExecutor = Executors.newSingleThreadExecutor()
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.KOREA)
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy년 M월 d일 EEEE", Locale.KOREA)
    private lateinit var voiceInteractionController: VoiceInteractionController

    private lateinit var kioskContainer: View
    private lateinit var listeningOverlay: View
    private lateinit var resultContainer: View
    private lateinit var adminContainer: View
    private lateinit var institutionPanel: View
    private lateinit var timeText: TextView
    private lateinit var dateText: TextView
    private lateinit var noticePrimaryText: TextView
    private lateinit var noticeSecondaryText: TextView
    private lateinit var resultAnswerText: TextView
    private lateinit var routeStepText: TextView
    private lateinit var resultTitleText: TextView
    private lateinit var listeningStatusText: TextView

    private val requestAudioPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            startVoiceQuestion()
        } else {
            Toast.makeText(this, "음성 질문을 사용하려면 마이크 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private val clockRunnable = object : Runnable {
        override fun run() {
            updateClock()
            handler.postDelayed(this, CLOCK_UPDATE_INTERVAL_MS)
        }
    }

    private val idleReturnRunnable = Runnable {
        showState(DeskHubScreenState.KioskIdle)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_main)
        hideSystemBars()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            v.setPadding(0, 0, 0, 0)
            insets
        }
        bindViews()
        bindActions()
        voiceInteractionController = VoiceInteractionController(this)
        showState(DeskHubScreenState.KioskIdle)
    }

    override fun onResume() {
        super.onResume()
        hideSystemBars()
        handler.post(clockRunnable)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemBars()
        }
    }

    override fun onPause() {
        voiceInteractionController.stopListening()
        handler.removeCallbacks(clockRunnable)
        handler.removeCallbacks(idleReturnRunnable)
        super.onPause()
    }

    override fun onDestroy() {
        voiceInteractionController.destroy()
        aiExecutor.shutdownNow()
        super.onDestroy()
    }

    private fun bindViews() {
        kioskContainer = findViewById(R.id.kioskContainer)
        listeningOverlay = findViewById(R.id.listeningOverlay)
        resultContainer = findViewById(R.id.resultContainer)
        adminContainer = findViewById(R.id.adminContainer)
        institutionPanel = findViewById(R.id.institutionPanel)
        timeText = findViewById(R.id.timeText)
        dateText = findViewById(R.id.dateText)
        noticePrimaryText = findViewById(R.id.noticePrimaryText)
        noticeSecondaryText = findViewById(R.id.noticeSecondaryText)
        resultAnswerText = findViewById(R.id.resultAnswerText)
        routeStepText = findViewById(R.id.routeStepText)
        resultTitleText = findViewById(R.id.resultTitleText)
        listeningStatusText = findViewById(R.id.listeningStatusText)
    }

    private fun bindActions() {
        findViewById<View>(R.id.listenButton).setOnClickListener {
            requestVoiceQuestion()
        }
        findViewById<View>(R.id.sampleResultButton).setOnClickListener {
            submitQuestion("AI Desk Hub는 어떤 앱이야?")
        }
        findViewById<View>(R.id.adminEntryButton).setOnClickListener {
            showAdminPasswordDialog()
        }
        institutionPanel.setOnLongClickListener {
            showAdminPasswordDialog()
            true
        }
        findViewById<View>(R.id.closeResultButton).setOnClickListener {
            showState(DeskHubScreenState.KioskIdle)
        }
        findViewById<View>(R.id.adminExitButton).setOnClickListener {
            showState(DeskHubScreenState.KioskIdle)
        }
        listeningOverlay.setOnClickListener {
            voiceInteractionController.stopListening()
            showState(DeskHubScreenState.KioskIdle)
        }
    }

    private fun showState(state: DeskHubScreenState) {
        handler.removeCallbacks(idleReturnRunnable)

        kioskContainer.visibility =
            if (state == DeskHubScreenState.KioskIdle || state == DeskHubScreenState.Listening) {
                View.VISIBLE
            } else {
                View.GONE
            }
        listeningOverlay.visibility = if (state == DeskHubScreenState.Listening) View.VISIBLE else View.GONE
        resultContainer.visibility = if (state == DeskHubScreenState.Result) View.VISIBLE else View.GONE
        adminContainer.visibility = if (state == DeskHubScreenState.Admin) View.VISIBLE else View.GONE

        if (state == DeskHubScreenState.KioskIdle) {
            renderNotices()
        }
    }

    private fun requestVoiceQuestion() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            startVoiceQuestion()
        } else {
            requestAudioPermission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun startVoiceQuestion() {
        listeningStatusText.text = "질문을 말씀해 주세요"
        showState(DeskHubScreenState.Listening)
        voiceInteractionController.startListening(
            object : VoiceInteractionController.Listener {
                override fun onReady() {
                    listeningStatusText.text = "듣고 있어요"
                }

                override fun onPartialText(text: String) {
                    listeningStatusText.text = text
                }

                override fun onResult(text: String) {
                    submitQuestion(text)
                }

                override fun onError(message: String) {
                    showQuestionError(message)
                }
            },
        )
    }

    private fun submitQuestion(question: String) {
        val trimmedQuestion = question.trim()
        if (trimmedQuestion.isBlank()) {
            showQuestionError("질문을 인식하지 못했습니다. 다시 말씀해 주세요.")
            return
        }

        resultTitleText.text = "AI 답변"
        routeStepText.text = "질문\n\n$trimmedQuestion"
        resultAnswerText.text = "답변을 생성하고 있습니다..."
        showState(DeskHubScreenState.Result)

        aiExecutor.execute {
            val result = geminiClient.generateAnswer(trimmedQuestion)
            runOnUiThread {
                resultAnswerText.text = result.getOrElse { error ->
                    error.message ?: "답변을 가져오지 못했습니다."
                }
                handler.postDelayed(idleReturnRunnable, AUTO_IDLE_DELAY_MS)
            }
        }
    }

    private fun showQuestionError(message: String) {
        resultTitleText.text = "음성 질문"
        routeStepText.text = "질문을 처리하지 못했습니다."
        resultAnswerText.text = message
        showState(DeskHubScreenState.Result)
        handler.postDelayed(idleReturnRunnable, AUTO_IDLE_DELAY_MS)
    }

    private fun showAdminPasswordDialog() {
        val passwordInput = EditText(this).apply {
            hint = "관리자 비밀번호"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
        }

        AlertDialog.Builder(this)
            .setTitle("관리자 모드")
            .setView(passwordInput)
            .setPositiveButton("확인") { _, _ ->
                if (passwordInput.text.toString() == getString(R.string.admin_password)) {
                    showState(DeskHubScreenState.Admin)
                } else {
                    Toast.makeText(this, "비밀번호가 올바르지 않습니다.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun renderNotices() {
        noticePrimaryText.text = getString(R.string.notice_1)
        noticeSecondaryText.text = getString(R.string.notice_2)
    }

    private fun updateClock() {
        val now = LocalDateTime.now()
        timeText.text = now.format(timeFormatter)
        dateText.text = now.format(dateFormatter)
    }

    private fun hideSystemBars() {
        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    companion object {
        private const val CLOCK_UPDATE_INTERVAL_MS = 30_000L
        private const val AUTO_IDLE_DELAY_MS = 15_000L
    }
}
