package ke.co.talin.myapplication.ViewHolder;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import ke.co.talin.myapplication.Interface.ItemClickListener;
import ke.co.talin.myapplication.R;

public class FoodViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{

    public TextView txtfood,txtprice;
    public ImageView images,fave,share_btn,cart;

    private ItemClickListener itemClickListener;

    public FoodViewHolder(@NonNull View itemView) {
        super(itemView);

        txtprice = itemView.findViewById(R.id.food_price);
        txtfood = itemView.findViewById(R.id.food_name);
        images = itemView.findViewById(R.id.food_image);
        fave = itemView.findViewById(R.id.fav);
        share_btn = itemView.findViewById(R.id.btnShare);
        cart = itemView.findViewById(R.id.btn_quick_cart);

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
