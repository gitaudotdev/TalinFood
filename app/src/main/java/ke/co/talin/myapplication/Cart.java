package ke.co.talin.myapplication;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.rengwuxian.materialedittext.MaterialEditText;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import ke.co.talin.myapplication.Common.Common;
import ke.co.talin.myapplication.Database.Database;
import ke.co.talin.myapplication.Model.MyResponse;
import ke.co.talin.myapplication.Model.Notification;
import ke.co.talin.myapplication.Model.Order;
import ke.co.talin.myapplication.Model.Request;
import ke.co.talin.myapplication.Model.Sender;
import ke.co.talin.myapplication.Model.Token;
import ke.co.talin.myapplication.Remote.APIService;
import ke.co.talin.myapplication.ViewHolder.CartAdapter;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class Cart extends AppCompatActivity {

    RecyclerView mRecyclerView;
    RecyclerView.LayoutManager mLayoutManager;

    FirebaseDatabase mDatabase;
    DatabaseReference requests;

    TextView txt_total;
    Button btnOrder;

    List<Order> cart = new ArrayList<>();
    CartAdapter mCartAdapter;

    APIService mService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);


        mDatabase = FirebaseDatabase.getInstance();
        requests = mDatabase.getReference("Requests");

        //Init Service
        mService = Common.getFCMService();

        //Init
        mRecyclerView = findViewById(R.id.cart_recycler);
        mRecyclerView.setHasFixedSize(true);
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);

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
        new Database(this).cleanCart();
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

        final MaterialEditText edtAddress = order_address_comment.findViewById(R.id.edtAddress);
        final MaterialEditText edtComment = order_address_comment.findViewById(R.id.edtComment);


        alertdialog.setView(order_address_comment);
        alertdialog.setIcon(R.drawable.ic_shopping_cart_black_24dp);


        alertdialog.setPositiveButton("YES", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                //Create New Request
                Request request = new Request(
                        Common.currentUser.getPhone(),
                        Common.currentUser.getName(),
                        edtAddress.getText().toString(),
                        txt_total.getText().toString(),
                        "0",
                        edtComment.getText().toString(),
                        cart
                );
                //submit to Firebase
                //we will use system.CurrentMilli to key
                String order_number = String.valueOf(System.currentTimeMillis());
                requests.child(String.valueOf(System.currentTimeMillis()))
                        .setValue(request);
                //Delete Cart
                new Database(getBaseContext()).cleanCart();

                sendNotificationOrder(order_number);

//
            }
        });
        alertdialog.setNegativeButton("NO", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
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
                for (DataSnapshot snapshot : dataSnapshot.getChildren())
                {
                    Token serverToken = snapshot.getValue(Token.class);

                    //Create raw payload
                    Notification notification = new Notification("CodeBender","You Have a new Order"+order_number);
                    Sender content = new Sender(serverToken.getToken(),notification);

                    mService.sendNotification(content)
                            .enqueue(new Callback<MyResponse>() {
                                @Override
                                public void onResponse(Call<MyResponse> call, Response<MyResponse> response) {
                                    if(response.body().success==1)
                                    {
                                        Toast.makeText(Cart.this, "Thank you, Your Order has been placed", Toast.LENGTH_SHORT).show();
                                        finish();
                                    }
                                    else
                                    {
                                        Toast.makeText(Cart.this, "Failed!!..", Toast.LENGTH_SHORT).show();
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
        cart = new Database(this).getCarts();
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
}
