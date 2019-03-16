package ke.co.talin.myapplication;

import android.app.AlertDialog;
import android.graphics.Color;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import dmax.dialog.SpotsDialog;
import ke.co.talin.myapplication.Common.Common;
import ke.co.talin.myapplication.Helper.DirectionJSONParser;
import ke.co.talin.myapplication.Model.Request;
import ke.co.talin.myapplication.Model.ShippingInfo;
import ke.co.talin.myapplication.Remote.IGoogleService;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TrackShipper extends FragmentActivity implements OnMapReadyCallback,ValueEventListener{

    private GoogleMap mMap;

    FirebaseDatabase database;
    DatabaseReference requests,shippingOrder;

    Request currentorder;

    IGoogleService mService;


    Marker shipperMarker;
    Polyline polyline;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_track_shipper);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        database = FirebaseDatabase.getInstance();
        requests = database.getReference("Requests");
        shippingOrder = database.getReference("OrdersToBeShipped");
        shippingOrder.addValueEventListener(this);

        mService = Common.getGoogleMapService();
    }

    @Override
    protected void onStop() {
        shippingOrder.removeEventListener(this);
        super.onStop();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);

        trackLocation();
    }

    private void trackLocation() {
        requests.child(Common.currentKey)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        currentorder = dataSnapshot.getValue(Request.class);
                        //if order has Address
                        if (currentorder.getAddress() != null && !currentorder.getAddress().isEmpty())
                        {
                            mService.getLocationFromAddress(new StringBuilder("https://maps.googleapis.com/maps/api/geocode/json?address=")
                                    .append(currentorder.getAddress()).toString())
                                    .enqueue(new Callback<String>() {
                                        @Override
                                        public void onResponse(Call<String> call, Response<String> response) {
                                            try{
                                                JSONObject jsonObject = new JSONObject(response.body());

                                                String lat =((JSONArray)jsonObject.get("results"))
                                                        .getJSONObject(0)
                                                        .getJSONObject("geometry")
                                                        .getJSONObject("location")
                                                        .get("lat").toString();

                                                String lng =((JSONArray)jsonObject.get("results"))
                                                        .getJSONObject(0)
                                                        .getJSONObject("geometry")
                                                        .getJSONObject("location")
                                                        .get("lng").toString();

                                                LatLng location = new LatLng(Double.parseDouble(lat),
                                                        Double.parseDouble(lng));

                                                mMap.addMarker(new MarkerOptions().position(location)
                                                        .title("Order destination")
                                                        .icon(BitmapDescriptorFactory.defaultMarker()));


                                                //Set Shipper Location
                                                shippingOrder.child(Common.currentKey)
                                                        .addListenerForSingleValueEvent(new ValueEventListener() {
                                                            @Override
                                                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                                ShippingInfo shippingInfo = dataSnapshot.getValue(ShippingInfo.class);

                                                                LatLng shipperLocation = new LatLng(shippingInfo.getLat(),shippingInfo.getLng());

                                                                if (shipperMarker == null)
                                                                {
                                                                    shipperMarker = mMap.addMarker(
                                                                            new MarkerOptions()
                                                                                    .position(shipperLocation)
                                                                                    .title("Shipper #"+shippingInfo.getOrderId())
                                                                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
                                                                    );
                                                                }else
                                                                {
                                                                    shipperMarker.setPosition(shipperLocation);
                                                                }

                                                                //Update Camera
                                                                CameraPosition cameraPosition = new CameraPosition.Builder()
                                                                        .target(shipperLocation)
                                                                        .zoom(16f)
                                                                        .bearing(0)
                                                                        .tilt(45)
                                                                        .build();
                                                                mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

                                                                //draw Routes
                                                                if (polyline != null)
                                                                    polyline.remove();
                                                                mService.getDirections(shipperLocation.latitude+","+shipperLocation.longitude
                                                                        ,currentorder.getAddress())
                                                                        .enqueue(new Callback<String>() {
                                                                            @Override
                                                                            public void onResponse(Call<String> call, Response<String> response) {
                                                                                new ParserTask().execute(response.body().toString());
                                                                            }

                                                                            @Override
                                                                            public void onFailure(Call<String> call, Throwable t) {

                                                                            }
                                                                        });

                                                            }

                                                            @Override
                                                            public void onCancelled(@NonNull DatabaseError databaseError) {

                                                            }
                                                        });

                                            } catch (JSONException e) {
                                                e.printStackTrace();
                                            }
                                        }

                                        @Override
                                        public void onFailure(Call<String> call, Throwable t) {

                                        }
                                    });
                        }//If Order has LatLng
                        else if(currentorder.getLatLng() != null && !currentorder.getAddress().isEmpty())
                        {
                            mService.getLocationFromAddress(new StringBuilder("https://maps.googleapis.com/maps/api/geocode/json?latlng=")
                                    .append(currentorder.getLatLng()).toString())
                                    .enqueue(new Callback<String>() {
                                        @Override
                                        public void onResponse(Call<String> call, Response<String> response) {
                                            try{
                                                JSONObject jsonObject = new JSONObject(response.body());

                                                String lat =((JSONArray)jsonObject.get("results"))
                                                        .getJSONObject(0)
                                                        .getJSONObject("geometry")
                                                        .getJSONObject("location")
                                                        .get("lat").toString();

                                                String lng =((JSONArray)jsonObject.get("results"))
                                                        .getJSONObject(0)
                                                        .getJSONObject("geometry")
                                                        .getJSONObject("location")
                                                        .get("lng").toString();

                                                LatLng location = new LatLng(Double.parseDouble(lat),
                                                        Double.parseDouble(lng));

                                                mMap.addMarker(new MarkerOptions().position(location)
                                                        .title("Order destination")
                                                        .icon(BitmapDescriptorFactory.defaultMarker()));


                                                //Set Shipper Location
                                                shippingOrder.child(Common.currentKey)
                                                        .addListenerForSingleValueEvent(new ValueEventListener() {
                                                            @Override
                                                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                                ShippingInfo shippingInfo = dataSnapshot.getValue(ShippingInfo.class);

                                                                LatLng shipperLocation = new LatLng(shippingInfo.getLat(),shippingInfo.getLng());

                                                                if (shipperMarker == null)
                                                                {
                                                                    shipperMarker = mMap.addMarker(
                                                                            new MarkerOptions()
                                                                                    .position(shipperLocation)
                                                                                    .title("Shipper #"+shippingInfo.getOrderId())
                                                                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
                                                                    );
                                                                }else
                                                                {
                                                                    shipperMarker.setPosition(shipperLocation);
                                                                }

                                                                //Update Camera
                                                                CameraPosition cameraPosition = new CameraPosition.Builder()
                                                                        .target(shipperLocation)
                                                                        .zoom(16f)
                                                                        .bearing(0)
                                                                        .tilt(45)
                                                                        .build();
                                                                mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

                                                                //draw Routes
                                                                if (polyline != null)
                                                                    polyline.remove();
                                                                mService.getDirections(shipperLocation.latitude+","+shipperLocation.longitude
                                                                        ,currentorder.getLatLng())
                                                                        .enqueue(new Callback<String>() {
                                                                            @Override
                                                                            public void onResponse(Call<String> call, Response<String> response) {
                                                                                new ParserTask().execute(response.body().toString());
                                                                            }

                                                                            @Override
                                                                            public void onFailure(Call<String> call, Throwable t) {

                                                                            }
                                                                        });

                                                            }

                                                            @Override
                                                            public void onCancelled(@NonNull DatabaseError databaseError) {

                                                            }
                                                        });

                                            } catch (JSONException e) {
                                                e.printStackTrace();
                                            }
                                        }

                                        @Override
                                        public void onFailure(Call<String> call, Throwable t) {

                                        }
                                    });
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }

    @Override
    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
        trackLocation();
    }

    @Override
    public void onCancelled(@NonNull DatabaseError databaseError) {

    }

    private class ParserTask extends AsyncTask<String,Integer,List<List<HashMap<String,String>>>> {
        AlertDialog mDialog = new SpotsDialog(TrackShipper.this);

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mDialog.show();
            mDialog.setMessage("Please Wait...");
        }

        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... strings) {
            JSONObject jObject;
            List<List<HashMap<String,String>>> routes =null;
            try{
                jObject = new JSONObject(strings[0]);
                DirectionJSONParser parser = new DirectionJSONParser();

                routes =parser.parse(jObject);

            } catch (JSONException e) {
                e.printStackTrace();
            }
            return routes;
        }


        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> lists) {
            mDialog.dismiss();

            ArrayList points = null;
            PolylineOptions lineOptions = null;

            for(int i=0;i<lists.size();i++)
            {
                points = new ArrayList();
                lineOptions = new PolylineOptions();

                List<HashMap<String,String>> path = lists.get(i);

                for(int j =0;j<path.size();j++)
                {
                    HashMap<String,String> point = path.get(j);

                    double lat = Double.parseDouble(point.get("lat"));
                    double lng = Double.parseDouble(point.get("lng"));

                    LatLng position = new LatLng(lat,lng);

                    points.add(position);
                }
                lineOptions.addAll(points);
                lineOptions.width(12);
                lineOptions.color(Color.BLUE);
                lineOptions.geodesic(true);
            }
            polyline =  mMap.addPolyline(lineOptions);
        }
    }
}
