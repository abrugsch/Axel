package fr.xgouchet.xmleditor;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.widget.ExpandableListView;

import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.builder.Builders.Any.B;

import fr.xgouchet.androidlib.ui.Toaster;
import fr.xgouchet.xmleditor.network.ValidateFileTask;
import fr.xgouchet.xmleditor.network.ValidateFileTask.ValidationListener;
import fr.xgouchet.xmleditor.parser.validator.ValidatorParser;
import fr.xgouchet.xmleditor.parser.validator.ValidatorResult;
import fr.xgouchet.xmleditor.ui.adapter.ValidatorEntryAdapter;

/**
 * Idependant activity checking for Errors in XML files
 * 
 * @author xgouchet
 * 
 */
public class AxelValidatorActivity extends Activity implements
		ValidationListener, FutureCallback<String> {

	private File mCurrentFile;
	private ValidateFileTask mValidateTask;

	/**
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_PROGRESS);
		setContentView(R.layout.layout_validator);

		setProgressBarIndeterminate(true);
		setProgressBarVisibility(true);

		// Activity
		readIntent();

	}

	/**
	 * Read the intent used to start this activity (open the xml file)
	 */
	@SuppressWarnings("deprecation")
	private void readIntent() {
		Intent intent;
		String action;
		File file;

		intent = getIntent();
		if (intent == null) {
			// TODO toast error
			finish();
			return;
		}

		action = intent.getAction();
		if (action == null) {
			// TODO toast error
			finish();
			return;
		}

		if ((action.equals(Intent.ACTION_VIEW))
				|| (action.equals(Intent.ACTION_EDIT))) {
			try {
				mCurrentFile = new File(new URI(intent.getData().toString()));
				setTitle(getString(R.string.title_validator,
						mCurrentFile.getName()));
				validateCurrentFile();
			} catch (URISyntaxException e) {
				Toaster.showToast(this, R.string.toast_intent_invalid_uri, true);
				finish();
			} catch (IllegalArgumentException e) {
				Toaster.showToast(this, R.string.toast_intent_illegal, true);
				finish();
			}
		}
	}

	/**
	 * 
	 */
	private void validateCurrentFile() {

		// TODO rewrite all the network thing to get rid of 3 heavy libraries
		// mValidateTask = new ValidateFileTask();
		// mValidateTask.setListener(this);
		// mValidateTask.execute(mCurrentFile);

		B requestBuilder = Ion.with(this, ValidateFileTask.VALIDATOR_URL);
		requestBuilder.setMultipartParameter("output", "soap12");
		requestBuilder.setMultipartParameter("debug", "1");
		requestBuilder.setMultipartFile("uploaded_file", mCurrentFile);

		requestBuilder.asString().setCallback(this);
	}

	@Override
	public void onCompleted(final Exception e, final String response) {
		if (e == null) {

			InputStream input = new ByteArrayInputStream(response.getBytes());
			ValidatorResult result = null;
			try {
				Log.v("PARSER", response);
				result = ValidatorParser.parseValidatorResponse(input);
				displayResult(result);
			} catch (Exception e1) {
				Log.e("RESPONSE", "PARSE ERROR", e1);
			}

		} else {
			Log.e("RESPONSE", "REQUEST ERROR", e);
		}
	}

	@Override
	public void onValidationRequestProgress(final int progress) {
		setProgressBarIndeterminate(false);
		setProgress(progress);
	}

	private void displayResult(final ValidatorResult result) {
		setProgressBarVisibility(false);

		ExpandableListView list = (ExpandableListView) findViewById(android.R.id.list);

		ValidatorEntryAdapter adapter = new ValidatorEntryAdapter(this,
				result.getEntries());
		list.setAdapter(adapter);
	}
}
