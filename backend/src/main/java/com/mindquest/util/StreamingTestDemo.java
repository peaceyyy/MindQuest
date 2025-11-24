package com.mindquest.util;

import com.mindquest.llm.*;
import com.mindquest.llm.providers.MockProvider;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;

/**
 * Quick demonstration of streaming LLM responses.
 * Tests the streaming API with both MockProvider (for local testing).
 */
public class StreamingTestDemo {

    public static void main(String[] args) throws Exception {
        System.out.println("===========================================");
        System.out.println("    STREAMING LLM TEST DEMO");
        System.out.println("===========================================\n");

        testMockProviderStreaming();
        
        System.out.println("\n===========================================");
        System.out.println("    DEMO COMPLETE");
        System.out.println("===========================================");
    }

    private static void testMockProviderStreaming() throws Exception {
        System.out.println("─────────────────────────────────────────");
        System.out.println("Testing MockProvider Streaming");
        System.out.println("─────────────────────────────────────────");

        MockProvider provider = new MockProvider(
            "This is a simulated streaming response from the mock LLM provider.",
            false,
            100
        );

        Prompt prompt = new Prompt.Builder()
            .id("stream-test-001")
            .instruction("Generate a test response")
            .stream(true)
            .build();

        CountDownLatch latch = new CountDownLatch(1);
        StringBuilder fullResponse = new StringBuilder();

        Flow.Publisher<StreamEvent> publisher = provider.stream(prompt);

        publisher.subscribe(new Flow.Subscriber<StreamEvent>() {
            private Flow.Subscription subscription;

            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                this.subscription = subscription;
                System.out.println("✓ Subscribed to stream");
                subscription.request(Long.MAX_VALUE); // Request all items
            }

            @Override
            public void onNext(StreamEvent event) {
                if (event.hasError()) {
                    System.out.println("✗ Error event: " + event.getError().getMessage());
                } else if (event.isDone()) {
                    System.out.println("\n✓ Stream complete");
                } else {
                    // Print partial text without newline
                    System.out.print(event.getPartialText());
                    fullResponse.append(event.getPartialText());
                }
            }

            @Override
            public void onError(Throwable throwable) {
                System.out.println("\n✗ Stream error: " + throwable.getMessage());
                latch.countDown();
            }

            @Override
            public void onComplete() {
                System.out.println("\n✓ Stream closed");
                System.out.println("Full response (" + fullResponse.length() + " chars): " + fullResponse);
                latch.countDown();
            }
        });

        // Wait for completion (max 10 seconds)
        if (!latch.await(10, TimeUnit.SECONDS)) {
            System.out.println("✗ Timeout waiting for stream");
        }

        provider.close();
        System.out.println();
    }
}
