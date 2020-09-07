package de.gorian.sensorreader.ui.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import de.gorian.sensorreader.R;

public class SettingsFragment extends Fragment {

	public View onCreateView(@NonNull LayoutInflater inflater,
	                         ViewGroup container, Bundle savedInstanceState) {
		SettingsViewModel settingsViewModel = new ViewModelProvider(this).get(SettingsViewModel.class);
		View root = inflater.inflate(R.layout.fragment_settings, container, false);
		final TextView textView = root.findViewById(R.id.text_slideshow);
		settingsViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);
		return root;
	}
}