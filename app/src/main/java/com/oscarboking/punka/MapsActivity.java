package com.oscarboking.punka;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.StrictMode;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, ActivityCompat.OnRequestPermissionsResultCallback, LocationListener  {

    private GoogleMap mMap;

    GoogleApiClient mGoogleApiClient;

    LatLng latLng;
    SupportMapFragment mFragment;
    Marker currLocationMarker;
    LocationManager locManager;
    private static final int REQUEST_LOCATION = 2;
    private double currentLat;
    private double currentLong;
    private List<ParkingArea> currentParkingAreas = new LinkedList<ParkingArea>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {

        mMap = googleMap;


        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        } else {
            // Show rationale and request permission.
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION);
        }

        // Use the location manager through GPS
        locManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);

        //get the current location (last known location) from the location manager
        Location location = locManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

        //if location found display as a toast the current latitude and longitude
        if (location != null) {
            currentLat = location.getLatitude();
            currentLong = location.getLongitude();
        } else {
            Toast.makeText(this, "Cannot fetch current location!",
                    Toast.LENGTH_SHORT).show();
        }

        System.out.println("location was: Lat: " + currentLat + ", Long: " + currentLong);
        //when the current location is found – stop listening for updates (preserves battery)
        locManager.removeUpdates(this);

        mMap.getUiSettings().setMapToolbarEnabled(false);
        mMap.setInfoWindowAdapter(new MyInfoWindowAdapter());

        getStations();

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(currentLat,currentLong),15));

    }

    public static double getDistanceFromLatLonInKm(double lat1, double lng1, double lat2, double lng2) {
        double earthRadius = 6371000; //meters
        double dLat = Math.toRadians(lat2-lat1);
        double dLng = Math.toRadians(lng2-lng1);
        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLng/2) * Math.sin(dLng/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        double dist = (float) (earthRadius * c);

        return dist;
    }


    public void getStations() {

        int SDK_INT = android.os.Build.VERSION.SDK_INT;
        if (SDK_INT > 8)
        {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
                    .permitAll().build();
            StrictMode.setThreadPolicy(policy);


            String uri = "http://data.goteborg.se/ParkingService/v2.1/BikeParkings/{1b6ba419-fdc5-4101-a241-91a83ef7610d}?latitude={"+currentLat+"}&longitude={"+currentLong+"}&radius={10}&format={XML}";
            URL url = null;

            try {
                url = new URL(uri);
                HttpURLConnection connection =
                        (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Accept", "application/xml");

                InputStream xml = connection.getInputStream();

                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                DocumentBuilder db = dbf.newDocumentBuilder();
                Document doc = db.parse(xml);

                doc.getDocumentElement().normalize();

                NodeList nodeList = doc.getElementsByTagName("BikeParking");

                for (int i = 0; i < nodeList.getLength(); i++) {

                    Node currentNode = nodeList.item(i);

                    if (currentNode.getNodeType() == Node.ELEMENT_NODE) {
                        Element eElement = (Element) currentNode;


                        Double latitude = Double.parseDouble(eElement.getElementsByTagName("Lat").item(0).getTextContent());
                        Double longitute = Double.parseDouble(eElement.getElementsByTagName("Long").item(0).getTextContent());
                        System.out.println("distance between node and user: " + getDistanceFromLatLonInKm(currentLat,currentLong,latitude,longitute));

                        if(getDistanceFromLatLonInKm(currentLat,currentLong,latitude,longitute)<1200){
                            String id = eElement.getElementsByTagName("Id").item(0).getTextContent();
                            int parkingSpaces = Integer.parseInt(eElement.getElementsByTagName("Spaces").item(0).getTextContent());
                            String address = eElement.getElementsByTagName("Address").item(0).getTextContent();
                            System.out.println("Adding parking area: " + id);

                            currentParkingAreas.add(new ParkingArea(id,latitude,longitute,parkingSpaces,address));

                            MarkerOptions marker = new MarkerOptions().position(new LatLng(latitude, longitute)).title(id);
                            marker.icon(BitmapDescriptorFactory.fromResource(R.drawable.bike_icon_smaller));
                            mMap.addMarker(marker);
                        }
                    }
                }
            } catch (ProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ParserConfigurationException e) {
                e.printStackTrace();
            } catch (SAXException e) {
                e.printStackTrace();
            }
        }

    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions,
                                           int[] grantResults) {
        if (requestCode == REQUEST_LOCATION) {
            if(grantResults.length == 1
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // We can now safely use the API we requested access to
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED) {
                    mMap.setMyLocationEnabled(true);
                }
            } else {
                // Permission was denied or request was cancelled
            }
        }
    }

    @Override
    public void onLocationChanged(Location location) {

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    private class MyInfoWindowAdapter implements GoogleMap.InfoWindowAdapter {

        private final View myContentsView;

        MyInfoWindowAdapter(){
            myContentsView = getLayoutInflater().inflate(R.layout.custom_info_window, null);
        }

        @Override
        public View getInfoContents(Marker marker) {

            ParkingArea area = getAreaFromId(marker.getTitle());

            TextView infoWindowName = ((TextView)myContentsView.findViewById(R.id.infoWindowName));
            infoWindowName.setText(area.getAddress());
            TextView infoWindowSpaces = ((TextView)myContentsView.findViewById(R.id.infoWindowSpaces));
            infoWindowSpaces.setText("Antal platser: " + area.getParkingSpaces());

            return myContentsView;
        }


        @Override
        public View getInfoWindow(Marker marker) {
            // TODO Auto-generated method stub
            return null;
        }

    }

    public ParkingArea getAreaFromId(String id){
        System.out.println("inside getareafrommarker");
        for(ParkingArea area : currentParkingAreas) {
            System.out.println("checking if area.getId() (" + area.getId()+") is equal to marker title (id) ("+id+")");
            if (area.getId().equals(id)){
                return area;
            }
        }
        return new ParkingArea("",-1,-1,0,"");
    }
}
