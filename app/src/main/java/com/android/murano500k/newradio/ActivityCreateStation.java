package com.android.murano500k.newradio;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

public class ActivityCreateStation extends AppCompatActivity {
	Button buttonAdd, buttonCancel;
	EditText editTextName, editTextUrl;
	PlaylistManager playlistManager;
	CheckBox checkBoxFav;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_create_station);
		buttonAdd=(Button) findViewById(R.id.buttonAdd);
		buttonCancel=(Button) findViewById(R.id.buttonCancel);
		//editTextName=(EditText) findViewById(R.id.editTextName);
		editTextUrl=(EditText) findViewById(R.id.editTextUrl);
		checkBoxFav=(CheckBox) findViewById(R.id.checkBoxFav);
		playlistManager=new PlaylistManager(getApplicationContext());

		buttonAdd.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				/*String name = editTextName.getText().toString();
				if(name==null || name.length()<1) {
					Toast.makeText(getApplicationContext(), "name shoudn't be empty", Toast.LENGTH_SHORT).show();
					return;
				}*/
				String url = editTextUrl.getText().toString();
				if(url.length()<1 || !url.contains("http")  || !url.contains("://") ) {
					Toast.makeText(getApplicationContext(), "url incorrect", Toast.LENGTH_SHORT).show();
					return;
				}
				playlistManager.addStation(url, checkBoxFav.isChecked());
				setResult(RESULT_OK);
				finish();

			}
		});
		buttonCancel.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				setResult(RESULT_CANCELED);
				finish();

			}
		});




	}
}
