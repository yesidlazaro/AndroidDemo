package com.android.test.fragment;

import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.android.test.R;
import com.android.test.adapter.VenueAdapter;
import com.android.test.client.FoursquareClient;
import com.android.test.dialog.DialogFragmentHelper;
import com.android.test.dialog.ProgressDialogFragment;
import com.android.test.dialog.VenueDialogFragment;
import com.android.test.domain.Venue;
import com.android.test.dto.ErrorType;
import com.android.test.dto.FoursquareApiErrorDto;
import com.android.test.dto.VenueDto;
import com.android.test.location.GPSTracker;
import com.android.test.task.FoursquareAsyncTask;

import java.util.List;

/**
 * MainFragment
 * Created by nicolas on 12/22/13.
 */
public class MainFragment extends Fragment implements AdapterView.OnItemClickListener {

	private EditText editText;
	private Button searchButton;

	private ListView listView;
	private ProgressDialogFragment progressDialog;
	private VenueDialogFragment venueDialogFragment;

	private VenueAdapter venueAdapter;

	private GPSTracker gpsTracker;

	public static Fragment newInstance() {
		return new MainFragment();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_main, container, false);
		editText = (EditText) view.findViewById(R.id.fragment_main_edittext);
		searchButton = (Button) view.findViewById(R.id.fragment_main_button);
		listView = (ListView) view.findViewById(R.id.fragment_main_listview);
		listView.setOnItemClickListener(this);

		progressDialog = ProgressDialogFragment.newInstance();
		venueDialogFragment = VenueDialogFragment.newInstance();
		gpsTracker = new GPSTracker(getActivity());

		return view;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		searchButton.setOnClickListener(onClickListener);
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		Venue venue = (Venue)parent.getItemAtPosition(position);
		createVenueDialog(venue);

	}

	private void createProgressDialog(int resId) {
		Bundle arguments = new Bundle();
		arguments.putString(ProgressDialogFragment.MESSAGE, getString(resId));
		DialogFragmentHelper.show(getActivity(), progressDialog, arguments);
	}

	private void createVenueDialog(Venue venue) {
		Bundle arguments = new Bundle();
		arguments.putSerializable(VenueDialogFragment.SELECTED_VENUE, venue);
		DialogFragmentHelper.show(getActivity(), venueDialogFragment, arguments);
	}

	private View.OnClickListener onClickListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			String criteria = editText.getText().toString().trim();

			if(criteria == null || criteria.length() == 0) {
				Toast.makeText(getActivity(), R.string.edit_text_empty, Toast.LENGTH_SHORT).show();
			}else {
				// check if GPS enabled
				if(gpsTracker.canGetLocation()){
					new VenueTask(getActivity(), criteria, gpsTracker.getLocation()).execute();
				}else{
					gpsTracker.showSettingsAlert();
				}
			}
		}
	};

	@Override
	public void onDestroy() {
		super.onDestroy();
		gpsTracker.stopUsingGPS();
	}



	/**
	 * VenueTask
	 */
	public class VenueTask extends FoursquareAsyncTask<VenueDto> {

		private String criteria;
		private Location currentLocation;

		public VenueTask(Context context, String criteria, Location currentLocation) {
			super(context);
			this.criteria = criteria;
			this.currentLocation = currentLocation;
		}

		@Override
		protected void onPreExecute() throws Exception {
			super.onPreExecute();
			createProgressDialog(R.string.connecting_to_server);
		}

		@Override
		public VenueDto call() throws Exception {
			return FoursquareClient.getInstance().searchForVenues(this.criteria);
		}

		@Override
		protected void onSuccess(VenueDto venueDto) throws Exception {
			super.onSuccess(venueDto);
			List<Venue> venues = venueDto.getResponse().getVenues();

			if(venues == null && venues.size() > 0) {
				Toast.makeText(getContext(), R.string.no_results_found, Toast.LENGTH_SHORT).show();
			}else {
				venueAdapter = new VenueAdapter(venues, currentLocation);
				listView.setAdapter(venueAdapter);
			}
		}

		@Override
		protected void onApiError(FoursquareApiErrorDto errorDto) {
			if(errorDto.getMeta().getErrorType() == ErrorType.failed_geocode) {
				Toast.makeText(getContext(), R.string.no_results_found, Toast.LENGTH_SHORT).show();
			}else {
				Toast.makeText(getContext(), R.string.unknown_error, Toast.LENGTH_SHORT).show();
			}
		}

		private void closeKeyboard() {
			InputMethodManager imm = (InputMethodManager)getContext().getSystemService(
				Context.INPUT_METHOD_SERVICE);
			imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
		}

		@Override
		protected void onFinally() throws RuntimeException {
			super.onFinally();
			closeKeyboard();
			progressDialog.dismiss();
		}
	}

}
