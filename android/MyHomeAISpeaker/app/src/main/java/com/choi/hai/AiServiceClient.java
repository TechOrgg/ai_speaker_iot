package com.choi.hai;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

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
				.usePlaintext(true)
                .build());
	}

	AiServiceClient(ManagedChannel channel) {
		this.channel = channel;
		blockingStub = AiServiceGrpc.newBlockingStub(channel);
	}

	public void shutdown() throws InterruptedException {
		channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
	}

	/**
	 * 서비스 서버로 gRPC로 사용자 입력을 전달한다.
     *
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
}
