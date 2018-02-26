package com.grs;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.homeai.AiServiceGrpc;
import com.homeai.AiServiceReply;
import com.homeai.AiServiceRequest;

/**
 * @author 최의신 (dreamsalmon@gmail.com)
 *
 */
public class AiServiceClient
{
	private final ManagedChannel channel;
	private final AiServiceGrpc.AiServiceBlockingStub blockingStub;

	public AiServiceClient(String host, int port)
	{
		this(ManagedChannelBuilder.forAddress(host, port)
				.usePlaintext(true).build());
	}

	/**
	 * Construct client for accessing HelloWorld server using the existing
	 * channel.
	 */
	AiServiceClient(ManagedChannel channel) {
		this.channel = channel;
		blockingStub = AiServiceGrpc.newBlockingStub(channel);
	}

	public void shutdown() throws InterruptedException {
		channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
	}

	/**
	 * @param text
	 * @return
	 */
	public JsonObject say(String text)
	{
		JsonObject obj = null;
		AiServiceRequest request = AiServiceRequest.newBuilder().setPayload(text).build();
		try {
			AiServiceReply response = blockingStub.say(request);
			
			obj = (new JsonParser()).parse(response.getPayload()).getAsJsonObject();
		} catch (StatusRuntimeException e) {
			System.out.println("RPC failed: " + e.getStatus());
		}
		
		return obj;
	}
	
	
	
	/**
	 * 
	 * @author 
	 *
	 */
	static class InputThread extends Thread
	{
		AiServiceClient client;

		public InputThread()
		{
			client = new AiServiceClient("localhost", 50050);
		}
		
		public void run()
		{
			String line = null;
			BufferedReader br = null;
			
			try
			{
				br = new BufferedReader(new InputStreamReader(System.in));
				do
				{
					line = br.readLine();
					if ( line != null ) {
						JsonObject payload = new JsonObject();
						payload.addProperty("text", line);
						JsonObject req = new JsonObject();
						req.add("request", payload);
						
						long t1 = System.currentTimeMillis();
						JsonObject rx = client.say(req.toString());
						long t2 = System.currentTimeMillis();
						
						System.out.println("##<" + (t2-t1/1000.0) + ">## " + rx);
					}
				}while(line != null);
			}catch(Exception e) {
				e.printStackTrace();
			}finally{
				try{ br.close(); }catch(Exception ig) {}
				try {
					client.shutdown();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	/**
	 * Greet server. If provided, the first element of {@code args} is the name
	 * to use in the greeting.
	 */
	public static void main(String[] args) throws Exception
	{
		InputThread in = new InputThread();
		in.start();
		try {
			in.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
