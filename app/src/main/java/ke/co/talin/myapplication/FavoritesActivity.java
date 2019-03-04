package ke.co.talin.myapplication;

import android.graphics.Color;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.widget.RelativeLayout;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

import ke.co.talin.myapplication.Common.Common;
import ke.co.talin.myapplication.Database.Database;
import ke.co.talin.myapplication.Helper.RecyclerItemTouchHelper;
import ke.co.talin.myapplication.Interface.RecyclerItemTouchHelperListener;
import ke.co.talin.myapplication.Model.Favorites;
import ke.co.talin.myapplication.Model.Order;
import ke.co.talin.myapplication.ViewHolder.FavoritesAdapter;
import ke.co.talin.myapplication.ViewHolder.FavoritesViewHolder;

public class FavoritesActivity extends AppCompatActivity implements RecyclerItemTouchHelperListener {

    RecyclerView mRecyclerView;
    RecyclerView.LayoutManager mLayoutManager;

    FavoritesAdapter adapter;

    RelativeLayout rootLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites);

        rootLayout = findViewById(R.id.roots);

        mRecyclerView = findViewById(R.id.fave_recycler);
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);
        LayoutAnimationController controller = AnimationUtils.loadLayoutAnimation(mRecyclerView.getContext(),
                R.anim.fade_in_left);
        mRecyclerView.setLayoutAnimation(controller);


        //Swipe to Delete
        ItemTouchHelper.SimpleCallback itemTouchCallback = new RecyclerItemTouchHelper(0,ItemTouchHelper.LEFT,this);
        new ItemTouchHelper(itemTouchCallback).attachToRecyclerView(mRecyclerView);

        loadFavorites();
    }

    private void loadFavorites() {
        adapter = new FavoritesAdapter(this,new Database(this).getAllFavorites(Common.currentUser.getPhone()));
        mRecyclerView.setAdapter(adapter);
    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction, int position) {
        if (viewHolder instanceof FavoritesViewHolder)
        {
            String name = ((FavoritesAdapter)mRecyclerView.getAdapter()).getItem(position).getFoodName();

            final Favorites deleteItem = ((FavoritesAdapter)mRecyclerView.getAdapter()).getItem(viewHolder.getAdapterPosition());
            final int deleteIndex = viewHolder.getAdapterPosition();

            adapter.removeItem(viewHolder.getAdapterPosition());
            new Database(getBaseContext()).removeFromFavorites(deleteItem.getFoodId(), Common.currentUser.getPhone());

            Snackbar snackbar = Snackbar.make(rootLayout,name+"removed From Favorites",Snackbar.LENGTH_LONG);
            snackbar.setAction("UNDO", new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    adapter.restoreItem(deleteItem,deleteIndex);
                    new Database(getBaseContext()).addToFavorites(deleteItem);


                }
            });
            snackbar.setActionTextColor(Color.YELLOW);
            snackbar.show();
        }
    }
}
