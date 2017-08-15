package locatr.android.bignerdranch.com.locatr;

import android.content.pm.PackageManager;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.util.List;

public class LocatrFragment extends Fragment {

	private static final String TAG = "LocatrFragment";

	private static final String[] LOCATION_PERMISSIONS = new String[] {
			android.Manifest.permission.ACCESS_FINE_LOCATION,
			android.Manifest.permission.ACCESS_COARSE_LOCATION
	};

	private static final int REQUEST_LOCATION_PERMISSIONS = 0;

	private ImageView mImageView;
	//Access to Play Services
	private GoogleApiClient mClient;

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
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_locatr, container, false);

		mImageView = (ImageView) v.findViewById(R.id.image);

		return v;
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

		LocationServices.FusedLocationApi
				.requestLocationUpdates(mClient, request, new LocationListener() {
					@Override
					public void onLocationChanged(Location location) {
						Log.i(TAG, "Got a fix: " + location);
					}
				});
	}

	private class SearchTask extends AsyncTask<Location, Void, Void> {

		private GalleryItem mGalleryItem;

		@Override
		protected Void doInBackground(Location... params) {
			FlickrFetchr fetchr = new FlickrFetchr();
			List<GalleryItem> items = fetchr.searchPhotos(params[0]);

			if (items.size() == 0) {
				return null;
			}

			mGalleryItem = items.get(0);

			return null;
		}

	}

	//Return true/false depending on whether app was given location permission by user
	private boolean hasLocationPermission() {
		int result = ContextCompat.checkSelfPermission(getActivity(), LOCATION_PERMISSIONS[0]);

		return result == PackageManager.PERMISSION_GRANTED;
	}

}