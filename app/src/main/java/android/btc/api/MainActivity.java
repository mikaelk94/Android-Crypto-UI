package android.btc.api;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

public class MainActivity extends AppCompatActivity {

    // Initialize the UI-widgets
    TextView tvCurrentPrice;
    EditText etStartDate;
    EditText etEndDate;
    Button btnUpdate;

    // Initialize new object out of HandleRequest.class for handling the api-requests
    HandleRequest handleRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvCurrentPrice = findViewById(R.id.tvCurrentPrice);
        etStartDate = findViewById(R.id.etStartDate);
        etEndDate = findViewById(R.id.etEndDate);
        btnUpdate = findViewById(R.id.btnUpdate);

        handleRequest = new HandleRequest();

        // When btnUpdate is clicked, the user chosen date is converted into epoch timestamp using the handleRequest's dateToEpochTime() -method.
        btnUpdate.setOnClickListener(v -> {
            String startDate = etStartDate.getText().toString();
            String endDate = etEndDate.getText().toString();
            handleRequest.dateToEpochTime(startDate,endDate);
            fetchData();
        });
    }

    private void fetchData () {

        String startDate = String.valueOf(handleRequest.startTimestamp);
        String endDate = String.valueOf(handleRequest.endTimestamp);

        try {
            RequestQueue queue = Volley.newRequestQueue(this);
            String url = "https://api.coingecko.com/api/v3/coins/bitcoin/market_chart/range?vs_currency=eur&from=" + startDate + "&to=" + endDate;
            System.out.println(url);
            StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                    response -> {
                        handleRequest.handleJson(response);
                        tvCurrentPrice.setText(String.format("%.2f", handleRequest.currentPrice) + " €");
                    },
                    error -> System.out.println(error.toString())
            );
            queue.add(stringRequest);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}