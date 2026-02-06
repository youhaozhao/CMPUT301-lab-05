package com.example.lab5_starter;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements CityDialogFragment.CityDialogListener {

    private Button addCityButton;
    private Button deleteCityButton;
    private ListView cityListView;

    private ArrayList<City> cityArrayList;
    private ArrayAdapter<City> cityArrayAdapter;

    private FirebaseFirestore db;

    private CollectionReference citiesRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        db = FirebaseFirestore.getInstance();
        citiesRef = db.collection("cities");

        citiesRef.addSnapshotListener((value, error) -> {
            if (error != null) {
                Log.e("Firestore", error.toString());
            }
            if (value != null && !value.isEmpty()) {
                cityArrayList.clear();
                for (QueryDocumentSnapshot snapshot : value) {
                    String name = snapshot.getString("name");
                    String province = snapshot.getString("province");

                    cityArrayList.add(new City(name, province));
                }
                cityArrayAdapter.notifyDataSetChanged();
            }
        });

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Set views
        addCityButton = findViewById(R.id.buttonAddCity);
        cityListView = findViewById(R.id.listviewCities);
        deleteCityButton = findViewById(R.id.buttonDeleteCity);

        // create city array
        cityArrayList = new ArrayList<>();
        cityArrayAdapter = new CityArrayAdapter(this, cityArrayList);
        cityListView.setAdapter(cityArrayAdapter);

        // Fully driven by firestore
        // addDummyData();

        // set listeners
        addCityButton.setOnClickListener(view -> {
            CityDialogFragment cityDialogFragment = new CityDialogFragment();
            cityDialogFragment.show(getSupportFragmentManager(), "Add City");
        });

        // Used for record of list view's selected position and deletion
        final int[] selectedPosition = {-1};

        // Flag for delete mode
        final boolean[] isDeleteMode = {false};

        cityListView.setOnItemClickListener((adapterView, view, i, l) -> {
            selectedPosition[0] = i;
            City city = cityArrayAdapter.getItem(i);

            // if not in delete mode, call the cityDialogFragment to show edit page
            if (!isDeleteMode[0]) {
                CityDialogFragment cityDialogFragment = CityDialogFragment.newInstance(city);
                cityDialogFragment.show(getSupportFragmentManager(), "City Details");
            } else {
                if (selectedPosition[0] != -1) {
                    City tmp = cityArrayList.get(selectedPosition[0]);
                    deleteCity(tmp);
                    isDeleteMode[0] = false;
                    selectedPosition[0] = -1;
                }
            }
        });


        deleteCityButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isDeleteMode[0] = true;
            }
        });
    }

    @Override
    public void updateCity(City city, String title, String year) {
        // Delete the previous city
        citiesRef.document(city.getName()).delete();

        // Set the new city name and province
        city.setName(title);
        city.setProvince(year);
        cityArrayAdapter.notifyDataSetChanged();

        // Add new city to remote database
        citiesRef.document(city.getName()).set(city);
    }

    @Override
    public void addCity(City city) {
        cityArrayList.add(city);
        cityArrayAdapter.notifyDataSetChanged();

        citiesRef
                .document(city.getName())
                .set(city)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d("Firestore", "City has been successfully added");
                    }
                });
    }

    @Override
    public void deleteCity(City city) {
        cityArrayList.remove(city);
        cityArrayAdapter.notifyDataSetChanged();

        citiesRef.document(city.getName()).delete().addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Log.d("Firestore", "City has been successfully deleted");
            }
        });
    }

//    public void addDummyData() {
//        City m1 = new City("Edmonton", "AB");
//        City m2 = new City("Vancouver", "BC");
//        cityArrayList.add(m1);
//        cityArrayList.add(m2);
//        cityArrayAdapter.notifyDataSetChanged();
//    }
}