package com.grs;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import javax.servlet.ServletException;

import com.dl.ITextCallback;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.homeai.AiServiceGrpc.AiServiceImplBase;
import com.ibm.watson.developer_cloud.conversation.v1.model.Entity;
import com.klab.ctx.ConversationLogInfo;
import com.klab.ctx.ConversationSession;
import com.klab.ctx.SessionManager;
import com.klab.svc.AppsPropertiy;
import com.klab.svc.ConsoleLogger;
import com.klab.svc.ConversationLogger;
import com.klab.svc.SimpleAppFrame;
import com.svc.Message;
import com.svc.MqttAgent;
import com.homeai.AiServiceReply;
import com.homeai.AiServiceRequest;

import io.grpc.stub.StreamObserver;

/**
 * @author 최의신 (dreamsalmon@gmail.com)
 *
 */
public class AiServiceImpl extends AiServiceImplBase
{
	public static final String CRLF = "\n";
	private static final String ATTR_NAME = "Conversation";
	
	private SimpleAppFrame appFrame;
	private ConversationLogger convLogger;
	private boolean debug = false;
	
	private MqttAgent mqttAgent;
	private BlockingQueue<Message> toWorker = new ArrayBlockingQueue<Message>(100);
	private BlockingQueue<Message> fromWorker = new ArrayBlockingQueue<Message>(100);

	public AiServiceImpl()
	{
		debug = "true".equals(AppsPropertiy.getInstance().getProperty("wcs.debug"));
		
		appFrame = new SimpleAppFrame();
		appFrame.setUsername(AppsPropertiy.getInstance().getProperty("wcs.user"));
		appFrame.setPassword(AppsPropertiy.getInstance().getProperty("wcs.passwd"));
		appFrame.setWorkspaceId(AppsPropertiy.getInstance().getProperty("wcs.workid"));
		
		/*
		 * 대화를 저장할 로거를 생성
		 */
		convLogger = new ConversationLogger();
		
//		String logger = AppsPropertiy.getInstance().getProperty("logger.className");
//		if ( logger != null && logger.length() > 0 )
//		{
//			try {
//				convLogger.setLogger((ILogger)Utils.loadClass(logger));
//			} catch (Exception e) {
//				convLogger.setLogger(new ConsoleLogger());
//			}
//		}
//		else {
//			convLogger.setLogger(new ConsoleLogger());
//		}
		
		convLogger.setLogger(new ConsoleLogger());
		convLogger.start();
		
		mqttAgent = new MqttAgent(toWorker, fromWorker);
		mqttAgent.start();		
	}
	
	@Override
	public void say(AiServiceRequest request, StreamObserver<AiServiceReply> responseObserver)
	{
		JsonObject payload = (new JsonParser()).parse(request.getPayload()).getAsJsonObject();
		
		String res = process(payload);
		AiServiceReply reply = AiServiceReply.newBuilder().setPayload(res).build();
		responseObserver.onNext(reply);
		responseObserver.onCompleted();
	}
	
	
	/**
	 * @param req
	 * @return
	 */
	private String process(JsonObject req)
	{
		JsonObject result = new JsonObject();
		
		try
		{
			ConversationSession session = SessionManager.getInstance().getSession(ATTR_NAME);
			doConversation(req, result, session);
		}catch(Exception e) {
			result.addProperty("returnCode", "FAIL");
			result.addProperty("errorString", "EMPTY TEXT");
		}

		return result.toString();
	}
	
	/**
	 * @param request
	 * @param response
	 * @throws ServletException
	 * @throws IOException
	 */
	private void doConversation(JsonObject request, JsonObject result, final ConversationSession session) throws ServletException, IOException
	{
		String text = request.get("request").getAsJsonObject().get("text").getAsString();

		if ( text == null || text.length() == 0 )
		{
			result.addProperty("returnCode", "FAIL");
			result.addProperty("errorString", "EMPTY TEXT");
		}
		else {
			try
			{
				session.addProperty("NEWLINE", CRLF);
				
				if ( "__INIT__".equals(text) ) {
					processText(session, true, null, new ITextCallback() {
						@Override
						public void setResult(String resText, Object actionResult, JsonElement postResult)
						{
							result.addProperty("returnCode", "OK");
							result.addProperty("errorString", "");
							JsonArray ja = new JsonArray();
							result.add("response", ja);
							addSpeak(ja, resText);
						}
					});
				}
				else {
					processText(session, false, text, new ITextCallback() {
						@Override
						public void setResult(String resText, Object actionResult, JsonElement postResult)
						{
							//String target = getEntityValue(session, "target");
							String action = getEntityValue(session, "action");
							
							JsonObject jo = (JsonObject)actionResult;
							JsonArray ja = new JsonArray();
							result.add("response", ja);

							//if ( "music".equals(target) && "play".equals(action) ) {
							if ( "play".equals(action) || "turnon".equals(action) || "turnoff".equals(action) || "up".equals(action) || "down".equals(action) )
							{
								if ( jo.has("MUSIC_URL") )
								{
									//addMusic(ja, jo.get("MUSIC_URL").getAsString(), jo.get("AUTH_TOKEN").getAsString());
									addMusic(ja, resText, jo.get("AUTH_TOKEN").getAsString());
								}
								else if ( jo.has("MUSIC_MESSAGE") ) {
									addSpeak(ja, jo.get("MUSIC_MESSAGE").getAsString());
								}
								else if ( jo.has("CONTROL_DEVICE") )
								{
									JsonPrimitive o = (JsonPrimitive)jo.get("CONTROL_DEVICE");
									
									JsonParser jp = new JsonParser();
									JsonElement jobj = jp.parse(o.getAsString());
									JsonArray list = jobj.getAsJsonArray();

									for(int i = 0; i < list.size(); i++)
									{
										JsonObject obj = list.get(i).getAsJsonObject();
										
										String topic = obj.get("TOPIC").getAsString();
										String payload = obj.get("PAYLOAD").toString();
										
										Message bus = new Message();
										bus.setTopic(topic);
										bus.setPayload(payload);			
										try {
											fromWorker.put(bus);
											Thread.sleep(500);
										} catch (InterruptedException e) {
										}
									}
									
									addTask(ja, "COMPLETED");
								}
							}
							else {
								if ( jo.has("BREAK") )
									addTask(ja, resText);
								else
									addSpeak(ja, resText);
							}
							
							result.addProperty("returnCode", "OK");
							result.addProperty("errorString", "");
						}
					});
				}

//				if ( session.getPostResult() != null )
//					result.add("postResult", session.getPostResult());
//				
				if ( session.getWarning() != null )
					result.addProperty("warning", session.getWarning());

				if ( debug )
					result.addProperty("debug", session.getDebug());

			}catch(Exception ex) {
				ex.printStackTrace();
				result.addProperty("returnCode", "FAIL");
				result.addProperty("errorString", ex.getMessage());
			}
		}
	}
	
	/**
	 * @param session
	 * @param name
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	private String getEntityValue(ConversationSession session, String name)
	{
		String value = null;
		List list = session.getEntities();
		
		for(int i = 0; i < list.size(); i++)
		{
			Entity e = (Entity)list.get(i);
			if ( name.equals(e.getEntity()) ) {
				value = e.getValue();
				break;
			}
		}
		
		return value;
	}
	
	/**
	 * 
	 * @param ja
	 * @param text
	 */
	private void addSpeak(JsonArray ja, String text)
	{
		JsonObject obj = new JsonObject();
		obj.addProperty("action", "speak");
		obj.addProperty("data", text);
		
		ja.add(obj);
	}
	
	/**
	 * @param ja
	 * @param data
	 */
	private void addTask(JsonArray ja, String data)
	{
		JsonObject obj = new JsonObject();
		obj.addProperty("action", "task");
		obj.addProperty("data", data);
		
		ja.add(obj);
	}
	
	
//	/**
//	 * @param ja
//	 * @param data
//	 */
//	private void addActuator(JsonArray ja, String data)
//	{
//	}
//	
//	/**
//	 * @param ja
//	 * @param data
//	 */
//	private void addDisplay(JsonArray ja, String data)
//	{
//	}
	
	/**
	 * @param ja
	 * @param songUrl
	 * @param auth
	 */
	private void addMusic(JsonArray ja, String songUrl, String auth)
	{
		JsonObject obj = new JsonObject();
		obj.addProperty("action", "playMusic");
		
		JsonObject data = new JsonObject();
		data.addProperty("songUrl", songUrl);
		data.addProperty("authToken", auth);
		
		obj.add("data", data);
		
		ja.add(obj);
	}
	
//	/**
//	 * @param ja
//	 * @param data
//	 */
//	private void addVideo(JsonArray ja, String data)
//	{
//	}
	
	/**
	 * @param session
	 * @param clean
	 * @param text
	 * @param callback
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public void processText(ConversationSession session, boolean clean, String text, ITextCallback callback) throws Exception
	{
		ConversationLogInfo log = null;
		
		if ( clean ) {
			session.getContext().clear();
			log = appFrame.message(session, "");
		}
		else {
			log = appFrame.message(session, text);
		}

		String newline = "<br>";
		Object obj = session.getProperty("NEWLINE");
		if ( obj != null )
			newline = obj.toString();
		
		StringBuffer resText = new StringBuffer();
		List<String> list = session.getOutputString();
		for(int i = 0; i < list.size(); i++)
		{
			resText.append(list.get(i));
			if ( i < list.size()-1 )
				resText.append(newline);
		}
		
		if ( callback != null )
			callback.setResult(resText.toString(), session.getActionResult(), session.getPostResult());
		
		/*
		 * 대화 이력을 저장한다.
		 */
		if ( convLogger != null && log != null )
			convLogger.addDialog(log);
	}	
}
