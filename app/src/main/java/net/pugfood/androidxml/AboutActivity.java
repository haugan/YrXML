package net.pugfood.androidxml;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class AboutActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        TextView textAbout1 = (TextView) findViewById(R.id.textAbout1);
        TextView textAbout2 = (TextView) findViewById(R.id.textAbout2);
        textAbout1.setText(R.string.about_text_1);
        textAbout2.setText(R.string.about_text_2);
    }
}