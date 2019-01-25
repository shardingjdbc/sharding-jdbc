/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shardingsphere.shardingproxy.backend.communication.netty.client.response.mysql;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.shardingsphere.core.metadata.datasource.DataSourceMetaData;
import org.apache.shardingsphere.shardingproxy.backend.communication.netty.client.response.ResponseHandler;
import org.apache.shardingsphere.shardingproxy.backend.communication.netty.future.FutureRegistry;
import org.apache.shardingsphere.shardingproxy.config.yaml.YamlDataSourceParameter;
import org.apache.shardingsphere.shardingproxy.runtime.ChannelRegistry;
import org.apache.shardingsphere.shardingproxy.runtime.GlobalRegistry;
import org.apache.shardingsphere.shardingproxy.transport.mysql.constant.CapabilityFlag;
import org.apache.shardingsphere.shardingproxy.transport.mysql.constant.ServerInfo;
import org.apache.shardingsphere.shardingproxy.transport.mysql.packet.MySQLPacketPayload;
import org.apache.shardingsphere.shardingproxy.transport.mysql.packet.command.query.ColumnDefinition41Packet;
import org.apache.shardingsphere.shardingproxy.transport.mysql.packet.command.query.text.TextResultSetRowPacket;
import org.apache.shardingsphere.shardingproxy.transport.mysql.packet.generic.EofPacket;
import org.apache.shardingsphere.shardingproxy.transport.mysql.packet.generic.ErrPacket;
import org.apache.shardingsphere.shardingproxy.transport.mysql.packet.generic.OKPacket;
import org.apache.shardingsphere.shardingproxy.transport.mysql.packet.handshake.HandshakePacket;
import org.apache.shardingsphere.shardingproxy.transport.mysql.packet.handshake.HandshakeResponse41Packet;

import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

/**
 * Response handler for MySQL.
 *
 * @author wangkai
 * @author linjiaqi
 */
@Slf4j
@RequiredArgsConstructor
public final class MySQLResponseHandler extends ResponseHandler {
    
    private static final GlobalRegistry GLOBAL_REGISTRY = GlobalRegistry.getInstance();
    
    private final YamlDataSourceParameter dataSourceParameter;
    
    private final DataSourceMetaData dataSourceMetaData;
    
    private final Map<Integer, MySQLQueryResult> resultMap;
    
    public MySQLResponseHandler(final String dataSourceName, final String schema) {
        dataSourceParameter = GLOBAL_REGISTRY.getLogicSchema(schema).getDataSources().get(dataSourceName);
        dataSourceMetaData = GLOBAL_REGISTRY.getLogicSchema(schema).getMetaData().getDataSource().getActualDataSourceMetaData(dataSourceName);
        resultMap = new HashMap<>();
    }
    
    @Override
    protected int getHeader(final ByteBuf byteBuf) {
        MySQLPacketPayload payload = new MySQLPacketPayload(byteBuf);
        payload.getByteBuf().markReaderIndex();
        payload.readInt1();
        int result = payload.readInt1();
        payload.getByteBuf().resetReaderIndex();
        return result;
    }
    
    @Override
    protected void auth(final ChannelHandlerContext context, final ByteBuf byteBuf) {
        try (MySQLPacketPayload payload = new MySQLPacketPayload(byteBuf)) {
            HandshakePacket handshakePacket = new HandshakePacket(payload);
            byte[] authResponse = securePasswordAuthentication(
                    (null == dataSourceParameter.getPassword() ? "" : dataSourceParameter.getPassword()).getBytes(), handshakePacket.getAuthPluginData().getAuthPluginData());
            HandshakeResponse41Packet handshakeResponse41Packet = new HandshakeResponse41Packet(
                    handshakePacket.getSequenceId() + 1, CapabilityFlag.calculateHandshakeCapabilityFlagsLower(), 16777215, ServerInfo.CHARSET,
                    dataSourceParameter.getUsername(), authResponse, dataSourceMetaData.getSchemaName());
            ChannelRegistry.getInstance().putConnectionId(context.channel().id().asShortText(), handshakePacket.getConnectionId());
            context.writeAndFlush(handshakeResponse41Packet);
        }
    }
    
    @SneakyThrows
    private byte[] securePasswordAuthentication(final byte[] password, final byte[] authPluginData) {
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-1");
        byte[] part1 = messageDigest.digest(password);
        messageDigest.reset();
        byte[] part2 = messageDigest.digest(part1);
        messageDigest.reset();
        messageDigest.update(authPluginData);
        byte[] result = messageDigest.digest(part2);
        for (int i = 0; i < result.length; i++) {
            result[i] = (byte) (result[i] ^ part1[i]);
        }
        return result;
    }
    
    @Override
    protected void executeCommand(final ChannelHandlerContext context, final ByteBuf byteBuf, final int header) {
        switch (header) {
            case EofPacket.HEADER:
                eofPacket(context, byteBuf);
                break;
            case OKPacket.HEADER:
                okPacket(context, byteBuf);
                break;
            case ErrPacket.HEADER:
                errPacket(context, byteBuf);
                break;
            default:
                commandPacket(context, byteBuf);
        }
    }
    
    private void okPacket(final ChannelHandlerContext context, final ByteBuf byteBuf) {
        int connectionId = ChannelRegistry.getInstance().getConnectionId(context.channel().id().asShortText());
        try (MySQLPacketPayload payload = new MySQLPacketPayload(byteBuf)) {
            MySQLQueryResult mysqlQueryResult = new MySQLQueryResult();
            mysqlQueryResult.setGenericResponse(new OKPacket(payload));
            resultMap.put(connectionId, mysqlQueryResult);
            setResponse(context);
        } finally {
            resultMap.remove(connectionId);
        }
    }
    
    private void errPacket(final ChannelHandlerContext context, final ByteBuf byteBuf) {
        int connectionId = ChannelRegistry.getInstance().getConnectionId(context.channel().id().asShortText());
        try (MySQLPacketPayload payload = new MySQLPacketPayload(byteBuf)) {
            MySQLQueryResult mysqlQueryResult = new MySQLQueryResult();
            mysqlQueryResult.setGenericResponse(new ErrPacket(payload));
            resultMap.put(connectionId, mysqlQueryResult);
            setResponse(context);
        } finally {
            resultMap.remove(connectionId);
        }
    }
    
    private void eofPacket(final ChannelHandlerContext context, final ByteBuf byteBuf) {
        int connectionId = ChannelRegistry.getInstance().getConnectionId(context.channel().id().asShortText());
        MySQLQueryResult mysqlQueryResult = resultMap.get(connectionId);
        MySQLPacketPayload payload = new MySQLPacketPayload(byteBuf);
        if (mysqlQueryResult.isColumnFinished()) {
            mysqlQueryResult.setRowFinished(new EofPacket(payload));
            resultMap.remove(connectionId);
            payload.close();
        } else {
            mysqlQueryResult.setColumnFinished(new EofPacket(payload));
            setResponse(context);
        }
    }
    
    private void setResponse(final ChannelHandlerContext context) {
        int connectionId = ChannelRegistry.getInstance().getConnectionId(context.channel().id().asShortText());
        if (null != FutureRegistry.getInstance().get(connectionId)) {
            FutureRegistry.getInstance().get(connectionId).setResponse(resultMap.get(connectionId));
        }
    }
    
    private void commandPacket(final ChannelHandlerContext context, final ByteBuf byteBuf) {
        int connectionId = ChannelRegistry.getInstance().getConnectionId(context.channel().id().asShortText());
        MySQLQueryResult mysqlQueryResult = resultMap.get(connectionId);
        MySQLPacketPayload payload = new MySQLPacketPayload(byteBuf);
        if (null == mysqlQueryResult) {
            mysqlQueryResult = new MySQLQueryResult(payload);
            resultMap.put(connectionId, mysqlQueryResult);
        } else if (mysqlQueryResult.needColumnDefinition()) {
            mysqlQueryResult.addColumnDefinition(new ColumnDefinition41Packet(payload));
        } else {
            mysqlQueryResult.addTextResultSetRow(new TextResultSetRowPacket(payload, mysqlQueryResult.getColumnCount()));
        }
    }
}
