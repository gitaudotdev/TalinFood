package ke.co.talin.myapplication;

import android.app.ProgressDialog;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.rengwuxian.materialedittext.MaterialEditText;

import ke.co.talin.myapplication.Common.Common;
import ke.co.talin.myapplication.Model.User;
import uk.co.chrisjenx.calligraphy.CalligraphyConfig;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class SignUp extends AppCompatActivity {

    MaterialEditText etphone,etpass,etname,edtSecureCode;
    Button btnreg;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        etphone = findViewById(R.id.etPhone);
        etpass = findViewById(R.id.etPass);
        etname = findViewById(R.id.etName);
        edtSecureCode = findViewById(R.id.etSecureCode);

        btnreg = findViewById(R.id.btnRegister);

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        final DatabaseReference user_tbl = database.getReference("User");


        btnreg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (Common.isConnectedToInternet(getBaseContext())) {

                    final ProgressDialog dialog = new ProgressDialog(SignUp.this);
                    dialog.setMessage("Please Wait...");
                    dialog.setCanceledOnTouchOutside(false);
                    dialog.show();


                    user_tbl.addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            //check if user is in database
                            if (dataSnapshot.child(etphone.getText().toString()).exists()) {
                                dialog.dismiss();
                                Toast.makeText(SignUp.this, "Phone Number is Already registered..", Toast.LENGTH_SHORT).show();
                            } else {
                                dialog.dismiss();

                                User user = new User(etname.getText().toString(), etpass.getText().toString(),edtSecureCode.getText().toString());
                                user.setBalance(0.0);

                                user_tbl.child(etphone.getText().toString()).setValue(user);

                                Toast.makeText(SignUp.this, "Sign Up Successful..", Toast.LENGTH_SHORT).show();
                                finish();

                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });
                }
                else
                {
                    Toast.makeText(SignUp.this, "Please Check Your Internet Connection!!...", Toast.LENGTH_SHORT).show();
                }
            }
        });

    }
}
