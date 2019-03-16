package ke.co.talin.myapplication;


import android.app.AlertDialog;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.squareup.picasso.Picasso;

import ke.co.talin.myapplication.Common.Common;
import ke.co.talin.myapplication.Interface.ItemClickListener;
import ke.co.talin.myapplication.Model.Category;
import ke.co.talin.myapplication.Model.Restaurant;
import ke.co.talin.myapplication.ViewHolder.MenuViewHolder;
import ke.co.talin.myapplication.ViewHolder.RestaurantViewHolder;

public class RestaurantList extends AppCompatActivity {

    AlertDialog waitingDialog;
    RecyclerView recyclerView;
    SwipeRefreshLayout refreshLayout;

    FirebaseDatabase database;
    DatabaseReference restaurants;

    FirebaseRecyclerOptions<Restaurant> options = new FirebaseRecyclerOptions.Builder<Restaurant>()
            .setQuery(restaurants.child("Restaurants"), Restaurant.class)
            .build();

    FirebaseRecyclerAdapter<Restaurant,RestaurantViewHolder> adapter = new FirebaseRecyclerAdapter<Restaurant, RestaurantViewHolder>(options) {
        @Override
        protected void onBindViewHolder(@NonNull RestaurantViewHolder holder, int position, @NonNull Restaurant model) {
            holder.txtmenu.setText(model.getName());
            Picasso.with(getBaseContext())
                    .load(model.getImage())
                    .into(holder.images);
            final Restaurant clickItem = model;
            holder.setItemClickListener(new ItemClickListener() {
                @Override
                public void onClick(View view, int position, boolean isLongClick) {
                    Intent foodList = new Intent(RestaurantList.this,HomeActivity.class);
                    //When user selects a restaurant, we will save restaurant id to select category  of the restaurant
                    Common.retaurantSelected = adapter.getRef(position).getKey();
                    startActivity(foodList);
                }
            });
        }


        @NonNull
        @Override
        public RestaurantViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int position) {
            View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.restaurant_item,parent,false);
            return new RestaurantViewHolder(itemView);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_restaurant_list);

        //View
        refreshLayout = findViewById(R.id.swipe_layout);

        refreshLayout.setColorSchemeResources(R.color.colorPrimary,
                android.R.color.holo_green_dark,
                android.R.color.holo_orange_dark,
                android.R.color.holo_blue_dark);
        refreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if(Common.isConnectedToInternet(getBaseContext()))
                    loadRestaurant();
                else
                {
                    Toast.makeText(getBaseContext(), "Please Check Your Connection..", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
        });

        //Default Load for first time
        refreshLayout.post(new Runnable() {
            @Override
            public void run() {
                if(Common.isConnectedToInternet(getBaseContext()))
                    loadRestaurant();
                else
                {
                    Toast.makeText(getBaseContext(), "Please Check Your Connection..", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
        });

        //Load Menu
        recyclerView = findViewById(R.id.recycler_restaurants);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

    }

    private void loadRestaurant() {
        adapter.startListening();
        recyclerView.setAdapter(adapter);
        refreshLayout.setRefreshing(false);

        //Animation
//        recyclerView.getAdapter().notifyDataSetChanged();

    }

    @Override
    protected void onStop() {
        super.onStop();
        adapter.stopListening();

    }
}
