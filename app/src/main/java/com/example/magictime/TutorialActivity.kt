package com.example.magictime

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.magictime.databinding.ActivityTutorialBinding

class TutorialActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTutorialBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTutorialBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        binding.cardSecretManual.setOnClickListener {
            showSecretManualDialog()
        }

        binding.cardVideoRoutine.setOnClickListener {
            openWebLink("https://www.youtube.com/watch?v=dQw4w9WgXcQ")
        }

        binding.cardVideoFloat.setOnClickListener {
            openWebLink("https://www.youtube.com/watch?v=dQw4w9WgXcQ")
        }
    }

    private fun openWebLink(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(intent)
    }

    private fun showSecretManualDialog() {
        val manualHtml = """
            <font color="#5800D1"><b><u>[1] ENGINE & ADVANCED SETTINGS</u></b></font><br>
            <b><u>A. TIME TRAVEL (CLOCK ENGINE)</u></b><br>
            &#8226; <i>Enable Time Travel:</i> Master switch for the clock manipulation illusion.<br>
            &#8226; <i>Trigger Method:</i> Choose Volume Down or Double Tap. In <b>PRESET</b> mode, this defaults to <b>Double Tap</b>.<br>
            &#8226; <i>Delay Start:</i> Time gap (ms) before the numbers start rolling after a trigger.<br>
            &#8226; <i>Time Flow Speed:</i> Controls how fast the digits animate during the jump.<br><br>
            
            <b><u>B. PREDICTION & STACK ENGINE</u></b><br>
            &#8226; <i>Card Stack System:</i> Select your memorized deck (Bart Harding, Mnemonica, or Aronson). This governs all Secret PIN text reveals.<br>
            &#8226; <i>Secret Message (Reveal):</i> Injects text into the UI. Trigger is always the <b>opposite</b> of the Time Travel trigger.<br>
            &#8226; <i>Reveal Delay & Duration:</i> Set the wait time before the text changes and how long it stays visible.<br><br>
            
            <b><u>C. AR FLOAT (PHYSICS ENGINE)</u></b><br>
            &#8226; <i>Shake Trigger:</i> Replaces Volume Up with a physical shake (includes silent haptic feedback).<br>
            &#8226; <i>Object Scale:</i> Adjust the size to match your physical cards for "out-of-phone" materializations.<br>
            &#8226; <i>Speed & Friction:</i> Controls how the object reacts to gravity and touch inertia.<br><br>
        
            <font color="#CCCCCC">━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</font><br><br>
        
            <font color="#5800D1"><b><u>[2] GENERAL & PERSONALIZATION</u></b></font><br>
            &#8226; <b>Wallpaper:</b> Use high-quality screenshots of your actual lockscreen for a 1:1 illusion.<br>
            &#8226; <b>Status Bar Realism:</b> Enabling Wi-Fi automatically hides cellular icons. Match this to your phone's real state before performing.<br>
            &#8226; <b>Custom UI Text:</b> 'Operator' changes the top-left carrier. 'Marquee' changes the bottom scrolling text.<br>
            &#8226; <b>Language & Format:</b> Supports English/Indonesian date formats and 24h/12h clock displays.<br><br>
        
            <font color="#CCCCCC">━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</font><br><br>
        
            <font color="#5800D1"><b><u>[3] DASHBOARD NAVIGATION</u></b></font><br>
            &#8226; <b>Preset Routine (Green):</b> Instantly activates a balanced setup (Double Tap trigger) without manual configuration.<br>
            &#8226; <b>Personalize:</b> Focuses purely on visual aesthetics (Wallpaper, Text, PIN) without touching the engine logic.<br>
            &#8226; <b>Advanced Settings:</b> The "Under the Hood" menu for fine-tuning delays, multipliers, and physics.<br>
            &#8226; <b>Load Setting:</b> Access 5 dedicated memory slots to switch between different stage/street setups instantly.<br>
            &#8226; <b>Tutorial & Grimoire:</b> You are reading it now. Handle this information with absolute secrecy.<br><br>
        
            <font color="#CCCCCC">━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</font><br><br>
        
            <font color="#5800D1"><b><u>[4] HARDWARE CONTROLS</u></b></font><br>
            &#8226; <b>Hold 'OK' Button:</b> Silently activates <b>Secret Mode</b> for hidden PIN commands.<br>
            &#8226; <b>Physical Lock Button:</b> Instant kill-switch. Cuts off reveal text or exits PIN view to the fake lockscreen.<br>
            &#8226; <b>Navigation:</b> Swipe up/sideways to unlock. Swipe down is disabled to prevent exposing the real status bar.<br><br>
        
            <font color="#CCCCCC">━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</font><br><br>
        
            <font color="#5800D1"><b><u>[5] SECRET PIN INPUTS (HIDDEN CORE)</u></b></font><br>
            <i>Activate Secret Mode (Hold OK) before entering these formulas:</i><br><br>
            &#8226; <b>Time Offset Injection:</b> 000 &#8594; OK &#8594; [Minutes] &#8594; OK<br>
            &#8226; <b>Force Float Card:</b> [Value] &#8594; OK &#8594; [Suit] &#8594; OK<br>
            &#8226; <b>Stack Reveal & Back Card:</b> 0 &#8594; OK &#8594; 0 &#8594; OK<br>
            &#8226; <b>Card Position:</b> 0 &#8594; OK &#8594; [Position] &#8594; OK<br>
            &#8226; <b>Find Card:</b> 0[Value] &#8594; OK &#8594; [Suit] &#8594; OK<br><br>
        
            <font color="#CCCCCC">━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</font><br><br>
        
            <font color="#5800D1"><b><u>[6] PRO PERFORMANCE TIPS</u></b></font><br>
            &#8226; <b>Biometrics:</b> Use Fingerprint/FaceID to bypass the real lockscreen directly into this app.<br>
            &#8226; <b>DND Mode:</b> Always activate 'Do Not Disturb' to prevent real notifications from breaking the Art.<br>
            &#8226; <b>Misdirection:</b> Treat the phone casually. Practice PIN inputs blindly to maintain eye contact.<br>
            &#8226; <b>Vanishing:</b> Overlapping the AR object with the screen edge triggers an instant vanish animation.<br><br>
            
            <br><br>
            <center>
            <font size="2" color="#5800D1"><b>APP & ROUTINE ENGINEERED BY ARDAN DESTA VAUNENDRA</b></font><br>
            <font size="2" color="#777777"><i>Developed for professional magic performances. Handle with absolute secrecy.</i></font>
            </center>
        """.trimIndent()

        val textView = android.widget.TextView(this).apply {
            text = androidx.core.text.HtmlCompat.fromHtml(manualHtml, androidx.core.text.HtmlCompat.FROM_HTML_MODE_LEGACY)
            setPadding(60, 40, 60, 20)
            textSize = 14f
            setTextColor(android.graphics.Color.parseColor("#1A1A1A"))
            setLineSpacing(0f, 1.2f)
        }

        val scrollView = android.widget.ScrollView(this).apply { addView(textView) }

        android.app.AlertDialog.Builder(this)
            .setTitle(androidx.core.text.HtmlCompat.fromHtml("<font color='#1D1D1F'><b>Secret Manual</b></font>", androidx.core.text.HtmlCompat.FROM_HTML_MODE_LEGACY))
            .setView(scrollView)
            .setPositiveButton("Close", null)
            .show()
    }
}