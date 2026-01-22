package ro.pub.cs.systems.eim.lab10.siplinphone

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import org.linphone.core.*

class MainActivity:  AppCompatActivity() {
    // -->> Obiectul principal Linphone care face toată magia (rețea, audio, sip)
    private lateinit var core: Core

    // -->> Aici definim "Ascultătorul". Acesta va reacționa automat când se întâmplă ceva în rețea.
    private val coreListener = object: CoreListenerStub() {

//        // -->> 1. Callback pentru starea ÎNREGISTRĂRII (Login)
//        override fun onAccountRegistrationStateChanged(core: Core, account: Account, state: RegistrationState?, message: String) {
//            // Afișăm mesajul de la server (ex: "Registration successful" sau "Unauthorized")
//            findViewById<TextView>(R.id.registration_status).text = message
//
//            when (state) {
//                // Dacă a eșuat login-ul, reactivăm butonul de Register să mai încerci o dată
//                RegistrationState.Failed -> {
//                    findViewById<Button>(R.id.register).isEnabled = true
//                }
//
//                // Dacă s-a făcut Logout (Cleared), arătăm din nou formularul de Login
//                RegistrationState.Cleared -> {
//                    findViewById<LinearLayout>(R.id.register_layout).visibility = View.VISIBLE
//                    findViewById<RelativeLayout>(R.id.call_layout).visibility = View.GONE
//                    findViewById<Button>(R.id.register).isEnabled = true
//                }
//
//                // Dacă Login-ul e OK: Ascundem login-ul, arătăm ecranul de apelare
//                RegistrationState.Ok -> {
//                    findViewById<LinearLayout>(R.id.register_layout).visibility = View.GONE
//                    findViewById<RelativeLayout>(R.id.call_layout).visibility = View.VISIBLE
//                    findViewById<Button>(R.id.unregister).isEnabled = true
//                    findViewById<EditText>(R.id.remote_address).isEnabled = true
//                }
//                else -> {}
//            }
//        }
    override fun onAccountRegistrationStateChanged(core: Core, account: Account, state: RegistrationState?, message: String) {
        findViewById<TextView>(R.id.registration_status).text = message

        when (state) {
            // --->> AICI E MODIFICAREA: Tratăm eșecul ca pe un succes
            RegistrationState.Failed -> {
                Toast.makeText(this@MainActivity, "Login Eșuat - Bypass activat", Toast.LENGTH_SHORT).show()

                // Ascundem ecranul de login
                findViewById<LinearLayout>(R.id.register_layout).visibility = View.GONE
                // Arătăm ecranul de apel
                findViewById<RelativeLayout>(R.id.call_layout).visibility = View.VISIBLE

                // Activăm butoanele
                findViewById<Button>(R.id.unregister).isEnabled = true
                findViewById<EditText>(R.id.remote_address).isEnabled = true
            }
            // Dacă s-a făcut Logout (Cleared), arătăm din nou formularul de Login
            RegistrationState.Cleared -> {
                findViewById<LinearLayout>(R.id.register_layout).visibility = View.VISIBLE
                findViewById<RelativeLayout>(R.id.call_layout).visibility = View.GONE
                findViewById<Button>(R.id.register).isEnabled = true
            }
            // Dacă Login-ul e OK: Ascundem login-ul, arătăm ecranul de apelare
            RegistrationState.Ok -> {
                findViewById<LinearLayout>(R.id.register_layout).visibility = View.GONE
                findViewById<RelativeLayout>(R.id.call_layout).visibility = View.VISIBLE
                findViewById<Button>(R.id.unregister).isEnabled = true
                findViewById<EditText>(R.id.remote_address).isEnabled = true
            }
            else -> {}
        }
    }

        // -->> 2. Callback pentru starea APELULUI (Call)
        override fun onCallStateChanged(core: Core, call: Call, state: Call.State?, message: String) {
            findViewById<TextView>(R.id.call_status).text = message

            when (state) {
                // Cineva ne sună! (Incoming)
                Call.State.IncomingReceived -> {
                    // Activăm butoanele de Răspuns și Respins
                    findViewById<Button>(R.id.hang_up).isEnabled = true
                    findViewById<Button>(R.id.answer).isEnabled = true

                    // Afișăm cine ne sună în câmpul de text
                    val remoteAddress = call.remoteAddressAsString
                    if (remoteAddress != null)
                        findViewById<EditText>(R.id.remote_address).setText(
                            call.remoteAddressAsString ?: "unknown"
                        )
                }
                // Apelul a fost preluat (vorbim)
                Call.State.Connected -> {
                    findViewById<Button>(R.id.mute_mic).isEnabled = true
                    findViewById<Button>(R.id.toggle_speaker).isEnabled = true
                    Toast.makeText(this@MainActivity, "remote party answered",  Toast.LENGTH_LONG).show()
                }
                // Apelul s-a încheiat (sau a fost respins)
                Call.State.Released -> {
                    // Resetăm butoanele la starea inițială
                    findViewById<Button>(R.id.hang_up).isEnabled = false
                    findViewById<Button>(R.id.answer).isEnabled = false
                    findViewById<Button>(R.id.mute_mic).isEnabled = false
                    findViewById<Button>(R.id.toggle_speaker).isEnabled = false

                    findViewById<EditText>(R.id.remote_address).text.clear()
                    findViewById<Button>(R.id.call).isEnabled = true
                }

                else -> {}
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)

        // -->> Inițializăm motorul Linphone
        val factory = Factory.instance()
        core = factory.createCore(null, null, this)
        // Dezactivăm IPv6 pentru a evita probleme pe unele rețele WiFi
        core.enableIpv6(false)

        // -->> Butonul REGISTER: Apelează funcția login()
        findViewById<Button>(R.id.register).setOnClickListener {
            // Dacă login() returnează false (ex: lipsă permisiuni), butonul rămâne activ
            it.isEnabled = !login()
        }

        // -->> Setări inițiale: dezactivăm butoanele de apel până nu suntem logați/în apel
        findViewById<Button>(R.id.hang_up).isEnabled = false
        findViewById<Button>(R.id.answer).isEnabled = false
        findViewById<Button>(R.id.mute_mic).isEnabled = false
        findViewById<Button>(R.id.toggle_speaker).isEnabled = false
        findViewById<EditText>(R.id.remote_address).isEnabled = true


        // -->> Butonul ANSWER (Răspunde): Acceptă apelul curent
        findViewById<Button>(R.id.answer).setOnClickListener {
            core.currentCall?.accept()
        }

        // -->> Butonul MUTE: Inversează starea microfonului (On/Off)
        findViewById<Button>(R.id.mute_mic).setOnClickListener {
            core.enableMic(!core.micEnabled())
        }

        // -->> Butonul SPEAKER: Comută între difuzor și cască
        findViewById<Button>(R.id.toggle_speaker).setOnClickListener {
            toggleSpeaker()
        }

        // -->> Butonul CALL (Sună): Inițiază apelul
        findViewById<Button>(R.id.call).setOnClickListener {
            outgoingCall()
            // Blocăm editarea adresei în timp ce sunăm
            findViewById<EditText>(R.id.remote_address).isEnabled = false
            it.isEnabled = false
            findViewById<Button>(R.id.hang_up).isEnabled = true
        }

        // -->> Butonul HANG UP (Închide): Terminăm apelul
        findViewById<Button>(R.id.hang_up).setOnClickListener {

            findViewById<EditText>(R.id.remote_address).isEnabled = true
            findViewById<Button>(R.id.call).isEnabled = true

            // Verificăm dacă există apeluri active
            if (core.callsNb != 0) {
                // Luăm apelul curent sau primul din listă
                val call = if (core.currentCall != null) core.currentCall else core.calls[0]
                if(call != null)
                    call.terminate() // Închide apelul
            }
        }

        // -->> Butonul UNREGISTER (Logout)
        findViewById<Button>(R.id.unregister).setOnClickListener {
            val account = core.defaultAccount
            if(account != null) {
                // Clonăm parametrii și setăm registerEnabled = false
                val params = account.params
                val clonedParams = params.clone()
                clonedParams.setRegisterEnabled(false)
                account.params = clonedParams

                it.isEnabled = false // Dezactivăm butonul până se confirmă logout-ul
            }
        }

        // -->> Buton DTMF (Trimite taste numerice în timpul apelului, ex: apasă 1 pentru engleză)
        findViewById<Button>(R.id.dtmfsend).setOnClickListener {
            val keypress = (findViewById<EditText>(R.id.dtmfedit)).text.toString()
            if(keypress.isEmpty()){
                Toast.makeText(this@MainActivity, "Need phone key character 0-9, +, #",  Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val call = if (core.currentCall != null)
                core.currentCall
            else if( core.calls.size > 0)
                core.calls[0]
            else null

            // Trimitem doar primul caracter (ex: '1')
            if(call != null)
                call.sendDtmf(keypress[0])
        }

    }

    // -->> Funcția care configurează contul SIP și pornește conexiunea
    private fun login():Boolean {
        // Luăm datele din UI
        val username = findViewById<EditText>(R.id.username).text.toString()
        val password = findViewById<EditText>(R.id.password).text.toString()
        val domain = findViewById<EditText>(R.id.domain).text.toString()

        // Verificăm ce transport a fost selectat (UDP/TCP/TLS)
        val transportType = when (findViewById<RadioGroup>(R.id.transport).checkedRadioButtonId) {
            R.id.udp -> TransportType.Udp
            R.id.tcp -> TransportType.Tcp
            else -> TransportType.Tls
        }

        Log.i("REGISTER", "Se începe configurarea...")

        // 1. Creăm informațiile de autentificare (User + Pass)
        val authInfo = Factory.instance().createAuthInfo(username, null, password, null, null, domain, null)

        // 2. Creăm parametrii contului
        val params = core.createAccountParams()

        // 3. Creăm identitatea SIP (ex: sip:student@sip.linphone.org)
        val identity = Factory.instance().createAddress("sip:$username@$domain")
        if(identity == null){
            Toast.makeText(this@MainActivity, "Identity not valid",  Toast.LENGTH_LONG).show()
            return false
        }
        params.identityAddress = identity

        // 4. Setăm adresa serverului (Proxy)
        val address = Factory.instance().createAddress("sip:$domain")
        address?.transport = transportType
        params.serverAddress = address

        // 5. Activăm înregistrarea
        params.setRegisterEnabled(true)

        // 6. Creăm contul și îl adăugăm în Core
        val account = core.createAccount(params)
        core.addAuthInfo(authInfo)
        core.addAccount(account)
        core.defaultAccount = account // Îl setăm ca default

        // -->> IMPORTANT: Aici atașăm Listener-ul definit la începutul clasei
        core.addListener(coreListener)

        // Pornim motorul!
        core.start()

        // -->> Verificăm permisiunea de Microfon (critică pentru Android 6.0+)
        if (packageManager.checkPermission(Manifest.permission.RECORD_AUDIO, packageName) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 0)
            return false
        }
        return true
    }

    // -->> Funcția care schimbă ieșirea audio (Speaker vs Earpiece)
    private fun toggleSpeaker() {
        val currentAudioDevice = core.currentCall?.outputAudioDevice
        // Verificăm dacă suntem pe speaker acum
        val speakerEnabled = currentAudioDevice?.type == AudioDevice.Type.Speaker

        // Căutăm în lista de dispozitive audio disponibile
        for (audioDevice in core.audioDevices) {
            if (speakerEnabled && audioDevice.type == AudioDevice.Type.Earpiece) {
                // Dacă era pe speaker, mutăm pe Earpiece (receptor telefon)
                core.currentCall?.outputAudioDevice = audioDevice
                return
            } else if (!speakerEnabled && audioDevice.type == AudioDevice.Type.Speaker) {
                // Dacă nu era pe speaker, mutăm pe Speaker
                core.currentCall?.outputAudioDevice = audioDevice
                return
            }
        }
    }


    // -->> Funcția de inițiere apel
    private fun outgoingCall() {
        val remoteSipUri = findViewById<EditText>(R.id.remote_address).text.toString()

        // Transformăm textul în adresă SIP validă
        val remoteAddress = Factory.instance().createAddress("sip:$remoteSipUri")
        remoteAddress ?: return // Dacă adresa e greșită, ieșim

        // Creăm parametrii pentru apel
        val params = core.createCallParams(null)
        params ?: return

        // Setăm criptarea pe None (ca să putem vedea pachetele în Wireshark pentru laborator)
        params.mediaEncryption = MediaEncryption.None

        // -->> LANSĂM APELUL
        core.inviteAddressWithParams(remoteAddress, params)
        // De aici, starea apelului va fi gestionată în onCallStateChanged
    }

}