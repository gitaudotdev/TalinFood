package ke.co.talin.myapplication.Service;


import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;

import ke.co.talin.myapplication.Common.Common;
import ke.co.talin.myapplication.Model.Token;


public class MyFirebaseIdService extends FirebaseInstanceIdService {

    @Override
    public void onTokenRefresh() {
        super.onTokenRefresh();
        String tokenRefreshed = FirebaseInstanceId.getInstance().getToken();
        updateTokenToFirebase(tokenRefreshed);
    }

    private void updateTokenToFirebase(String tokenRefreshed) {
        if(Common.currentUser !=null)
        {
            FirebaseDatabase db = FirebaseDatabase.getInstance();
            DatabaseReference tokens = db.getReference("Tokens");
            Token token = new Token(tokenRefreshed,false); //false because this token is sent from Client
            tokens.child(Common.currentUser.getPhone()).setValue(token);

        }

    }
}
