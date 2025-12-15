package ro.pub.cs.systems.eim.lab10.siplinphone;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import org.linphone.core.*;

public class MainActivity extends AppCompatActivity {
    private Core core;

    private final CoreListener coreListener = new CoreListenerStub() {
        @Override
        public void onAccountRegistrationStateChanged(Core core, Account account, RegistrationState state, String message) {
            ((TextView) findViewById(R.id.registration_status)).setText(message);

            if (state != null) {
                switch (state) {
                    case Failed:
                        findViewById(R.id.register).setEnabled(true);
                        break;
                    case Cleared:
                        findViewById(R.id.register_layout).setVisibility(View.VISIBLE);
                        findViewById(R.id.call_layout).setVisibility(View.GONE);
                        findViewById(R.id.register).setEnabled(true);
                        break;
                    case Ok:
                        findViewById(R.id.register_layout).setVisibility(View.GONE);
                        findViewById(R.id.call_layout).setVisibility(View.VISIBLE);
                        findViewById(R.id.unregister).setEnabled(true);
                        findViewById(R.id.remote_address).setEnabled(true);
                        break;
                }
            }
        }

        @Override
        public void onCallStateChanged(Core core, Call call, Call.State state, String message) {
            ((TextView) findViewById(R.id.call_status)).setText(message);

            if (state != null) {
                switch (state) {
                    case IncomingReceived:
                        findViewById(R.id.hang_up).setEnabled(true);
                        findViewById(R.id.answer).setEnabled(true);

                        String remoteAddress = call.getRemoteAddressAsString();
                        if (remoteAddress != null) {
                            ((EditText) findViewById(R.id.remote_address)).setText(remoteAddress);
                        }
                        break;
                    case Connected:
                        findViewById(R.id.mute_mic).setEnabled(true);
                        findViewById(R.id.toggle_speaker).setEnabled(true);
                        Toast.makeText(MainActivity.this, "Remote party answered", Toast.LENGTH_LONG).show();
                        break;
                    case Released:
                        findViewById(R.id.hang_up).setEnabled(false);
                        findViewById(R.id.answer).setEnabled(false);
                        findViewById(R.id.mute_mic).setEnabled(false);
                        findViewById(R.id.toggle_speaker).setEnabled(false);
                        ((EditText) findViewById(R.id.remote_address)).getText().clear();
                        findViewById(R.id.call).setEnabled(true);
                        break;
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        Factory factory = Factory.instance();
        core = factory.createCore(null, null, this);
        core.setIpv6Enabled(false);

        findViewById(R.id.register).setOnClickListener(v -> v.setEnabled(!login()));

        findViewById(R.id.hang_up).setEnabled(false);
        findViewById(R.id.answer).setEnabled(false);
        findViewById(R.id.mute_mic).setEnabled(false);
        findViewById(R.id.toggle_speaker).setEnabled(false);
        findViewById(R.id.remote_address).setEnabled(true);

        findViewById(R.id.answer).setOnClickListener(v -> {
            Call currentCall = core.getCurrentCall();
            if (currentCall != null) {
                currentCall.accept();
            }
        });

        findViewById(R.id.mute_mic).setOnClickListener(v -> {
            core.setMicEnabled(!core.isAgcEnabled());
        });

        findViewById(R.id.toggle_speaker).setOnClickListener(v -> toggleSpeaker());

        findViewById(R.id.call).setOnClickListener(v -> {
            outgoingCall();
            findViewById(R.id.remote_address).setEnabled(false);
            v.setEnabled(false);
            findViewById(R.id.hang_up).setEnabled(true);
        });

        findViewById(R.id.hang_up).setOnClickListener(v -> {
            findViewById(R.id.remote_address).setEnabled(true);
            findViewById(R.id.call).setEnabled(true);

            if (core.getCallsNb() != 0) {
                Call call = core.getCurrentCall() != null ? core.getCurrentCall() : core.getCalls()[0];
                if (call != null) {
                    call.terminate();
                }
            }
        });

        findViewById(R.id.unregister).setOnClickListener(v -> {
            Account account = core.getDefaultAccount();
            if (account != null) {
                AccountParams params = account.getParams();
                AccountParams clonedParams = params.clone();
                clonedParams.setRegisterEnabled(false);
                account.setParams(clonedParams);

                v.setEnabled(false);
            }
        });

        findViewById(R.id.dtmfsend).setOnClickListener(v -> {
            String keypress = ((EditText) findViewById(R.id.dtmfedit)).getText().toString();
            if (keypress.isEmpty()) {
                Toast.makeText(MainActivity.this, "Need phone key character 0-9, +, #", Toast.LENGTH_LONG).show();
                return;
            }

            Call call = core.getCurrentCall() != null
                    ? core.getCurrentCall()
                    : (core.getCalls().length > 0 ? core.getCalls()[0] : null);

            if (call != null) {
                call.sendDtmf(keypress.charAt(0));
            }
        });
    }

    private boolean login() {
        String username = ((EditText) findViewById(R.id.username)).getText().toString();
        String password = ((EditText) findViewById(R.id.password)).getText().toString();
        String domain = ((EditText) findViewById(R.id.domain)).getText().toString();

        TransportType transportType;
        int checkedId = ((RadioGroup) findViewById(R.id.transport)).getCheckedRadioButtonId();
        if (checkedId == R.id.udp) {
            transportType = TransportType.Udp;
        } else if (checkedId == R.id.tcp) {
            transportType = TransportType.Tcp;
        } else {
            transportType = TransportType.Tls;
        }

        Log.i("REGISTER", "success0");
        AuthInfo authInfo = Factory.instance().createAuthInfo(username, null, password, null, null, domain, null);
        Log.i("REGISTER", "success1");

        AccountParams params = core.createAccountParams();
        Address identity = Factory.instance().createAddress("sip:" + username + "@" + domain);
        if (identity == null) {
            Toast.makeText(this, "Identity not valid", Toast.LENGTH_LONG).show();
            return false;
        }
        params.setIdentityAddress(identity);

        Log.i("REGISTER", "success2");
        Address address = Factory.instance().createAddress("sip:" + domain);
        if (address != null) {
            address.setTransport(transportType);
        }
        params.setServerAddress(address);
        params.setRegisterEnabled(true);

        Account account = core.createAccount(params);
        core.addAuthInfo(authInfo);
        core.addAccount(account);

        core.setDefaultAccount(account);
        core.addListener(coreListener);

        core.start();

        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 0);
            return false;
        }
        return true;
    }

    private void toggleSpeaker() {
        AudioDevice currentAudioDevice = core.getCurrentCall() != null
                ? core.getCurrentCall().getOutputAudioDevice()
                : null;
        boolean speakerEnabled = currentAudioDevice != null && currentAudioDevice.getType() == AudioDevice.Type.Speaker;

        for (AudioDevice audioDevice : core.getAudioDevices()) {
            if (speakerEnabled && audioDevice.getType() == AudioDevice.Type.Earpiece) {
                if (core.getCurrentCall() != null) {
                    core.getCurrentCall().setOutputAudioDevice(audioDevice);
                }
                return;
            } else if (!speakerEnabled && audioDevice.getType() == AudioDevice.Type.Speaker) {
                if (core.getCurrentCall() != null) {
                    core.getCurrentCall().setOutputAudioDevice(audioDevice);
                }
                return;
            }
        }
    }

    private void outgoingCall() {
        String remoteSipUri = ((EditText) findViewById(R.id.remote_address)).getText().toString();
        Address remoteAddress = Factory.instance().createAddress("sip:" + remoteSipUri);
        if (remoteAddress == null) {
            return;
        }

        CallParams params = core.createCallParams(null);
        if (params == null) {
            return;
        }

        params.setMediaEncryption(MediaEncryption.None);
        core.inviteAddressWithParams(remoteAddress, params);
    }
}
