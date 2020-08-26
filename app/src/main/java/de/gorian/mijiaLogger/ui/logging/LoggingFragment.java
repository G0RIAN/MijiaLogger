package de.gorian.mijiaLogger.ui.logging;

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

import de.gorian.mijiaLogger.R;

public class LoggingFragment extends Fragment {

    private LoggingViewModel loggingViewModel;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        loggingViewModel =
                ViewModelProviders.of(this).get(LoggingViewModel.class);
        View root = inflater.inflate(R.layout.fragment_logs, container, false);
        final TextView textView = root.findViewById(R.id.text_logs);
        loggingViewModel.getText().observe(getViewLifecycleOwner(), new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
                textView.setText(s);
            }
        });
        return root;
    }
}