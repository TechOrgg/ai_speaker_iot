package com.music;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.JsonObject;
import com.grs.AiServiceServer;
import com.utils.SqlSessionManager;

/**
 * @author 최의신 (dreamsalmon@gmail.com)
 *
 */
@WebServlet(
		asyncSupported = true,
		loadOnStartup = 1, 
		urlPatterns = {"/songMig/*"}
		)
public class SongMigServlet extends HttpServlet
{
	class GrpcStarter extends Thread
	{
		public void run()
		{
			try
			{
				server = new AiServiceServer();
				server.start();
				server.blockUntilShutdown();
			}catch(Exception e) {
			}
		}
	}
	
	private AiServiceServer server = null;
	
	
    /* (non-Javadoc)
	 * @see javax.servlet.GenericServlet#init(javax.servlet.ServletConfig)
	 */
	@Override
	public void init(ServletConfig config) throws ServletException
	{
		super.init(config);
		(new GrpcStarter()).start();
	}

	@Override
	public void destroy() {
		if ( server != null )
			server.stop();

		super.destroy();
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		JsonObject result = new JsonObject();

		try
		{
			DBManager.getInstance().connect();
		}catch(Exception ex) {
			result.addProperty("returnCode", "FAIL");
			result.addProperty("errorString", ex.getMessage());
			response.setContentType("text/plain; charset=utf-8");
	        response.getWriter().print(result.toString());		
			return;
		}
		
		try
		{
			if ( DBManager.getInstance().isConnect() )
			{
				/*
				 * 이전 데이터 삭제
				 */
				SqlSessionManager.getSqlMapClient().delete("MYHOME.deleteSong");

				/*
				 * Batch
				 */
				SqlSessionManager.getSqlMapClient().startTransaction();
				SqlSessionManager.getSqlMapClient().startBatch();

				Statement st = DBManager.getInstance().getSQLite().createStatement();
				ResultSet rs = st.executeQuery("SELECT id,fname,title,artist,album,genre,type FROM songs");
				int index = 1;
				Map parm = new HashMap();
				while(rs.next())
				{
					parm.put("id", rs.getInt("id"));
					parm.put("fname", rs.getString("fname"));
					parm.put("title", rs.getString("title").replaceAll("\\s",""));
					parm.put("artist", rs.getString("artist").replaceAll("\\s",""));
					parm.put("album", rs.getString("album"));
					parm.put("genre", rs.getString("genre"));
					parm.put("type", rs.getString("type"));
					
					SqlSessionManager.getSqlMapClient().insert("MYHOME.insertSong", parm);
					
			        if (index++ % 500 == 0) {
			        	SqlSessionManager.getSqlMapClient().executeBatch();
			            SqlSessionManager.getSqlMapClient().startBatch();
			        }
				}
				
				rs.close();
				st.close();
				
				result.addProperty("returnCode", "OK");
			}
			else {
				result.addProperty("returnCode", "FAIL");
				result.addProperty("errorString", "Can not connect to songs database");
			}
			
		}catch(Exception ex) {
			result.addProperty("returnCode", "FAIL");
			result.addProperty("errorString", ex.getMessage());
		}finally{
			DBManager.getInstance().close();
			
			try
			{
				SqlSessionManager.getSqlMapClient().executeBatch();
				SqlSessionManager.getSqlMapClient().commitTransaction();
			    SqlSessionManager.getSqlMapClient().endTransaction();
			}catch(Exception ex) {
				ex.printStackTrace();
			}
		}
		
		response.setContentType("text/plain; charset=utf-8");
        response.getWriter().print(result.toString());		
	}
}