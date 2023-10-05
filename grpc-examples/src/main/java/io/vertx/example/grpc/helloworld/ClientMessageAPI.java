package io.vertx.example.grpc.helloworld;

import com.google.protobuf.Message;
import com.google.protobuf.Struct;
import com.google.protobuf.util.JsonFormat;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Launcher;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.SocketAddress;
import io.vertx.grpc.client.GrpcClient;
import io.vertx.grpc.client.GrpcClientRequest;
import io.vertx.grpc.client.GrpcClientResponse;
import io.vertx.grpc.common.ServiceName;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class ClientMessageAPI extends AbstractVerticle {

  public static void main(String[] args) {
    Launcher.executeCommand("run", ClientMessageAPI.class.getName());
  }

  @Override
  public void start() {
    // proto reminder
    // The request message containing the user's name.
    //message HelloRequest {
    //  string name = 1;
    //}
    //
    //// The response message containing the greetings
    //message HelloReply {
    //  string message = 1;
    //}

    String json = "{\n" +
      "  \"name\": \"Julien\"\n" +
      "}";
    GrpcClient client = GrpcClient.client(vertx);
    Future<GrpcClientRequest<Buffer, Buffer>> requestFuture = client.request(SocketAddress.inetSocketAddress(8080, "localhost"));
    requestFuture.onSuccess(request -> {
      // Set the service name and the method to call
      request.fullMethodName("helloworld.Greeter/SayHello");
      request.encoding("identity");
      try {
        Message msg = fromJson(json);
        Buffer buffer = Buffer.buffer(msg.toByteArray());
        request.end(buffer);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }

      // Handle the response
      Future<GrpcClientResponse<Buffer, Buffer>> responseFut = request.response();
      responseFut.onSuccess(response -> {
        response.handler(protoReply -> {
          System.out.println(protoReply.toJson());
          // Handle the protobuf reply
        });
      });
      responseFut.onFailure(Throwable::printStackTrace);
    });
  }

  public static Message fromJson(String json) throws IOException {
    Struct.Builder structBuilder = Struct.newBuilder();
    JsonFormat.parser().ignoringUnknownFields().merge(json, structBuilder);
    return structBuilder.build();
  }
}
