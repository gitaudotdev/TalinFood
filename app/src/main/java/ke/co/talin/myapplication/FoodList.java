package ke.co.talin.myapplication;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.facebook.CallbackManager;
import com.facebook.share.model.SharePhoto;
import com.facebook.share.model.SharePhotoContent;
import com.facebook.share.widget.ShareDialog;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.mancj.materialsearchbar.MaterialSearchBar;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.util.ArrayList;
import java.util.List;

import ke.co.talin.myapplication.Common.Common;
import ke.co.talin.myapplication.Database.Database;
import ke.co.talin.myapplication.Interface.ItemClickListener;
import ke.co.talin.myapplication.Model.Favorites;
import ke.co.talin.myapplication.Model.Food;
import ke.co.talin.myapplication.Model.Order;
import ke.co.talin.myapplication.ViewHolder.FoodViewHolder;
import uk.co.chrisjenx.calligraphy.CalligraphyConfig;

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

    //FavoritesActivity
    Database localDb;

    //Facebook Share
    CallbackManager callbackManager;
    ShareDialog shareDialog;


    //Create Target From Picasso
    Target target = new Target() {
        @Override
        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
            //Create Photo From Bitmap
            SharePhoto photo = new SharePhoto.Builder()
                    .setBitmap(bitmap)
                    .build();
            if(ShareDialog.canShow(SharePhotoContent.class)){
                SharePhotoContent content = new SharePhotoContent.Builder()
                        .addPhoto(photo)
                        .build();
                shareDialog.show(content);
            }
        }

        @Override
        public void onBitmapFailed(Drawable errorDrawable) {

        }



        @Override
        public void onPrepareLoad(Drawable placeHolderDrawable) {

        }
    };

    SwipeRefreshLayout swipeRefreshLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CalligraphyConfig.initDefault(new CalligraphyConfig.Builder()
                .setDefaultFontPath("fonts/restaurant_font.otf")
                .setFontAttrId(R.attr.fontPath)
                .build());
        setContentView(R.layout.activity_food_list);

        //Init FaceBook
        callbackManager = CallbackManager.Factory.create();
        shareDialog = new ShareDialog(this);

        swipeRefreshLayout = findViewById(R.id.swipe_refresh);

        mDatabase = FirebaseDatabase.getInstance();
        foodsList = mDatabase.getReference("Restaurants").child(Common.retaurantSelected).child("detail").child("Foods");;

        //Local DB
        localDb = new Database(this);

        //view
        swipeRefreshLayout = findViewById(R.id.swipe_refresh);
        swipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary,
                android.R.color.holo_green_dark,
                android.R.color.holo_orange_dark,
                android.R.color.holo_blue_dark);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                //Get Intent Here
                if(getIntent() != null)
                    categoryId = getIntent().getStringExtra("CategoryId");
                if(!categoryId.isEmpty() && categoryId != null)
                {
                    if(Common.isConnectedToInternet(getBaseContext()))
                        loadFoodsList(categoryId);
                    else
                    {
                        Toast.makeText(FoodList.this, "Please Check Your Connection!!..", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
            }
        });

        swipeRefreshLayout.post(new Runnable() {
            @Override
            public void run() {
                //Get Intent Here
                if(getIntent() != null)
                    categoryId = getIntent().getStringExtra("CategoryId");
                if(!categoryId.isEmpty() && categoryId != null)
                {
                    if(Common.isConnectedToInternet(getBaseContext()))
                        loadFoodsList(categoryId);
                    else
                    {
                        Toast.makeText(FoodList.this, "Please Check Your Connection!!..", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
                //Search function needs category so i'll paste the search code here
                //Search
                mSearchBar = findViewById(R.id.searchBar);
                mSearchBar.setHint("Enter Your Food");
                //mSearchBar.setSpeechMode(false);
                loadSuggestionList();
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
        });

        mRecyclerView = findViewById(R.id.foods_recycler);
        mRecyclerView.setHasFixedSize(true);
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);

    }

    @Override
    protected void onResume() {
        super.onResume();
        //Fix click back on Food Detail and get no item in Food List
        if(mAdapter != null)
            mAdapter.startListening();
    }

    private void startSearch(CharSequence text) {
        //Create query by name
        Query searchByName = foodsList.orderByChild("name").equalTo(text.toString());
        //Create Options with Query
        FirebaseRecyclerOptions<Food> foodOptions = new FirebaseRecyclerOptions.Builder<Food>()
                .setQuery(searchByName,Food.class)
                .build();

        searchAdapter = new FirebaseRecyclerAdapter<Food, FoodViewHolder>(foodOptions) {
            @Override
            protected void onBindViewHolder(@NonNull FoodViewHolder viewHolder, int position, @NonNull Food model) {
                viewHolder.txtfood.setText(model.getName());
                Picasso.with(getBaseContext()).load(model.getImage())
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

            @NonNull
            @Override
            public FoodViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
                View itemView = LayoutInflater.from(viewGroup.getContext())
                        .inflate(R.layout.food_item,viewGroup,false);
                return new FoodViewHolder(itemView);
            }
        };
        searchAdapter.startListening();
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

                        mSearchBar.setLastSuggestions(suggestList);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }

    private void loadFoodsList(String categoryId) {
        //Create query by Category Id
        Query searchByCat = foodsList.orderByChild("menuId").equalTo(categoryId);
        //Create Options with Query
        FirebaseRecyclerOptions<Food> menuOptions = new FirebaseRecyclerOptions.Builder<Food>()
                .setQuery(searchByCat,Food.class)
                .build();

        mAdapter = new FirebaseRecyclerAdapter<Food, FoodViewHolder>(menuOptions) {
            @Override
            protected void onBindViewHolder(@NonNull final FoodViewHolder holder, final int position, @NonNull final Food model) {
                holder.txtfood.setText(model.getName());
                holder.txtprice.setText(String.format("KES %s",model.getPrice().toString()));
                Picasso.with(getBaseContext()).load(model.getImage())
                        .into(holder.images);

                //Quick Cart

                holder.cart.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        boolean isExists = new Database(getBaseContext()).checkFoodExists(mAdapter.getRef(position).getKey(),Common.currentUser.getPhone());

                        if (!isExists) {

                            new Database(getBaseContext()).addToCart(new Order(
                                    Common.currentUser.getPhone(),
                                    mAdapter.getRef(position).getKey(),
                                    model.getName(),
                                    "1",
                                    model.getPrice(),
                                    model.getDiscount(),
                                    model.getImage()
                            ));

                        } else {
                            new Database(getBaseContext()).increaseCart(Common.currentUser.getPhone(), mAdapter.getRef(position).getKey());
                        }

                        Toast.makeText(FoodList.this, "Added to Cart", Toast.LENGTH_SHORT).show();

                    }
                });


                //Add FavoritesActivity
                if(localDb.isFavorites(mAdapter.getRef(position).getKey(),Common.currentUser.getPhone()))
                    holder.fave.setImageResource(R.drawable.ic_favorite_black_24dp);

                //Click to Share
                holder.share_btn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Picasso.with(getBaseContext())
                                .load(model.getImage())
                                .into(target);

                        Toast.makeText(FoodList.this, "Clicked", Toast.LENGTH_SHORT).show();
                    }
                });

                //click to change state of FavoritesActivity
                holder.fave.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                        Favorites favorites = new Favorites();
                        favorites.setFoodId(mAdapter.getRef(position).getKey());
                        favorites.setFoodName(model.getName());
                        favorites.setFoodDescription(model.getDescription());
                        favorites.setFoodDiscount(model.getDiscount());
                        favorites.setFoodImage(model.getImage());
                        favorites.setFoodMenuId(model.getMenuId());
                        favorites.setUserPhone(Common.currentUser.getPhone());
                        favorites.setFoodPrice(model.getPrice());

                        if(!localDb.isFavorites(mAdapter.getRef(position).getKey(),Common.currentUser.getPhone()))
                        {
                            localDb.addToFavorites(favorites);
                            holder.fave.setImageResource(R.drawable.ic_favorite_black_24dp);
                            Toast.makeText(FoodList.this, ""+model.getName()+"was Added To FavoritesActivity", Toast.LENGTH_SHORT).show();

                        }
                        else
                        {
                            localDb.removeFromFavorites(mAdapter.getRef(position).getKey(),Common.currentUser.getPhone());
                            holder.fave.setImageResource(R.drawable.ic_favorite_border_black_24dp);
                            Toast.makeText(FoodList.this, ""+model.getName()+"was removed from FavoritesActivity", Toast.LENGTH_SHORT).show();


                        }
                    }
                });

                final Food local = model;
                holder.setItemClickListener(new ItemClickListener() {
                    @Override
                    public void onClick(View view, int position, boolean isLongClick) {
                        //Start New Activity
                        Intent foodDetail = new Intent(FoodList.this,FoodDetail.class);
                        foodDetail.putExtra("FoodId",mAdapter.getRef(position).getKey()); //send food Id to new Activity
                        startActivity(foodDetail);
                    }
                });
            }


            @NonNull
            @Override
            public FoodViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
                View itemView = LayoutInflater.from(viewGroup.getContext())
                        .inflate(R.layout.food_item,viewGroup,false);
                return new FoodViewHolder(itemView);
            }
        };
        mAdapter.startListening();
        mRecyclerView.setAdapter(mAdapter);
        swipeRefreshLayout.setRefreshing(false);

    }

    @Override
    protected void onStop() {
        if (mAdapter != null)
            mAdapter.stopListening();
        if (searchAdapter != null)
            searchAdapter.stopListening();
        super.onStop();

    }
}
