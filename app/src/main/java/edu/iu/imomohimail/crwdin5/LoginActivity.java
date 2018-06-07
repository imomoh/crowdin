package edu.iu.imomohimail.crwdin5;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.parse.LogInCallback;
import com.parse.ParseAnalytics;
import com.parse.ParseAnonymousUtils;
import com.parse.ParseException;
import com.parse.ParseUser;
import com.parse.SaveCallback;

public class LoginActivity extends AppCompatActivity {

    public Button LogInButton;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        getSupportActionBar().hide();

        if (ParseUser.getCurrentUser()!=null){


            Intent intent = new Intent(getApplicationContext(),MapsActivity.class);
            startActivity(intent);
        }

        LogInButton = (Button)findViewById(R.id.anonymous_login_button);
        LogInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                ParseAnonymousUtils.logIn(new LogInCallback() {
                    @Override
                    public void done(ParseUser user, ParseException e) {
                        if (e==null){
                            Log.i("Info", "Anonymous login successful");
                        }else {
                            Log.i("Info", "Anonymous login fail");
                        }
                    }
                });


                Intent intent = new Intent(getApplicationContext(),MapsActivity.class);
                startActivity(intent);
            }
        });


        ParseAnalytics.trackAppOpenedInBackground(getIntent());
    }

}
