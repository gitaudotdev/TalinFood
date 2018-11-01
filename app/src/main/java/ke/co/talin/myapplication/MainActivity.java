package ke.co.talin.myapplication;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.graphics.Typeface;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import io.paperdb.Paper;
import ke.co.talin.myapplication.Common.Common;
import ke.co.talin.myapplication.Model.User;

public class MainActivity extends AppCompatActivity {

    Button btnSignIn,btnSignUp;
    TextView tvSlogan;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        btnSignUp = findViewById(R.id.btnSignUp);
        btnSignIn = findViewById(R.id.btnSignIn);
        tvSlogan = findViewById(R.id.tvSlogan);
        Typeface face = Typeface.createFromAsset(getAssets(),"fonts/NABILA.TTF");
        tvSlogan.setTypeface(face);

        //Init Paper
        Paper.init(this);


        printKeyHash();
        

        btnSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this,SignIn.class);
                startActivity(intent);
            }
        });

        btnSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this,SignUp.class);
                startActivity(intent);
            }
        });

        //Check remember
        String user = Paper.book().read(Common.USER_KEY);
        String pwd = Paper.book().read(Common.PWD_KEY);
        if(user!=null && pwd !=null)
        {
            if(!user.isEmpty() && !pwd.isEmpty())
                login(user,pwd);
        }
    }

    private void printKeyHash() {
//        try{
//            PackageInfo info = getPackageManager().getPackageInfo("ke.co.talin.myapplication", );
//
//        }

    }

    private void login(final String phone, final String pwd) {

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        final DatabaseReference user_tbl = database.getReference("User");

        if (Common.isConnectedToInternet(getBaseContext())) {

            final ProgressDialog dialog = new ProgressDialog(MainActivity.this);
            dialog.setMessage("Please Wait...");
            dialog.show();

            user_tbl.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    //check if user exists on database
                    if (dataSnapshot.child(phone).exists()) {
                        //Getting user info from database
                        dialog.dismiss();

                        User user = dataSnapshot.child(phone).getValue(User.class);
                        user.setPhone(phone); //set phone
                        if (user.getPassword().equals(pwd)) {
                            Intent intent = new Intent(MainActivity.this, HomeActivity.class);
                            Common.currentUser = user;
                            startActivity(intent);
                            finish();

                        } else {
                            Toast.makeText(MainActivity.this, "Wrong Password", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        dialog.dismiss();
                        Toast.makeText(MainActivity.this, "User Does not exist!!", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }
        else
        {
            Toast.makeText(MainActivity.this, "Please Check Your Internet Connection!!..", Toast.LENGTH_SHORT).show();
            return;
        }
    }
}
