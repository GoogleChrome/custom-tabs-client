package android.support.customtabs.trusted;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;

/**
 * A fake Browser that accepts browsable Intents.
 */
public class TestBrowser extends AppCompatActivity {
    public Intent getLaunchIntent() {
        return getIntent();
    }
}
