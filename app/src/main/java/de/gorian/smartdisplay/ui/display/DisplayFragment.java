package de.gorian.smartdisplay.ui.display;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import de.gorian.smartdisplay.R;

public class DisplayFragment extends Fragment {

    private DisplayViewModel displayViewModel;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        displayViewModel =
                ViewModelProviders.of(this).get(DisplayViewModel.class);
        View root = inflater.inflate(R.layout.fragment_display, container, false);
        final TextView textView = root.findViewById(R.id.text_home);
        displayViewModel.getText().observe(getViewLifecycleOwner(), new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
                textView.setText(s);
            }
        });
        return root;
    }
}