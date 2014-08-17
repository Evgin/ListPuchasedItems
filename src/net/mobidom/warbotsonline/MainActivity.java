package net.mobidom.warbotsonline;

import java.util.ArrayList;
import java.util.Arrays;

import org.json.JSONObject;

import android.app.Activity;
import android.app.ListFragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.android.vending.billing.IInAppBillingService;

public class MainActivity extends Activity {

	ArrayList<String> skuList = new ArrayList<String>(Arrays.asList("chips10", "chips25", "chips55", "chips125", "chips400", "chips1000"));

	IInAppBillingService mService;

	ServiceConnection mServiceConn = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			mService = IInAppBillingService.Stub.asInterface(service);
			Log.i("XXX", "mService ready");
			mServiceReady();
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			mService = null;
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		bindService(new Intent("com.android.vending.billing.InAppBillingService.BIND"), mServiceConn, Context.BIND_AUTO_CREATE);

	}

	private void mServiceReady() {
		new ProgressTask(this, "Загрузка", "Получение данных продуктов", new Runnable() {

			@Override
			public void run() {

				Bundle querySkus = new Bundle();
				querySkus.putStringArrayList("ITEM_ID_LIST", skuList);

				Bundle skuDetails = null;
				try {
					skuDetails = mService.getSkuDetails(3, getPackageName(), "inapp", querySkus);
					int responseCode = skuDetails.getInt("RESPONSE_CODE");
					if (responseCode == 0) {
						Log.i("XXX", "skuDetail is normal");

						ArrayList<String> responseList = skuDetails.getStringArrayList("DETAILS_LIST");
						final ArrayList<JSONObject> productList = new ArrayList<JSONObject>();
						for (String productString : responseList) {
							productList.add(new JSONObject(productString));
						}

						MainActivity.this.runOnUiThread(new Runnable() {

							@Override
							public void run() {
								getFragmentManager().beginTransaction().add(R.id.container, new ProductsListFragment(productList)).commit();
							}
						});

					} else {
						throw new Exception("response code = " + responseCode);
					}
				} catch (Exception e) {
					Log.e("XXX", "cant get skuDetails", e);

					MainActivity.this.runOnUiThread(new Runnable() {

						@Override
						public void run() {
							Toast.makeText(MainActivity.this, "Ошибка загрузки", Toast.LENGTH_SHORT).show();
						}
					});
				}
			}
		}).execute();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (mService != null) {
			unbindService(mServiceConn);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	public static class ProductsListFragment extends ListFragment {

		ArrayList<JSONObject> productList;

		public ProductsListFragment(ArrayList<JSONObject> productList) {
			this.productList = productList;
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			setListAdapter(new ProductListAdapter(getActivity(), productList));
			return super.onCreateView(inflater, container, savedInstanceState);
		}
	}

	public static class ProductListAdapter extends ArrayAdapter<JSONObject> {

		ArrayList<JSONObject> items;

		public ProductListAdapter(Context context, ArrayList<JSONObject> products) {
			super(context, R.layout.product_item);
			this.items = products;
		}

		@Override
		public int getCount() {
			return items.size();
		}

		@Override
		public JSONObject getItem(int position) {
			return items.get(position);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v = null;
			if (convertView == null) {
				convertView = View.inflate(getContext(), R.layout.product_item, null);
			}
			v = convertView;

			JSONObject obj = getItem(position);

			TextView text = (TextView) v.findViewById(R.id.info);
			try {
				text.setText(String.format("id: %s\n" + "type: %s\n" + "price: %s\n" + "title: %s\n" + "descr: %s", obj.get("productId"),
						obj.get("type"), obj.get("price"), obj.get("title"), obj.get("description")));
			} catch (Exception e) {
				Log.e("XXX", "", e);
				text.setText(e.toString());
			}

			return v;
		}
	}

}
