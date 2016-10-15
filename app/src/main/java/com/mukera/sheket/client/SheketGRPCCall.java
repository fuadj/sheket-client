package com.mukera.sheket.client;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;

/**
 * Created by fuad on 10/11/16.
 * <p>
 * Runs gRPC calls within a limited amount of time.
 * If the call doesn't finish, it throws an exception.
 */
public class SheketGRPCCall<T> {
    public static final long GRPC_TIME_OUT_SECONDS = 3;

    public interface GRPCCallable<T> {
        T runGRPCCall() throws Exception;
    }

    public T runBlockingCall(final GRPCCallable<T> grpcCallable) throws SheketException {
        try {
            Future<T> future = Executors.newCachedThreadPool().submit(
                    new Callable<T>() {
                        @Override
                        public T call() throws Exception {
                            return grpcCallable.runGRPCCall();
                        }
                    });
            return future.get(GRPC_TIME_OUT_SECONDS, TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            StatusRuntimeException exception = (StatusRuntimeException) e.getCause();

            if (exception.getStatus().getCode() == Status.Code.UNAUTHENTICATED) {
                throw new SheketInvalidLoginException(e);
            } else {
                throw new SheketException(e);
            }
        } catch (TimeoutException | InterruptedException e) {
            throw new SheketInternetException(e);
        }
    }

    public static class SheketException extends Exception {
        public SheketException() {
            super();
        }

        public SheketException(String detailMessage) {
            super(detailMessage);
        }

        public SheketException(String detailMessage, Throwable throwable) {
            super(detailMessage, throwable);
        }

        public SheketException(Throwable throwable) {
            super(throwable);
        }
    }

    public static class SheketInternetException extends SheketException {
        public SheketInternetException(Throwable throwable) {
            super(throwable);
        }
    }

    public static class SheketInvalidLoginException extends SheketException {
        public SheketInvalidLoginException(Throwable throwable) {
            super(throwable);
        }
    }
}

