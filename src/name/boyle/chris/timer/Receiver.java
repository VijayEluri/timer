/*
 * Copyright (C) 2010 Chris Boyle
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package name.boyle.chris.timer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;

public class Receiver extends BroadcastReceiver
{
	protected static String ACTION_ALARM = "name.boyle.chris.timer.ALARM";

	@Override
	public void onReceive(Context context, Intent intent)
	{
		String action = intent.getAction();
		if (action.equals(Intent.ACTION_BOOT_COMPLETED) || action.equals(Intent.ACTION_PACKAGE_REPLACED)) {
			// Alarms don't survive reboots or upgrades, so restore them all
			TimerDB db = new TimerDB(context);
			db.open();
			Cursor c = db.getAllEntries();
			if (c.moveToFirst()) {
				do {
					Timer t = db.cursorToEntry(c);
					if (! t.enabled) continue;
					t.setNextAlarm(context);
				} while (c.moveToNext());
			}
			db.close();
		} else if (action.equals(ACTION_ALARM)) {
			// It's time to sound/show an alarm
			long id = intent.getLongExtra(TimerDB.KEY_ID, -1);
			if (id < 0) return;
			TimerDB db = new TimerDB(context);
			db.open();
			Timer t = db.getEntry(id);
			boolean needSave = t.nightNext;  // a one-time flag is about to be cleared
			t.notify(context);
			if (! t.shouldWait()) {
				t.reset();
				needSave = true;  // to save new alarm time
				t.setNextAlarm(context);
				// ticker should pick up the change
			}
			if (needSave) db.saveEntry(t);
			db.close();
		}
	}

}
