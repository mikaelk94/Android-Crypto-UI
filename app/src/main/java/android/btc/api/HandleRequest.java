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
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class HandleRequest {

    private static final long dailyData = 7776000; // 90 days in seconds
    public static double maxVolume;
    public static String buyDateString, sellDateString, bearishDaysString;
    public static Long startTimestamp, endTimestamp, timeLong;
    static double currentPrice, previousPrice, nextPrice, buyPrice, sellPrice, volume, currentDifference, newDifference;
    static int bearishDaysCount;
    static Double price;
    static String currentTime, previousTime, time, volumeTime, buyTime, sellTime;
    static LocalDate volumeDate, buyDate, sellDate;

    /* The response string is converted into JSON object */
    public static void handleJSON(String response) {
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

    /* Converts date to unix timestamp */
    public static void dateToTimestamp(String startDate, String endDate) {
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

    /* Gets the key for equivalent value in a map */
    private static <K, V> K getKey(Map<K, V> map, V value) {
        return map.entrySet()
                .stream()
                .filter(entry -> value.equals(entry.getValue()))
                .map(Map.Entry::getKey)
                .findFirst().get();
    }

    /* Gets the hour out of a timestamp */
    private static Integer timestampToHour(String timestamp) {
        return Instant.ofEpochMilli(Long.parseLong(timestamp)).atOffset(ZoneOffset.UTC).getHour();
    }

    /* Function for getting the longest bearish trend for the examined time period */
    private static void getBearishDays(@NonNull JSONArray pricesArray) throws JSONException {
        List<Integer> bearishDaysList = new ArrayList<>();
        currentPrice = 0;
        currentTime = "0";
        previousPrice = 0;
        previousTime = "0";
        bearishDaysCount = 0;
        bearishDaysString = "0";

        // If the examined time period is less than 90 days (hourly data)
        if (endTimestamp - startTimestamp < dailyData) {
            for (int i=0; i<(pricesArray.length()); i++) {
                // Get the price if the current JSON array's timestamp is 00:00 UTC in hours
                if (timestampToHour(pricesArray.getJSONArray(i).get(0).toString()) == 0) {
                    previousPrice = currentPrice;
                    previousTime = currentTime;
                    currentPrice = Double.parseDouble(pricesArray.getJSONArray(i).get(1).toString());
                    currentTime = pricesArray.getJSONArray(i).get(0).toString();
                    // Increase the bearishDaysCount when the price decreases
                    if (currentPrice < previousPrice) {
                        bearishDaysCount = bearishDaysCount + 1;
                    }
                    // BearishDaysCount gets added into the list when the price is increasing or the pricesArray comes to an end, and the count is not 0
                    if (currentPrice > previousPrice || (i == pricesArray.length()-1) && bearishDaysCount != 0) {
                        bearishDaysList.add(bearishDaysCount);
                        bearishDaysCount = 0;
                    }
                }
            }
        }

        // If the examined time period is greater than 90 days (daily data)
        else if (endTimestamp - startTimestamp >= dailyData) {
            for (int i=0; i<pricesArray.length(); i++ ) {
                previousPrice = currentPrice;
                previousTime = currentTime;
                currentPrice = Double.parseDouble(pricesArray.getJSONArray(i).get(1).toString());
                currentTime = pricesArray.getJSONArray(i).get(0).toString();
                // Increase the bearishDaysCount when the price decreases
                if (currentPrice < previousPrice) {
                    bearishDaysCount = bearishDaysCount + 1;
                }
                // BearishDaysCount gets added into the list when the price is increasing, and the count is not 0
                if (currentPrice > previousPrice && bearishDaysCount != 0){
                    bearishDaysList.add(bearishDaysCount);
                    bearishDaysCount = 0;
                }
            }
        }

        // If the list contains bearish days, the max value from the list is stored into bearishDaysString
        if (bearishDaysList.size() > 0) {
            bearishDaysCount = Collections.max(bearishDaysList);
            bearishDaysString = String.valueOf(bearishDaysCount);
            bearishDaysList.clear();
        }
    }

    /* Function for getting the highest volume and its date for the examined time period */
    private static void getHighestVolume(JSONArray volumesArray) throws JSONException {
        TreeMap<String, Double> volumesMap = new TreeMap<>();

        // If the examined time period is less than 90 days (hourly data)
        if (endTimestamp - startTimestamp < dailyData) {
            for (int i=0; i<volumesArray.length(); i++) {
                // Get the time and volume if the current JSON array's timestamp is 00:00 UTC in hours
                if (timestampToHour(volumesArray.getJSONArray(i).get(0).toString()) == 0) {
                    time = volumesArray.getJSONArray(i).get(0).toString();
                    volume = Double.parseDouble(volumesArray.getJSONArray(i).get(1).toString());
                    volumesMap.put(time, volume);
                }
            }
        }

        // If the examined time period is greater than 90 days (daily data)
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

    /* Function for getting the best buy and sell date for the examined time period */
    private static void getMostProfit(JSONArray pricesArray) throws JSONException {
        List<Double> pricesList = new ArrayList<>();
        TreeMap<Long, Double> pricesMap = new TreeMap<>();
        currentDifference = 0;

        // If the examined time period is less than 90 days (hourly data)
        if (endTimestamp - startTimestamp < dailyData) {
            for (int i=0; i<(pricesArray.length()-1); i++) {
                // Get the time and price if the current JSON array's timestamp is 00:00 UTC in hours
                if (timestampToHour(pricesArray.getJSONArray(i).get(0).toString()) == 0) {
                    timeLong = Long.parseLong(pricesArray.getJSONArray(i).get(0).toString());
                    price = Double.parseDouble(pricesArray.getJSONArray(i).get(1).toString());
                    pricesMap.put(timeLong, price);
                    pricesList.add(price);
                }
            }
        }

        // If the examined time period is greater than 90 days (daily data)
        else if (endTimestamp - startTimestamp >= dailyData) {
            for (int i=0; i<pricesArray.length(); i++) {
                timeLong = Long.parseLong(pricesArray.getJSONArray(i).get(0).toString());
                price = Double.parseDouble(pricesArray.getJSONArray(i).get(1).toString());
                pricesMap.put(timeLong, price);
                pricesList.add(price);
            }
        }

        // Nested for-loop to get the best days for buying and selling. The nesting makes sure that the buying day is always prior to the selling day.
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
}
