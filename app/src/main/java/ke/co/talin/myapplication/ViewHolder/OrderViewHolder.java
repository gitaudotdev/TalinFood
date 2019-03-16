package ke.co.talin.myapplication.ViewHolder;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import ke.co.talin.myapplication.Interface.ItemClickListener;
import ke.co.talin.myapplication.R;

public class OrderViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{

    public TextView txtOrderId,txtOrderStatus,txtOrderPhone,txtOrderAddress;
    public ImageView btn_delete;

    private ItemClickListener mItemClickListener;

    public OrderViewHolder(@NonNull View itemView) {
        super(itemView);
        txtOrderAddress = itemView.findViewById(R.id.order_address);
        txtOrderId = itemView.findViewById(R.id.order_id);
        txtOrderStatus = itemView.findViewById(R.id.order_status);
        txtOrderPhone = itemView.findViewById(R.id.order_phone);

        btn_delete = itemView.findViewById(R.id.btn_delete);

        itemView.setOnClickListener(this);
    }

    public void setItemClickListener(ItemClickListener itemClickListener) {
        mItemClickListener = itemClickListener;
    }

    @Override
    public void onClick(View view) {
        mItemClickListener.onClick(view,getAdapterPosition(),false);
    }
}
