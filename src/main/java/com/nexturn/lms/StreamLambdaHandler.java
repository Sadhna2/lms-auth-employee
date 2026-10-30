package com.nexturn.lms;

import com.amazonaws.serverless.exceptions.ContainerInitializationException;
import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.spring.SpringBootLambdaContainerHandler;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/** Adapts API Gateway REST proxy events to Spring MVC and reuses Spring on warm invocations. */
public class StreamLambdaHandler implements RequestStreamHandler {
    private final SpringBootLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> handler;

    /** Lets Lambda report startup failures; MVC advice applies only after Spring has started. */
    public StreamLambdaHandler() throws ContainerInitializationException {
        handler = SpringBootLambdaContainerHandler.getAwsProxyHandler(Application.class);
    }

    /** Proxies a request; infrastructure I/O errors propagate to the Lambda runtime. */
    @Override
    public void handleRequest(InputStream input, OutputStream output, Context context) throws IOException {
        handler.proxyStream(input, output, context);
    }
}
