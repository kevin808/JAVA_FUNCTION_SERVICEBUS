package com.function;

import com.azure.identity.ManagedIdentityCredential;
import com.azure.identity.ManagedIdentityCredentialBuilder;
import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusMessageBatch;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import com.azure.core.util.BinaryData;

import java.util.Optional;
import java.util.stream.IntStream;

/**
 * Azure Functions with HTTP Trigger.
 */
public class Function {
    /**
     * This function listens at endpoint "/api/HttpExample". Two ways to invoke it using "curl" command in bash:
     * 1. curl -d "HTTP Body" {your host}/api/HttpExample
     * 2. curl "{your host}/api/HttpExample?name=HTTP%20Query"
     */
    @FunctionName("HttpExample")
    public HttpResponseMessage run(
            @HttpTrigger(
                name = "req",
                methods = {HttpMethod.GET, HttpMethod.POST},
                authLevel = AuthorizationLevel.ANONYMOUS)
                HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {
        context.getLogger().info("Java HTTP trigger processed a request.");
        
        String MSI_SECRET = System.getenv("MSI_SECRET");

        if (MSI_SECRET != null){
            ManagedIdentityCredential credential = new ManagedIdentityCredentialBuilder().clientId("clientId").build();
            ServiceBusSenderClient sender = new ServiceBusClientBuilder()
            .credential("your.servicebus.windows.net", credential)
            .sender()
            .queueName("test")
            .buildClient();

            try {
                // Create a message to send.
                final ServiceBusMessageBatch messageBatch = sender.createMessageBatch();
                IntStream.range(0, 10)
                    .mapToObj(index -> new ServiceBusMessage(BinaryData.fromString("Hello ManagedIdentity! " + index)))
                    .forEach(message -> messageBatch.tryAddMessage(message));

                // Send that message. It completes successfully when the event has been delivered to the Service queue or topic.
                // It completes with an error if an exception occurred while sending the message.
                sender.sendMessages(messageBatch);

                // Close the sender.
                // sender.close();
            } catch (Exception e) {
                    context.getLogger().warning(String.format(e.getMessage()));
            }

        }
        else{
            

        }

        // Parse query parameter
        final String query = request.getQueryParameters().get("name");
        final String name = request.getBody().orElse(query);

        if (name == null) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST).body("Please pass a name on the query string or in the request body").build();
        } else {
            return request.createResponseBuilder(HttpStatus.OK).body("Hello, " + name).build();
        }
    }
}
