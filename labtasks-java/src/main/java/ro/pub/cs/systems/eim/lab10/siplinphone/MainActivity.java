package ro.pub.cs.systems.eim.lab10.siplinphone;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.linphone.core.Core;
import org.linphone.core.Factory;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        Factory factory = Factory.instance();
        Core core = factory.createCore(null, null, this);

        TextView status = findViewById(R.id.registration_status);
        status.setText("linphone lib version: " + core.getVersion());

        findViewById(R.id.register).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                findViewById(R.id.register_layout).setVisibility(View.GONE);
                findViewById(R.id.call_layout).setVisibility(View.VISIBLE);
            }
        });

        findViewById(R.id.unregister).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                findViewById(R.id.register_layout).setVisibility(View.VISIBLE);
                findViewById(R.id.call_layout).setVisibility(View.GONE);
            }
        });
    }
}

