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
import java.util.TreeMap;

public class HandleRequest {

    String currentTime, previousTime, time, volumeTime, buyTime, sellTime, buyDateString, sellDateString, bearishDaysString;
    Long startTimestamp, endTimestamp, timeLong;
    Double price;
    long dailyData = 7776000;
    int bearishDaysCount;
    double currentPrice, previousPrice, nextPrice, buyPrice, sellPrice, volume, maxVolume, currentDifference, newDifference;

    List<Integer> bearishDaysList = new ArrayList<>();
    LocalDate volumeDate, buyDate, sellDate;

    /* The response string is converted into JSON object */
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

    /* Gets the key for equivalent value in a map */
    public static <K, V> K getKey(Map<K, V> map, V value) {
        return map.entrySet()
                .stream()
                .filter(entry -> value.equals(entry.getValue()))
                .map(Map.Entry::getKey)
                .findFirst().get();
    }

    /* Converts datetime to unix timestamp */
    public void dateToTimestamp(String startDate, String endDate) {
        try {
            startTimestamp = LocalDate
                    .parse(startDate, DateTimeFormatter.ofPattern("dd-MM-uu"))
                    .atStartOfDay(ZoneOffset.UTC)
                    .toInstant().toEpochMilli() / 1000;
            // One hour (3600 seconds) is added to the endTimestamp.
            endTimestamp = LocalDate
                    .parse(endDate, DateTimeFormatter.ofPattern("dd-MM-uu"))
                    .atStartOfDay(ZoneOffset.UTC)
                    .toInstant().toEpochMilli() / 1000 + 3600;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* Function for getting the best buy and sell date for the examined time period */
    public void getMostProfit(JSONArray pricesArray) throws JSONException {
        List<Double> pricesList = new ArrayList<>();
        TreeMap<Long, Double> pricesMap = new TreeMap<>();
        currentDifference = 0;

        // If the examined time period is less than 90 days
        if (endTimestamp - startTimestamp < dailyData) {
            for (int i=0; i<(pricesArray.length()-1); i++) {
                // Get the time and price every 24 hours
                if (i%24 == 0) {
                    timeLong = Long.parseLong(pricesArray.getJSONArray(i).get(0).toString());
                    price = Double.parseDouble(pricesArray.getJSONArray(i).get(1).toString());
                    if (i > 0) {
                        timeLong = Long.parseLong(pricesArray.getJSONArray(i+1).get(0).toString());
                        price = Double.parseDouble(pricesArray.getJSONArray(i+1).get(1).toString());
                    }
                    pricesMap.put(timeLong, price);
                    pricesList.add(price);
                }
            }
        }

        // If the examined time period is greater than 90 days
        else if (endTimestamp - startTimestamp >= dailyData) {
            for (int i=0; i<pricesArray.length(); i++) {
                timeLong = Long.parseLong(pricesArray.getJSONArray(i).get(0).toString());
                price = Double.parseDouble(pricesArray.getJSONArray(i).get(1).toString());
                pricesMap.put(timeLong, price);
                pricesList.add(price);
            }
        }

        // I used nested for-loop to get the best days for selling and buying. The nesting makes sure that the buying day is always prior to the selling day.
        for (int i=0; i<pricesList.size(); i++) {
            for (int j=i; j<pricesList.size(); j++) {
                currentPrice = Double.parseDouble(pricesList.get(i).toString());
                nextPrice = Double.parseDouble(pricesList.get(j).toString());
                newDifference = nextPrice - currentPrice;
                if (newDifference > currentDifference) {
                    currentDifference = newDifference;
                    currentTime = getKey(pricesMap, currentPrice).toString();
                    buyTime = currentTime;
                    sellTime = getKey(pricesMap, nextPrice).toString();
                    buyPrice = currentPrice;
                    sellPrice = nextPrice;
                    buyDate = Instant.ofEpochMilli(Long.parseLong(buyTime)).atZone(ZoneId.systemDefault()).toLocalDate();
                    sellDate = Instant.ofEpochMilli(Long.parseLong(sellTime)).atZone(ZoneId.systemDefault()).toLocalDate();
                    buyDateString = buyDate.toString();
                    sellDateString = sellDate.toString();
                }

                // If the price is only decreasing, currentDifference is 0
                if (currentDifference == 0) {
                    buyDateString = "No profit available";
                    sellDateString = "";
                }
            }
        }
    }

    /* Function for getting the highest volume and its date for the examined time period */
    public void getHighestVolume(JSONArray volumesArray) throws JSONException {
        HashMap<String, Double> volumesMap = new HashMap<>();

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
    }

    /* Function for getting the longest bearish trend for the examined time period */
    public void getBearishDays(@NonNull JSONArray pricesArray) throws JSONException {
        bearishDaysCount = 0;
        bearishDaysString = "0";

        // If the examined time period is less than 90 days
        if (endTimestamp - startTimestamp < dailyData) {
            System.out.println(pricesArray.length());
            for (int i=0; i<(pricesArray.length()); i++) {
                // Get the price every 24 hours
                if (i%24 == 0) {
                    currentPrice = Double.parseDouble(pricesArray.getJSONArray(i).get(1).toString());
                    currentTime = pricesArray.getJSONArray(i).get(0).toString();
                    if (i > 0) {
                        previousPrice = Double.parseDouble(pricesArray.getJSONArray(i-24).get(1).toString());
                        previousTime = pricesArray.getJSONArray(i-24).get(0).toString();
                        if (i > 24) {
                            previousTime = pricesArray.getJSONArray(i-23).get(0).toString();
                            previousPrice = Double.parseDouble(pricesArray.getJSONArray(i-23).get(1).toString());
                        }
                        if (currentPrice < previousPrice) {
                            bearishDaysCount = bearishDaysCount + 1;
                        }
                        // BearishDaysCount gets added into the list when the price is increasing or the pricesArray comes to an end
                        if (currentPrice > previousPrice || (i >= pricesArray.length()-23)) {
                            bearishDaysList.add(bearishDaysCount);
                            bearishDaysCount = 0;
                        }
                    }
                }
            }
        }

        // If the examined time period is greater than 90 days
        else if (endTimestamp - startTimestamp >= dailyData) {
            for (int i=0; i<pricesArray.length(); i++) {
                currentPrice = Double.parseDouble(pricesArray.getJSONArray(i).get(1).toString());
                currentTime = pricesArray.getJSONArray(i).get(0).toString();
                if (i > 0) {
                    previousPrice = Double.parseDouble(pricesArray.getJSONArray(i-1).get(1).toString());
                    previousTime = pricesArray.getJSONArray(i-1).get(0).toString();
                    if (currentPrice < previousPrice) {
                        bearishDaysCount = bearishDaysCount + 1;
                    }
                    if (currentPrice > previousPrice && bearishDaysCount != 0){
                        bearishDaysList.add(bearishDaysCount);
                        bearishDaysCount = 0;
                    }
                }
            }
        }

        if (bearishDaysList.size() > 0) {
            bearishDaysCount = Collections.max(bearishDaysList);
            bearishDaysString = String.valueOf(bearishDaysCount);
            bearishDaysList.clear();
        }
    }
}
