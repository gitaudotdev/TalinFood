package ke.co.talin.myapplication;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.mancj.materialsearchbar.MaterialSearchBar;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import ke.co.talin.myapplication.Common.Common;
import ke.co.talin.myapplication.Database.Database;
import ke.co.talin.myapplication.Interface.ItemClickListener;
import ke.co.talin.myapplication.Model.Food;
import ke.co.talin.myapplication.ViewHolder.FoodViewHolder;

public class FoodList extends AppCompatActivity {


    RecyclerView mRecyclerView;
    RecyclerView.LayoutManager mLayoutManager;

    FirebaseDatabase mDatabase;
    DatabaseReference foodsList;

    String categoryId ="";

    FirebaseRecyclerAdapter<Food,FoodViewHolder> mAdapter;

    //Search Functionality
    FirebaseRecyclerAdapter<Food,FoodViewHolder> searchAdapter;
    List<String> suggestList = new ArrayList<>();
    MaterialSearchBar mSearchBar;

    //Favorites
    Database localDb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_food_list);

        mDatabase = FirebaseDatabase.getInstance();
        foodsList = mDatabase.getReference("Foods");

        //Local DB
        localDb = new Database(this);

        mRecyclerView = findViewById(R.id.foods_recycler);
        mRecyclerView.setHasFixedSize(true);
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);


        //Get Intent Here
        if(getIntent() != null)
            categoryId = getIntent().getStringExtra("categoryId");
        if(!categoryId.isEmpty())
        {
            if(Common.isConnectedToInternet(getBaseContext()))
                loadFoodsList(categoryId);
            else
            {
                Toast.makeText(this, "Please Check Your Connection!!..", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        //Search
        mSearchBar = findViewById(R.id.searchBar);
        mSearchBar.setHint("Enter Your Food");
//        mSearchBar.setSpeechMode(false);
        loadSuggestionList();
        mSearchBar.setLastSuggestions(suggestList);
        mSearchBar.setCardViewElevation(10);
        mSearchBar.addTextChangeListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                //when user types their text , we will change the suggestion list

                List<String> suggest = new ArrayList<String>();
                for(String search:suggestList) //loop in suggestion list
                {
                    if(search.toLowerCase().contains(mSearchBar.getText().toLowerCase()))
                        suggest.add(search);
                }
                mSearchBar.setLastSuggestions(suggest);
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
        mSearchBar.setOnSearchActionListener(new MaterialSearchBar.OnSearchActionListener() {
            @Override
            public void onSearchStateChanged(boolean enabled) {
                //when Search bar is closed
                //return original suggest adapter
                if(!enabled)
                    mRecyclerView.setAdapter(mAdapter);
            }

            @Override
            public void onSearchConfirmed(CharSequence text) {
                //when search is finished
                //show result
                startSearch(text);
            }

            @Override
            public void onButtonClicked(int buttonCode) {

            }
        });

    }

    private void startSearch(CharSequence text) {
        searchAdapter = new FirebaseRecyclerAdapter<Food, FoodViewHolder>(
                Food.class,
                R.layout.food_item,
                FoodViewHolder.class,
                foodsList.orderByChild("name").equalTo(text.toString())
        ) {
            @Override
            protected void populateViewHolder(FoodViewHolder viewHolder, Food model, int position) {
                viewHolder.txtfood.setText(model.getName());
                Picasso.get().load(model.getImage())
                        .into(viewHolder.images);

                final Food local = model;
                viewHolder.setItemClickListener(new ItemClickListener() {
                    @Override
                    public void onClick(View view, int position, boolean isLongClick) {
                        //Start New Activity
                        Intent foodDetail = new Intent(FoodList.this,FoodDetail.class);
                        foodDetail.putExtra("FoodId",searchAdapter.getRef(position).getKey()); //send food Id to new Activity
                        startActivity(foodDetail);
                    }
                });
            }
        };
        mRecyclerView.setAdapter(searchAdapter);
    }

    private void loadSuggestionList() {
        foodsList.orderByChild("menuId").equalTo(categoryId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for(DataSnapshot snapshot: dataSnapshot.getChildren())
                        {
                            Food item = snapshot.getValue(Food.class);
                            suggestList.add(item.getName()); //Add name of foods to suggestions list

                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }

    private void loadFoodsList(String categoryId) {
        mAdapter = new FirebaseRecyclerAdapter<Food, FoodViewHolder>(Food.class,R.layout.food_item,
                FoodViewHolder.class,
                foodsList.orderByChild("menuId").equalTo(categoryId)) //same as : Select * from Foods Where MenuId = ?

        {
            @Override
            protected void populateViewHolder(final FoodViewHolder viewHolder, final Food model, final int position) {
                viewHolder.txtfood.setText(model.getName());
                Picasso.get().load(model.getImage())
                        .into(viewHolder.images);

                //Add Favorites
                if(localDb.isFavorites(mAdapter.getRef(position).getKey()))
                    viewHolder.fave.setImageResource(R.drawable.ic_favorite_black_24dp);

                //click to change state of Favorites
                viewHolder.fave.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if(!localDb.isFavorites(mAdapter.getRef(position).getKey()))
                        {
                            localDb.addToFavorites(mAdapter.getRef(position).getKey());
                            viewHolder.fave.setImageResource(R.drawable.ic_favorite_black_24dp);
                            Toast.makeText(FoodList.this, ""+model.getName()+"was Added To Favorites", Toast.LENGTH_SHORT).show();

                        }
                        else
                        {
                            localDb.removeFromFavorites(mAdapter.getRef(position).getKey());
                            viewHolder.fave.setImageResource(R.drawable.ic_favorite_black_24dp);
                            Toast.makeText(FoodList.this, ""+model.getName()+"was removed from Favorites", Toast.LENGTH_SHORT).show();


                        }
                    }
                });

                final Food local = model;
                viewHolder.setItemClickListener(new ItemClickListener() {
                    @Override
                    public void onClick(View view, int position, boolean isLongClick) {
                      //Start New Activity
                        Intent foodDetail = new Intent(FoodList.this,FoodDetail.class);
                        foodDetail.putExtra("FoodId",mAdapter.getRef(position).getKey()); //send food Id to new Activity
                        startActivity(foodDetail);
                    }
                });
            }
        };
        //Set Adapter
        Log.d("TAG",""+mAdapter.getItemCount());
        mRecyclerView.setAdapter(mAdapter);

    }


}
