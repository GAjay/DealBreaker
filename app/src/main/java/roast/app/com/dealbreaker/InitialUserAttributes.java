package roast.app.com.dealbreaker;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import roast.app.com.dealbreaker.models.Age;
import roast.app.com.dealbreaker.models.UserLocation;
import roast.app.com.dealbreaker.util.Constants;

public class InitialUserAttributes extends AppCompatActivity {

    public String mFirstName, mLastName, mBirthDay, mGender, mSexualOrientation, mHeight, mLocation, locationUserValue;
    public Long mAge, height;
    private EditText firstNameUserText, lastNameUserText, heightUserText, sexualOrientationUserText;
    private EditText birthDateText;
    private TextView locationText;
    private RadioButton setMale, setFemale;
    RadioGroup maleFemaleGroup;
    Button sendUserValues, retrieveLocationButton;
    Context context;
    public String userName;
    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_intial_user_info);
        firstNameUserText=(EditText) findViewById(R.id.et_user_first_name);
        lastNameUserText=(EditText) findViewById(R.id.et_user_last_name);
        maleFemaleGroup = (RadioGroup) findViewById(R.id.radioGroupSex);
        setMale = (RadioButton) findViewById(R.id.radioButtonMale);
        setFemale = (RadioButton) findViewById(R.id.radioButtonFemale);
        heightUserText=(EditText) findViewById(R.id.et_user_height);
        sexualOrientationUserText=(EditText)findViewById(R.id.et_user_sexual_or);
        birthDateText = (EditText)findViewById(R.id.birthDateText);
        locationText = (TextView) findViewById(R.id.initialLocationTextValue);
        sendUserValues = (Button) findViewById(R.id.user_attribute_finished_button);
        retrieveLocationButton = (Button) findViewById(R.id.locationButton);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("User Info");

        // Grab intent
        Bundle arg = getIntent().getExtras();
        userName = arg.getString(getString(R.string.key_UserName));

        // Grab the users current location
        retrieveLocationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getUserLocation();
                locationText.setText(locationUserValue);
            }
        });

        // Run when the set button is pressed
        sendUserValues.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                grabData();
                Boolean validData = isDataValid();  // Returns true if the data is valid
                if(validData){
                    setData();
                    addUserToRoamingList();

                    Intent intent = new Intent(InitialUserAttributes.this, InitialRoamingAttributes.class);
                    intent.putExtra(getString(R.string.key_UserName), userName);
                    startActivity(intent);
                }

            }
        });

    }

    // Grab the user's location
    private void getUserLocation(){
        UserLocation mUserLocation = new UserLocation(getApplicationContext(), InitialUserAttributes.this);
        if(mUserLocation.isLocationPossible()){
            locationUserValue = mUserLocation.getUserLocation();
            if(locationUserValue != null){
                Log.d("Location in USER", locationUserValue);
            }
        }
    }

    // Grab the data from the activity and assign the data to the activity members
    private void grabData(){
        mFirstName = firstNameUserText.getText().toString();
        mLastName = lastNameUserText.getText().toString();

        mBirthDay = birthDateText.getText().toString();



        if(setMale.isChecked()) {
            mGender = "male";
        }
        else if(setFemale.isChecked()){
            mGender = "female";
        }

        mSexualOrientation = sexualOrientationUserText.getText().toString();
        mHeight = heightUserText.getText().toString().trim();
    }

    // Check if the data is valid. Returns true when valid.
    public boolean isDataValid(){
        if(TextUtils.isEmpty(mFirstName) || mFirstName == null){
            firstNameUserText.setError("First name cannot be empty!");
            return false;
        }
        else if(TextUtils.isEmpty(mLastName) || mLastName == null){
            lastNameUserText.setError("Last name cannot be empty!");
            return false;
        }
        else if(TextUtils.isEmpty(mBirthDay) || !isThisDateValid(mBirthDay,"MM/dd/yyyy")){
            birthDateText.setError("Incorrect birthday format!");
            return false;
        }
        else if((!setFemale.isChecked() && !setMale.isChecked()) || mGender == null || (!mGender.equals("male") && !mGender.equals("female")))
        {
            setMale.setError("Sex must be chosen!");
            return false;
        }
        else if(TextUtils.isEmpty(mSexualOrientation) || mSexualOrientation == null ||((!mSexualOrientation.equals("straight")&&(!mSexualOrientation.equals("bisexual"))&&(!mSexualOrientation.equals("gay"))))){
            sexualOrientationUserText.setError("Sexual orientation cannot be empty, and must be straight, gay, or bisexual!");
            return false;
        }
        else if(TextUtils.isEmpty(mHeight) || mHeight == null || !TextUtils.isDigitsOnly(mHeight)) {
            heightUserText.setError("Height cannot be empty and is in terms of inches numerically!");
            return false;
        }
        else if(TextUtils.isEmpty(locationUserValue) || locationUserValue == null) {
            retrieveLocationButton.setError("Please grab your current location!");
            return false;
        }
        else{
            Age a = new Age();
            Date birth = a.ConvertToDate(mBirthDay);
            mAge = Long.valueOf(a.calculateAge(birth));

            if(mAge < 18) {
                birthDateText.setError("Must be 18 years or older!");
                return false;
            }
            else if(mAge >= 130) {
                birthDateText.setError("Must be less than 130 years old!");
                return false;
            }

            height = Long.valueOf(mHeight);

            Log.d("Age", mAge.toString());
          return true;
        }

    }

    // Check to see if the date entered is a valid format.
    public boolean isThisDateValid(String dateToValidate, String dateFormat){
        Date date;
        if(dateToValidate == null){
            return false;
        }

        SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
        sdf.setLenient(false);

        try {

            //if not valid, it will throw ParseException
            date = sdf.parse(dateToValidate);
            Log.d("UserAttributes Date",date.toString());

        } catch (ParseException e) {
            Log.d("UserAttributes Date","Not working");
            e.printStackTrace();
            return false;
        }
        Date current = new Date();
        try {
            current = sdf.parse(sdf.format(current));
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return current.getTime() >= date.getTime();  //current date < entered date
    }

    // Set the user's data to the Firebase user info branch
    public void setData(){
        Firebase usersURL = new Firebase (Constants.FIREBASE_URL_USERS).child(userName).child(Constants.FIREBASE_LOC_USER_INFO);

        usersURL.child("firstName").setValue(mFirstName);
        usersURL.child("lastName").setValue(mLastName);
        usersURL.child("age").setValue(mAge);
        usersURL.child("birthDate").setValue(mBirthDay);
        usersURL.child("sex").setValue(mGender);
        usersURL.child("sexualOrientation").setValue(mSexualOrientation);
        usersURL.child("height").setValue(height);
        usersURL.child("location").setValue(locationUserValue);
        usersURL.child("userName").setValue(userName);
    }

    // Add users to the roaming list
    public void addUserToRoamingList(){
        Firebase roamingURL = new Firebase(Constants.FIREBASE_URL + "roamingList").child(locationUserValue).child(mGender).child(mSexualOrientation);

        long userAge = mAge;

        // If statements to check which branch the user will fall under.
        if(userAge <= 20){
            roamingURL.child("18-20").child(userName).child("mark").setValue(0);
        }
        else if(userAge >= 21 && userAge <= 29){
            roamingURL.child("21-29").child(userName).child("mark").setValue(0);
        }
        else if(userAge >= 30 && userAge <= 39){
            roamingURL.child("30-39").child(userName).child("mark").setValue(0);
        }
        else if(userAge >= 40 && userAge <= 49){
            roamingURL.child("40-49").child(userName).child("mark").setValue(0);
        }
        else if(userAge >= 50 && userAge <= 59){
            roamingURL.child("50-59").child(userName).child("mark").setValue(0);
        }
        else {
            roamingURL.child("60+").child(userName).child("mark").setValue(0);
        }
    }
}


