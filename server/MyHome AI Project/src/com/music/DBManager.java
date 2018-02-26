package com.music;

import java.sql.Connection;

import com.klab.svc.AppsPropertiy;


/**
 * @author 최의신 (dreamsalmon@gmail.com)
 * 
 * SQLite3 데이터베이스 연결을 관리한다.
 *
 */
public class DBManager
{
	private static DBManager instance = new DBManager();
	
	private Connection sqlite = null;
	private boolean connect = false;
	
	private DBManager() {}
	
	/**
	 * @return
	 */
	public static DBManager getInstance()
	{
		return instance;
	}
	
	/**
	 * @return
	 * @throws Exception
	 */
	public Connection getSQLite() throws Exception
	{
		if ( sqlite == null ) throw new Exception("[SQLite] Not Connected");

		return sqlite;	
	}
	
	/**
	 * @throws Exception
	 */
	public void connect() throws Exception
	{
		try
		{
			java.sql.DriverManager.registerDriver(new org.sqlite.JDBC());
			
			// song3.db 파일의 경로
			String dbFile = AppsPropertiy.getInstance().getProperty("songs.file");
			StringBuffer url = new StringBuffer();

			url.append ( "jdbc:sqlite:" ).append(dbFile);

			sqlite = java.sql.DriverManager.getConnection (url.toString()); 
			
			System.out.println("[SQLite] Connected ...");
			
			connect = true;
		}catch(Exception e) {
			e.printStackTrace();
			throw e;
		}		
	}
	
	/**
	 * 
	 */
	public void close()
	{
		if ( sqlite != null ) {
			try {
				sqlite.close();
			}catch(Exception e){}
			
			sqlite = null;
		}
		
		connect = false;
	}
	
	/**
	 * @throws Exception
	 */
	public void reconnect() throws Exception
	{
		if ( sqlite != null ) {
			try {
				sqlite.close();
			}catch(Exception e){}
		}
		
		try
		{
			connect();
			connect = true;
		}catch(Exception e) {
			connect = false;
			e.printStackTrace();
			throw e;
		}
	}
	
	/**
	 * @return
	 */
	public boolean isConnect() {
		return connect;
	}
}
