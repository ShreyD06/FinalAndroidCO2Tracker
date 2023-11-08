package com.shreyd.co2tracker

//import io.karn.notify.Notify
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.text.SpannableString
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.view.Gravity
import android.widget.Toast
import com.google.android.gms.location.ActivityTransitionEvent
import com.google.android.gms.location.ActivityTransitionResult
import com.google.android.gms.location.DetectedActivity
import org.greenrobot.eventbus.EventBus
import java.text.SimpleDateFormat
import java.util.*

class ActivityTransitionReceiver : BroadcastReceiver() {

    var ChannelID = "channel1"

    class OnReceiverEvent(private val data: List<String>) {
        private val event1 = data

        fun getEvents(): List<String> {
            return event1
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        println("RECEIVED")
        println(ActivityTransitionResult.extractResult(intent))
        println(intent.extras)
        if (ActivityTransitionResult.hasResult(intent)) {
            val result = ActivityTransitionResult.extractResult(intent)
            println(result?.transitionEvents)
            result?.let {
                result.transitionEvents.forEach { event ->
                    // Info about activity
                    val info = listOf(ActivityTransitionsUtil.toActivityString(event.activityType), ActivityTransitionsUtil.toTransitionType(event.transitionType))
                    val SInfo = "Transition: " + ActivityTransitionsUtil.toActivityString(event.activityType) +
                            " (" + ActivityTransitionsUtil.toTransitionType(event.transitionType) + ")" + "   " +
                            SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
                    val otherSInfo = ActivityTransitionsUtil.toActivityString(event.activityType) +
                            " (" + ActivityTransitionsUtil.toTransitionType(event.transitionType) + ")"

                    println("This is Shrey ${ActivityTransitionsUtil.toActivityString(event.activityType)}")




                    if (ActivityTransitionsUtil.toActivityString(event.activityType) == "IN VEHICLE") {
                        var check = 0
                        val spannableString = SpannableString(otherSInfo)
                        spannableString.setSpan(
                            ForegroundColorSpan(Color.BLACK), 0, spannableString.length, 0
                        )
                        spannableString.setSpan(
                            AbsoluteSizeSpan(100), 0, spannableString.length, 0
                        )
                        val toast = Toast.makeText(context, otherSInfo, Toast.LENGTH_LONG)

                        println("CALLING TOAST")
                        toast.show()

                        //print statement is for debugging purposes
                        println(check)
                        println("sent")
                        EventBus.getDefault().post(OnReceiverEvent(info))
                    }
                }
            }
        }

    }

}