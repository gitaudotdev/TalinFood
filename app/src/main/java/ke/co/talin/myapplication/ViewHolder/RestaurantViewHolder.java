package ke.co.talin.myapplication.ViewHolder;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import ke.co.talin.myapplication.Interface.ItemClickListener;
import ke.co.talin.myapplication.R;

public class RestaurantViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{

    public TextView txtmenu;
    public ImageView images;

    private ItemClickListener itemClickListener;

    public RestaurantViewHolder(@NonNull View itemView) {
        super(itemView);

        txtmenu = itemView.findViewById(R.id.restaurant_name);
        images = itemView.findViewById(R.id.restaurant_image);

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

