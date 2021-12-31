package android.btc.api;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HandleRequest {

    long startTimestamp, endTimestamp;
    long dailyData = 7776000;
    int bearishDaysCount;
    double currentDay, previousDay, volume, maxVolume, buyPrice, sellPrice, price;
    String currentTime, previousTime, time, volumeTime, buyTime, sellTime;

    List<Integer> bearishDaysList = new ArrayList<>();
    LocalDate volumeDate, buyDate, sellDate;

    // The response string is converted into JSON object
    public void handleJson(String response) {
        try {
            JSONObject jsonObject = new JSONObject(response);
            JSONArray pricesArray = jsonObject.getJSONArray("prices");
            JSONArray volumesArray = jsonObject.getJSONArray("total_volumes");
            getBearishDays(pricesArray);
            getHighestVolume(volumesArray);
            getMostProfit(pricesArray);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    // Gets the key for equivalent value in a hashmap
    public static <K, V> K getKey(Map<K, V> map, V value)
    {
        return map.entrySet()
                .stream()
                .filter(entry -> value.equals(entry.getValue()))
                .map(Map.Entry::getKey)
                .findFirst().get();
    }

    // Converts datetime to unix timestamp
    public void dateToTimeStamp(String startDate, String endDate) {
        try {
            startTimestamp = LocalDate
                    .parse(startDate, DateTimeFormatter.ofPattern("dd-MM-uu"))
                    .atStartOfDay(ZoneOffset.UTC)
                    .toInstant().toEpochMilli() / 1000;
            endTimestamp = LocalDate
                    .parse(endDate, DateTimeFormatter.ofPattern("dd-MM-uu"))
                    .atStartOfDay(ZoneOffset.UTC)
                    .toInstant().toEpochMilli() / 1000 + 3600;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Function for getting the best buy and sell date for the examined time period
    public void getMostProfit(JSONArray pricesArray) throws JSONException {
        HashMap<String, Double> pricesMap = new HashMap<>();
        // If the examined time period is less than 90 days
        if (endTimestamp - startTimestamp < dailyData) {
            for (int i=0; i<pricesArray.length(); i++) {
                if (i%24 == 0) {
                    time = pricesArray.getJSONArray(i).get(0).toString();
                    price = Double.parseDouble(pricesArray.getJSONArray(i).get(1).toString());
                    if (i > 0) {
                        time = pricesArray.getJSONArray(i+1).get(0).toString();
                        price = Double.parseDouble(pricesArray.getJSONArray(i+1).get(1).toString());
                    }
                    pricesMap.put(time, price);
                }
            }
        }
        // If the examined time period is greater than 90 days
        else if (endTimestamp - startTimestamp >= dailyData) {
            for (int i=0; i<pricesArray.length(); i++) {
                time = pricesArray.getJSONArray(i).get(0).toString();
                price = Double.parseDouble(pricesArray.getJSONArray(i).get(1).toString());
                pricesMap.put(time, price);
            }
        }
        buyPrice = Collections.min(pricesMap.values());
        sellPrice = Collections.max(pricesMap.values());
        buyTime = getKey(pricesMap, buyPrice);
        sellTime = getKey(pricesMap, sellPrice);
        buyDate = Instant.ofEpochMilli(Long.parseLong(buyTime)).atZone(ZoneId.systemDefault()).toLocalDate();
        sellDate = Instant.ofEpochMilli(Long.parseLong(sellTime)).atZone(ZoneId.systemDefault()).toLocalDate();
        System.out.println(buyTime + " " + buyPrice);
        System.out.println(sellTime + " " + sellPrice);
    }

    // Function for getting the highest volume and its date for the examined time period
    public void getHighestVolume(JSONArray volumesArray) throws JSONException {
        HashMap<String, Double> volumesMap = new HashMap<>();
        System.out.println(volumesArray.length());
        // If the examined time period is less than 90 days
        if (endTimestamp - startTimestamp < dailyData) {
            for (int i=0; i<volumesArray.length(); i++) {
                // Get the time and volume every 24 hours
                if (i%24 == 0) {
                    time = volumesArray.getJSONArray(i).get(0).toString();
                    volume = Double.parseDouble(volumesArray.getJSONArray(i).get(1).toString());
                    volumesMap.put(time, volume);
                }
            }
        }
        // If the examined time period is greater than 90 days
        else if (endTimestamp - startTimestamp >= dailyData) {
            for (int i=0; i<volumesArray.length(); i++) {
                time = volumesArray.getJSONArray(i).get(0).toString();
                volume = Double.parseDouble(volumesArray.getJSONArray(i).get(1).toString());
                volumesMap.put(time, volume);
            }
        }
        maxVolume = Collections.max(volumesMap.values());
        volumeTime = getKey(volumesMap, maxVolume);
        volumeDate = Instant.ofEpochMilli(Long.parseLong(volumeTime)).atZone(ZoneId.systemDefault()).toLocalDate();
        System.out.println(volumeTime + " " + maxVolume);
    }

    // Function for getting the longest bearish trend for the examined time period
    public void getBearishDays(@NonNull JSONArray pricesArray) throws JSONException {
        bearishDaysCount = 0;
        // If the examined time period is less than 90 days
        if (endTimestamp - startTimestamp < dailyData) {
            for (int i=0; i<(pricesArray.length()-1); i++) {
                // Get the price every 24 hours
                if (i%24 == 0) {
                    currentDay = Double.parseDouble(pricesArray.getJSONArray(i).get(1).toString());
                    currentTime = pricesArray.getJSONArray(i).get(0).toString();
                    if (i > 0) {
                        previousDay = Double.parseDouble(pricesArray.getJSONArray(i-24).get(1).toString());
                        previousTime = pricesArray.getJSONArray(i-24).get(0).toString();
                    }
                    if (i > 24) {
                        previousTime = pricesArray.getJSONArray(i-23).get(0).toString();
                        previousDay = Double.parseDouble(pricesArray.getJSONArray(i-23).get(1).toString());
                    }
                    if (currentDay < previousDay) {
                        bearishDaysCount = bearishDaysCount + 1;
                    }
                    else if (currentDay > previousDay && bearishDaysCount > 0){
                        bearishDaysList.add(bearishDaysCount);
                        bearishDaysCount = 0;
                    }
                }
            }
        }
        // If the examined time period is greater than 90 days
        else if (endTimestamp - startTimestamp >= dailyData) {
            for (int i=0; i<pricesArray.length(); i++) {
                currentDay = Double.parseDouble(pricesArray.getJSONArray(i).get(1).toString());
                currentTime = pricesArray.getJSONArray(i).get(0).toString();
                if (i > 0) {
                    previousDay = Double.parseDouble(pricesArray.getJSONArray(i-1).get(1).toString());
                    previousTime = pricesArray.getJSONArray(i-1).get(0).toString();
                }
                if (currentDay < previousDay) {
                    bearishDaysCount = bearishDaysCount + 1;
                }
                else if (currentDay > previousDay && bearishDaysCount > 0){
                    bearishDaysList.add(bearishDaysCount);
                    bearishDaysCount = 0;
                }
            }
        }
        if (bearishDaysList.size() > 0) {
            bearishDaysCount = Collections.max(bearishDaysList);
            /* For debugging
            System.out.println("Subsequent Bearish days list size: " + bearishDaysList.size());
            System.out.println("Subsequent Bearish days list: " + bearishDaysList);
            System.out.println("Most subsequent bearish days: " + bearishDaysCount);
            */
            bearishDaysList.clear();
        }
    }
}
