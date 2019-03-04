package ke.co.talin.myapplication;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.androidstudy.daraja.Daraja;
import com.androidstudy.daraja.DarajaListener;
import com.androidstudy.daraja.model.AccessToken;
import com.androidstudy.daraja.model.LNMExpress;
import com.androidstudy.daraja.model.LNMResult;
import com.androidstudy.daraja.util.TransactionType;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.rengwuxian.materialedittext.MaterialEditText;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import ke.co.talin.myapplication.Common.Common;
import ke.co.talin.myapplication.Database.Database;
import ke.co.talin.myapplication.Helper.RecyclerItemTouchHelper;
import ke.co.talin.myapplication.Interface.RecyclerItemTouchHelperListener;
import ke.co.talin.myapplication.Model.DataMessage;
import ke.co.talin.myapplication.Model.MyResponse;
import ke.co.talin.myapplication.Model.Order;
import ke.co.talin.myapplication.Model.Request;
import ke.co.talin.myapplication.Model.Token;
import ke.co.talin.myapplication.Model.User;
import ke.co.talin.myapplication.Remote.APIService;
import ke.co.talin.myapplication.Remote.IGoogleService;
import ke.co.talin.myapplication.ViewHolder.CartAdapter;
import ke.co.talin.myapplication.ViewHolder.CartViewHolder;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import uk.co.chrisjenx.calligraphy.CalligraphyConfig;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class Cart extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks,GoogleApiClient.OnConnectionFailedListener,
        LocationListener, RecyclerItemTouchHelperListener {

    private static final int LOCATION_REQUEST_CODE = 9999;
    private static final int PLAY_SERVICES_REQ_CODE = 8888;
    RecyclerView mRecyclerView;
    RecyclerView.LayoutManager mLayoutManager;

    FirebaseDatabase mDatabase;
    DatabaseReference requests;

    public TextView txt_total;
    Button btnOrder;

    String address;
    String comment;

    List<Order> cart = new ArrayList<>();
    CartAdapter mCartAdapter;

    Place shippingAddress;

    //Location
    private LocationRequest mlocationRequest;
    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;

    private static final int UPDATE_INTERVAL = 5000;
    private static final int FASTEST_INTERVAL = 3000;
    private static final int DISPLACEMENT = 10;

    //Declare Google MAp API
    IGoogleService mGoogleService;
    APIService mService;

    RelativeLayout rootLayout;


    //Mpesa
    Daraja daraja;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CalligraphyConfig.initDefault(new CalligraphyConfig.Builder()
                .setDefaultFontPath("fonts/restaurant_font.otf")
                .setFontAttrId(R.attr.fontPath)
                .build());
        setContentView(R.layout.activity_cart);

        //Init
        mGoogleService = Common.getGoogleMapService();

        rootLayout = findViewById(R.id.rootLayout);

        //Runtime Permission
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        )
        {
            ActivityCompat.requestPermissions(this,new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            },LOCATION_REQUEST_CODE);
        }else{
            if(checkPlayServices()) //if device has play services
            {
                buildGoogleApiClient();
                createLocationRequest();
            }
        }

        mDatabase = FirebaseDatabase.getInstance();
        requests = mDatabase.getReference("Requests");

        //Init Service
        mService = Common.getFCMService();

        //Init
        mRecyclerView = findViewById(R.id.cart_recycler);
        mRecyclerView.setHasFixedSize(true);
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);

        //Swipe to Delete
        ItemTouchHelper.SimpleCallback itemTouchCallback = new RecyclerItemTouchHelper(0,ItemTouchHelper.LEFT,this);
        new ItemTouchHelper(itemTouchCallback).attachToRecyclerView(mRecyclerView);

        txt_total = findViewById(R.id.txt_total);
        btnOrder = findViewById(R.id.btnPlaceOrder);

        btnOrder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(cart.size()>0)
                    showAlertDialog();
                else
                    Toast.makeText(Cart.this, "Your Cart is Empty!!!..", Toast.LENGTH_SHORT).show();

            }
        });

        LoadListFood();

    }

    private void createLocationRequest() {
        mlocationRequest = new LocationRequest();
        mlocationRequest.setInterval(UPDATE_INTERVAL);
        mlocationRequest.setFastestInterval(FASTEST_INTERVAL);
        mlocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mlocationRequest.setSmallestDisplacement(DISPLACEMENT);
    }

    private synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        mGoogleApiClient.connect();
    }

    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if(resultCode != ConnectionResult.SUCCESS)
        {
            if(GooglePlayServicesUtil.isUserRecoverableError(resultCode))
                GooglePlayServicesUtil.getErrorDialog(resultCode,this,PLAY_SERVICES_REQ_CODE).show();
            else{
                Toast.makeText(this, "This device is not Supported", Toast.LENGTH_SHORT).show();
                finish();
            }
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode)
        {
            case LOCATION_REQUEST_CODE:
            {
                if(grantResults.length >0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    if(checkPlayServices()) //if device has play services
                    {
                        buildGoogleApiClient();
                        createLocationRequest();
                    }
                }
            }
            break;
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if(item.getTitle().equals(Common.DELETE))
            deleteCart(item.getOrder());
        return true;
    }

    private void deleteCart(int position) {
        // remove item from List<Order> by position
        cart.remove(position);
        //After that, we will delete all old data from SQLite
        new Database(this).cleanCart(Common.currentUser.getPhone());
        //And final we will update new Data from List<Order> to SQLite
        for(Order item:cart)
            new Database(this).addToCart(item);
        //Refresh
        LoadListFood();
    }

    private void showAlertDialog() {
        AlertDialog.Builder alertdialog = new AlertDialog.Builder(Cart.this);
        alertdialog.setTitle("One More Step!");
        alertdialog.setMessage("Enter your address: ");

        LayoutInflater inflater = this.getLayoutInflater();
        View order_address_comment = inflater.inflate(R.layout.order_address_comment,null);

//        final MaterialEditText edtAddress = order_address_comment.findViewById(R.id.edtAddress);
        final PlaceAutocompleteFragment edtAddress = (PlaceAutocompleteFragment)getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment);

        //Hide search icon before fragment
        edtAddress.getView().findViewById(R.id.place_autocomplete_search_button).setVisibility(View.GONE);

        //Set Hint for Autocomplete Edit Text
        ((EditText)edtAddress.getView().findViewById(R.id.place_autocomplete_search_input))
                .setHint("Enter your Address");

        //Set Text Size
        ((EditText)edtAddress.getView().findViewById(R.id.place_autocomplete_search_input))
                .setTextSize(14);

        //Get Address from Place AutoComplete
        edtAddress.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                shippingAddress = place;
            }

            @Override
            public void onError(Status status) {
                Log.e("ERROR",status.getStatusMessage());
            }
        });

        final MaterialEditText edtComment = order_address_comment.findViewById(R.id.edtComment);

        //Radio
        final RadioButton rdiShipToAddress = order_address_comment.findViewById(R.id.rdShipToAddress);
        final RadioButton rdiHmeAddress = order_address_comment.findViewById(R.id.rdHomeAddress);
        final RadioButton rdiMpesa = order_address_comment.findViewById(R.id.rdiMpesa);
        final RadioButton rdiCOD = order_address_comment.findViewById(R.id.rdCOd);
        final RadioButton rdiBalance = order_address_comment.findViewById(R.id.rdiBalance);

        //Event for Radio Buttons
        rdiHmeAddress.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked)
                {
                    if (Common.currentUser.getHomeAddress() != null || !TextUtils.isEmpty(Common.currentUser.getHomeAddress())) {
                        address = Common.currentUser.getHomeAddress();
                        ((EditText)edtAddress.getView().findViewById(R.id.place_autocomplete_search_input))
                                .setText(address);
                    }
                    else {
                        Toast.makeText(Cart.this, "Please Update Your Home Address", Toast.LENGTH_SHORT).show();
                    }

                }
            }
        });

        rdiShipToAddress.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                //Ship to this Address features
                if(isChecked) //isChecked == true
                {
                    mGoogleService.getAddressName(String.format("https://maps.googleapis.com/maps/api/geocode/json?latlng=%f,%f&sensor=false",mLastLocation.getLatitude(),mLastLocation.getLongitude()))
                            .enqueue(new Callback<String>() {
                                @Override
                                public void onResponse(Call<String> call, Response<String> response) {
                                    //If fetch API is okay
                                    try {
                                        JSONObject jsonObject = new JSONObject(response.body().toString());
                                        JSONArray responseArray  = jsonObject.getJSONArray("results");
                                        JSONObject firstObject = responseArray.getJSONObject(0);

                                        address = firstObject.getString("formatted_address");


                                        ((EditText)edtAddress.getView().findViewById(R.id.place_autocomplete_search_input))
                                                .setText(address);

                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }

                                }

                                @Override
                                public void onFailure(Call<String> call, Throwable t) {
                                    Toast.makeText(Cart.this, ""+t.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            });
                }
            }
        });

        alertdialog.setView(order_address_comment);
        alertdialog.setIcon(R.drawable.ic_shopping_cart_black_24dp);


        alertdialog.setPositiveButton("YES", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

                //Add Check condition Here
                //If user selects address from Places Fragment just use it
                //If user selects Ship To This Address, get address from location and use it
                //if user selects Home Address,get Home Address from profile and use it
                if(!rdiShipToAddress.isChecked() && !rdiHmeAddress.isChecked()) {
                    //if none of the radio buttons is selected
                    if(shippingAddress !=null)
                        address = shippingAddress.getAddress().toString();
                    else
                    {
                        Toast.makeText(Cart.this, "Please Enter an Address or Select an Option", Toast.LENGTH_SHORT).show();

                        //Fix Crash Fragment
                        getFragmentManager().beginTransaction()
                                .remove(getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment))
                                .commit();

                        return;
                    }
                }

                if(TextUtils.isEmpty(address))
                {
                    Toast.makeText(Cart.this, "Please Enter an Address or Select an Option", Toast.LENGTH_SHORT).show();

                    //Fix Crash Fragment
                    getFragmentManager().beginTransaction()
                            .remove(getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment))
                            .commit();

                    return;
                }
                comment = edtComment.getText().toString();

                //Check Payment
                if(!rdiCOD.isChecked() && !rdiCOD.isChecked() && !rdiBalance.isChecked()) //if both cod and paypal and Balance is not checked
                {
                    Toast.makeText(Cart.this, "Please  Select an Payment Option", Toast.LENGTH_SHORT).show();

                    //Fix Crash Fragment
                    getFragmentManager().beginTransaction()
                            .remove(getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment))
                            .commit();

                    return;
                }else if(!rdiMpesa.isChecked() && !rdiMpesa.isChecked())
                {
                    daraja = Daraja.with(Common.Consumer_KEY, Common.Consumer_SECRET, new DarajaListener<AccessToken>() {
                        @Override
                        public void onResult(@NonNull AccessToken accessToken) {
//                        Log.i(Cart.this.getClass().getSimpleName(), accessToken.getAccess_token());
//                        Toast.makeText(Cart.this, "TOKEN : " + accessToken.getAccess_token(), Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onError(String error) {
                            Toast.makeText(Cart.this, ""+error, Toast.LENGTH_SHORT).show();
                        }
                    });



                    final LNMExpress lnmExpress = new LNMExpress(
                            "174379",
                            "bfb279f9aa9bdbcf158e97dd71a467cd2e0c893059b10f78e6b72ada1ed2c919",  //https://developer.safaricom.co.ke/test_credentials
                            TransactionType.CustomerBuyGoodsOnline, // TransactionType.CustomerPayBillOnline  <- Apply any of these two
                            txt_total.getText().toString(),
                            "254708374149",
                            "174379",
                            Common.currentUser.getPhone(),
                            "http://mycallbackurl.com/checkout.php",
                            "001ABC",
                            "Goods Payment"
                    );

                    daraja.requestMPESAExpress(
                            lnmExpress, new DarajaListener<LNMResult>() {
                                @Override
                                public void onResult(@NonNull LNMResult lnmResult) {

//                                if(lnmResult.ResponseDescription.equals("success"))
//                                {
                                    //Create New Request
                                    Request request = new Request(
                                            Common.currentUser.getPhone(),
                                            Common.currentUser.getName(),
                                            address,
                                            txt_total.getText().toString(),
                                            "0",
                                            comment,
                                            "Mpesa",
                                            lnmResult.ResponseDescription,
                                            String.format("%s,%s",shippingAddress.getLatLng().latitude,shippingAddress.getLatLng().longitude),
                                            cart
                                    );
                                    //submit to Firebase
                                    //we will use system.CurrentMilli as unique identifier
                                    String order_number = String.valueOf(System.currentTimeMillis());
                                    requests.child(String.valueOf(System.currentTimeMillis()))
                                            .setValue(request);

                                    //Delete Cart
                                    new Database(getBaseContext()).cleanCart(Common.currentUser.getPhone());

                                    sendNotificationOrder(order_number);

                                    Toast.makeText(Cart.this, "Thank You ,For Placing Your Order", Toast.LENGTH_SHORT).show();
                                    finish();
//


                                }

                                @Override
                                public void onError(String error) {
                                    Toast.makeText(Cart.this, ""+error, Toast.LENGTH_SHORT).show();
                                }
                            }
                    );
                }else if (rdiCOD.isChecked())
                {

                    //Create New Request
                    Request request = new Request(
                            Common.currentUser.getPhone(),
                            Common.currentUser.getName(),
                            address,
                            txt_total.getText().toString(),
                            "0",
                            comment,
                            "COD",
                            "Unpaid",
                            String.format("%s,%s",mLastLocation.getLatitude(),mLastLocation.getLongitude()), //coordinates wen user makes order
                            cart
                    );
                    //submit to Firebase
                    //we will use system.CurrentMilli as unique identifier
                    String order_number = String.valueOf(System.currentTimeMillis());
                    requests.child(String.valueOf(System.currentTimeMillis()))
                            .setValue(request);

                    //Delete Cart
                    new Database(getBaseContext()).cleanCart(Common.currentUser.getPhone());

                    sendNotificationOrder(order_number);

                    Toast.makeText(Cart.this, "Thank You ,For Placing Your Order", Toast.LENGTH_SHORT).show();
                    finish();
                }else if (rdiBalance.isChecked())
                {
                    double amount = 0;
                    //First we will get Total Price from txtTotal
                    try {
                        amount = Common.formatCurrency(txt_total.getText().toString(),Locale.US).doubleValue();
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }

                    //After we receive tota; of this order,just compare with user balance
                    if (Double.parseDouble(Common.currentUser.getBalance().toString()) >= amount)
                    {
                        //Create New Request
                        Request request = new Request(
                                Common.currentUser.getPhone(),
                                Common.currentUser.getName(),
                                address,
                                txt_total.getText().toString(),
                                "0",
                                comment,
                                "Talin's Balance",
                                "Paid",
                                String.format("%s,%s",mLastLocation.getLatitude(),mLastLocation.getLongitude()), //coordinates wen user makes order
                                cart
                        );
                        //submit to Firebase
                        //we will use system.CurrentMilli as unique identifier
                        final String order_number = String.valueOf(System.currentTimeMillis());
                        requests.child(String.valueOf(System.currentTimeMillis()))
                                .setValue(request);

                        //Delete Cart
                        new Database(getBaseContext()).cleanCart(Common.currentUser.getPhone());

                        //Update Balance
                        double balance =Double.parseDouble(Common.currentUser.getBalance().toString())  - amount;
                        Map<String,Object> update_balance = new HashMap<>();
                        update_balance.put("balance",balance);

                        FirebaseDatabase.getInstance()
                                .getReference("User")
                                .child(Common.currentUser.getPhone())
                                .updateChildren(update_balance)
                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        //If Task is successful
                                        if (task.isSuccessful())
                                        {
                                            //Refresh User
                                            FirebaseDatabase.getInstance()
                                                    .getReference("User")
                                                    .child(Common.currentUser.getPhone())
                                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                                        @Override
                                                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                            Common.currentUser = dataSnapshot.getValue(User.class);
                                                            //Send Order to Server
                                                            sendNotificationOrder(order_number);

                                                        }

                                                        @Override
                                                        public void onCancelled(@NonNull DatabaseError databaseError) {

                                                        }
                                                    });
                                        }

                                    }
                                });

                        Toast.makeText(Cart.this, "Thank You ,For Placing Your Order", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                    else
                    {
                        Toast.makeText(Cart.this, "Your Balance is not Enough Please choose other Payment", Toast.LENGTH_SHORT).show();
                    }
                }


                //Remove Fragment
                getFragmentManager().beginTransaction()
                        .remove(getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment))
                        .commit();

//
            }
        });
        alertdialog.setNegativeButton("NO", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();

                //Remove Fragment
                getFragmentManager().beginTransaction()
                        .remove(getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment))
                        .commit();
            }
        });

        alertdialog.show();

    }

    private void sendNotificationOrder(final String order_number) {
        DatabaseReference tokens = FirebaseDatabase.getInstance().getReference("Tokens");
        Query data = tokens.orderByChild("isServerToken").equalTo(true); //get all nodes with isServerToken true
        data.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Token serverToken = null;
                for (DataSnapshot snapshot : dataSnapshot.getChildren())
                {
                    serverToken = snapshot.getValue(Token.class);

                    //Create raw payload
//                    Notification notification = new Notification("CodeBender","You Have a new Order"+order_number);
//                    Sender content = new Sender(serverToken.getToken(),notification);

                    Map<String,String> dataSend = new HashMap<>();
                    dataSend.put("title","CodeBender");
                    dataSend.put("message","You Have a new Order"+order_number);
                    DataMessage dataMessage = new DataMessage(serverToken.getToken(),dataSend);



                    mService.sendNotification(dataMessage)
                            .enqueue(new Callback<MyResponse>() {
                                @Override
                                public void onResponse(Call<MyResponse> call, Response<MyResponse> response) {
                                    //Only run when get result
                                    if(response.code()==200) {
                                        if (response.body().success == 1) {
                                            Toast.makeText(Cart.this, "Thank you, Your Order has been placed", Toast.LENGTH_SHORT).show();
                                            finish();
                                        } else {
                                            Toast.makeText(Cart.this, "Failed!!..", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                }

                                @Override
                                public void onFailure(Call<MyResponse> call, Throwable t) {
                                    Log.e("ERROR",t.getMessage());
                                }
                            });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void LoadListFood() {
        cart = new Database(this).getCarts(Common.currentUser.getPhone());
        mCartAdapter = new CartAdapter(cart,this);
        mCartAdapter.notifyDataSetChanged();
        mRecyclerView.setAdapter(mCartAdapter);

        //Calculate totalPrice
        int total = 0;
        for(Order order:cart)
            total+=(Integer.parseInt(order.getPrice()))*(Integer.parseInt(order.getQuantity()));
        Locale locale = new Locale("en","KE");
        NumberFormat fmt = NumberFormat.getCurrencyInstance(locale);

        txt_total.setText(fmt.format(total));

    }

    private void startLocationUpdates() {
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
        )
        {
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient,mlocationRequest,this);
    }

    private void displayLocation() {
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
        )
        {
            return;
        }
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if(mLastLocation != null)
        {
            Log.d("LOCATION","YOUR LOCATION: "+mLastLocation.getLatitude()+","+mLastLocation.getLongitude());
        }
        else
        {
            Log.d("LOCATION","COULD NOT GET YOUR LOCATION");
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        displayLocation();
        startLocationUpdates();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
        mLastLocation = location;
        displayLocation();
    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction, int position) {
        if (viewHolder instanceof CartViewHolder)
        {
            String name =((CartAdapter)mRecyclerView.getAdapter()).getItem(viewHolder.getAdapterPosition()).getProductName();

            final Order deleteItem =((CartAdapter) mRecyclerView.getAdapter()).getItem(viewHolder.getAdapterPosition());

            final int deleteIndex = viewHolder.getAdapterPosition();

            mCartAdapter.removeItem(deleteIndex);

            new Database(getBaseContext()).removeFromCart(deleteItem.getProductId(),Common.currentUser.getPhone());

            //calculate Total Price
            int total = 0;
            List<Order> orders = new Database(getBaseContext()).getCarts(Common.currentUser.getPhone());
            for(Order item:orders)
                total+=(Integer.parseInt(item.getPrice()))*(Integer.parseInt(item.getQuantity()));
            Locale locale = new Locale("en","KE");
            NumberFormat fmt = NumberFormat.getCurrencyInstance(locale);

            txt_total.setText(fmt.format(total));

            //Make SnackBar
            Snackbar snackbar = Snackbar.make(rootLayout,name+"removed From Cart",Snackbar.LENGTH_LONG);
            snackbar.setAction("UNDO", new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mCartAdapter.restoreItem(deleteItem,deleteIndex);
                    new Database(getBaseContext()).addToCart(deleteItem);

                    //calculate Total Price
                    int total = 0;
                    List<Order> orders = new Database(getBaseContext()).getCarts(Common.currentUser.getPhone());
                    for(Order item:orders)
                        total+=(Integer.parseInt(item.getPrice()))*(Integer.parseInt(item.getQuantity()));
                    Locale locale = new Locale("en","KE");
                    NumberFormat fmt = NumberFormat.getCurrencyInstance(locale);

                    txt_total.setText(fmt.format(total));
                }
            });
            snackbar.setActionTextColor(Color.YELLOW);
            snackbar.show();
        }
    }
}
