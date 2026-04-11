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
        <center>
        <font color="#5800D1"><b><u>ARCANE FLOW: THE GRIMOIRE</u></b></font><br>
        <font size="2" color="#777777"><i>Ultimate reference guide for all routines and mechanics</i></font>
        </center><br><br>

        <font color="#5800D1"><b><u>[1] DASHBOARD NAVIGATION</u></b></font><br>
        &#8226; <b>Preset Routine (Green):</b> Instantly activates a balanced setup (Double Tap trigger) without manual configuration.<br>
        &#8226; <b>Personalize:</b> Focuses purely on visual aesthetics (Wallpaper, Text, PIN) without touching the engine logic.<br>
        &#8226; <b>Advanced Settings:</b> The "Under the Hood" menu for fine-tuning delays, multipliers, and physics.<br>
        &#8226; <b>Load Setting:</b> Access 5 dedicated memory slots to switch between different stage/street setups instantly.<br>
        &#8226; <b>Tutorial & Grimoire:</b> You are reading it now. Handle this information with absolute secrecy.<br><br>

        <font color="#CCCCCC">━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</font><br><br>

        <font color="#5800D1"><b><u>[2] ENGINE & ADVANCED SETTINGS</u></b></font><br>
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
    
        <font color="#5800D1"><b><u>[3] GENERAL & PERSONALIZATION</u></b></font><br>
        &#8226; <b>Wallpaper:</b> Use high-quality screenshots of your actual lockscreen for a 1:1 illusion.<br>
        &#8226; <b>Status Bar Realism:</b> Enabling Wi-Fi automatically hides cellular icons. Match this to your phone's real state before performing.<br>
        &#8226; <b>Custom UI Text:</b> 'Operator' changes the top-left carrier. 'Marquee' changes the bottom scrolling text.<br>
        &#8226; <b>Language & Format:</b> Supports English/Indonesian date formats and 24h/12h clock displays.<br><br>
    
        <font color="#CCCCCC">━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</font><br><br>
    
        <font color="#5800D1"><b><u>[4] HARDWARE CONTROLS</u></b></font><br>
        &#8226; <b>Hold 'OK' Button:</b> Silently activates <b>Secret Mode</b> for hidden PIN commands.<br>
        &#8226; <b>Physical Lock Button:</b> Instant kill-switch. Cuts off reveal text or exits PIN view to the fake lockscreen.<br>
        &#8226; <b>Navigation:</b> Swipe up/sideways to unlock. Swipe down is disabled to prevent exposing the real status bar.<br><br>
    
        <font color="#CCCCCC">━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</font><br><br>

        <font color="#5800D1"><b><u>[5] SECRET PIN INPUTS (THE HIDDEN CORE)</u></b></font><br>
        These commands are NOT in the dashboard settings. To execute them, you MUST <b>HOLD the 'OK' button</b> to activate Secret Mode. The system will silently listen for your input without giving visual feedback.<br><br>

        <b><u>A. TIME JUMP INJECTION (DYNAMIC OFFSET)</u></b><br>
        <i>Function: Silently injects a custom time offset (in minutes) on the fly before triggering the Time Travel illusion.</i><br>
        &#8226; <b>Formula:</b> Hold OK &#8594; 000 &#8594; <b>OK</b> &#8594; [Minutes] &#8594; <b>OK</b><br>
        &#8226; <i>Future (+8 mins):</i> Enter 8<br>
        &#8226; <i>Past (-8 mins):</i> Enter 08<br><br>

        <b><u>B. SET AR FLOAT CARD (FORCED PREDICTION)</u></b><br>
        <i>Function: Silently forces the AR Floating Object to display a specific playing card, overriding the gallery/dashboard settings.</i><br>
        &#8226; <b>Formula:</b> Hold OK &#8594; [Card Value] &#8594; <b>OK</b> &#8594; [Card Suit] &#8594; <b>OK</b><br>
        &#8226; <i>Values:</i> Ace=1, Jack=11, Queen=12, King=13 (Numbers 2-10 are normal)<br>
        &#8226; <i>Suits:</i> 1=Diamonds, 2=Clubs, 3=Hearts, 4=Spades<br>
        &#8226; <i>Example (Queen of Hearts):</i> Hold OK &#8594; 12 &#8594; OK &#8594; 3 &#8594; OK<br><br>

        <b><u>C. CARD STACK SYSTEM (MARQUEE TEXT REVEAL & BACK CARD FORCING)</u></b><br>
        <i>Function: Injects specific stack information into the running text or carrier as a prediction. The stack used will depend on your selection in the dashboard (Bart Harding, Mnemonica, or Aronson).</i><br>
        &#8226; <b>Show Full Stack & Force Back Card:</b> Hold OK &#8594; 0 &#8594; OK &#8594; 0 &#8594; OK<br>
        <i>(Note: This command has a dual effect. Not only does it reveal the stack in the text, but it also temporarily forces the AR Float to show a facedown 'Back Card' without saving it to memory. The color of the back card will follow your dashboard settings.)</i><br>
        &#8226; <b>Show Card at Position (1-52):</b> Hold OK &#8594; 0 &#8594; OK &#8594; [Position] &#8594; OK<br>
        &#8226; <b>Find Position of a Card:</b> Hold OK &#8594; 0[Value] &#8594; OK &#8594; [Suit] &#8594; OK <i>(e.g., 08 &#8594; OK &#8594; 3 &#8594; OK)</i><br>
        &#8226; <b>Find Cards Before & After:</b> Hold OK &#8594; 00[Value] &#8594; OK &#8594; [Suit] &#8594; OK <i>(e.g., 008 &#8594; OK &#8594; 3 &#8594; OK)</i><br><br>

        <font color="#777777"><i>*Bart Harding Stack Reference:*</i></font><br>
        <font size="2">10c, 7h, 4s, Ad, Jd, 6c, 7c, 9s, 6d, Ac, Jc, 8h, 5s, 2d, Qd, 3h, Kh, 10s, 7d, 2c, Qc, 9h, 6s, 3d, Kd, 4h, As, Js, 8d, 3c, Kc, 10h, 7s, 4d, 8c, 5h, 2s, Qs, 9d, 4c, Ah, Jh, 8s, 5d, 9c, 6h, 3s, Ks, 10d, 5c, 2h, Qh</font><br><br>

        <font color="#777777"><i>*Mnemonica Stack Reference:*</i></font><br>
        <font size="2">4c, 2h, 7d, 3c, 4h, 6d, As, 5h, 9s, 2s, Qh, 3d, Qc, 8h, 6s, 5s, 9h, Kc, 2d, Jh, 3s, 8s, 6h, 10c, 5d, Kd, 2c, 3h, 8d, 5c, Ks, Jd, 8c, 10s, Kh, Jc, 7s, 10h, Ad, 4s, 7h, 4d, Ac, 9c, Js, Qd, 7c, Qs, 10d, 6c, Ah, 9d</font><br><br>

        <font color="#777777"><i>*Aronson Stack Reference:*</i></font><br>
        <font size="2">Js, Kc, 5c, 2h, 9s, As, 3h, 6c, 8d, Ac, 10s, 5h, 2d, Kd, 7d, 8c, 3s, Ad, 7s, 5s, Qd, Ah, 8s, 3d, 7h, Qh, 5d, 7c, 4h, Kh, 4d, 10d, Jc, Jh, 10c, Jd, 4s, 10h, 6h, 3c, 2s, 9h, Ks, 6s, 4c, 8h, 9c, Qs, 6d, Qc, 2c, 9d</font><br><br>

        <font color="#CCCCCC">━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</font><br><br>

        <font color="#5800D1"><b><u>[6] PRO PERFORMANCE TIPS</u></b></font><br>
        &#8226; <b>Biometrics:</b> Use Fingerprint/FaceID to bypass the real lockscreen directly into this app.<br>
        &#8226; <b>In-App Zero Notif Mode:</b> Activate 'Do Not Disturb' directly from the main menu to safely block incoming notifications from breaking the Art.<br>
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
            .setView(scrollView)
            .setPositiveButton("Close", null)
            .show()
    }
}