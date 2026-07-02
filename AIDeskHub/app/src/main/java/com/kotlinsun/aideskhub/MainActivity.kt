package com.kotlinsun.aideskhub

import android.app.AlertDialog
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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.kotlinsun.aideskhub.data.LocalGuideRepository
import com.kotlinsun.aideskhub.model.DeskHubScreenState
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private val handler = Handler(Looper.getMainLooper())
    private val repository = LocalGuideRepository()
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.KOREA)
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy년 M월 d일 EEEE", Locale.KOREA)

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
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_main)
        val contentPadding = resources.getDimensionPixelSize(R.dimen.screen_content_padding)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(
                systemBars.left + contentPadding,
                systemBars.top + contentPadding,
                systemBars.right + contentPadding,
                systemBars.bottom + contentPadding,
            )
            insets
        }
        bindViews()
        bindActions()
        showState(DeskHubScreenState.KioskIdle)
    }

    override fun onResume() {
        super.onResume()
        handler.post(clockRunnable)
    }

    override fun onPause() {
        handler.removeCallbacks(clockRunnable)
        handler.removeCallbacks(idleReturnRunnable)
        super.onPause()
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
    }

    private fun bindActions() {
        findViewById<View>(R.id.listenButton).setOnClickListener {
            showState(DeskHubScreenState.Listening)
        }
        findViewById<View>(R.id.sampleResultButton).setOnClickListener {
            showSampleResult()
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
            showSampleResult()
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

    private fun showSampleResult() {
        val guide = repository.getSampleRouteGuide()
        resultAnswerText.text = guide.answer
        routeStepText.text = guide.routeSteps.firstOrNull() ?: "등록된 길 안내 이미지가 없습니다."
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
        val notices = repository.getSampleNotices()
        noticePrimaryText.text = notices.firstOrNull().orEmpty()
        noticeSecondaryText.text = notices.drop(1).firstOrNull().orEmpty()
    }

    private fun updateClock() {
        val now = LocalDateTime.now()
        timeText.text = now.format(timeFormatter)
        dateText.text = now.format(dateFormatter)
    }

    companion object {
        private const val CLOCK_UPDATE_INTERVAL_MS = 30_000L
        private const val AUTO_IDLE_DELAY_MS = 15_000L
    }
}
