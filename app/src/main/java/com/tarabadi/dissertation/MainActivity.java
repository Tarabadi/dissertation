package com.tarabadi.dissertation;

/**
    DISSERTATION: Create app for finding skateable areas nearby

    TITLE: GeoSkate
*/

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.lang.GeoLocation;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.GpsDirectory;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ListFragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import android.view.View.OnClickListener;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;

import com.tarabadi.dissertation.DbRead.AsyncReadResponse;
import com.tarabadi.dissertation.DbWrite.AsyncWriteResponse;
import com.tarabadi.dissertation.LocationGetter.LocationResult;

import com.bumptech.glide.Glide;

public class MainActivity extends AppCompatActivity
        implements AsyncReadResponse, AsyncWriteResponse
{

    //image picker stuff
    private final int SELECT_PHOTO = 1;
    //taking photo stuff
    static final int CAMERA_REQUEST = 2;
    private final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 3;
    //for checkbox
    private boolean checked = false;
    //toasts
    private Toast noInfoToast;

    final int maxTitleLength = 30;
    final int maxDescLength = 100;

    final String titleTooLong = "Title must be less than "+maxTitleLength+" characters";
    final String descTooLong  = "Description must be less than "+maxDescLength+" characters";

    Collection<PhotoLocation> photoLocations = new ArrayList<PhotoLocation>();
    //db stuff
    private static SpotReaderDbHelper mDbHelper;

    private double lat;
    private double lng;
    private double latPhoto;
    private double lngPhoto;
    //list fragment stuff:
    private ListFragment sList;
    private SpotAdapter sAdapter;
    //ArrayList of db info
    ArrayList<Spot> spotCollection = new ArrayList<Spot>();
    private Context mainContext;

    //current LatLng
    LatLng current;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        //database access
        mDbHelper = new SpotReaderDbHelper(getBaseContext());

        mainContext = getApplicationContext();

        //get current location
        LocationResult locationResult = new LocationResult(){
            @Override
            public void gotLocation(Location location){
                //got the location
                current = new LatLng(location.getLatitude(), location.getLongitude());
                loadSpots(mDbHelper);
            }
        };
        LocationGetter myLocation = new LocationGetter();
        boolean located = myLocation.getLocation(this, locationResult);

        if (!located){
            //display saying turn location services on and hit refresh
            showNoLocDataDialog();
            readFinish(null);
        }

        //toast creation
        noInfoToast = Toast.makeText(getApplicationContext(), R.string.noInfoToast, Toast.LENGTH_LONG);
        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);
        Activity thisActivity = this;
        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(thisActivity,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(thisActivity,
                    Manifest.permission.READ_EXTERNAL_STORAGE)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(thisActivity,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
                // MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }
    }

    @Override
    public void readFinish(ArrayList<Spot> dbRecords) {

        if (dbRecords == null){
            //no location data so put empty list??
            if (sList == null){
                sList = (ListFragment) getSupportFragmentManager().findFragmentById(R.id.list);
                sList.setEmptyText("No data, enable location services and refresh");
                sList.setListAdapter(null);
            }

        }else {

            spotCollection = dbRecords;
            //calculate distances for all retrieved spots
            spotDistances(spotCollection, current);
            //sort arraylist by distance from current position
            Collections.sort(spotCollection, new Comparator<Spot>() {
                @Override
                public int compare(Spot s1, Spot s2) {
                    return s1.distance - s2.distance; //distance from position ascending
                }
            });

            sAdapter = new SpotAdapter(this, spotCollection);
            if (sList == null) {
                sList = (ListFragment) getSupportFragmentManager().findFragmentById(R.id.list);
                sList.setListAdapter(sAdapter);
                Toast.makeText(getBaseContext(), "List downloaded", Toast.LENGTH_SHORT).show();
            } else {
                sAdapter.notifyDataSetChanged();
                sList.setListAdapter(sAdapter);
                Toast.makeText(getBaseContext(), "List downloaded", Toast.LENGTH_SHORT).show();
            }

        }

    }

    public void onListItemButtonClicked(int position){

            //google maps/direction stuff
            Spot temp = spotCollection.get(position);
            String intentUri = "http://maps.google.com/maps?saddr="
                    + current.latitude + "," + current.longitude
                    + "(" + "Your Location" + ")&daddr="
                    + temp.lat + "," + temp.lng
                    + "(" + temp.title + ")";
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(intentUri));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setPackage("com.google.android.apps.maps");
            try {
                mainContext.startActivity(intent);
            }catch(ActivityNotFoundException e){
                try{
                    Intent unrestrictedIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(intentUri));
                    unrestrictedIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    mainContext.startActivity(unrestrictedIntent);
                }catch (ActivityNotFoundException innerE){
                    Toast.makeText(this, "Please install a maps application", Toast.LENGTH_LONG);
                }
            }


    }

    public void onListFragmentItemClick(final int position) {


        //need to create dialog with information from spot at position in arraylist
        Spot temp = spotCollection.get(position);

        View dialogView = View.inflate(this, R.layout.dialog_listitem, null);

        TextView spotTitle = (TextView)dialogView.findViewById(R.id.spotTitleDisplay);
        TextView spotDesc = (TextView) dialogView.findViewById(R.id.spotDescDisplay);
        ImageView img = (ImageView)dialogView.findViewById(R.id.spotPhotoDisplay);
        Button btn = (Button)dialogView.findViewById(R.id.spotButtonDisplay);
        spotTitle.setText(temp.title);
        spotDesc.setText(temp.desc);

        Glide.with(getApplicationContext())
                .load(temp.pic)
                .asBitmap()
                .centerCrop()
                .into(img);

        btn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onListItemButtonClicked(position);
            }
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setView(dialogView);

        builder.setPositiveButton(R.string.dialogNeg, null);

        final AlertDialog dialog = builder.create();

        dialog.show();



    }

    public void showSpotCreateDialog(final Uri imageUri){

        View dialogView = View.inflate(this, R.layout.dialog_editspot, null);

        //set image preview to selected image
        ImageView img = (ImageView)dialogView.findViewById(R.id.photopreview);

        Glide.with(getApplicationContext())
                .load(imageUri)
                .asBitmap()
                .centerCrop()
                .into(img);

        final EditText spotTitle = (EditText)dialogView.findViewById(R.id.spotTitle);
        final EditText spotDesc = (EditText)dialogView.findViewById(R.id.spotDesc);

        //create LatLng for location of photo
        final LatLng position = new LatLng(latPhoto,lngPhoto);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setView(dialogView);

        builder.setPositiveButton(R.string.dialogPosi, null);

        builder.setNegativeButton(R.string.dialogNeg, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        final AlertDialog dialog = builder.create();

        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                String title = spotTitle.getText().toString().trim();
                String desc = spotDesc.getText().toString().trim();

                boolean textEmpty = (title.isEmpty() || desc.isEmpty());

                if (textEmpty){
                    //toast
                    noInfoToast.show();
                }else if (title.length() > maxTitleLength){
                    //show toast about title being too long
                    Toast.makeText(getApplicationContext(), titleTooLong + ", is " + (title.length() - maxTitleLength)
                            + " characters too long."
                            , Toast.LENGTH_LONG).show();
                }else if (desc.length() > maxDescLength){
                    //show toast about desc being too long
                    Toast.makeText(getApplicationContext(), descTooLong + ", is " + (desc.length() - maxDescLength)
                            + " characters too long.", Toast.LENGTH_LONG).show();
                }else{
                    //upload data to db
                    dialog.cancel();
                    try {
                        addSpotToDb(mDbHelper, imageUri, position, title, desc);
                        Toast.makeText(getApplicationContext(), "Spot added", Toast.LENGTH_SHORT);
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e("DB ERROR", e.toString());
                        Toast.makeText(getApplicationContext(), "DB Error: " + e.toString(), Toast.LENGTH_LONG);
                    }
                    dialog.cancel();
                }
            }
        });
    }

    private void spotDistances(ArrayList<Spot> spots, LatLng pos){
        if (pos != null && (pos.longitude != 0 && pos.latitude != 0)) {
            for (int i = 0; i < spots.size(); i++) {
                spots.get(i).distanceCalc(pos);
            }
        }
    }

    @Override
    public void writeFinish(long rowID) {
        if (rowID != -1){
            loadSpots(mDbHelper);
        }
    }

    private void loadSpots(SpotReaderDbHelper mdb){

        new DbRead(this).execute(mdb);

    }

    private void showNoLocDataDialog(){

            //dialog to be shown if no geo data on photo selected
            View dialogView = View.inflate(this, R.layout.dialog_nolocationdata, null);

            AlertDialog.Builder builder = new AlertDialog.Builder(this);

            builder.setView(dialogView).setPositiveButton(R.string.dialogPosi, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            }).show();


    }

    private class SpotAdapter extends ArrayAdapter<Spot> {

        ArrayList<Spot> update;

        public SpotAdapter(Context context, ArrayList<Spot> spots) {
            super(context, R.layout.activity_main_row, R.id.row_title, spots);
            update = spots;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent){
            View row = convertView;
            final SpotHolder holder;
            //check if a view can be reused, otherwise inflate a layout and set up view holder
            if (row == null){
                //inflate view from layout file
                row = getLayoutInflater().inflate(R.layout.activity_main_row, null);
                //set up holder and assign it to the view
                holder = new SpotHolder();
                holder.title = (TextView) row.findViewById(R.id.row_title);
                holder.desc = (TextView) row.findViewById(R.id.row_desc);
                holder.distance = (TextView) row.findViewById(R.id.row_distance);
                holder.photo = (ImageView) row.findViewById(R.id.row_pic);
                //set holder as tag for row for more efficient access.
                row.setTag(holder);
            }else{
                holder = (SpotHolder) row.getTag();
            }

            //get the spot for this item
            Spot item = getItem(position);

            //set title and desc for this item
            holder.title.setText(item.title);

            holder.distance.setText("Distance: " + item.distance + "m");

            holder.photo.setImageURI(null);

            Glide.with(getApplicationContext())
                    .load(item.pic) //uri of pic
                    .asBitmap()
                    .centerCrop()
                    .into(holder.photo);

            row.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    onListFragmentItemClick(position);
                }
            });

            return row;
        }

    }

    class SpotHolder implements OnMapReadyCallback{

        MapView mapView;
        GoogleMap map;

        TextView title;
        TextView desc;
        TextView distance;
        LatLng local;
        ImageView photo;



        @Override
        public void onMapReady(GoogleMap googleMap) {
            MapsInitializer.initialize(getApplicationContext());
            map = googleMap;
            local = (LatLng) mapView.getTag();
            if (local != null){
                setPreviewMapLocation(map, local);
            }

        }

    }

    private void showNoGeoDataDialog(){
        //dialog to be shown if no geo data on photo selected
        View dialogView = View.inflate(this, R.layout.dialog_nogeodata, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setView(dialogView).setPositiveButton(R.string.noGeoDataButton, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        }).show();

    }

    private void addSpotToDb(SpotReaderDbHelper mDbHelper, Uri imageUri, LatLng local, String title, String desc) throws IOException {

        Spot temp = new Spot(-3, title, desc, local.latitude, local.longitude, imageUri);
        new DbWrite(this, temp, mDbHelper).execute();

    }

    private void setPreviewMapLocation(GoogleMap map, LatLng local){
        //add marker and set camera
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(local, 25f));
        map.addMarker(new MarkerOptions().position(local));
        //set map type back to normal
        map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
    }

    @Override
    protected void onStart(){
        super.onStart();
    }

    @Override
    protected void onStop(){
        super.onStop();
    }

    @Override
    protected void onDestroy(){
        mDbHelper.close();
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults){
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                } else {

                    // permission denied
                }
            }

        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent){
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);

        switch(requestCode) {
            case SELECT_PHOTO:
                if (resultCode == RESULT_OK){
                    try {
                        //after selecting a photo
                        final Uri imageUri = imageReturnedIntent.getData();
                        //InputStream of selected photo
                        final InputStream imageStream = getContentResolver().openInputStream(imageUri);
                        //metadata extractor library:
                        BufferedInputStream bis = new BufferedInputStream(imageStream);
                        File temp = new File(imageUri.getPath());
                        Metadata metadata = ImageMetadataReader.readMetadata(bis);
                        //adapted from
                        // https://github.com/drewnoakes/metadata-extractor/blob/master/Samples/com/drew/metadata/GeoTagMapBuilder.java
                        //read gps data
                        Collection<GpsDirectory> gpsDirectories = metadata.getDirectoriesOfType(GpsDirectory.class);
                        for (GpsDirectory gpsDirectory : gpsDirectories){
                            //try to read out the location, making sure it's non-zero
                            GeoLocation geoLocation = gpsDirectory.getGeoLocation();
                            if (geoLocation != null && !geoLocation.isZero()){
                                photoLocations.add(new PhotoLocation(geoLocation, temp));
                                latPhoto = geoLocation.getLatitude();
                                lngPhoto = geoLocation.getLongitude();
                            }
                        }
                        //if lat && lng == 0, bring noGeoDataPopup
                        if (latPhoto == 0 && lngPhoto == 0){
                            showNoGeoDataDialog();
                        }else {
                            showSpotCreateDialog(imageUri);
                        }
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (ImageProcessingException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e("PICS", "IOException: " + e.toString());
                    }
                }
            case CAMERA_REQUEST:
                if (resultCode == RESULT_OK){
                    //retrieve photo just taken from storage
                    //get latlong of current position
                    Uri photo = imageReturnedIntent.getData();
                    if (current != null) {
                        lat = current.latitude;
                        lng = current.longitude;
                        showSpotCreateDialog(photo);
                    }
                }


        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.mainmenu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.action_settings:
                //user chose settings item, show app settings ui
                Intent settingsActivity = new Intent(getBaseContext(), SettingsActivity.class);
                startActivity(settingsActivity);
                return true;
            case R.id.action_upload_photo:
                //when photo has location tags but location off tag is set to equator, so
                //need check to make sure tag is not in ocean or something
                //also need check for location tags in general
                //if taking a photo with camera then can use current location tag and let user edit
                //up to certain distance
                uploadCheckboxSetter();
                //user chose take photo action, so open camera intent, take photo, and then use
                //that along with saved "current" LatLng to create new spot
                return true;
            case R.id.action_take_photo:
                if (current != null) {
                    Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    startActivityForResult(cameraIntent, CAMERA_REQUEST);
                }
                return true;
            //refresh list by obtaining new gps data and ordering spots accordingly
            case R.id.action_refresh:
                //get current location too
                LocationResult locationResult = new LocationResult(){
                    @Override
                    public void gotLocation(Location location){
                        //got the location
                        current = new LatLng(location.getLatitude(), location.getLongitude());
                        loadSpots(mDbHelper);
                    }
                };
                LocationGetter myLocation = new LocationGetter();
                boolean located = myLocation.getLocation(this, locationResult);

                if (!located){
                    //display dialog saying turn location services on and hit refresh
                    showNoLocDataDialog();
                    readFinish(null);
                }else {
                    Toast.makeText(getBaseContext(), "Obtaining data...", Toast.LENGTH_SHORT).show();
                }
                return true;
            default:
                //if we got here, user's action was not recognized. invoke superclass to handle it
                return super.onOptionsItemSelected(item);
        }
    }

    private void uploadCheckboxSetter(){

        final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        boolean isChecked = sharedPref.getBoolean(SettingsActivity.KEY_GEOALERT, false);

        if (! isChecked) {
            View checkBoxView = View.inflate(this, R.layout.checkbox_upload, null);
            CheckBox checkBox = (CheckBox) checkBoxView.findViewById(R.id.checkbox);
            checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    checked = isChecked;

                }
            });
            checkBox.setText(R.string.uploadCheckboxText);
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.uploadAlertText)
                    .setView(checkBoxView)
                    .setCancelable(false)
                    .setPositiveButton("Okay", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {

                            if (checked) {
                                //save to shared prefs so checkbox no longer appears
                                SharedPreferences.Editor editor = sharedPref.edit();
                                editor.putBoolean(SettingsActivity.KEY_GEOALERT, checked);
                                editor.commit();
                            }

                            Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                            photoPickerIntent.setType("image/*");
                            startActivityForResult(photoPickerIntent, SELECT_PHOTO);

                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    }).show();
        }else{
            //already seen and ticked box, so just run gallery
            Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
            photoPickerIntent.setType("image/*");
            startActivityForResult(photoPickerIntent, SELECT_PHOTO);
        }

    }

    /**
     * Simple tuple type, which pairs an image file with its {@link GeoLocation}.
     */
    public static class PhotoLocation
    {
        public final GeoLocation location;
        public final File file;

        public PhotoLocation(final GeoLocation location, final File file)
        {
            this.location = location;
            this.file = file;
        }
    }

}

