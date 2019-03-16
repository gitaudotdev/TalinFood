package ke.co.talin.myapplication;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.graphics.Typeface;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.FacebookSdk;
import com.facebook.accountkit.Account;
import com.facebook.accountkit.AccountKit;
import com.facebook.accountkit.AccountKitCallback;
import com.facebook.accountkit.AccountKitError;
import com.facebook.accountkit.AccountKitLoginResult;
import com.facebook.accountkit.ui.AccountKitActivity;
import com.facebook.accountkit.ui.AccountKitConfiguration;
import com.facebook.accountkit.ui.LoginType;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import dmax.dialog.SpotsDialog;
import io.paperdb.Paper;
import ke.co.talin.myapplication.Common.Common;
import ke.co.talin.myapplication.Model.User;
import uk.co.chrisjenx.calligraphy.CalligraphyConfig;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE = 9991;
    Button btnSignIn;
    TextView tvSlogan;

    FirebaseDatabase database;
    DatabaseReference users;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CalligraphyConfig.initDefault(new CalligraphyConfig.Builder()
                .setDefaultFontPath("fonts/restaurant_font.otf")
                .setFontAttrId(R.attr.fontPath)
                .build());
        FacebookSdk.sdkInitialize(getApplicationContext());
        AccountKit.initialize(this);
        setContentView(R.layout.activity_main);

        //Init Firebase
        database = FirebaseDatabase.getInstance();
        users = database.getReference("User");


        btnSignIn = findViewById(R.id.btn_continue);

        tvSlogan = findViewById(R.id.tvSlogan);
        Typeface face = Typeface.createFromAsset(getAssets(),"fonts/NABILA.TTF");
        tvSlogan.setTypeface(face);


//        printKeyHash();
        

        btnSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startLogin();
            }
        });

        //Check Session Facebook
        if (AccountKit.getCurrentAccessToken() !=null)
        {
            //Create Dialog
            final AlertDialog waitingDialog = new SpotsDialog(this);
            waitingDialog.show();
            waitingDialog.setMessage("Please Wait");
            waitingDialog.setCancelable(false);


            AccountKit.getCurrentAccount(new AccountKitCallback<Account>() {
                @Override
                public void onSuccess(Account account) {
                    //Copy Code from where User exists
                    users.child(account.getPhoneNumber().toString())
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                    User localUser = dataSnapshot.getValue(User.class);

                                    //copy from sign_in activity
                                    Intent intent = new Intent(MainActivity.this, RestaurantList.class);
                                    Common.currentUser = localUser;
                                    startActivity(intent);
                                    //dismiss dialog
                                    waitingDialog.dismiss();
                                    finish();

                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                }
                            });
                }

                @Override
                public void onError(AccountKitError accountKitError) {

                }
            });
        }
    }

    private void startLogin() {
        Intent intent = new Intent(MainActivity.this, AccountKitActivity.class);
        AccountKitConfiguration.AccountKitConfigurationBuilder configBuilder =
                new AccountKitConfiguration.AccountKitConfigurationBuilder(LoginType.PHONE,
                        AccountKitActivity.ResponseType.TOKEN);
        intent.putExtra(AccountKitActivity.ACCOUNT_KIT_ACTIVITY_CONFIGURATION,configBuilder.build());
        startActivityForResult(intent,REQUEST_CODE);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE)
        {
            AccountKitLoginResult result = data.getParcelableExtra(AccountKitLoginResult.RESULT_KEY);
            if (result.getError() != null)
            {
                Toast.makeText(this, ""+result.getError().getErrorType().getMessage(), Toast.LENGTH_SHORT).show();
                return;
            }else if(result.wasCancelled())
            {
                Toast.makeText(this, "Cancel", Toast.LENGTH_SHORT).show();
                return;
            }else
            {
                if (result.getAccessToken() != null)
                {
                    //Show Dialog
                    final AlertDialog waitingDialog = new SpotsDialog(this);
                    waitingDialog.show();
                    waitingDialog.setMessage("Please Wait");
                    waitingDialog.setCancelable(false);

                    //Get Current Phone
                    AccountKit.getCurrentAccount(new AccountKitCallback<Account>() {
                        @Override
                        public void onSuccess(Account account) {
                            final String userPhone = account.getPhoneNumber().toString();

                            //Check if It Exists on Firebase Users
                            users.orderByKey().equalTo(userPhone)
                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                            if (!dataSnapshot.child(userPhone).exists()) //if it does not exist
                                            {

                                                //Create new User and Login
                                                User newUser = new User();
                                                newUser.setPhone(userPhone);
                                                newUser.setName("");
                                                newUser.setBalance(String.valueOf(0.0));

                                                //Add to Firebase
                                                users.child(userPhone)
                                                        .setValue(newUser)
                                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                            @Override
                                                            public void onComplete(@NonNull Task<Void> task) {
                                                                if (task.isSuccessful())
                                                                    Toast.makeText(MainActivity.this, "User Registered Successfully!!..", Toast.LENGTH_SHORT).show();

                                                                //Login
                                                                users.child(userPhone)
                                                                        .addListenerForSingleValueEvent(new ValueEventListener() {
                                                                            @Override
                                                                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                                                                User localUser = dataSnapshot.getValue(User.class);

                                                                                //copy from sign_in activity
                                                                                Intent intent = new Intent(MainActivity.this, RestaurantList.class);
                                                                                Common.currentUser = localUser;
                                                                                startActivity(intent);
                                                                                //dismiss dialog
                                                                                waitingDialog.dismiss();
                                                                                finish();

                                                                            }

                                                                            @Override
                                                                            public void onCancelled(@NonNull DatabaseError databaseError) {

                                                                            }
                                                                        });
                                                            }
                                                        });

                                            }else //if it exists
                                            {
                                                
                                                //Login
                                                users.child(userPhone)
                                                        .addListenerForSingleValueEvent(new ValueEventListener() {
                                                            @Override
                                                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                                                User localUser = dataSnapshot.getValue(User.class);

                                                                //copy from sign_in activity
                                                                Intent intent = new Intent(MainActivity.this, RestaurantList.class);
                                                                Common.currentUser = localUser;
                                                                startActivity(intent);
                                                                //dismiss dialog
                                                                waitingDialog.dismiss();
                                                                finish();

                                                            }

                                                            @Override
                                                            public void onCancelled(@NonNull DatabaseError databaseError) {

                                                            }
                                                        });
                                            }
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError databaseError) {

                                        }
                                    });
                        }

                        @Override
                        public void onError(AccountKitError accountKitError) {
                            Toast.makeText(MainActivity.this, ""+accountKitError.getErrorType().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });



                }

            }
        }
        
    }
}
