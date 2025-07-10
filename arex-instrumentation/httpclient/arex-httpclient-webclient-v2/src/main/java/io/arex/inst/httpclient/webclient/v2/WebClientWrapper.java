package io.arex.inst.httpclient.webclient.v2;

import io.arex.agent.bootstrap.ctx.TraceTransmitter;
import io.arex.agent.bootstrap.model.MockResult;
import io.arex.agent.bootstrap.util.ArrayUtils;
import io.arex.inst.httpclient.common.HttpClientExtractor;
import io.arex.inst.httpclient.webclient.v2.model.WebClientRequest;
import io.arex.inst.httpclient.webclient.v2.model.WebClientResponse;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class WebClientWrapper {
    private final ClientRequest httpRequest;
    private final ExchangeStrategies strategies;
    private final HttpClientExtractor<ClientRequest, WebClientResponse> extractor;
    private final WebClientAdapter adapter;
    private final TraceTransmitter traceTransmitter;
    private final TraceTransmitter traceTransmitter1;
    private final TraceTransmitter traceTransmitter2;
    public WebClientWrapper(ClientRequest httpRequest, ExchangeStrategies strategies) {
        this.httpRequest = httpRequest;
        this.strategies = strategies;
        this.adapter = new WebClientAdapter(httpRequest, strategies);
        this.extractor = new HttpClientExtractor<>(this.adapter);
        this.traceTransmitter = TraceTransmitter.create();
        this.traceTransmitter1 = TraceTransmitter.create();
        this.traceTransmitter2 = TraceTransmitter.create();
    }

    public Mono<ClientResponse> record(Mono<ClientResponse> responseMono) {
        if (responseMono == null) {
            return null;
        }

        convertRequest();

        return responseMono.flatMap(response -> {
            try (TraceTransmitter tm = traceTransmitter.transmit()) {
                List<byte[]> bytes = new ArrayList<>();

                // 读取原始响应体为 DataBuffer 流
                Flux<DataBuffer> originalBody = response.bodyToFlux(DataBuffer.class);

                // 聚合响应体内容
                Flux<DataBuffer> recordingBody = originalBody
                        .map(dataBuffer -> {
                            collect(dataBuffer, bytes);
                            return dataBuffer;
                        })
                        .doOnComplete(() -> {
                            byte[] bodyArray = mergeBytes(bytes);
                            try (TraceTransmitter tm1 = traceTransmitter1.transmit()) {
                                extractor.record(WebClientResponse.of(response, bodyArray));
                            }
                        });

                // 构造新的 ClientResponse
                ClientResponse newResponse = ClientResponse
                        .create(response.statusCode())
                        .headers(headers -> headers.addAll(response.headers().asHttpHeaders()))
                        .cookies(cookies -> cookies.addAll(response.cookies()))
                        .body(recordingBody)
                        .build();

                return Mono.just(newResponse);
            }
        }).doOnCancel(() -> {
            try (TraceTransmitter tm = traceTransmitter.transmit()) {
                extractor.record((Exception) null);
            }
        }).doOnError(throwable -> {
            try (TraceTransmitter tm = traceTransmitter.transmit()) {
                extractor.record(throwable);
            }
        });
    }

    private byte[] mergeBytes(List<byte[]> bytes) {
        if (bytes.isEmpty()) {
            return new byte[0];
        }
        if (bytes.size() == 1) {
            return bytes.get(0);
        }
        byte[] result = new byte[0];
        for (byte[] part : bytes) {
            result = ArrayUtils.addAll(result, part);
        }
        return result;
    }

    private void collect(DataBuffer dataBuffer, List<byte[]> bytes) {
        if (dataBuffer != null) {
            bytes.add(dataBuffer.toString(StandardCharsets.UTF_8).getBytes());
        }
    }

    private void convertRequest() {
        WebClientRequest request = WebClientRequest.of(httpRequest);
        httpRequest.writeTo(request, strategies).subscribe();
        adapter.setHttpRequest(request);
    }

    public MockResult replay() {
        try (TraceTransmitter tm = traceTransmitter2.transmit()) {
            convertRequest();
            return extractor.replay();
        }
    }

    public Mono<ClientResponse> replay(MockResult mockResult) {
        if (mockResult.getThrowable() != null) {
            return Mono.error(mockResult.getThrowable());
        }
        WebClientResponse webClientResponse = (WebClientResponse)mockResult.getResult();
        return Mono.just(webClientResponse.originalResponse());
    }
}