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

    // Declare the UI-widgets
    TextView tvBearishDays, tvVolume, tvBuy, tvSell;
    EditText etStartDate, etEndDate;
    Button btnUpdate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvBearishDays = findViewById(R.id.tvBearishDays);
        tvVolume = findViewById(R.id.tvVolume);
        tvBuy = findViewById(R.id.tvBuy);
        tvSell = findViewById(R.id.tvSell);
        etStartDate = findViewById(R.id.etStartDate);
        etEndDate = findViewById(R.id.etEndDate);
        btnUpdate = findViewById(R.id.btnUpdate);

        // When btnUpdate is clicked, the user chosen date is converted into unix timestamp using the HandleRequest's dateToTimestamp() -method.
        btnUpdate.setOnClickListener(v -> {
            String startDate = etStartDate.getText().toString();
            String endDate = etEndDate.getText().toString();
            HandleRequest.dateToTimestamp(startDate,endDate);
            fetchData();
        });
    }

    private void fetchData () {
        String startDate = String.valueOf(HandleRequest.startTimestamp);
        String endDate = String.valueOf(HandleRequest.endTimestamp);
        try {
            RequestQueue queue = Volley.newRequestQueue(this);
            String url = "https://api.coingecko.com/api/v3/coins/bitcoin/market_chart/range?vs_currency=eur&from=" + startDate + "&to=" + endDate;
            StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                    response -> {
                        HandleRequest.handleJSON(response);
                        tvBearishDays.setText(HandleRequest.bearishDaysString);
                        tvVolume.setText(String.format("%.2f", HandleRequest.maxVolume) + " € " + " on " + HandleRequest.volumeDate);
                        tvBuy.setText(HandleRequest.buyDateString);
                        tvSell.setText(HandleRequest.sellDateString);
                    },
                    error -> System.out.println(error.toString())
            );
            queue.add(stringRequest);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}