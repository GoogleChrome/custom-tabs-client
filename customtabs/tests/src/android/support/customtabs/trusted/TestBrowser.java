package android.support.customtabs.trusted;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

/**
 * A fake Browser that accepts browsable Intents.
 */
public class TestBrowser extends AppCompatActivity {
    private static TestBrowser sInstance;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sInstance = this;
    }

    public static boolean hasLaunched() {
        return sInstance != null;
    }

    public static Intent getLaunchIntent() {
        return sInstance.getIntent();
    }
}
