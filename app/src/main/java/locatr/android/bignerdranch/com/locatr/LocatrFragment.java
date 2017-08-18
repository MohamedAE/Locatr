package locatr.android.bignerdranch.com.locatr;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.List;

public class LocatrFragment extends SupportMapFragment {

	private static final String TAG = "LocatrFragment";

	private static ProgressDialog mProgressDialog;

	private static final String[] LOCATION_PERMISSIONS = new String[] {
			android.Manifest.permission.ACCESS_FINE_LOCATION,
			android.Manifest.permission.ACCESS_COARSE_LOCATION
	};

	private static final int REQUEST_LOCATION_PERMISSIONS = 0;

	//Access to Play Services
	private GoogleApiClient mClient;
	private GoogleMap mMap;
	private Bitmap mMapImage;
	private GalleryItem mMapItem;
	private Location mCurrentLocation;

	public static LocatrFragment newInstance() {
		return new LocatrFragment();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);

		mClient = new GoogleApiClient.Builder(getActivity())
				.addApi(LocationServices.API)
				.addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
					//When connected to GPlayServices, recreate menu
					@Override
					public void onConnected(Bundle bundle) {
						getActivity().invalidateOptionsMenu();
					}

					@Override
					public void onConnectionSuspended(int i) {

					}
				})
				.build();

		/*Acquire a reference to the master GoogleMap object
		* GoogleMap - The main class for Google Maps API, cannot be instantiated
		* SupportMapFragment.getMapAsync(...) - Retrieves GoogleMap object asynchronously*/
		getMapAsync(new OnMapReadyCallback() {
			@Override
			public void onMapReady(GoogleMap googleMap) {
				mMap = googleMap;
				//Update map view as soon as mMap is instantiated
				updateUI();
			}
		});
	}

	@Override
	public void onStart() {
		super.onStart();

		//Declare the options menu has changed, recreate it
		getActivity().invalidateOptionsMenu();
		//Connect to Google Play Services
		mClient.connect();
	}

	@Override
	public void onStop() {
		super.onStop();

		mClient.disconnect();
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.fragment_locatr, menu);

		MenuItem searchItem = menu.findItem(R.id.action_locate);
		//Enable/disable search button depending on Play Services connection
		searchItem.setEnabled(mClient.isConnected());
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.action_locate:
				if (hasLocationPermission()) {
					//If user has granter location permission
					findImage();
				} else {
					//If user hasn't, request permissions, update permission field
					requestPermissions(LOCATION_PERMISSIONS, REQUEST_LOCATION_PERMISSIONS);
				}
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	/*Callback for the result of a permission request*/
	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		switch (requestCode) {
			case REQUEST_LOCATION_PERMISSIONS:
				if (hasLocationPermission()) {
					findImage();
				}
			default:
				super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		}
	}

	/*Construct and send out a location request*/
	private void findImage() {
		LocationRequest request = LocationRequest.create();
		//How Android should prioritize battery life/accuracy
		request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
		//How many times location should be updated
		request.setNumUpdates(1);
		//How frequently location should be updated
		request.setInterval(0);

		if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
			requestPermissions(LOCATION_PERMISSIONS, REQUEST_LOCATION_PERMISSIONS);
		} else {
			LocationServices.FusedLocationApi
					.requestLocationUpdates(mClient, request, new LocationListener() {
						@Override
						public void onLocationChanged(Location location) {
							Log.i(TAG, "Got a fix: " + location);
							new SearchTask().execute(location);
						}
					});
		}
	}

	/*Updates Map view after pinch-zooming and panning*/
	private void updateUI() {
		if (mMap == null || mMapImage == null) {
			return;
		}

		//Get two corner points bounding updated view
		LatLng itemPoint = new LatLng(mMapItem.getLat(), mMapItem.getLon());
		LatLng myPoint = new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude());

		//BitmapDescriptor - Describes meta data of a Bitmap object
		//Instantiate a BitmapDescriptor
		BitmapDescriptor itemBitmap = BitmapDescriptorFactory.fromBitmap(mMapImage);

		//MarkerOptions - Desciption details of a Bitmap's attributes
		MarkerOptions itemMarker = new MarkerOptions()
				.position(itemPoint)
				.icon(itemBitmap);
		MarkerOptions myMarker = new MarkerOptions()
				.position(myPoint);

		//Remove all markers, polygons, overlays, etc.
		mMap.clear();
		//Add markers to global GoogleMap object
		mMap.addMarker(itemMarker);
		mMap.addMarker(myMarker);

		//Construct latitude/longitude aligned rectangle
		LatLngBounds bounds = new LatLngBounds.Builder()
				.include(itemPoint)
				.include(myPoint)
				.build();

		int margin = getResources().getDimensionPixelSize(R.dimen.map_inset_margin);

		//CameraUpdate - Holds methods to handle manipulation of map view
		//Create a new update to point view at the new LatLngBounds
		CameraUpdate update = CameraUpdateFactory.newLatLngBounds(bounds, margin);

		//Move camera to new location, with an animation
		mMap.animateCamera(update);
	}

	/*An async task that passes a location as a search parameter to Flickr*/
	private class SearchTask extends AsyncTask<Location, Void, Void> {

		private GalleryItem mGalleryItem;
		private Bitmap mBitmap;
		private Location mLocation;

		@Override
		protected Void doInBackground(Location... params) {
			mLocation = params[0];
			FlickrFetchr fetchr = new FlickrFetchr();
			List<GalleryItem> items = fetchr.searchPhotos(params[0]);

			if (items.size() == 0) {
				return null;
			}

			mGalleryItem = items.get(0);

			try {
				byte[] bytes = fetchr.getUrlBytes(mGalleryItem.getUrl());
				mBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
			} catch (IOException ioe) {
				Log.i(TAG, "Unable to download bitmap", ioe);
			}

			return null;
		}

		@Override
		public void onPreExecute() {
			mProgressDialog = new ProgressDialog(getActivity());
			mProgressDialog.setCancelable(false);
			mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			mProgressDialog.show();
		}

		@Override
		protected void onPostExecute(Void result) {
			mMapImage = mBitmap;
			mMapItem = mGalleryItem;
			mCurrentLocation = mLocation;

			if (mProgressDialog != null) {
				mProgressDialog.dismiss();
			}

			//Update UI on the completion of a search
			updateUI();
		}

	}

	//Return true/false depending on whether app was given location permission by user
	private boolean hasLocationPermission() {
		int result = ContextCompat.checkSelfPermission(getActivity(), LOCATION_PERMISSIONS[0]);

		return result == PackageManager.PERMISSION_GRANTED;
	}

}