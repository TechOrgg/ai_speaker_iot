package com.mycode;

import java.util.HashMap;
import java.util.Map;

import com.ibm.watson.developer_cloud.conversation.v1.model.Entity;
import com.klab.ctx.ConversationSession;

/**
 * @author 최의신 (dreamsalmon@gmail.com)
 *
 */
@SuppressWarnings("rawtypes")
public class BreakAction extends HomeAction
{
	/* (non-Javadoc)
	 * @see com.klab.svc.BaseAction#execute(java.lang.String, java.util.Map)
	 */
	@Override
	protected Object execute(String actionId, Map params, ConversationSession session)
	{
		Object exeResult = null;
		
		try
		{
			String inputStr = session.getInputString();
			Entity target = findEntity(session, "target");
			
			final Map<String, String> result = new HashMap<String, String>();

			result.put("BREAK", "BREAK");

			exeResult = result;				
		}catch(Exception ex) {
			ex.printStackTrace();
		}

		return exeResult;
	}
	
}
