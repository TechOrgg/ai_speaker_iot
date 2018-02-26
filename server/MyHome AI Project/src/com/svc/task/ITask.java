package com.svc.task;

import java.util.concurrent.BlockingQueue;

import com.svc.Message;

/**
 * @author 최의신 (dreamsalmon@gmail.com)
 *
 */
public interface ITask
{
	public void execute(BlockingQueue<Message> pub, Message payload);
}
