package com.android.murano500k.newradio;

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

import static com.vk.sdk.VKUIHelper.getApplicationContext;

/**
 * Created by artem on 8/23/16.
 */

public class DialogShower {
public static final String TAG="DialogShower";
	MenuItem sleepTimerMenuItem;
	Context context;
	private AlertDialog dialogSetBufferSize;
	PlaylistManager playlistManager;
	private int sizeBuffer, sizeDecode;

	public DialogShower(Context context, PlaylistManager plm) {
		this.context = context;
		this.playlistManager = plm;
	}

	public void showDialogSetBufferSize(LayoutInflater layoutInflater) {


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
					Intent intent = new Intent(getApplicationContext(), ServiceRadio.class);
					intent.setAction(Constants.INTENT.SET_BUFFER_SIZE);
					intent.putExtra(Constants.DATA_AUDIO_BUFFER_CAPACITY, sizeBuffer);
					intent.putExtra(Constants.DATA_AUDIO_DECODE_CAPACITY, sizeDecode);
					context.startService(intent);
					if (dialogSetBufferSize != null && dialogSetBufferSize.isShowing())
						dialogSetBufferSize.dismiss();
				}
			});

			b2.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					if (dialogSetBufferSize != null && dialogSetBufferSize.isShowing()) {
						Toast.makeText(getApplicationContext(), "Set buffer size cancelled", Toast.LENGTH_SHORT).show();
						dialogSetBufferSize.dismiss();
					}
				}
			});
			dialogSetBufferSize = new AlertDialog.Builder(context)
					.setTitle("Set buffer size in ms")
					.setView(dialogView)
					.setCancelable(true)
					.create();
			dialogSetBufferSize.show();



	}
	public void showCancelTimerDialog(MenuItem menuItem) {
		sleepTimerMenuItem=menuItem;
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		AlertDialog dialog = builder.setTitle("Cancel current timer?")
				.setPositiveButton("OK",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
							                    int whichButton) {
								Log.d(TAG, "try to cancel timer");

									Intent intent = new Intent();
									intent.setAction(Constants.INTENT.SLEEP.CANCEL);
									PendingIntent pending = PendingIntent.getService(getApplicationContext(), 0, intent, 0);
									menuItem.setChecked(false);
									try {
										pending.send();
									} catch (PendingIntent.CanceledException e) {
										Log.d(TAG, "PendingIntent.CanceledException e: " + e.getMessage());
										e.printStackTrace();
										menuItem.setChecked(true);
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

	public void showSetTimerDialog(MenuItem menuItem) {
		sleepTimerMenuItem=menuItem;
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
									Intent intent = new Intent(getApplicationContext(), ServiceRadio.class);
									intent.setAction(Constants.INTENT.SLEEP.SET);
									Log.d(TAG, "secondsBeforeSleep putExtra : " + secondsBeforeSleep);
									intent.putExtra(Constants.DATA_SLEEP_TIMER_LEFT_SECONDS, secondsBeforeSleep);
									PendingIntent pending = PendingIntent.getService(getApplicationContext(), 0, intent, 0);
									sleepTimerMenuItem.setChecked(true);
									try {
										pending.send();
									} catch (PendingIntent.CanceledException e) {
										Log.d(TAG, "PendingIntent.CanceledException e: " + e.getMessage());
										e.printStackTrace();
										sleepTimerMenuItem.setChecked(false);
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
}
