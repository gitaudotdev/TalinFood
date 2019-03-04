package ke.co.talin.myapplication.ViewHolder;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.RatingBar;
import android.widget.TextView;

import ke.co.talin.myapplication.R;

public class CommentsViewHolder extends RecyclerView.ViewHolder {
    public TextView txtComments,txtUserPhone;
    public RatingBar ratings;

    public CommentsViewHolder(@NonNull View itemView) {
        super(itemView);

        ratings = itemView.findViewById(R.id.ratings);
        txtComments = itemView.findViewById(R.id.txtComments);
        txtUserPhone = itemView.findViewById(R.id.txtUserPhone);


    }
}
