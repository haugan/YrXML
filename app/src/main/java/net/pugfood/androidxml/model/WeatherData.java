package net.pugfood.androidxml.model;

public class WeatherData {

    // <location>
    public String locationName = "";

    // <meta>
    public String lastUpdate = "";

    // <forecast><text><location><time>
    public String weekday = "";
    public String forecastText = "";

    // <forecast><tabular><time>
    public String symbolName = "";
    public String symbolVar = "";
    public String windDirectionDeg = "";
    public String windDirectionName = "";
    public String windSpeedMps = "";
    public String windSpeedName = "";
    public String temperatureValue = "";
}