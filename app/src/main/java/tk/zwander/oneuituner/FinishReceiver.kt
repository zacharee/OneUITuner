package tk.zwander.oneuituner

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import tk.zwander.oneuituner.util.WorkaroundInstaller
import tk.zwander.oneuituner.util.completionIntent

class FinishReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            WorkaroundInstaller.ACTION_FINISHED -> {
                val compIntent = completionIntent
                compIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                compIntent.putExtras(intent.extras!!)

                context.startActivity(compIntent)
            }
        }
    }
}
