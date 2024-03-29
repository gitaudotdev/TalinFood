package ke.co.talin.myapplication;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.andremion.counterfab.CounterFab;
import com.cepheuen.elegantnumberbutton.view.ElegantNumberButton;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;
import com.stepstone.apprating.AppRatingDialog;
import com.stepstone.apprating.listener.RatingDialogListener;

import java.util.Arrays;

import info.hoang8f.widget.FButton;
import ke.co.talin.myapplication.Common.Common;
import ke.co.talin.myapplication.Database.Database;
import ke.co.talin.myapplication.Model.Food;
import ke.co.talin.myapplication.Model.Order;
import ke.co.talin.myapplication.Model.Rating;
import uk.co.chrisjenx.calligraphy.CalligraphyConfig;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class FoodDetail extends AppCompatActivity implements RatingDialogListener{

    TextView food_name,food_price,food_description;
    ImageView food_image;
    CollapsingToolbarLayout mCollapsingToolbarLayout;
    FloatingActionButton btnRate;
    CounterFab btn_cart;
    ElegantNumberButton mNumberButton;
    RatingBar ratingBar;

    FButton comments;

    String foodId="";

    DatabaseReference foods;
    FirebaseDatabase database;
    DatabaseReference ratingDb;

    Food currentfood;

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
        setContentView(R.layout.activity_food_detail);

        database = FirebaseDatabase.getInstance();
        foods = database.getReference("Restaurants").child(Common.retaurantSelected).child("detail").child("Foods");
        ratingDb = database.getReference("Ratings");

        //InitViews
        mNumberButton = findViewById(R.id.number);
        food_description = findViewById(R.id.food_description);
        food_price = findViewById(R.id.food_price);
        food_name = findViewById(R.id.food_nameTv);
        food_image = findViewById(R.id.img_food);

        btn_cart = findViewById(R.id.btnCart);
        btnRate = findViewById(R.id.btn_rating);
        ratingBar = findViewById(R.id.ratingBar);

        comments = findViewById(R.id.btnShowComments);

        mCollapsingToolbarLayout = findViewById(R.id.collapsing);
        mCollapsingToolbarLayout.setExpandedTitleTextAppearance(R.style.ExpandedAppbar);
        mCollapsingToolbarLayout.setCollapsedTitleTextAppearance(R.style.CollapsedAppbar);


        //Event
        comments.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(FoodDetail.this,ViewComments.class);
                intent.putExtra(Common.INTENT_FOOD_ID,foodId);
                startActivity(intent);
            }
        });

        //Get Food Id from Intent
        if(getIntent() !=null)
            foodId = getIntent().getStringExtra("FoodId");
        if(!foodId.isEmpty())
        {
            if(Common.isConnectedToInternet(getBaseContext())) {
                getFoodDetail(foodId);
                getFoodRating(foodId);
            }
            else 
            {
                Toast.makeText(this, "Please Check Your Connection!!..", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        btn_cart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new Database(getBaseContext()).addToCart(new Order(
                        Common.currentUser.getPhone(),
                        foodId,
                        currentfood.getName(),
                        mNumberButton.getNumber(),
                        currentfood.getPrice(),
                        currentfood.getDiscount(),
                        currentfood.getImage()
                ));
                Toast.makeText(FoodDetail.this, "Added to Cart", Toast.LENGTH_SHORT).show();
            }
        });

        btn_cart.setCount(new Database(this).getCountCart(Common.currentUser.getPhone()));

        btnRate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showRatingDialog();
            }
        });
    }

    private void getFoodRating(String foodId) {

        Query foodRating = ratingDb.orderByChild("foodId").equalTo(foodId);

        foodRating.addValueEventListener(new ValueEventListener() {
            int count=0,sum=0;
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for(DataSnapshot snapshot: dataSnapshot.getChildren())
                {
                    Rating item = snapshot.getValue(Rating.class);
                    sum+=Integer.parseInt(item.getRatingValue());
                    count++;
                }if(count!=0)
                {
                    float average= sum/count;
                    ratingBar.setRating(average);
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    private void showRatingDialog() {
        new AppRatingDialog.Builder()
                .setPositiveButtonText("Submit")
                .setNegativeButtonText("Cancel")
                .setNoteDescriptions(Arrays.asList("Very Bad","Not Good","Quite Ok","Very Good","Excellent"))
                .setDefaultRating(1)
                .setTitle("Rate This Food")
                .setDescription("Please select stars and give your Feedback")
                .setTitleTextColor(R.color.colorPrimary)
                .setDescriptionTextColor(R.color.colorPrimary)
                .setHint("Please write your Comments Here")
                .setHintTextColor(R.color.colorAccent)
                .setCommentTextColor(android.R.color.white)
                .setCommentBackgroundColor(R.color.colorPrimaryDark)
                .setWindowAnimation(R.style.RatingDialogDialogFadeAnim)
                .create(FoodDetail.this)
                .show();
    }

    private void getFoodDetail(String foodId) {
        foods.child(foodId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                currentfood = dataSnapshot.getValue(Food.class);

                //Set Image
                Picasso.with(getBaseContext()).load(currentfood.getImage()).into(food_image);

                mCollapsingToolbarLayout.setTitle(currentfood.getName());

                food_price.setText(currentfood.getPrice());

                food_description.setText(currentfood.getDescription());

                food_name.setText(currentfood.getName());

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    @Override
    public void onNegativeButtonClicked() {

    }

    @Override
    public void onPositiveButtonClicked(int value, String comments) {
        //Get Rating and upload to firebase
        final Rating rating = new Rating(Common.currentUser.getPhone(),
                foodId,
                String.valueOf(value),comments);

        //Fix User can rate multiple Times
        ratingDb.push()
                .setValue(rating)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        Toast.makeText(FoodDetail.this, "Thank you For Your Feedback", Toast.LENGTH_SHORT).show();

                    }
                });
/*
        ratingDb.child(Common.currentUser.getPhone())
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if(dataSnapshot.child(Common.currentUser.getPhone()).exists())
                        {
                            //Remove old value
                            ratingDb.child(Common.currentUser.getPhone()).removeValue();
                            //update new Value
                            ratingDb.child(Common.currentUser.getPhone()).setValue(rating);
                        }
                        else
                        {
                            //update new Value
                            ratingDb.child(Common.currentUser.getPhone()).setValue(rating);
                        }
                        Toast.makeText(FoodDetail.this, "Thank you For Your Feedback", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
*/
    }
}
