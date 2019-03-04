package ke.co.talin.myapplication.ViewHolder;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import ke.co.talin.myapplication.Interface.ItemClickListener;
import ke.co.talin.myapplication.R;

public class FavoritesViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{

    public TextView txtfood,txtprice;
    public ImageView images,cart;

    private ItemClickListener itemClickListener;

    public RelativeLayout view_background;
    public LinearLayout view_foreground;

    public FavoritesViewHolder(@NonNull View itemView) {
        super(itemView);

        txtprice = itemView.findViewById(R.id.food_price);
        txtfood = itemView.findViewById(R.id.food_name);
        images = itemView.findViewById(R.id.food_image);
        cart = itemView.findViewById(R.id.btn_quick_cart);

        view_foreground = itemView.findViewById(R.id.view_foreground);
        view_background = itemView.findViewById(R.id.view_background);

        itemView.setOnClickListener(this);
    }


    public void setItemClickListener(ItemClickListener itemClickListener) {
        this.itemClickListener = itemClickListener;
    }

    @Override
    public void onClick(View view) {
        itemClickListener.onClick(view,getAdapterPosition(),false);
    }
}

