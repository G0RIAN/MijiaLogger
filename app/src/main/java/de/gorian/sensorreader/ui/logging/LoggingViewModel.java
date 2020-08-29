package de.gorian.sensorreader.ui.logging;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class LoggingViewModel extends ViewModel {

	private MutableLiveData<String> mText;

	public LoggingViewModel() {
		mText = new MutableLiveData<>();
		mText.setValue("This is logging fragment");
	}

	public LiveData<String> getText() {
		return mText;
	}
}