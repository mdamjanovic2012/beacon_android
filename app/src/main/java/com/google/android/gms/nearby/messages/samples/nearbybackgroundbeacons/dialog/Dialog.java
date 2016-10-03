package com.google.android.gms.nearby.messages.samples.nearbybackgroundbeacons.dialog;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

/**
 * Entrance dialog.
 */
public class Dialog {

	public AlertDialog.Builder dialog = null;

	/**
	 * Initialize the dialog.
	 *
	 * @param context  The application context.
	 * @param listener The listener for buttons.
	 */
	public Dialog(Context context, final OnLocationEnabled listener) {
		dialog = new AlertDialog.Builder(context);
		dialog.setTitle("Title");
		dialog.setMessage("Message");

		dialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				listener.onLocationEnabled();
			}
		});

		dialog.show();
	}

	/**
	 * Callback interface.
	 */
	public interface OnLocationEnabled {

		/**
		 * Callback method.
		 */
		void onLocationEnabled();
	}
}
