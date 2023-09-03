package com.example.weatherapp;


import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private RelativeLayout homeRL;
    private ProgressBar loadingPB;
    private TextView cityNameTv, temperatureTV, conditionTV, windTV, cloudTV, humidityTV;
    private EditText cityEdit;
    private ImageView backIV, iconIV, searchIV, countryFlag;
    private LocationManager locationManager;
    private int PERMISSION_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        );

        setContentView(R.layout.activity_main);

        loadingPB = findViewById(R.id.Loading_id);
        cityNameTv = findViewById(R.id.idTVCityName);
        temperatureTV = findViewById(R.id.idTVTemperature);
        conditionTV = findViewById(R.id.idTVCondition);
        cityEdit = findViewById(R.id.idETCity);
        backIV = findViewById(R.id.IdIVBack);
        iconIV = findViewById(R.id.idIVIcon);
        searchIV = findViewById(R.id.idTVSearch);
        windTV = findViewById(R.id.idTVWindTextMetric);
        cloudTV = findViewById(R.id.idTVCloudTextMetric);
        humidityTV = findViewById(R.id.idTVCHumidTextMetric);
        countryFlag = findViewById(R.id.idIVFlag);
        homeRL = findViewById(R.id.idRLHome);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    MainActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    PERMISSION_CODE
            );
        } else {
            getLocation();
        }

        setTimeBasedBackground();

        searchIV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String city = cityEdit.getText().toString().trim();
                if (city.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Please enter a city name", Toast.LENGTH_SHORT).show();
                } else {
                    cityNameTv.setText(city);
                    getWeatherInfo(city);
                }
            }
        });
    }

    private void getLocation() {
        try {
            Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if (location != null) {
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();
                String cityName = getCityName(latitude, longitude);
                cityNameTv.setText(cityName);
                getWeatherInfo(cityName);
            }
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    private void setTimeBasedBackground() {
        Calendar calendar = Calendar.getInstance();
        int currentHour = calendar.get(Calendar.HOUR_OF_DAY);
        int backgroundResource;

        if (currentHour >= 18 || currentHour < 6) {
            backgroundResource = R.drawable.night;
        } else {
            backgroundResource = R.drawable.day;
        }

        backIV.setImageResource(backgroundResource);
        backIV.setScaleType(ImageView.ScaleType.CENTER_CROP);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLocation();
            } else {
                Toast.makeText(this, "Please provide location permission", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private String getCityName(double latitude, double longitude) {
        String cityName = "Not Found";
        Geocoder gcd = new Geocoder(getBaseContext(), Locale.getDefault());
        try {
            List<Address> addresses = gcd.getFromLocation(latitude, longitude, 1);
            if (!addresses.isEmpty()) {
                cityName = addresses.get(0).getLocality();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return cityName;
    }

    private void getWeatherInfo(String city) {
        String apiKey = "6dd11d9eedc30352714b710567053ba5"; // Replace with your actual OpenWeatherMap API key
        String url = "https://api.openweathermap.org/data/2.5/weather?q=" + city + "&appid=" + apiKey + "&units=metric";

        RequestQueue requestQueue = null;
        try {
            requestQueue = Volley.newRequestQueue(MainActivity.this);
        } catch (Exception e) {
            e.printStackTrace();
        }


        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONObject mainObject = response.getJSONObject("main");
                            String temperature = mainObject.getString("temp");
                            temperatureTV.setText(temperature + "°C");

                            JSONArray weatherArray = response.getJSONArray("weather");
                            if (weatherArray.length() > 0) {
                                JSONObject weatherObject = weatherArray.getJSONObject(0);
                                String condition = weatherObject.getString("main");
                                String description = weatherObject.getString("description");
                                conditionTV.setText(condition + " (" + description + ")");

                                double windSpeed = response.getJSONObject("wind").getDouble("speed");
                                windTV.setText(windSpeed + " m/s");

                                int cloudPercentage = response.getJSONObject("clouds").getInt("all");
                                cloudTV.setText(cloudPercentage + "%");

                                double humidity = mainObject.getDouble("humidity");
                                humidityTV.setText(humidity + "%");

                                //    String countryCode = response.getJSONObject("sys").getString("country");
                                //     String countryFlagUrl = "https://flagcdn.com/144x108/" + countryCode.toLowerCase() + ".png";
                                //      Picasso.get().load(countryFlagUrl).into(countryFlag);

                                //      String iconCode = weatherObject.getString("icon");
                                //      String iconUrl = "https://openweathermap.org/img/w/" + iconCode + ".png";
                                //       Picasso.get().load(iconUrl).into(iconIV);

                                // Update background based on weather conditions (if needed)

                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(MainActivity.this, "Error fetching weather data", Toast.LENGTH_SHORT).show();
                    }
                });

        requestQueue.add(jsonObjectRequest);
    }
}
