package io.vertx.example.grpc.helloworld;

import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.Message;
import com.google.protobuf.Struct;
import com.google.protobuf.util.JsonFormat;
import com.squareup.protoparser.*;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Launcher;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.SocketAddress;
import io.vertx.grpc.client.GrpcClient;
import io.vertx.grpc.client.GrpcClientRequest;
import io.vertx.grpc.client.GrpcClientResponse;
import io.vertx.grpc.common.ServiceName;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class ClientMessageAPI extends AbstractVerticle {

  public static void main(String[] args) {
    Launcher.executeCommand("run", ClientMessageAPI.class.getName());
  }

  @Override
  public void start() throws Exception {
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

    String proto = ""
      + "message HelloRequest {\n"
      + "  required string name = 1;"
      + "}\n";

    ProtoFile protofile = ProtoParser.parse("foo.proto",
      proto);

    MessageElement helloRequestTE = (MessageElement) protofile.typeElements().get(0);
    Map<String, FieldElement> fieldMap = new HashMap<>();
    helloRequestTE.fields().forEach(field -> {
      fieldMap.put(field.name(), field);
    });

    JsonObject json = new JsonObject();
    json.put("name", "Julien");

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    CodedOutputStream out = CodedOutputStream.newInstance(baos);

    for (Map.Entry<String, Object> elt : json) {
      String name = elt.getKey();
      FieldElement field = fieldMap.get(name);
      if (field != null) {
        DataType type = field.type();
        switch (type.kind()) {
          case SCALAR:
            DataType.ScalarType scalarType = (DataType.ScalarType) type;
            switch (scalarType) {
              case STRING:
                out.writeString(field.tag(), (String)elt.getValue());
                break;
              default:
                throw new UnsupportedOperationException("Not yet implemented");
            }
            break;
          default:
            throw new UnsupportedOperationException("Not yet implemented");
        }
      }
    }
    out.flush();
    byte[] protobufEncoded = baos.toByteArray();


    // a json structure + a message definition (proto file) => protobuf message

    GrpcClient client = GrpcClient.client(vertx);
    Future<GrpcClientRequest<Buffer, Buffer>> requestFuture = client.request(SocketAddress.inetSocketAddress(8080, "localhost"));
    requestFuture.onSuccess(request -> {
      // Set the service name and the method to call
      request.fullMethodName("helloworld.Greeter/SayHello");
      request.encoding("identity");
      Buffer buffer = Buffer.buffer(protobufEncoded);
      request.end(buffer);

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
