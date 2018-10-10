package eu.mcomputing.cohave.funfi.rating.fragments;

import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.database.Cursor;
import android.location.Location;
import android.net.wifi.ScanResult;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import eu.mcomputing.cohave.funfi.R;
import eu.mcomputing.cohave.funfi.contentprovider.WifiContentProvider;
import eu.mcomputing.cohave.funfi.contentprovider.dao.PlaceVoteModel;
import eu.mcomputing.cohave.funfi.contentprovider.dao.WifiModel;
import eu.mcomputing.cohave.funfi.contentprovider.dao.WifiStaticModel;
import eu.mcomputing.cohave.funfi.helper.config.Config;
import eu.mcomputing.cohave.funfi.helper.log.MyLog;
import eu.mcomputing.cohave.funfi.logger.sensor.LocationInfo;
import eu.mcomputing.cohave.funfi.rating.RatingFragment;
import eu.mcomputing.cohave.funfi.syncadapter.accessor.PlaceVoteServerAccessor;

/*
 The MIT License (MIT)

 Copyright (c) 2015 Maros Cavojsky (www.mpage.sk), mComputing (www.mcomputig.eu)

 Permission is hereby granted, free of charge, to any person obtaining a copy
 of this software and associated documentation files (the "Software"), to deal
 in the Software without restriction, including without limitation the rights
 to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the Software is
 furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in all
 copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 SOFTWARE.
 */
public class PlacesFragment extends Fragment
{
    private View screenView;
    private TextView nearestPoiText;
    private TextView coordsText;
    private TextView distanceText;
    private TextView questionText;
    private ImageButton buttonRefresh;
    private ImageButton buttonYes;
    private ImageButton buttonNo;
    private NearestPoiData currentPoi = null;
    private Location currentLocation = null;
    private Location lastLikedLocation = null;
    private List<Long> dislikedPOIs = new ArrayList<>();
    private final int MIN_ANOTHER_VOTE_DIST = 10;


    public PlacesFragment() {
        super();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        screenView = inflater.inflate(R.layout.places_fragment, container, false);

        return screenView;
    }


    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        nearestPoiText = (TextView)getView().findViewById(R.id.poiText);
        coordsText = (TextView)getView().findViewById(R.id.coordText);
        distanceText = (TextView)getView().findViewById(R.id.distText);
        questionText = (TextView)getView().findViewById(R.id.questionText);
        buttonYes = (ImageButton)getView().findViewById(R.id.buttonYes);
        buttonNo = (ImageButton)getView().findViewById(R.id.buttonNo);
        buttonRefresh = (ImageButton)getView().findViewById(R.id.buttonRefresh);

        buttonRefresh.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                //Do stuff here
                refreshData();
            }
        });

        buttonYes.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                //Clicked yes
                if (currentLocation==null || currentPoi.name.equals(""))
                    return;
                vote(true);
                HideVoteButtons();
            }
        });

        buttonNo.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                //Clicked no
                if (currentLocation==null || currentPoi.name.equals(""))
                    return;
                vote(false);
                dislikedPOIs.add(currentPoi.poi_id);
                refreshData();
            }
        });
        refreshData();
    }

    // Show buttons if hodden
    private void ShowVoteButtons()
    {
        if (lastLikedLocation == null)
            return;
        if(lastLikedLocation.distanceTo(currentLocation) < MIN_ANOTHER_VOTE_DIST)
            return;

        ViewGroup layout = (ViewGroup) buttonRefresh.getParent();
        layout.removeView(buttonRefresh);

        layout.addView(buttonYes);
        layout.addView(buttonRefresh);
        layout.addView(buttonNo);

        dislikedPOIs.clear();
        lastLikedLocation = null;
        questionText.setText("Nachádzate sa na tomto mieste?");
    }

    // Hide buttons
    private void HideVoteButtons()
    {
        // remove yes button
        ViewGroup layout = (ViewGroup) buttonYes.getParent();
        if(null!=layout) //for safety only  as you are doing onClick
            layout.removeView(buttonYes);

        // remove no button
        layout = (ViewGroup) buttonNo.getParent();
        if(null!=layout) //for safety only  as you are doing onClick
            layout.removeView(buttonNo);

        // remove question text
        questionText.setText("");

        // save last liked location
        lastLikedLocation = new Location("LastLiked");
        lastLikedLocation.setLatitude(currentLocation.getLatitude());
        lastLikedLocation.setLongitude(currentLocation.getLongitude());
    }

    // refresh location and poi
    private void refreshData()
    {
        LocationInfo locationInfo = new LocationInfo(this.getActivity().getApplicationContext());
        locationInfo.senseLocation();
        currentLocation = LocationInfo.getCurrentLocation();

        ShowVoteButtons();

        if (currentLocation!=null) {
            currentPoi = getNearestPOI(currentLocation.getLatitude(), currentLocation.getLongitude());

            coordsText.setText("    Aktuálna poloha\n" + currentLocation.getLatitude() + " lat, " + currentLocation.getLongitude()+" lon");
            nearestPoiText.setText(currentPoi.name.equals("") ? "Žiadne nájdené miesta v okolí" : currentPoi.name);
            distanceText.setText(String.format("%.2f",currentPoi.distance) + " m");
        }
        else {
            coordsText.setText("0, 0");
            distanceText.setText("0 m");
            nearestPoiText.setText("Neznáma poloha");
        }
    }

    // Find nearest poi in local DB without already diliked pois
    private NearestPoiData getNearestPOI(Double latitude, Double longitude)
    {
        NearestPoiData poi = new NearestPoiData();
        double shortestDist = Double.MAX_VALUE;

        poi.name = "";

        Cursor cursor = getActivity().getApplicationContext().getContentResolver().query(
                WifiContentProvider.WIFI_STATIC_CONTENT_URI,
                new String[]{"latitude","longitude","poi_id","poi"},
                "(?-latitude) > -0.015 AND (?-latitude) < 0.015 AND (?-longitude) > -0.015 AND (?-longitude) < 0.015",
                new String[]{latitude.toString(),latitude.toString(),longitude.toString(),longitude.toString()},
                WifiStaticModel.Table._ID
        );

        cursor.moveToFirst();

        while (!cursor.isAfterLast()) {
            Location locationA = new Location("point A");

            locationA.setLatitude(latitude);
            locationA.setLongitude(longitude);

            Location locationB = new Location("point B");

            locationB.setLatitude(cursor.getDouble(0));
            locationB.setLongitude(cursor.getDouble(1));

            double distance = locationA.distanceTo(locationB);

            if (distance < shortestDist) {
                Long poiID = cursor.getLong(2);

                if (!dislikedPOIs.contains(poiID)) {
                    poi.name = cursor.getString(3);
                    poi.distance = distance;
                    poi.latitude = locationB.getLatitude();
                    poi.longitude = locationB.getLongitude();
                    poi.poi_id = poiID;
                    shortestDist = distance;
                }
            }

            cursor.moveToNext();
        }
        cursor.close();

        return poi;

    }

    // get last record id from vote table
    private long getLastDBid()
    {
        Cursor c = this.getActivity().getContentResolver().query(
                WifiContentProvider.getLimitUri(WifiContentProvider.PLACE_VOTE_URL, 1, 0), new String[]{PlaceVoteModel.Table._ID}, null,null, PlaceVoteModel.Table._ID+" DESC"
        );
        c.moveToFirst();
        long last_id = c.isAfterLast() ? -1 : c.getLong(0);
        c.close();

        return last_id;
    }

    // write answer into local database
    private void vote(boolean answer)
    {
        PlaceVoteModel m = new PlaceVoteModel(getLastDBid()+1,currentPoi.latitude, currentPoi.longitude, currentPoi.poi_id, (answer) ? "YES" : "NO");

        ContentValues[] contentValues = new ContentValues[1];
        contentValues[0] = m.getContentValues();
        this.getActivity().getContentResolver().bulkInsert(WifiContentProvider.PLACE_VOTE_CONTENT_URI, contentValues);
    }

    class NearestPoiData
    {
        public String name;
        public double distance;
        public long poi_id;
        public double latitude;
        public double longitude;
    }
}