package com.svc;

public interface IServerInformation
{
	/**
	 * @return
	 */
	public String getIdentity();
	
	/**
	 * @return
	 */
	public String getStartTime();
	
	/**
	 * @return
	 */
	public boolean isError();
	
	/**
	 * @return
	 */
	public String getErrorString();
}
