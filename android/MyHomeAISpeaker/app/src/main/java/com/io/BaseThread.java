package com.io;

/**
 * @author 최의신 (dreamsalmon@gmail.com)
 *
 */
public abstract class BaseThread extends Thread
{
	protected boolean running = true;
	protected String startTime;
	
	public void shutdown()
	{
		this.running = false;
	}
}
