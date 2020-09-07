package de.gorian.sensorreader.ui.logging;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import de.gorian.sensorreader.R;

public class LoggingFragment extends Fragment {

	public View onCreateView(@NonNull LayoutInflater inflater,
	                         ViewGroup container, Bundle savedInstanceState) {
		new ViewModelProvider(this).get(LoggingViewModel.class);
		//		final TextView textView = root.findViewById(R.id.text_logging);
//		loggingViewModel.getText().observe(getViewLifecycleOwner(), new Observer<String>() {
//			@Override
//			public void onChanged(@Nullable String s) {
//				textView.setText(s);
//			}
//		});
		return inflater.inflate(R.layout.fragment_logging, container, false);
	}
}