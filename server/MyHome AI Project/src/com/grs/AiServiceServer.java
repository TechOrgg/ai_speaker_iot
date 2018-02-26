package com.grs;

import java.io.IOException;

import com.klab.svc.AppsPropertiy;

import io.grpc.Server;
import io.grpc.ServerBuilder;

/**
 * @author 최의신 (dreamsalmon@gmail.com)
 *
 */
public class AiServiceServer
{
	private Server server;

	/**
	 * @throws IOException
	 */
	public void start() throws IOException {
		/* The port on which the server should run */
		int port = AppsPropertiy.getInstance().getIntProperty("grpc.port");
		server = ServerBuilder.forPort(port).addService(new AiServiceImpl()).build().start();
		System.out.println("@.@ Server started, listening on " + port);
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				// Use stderr here since the logger may have been reset by its
				// JVM shutdown hook.
				System.err.println("*** shutting down gRPC server since JVM is shutting down");
				AiServiceServer.this.stop();
				System.err.println("*** server shut down");
			}
		});
	}

	public void stop() {
		if (server != null) {
			server.shutdown();
		}
	}

	/**
	 * Await termination on the main thread since the grpc library uses daemon
	 * threads.
	 */
	public void blockUntilShutdown() throws InterruptedException {
		if (server != null) {
			server.awaitTermination();
		}
	}
	
	public static void main(String[] args) throws IOException, InterruptedException
	{
		final AiServiceServer server = new AiServiceServer();
		server.start();
		server.blockUntilShutdown();
	}
}
