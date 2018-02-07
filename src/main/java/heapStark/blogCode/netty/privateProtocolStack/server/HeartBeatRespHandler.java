/*
 * Copyright 2013-2018 Lilinfeng.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package heapStark.blogCode.netty.privateProtocolStack.server;


import heapStark.blogCode.netty.privateProtocolStack.MessageType;
import heapStark.blogCode.netty.privateProtocolStack.struct.Header;
import heapStark.blogCode.netty.privateProtocolStack.struct.NettyMessage;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author Lilinfeng
 * @date 2014年3月15日
 * @version 1.0
 */
public class HeartBeatRespHandler extends ChannelHandlerAdapter {

	private static final Logger LOG = LoggerFactory.getLogger(HeartBeatRespHandler.class);


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg)
	    throws Exception {
	NettyMessage message = (NettyMessage) msg;
	// 返回心跳应答消息
	if (message.getHeader() != null
		&& message.getHeader().getType() == MessageType.HEARTBEAT_REQ
			.value()) {
	    LOG.info("Receive client heart beat message : ---> "
		    + message);
	    NettyMessage heartBeat = buildHeatBeat();
	    LOG.info("Send heart beat response message to client : ---> "
			    + heartBeat);
	    ctx.writeAndFlush(heartBeat);
	} else
	    ctx.fireChannelRead(msg);
    }

    private NettyMessage buildHeatBeat() {
	NettyMessage message = new NettyMessage();
	Header header = new Header();
	header.setType(MessageType.HEARTBEAT_RESP.value());
	message.setHeader(header);
	return message;
    }

}
