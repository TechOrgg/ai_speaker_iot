package com.svc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author 최의신 (dreamsalmon@gmail.com)
 *
 */
public abstract class BaseThread extends Thread implements IServerInformation, IServerControl
{
	protected Logger logger = LoggerFactory.getLogger(BaseThread.class);
	
	protected boolean active = true;
	protected boolean error;
	protected String errorString;
	protected String startTime;
	
	/* (non-Javadoc)
	 * @see com.choi.pi.svr.IServerControl#shutdown()
	 */
	public void shutdown()
	{
		this.active = false;
	}

	/* (non-Javadoc)
	 * @see com.choi.pi.svr.IServerError#isError()
	 */
	public boolean isError() {
		return error;
	}

	public void setError(boolean error) {
		this.error = error;
	}

	/* (non-Javadoc)
	 * @see com.choi.pi.svr.IServerError#getErrorString()
	 */
	public String getErrorString() {
		return errorString;
	}

	public void setErrorString(String errorString) {
		this.errorString = errorString;
	}
}
