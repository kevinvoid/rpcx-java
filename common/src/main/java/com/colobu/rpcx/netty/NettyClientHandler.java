package com.colobu.rpcx.netty;

import com.colobu.rpcx.protocol.RemotingCommand;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * Created by zhangzhiyong on 2018/7/3.
 */
public class NettyClientHandler extends SimpleChannelInboundHandler<RemotingCommand> {

    private final NettyRemotingAbstract nettyClient;

    public NettyClientHandler(NettyRemotingAbstract nettyClient) {
        this.nettyClient = nettyClient;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RemotingCommand msg) throws Exception {
        final RemotingCommand cmd = msg;
        if (cmd != null) {
            switch (cmd.getType()) {
                case REQUEST_COMMAND:
//                    processRequestCommand(ctx, cmd);
                    break;
                case RESPONSE_COMMAND:
                    nettyClient.processResponseCommand(ctx, cmd);
                    break;
                default:
                    break;
            }
        }
    }


}
