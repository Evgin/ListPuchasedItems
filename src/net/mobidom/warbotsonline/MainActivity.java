package net.mobidom.warbotsonline;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ListFragment;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
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
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.vending.billing.IInAppBillingService;

public class MainActivity extends Activity {

	ArrayList<String> skuList = new ArrayList<String>(Arrays.asList("chips10", "chips25", "chips55", "chips125", "chips400", "chips1000"));

	static IInAppBillingService mService;

	ServiceConnection mServiceConn = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			mService = IInAppBillingService.Stub.asInterface(service);
			Log.i("XXX", "mService ready");
			loadProducts();
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

	private void loadPurchases() {
		Log.i("XXX", "load purchases");

		setTitle(R.string.purchases);

		new ProgressTask(this, "Загрузка", "Получение данных о покупках", new Runnable() {

			@Override
			public void run() {
				try {
					Bundle purchases = mService.getPurchases(3, getPackageName(), "inapp", null);
					int responseCode = purchases.getInt("RESPONSE_CODE");
					if (responseCode == 0) {
						Log.i("XXX", "purchasesData is normal");

						ArrayList<String> purchasesStringList = purchases.getStringArrayList("INAPP_PURCHASE_DATA_LIST");
						final ArrayList<JSONObject> purchaseItems = new ArrayList<JSONObject>();

						for (String str : purchasesStringList) {
							purchaseItems.add(new JSONObject(str));
						}
						MainActivity.this.runOnUiThread(new Runnable() {

							@Override
							public void run() {
								getFragmentManager().beginTransaction()
										.replace(R.id.container, new PListFragment(new PurchasesListAdapter(MainActivity.this, purchaseItems))).commit();
								setTitle(R.string.purchases);
							}
						});
					} else {
						throw new Exception("response code = " + responseCode);
					}
				} catch (Exception e) {
					Log.e("XXX", "", e);
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

	private void loadProducts() {

		Log.i("XXX", "load products");

		setTitle(R.string.products);

		new ProgressTask(this, "Загрузка", "Получение данных о продуктах", new Runnable() {

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
								getFragmentManager().beginTransaction()
										.replace(R.id.container, new PListFragment(new ProductListAdapter(MainActivity.this, productList))).commit();
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
		int id = item.getItemId();
		if (id == R.id.show_products) {
			loadProducts();
			return true;
		}

		if (id == R.id.show_purchases) {
			loadPurchases();
			return true;
		}

		if (id == R.id.buy_chips10) {
			try {
				Bundle buyIntentBundle = mService.getBuyIntent(3, getPackageName(), "chips10", "inapp", "evgin_test");
				int responseCode = buyIntentBundle.getInt("BILLING_RESPONSE_RESULT_OK");
				if (responseCode == 0) {
					PendingIntent pendingIntent = buyIntentBundle.getParcelable("BUY_INTENT");
					startIntentSenderForResult(pendingIntent.getIntentSender(), 1001, new Intent(), Integer.valueOf(0), Integer.valueOf(0), Integer.valueOf(0));
				} else {
					throw new Exception("cant buy product responseCode = " + responseCode);
				}
			} catch (Exception e) {
				Log.e("XXX", "cant buy product", e);
			}
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == 1001) {
			Log.i("XXX", "returned activity result");
			if (resultCode == Activity.RESULT_OK) {

			} else {
				Log.e("XXX", "resultCode = " + resultCode);
				Toast.makeText(this, "resultCode = " + resultCode, Toast.LENGTH_SHORT).show();
			}
		}

		super.onActivityResult(requestCode, resultCode, data);
	}

	public static class PListFragment extends ListFragment {

		ArrayAdapter<JSONObject> listAdapter;

		public PListFragment(ArrayAdapter<JSONObject> listAdapter) {
			this.listAdapter = listAdapter;
		}

		@Override
		public void onListItemClick(ListView l, View v, final int position, long id) {
			super.onListItemClick(l, v, position, id);
			if (l.getAdapter() instanceof PurchasesListAdapter) {
				final PurchasesListAdapter adapter = (PurchasesListAdapter) l.getAdapter();

				final AlertDialog.Builder builder = new Builder(getActivity());
				builder.setTitle("Предупреждение");
				builder.setMessage("Доставить покупку?");
				builder.setPositiveButton("ДА", new OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						JSONObject obj = adapter.items.get(position);
						String purchaseToken = null;
						try {
							purchaseToken = obj.getString("purchaseToken");
							mService.consumePurchase(3, getActivity().getPackageName(), purchaseToken);
						} catch (Exception e) {
							Log.e("XXX", "cant consume purchase" + (purchaseToken != null ? purchaseToken : "unknown"), e);
						}
					}
				});
				builder.setNegativeButton("нет", null);

				getActivity().runOnUiThread(new Runnable() {

					@Override
					public void run() {
						builder.show();
					}
				});
			}
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			setListAdapter(listAdapter);
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
				text.setText(String.format("id: %s\n" + "type: %s\n" + "price: %s\n" + "title: %s\n" + "descr: %s", obj.get("productId"), obj.get("type"),
						obj.get("price"), obj.get("title"), obj.get("description")));
			} catch (Exception e) {
				Log.e("XXX", "", e);
				text.setText(e.toString());
			}

			return v;
		}
	}

	public static class PurchasesListAdapter extends ArrayAdapter<JSONObject> {

		ArrayList<JSONObject> items;

		public PurchasesListAdapter(Context context, ArrayList<JSONObject> products) {
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
				text.setText(String.format("orderId: %s\n" + "productId: %s\n" + "purchaseTime: %s\n" + "purchaseState: %s\n" + "token: %s",
						obj.get("orderId"), obj.get("productId"), new Date(obj.getLong("purchaseTime")), getStateString(obj.getInt("purchaseState")),
						obj.get("purchaseToken")));
			} catch (Exception e) {
				Log.e("XXX", "", e);
				text.setText(e.toString());
			}

			return v;
		}

		private String getStateString(int state) {
			switch (state) {
			case 0:
				return "purchased";
			case 1:
				return "canceled";
			case 2:
				return "refunded";
			default:
				break;
			}

			return "undefined: " + state;
		}

	}

}
