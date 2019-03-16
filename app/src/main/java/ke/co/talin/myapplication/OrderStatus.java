package ke.co.talin.myapplication;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;

import ke.co.talin.myapplication.Common.Common;
import ke.co.talin.myapplication.Interface.ItemClickListener;
import ke.co.talin.myapplication.Model.Request;
import ke.co.talin.myapplication.ViewHolder.OrderViewHolder;
import uk.co.chrisjenx.calligraphy.CalligraphyConfig;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class OrderStatus extends AppCompatActivity {

    public RecyclerView mRecyclerView;
    public RecyclerView.LayoutManager mLayoutManager;

    FirebaseRecyclerAdapter<Request,OrderViewHolder> adapter;

    FirebaseDatabase mDatabase;
    DatabaseReference requests;

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
        setContentView(R.layout.activity_order_status);

        //Firebase
        mDatabase = FirebaseDatabase.getInstance();
        requests = mDatabase.getReference("Requests");

        mRecyclerView = findViewById(R.id.listOrders);
        mRecyclerView.setHasFixedSize(true);
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);

        //if we start order status activity from home activity
        // we wont get any extras , so we will load orders by phone from common
        if(getIntent()!=null)
            loadOrders(Common.currentUser.getPhone());
        else
        {
            if(getIntent().getStringExtra("userPhone")== null)
                loadOrders(Common.currentUser.getPhone());
            else
                loadOrders(getIntent().getStringExtra("userPhone"));
        }
    }

    private void loadOrders(String phone) {
        Query getOrderByUser = requests.orderByChild("phone").equalTo(phone);

        FirebaseRecyclerOptions<Request> orderOptions =new FirebaseRecyclerOptions.Builder<Request>()
                .setQuery(getOrderByUser,Request.class)
                .build();


        adapter = new FirebaseRecyclerAdapter<Request, OrderViewHolder>(orderOptions) {
            @Override
            protected void onBindViewHolder(@NonNull OrderViewHolder viewHolder, final int position, @NonNull Request model) {
                viewHolder.txtOrderId.setText(adapter.getRef(position).getKey());
                viewHolder.txtOrderStatus.setText(Common.convertCodeToStatus(model.getStatus()));
                viewHolder.txtOrderAddress.setText(model.getAddress());
                viewHolder.txtOrderPhone.setText(model.getPhone());
                viewHolder.btn_delete.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (adapter.getItem(position).getStatus().equals("0"))
                            deleteOrder(adapter.getRef(position).getKey());
                        else
                            Toast.makeText(OrderStatus.this, "You Cannot Delete This Order", Toast.LENGTH_SHORT).show();
                    }
                });

                viewHolder.setItemClickListener(new ItemClickListener() {
                    @Override
                    public void onClick(View view, int position, boolean isLongClick) {
                        Common.currentKey = adapter.getRef(position).getKey();
                        startActivity(new Intent(OrderStatus.this,TrackShipper.class));
                    }
                });
            }

            @NonNull
            @Override
            public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
                View itemView = LayoutInflater.from(viewGroup.getContext())
                        .inflate(R.layout.order_layout,viewGroup,false);
                return new OrderViewHolder(itemView);
            }
        };
        adapter.startListening();
        mRecyclerView.setAdapter(adapter);
    }

    private void deleteOrder(final String key) {
        requests.child(key)
                .removeValue()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(OrderStatus.this, new StringBuilder("Order")
                                .append(key)
                                .append("has Been Deleted"), Toast.LENGTH_SHORT).show();
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(OrderStatus.this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        adapter.stopListening();
    }
}
