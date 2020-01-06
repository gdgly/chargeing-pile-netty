package com.chargeingpile.netty.chargeingpilenetty.netty.server;

import com.chargeingpile.netty.chargeingpilenetty.shenghong.SHUtils;
import com.chargeingpile.netty.chargeingpilenetty.shenghong.manager.ClientConnection;
import com.chargeingpile.netty.chargeingpilenetty.shenghong.manager.ClientManager;
import com.chargeingpile.netty.chargeingpilenetty.shenghong.message.*;
import com.chargeingpile.netty.chargeingpilenetty.shenghong.utils.BytesUtil;
import com.chargeingpile.netty.chargeingpilenetty.shenghong.utils.CommonUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelId;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class NettyServerHandler extends ChannelInboundHandlerAdapter {



    /**
     * 管理一个全局map，保存连接进服务端的通道数量
     */
    private static final ConcurrentHashMap<ChannelId, ChannelHandlerContext> CHANNEL_MAP = new ConcurrentHashMap<>();





    /**
     * @param ctx
     * @author
     * @DESCRIPTION: 有客户端连接服务器会触发此函数
     * @return: void
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) {

        InetSocketAddress insocket = (InetSocketAddress) ctx.channel().remoteAddress();

        String clientIp = insocket.getAddress().getHostAddress();
        int clientPort = insocket.getPort();

        //获取连接通道唯一标识
        ChannelId channelId = ctx.channel().id();

        System.out.println();
        //如果map中不包含此连接，就保存连接
        if (CHANNEL_MAP.containsKey(channelId)) {

            System.out.println("客户端【" + channelId + "】是连接状态，连接通道数量: " + CHANNEL_MAP.size());


        } else {
            //保存连接
            CHANNEL_MAP.put(channelId, ctx);

            System.out.println("客户端【" + channelId + "】连接netty服务器[IP:" + clientIp + "--->PORT:" + clientPort + "]");
            System.out.println("连接通道数量: " + CHANNEL_MAP.size());

        }
    }





    /**
     * @param ctx
     * @DESCRIPTION: 有客户端终止连接服务器会触发此函数
     * @return: void
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) {

        InetSocketAddress insocket = (InetSocketAddress) ctx.channel().remoteAddress();

        String clientIp = insocket.getAddress().getHostAddress();

        ChannelId channelId = ctx.channel().id();

        //包含此客户端才去删除
        if (CHANNEL_MAP.containsKey(channelId)) {
            //删除连接
            CHANNEL_MAP.remove(channelId);

            System.out.println("客户端【" + channelId + "】退出netty服务器[IP:" + clientIp + "--->PORT:" + insocket.getPort() + "]");
            System.out.println("连接通道数量: " + CHANNEL_MAP.size());


        }
    }




    /**
     * @param ctx
     * @DESCRIPTION: 有客户端发消息会触发此函数
     * @return: void
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

        byte[] msg1 = (byte[]) msg;
        if (!SHUtils.isShengHong(msg1)){
            ctx.fireChannelRead(msg1);
        }
        String cmd = BytesUtil.getMsgCmd(msg1);
        String pileCode = SHUtils.getPileNum(msg1);


       // final ClientConnection client = ClientManager.getClientConnection(ctx, pileCode);


        System.out.println("cmd======="+cmd+"-------------pilecode===="+pileCode);

        if (cmd.equalsIgnoreCase("6a00")){ //充电桩签到 cmd=106
           // System.out.println("充电桩签到 cmd=106");

            SignResponse sr = new SignResponse();
            byte[] signResp = sr.getMsgByte(1);


            ctx.writeAndFlush(signResp);


            System.out.println("签到-----105");
            //响应客户端
            //this.channelWrite(ctx.channel().id(),signResp);

        } else

        if (cmd.equalsIgnoreCase("6600")){ //充电桩上传心跳包 cmd=102

            //System.out.println("充电桩上传心跳包 cmd=102");
            HbResponse hs = new HbResponse(1, 2);
            byte[] hbSlave = hs.getMsgByte(1);
            ctx.writeAndFlush(hbSlave);
            System.out.println("心跳 cmd=101");
            //响应客户端
            //this.channelWrite(ctx.channel().id(),hbSlave);

        }else {

            ctx.fireChannelRead(msg);
        }




            //响应客户端
        //this.channelWrite(ctx.channel().id(), msg);
    }




    /**
     * @param msg        需要发送的消息内容
     * @param channelId 连接通道唯一id
     * @author xiongchuan on 2019/4/28 16:10
     * @DESCRIPTION: 服务端给客户端发送消息
     * @return: void
     */
    public void channelWrite(ChannelId channelId, Object msg) throws Exception {

        ChannelHandlerContext ctx = CHANNEL_MAP.get(channelId);

        if (ctx == null) {

            System.out.println("通道【" + channelId + "】不存在");
            return;
        }

        if (msg == null && msg == "") {
            System.out.println("服务端响应空的消息");
            return;
        }

        //将客户端的信息直接返回写入ctx
        ctx.write(msg);
        //刷新缓存区
        ctx.flush();
    }


private String pileCode = "";



    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {

        String socketString = ctx.channel().remoteAddress().toString();

        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            if (event.state() == IdleState.READER_IDLE) {

                System.out.println("Client: " + socketString + " READER_IDLE 读超时");
                ctx.disconnect();

            } else if (event.state() == IdleState.WRITER_IDLE) {

                System.out.println("Client: " + socketString + " WRITER_IDLE 写超时");
                ctx.disconnect();

            } else if (event.state() == IdleState.ALL_IDLE) {

                System.out.println("Client: " + socketString + " ALL_IDLE 总超时");
                ctx.disconnect();
            }

            if (CommonUtil.isEmpty(pileCode)){
                ctx.fireUserEventTriggered(evt);

            }
            //超时
            System.out.println("超时了");
        }else {
            super.userEventTriggered(ctx,evt);
        }

    }




    /**
     * @param ctx
     * @author xiongchuan on 2019/4/28 16:10
     * @DESCRIPTION: 发生异常会触发此函数
     * @return: void
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {

        ctx.close();

        System.out.println(ctx.channel().id() + " 发生了错误,此连接被关闭" + "此时连通数量: " + CHANNEL_MAP.size());



        }

}
