package com.mycode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ibm.watson.developer_cloud.conversation.v1.model.Entity;
import com.klab.ctx.ConversationSession;
import com.utils.SqlSessionManager;

/**
 * @author 최의신 (dreamsalmon@gmail.com)
 *
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class DeviceControlAction extends HomeAction
{
	
	/* (non-Javadoc)
	 * @see com.klab.svc.BaseAction#execute(java.lang.String, java.util.Map)
	 */
	
	@Override
	protected Object execute(String actionId, Map params, ConversationSession session)
	{
		Object exeResult = null;

		Entity target = findEntity(session, "target");
		Entity action = findEntity(session, "action");
		Entity number = findEntity(session, "sys-number");
		Entity channel = findEntity(session, "channel");
		
		if (target != null )
			System.out.println(">> DEV_CNTR[target] " + target.getEntity() + "/" + target.getValue() );
		if (action != null )
			System.out.println(">> DEV_CNTR[action] " + action.getEntity() + "/" + action.getValue() );
		if ( number != null )
			System.out.println(">> DEV_CNTR[number] " + number.getEntity() + "/" + number.getValue() );
		if ( channel != null )
			System.out.println(">> DEV_CNTR[channel] " + channel.getEntity() + "/" + channel.getValue() );
		
		if ( action != null && target != null && ("tv".equals(target.getValue()) || "volumn".equals(target.getValue())) )
		{
			exeResult = controlDevice(action, target);
		}
		else if ( action != null && (number != null || channel != null) ) {
			exeResult = changeChannel(action, channel, number);
		}
		else {
			Map result = new HashMap();
			result.put("MESSAGE", "제어 대상이 없습니다.");
			exeResult = result;				
		}

		return exeResult;
	}
	

	/**
	 * @param action
	 * @param number
	 * @return
	 */
	private Object changeChannel(Entity action, Entity channel, Entity number)
	{
		Object exeResult = null;
		
		try
		{
			Map parm = new HashMap();
			parm.put("actionVal", action.getValue());
			
			if ( channel != null )
				parm.put("targetVal", channel.getValue());
			else
				parm.put("targetVal", "tv");
			
			JsonObject root = new JsonObject();
			JsonArray cntrList = new JsonArray();
			root.add("devControl", cntrList);
			
			List devList = SqlSessionManager.getSqlMapClient().queryForList("MYHOME.selectCommandInfo", parm);
			
			for(int ix = 0; ix < devList.size(); ix++)
			{
				Map info = (Map)devList.get(ix);
				
				JsonObject targetDev = new JsonObject();
				
				targetDev.addProperty("TOPIC", info.get("subTopic").toString());
				
				String cmdType = info.get("cmdType").toString();
				
				JsonObject payload = new JsonObject();
				JsonArray cmd = new JsonArray();
				payload.add("cmd", cmd);
				
				if ( "IR".equals(cmdType) ) {
					String [] irKey = info.get("cmdPayload").toString().split(",");
					getSingleIR(cmd, irKey);
				}
				else if ( "DIR".equals(cmdType) ) {
					JsonParser jp = new JsonParser();
					//JsonObject raw = jp.parse(info.get("cmdPayload").toString()).getAsJsonObject();
					cmd.add(jp.parse(info.get("cmdPayload").toString()));				
				}
				else if ( "PGM".equals(cmdType) ) {
					String num = number.getValue();
					String [] key = new String[num.length()];
					
					for(int i = 0; i < num.length(); i++)
						key[i] = "IPTV_" + num.charAt(i);
					
					getSingleIR(cmd, key);
				}

				targetDev.add("PAYLOAD", payload);
				
				cntrList.add(targetDev);
			}
			
			final Map result = new HashMap();
			result.put("MESSAGE", "");
			result.put("CONTROL_DEVICE", cntrList.toString());
			
			exeResult = result;				
		}catch(Exception ex) {
			ex.printStackTrace();
		}
		
		return exeResult;
	}
	
	
	/**
	 * @param action
	 * @param target
	 * @return
	 */
	private Object controlDevice(Entity action, Entity target)
	{
		Object exeResult = null;
		
		try
		{
			Map parm = new HashMap();
			parm.put("targetVal", target.getValue());
			parm.put("actionVal", action.getValue());
			
			JsonObject root = new JsonObject();
			JsonArray cntrList = new JsonArray();
			root.add("devControl", cntrList);
			
			List devList = SqlSessionManager.getSqlMapClient().queryForList("MYHOME.selectCommandInfo", parm);
			
			for(int ix = 0; ix < devList.size(); ix++)
			{
				Map info = (Map)devList.get(ix);
				
				JsonObject targetDev = new JsonObject();
				
				targetDev.addProperty("TOPIC", info.get("subTopic").toString());
				
				String cmdType = info.get("cmdType").toString();
				
				JsonObject payload = new JsonObject();
				JsonArray cmd = new JsonArray();
				payload.add("cmd", cmd);
				
				if ( "IR".equals(cmdType) ) {
					String [] irKey = info.get("cmdPayload").toString().split(",");
					getSingleIR(cmd, irKey);
				}
				else if ( "DIR".equals(cmdType) ) {
					JsonParser jp = new JsonParser();
					//JsonObject raw = jp.parse(info.get("cmdPayload").toString()).getAsJsonObject();
					cmd.add(jp.parse(info.get("cmdPayload").toString()));				
				}

				targetDev.add("PAYLOAD", payload);
				
				cntrList.add(targetDev);
			}
			
			final Map result = new HashMap();
			result.put("MESSAGE", "");
			result.put("CONTROL_DEVICE", cntrList.toString());
			
			exeResult = result;				
		}catch(Exception ex) {
			ex.printStackTrace();
		}
		
		System.out.println(exeResult);
		
		return exeResult;
	}
	
	/**
	 * @param cmd
	 * @param key
	 */
	private void getSingleIR(JsonArray cmd, String [] key) throws Exception
	{
		Map parm = new HashMap();
		
		for(int i = 0; i < key.length; i++)
		{
			parm.clear();
			parm.put("key", key[i]);

			List keyList = SqlSessionManager.getSqlMapClient().queryForList("MYHOME.selectIrDataSingle", parm);
		
			Map m = (Map)keyList.get(0);
			JsonObject raw = new JsonObject();
			raw.addProperty("rawLen", Integer.parseInt(m.get("rawDataLen").toString()));
			raw.addProperty("rawData", m.get("rawData").toString());

			cmd.add(raw);
		}
	}
}
