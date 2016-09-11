package com.android.murano500k.newradio.ui;

import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.android.murano500k.newradio.PlaylistManager;
import com.android.murano500k.newradio.R;
import com.android.murano500k.newradio.ServiceRadioRx;

import org.greenrobot.eventbus.EventBus;

import static com.android.murano500k.newradio.ServiceRadioRx.EXTRA_AUDIO_BUFFER_CAPACITY;
import static com.android.murano500k.newradio.ServiceRadioRx.EXTRA_AUDIO_DECODE_CAPACITY;

/**
 * Created by artem on 8/23/16.
 */

public class DialogShower {
public static final String TAG="DialogShower";
	MenuItem sleepTimerMenuItem;
	private AlertDialog dialogSetBufferSize;
	PlaylistManager playlistManager;
	private int sizeBuffer, sizeDecode;

	public DialogShower(PlaylistManager plm) {
		this.playlistManager = plm;
	}

	public void showDialogSetBufferSize(Context context, LayoutInflater layoutInflater) {


			View dialogView= layoutInflater.inflate(R.layout.dialog_buffer, null);
			Button b1 = (Button) dialogView.findViewById(R.id.buttonOk);
			Button b2 = (Button) dialogView.findViewById(R.id.buttonCancel);
			EditText editBuffer = (EditText) dialogView.findViewById(R.id.editTextBuffer);
			EditText editDecode = (EditText) dialogView.findViewById(R.id.editTextDecode);

			editBuffer.setText(String.valueOf(playlistManager.getBufferSize()));
			editDecode.setText(String.valueOf(playlistManager.getDecodeSize()));
			b1.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					sizeBuffer = Integer.parseInt(editBuffer.getText().toString());
					sizeDecode = Integer.parseInt(editDecode.getText().toString());
					Log.d(TAG, "onBufferOptionClick sizeBuffer=" + sizeBuffer + " sizeDecode=" + sizeDecode);
					Intent intent = new Intent(context, ServiceRadioRx.class);
					intent.setAction(ServiceRadioRx.INTENT_SET_BUFFER_SIZE);
					intent.putExtra(EXTRA_AUDIO_BUFFER_CAPACITY, sizeBuffer);
					intent.putExtra(EXTRA_AUDIO_DECODE_CAPACITY, sizeDecode);
					context.startService(intent);
					if (dialogSetBufferSize != null && dialogSetBufferSize.isShowing())
						dialogSetBufferSize.dismiss();
				}
			});

			b2.setOnClickListener(view -> {
				if (dialogSetBufferSize != null && dialogSetBufferSize.isShowing()) {
					Toast.makeText(context, "Set buffer size cancelled", Toast.LENGTH_SHORT).show();
					dialogSetBufferSize.dismiss();

				}
			});
			dialogSetBufferSize = new AlertDialog.Builder(context)
					.setTitle("Set buffer size in ms")
					.setView(dialogView)
					.setCancelable(true)
					.create();

			dialogSetBufferSize.show();



	}
	public void showCancelTimerDialog(Context context) {
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		AlertDialog dialog = builder.setTitle("Cancel current timer?")
				.setPositiveButton("OK",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
							                    int whichButton) {
								Log.d(TAG, "try to cancel timer");

									Intent intent = new Intent();
									intent.setAction(ServiceRadioRx.INTENT_SLEEP_CANCEL);
									PendingIntent pending = PendingIntent.getService(context, 0, intent, 0);
									try {
										pending.send();
									} catch (PendingIntent.CanceledException e) {
										Log.d(TAG, "PendingIntent.CanceledException e: " + e.getMessage());
										EventBus.getDefault().post(new SleepEvent(SleepEvent.SLEEP_ACTION.CANCEL, -1));
										e.printStackTrace();
									}
							}
						}).setNegativeButton("Cancel",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
							                    int whichButton) {
								Log.d(TAG, "cancelled");
							}
						}).create();
		dialog.show();
	}

	public int selected;

	public void showSetTimerDialog(Context context) {
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		final CharSequence[] array = {"1", "15", "30", "60", "90", "120", "240"};
		AlertDialog dialog = builder.setTitle("Set sleep timer in minutes")
				.setSingleChoiceItems(array, 0, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						selected = which;

					}
				})
				.setPositiveButton("OK",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
							                    int whichButton) {
								Log.d(TAG, "try to set timer " + array[selected] + " minutes");

								int mins = Integer.parseInt(array[selected].toString());
								int secondsBeforeSleep = mins * 60;
									Intent intent = new Intent(context, ServiceRadioRx.class);
									intent.setAction(ServiceRadioRx.INTENT_SLEEP_SET);
									Log.d(TAG, "secondsBeforeSleep putExtra : " + secondsBeforeSleep);
									intent.putExtra(ServiceRadioRx.EXTRA_SLEEP_SECONDS, secondsBeforeSleep);
									PendingIntent pending = PendingIntent.getService(context, 0, intent, 0);
									try {
										pending.send();
									} catch (PendingIntent.CanceledException e) {
										Log.d(TAG, "PendingIntent.CanceledException e: " + e.getMessage());
										e.printStackTrace();
									}
							}
						}).setNegativeButton("Cancel",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
							                    int whichButton) {
								Log.d(TAG, "cancelled");
								EventBus.getDefault().post(new SleepEvent(SleepEvent.SLEEP_ACTION.CANCEL, -1));
							}
						}).create();
		dialog.show();
	}
}
