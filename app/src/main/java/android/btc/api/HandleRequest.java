package android.btc.api;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HandleRequest {

    Double maxPrice, currentPrice;
    long startTimestamp, endTimestamp;

    List<String> pricesString = new ArrayList<>();
    List<Double> pricesDouble = new ArrayList<>();

    public void handleJson(String response) {
        try {
            JSONObject jsonObject = new JSONObject(response);
            JSONArray pricesArray = jsonObject.getJSONArray("prices");
            getBearishDays(pricesArray);
            for (int i=0; i<pricesArray.length(); i++) {
                pricesString.add(pricesArray.getJSONArray(i).get(1).toString());
            }

            for (int i=0; i<pricesString.size(); i++) {
                pricesDouble.add(Double.parseDouble(pricesString.get(i)));
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
        maxPrice = Collections.max(pricesDouble);
        currentPrice = pricesDouble.get(pricesDouble.size()-1);
    }

    public void getBearishDays(JSONArray pricesArray) throws JSONException {
        System.out.println(pricesArray.length());
        for (int i=0; i<pricesArray.length(); i++) {
            System.out.println();
            if (i%24 == 0) {
                System.out.println(pricesArray.getJSONArray(i));
            }
        }
    }

    public void dateToEpochTime(String startDate, String endDate) {
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
}
