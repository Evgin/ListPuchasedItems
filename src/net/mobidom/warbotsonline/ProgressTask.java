package net.mobidom.warbotsonline;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;

public class ProgressTask extends AsyncTask<Void, Void, Void> {

	ProgressDialog dialog;
	Runnable task;

	public ProgressTask(Context ctx, String title, String msg, Runnable task) {
		dialog = new ProgressDialog(ctx);
		dialog.setTitle(title);
		dialog.setMessage(msg);
		dialog.setCancelable(false);
		dialog.setCanceledOnTouchOutside(false);
		dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);

		this.task = task;
	}

	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		dialog.show();
	}

	@Override
	protected Void doInBackground(Void... params) {
		task.run();
		return null;
	}

	@Override
	protected void onPostExecute(Void result) {
		super.onPostExecute(result);
		if (dialog.isShowing())
			dialog.dismiss();
	}

}
