package ke.co.talin.myapplication.ViewHolder;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import java.util.List;

import ke.co.talin.myapplication.Common.Common;
import ke.co.talin.myapplication.Database.Database;
import ke.co.talin.myapplication.FoodDetail;
import ke.co.talin.myapplication.FoodList;
import ke.co.talin.myapplication.Interface.ItemClickListener;
import ke.co.talin.myapplication.Model.Favorites;
import ke.co.talin.myapplication.Model.Food;
import ke.co.talin.myapplication.Model.Order;
import ke.co.talin.myapplication.R;

public class FavoritesAdapter extends RecyclerView.Adapter<FavoritesViewHolder> {

    private Context context;
    private List<Favorites> faveList;

    public FavoritesAdapter(Context context, List<Favorites> faveList) {
        this.context = context;
        this.faveList = faveList;
    }

    @NonNull
    @Override
    public FavoritesViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int position) {
        View itemView = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.favorites_item_layout,viewGroup,false);
        return new FavoritesViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull final FavoritesViewHolder holder, final int position) {
        holder.txtfood.setText(faveList.get(position).getFoodName());
        holder.txtprice.setText(String.format("KES %s",faveList.get(position).getFoodPrice().toString()));
        Picasso.get().load(faveList.get(position).getFoodImage())
                .into(holder.images);

        //Quick Cart

        holder.cart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean isExists = new Database(context).checkFoodExists(faveList.get(position).getFoodId(),Common.currentUser.getPhone());

                if (!isExists) {

                    new Database(context).addToCart(new Order(
                            Common.currentUser.getPhone(),
                            faveList.get(position).getFoodId(),
                            faveList.get(position).getFoodName(),
                            "1",
                           faveList.get(position).getFoodPrice(),
                            faveList.get(position).getFoodDiscount(),
                            faveList.get(position).getFoodImage()
                    ));

                } else {
                    new Database(context).increaseCart(Common.currentUser.getPhone(), faveList.get(position).getFoodId());
                }

                Toast.makeText(context, "Added to Cart", Toast.LENGTH_SHORT).show();

            }
        });




        final Favorites local = faveList.get(position);
        holder.setItemClickListener(new ItemClickListener() {
            @Override
            public void onClick(View view, int position, boolean isLongClick) {
                //Start New Activity
                Intent foodDetail = new Intent(context, FoodDetail.class);
                foodDetail.putExtra("FoodId",faveList.get(position).getFoodId()); //send food Id to new Activity
                context.startActivity(foodDetail);
            }
        });
    }

    @Override
    public int getItemCount() {
        return faveList.size();
    }

    public void removeItem(int position) {
        faveList.remove(position);
        notifyItemRemoved(position);
    }

    public void restoreItem(Favorites item,int position){
        faveList.add(position,item);
        notifyItemInserted(position);

    }

    public Favorites getItem(int position)
    {
        return faveList.get(position);
    }
}
