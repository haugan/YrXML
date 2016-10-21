package net.pugfood.androidxml;

import android.util.Log;
import android.util.Xml;

import net.pugfood.androidxml.model.WeatherData;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class YrXMLParser {

    private final int START_TAG = XmlPullParser.START_TAG;
    private final int END_TAG = XmlPullParser.END_TAG;
    private final int END_DOCUMENT = XmlPullParser.END_DOCUMENT;

    private final String NAMESPACE = null;
    private final String DEBUG_TAG = "haugis";

    private int numberOfTagsRead = 0;

    ///// -------------------------
    ///// * * * * * * * * * * * * *
    ///// DEBUGGING INFO TO LOGGER

    /**
     * @param parser
     * @throws XmlPullParserException
     */
    private void logCurrentState(XmlPullParser parser) throws XmlPullParserException {
        numberOfTagsRead++;
        Log.i(DEBUG_TAG, "Line/Column: " + parser.getLineNumber() + "/" + parser.getColumnNumber());
        Log.i(DEBUG_TAG, "Event Type: " + parser.TYPES[parser.getEventType()]);
        Log.i(DEBUG_TAG, "Name: " + parser.getName());
        Log.i(DEBUG_TAG, "Depth: " + parser.getDepth());
        Log.i(DEBUG_TAG, "*************");
    }

    /**
     * @param xpp
     * @throws XmlPullParserException
     * @throws IOException
     */
    private void logAllEvents(XmlPullParser xpp) throws IOException, XmlPullParserException {
        while (xpp.getEventType() != xpp.END_DOCUMENT) {
            switch (xpp.getEventType()) {
                case XmlPullParser.START_DOCUMENT:
                    Log.d(DEBUG_TAG, "Start document!");
                    break;
                case XmlPullParser.START_TAG:
                    Log.d(DEBUG_TAG, "Start tag: " + xpp.getName());
                    break;
                case XmlPullParser.TEXT:
                    Log.d(DEBUG_TAG, "Text: " + xpp.getText());
                    break;
                case XmlPullParser.END_TAG:
                    Log.d(DEBUG_TAG, "End tag: " + xpp.getName());
                    Log.i(DEBUG_TAG, "*************");
                    break;
                case XmlPullParser.END_DOCUMENT:
                    Log.d(DEBUG_TAG, "End Document!");
                    break;
            }

            xpp.next();
        }
    }

    ///// -------------------------
    ///// * * * * * * * * * * * * *
    ///// INSTANTIATING THE PARSER

    /**
     * A parser is initialized to NOT process namespaces,
     * and to use provided InputStream as its input.
     *
     * It starts parsing with a call to nextTag(),
     * and invokes readXML() - which extracts and processes
     * all data the application is interested in.
     *
     * @param stream
     * @throws IOException
     * @throws XmlPullParserException
     */
    public List<WeatherData> parseXML(InputStream stream) throws IOException, XmlPullParserException {
        try {
            XmlPullParser xpp = Xml.newPullParser();
            xpp.setInput(stream, "utf-8");

            // Disable namespace awareness of the factory because it's not needed,
            // and will improve parsing speed.
            xpp.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);


            // Calls next() and return event type, but only if it's a START_ or END_TAG.
            // Skips whitespace TEXT before actual tag (if any).
            xpp.nextTag();
            // next() advances parser to next event.
            // The int value returned determines current parser state.
            // Empty elements <tag></tag> or </tag> both report two separate events:
            // START_TAG & END_TAG.

            return readXML(xpp);

        } finally {
            stream.close();
        }
    }

    ///// -------------------------
    ///// * * * * * * * * * * * * *
    ///// READING THE XML DOCUMENT

    /**
     * TODO: Få bedre kontroll over XML parsing av nested tags.
     *
     * Does actual work of processing the XML, looking for elements tagged as "location"
     * as a starting point for processing it.
     *
     * Once the whole XML document has been processed,
     * the method returns a List containing the entries (including the nested data members)
     * it extracted from the XML. This List is then returned by parseXML().
     *
     * @param xpp
     * @throws IOException
     * @throws XmlPullParserException
     */
    private List<WeatherData> readXML(XmlPullParser xpp) throws IOException, XmlPullParserException {
        List<WeatherData> weatherDataList = new ArrayList<>();
        WeatherData weatherData = new WeatherData();
        String tag;

        xpp.require(START_TAG, NAMESPACE, "weatherdata");
        while (xpp.next() != END_DOCUMENT) {
            if (xpp.getEventType() != START_TAG) {
                // Skip to the end of current iteration (the innermost while-loop's body)
                // and evaluate the expression that controls the loop again.
                continue;
            }

            // DEBUG!
            logCurrentState(xpp);
            Log.i(DEBUG_TAG, "Number of tags read: " + numberOfTagsRead);

            tag = xpp.getName();
            if (tag.equals("location")) {

                while (xpp.next() != END_TAG) {
                    if (xpp.getEventType() != START_TAG) {
                        continue;
                    }
                    tag = xpp.getName();
                    if (tag.equals("name")) {
                        weatherData.locationName = readText(xpp);
                    } else {
                        skip(xpp);
                    }
                }

            } else if (tag.equals("meta")) {

                while (xpp.next() != END_TAG) {
                    if(xpp.getEventType() != START_TAG) {
                        continue;
                    }
                    tag = xpp.getName();
                    if (tag.equals("lastupdate")) {
                        weatherData.lastUpdate = readText(xpp);
                    } else {
                        skip(xpp);
                    }
                }

            } else if (tag.equals("forecast")) {

                while (xpp.next() != END_TAG) {
                    if (xpp.getEventType() != START_TAG) {
                        continue;
                    }
                    tag = xpp.getName();
                    if (tag.equals("text")) {
                        skipTags(xpp, 3);
                        weatherData.weekday = readText(xpp);
                        skipTags(xpp, 1);
                        weatherData.forecastText = readText(xpp);
                    } else {
                        skip(xpp);
                    }
                }

            } else if (tag.equals("tabular")) {

                while (xpp.next() != END_TAG) {
                    if (xpp.getEventType() != START_TAG) {
                        continue;
                    }
                    tag = xpp.getName();
                    if (tag.equals("time")) {

                        // TODO: Henter kun første time tag..?

                        // </symbol>
                        skipTags(xpp, 2);
                        weatherData.symbolName = xpp.getAttributeValue(2);
                        weatherData.symbolVar = xpp.getAttributeValue(3);

                        // </precipitation>
                        skipTags(xpp, 2);

                        // </windDirection>
                        skipTags(xpp, 2);
                        weatherData.windDirectionDeg = xpp.getAttributeValue(0);
                        weatherData.windDirectionName = xpp.getAttributeValue(2);

                        // </windSpeed>
                        skipTags(xpp, 2);
                        weatherData.windSpeedMps = xpp.getAttributeValue(0);
                        weatherData.windSpeedName = xpp.getAttributeValue(1);

                        // </temperature>
                        skipTags(xpp, 2);
                        weatherData.temperatureValue = xpp.getAttributeValue(1);

                    } else {
                        skip(xpp);
                    }
                }

            } else {
                skip(xpp);
            }

        }

        weatherDataList.add(weatherData);
        return weatherDataList;
    }

    ///// ---------------------------
    ///// * * * * * * * * * * * * * *
    ///// NAVIGATING THE XML DOCUMENT

    /**
     * Call nextTag() method of XmlPullParser n times.
     *
     * @param xpp
     * @param n
     * @throws IOException
     * @throws XmlPullParserException
     */
    private void skipTags(XmlPullParser xpp, int n) throws IOException, XmlPullParserException {
        for (int i = 0; i < n; i++) {
            xpp.nextTag();
        }
    }

    /**
     * @param xpp
     * @throws IOException
     * @throws XmlPullParserException
     */
    private String readText(XmlPullParser xpp) throws IOException, XmlPullParserException {
        String result = "";

        if (xpp.next() == XmlPullParser.TEXT) {
            result = xpp.getText();
            xpp.nextTag();
        }

        return result;
    }

    /**
     * Skip tags the parser isn't interested in.
     * If the next tag after a START_TAG isn't a matching END_TAG,
     * keep going until a matching END_TAG is found (indicated by value of depth; "0").
     *
     * @param xpp
     * @throws XmlPullParserException
     * @throws IOException
     */
    private void skip(XmlPullParser xpp) throws XmlPullParserException, IOException {
        if (xpp.getEventType() != XmlPullParser.START_TAG) {
            throw new IllegalStateException();
        }

        int depth = 1;
        while (depth != 0) {
            switch (xpp.next()) {
                case XmlPullParser.END_TAG:
                    depth--;
                    break;
                case XmlPullParser.START_TAG:
                    depth++;
                    break;
            }
        }
    }
}