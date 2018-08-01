package com.colobu.rpcx.rpc.impl;

import com.colobu.rpcx.common.retry.RetryNTimes;
import com.colobu.rpcx.common.retry.RetryPolicy;
import com.colobu.rpcx.netty.IClient;
import com.colobu.rpcx.protocol.CompressType;
import com.colobu.rpcx.protocol.Message;
import com.colobu.rpcx.protocol.MessageType;
import com.colobu.rpcx.protocol.SerializeType;
import com.colobu.rpcx.rpc.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

public class RpcConsumerInvoker<T> implements Invoker<T> {

    private static final Logger logger = LoggerFactory.getLogger(RpcConsumerInvoker.class);

    private final AtomicInteger seq = new AtomicInteger();

    private IClient client;

    public RpcConsumerInvoker(IClient client) {
        this.client = client;
    }

    private URL url;

    @Override
    public Class<T> getInterface() {
        return null;
    }

    @Override
    public Result invoke(RpcInvocation invocation) throws RpcException {
        url = new URL("easy_go", "111111", 2222);

        String className = invocation.getClassName();
        String method = invocation.getMethodName();
        RpcResult result = new RpcResult();

        Message req = new Message(className, method);
        req.setVersion((byte) 0);
        req.setMessageType(MessageType.Request);
        req.setHeartbeat(false);
        req.setOneway(false);
        req.setCompressType(CompressType.None);
        req.setSerializeType(SerializeType.SerializeNone);
        req.metadata.put("language", "java");
        req.metadata.put("url", this.url.toFullString());

        byte[] data = HessianUtils.write(invocation);
        req.payload = data;

        RetryPolicy retryPolicy = new RetryNTimes(invocation.getRetryNum());//重试策略
        boolean retryResult = retryPolicy.retry((n) -> {
            try {
                req.setSeq(seq.incrementAndGet());//每次重发需要加1
                Message res = client.call(req, invocation.getTimeOut());

                if (res.metadata.containsKey("_error_code")) {
                    int code = Integer.parseInt(res.metadata.get("_error_code"));
                    String message = res.metadata.get("_error_message");
                    logger.warn("client call error:{}:{}", code, message);
                    RpcException error = new RpcException(message, code);
                    result.setThrowable(error);
                    return false;
                } else {
                    byte[] d = res.payload;
                    if (d.length > 0) {
                        Object r = HessianUtils.read(d);
                        result.setValue(r);
                    }
                    return true;
                }
            } catch (Throwable e) {
                result.setThrowable(e);
                logger.info("client call error need retry n:{}", n);
                return false;
            }
        });

        logger.info("class:{} method:{} retryResult:{}", className, method, retryResult);
        return result;
    }

    @Override
    public URL getUrl() {
        return url;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public void destroy() {

    }
}