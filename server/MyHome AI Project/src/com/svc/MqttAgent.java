package com.svc;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.klab.svc.AppsPropertiy;
import com.svc.task.ITask;
import com.svc.task.TaskEntry;
import com.utils.Utils;


/**
 * @author 최의신
 *
 */
@SuppressWarnings("rawtypes")
public class MqttAgent extends BaseThread implements MqttCallback
{
	private MqttClient mqttClient;
	
	private BlockingQueue<Message> toWorker;
	private BlockingQueue<Message> fromWorker;
	
	public MqttAgent(BlockingQueue<Message> toWk, BlockingQueue<Message> fromWk)
	{
		this.toWorker = toWk;
		this.fromWorker = fromWk;
	}

	@Override
	public void run()
	{
		try {
			active = true;
			
			connect();
			
			super.startTime = Utils.currentTime4();
			
			while(active)
			{
				try {
					Message co = fromWorker.poll(100, TimeUnit.MILLISECONDS);
					
					if (co != null )
					{
						MqttMessage mm = new MqttMessage();
						mm.setQos(2);
						mm.setPayload(co.getPayload().toString().getBytes());
						mqttClient.publish(co.getTopic(), mm);
						
						System.out.println("@.@ PUBLISH = " + co);
					}
					
					super.setError(false);
				} catch (Exception e) {
					e.printStackTrace();
					super.setError(true);
					super.setErrorString(e.getMessage());
					logger.debug("MqttReceiverThread", e);
					active = false;
				}
			};

			disconnect();
		} catch (Exception e1) {
			e1.printStackTrace();
			super.setError(true);
			super.setErrorString(e1.getMessage());
		}
		
		System.out.println("@.@ SHUTDOWN...");
	}

	/**
	 * 
	 */
	private void connect() throws Exception
	{
		AppsPropertiy conf = AppsPropertiy.getInstance();
		String broker = conf.getProperty("mqtt.server") + ":" + conf.getProperty("mqtt.port");
		MemoryPersistence persistence = new MemoryPersistence();

		mqttClient = new MqttClient(broker, conf.getProperty("client.id"), persistence);
		mqttClient.setCallback(this);

		MqttConnectOptions connOpts = new MqttConnectOptions();
	
		connOpts.setCleanSession(true);
		connOpts.setUserName(conf.getProperty("mqtt.id"));
		connOpts.setPassword(conf.getProperty("mqtt.pwd").toCharArray());
		
		//System.out.println("Connecting to broker: "+broker);
		
		mqttClient.connect(connOpts);
		
		Map<String, TaskEntry> list = getTopicList(conf);
		System.out.println("Connected.. " + list);

		for(Iterator it = list.keySet().iterator(); it.hasNext(); )
		{
			String key = it.next().toString();
			TaskEntry te = list.get(key);
			
			if ( te.getTopic().indexOf("+") == -1 ) {
				mqttClient.subscribe(te.getTopic());
				logger.debug("@.@ SUBS : " + te.getTopic());
			}
			else {
				Object ins = com.klab.svc.Utils.loadClass(te.getTaskName());
				te.setInstance((ITask)ins);
				mqttClient.subscribe(te.getTopic(), (IMqttMessageListener)ins);
				logger.debug("@.@ SUBS : " + te.getTopic());
			}
		}
		
		logger.debug("MQTT Connected.");
	}
	
	/**
	 * @param conf
	 * @return
	 */
	private Map<String, TaskEntry> getTopicList(AppsPropertiy conf)
	{
		Map<String, TaskEntry> topic = new HashMap<String, TaskEntry>();
		
		for(Iterator it = conf.keySet().iterator(); it.hasNext(); )
		{
			String key = it.next().toString();
			if ( key.startsWith("topic.") )
			{
				String val = conf.getProperty(key);
				String [] tok = val.split(",");
				
				TaskEntry te = new TaskEntry();
				te.setTaskName(tok[1]);
				te.setTopic(tok[0]);
				
				topic.put(te.getTopic(), te);
			}
		}
		
		return topic;
	}
	
	/**
	 * 
	 */
	private void disconnect()
	{
		if ( mqttClient == null )
			return;
		
//		try {
//			Map<String, TaskEntry> list = ServerPropertiy.getInstance().getTopicList();
//			for(Iterator it = list.keySet().iterator(); it.hasNext(); )
//			{
//				String key = it.next().toString();
//				TaskEntry te = list.get(key);
//				mqttClient.unsubscribe(te.getTopic());
//			}
//		} catch (Exception ex) {
//			System.out.println("An unexpected exception has occurred.");
//			super.setError(true);
//			super.setErrorString(ex.getMessage());
//		} finally {
//			try{
//				mqttClient.disconnect();
//			}catch(Exception ig) {}
//		}				
	}	
	
	@Override
	public String getIdentity() {
		return "MQTT Receiver";
	}

	@Override
	public String getStartTime() {
		return startTime;
	}

	@Override
	public void connectionLost(Throwable t) {
		System.out.println(t);
	}

	@Override
	public void deliveryComplete(IMqttDeliveryToken t) {
		System.out.println(t);
	}

	@Override
	public void messageArrived(String topic, MqttMessage msg) throws Exception
	{
		JsonParser jp = new JsonParser();
		JsonObject payload = jp.parse(msg.toString()).getAsJsonObject();

		Message mg = new Message();
		mg.setTopic(topic);
		mg.setPayload(payload);
		
		logger.debug("[RECV-LocalMqttThread-" + topic + "] " + payload);
		//System.out.println("[RECV-LocalMqttThread-" + topic + "] " + payload);
		
		toWorker.put(mg);
	}
}
