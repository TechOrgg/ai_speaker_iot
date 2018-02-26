package com.mycode;

import java.util.List;

import com.ibm.watson.developer_cloud.conversation.v1.model.Entity;
import com.klab.ctx.ConversationSession;
import com.klab.svc.BaseAction;

/**
 * @author 최의신 (dreamsalmon@gmail.com)
 *
 */
@SuppressWarnings("rawtypes")
public abstract class HomeAction extends BaseAction
{
	
	/**
	 * @param session
	 * @param name
	 * @return
	 */
	protected Entity findEntity(ConversationSession session, String name)
	{
		List list = session.getEntities();
		
		for(int i = 0; i < list.size(); i++)
		{
			Entity e = (Entity)list.get(i);
			if ( name.equals(e.getEntity()) ) {
				return e;
			}
		}
		
		return null;
	}
}
