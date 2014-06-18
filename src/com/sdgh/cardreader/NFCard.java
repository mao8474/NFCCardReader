/* NFCard is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 3 of the License, or
(at your option) any later version.

NFCard is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with Wget.  If not, see <http://www.gnu.org/licenses/>.

Additional permission under GNU GPL version 3 section 7 */

package com.sdgh.cardreader;

import org.xml.sax.XMLReader;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.ClipboardManager;
import android.text.Editable;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.widget.Toast;

import com.sdgh.cardreader.card.CardManager;

public final class NFCard extends Activity implements OnClickListener,
		Html.ImageGetter, Html.TagHandler {
	private NfcAdapter nfcAdapter;
	private PendingIntent pendingIntent;
	private TextView board;

	private enum ContentType {
		HINT, DATA, MSG
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.nfcard);

		board = (TextView)findViewById(R.id.board);

		findViewById(R.id.btnCopy).setOnClickListener(this);
		findViewById(R.id.btnNfc).setOnClickListener(this);
		findViewById(R.id.btnExit).setOnClickListener(this);

		board.setMovementMethod(LinkMovementMethod.getInstance());
		board.setFocusable(false);
		board.setClickable(false);
		board.setLongClickable(false);

		nfcAdapter = NfcAdapter.getDefaultAdapter(this);
		pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this,
				getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

		onNewIntent(getIntent());
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.clear:
			showData(null);
			return true;
		case R.id.help:
			showHelp(R.string.info_help);
			return true;
		case R.id.about:
			showHelp(R.string.info_about);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	protected void onPause() {
		super.onPause();

		if (nfcAdapter != null)
			nfcAdapter.disableForegroundDispatch(this);
	}

	@Override
	protected void onResume() {
		super.onResume();

		if (nfcAdapter != null)
			nfcAdapter.enableForegroundDispatch(this, pendingIntent,
					CardManager.FILTERS, CardManager.TECHLISTS);

		refreshStatus();
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);

		final Parcelable p = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
		Log.d("NFCTAG", intent.getAction());
		showData((p != null) ? CardManager.load(p, getResources()) : null);
	}

	@Override
	public void onClick(final View v) {
		switch (v.getId()) {
		case R.id.btnCopy: {
			copyData();
			break;
		}
		case R.id.btnNfc: {
			startActivityForResult(new Intent(
					android.provider.Settings.ACTION_WIRELESS_SETTINGS), 0);
			break;
		}
		case R.id.btnExit: {
			finish();
			break;
		}
		}
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		refreshStatus();
	}

	private void refreshStatus() {
		final String tip;
		if (nfcAdapter == null)
			tip = getResources().getString(R.string.tip_nfc_notfound);
		else if (nfcAdapter.isEnabled())
			tip = getResources().getString(R.string.tip_nfc_enabled);
		else
			tip = getResources().getString(R.string.tip_nfc_disabled);

		final StringBuilder s = new StringBuilder(
				getResources().getString(R.string.app_name));

		s.append("  --  ").append(tip);
		setTitle(s);

		final CharSequence text = board.getText();
		if (text == null || board.getTag() == ContentType.HINT)
			showHint();
	}

	private void copyData() {
		final CharSequence text = board.getText();
		if (text == null || board.getTag() != ContentType.DATA)
			return;

		((ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE))
				.setText(text);

		final String msg = getResources().getString(R.string.msg_copied);
		final Toast toast = Toast.makeText(this, msg, Toast.LENGTH_SHORT);
		toast.setGravity(Gravity.CENTER, 0, 0);
		toast.show();
	}

	private void showData(String data) {
		if (data == null || data.length() == 0) {
			showHint();
			return;
		}

		
		int padding = getResources().getDimensionPixelSize(R.dimen.pnl_margin);
		
		board.setPadding(padding, padding, padding, padding);
		board.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL));
		board.setTextSize(getResources().getDimension(R.dimen.text_small));
		board.setTextColor(getResources().getColor(R.color.text_default));
		board.setGravity(Gravity.NO_GRAVITY);
		board.setTag(ContentType.DATA);
		board.setText(Html.fromHtml(data));
	}

	private void showHelp(int id) {
		final TextView board = this.board;
		
		final int padding = getResources().getDimensionPixelSize(R.dimen.pnl_margin);
		
		board.setPadding(padding, padding, padding, padding);
		board.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL));
		board.setTextSize(getResources().getDimension(R.dimen.text_small));
		board.setTextColor(getResources().getColor(R.color.text_default));
		board.setGravity(Gravity.NO_GRAVITY);
		board.setTag(ContentType.MSG);
		board.setText(Html.fromHtml(getResources().getString(id), this, this));
	}

	private void showHint() {
		TextView board = this.board;
		Resources res = getResources();
		final String hint;
		
		if (nfcAdapter == null)
			hint = res.getString(R.string.msg_nonfc);
		else if (nfcAdapter.isEnabled())
			hint = res.getString(R.string.msg_nocard);
		else
			hint = res.getString(R.string.msg_nfcdisabled);
		
		final int padding = res.getDimensionPixelSize(R.dimen.text_middle);
		
		board.setPadding(padding, padding, padding, padding);
		board.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD_ITALIC));
		board.setTextSize(res.getDimension(R.dimen.text_middle));
		board.setTextColor(res.getColor(R.color.text_tip));
		board.setGravity(Gravity.CENTER_VERTICAL);
		board.setTag(ContentType.HINT);
		board.setText(Html.fromHtml(hint));
	}

	@Override
	public void handleTag(boolean opening, String tag, Editable output,
			XMLReader xmlReader) {
		if (!opening && "version".equals(tag)) {
			try {
				output.append(getPackageManager().getPackageInfo(
						getPackageName(), 0).versionName);
			} catch (NameNotFoundException e) {
			}
		}
	}

	@Override
	public Drawable getDrawable(String source) {
		final Resources r = getResources();

		final Drawable ret;
		final String[] params = source.split(",");
		if ("icon_main".equals(params[0])) {
			ret = r.getDrawable(R.drawable.ic_app_main);
		} else {
			ret = null;
		}

		if (ret != null) {
			final float f = r.getDisplayMetrics().densityDpi / 72f;
			final int w = (int) (Util.parseInt(params[1], 10, 16) * f + 0.5f);
			final int h = (int) (Util.parseInt(params[2], 10, 16) * f + 0.5f);
			ret.setBounds(0, 0, w, h);
		}

		return ret;
	}
}
