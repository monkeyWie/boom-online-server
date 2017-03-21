/*
 * Copyright 2014 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package lee.study.handle;

import com.alibaba.fastjson.JSON;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.util.CharsetUtil;
import lee.study.bean.BoomRequest;
import lee.study.bean.BoomResponse;
import lee.study.bean.Hero;
import lee.study.bean.Room;
import lee.study.server.BoomOnlineServer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.netty.handler.codec.http.HttpMethod.GET;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 * Handles handshakes and messages
 */
public class WebSocketServerHandler extends SimpleChannelInboundHandler<Object> {

    private static final String WEBSOCKET_PATH = "/websocket";

    private WebSocketServerHandshaker handshaker;

    @Override
    public void channelRead0(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof FullHttpRequest) {
            handleHttpRequest(ctx, (FullHttpRequest) msg);
        } else if (msg instanceof WebSocketFrame) {
            handleWebSocketFrame(ctx, (WebSocketFrame) msg);
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    private void handleHttpRequest(ChannelHandlerContext ctx, FullHttpRequest req) {
        // Handle a bad request.
        if (!req.decoderResult().isSuccess()) {
            sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HTTP_1_1, BAD_REQUEST));
            return;
        }

        // Allow only GET methods.
        if (req.method() != GET) {
            sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HTTP_1_1, FORBIDDEN));
            return;
        }

        if ("/favicon.ico".equals(req.uri())) {
            FullHttpResponse res = new DefaultFullHttpResponse(HTTP_1_1, NOT_FOUND);
            sendHttpResponse(ctx, req, res);
            return;
        }

        if(HttpHeaderValues.WEBSOCKET.toString().equals(req.headers().get(HttpHeaderNames.UPGRADE))){
            // Handshake
            WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(
                    getWebSocketLocation(req), null, true, 5 * 1024 * 1024);
            handshaker = wsFactory.newHandshaker(req);
            if (handshaker == null) {
                WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
            } else {
                handshaker.handshake(ctx.channel(), req);
                Hero hero = new Hero();
                //新的英雄进入房间
                Room.room.put(ctx,hero);
            }
        }else{
            FullHttpResponse res = new DefaultFullHttpResponse(HTTP_1_1, NOT_FOUND);
            sendHttpResponse(ctx, req, res);
            return;
        }
    }

    private void handleWebSocketFrame(ChannelHandlerContext ctx, WebSocketFrame frame) {
        // Check for closing frame
        if (frame instanceof CloseWebSocketFrame) {
            handshaker.close(ctx.channel(), (CloseWebSocketFrame) frame.retain());
            return;
        }
        if (frame instanceof PingWebSocketFrame) {
            ctx.write(new PongWebSocketFrame(frame.content().retain()));
            return;
        }
        if (frame instanceof TextWebSocketFrame) {
            byte[] bts = new byte[frame.content().readableBytes()];
            frame.content().readBytes(bts);
            BoomRequest request = JSON.parseObject(new String(bts),BoomRequest.class);
            if(request.getStatus()==0){//请求用户ID
                BoomResponse response = new BoomResponse(0,Room.room.get(ctx).getId());
                TextWebSocketFrame textWebSocketFrame = new TextWebSocketFrame();
                textWebSocketFrame.content().writeBytes(JSON.toJSONString(response).getBytes());
                ctx.writeAndFlush(textWebSocketFrame);
            }else if(request.getStatus()==1){//加载其他用户
                List<Hero> otherHeros = new ArrayList<Hero>();
                for (Map.Entry<ChannelHandlerContext,Hero> entry:Room.room.entrySet()){
                    if(entry.getKey()!=ctx){
                        otherHeros.add(entry.getValue());
                    }
                }
                BoomResponse response = new BoomResponse(1,otherHeros);
                for (Map.Entry<ChannelHandlerContext,Hero> entry:Room.room.entrySet()){
                    TextWebSocketFrame textWebSocketFrame = new TextWebSocketFrame();
                    textWebSocketFrame.content().writeBytes(JSON.toJSONString(response).getBytes());
                    entry.getKey().writeAndFlush(textWebSocketFrame);
                }
            }
            else if(request.getStatus()==2){//新用户加入
                BoomResponse response = new BoomResponse(2,Room.room.get(ctx));
                for (Map.Entry<ChannelHandlerContext,Hero> entry:Room.room.entrySet()){
                    TextWebSocketFrame textWebSocketFrame = new TextWebSocketFrame();
                    textWebSocketFrame.content().writeBytes(JSON.toJSONString(response).getBytes());
                    entry.getKey().writeAndFlush(textWebSocketFrame);
                }
            }else {//行走
                Hero currUser = Room.room.get(ctx);
                currUser.setStatus(request.getBussObj().getStatus());
                BoomResponse response = new BoomResponse(3,currUser);
                //停止时更新坐标
                if(request.getBussObj().getStatus()==0){
                    currUser.setX(request.getBussObj().getX());
                    currUser.setY(request.getBussObj().getY());
                }
                for (Map.Entry<ChannelHandlerContext,Hero> entry:Room.room.entrySet()){
                    TextWebSocketFrame textWebSocketFrame = new TextWebSocketFrame();
                    System.out.println(JSON.toJSONString(response));
                    textWebSocketFrame.content().writeBytes(JSON.toJSONString(response).getBytes());
                    entry.getKey().writeAndFlush(textWebSocketFrame);
                }
            }
            return;
        }
        if (frame instanceof BinaryWebSocketFrame) {
            // Echo the frame
            ctx.write(frame.retain());
        }
    }

    private static void sendHttpResponse(
            ChannelHandlerContext ctx, FullHttpRequest req, FullHttpResponse res) {
        // Generate an error page if response getStatus code is not OK (200).
        if (res.status().code() != 200) {
            ByteBuf buf = Unpooled.copiedBuffer(res.status().toString(), CharsetUtil.UTF_8);
            res.content().writeBytes(buf);
            buf.release();
            HttpUtil.setContentLength(res, res.content().readableBytes());
        }

        // Send the response and close the connection if necessary.
        ChannelFuture f = ctx.channel().writeAndFlush(res);
        if (!HttpUtil.isKeepAlive(req) || res.status().code() != 200) {
            f.addListener(ChannelFutureListener.CLOSE);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        Room.room.remove(ctx);
        cause.printStackTrace();
        ctx.close();
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        Room.room.remove(ctx);
    }

    private static String getWebSocketLocation(FullHttpRequest req) {
        String location =  req.headers().get(HttpHeaderNames.HOST) + WEBSOCKET_PATH;
        if (BoomOnlineServer.SSL) {
            return "wss://" + location;
        } else {
            return "ws://" + location;
        }
    }
}
