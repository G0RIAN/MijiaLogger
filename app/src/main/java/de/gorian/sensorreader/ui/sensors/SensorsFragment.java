package de.gorian.sensorreader.ui.sensors;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

import de.gorian.sensorreader.R;

public class SensorsFragment extends Fragment {

	private SensorsViewModel sensorsViewModel;


	public View onCreateView(@NonNull LayoutInflater inflater,
	                         ViewGroup container, Bundle savedInstanceState) {
		sensorsViewModel =
				ViewModelProviders.of(this).get(SensorsViewModel.class);
		View root = inflater.inflate(R.layout.fragment_sensors, container, false);
//		final TextView textView = root.findViewById(R);
//		sensorsViewModel.getText().observe(getViewLifecycleOwner(), new Observer<String>() {
//			@Override
//			public void onChanged(@Nullable String s) {
//				textView.setText(s);
//			}
//		});
		return root;
	}


}