package com.jadedev.covid_19;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RecyclerView mList;

    private LinearLayoutManager linearLayoutManager;
    private DividerItemDecoration dividerItemDecoration;
    private List<CountryCovid> countryCovidList;
    private CovidAdapter adapter;

    private String url = "https://covid19api.herokuapp.com/";

    private int order_list = -1;
    private int data_type = -1;
    private int latest_confirmed = -1;
    private int latest_recovered = -1;
    private int latest_deaths = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mList = findViewById(R.id.main_list);

        countryCovidList = new ArrayList<>();
        adapter = new CovidAdapter(getApplicationContext(), countryCovidList);

        linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        dividerItemDecoration = new DividerItemDecoration(mList.getContext(), linearLayoutManager.getOrientation());

        mList.setHasFixedSize(true);
        mList.setLayoutManager(linearLayoutManager);
        mList.addItemDecoration(dividerItemDecoration);
        mList.setAdapter(adapter);

        getData(data_type, order_list);
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_app, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_refresh:
                data_type = -1;
                order_list = -1;
                getData(data_type, order_list);
                return true;
            case R.id.action_info:
                Intent intent = new Intent(getBaseContext(), InfoActivity.class);
                intent.putExtra("confirmed",latest_confirmed);
                intent.putExtra("deaths",latest_deaths);
                intent.putExtra("recovered",latest_recovered);
                startActivity(intent);
                return true;
            case R.id.action_order:
                if (order_list == -1) {
                    order_list = 1;
                    Toast.makeText(this, "Ascending Order", Toast.LENGTH_SHORT).show();
                } else {
                    order_list = -1;
                    Toast.makeText(this, "Descending Order", Toast.LENGTH_SHORT).show();
                }
                getData(data_type, order_list);
                return true;
            case R.id.action_filter:
                // setup the alert builder
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Choose a COVID-19 data");

                // add a list
                final String[] data_type_ = {"Confirmed", "Deaths", "Recovered"};
                builder.setItems(data_type_, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                data_type = -1;
                                break;
                            case 1:
                                data_type = 0;
                                break;
                            default:
                                data_type = 1;
                                break;
                        }
                        getData(data_type, order_list);
                    }
                });

                // create and show the alert dialog
                AlertDialog dialog = builder.create();
                dialog.show();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void getData(final int type, final int order) {

        countryCovidList.clear();

        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Loading...");
        progressDialog.show();

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {

                String type_;

                switch (type) {
                    case 0:
                        type_ = "deaths";
                        break;
                    case 1:
                        type_ = "recovered";
                        break;
                    default:
                        type_ = "confirmed";
                }


                JSONArray jsonArray = null;
                JSONObject jsonTmp = null;

                try {
                    jsonArray = response.getJSONObject(type_).getJSONArray("locations");
                    jsonTmp = response.getJSONObject("latest");
                    latest_confirmed = jsonTmp.getInt("confirmed");
                    latest_recovered = jsonTmp.getInt("recovered");
                    latest_deaths = jsonTmp.getInt("deaths");
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                for (int i = 0; i < jsonArray.length(); i++) {
                    try {
                        JSONObject jsonObject = jsonArray.getJSONObject(i);

                        CountryCovid countryCovid = new CountryCovid();
                        countryCovid.setCountry(jsonObject.getString("country"));
                        countryCovid.setCountry_code(jsonObject.getString("country_code"));
                        countryCovid.setLatest(jsonObject.getInt("latest"));
                        countryCovid.setProvince(jsonObject.getString("province"));

                        countryCovidList.add(countryCovid);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        progressDialog.dismiss();
                    }
                }

                if (order_list == -1) {
                    Collections.sort(countryCovidList, new Comparator<CountryCovid>() {
                        @Override
                        public int compare(CountryCovid lhs, CountryCovid rhs) {
                            if (lhs.getLatest() >= rhs.getLatest()) {
                                return -1;
                            } else {
                                return 1;
                            }
                        }
                    });
                } else {
                    Collections.sort(countryCovidList, new Comparator<CountryCovid>() {
                        @Override
                        public int compare(CountryCovid lhs, CountryCovid rhs) {
                            if (lhs.getLatest() < rhs.getLatest()) {
                                return -1;
                            } else {
                                return 1;
                            }
                        }
                    });
                }

                adapter.notifyDataSetChanged();
                progressDialog.dismiss();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("Volley", error.toString());
                progressDialog.dismiss();
            }
        });
        RequestQueue requestQueue = Volley.newRequestQueue(this);
        requestQueue.add(jsonObjectRequest);
    }

}
