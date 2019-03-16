package ke.co.talin.myapplication.ViewHolder;

import android.content.Context;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.amulyakhare.textdrawable.TextDrawable;
import com.cepheuen.elegantnumberbutton.view.ElegantNumberButton;
import com.squareup.picasso.Picasso;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import ke.co.talin.myapplication.Cart;
import ke.co.talin.myapplication.Common.Common;
import ke.co.talin.myapplication.Database.Database;
import ke.co.talin.myapplication.Interface.ItemClickListener;
import ke.co.talin.myapplication.Model.Order;
import ke.co.talin.myapplication.R;

public class CartAdapter extends RecyclerView.Adapter<CartViewHolder>  {

    private List<Order> mList = new ArrayList<>();
    private Cart cart;


    public CartAdapter(List<Order> list, Cart cart) {
        mList = list;
        this.cart = cart;
    }

    @NonNull
    @Override
    public CartViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        LayoutInflater inflater = LayoutInflater.from(cart);
        View itemView = inflater.inflate(R.layout.cart_layout,viewGroup,false);
        return new CartViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull CartViewHolder holder, final int position) {
//        TextDrawable drawable = TextDrawable.builder()
//                .buildRound(""+mList.get(position).getQuantity(), Color.RED);
//        holder.image_count.setImageDrawable(drawable);

        //Fix Elegant number Button not showing added qty
        Picasso.with(cart.getBaseContext())
                .load(mList.get(position).getImage())
                .resize(70,70) //70dp
                .centerCrop()
                .into(holder.cart_image);

        holder.qty_count.setNumber(mList.get(position).getQuantity());
        holder.qty_count.setOnValueChangeListener(new ElegantNumberButton.OnValueChangeListener() {
            @Override
            public void onValueChange(ElegantNumberButton view, int oldValue, int newValue) {
                Order order = mList.get(position);
                order.setQuantity(String.valueOf(newValue));
                new Database(cart).updateCart(order);

                //Update txtTotal
//                cart.txt_total
                //Calculate totalPrice
                int total = 0;
                List<Order> orders = new Database(cart).getCarts(Common.currentUser.getPhone());
                for(Order item:orders)
                    total+=(Integer.parseInt(order.getPrice()))*(Integer.parseInt(item.getQuantity()));
                Locale locale = new Locale("en","KE");
                NumberFormat fmt = NumberFormat.getCurrencyInstance(locale);

                cart.txt_total.setText(fmt.format(total));
            }
        });

        Locale locale = new Locale("en","KE");
        NumberFormat fmt = NumberFormat.getCurrencyInstance(locale);
        int price = (Integer.parseInt(mList.get(position).getPrice()))*(Integer.parseInt(mList.get(position).getQuantity()));
        holder.txt_price.setText(fmt.format(price));

        holder.txt_cart_name.setText(mList.get(position).getProductName());
    }

    @Override
    public int getItemCount() {
        return mList.size();
    }

    public Order getItem(int postion)
    {
        return mList.get(postion);
    }

    public void removeItem(int position) {
        mList.remove(position);
        notifyItemRemoved(position);
    }

    public void restoreItem(Order item,int position){
        mList.add(position,item);
        notifyItemInserted(position);

    }
}

