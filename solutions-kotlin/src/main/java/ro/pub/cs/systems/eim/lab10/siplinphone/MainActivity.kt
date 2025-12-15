
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
    private lateinit var core: Core

    private val coreListener = object: CoreListenerStub() {
        override fun onAccountRegistrationStateChanged(core: Core, account: Account, state: RegistrationState?, message: String) {
            findViewById<TextView>(R.id.registration_status).text = message
            when (state) {
                RegistrationState.Failed -> {
                    findViewById<Button>(R.id.register).isEnabled = true
                }

                RegistrationState.Cleared -> {
                    findViewById<LinearLayout>(R.id.register_layout).visibility = View.VISIBLE
                    findViewById<RelativeLayout>(R.id.call_layout).visibility = View.GONE
                    findViewById<Button>(R.id.register).isEnabled = true
                }

                RegistrationState.Ok -> {
                    findViewById<LinearLayout>(R.id.register_layout).visibility = View.GONE
                    findViewById<RelativeLayout>(R.id.call_layout).visibility = View.VISIBLE
                    findViewById<Button>(R.id.unregister).isEnabled = true
                    findViewById<EditText>(R.id.remote_address).isEnabled = true
                }
                else -> {}
            }
        }

        override fun onCallStateChanged(core: Core, call: Call, state: Call.State?, message: String) {
            findViewById<TextView>(R.id.call_status).text = message

            when (state) {
                // I N C O M I N G
                Call.State.IncomingReceived -> {
                    findViewById<Button>(R.id.hang_up).isEnabled = true
                    findViewById<Button>(R.id.answer).isEnabled = true

                    val remoteAddress = call.remoteAddressAsString
                    if (remoteAddress != null)
                        findViewById<EditText>(R.id.remote_address).setText(
                            call.remoteAddressAsString ?: "unknown"
                        )
                }
                Call.State.Connected -> {
                    findViewById<Button>(R.id.mute_mic).isEnabled = true
                    findViewById<Button>(R.id.toggle_speaker).isEnabled = true
                    Toast.makeText(this@MainActivity, "remote party answered",  Toast.LENGTH_LONG).show()
                }
                Call.State.Released -> {
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
        val factory = Factory.instance()
        core = factory.createCore(null, null, this)
        core.setIpv6Enabled(false)

        findViewById<Button>(R.id.register).setOnClickListener {
            it.isEnabled = !login()
        }

        findViewById<Button>(R.id.hang_up).isEnabled = false
        findViewById<Button>(R.id.answer).isEnabled = false
        findViewById<Button>(R.id.mute_mic).isEnabled = false
        findViewById<Button>(R.id.toggle_speaker).isEnabled = false
        findViewById<EditText>(R.id.remote_address).isEnabled = true


        findViewById<Button>(R.id.answer).setOnClickListener {
            core.currentCall?.accept()
        }

        findViewById<Button>(R.id.mute_mic).setOnClickListener {
             core.setMicEnabled(!core.isMicEnabled())
        }

        findViewById<Button>(R.id.toggle_speaker).setOnClickListener {
            toggleSpeaker()
        }


        findViewById<Button>(R.id.call).setOnClickListener {
            outgoingCall()
            findViewById<EditText>(R.id.remote_address).isEnabled = false
            it.isEnabled = false
            findViewById<Button>(R.id.hang_up).isEnabled = true
        }

        findViewById<Button>(R.id.hang_up).setOnClickListener {

            findViewById<EditText>(R.id.remote_address).isEnabled = true
            findViewById<Button>(R.id.call).isEnabled = true

            if (core.callsNb != 0) {
                val call = if (core.currentCall != null) core.currentCall else core.calls[0]
                if(call != null)
                    call.terminate()
            }
        }

        findViewById<Button>(R.id.unregister).setOnClickListener {
            val account = core.defaultAccount
            if(account != null) {
                val params = account.params
                val clonedParams = params.clone()
                clonedParams.setRegisterEnabled(false)
                account.params = clonedParams

                it.isEnabled = false
            }
        }

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
              if(call != null)
                  call.sendDtmf(keypress[0])
          }

    }

    private fun login():Boolean {
        val username = findViewById<EditText>(R.id.username).text.toString()
        val password = findViewById<EditText>(R.id.password).text.toString()
        val domain = findViewById<EditText>(R.id.domain).text.toString()
        val transportType = when (findViewById<RadioGroup>(R.id.transport).checkedRadioButtonId) {
            R.id.udp -> TransportType.Udp
            R.id.tcp -> TransportType.Tcp
            else -> TransportType.Tls
        }
        Log.i("REGISTER", "success0")
        val authInfo = Factory.instance().createAuthInfo(username, null, password, null, null, domain, null)
        Log.i("REGISTER", "success1")
        val params = core.createAccountParams()
        val identity = Factory.instance().createAddress("sip:$username@$domain")
        if(identity == null){
            Toast.makeText(this@MainActivity, "Identity not valid",  Toast.LENGTH_LONG).show()
            return false
        }
        params.identityAddress = identity
        Log.i("REGISTER", "success2")
        val address = Factory.instance().createAddress("sip:$domain")
        address?.transport = transportType
        params.serverAddress = address
        params.setRegisterEnabled(true)
        val account = core.createAccount(params)

        core.addAuthInfo(authInfo)
        core.addAccount(account)

        core.defaultAccount = account
        core.addListener(coreListener)

        core.start()

        // We will need the RECORD_AUDIO permission for video call
        if (packageManager.checkPermission(Manifest.permission.RECORD_AUDIO, packageName) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 0)
            return false
        }
        return true
    }

    private fun toggleSpeaker() {
        // Get the currently used audio device
        val currentAudioDevice = core.currentCall?.outputAudioDevice
        val speakerEnabled = currentAudioDevice?.type == AudioDevice.Type.Speaker

        // We can get a list of all available audio devices using
        // Note that on tablets for example, there may be no Earpiece device
        for (audioDevice in core.audioDevices) {
            if (speakerEnabled && audioDevice.type == AudioDevice.Type.Earpiece) {
                core.currentCall?.outputAudioDevice = audioDevice
                return
            } else if (!speakerEnabled && audioDevice.type == AudioDevice.Type.Speaker) {
                core.currentCall?.outputAudioDevice = audioDevice
                return
            }/* If we wanted to route the audio to a bluetooth headset
            else if (audioDevice.type == AudioDevice.Type.Bluetooth) {
                core.currentCall?.outputAudioDevice = audioDevice
            }*/
        }
    }


    private fun outgoingCall() {
        // As for everything we need to get the SIP URI of the remote and convert it to an Address
        val remoteSipUri = findViewById<EditText>(R.id.remote_address).text.toString()
        val remoteAddress = Factory.instance().createAddress("sip:$remoteSipUri")
        remoteAddress ?: return // If address parsing fails, we can't continue with outgoing call process

        // We also need a CallParams object
        // Create call params expects a Call object for incoming calls, but for outgoing we must use null safely
        val params = core.createCallParams(null)
        params ?: return // Same for params

        params.mediaEncryption = MediaEncryption.None
        // If we wanted to start the call with video directly
        //params.enableVideo(true)

        // Finally we start the call
        core.inviteAddressWithParams(remoteAddress, params)
        // Call process can be followed in onCallStateChanged callback from core listener
    }

}