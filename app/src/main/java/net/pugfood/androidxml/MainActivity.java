package net.pugfood.androidxml;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import net.pugfood.androidxml.model.WeatherData;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    private StringBuilder webserviceUrl;

    private String fylke   = "";
    private String kommune = "";
    private String sted    = "";

    private EditText editFylke   = null;
    private EditText editKommune = null;
    private EditText editSted    = null;

    private ImageView imgView = null;
    private int imgResourceID;

    private final String TEXT_ENCODING = "utf-8";
    private final String DEBUG_TAG = "haugis";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize associated SharedPreferences file with default values for each Preference.
        // The boolean indicates whether default values should be set more than once..
        // If "true", any previous values are overridden with defaults.
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        editFylke   = (EditText) findViewById(R.id.editFylke);
        editKommune = (EditText) findViewById(R.id.editKommune);
        editSted    = (EditText) findViewById(R.id.editSted);
        imgView     = (ImageView) findViewById(R.id.imageView);

        webserviceUrl = new StringBuilder();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }

    /**
     * React to user clicking the button, and
     * fetch data but only if a network connection is available,
     * and if user has input text to text fields.
     *
     * The device may be out of range of a network,
     * or the user may have disabled Wi-Fi and mobile data access.
     *
     * @param view
     */
    public void clickHandler(View view) {
        ConnectivityManager conMan = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = conMan.getActiveNetworkInfo();

        if (netInfo != null && netInfo.isConnected()) {
            // Create full URL from user input through EditText widgets.
            fylke   = editFylke.getText().toString();
            kommune = editKommune.getText().toString();
            sted    = editSted.getText().toString();

            webserviceUrl.append("http://www.yr.no/sted/Norge/")
                         .append(fylke).append("/")
                         .append(kommune).append("/")
                         .append(sted).append("/varsel.xml");

            // Fetch the XML data with user input variables.
            new DownloadXMLTask().execute(webserviceUrl.toString());

            // Fetch XML with static URL input.
            //new DownloadXMLTask().execute("http://www.yr.no/sted/Norge/Vestfold/Andebu/Kodal/varsel.xml");

        } else {
            // Display error,
            // user might have disabled Wi-Fi and / or mobile data.
            Context context  = getApplicationContext();
            CharSequence msg = "No connection available.";
            int duration     = Toast.LENGTH_SHORT;

            Toast toast = Toast.makeText(context, msg, duration);
            toast.show();
        }

    }

    /**
     * Called by user selecting appropiate menu item
     * defined in /res/menu/main.xml,
     * and shows a screen with settings to the user.
     *
     * @param item
     */
    public void showSettingsFragment(MenuItem item) {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    /**
     * Called by user selecting appropiate menu item
     * defined in /res/menu/main.xml,
     * and shows a screen with information about the app to the user.
     *
     * @param item
     */
    public void showAboutFragment(MenuItem item) {
        Intent intent = new Intent(this, AboutActivity.class);
        startActivity(intent);
    }

    /**
     * Perform network operations on a separate thread from the UI,
     * to prevent unpredictable network delays to cause poor user experiences.
     *
     * This task takes a URL string from the execute() method,
     * and uses it to create an HttlUrlConnection.
     *
     * When connection has been established, the AsyncTask downloads content
     * of the XML as an InputStream, which is then converted into a string
     * and displayed in the UI by AsyncTask's onPostExecute() method.
     */
    private class DownloadXMLTask extends AsyncTask<String, Void, String> {

        @Override
        /**
         * Execute the loadXML() method, passing the webservice URL as a parameter.
         */
        protected String doInBackground(String ... params) {
            try {
                return loadXML(params[0]);

            } catch (IOException | XmlPullParserException e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        /**
         * Take the returned string (from doInBackground) and display it in the UI.
         */
        protected void onPostExecute(String result) {
            WebView webView = (WebView) findViewById(R.id.webview);

            // Set text encoding to be able to display scandinavian characters.
            WebSettings settings = webView.getSettings();
            settings.setDefaultTextEncodingName(TEXT_ENCODING);

            // Display result and correct weather symbol in UI.
            Log.d("haugis", "imgResourceID: " + imgResourceID);

            imgView.setImageResource(imgResourceID);
            webView.loadData(result, "text/html; charset=utf-8", TEXT_ENCODING);
        }

    }

    /**
     * Fetch and process XML content from yr.no, parseXML and combine it with HTML markup.
     *
     * @param webserviceURL
     * @throws IOException
     * @throws XmlPullParserException
     */
    private String loadXML(String webserviceURL) throws IOException, XmlPullParserException {
        InputStream stream = null;

        List<WeatherData> weatherDataList = null;
        StringBuilder htmlString = new StringBuilder();

        try {
            stream = downloadXML(webserviceURL);
            weatherDataList = new YrXMLParser().parseXML(stream);

        } finally {
            if (stream != null) {
                stream.close();
            }
        }

        StringBuilder imgFilename = new StringBuilder();
        for (WeatherData w : weatherDataList) {

            // Location
            htmlString
                    .append("<h1 >")
                    .append(w.locationName)
                    .append("</h1>");

            // Weekday: weather description, (temperature)
            htmlString
                    .append("<h4>")
                    .append(w.weekday.toUpperCase()).append(": ")
                    .append(w.symbolName).append(", ")
                    .append(w.temperatureValue).append("&deg;C")
                    .append("</h4>");

            // Weather forecast summary
            htmlString
                    .append("<p  style='clear:both'>")
                    .append(w.forecastText)
                    .append("</p>");

            // Append prefix "i" before symbol variable from XML file to get correct weather img.
            imgFilename.append("i").append(w.symbolVar);
        }

        imgResourceID = getResources().getIdentifier(imgFilename.toString(), "drawable", getPackageName());
        return htmlString.toString();
    }

    /**
     * @param webserviceURL
     * @throws IOException
     */
    private InputStream downloadXML(String webserviceURL) throws IOException {
        URL url = new URL(webserviceURL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setReadTimeout(10000 /* milliseconds */);
        conn.setConnectTimeout(15000 /* milliseconds */);
        conn.setRequestMethod("GET");
        conn.setDoInput(true);

        // Perform a GET request, and fetch XML content as an InputStream,
        // once connection has been established.
        conn.connect();
        InputStream stream = conn.getInputStream();

        return stream;
    }

}