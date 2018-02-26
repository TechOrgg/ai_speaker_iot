package com.mycode;

import java.util.HashMap;
import java.util.Map;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import com.klab.ctx.ConversationSession;
import com.klab.svc.BaseAction;

/**
 * @author 최의신 (dreamsalmon@gmail.com)
 *
 */
@SuppressWarnings("rawtypes")
public class BusArriveAction extends BaseAction
{
	private static final String URL = "http://bus.incheon.go.kr/iwcm/retrievebusstopcararriveinfo.laf?bstopid=165000735&routeid=16500001";
	private static final String NUMBER = "11";
	private static final String SELECTOR = "#cont1 > tbody > tr > td > div > table:last-child td";
	
	public String busArrive()
	{
		String str = "0000";
		
		try
		{
			Document doc = Jsoup.connect(URL).get();
			Elements info = doc.select(SELECTOR);
			
			for(int i = 0; i < info.size()/6; i++)
			{
				String num = info.get(i*6).text();
				if ( NUMBER.equals(num) )
				{
		            String data = info.get(i*6+5).text();
		            int mpos = data.indexOf("분");
		            int spos = data.indexOf("초");
		            int min = 0;
		            int sec = 0;
		            
		            if ( mpos != -1 )
		            {
		                min = Integer.parseInt(data.substring(0, mpos).trim());
		                
		                if ( spos != -1 )
		                {
		                    sec = Integer.parseInt(data.substring(mpos+1, spos).trim());
		                }
		            }
		            else {
		                if ( spos != -1 )
		                {
		                    sec = Integer.parseInt(data.substring(0, spos).trim());
		                }
		            }

		            str = "";
		            if ( min < 10 ) str += "0";
		            str += min;
		            if ( sec < 10 ) str += "0";
		            str += sec;
				}
			}
		}catch(Exception ex) {
			ex.printStackTrace();
		}
		
		return str;
	}
	
	/* (non-Javadoc)
	 * @see com.klab.svc.BaseAction#execute(java.lang.String, java.util.Map)
	 */
	@Override
	protected Object execute(String actionId, Map params, ConversationSession session)
	{
		int min = 0;
		int sec = 0;
		String time = busArrive();
		
		Object exeResult = null;
		
		try
		{
			min = Integer.parseInt(time.substring(0, 2));
			sec = Integer.parseInt(time.substring(2));
			
			Map<String, String> map = new HashMap<String, String>();
			if ( min == 0 )
			{
				if ( sec == 0 )
					map.put("BUS_ARRIVE", "곧 도착합니다.");
				else
					map.put("BUS_ARRIVE", sec + "초 후에 도착합니다.");
			}
			else {
				map.put("BUS_ARRIVE", min + "분 " + sec + "초 후에 도착합니다.");
			}
			
			exeResult = map;
		}catch(Exception ex) {
			ex.printStackTrace();
		}

		return exeResult;
	}
	
	public static void main(String [] args)
	{
		System.out.println((new BusArriveAction()).busArrive());
	}
}
