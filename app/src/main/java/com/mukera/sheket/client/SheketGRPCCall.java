package com.mukera.sheket.client;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Created by fuad on 10/11/16.
 *
 * Runs gRPC calls within a limited amount of time.
 * If the call doesn't finish, it throws an exception.
 */
public class SheketGRPCCall<T> {
    public static final long GRPC_TIME_OUT_SECONDS = 3;

    public interface GRPCCallable<T> {
        T runGRPCCall() throws Exception;
    }

    public T runBlockingCall(final GRPCCallable<T> grpcCallable) throws SheketGRPCException {
        try {
            Future<T> future = Executors.newCachedThreadPool().submit(
                    new Callable<T>() {
                        @Override
                        public T call() throws Exception {
                            return grpcCallable.runGRPCCall();
                        }
                    });
            return future.get(GRPC_TIME_OUT_SECONDS, TimeUnit.SECONDS);
        } catch (ExecutionException | TimeoutException | InterruptedException e) {
            throw new SheketGRPCException(e);
        }
    }

    public static class SheketGRPCException extends Exception {
        public SheketGRPCException(Throwable throwable) {
            super(throwable);
        }
    }
}
