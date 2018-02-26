package com.svc;

/**
 * @author 최의신 (dreamsalmon@gmail.com)
 *
 */
public class Message
{
	private String topic;
	private Object payload;
	public String getTopic() {
		return topic;
	}
	public void setTopic(String topic) {
		this.topic = topic;
	}
	public Object getPayload() {
		return payload;
	}
	public void setPayload(Object payload) {
		this.payload = payload;
	}
	
	public String toString()
	{
		return topic + " <<" + payload + ">>";
	}
}
