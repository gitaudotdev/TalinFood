package ke.co.talin.myapplication;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;

import ke.co.talin.myapplication.Common.Common;
import ke.co.talin.myapplication.Model.Rating;
import ke.co.talin.myapplication.ViewHolder.CommentsViewHolder;
import uk.co.chrisjenx.calligraphy.CalligraphyConfig;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class ViewComments extends AppCompatActivity {

    RecyclerView recyclerView;
    RecyclerView.LayoutManager layoutManager;

    FirebaseDatabase database;
    DatabaseReference ratingTbl;

    SwipeRefreshLayout refreshLayout;

    FirebaseRecyclerAdapter<Rating, CommentsViewHolder> adapter;

    String food_id ="";

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (adapter != null)
            adapter.stopListening();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CalligraphyConfig.initDefault(new CalligraphyConfig.Builder()
                .setDefaultFontPath("fonts/restaurant_font.otf")
                .setFontAttrId(R.attr.fontPath)
                .build());
        setContentView(R.layout.activity_view_comments);

        //Firebase
        database = FirebaseDatabase.getInstance();
        ratingTbl = database.getReference("Rating");

        recyclerView = findViewById(R.id.comments_recycler);
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        //SwipeLayout
        refreshLayout = findViewById(R.id.comment_swipe);
        refreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if(getIntent() != null)
                    food_id = getIntent().getStringExtra(Common.INTENT_FOOD_ID);
                if (!food_id.isEmpty() && food_id != null)
                {
                    //Create Request Query
                    Query query = ratingTbl.orderByChild("foodId").equalTo(food_id);

                    FirebaseRecyclerOptions<Rating> options = new FirebaseRecyclerOptions.Builder<Rating>()
                            .setQuery(query,Rating.class)
                            .build();

                    adapter = new FirebaseRecyclerAdapter<Rating, CommentsViewHolder>(options) {
                        @Override
                        protected void onBindViewHolder(@NonNull CommentsViewHolder holder, int position, @NonNull Rating model) {
                            holder.txtUserPhone.setText(model.getUserPhone());
                            holder.txtComments.setText(model.getComments());
                            holder.ratings.setRating(Float.parseFloat(model.getRatingValue()));
                        }

                        @NonNull
                        @Override
                        public CommentsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                            View view = LayoutInflater.from(parent.getContext())
                                    .inflate(R.layout.comment_layout,parent,false);
                            return new CommentsViewHolder(view);
                        }
                    };
                    
                    loadComment(food_id);

                }
            }
        });

        //Thread to load comment on first launch
        refreshLayout.post(new Runnable() {
            @Override
            public void run() {
                refreshLayout.setRefreshing(true);

                if(getIntent() != null)
                    food_id = getIntent().getStringExtra(Common.INTENT_FOOD_ID);
                if (!food_id.isEmpty() && food_id != null) {
                    //Create Request Query
                    Query query = ratingTbl.orderByChild("foodId").equalTo(food_id);

                    FirebaseRecyclerOptions<Rating> options = new FirebaseRecyclerOptions.Builder<Rating>()
                            .setQuery(query, Rating.class)
                            .build();

                    adapter = new FirebaseRecyclerAdapter<Rating, CommentsViewHolder>(options) {
                        @Override
                        protected void onBindViewHolder(@NonNull CommentsViewHolder holder, int position, @NonNull Rating model) {
                            holder.txtUserPhone.setText(model.getUserPhone());
                            holder.txtComments.setText(model.getComments());
                            holder.ratings.setRating(Float.parseFloat(model.getRatingValue()));
                        }

                        @NonNull
                        @Override
                        public CommentsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                            View view = LayoutInflater.from(parent.getContext())
                                    .inflate(R.layout.comment_layout, parent, false);
                            return new CommentsViewHolder(view);
                        }
                    };

                    loadComment(food_id);
                }
            }
        });

    }

    private void loadComment(String food_id) {
        adapter.startListening();

        recyclerView.setAdapter(adapter);
        refreshLayout.setRefreshing(false);
    }
}
