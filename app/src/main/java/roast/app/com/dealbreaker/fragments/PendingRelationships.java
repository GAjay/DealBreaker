package roast.app.com.dealbreaker.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.firebase.ui.FirebaseRecyclerAdapter;
import com.firebase.client.Query;

import java.util.ArrayList;


import roast.app.com.dealbreaker.PendingUserProfile;
import roast.app.com.dealbreaker.R;
import roast.app.com.dealbreaker.models.PendingRelationshipViewHolder;
import roast.app.com.dealbreaker.models.RelationshipAttribute;
import roast.app.com.dealbreaker.models.User;
import roast.app.com.dealbreaker.models.UserImages;
import roast.app.com.dealbreaker.util.Constants;
import roast.app.com.dealbreaker.util.DownloadImages;


//This class will be used to Show the status of the Pending Relationships
// so it will show other users that have added the current user , allowing
//the user to bypass the queue to someone who has directly expressed interest in them
//It may also be used to track if your requests have been denied or not sure yet

//May also be using a List or Recycler view from the Firebase-UI: https://github.com/firebase/FirebaseUI-Android
//https://www.learn2crack.com/2016/02/custom-swipe-recyclerview.html
// http://stackoverflow.com/questions/24885223/why-doesnt-recyclerview-have-onitemclicklistener-and-how-recyclerview-is-dif/24933117#24933117
public class PendingRelationships extends Fragment {

    FirebaseRecyclerAdapter<RelationshipAttribute ,PendingRelationshipViewHolder> pendingRecyclerAdapter;

    private RecyclerView recycViewPending;
    private String userName;
    private ArrayList<String> PendingUsername;
    private Query refPendingUser, getPendingUsersInfo;
    private ArrayList<User> pendingUserObjects;
    private View view;
    private Firebase refPendingUsersInfo, pendingFirebaseREF;
    private Firebase refRootConfirmed, refSelectedConfirmed, removeREFPending, removeREFSelectedPending;
    private ValueEventListener removeREFSelectedPendingListener, removeREFPendingListener;

    public PendingRelationships() {
        // Required empty public constructor
    }

    public static PendingRelationships newInstance(String username) {
        PendingRelationships fragment = new PendingRelationships();
        Bundle args = new Bundle();
        args.putString("userName", username);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            userName = getArguments().getString("userName");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        view = inflater.inflate(R.layout.fragment_pending_relationships, container, false);

        recyclerPage(view);
        listenerFunc();

        return view;
    }

    private void recyclerPage(View rootView){
        pendingFirebaseREF = new Firebase(Constants.FIREBASE_URL_PENDING).child(userName);
        //LinearLayoutManager RLM = new LinearLayoutManager(getActivity());
        //RLM.setOrientation(LinearLayoutManager.VERTICAL);

        getPendingUsersInfo = pendingFirebaseREF.orderByChild("lastName");
        recycViewPending = (RecyclerView) rootView.findViewById(R.id.pending_relationship_recycler_view);
        recycViewPending.setHasFixedSize(true);
        recycViewPending.setLayoutManager(new LinearLayoutManager(getContext()));

        if(getPendingUsersInfo != null) {
            pendingRecyclerAdapter = new FirebaseRecyclerAdapter<RelationshipAttribute, PendingRelationshipViewHolder>(
                    RelationshipAttribute.class,
                    R.layout.item_pending_relationship,
                    PendingRelationshipViewHolder.class,
                    getPendingUsersInfo
            ) {
                @Override
                protected void populateViewHolder(PendingRelationshipViewHolder pendingRelationshipViewHolder,final RelationshipAttribute PRA, final int i) {
                    String listedUserName = pendingRecyclerAdapter.getRef(i).getKey();
                    currentProfilePic(listedUserName);
                    updateUserInfo(listedUserName);
                    DownloadImages imageDownload = new DownloadImages(pendingRelationshipViewHolder.imageView ,getActivity());
                    imageDownload.execute(PRA.getProfilePic());
                    pendingRelationshipViewHolder.name.setText(PRA.getFirstName() + " " + PRA.getLastName() + "      Age: " + PRA.getAge());
                    pendingRelationshipViewHolder.attribute.setText(PRA.getLocation() + "   SO:"+ PRA.getSexualOrientation());

                    if(PRA.getMark() == 0) {
                        pendingRelationshipViewHolder.add.setText("Confirm");
                        pendingRelationshipViewHolder.add.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                String key = pendingRecyclerAdapter.getRef(i).getKey();
                                //Add to confirmed list
                                confirmFromPending(key);
                            }
                        });
                    }
                    else {
                        pendingRelationshipViewHolder.add.setText("Pending");
                        pendingRelationshipViewHolder.add.setEnabled(false);
                    }

                    //On Click Listener for bringing up User's Profile in PendingUserProfileActivity
                    pendingRelationshipViewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent selectedProfile = new Intent(getActivity(), PendingUserProfile.class);
                            selectedProfile.putExtra("userName",pendingRecyclerAdapter.getRef(i).getKey());
                            selectedProfile.putExtra("rootUser",userName);
                            startActivity(selectedProfile);
                        }
                    });
                }
            };
            recycViewPending.setAdapter(pendingRecyclerAdapter);
        }
    }

    private void confirmFromPending(String key){

        refRootConfirmed = new Firebase(Constants.FIREBASE_URL_CONFIRMED_RELATIONSHIPS).child(userName).child(key);  //addREFCONFIRMED
        refSelectedConfirmed = new Firebase(Constants.FIREBASE_URL_CONFIRMED_RELATIONSHIPS).child(key).child(userName); //addREFPENDING
        removeREFPending = new Firebase(Constants.FIREBASE_URL_PENDING).child(userName).child(key); //REFPendingUserPending
        removeREFSelectedPending = new Firebase(Constants.FIREBASE_URL_PENDING).child(key).child(userName); //REFRootUserPending

        removeREFPendingListener = removeREFPending.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                RelationshipAttribute pendingRA = dataSnapshot.getValue(RelationshipAttribute.class);
                if (pendingRA != null) {
                    pendingRA.setMark(1);
                    refSelectedConfirmed.setValue(pendingRA);
                    removeREFPending.removeValue();
                }
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {

            }
        });

        removeREFSelectedPendingListener = removeREFSelectedPending.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                RelationshipAttribute rootRA = dataSnapshot.getValue(RelationshipAttribute.class);
                if (rootRA != null) {
                    rootRA.setMark(1);
                    refRootConfirmed.setValue(rootRA);
                    removeREFSelectedPending.removeValue();
                }
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {

            }
        });



    }

    private void listenerFunc(){
        Firebase firebaseREF = new Firebase(Constants.FIREBASE_URL_PENDING).child(userName);
        PendingUsername = new ArrayList<>();

        refPendingUser = firebaseREF.orderByKey();
        refPendingUser.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                recyclerPage(view);
                for (DataSnapshot childUsers : dataSnapshot.getChildren()) {

                    String name = childUsers.getKey();
                    if (name != null) {
                        PendingUsername.add(name);
                    }
                }
                if (PendingUsername.size() > 0) {
                    getUserInfoPending(); //calls func getUserInfoPending to retrieve info
                }
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {

            }
        });


    }

    private void getUserInfoPending() {
        pendingUserObjects = new ArrayList<>();
        for (int i = 0; i < PendingUsername.size(); i++) {
            refPendingUsersInfo = new Firebase(Constants.FIREBASE_URL_USERS).child(PendingUsername.get(i)).child(Constants.FIREBASE_LOC_USER_INFO);
            getPendingUsersInfo = refPendingUsersInfo.orderByKey();
            getPendingUsersInfo.addValueEventListener(new ValueEventListener() {
                public void onDataChange(DataSnapshot dataSnapshot) {
                    User user = dataSnapshot.getValue(User.class);
                    if (user != null) {
                        Log.d("Size", String.valueOf(PendingUsername.size()));
                        Log.d("userObj", user.getFirstName());
                        pendingUserObjects.add(user);

                    }

                   recyclerPage(view);
                }

                @Override
                public void onCancelled(FirebaseError firebaseError) {
                    Log.e(getString(R.string.LogTagUserProfile),
                            getString(R.string.FirebaseOnCancelledError) +
                                    firebaseError.getMessage());
                }
            });
        }
    }

    @Override
    public void onResume(){
        super.onResume();
        recyclerPage(view);
    }

    @Override
    public void onPause(){
        super.onPause();
        if(removeREFSelectedPending != null){
            removeREFSelectedPending.removeEventListener(removeREFSelectedPendingListener);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (removeREFSelectedPending != null) {
            removeREFSelectedPending.removeEventListener(removeREFSelectedPendingListener);
        }
    }


    public void listener(Firebase firebaseREF)
    {
        //class list users  -- list of users
        final ArrayList<User> users = new ArrayList<>();

        firebaseREF.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    for(DataSnapshot user: dataSnapshot.getChildren()){
                        for(DataSnapshot programSnapshot :user.getChildren()) {
                            final User us = programSnapshot.getValue(User.class);
                            users.add(us);
                        }
                    }
                }

            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {

            }
        });
    }

    /* This allows for the Images updated by the listed user at any time to be updated directly to the pending list
    * */
    private void currentProfilePic(final String listedUser){
        Firebase listedUserImagesREF = new Firebase(Constants.FIREBASE_URL_IMAGES).child(listedUser).child(Constants.FIREBASE_LOC_PROFILE_PIC);
        listedUserImagesREF.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                UserImages userImages = dataSnapshot.getValue(UserImages.class);
                if (userImages != null) {
                    String profilePic = userImages.getProfilePic();
                    pendingFirebaseREF.child(listedUser).child("profilePic").setValue(profilePic);
                    //initializeView(view);
                }
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {

            }
        });

    }

        /* This allows for any pertinent User information updated by the listed user at any time to be updated directly to the pending list
        */
    private void updateUserInfo(final String listedUser){
        Firebase listedUserInfo = new Firebase(Constants.FIREBASE_URL_USERS).child(listedUser).child(Constants.FIREBASE_LOC_USER_INFO);
        listedUserInfo.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                User user = dataSnapshot.getValue(User.class);
                if (user != null) {
                    String firstName = user.getFirstName();
                    String lastName = user.getLastName();
                    Long age = user.getAge();
                    String sex = user.getSex();
                    String location = user.getLocation();
                    String sexualOrientation = user.getSexualOrientation();

                    pendingFirebaseREF.child(listedUser).child("firstName").setValue(firstName);
                    pendingFirebaseREF.child(listedUser).child("lastName").setValue(lastName);
                    pendingFirebaseREF.child(listedUser).child("age").setValue(age);
                    pendingFirebaseREF.child(listedUser).child("sex").setValue(sex);
                    pendingFirebaseREF.child(listedUser).child("location").setValue(location);
                    pendingFirebaseREF.child(listedUser).child("sexualOrientation").setValue(sexualOrientation);
                    //initializeView(view);
                }
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {

            }
        });
    }



}
