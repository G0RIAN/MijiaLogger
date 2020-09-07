package de.gorian.sensorreader.ui.sensors;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import de.gorian.sensorreader.R;

public class SensorsFragment extends Fragment {


	public View onCreateView(@NonNull LayoutInflater inflater,
	                         ViewGroup container, Bundle savedInstanceState) {
		new ViewModelProvider(this).get(SensorsViewModel.class);
		//		final TextView textView = root.findViewById(R);
//		sensorsViewModel.getText().observe(getViewLifecycleOwner(), new Observer<String>() {
//			@Override
//			public void onChanged(@Nullable String s) {
//				textView.setText(s);
//			}
//		});
		return inflater.inflate(R.layout.fragment_sensors, container, false);
	}


}