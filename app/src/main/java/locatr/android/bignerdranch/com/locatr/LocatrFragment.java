package locatr.android.bignerdranch.com.locatr;

import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.Fragment;
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

public class LocatrFragment extends Fragment {

	private static final String TAG = "LocatrFragment";

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
				findImage();
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

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

}