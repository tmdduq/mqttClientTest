package osy.kcg.mqttclient

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import android.view.Menu
import android.view.MenuItem
import android.view.View
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import osy.kcg.mqttclient.databinding.ActivityMainBinding
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    private lateinit var mqttClient : MqttClient
    private lateinit var mqttConnectOptions : MqttConnectOptions

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.sendButton.setOnClickListener{
            val topic = binding.topicText.text.toString().replace("[^a-zA-Z/0-9]".toRegex(),"")
            binding.topicText.setText(topic)
            mqttClient.getTopic(topic).publish(MqttMessage(binding.sendText.text.toString().encodeToByteArray()))
        }

        binding.init.setOnClickListener{
            mqttInit()
        }

        binding.topicAdd.setOnClickListener{
            val topic = binding.topicText.text.toString()
            mqttClient.subscribe(topic)
        }
    }

    private fun mqttInit(){
        try {
            mqttClient = MqttClient(
                "tcp://broker.hivemq.com:1883",
                "clientId-7swcIgpwkQvvv",
                MemoryPersistence()
            )
            mqttConnectOptions = MqttConnectOptions()
            mqttConnectOptions.isCleanSession = true
            mqttConnectOptions.isAutomaticReconnect = true
            mqttConnectOptions.connectionTimeout = 10000
            mqttClient.connect(mqttConnectOptions)
            var msg = Message()
            var bundle = Bundle()
            bundle.putString("source", "mqttInit")
            bundle.putString("payload", "connect Success")
            msg.data = bundle
            h.handleMessage(msg)

            mqttClient.setCallback(object : MqttCallback {
                override fun connectionLost(cause: Throwable?) {
                    Log.i("connectionLost", "$cause")
                    var msg = Message()
                    var bundle = Bundle()
                    bundle.putString("source", "connectionLost")
                    bundle.putString("payload", "connection close")
                    msg.data = bundle
                    h.handleMessage(msg)
                    mqttClient.close()
                }

                override fun messageArrived(topic: String?, message: MqttMessage?) {
                    var payload = ""
                    try{
                        if (message!!.isDuplicate) return
                        payload = String(message!!.payload)
                    }catch(e:Exception){e.printStackTrace()}
                    var msg = Message()
                    var bundle = Bundle()
                    bundle.putString("source", "messageArrived")
                    bundle.putString("id", ""+message!!.id)
                    bundle.putString("topic", topic)
                    bundle.putString("payload", "$payload")
                    msg.data = bundle
                    h.handleMessage(msg)
                }

                override fun deliveryComplete(token: IMqttDeliveryToken?) {
                    if(token == null) return
                    if(token.message == null) return
                }
            })
        }catch (e:Exception) {
            e.printStackTrace()
        }
    }

    var h = object : Handler(Looper.getMainLooper()){
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            val data = msg.data
            var t = binding.textLog.text.toString()
            if(t.length > 1000) t = t.substring(0, 1000)
            var payload = data.getString("payload","")
            if(payload.length > 100) payload = payload.substring(0,100)+"..."
            val text = StringBuilder()
            text.append(" * ${data.getString("id","")}\t // ${data.getString("topic", "")}")
                .appendLine()
                .append(" ** $payload")
                .appendLine()
            Log.i("handler", "${data.getString("topic", "")} - $payload")
            setText(text.toString() + t)
        }
    }

    private fun setText(text : String){
        runOnUiThread {
            try {
                val t = binding.textLog
                t.text = text
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

