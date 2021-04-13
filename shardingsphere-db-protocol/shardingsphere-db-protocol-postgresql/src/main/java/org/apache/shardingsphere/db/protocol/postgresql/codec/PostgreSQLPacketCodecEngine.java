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

package org.apache.shardingsphere.db.protocol.postgresql.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import org.apache.shardingsphere.db.protocol.codec.DatabasePacketCodecEngine;
import org.apache.shardingsphere.db.protocol.postgresql.packet.PostgreSQLIdentifierPacket;
import org.apache.shardingsphere.db.protocol.postgresql.packet.PostgreSQLPacket;
import org.apache.shardingsphere.db.protocol.postgresql.packet.generic.PostgreSQLErrorResponsePacket;
import org.apache.shardingsphere.db.protocol.postgresql.payload.PostgreSQLPacketPayload;

import java.util.List;

/**
 * Database packet codec for PostgreSQL.
 */
public final class PostgreSQLPacketCodecEngine implements DatabasePacketCodecEngine<PostgreSQLPacket> {
    
    @Override
    public boolean isValidHeader(final int readableBytes) {
        return readableBytes >= PostgreSQLPacket.MESSAGE_TYPE_LENGTH + PostgreSQLPacket.PAYLOAD_LENGTH;
    }
    
    @Override
    public void decode(final ChannelHandlerContext context, final ByteBuf in, final List<Object> out, final int readableBytes) {
        int messageTypeLength = 0;
        if ('\0' == in.markReaderIndex().readByte()) {
            in.resetReaderIndex();
        } else {
            messageTypeLength = PostgreSQLPacket.MESSAGE_TYPE_LENGTH;
        }
        int payloadLength = in.readInt();
        int realPacketLength = payloadLength + messageTypeLength;
        if (readableBytes < realPacketLength) {
            in.resetReaderIndex();
            return;
        }
        in.resetReaderIndex();
        out.add(in.readRetainedSlice(payloadLength + messageTypeLength));
    }
    
    @Override
    public void encode(final ChannelHandlerContext context, final PostgreSQLPacket message, final ByteBuf out) {
        PostgreSQLPacketPayload payload = new PostgreSQLPacketPayload(context.alloc().buffer());
        try {
            message.write(payload);
            // CHECKSTYLE:OFF
        } catch (final Exception ex) {
            // CHECKSTYLE:ON
            payload.getByteBuf().resetWriterIndex();
            PostgreSQLErrorResponsePacket postgreSQLErrorResponsePacket = new PostgreSQLErrorResponsePacket();
            postgreSQLErrorResponsePacket.addField(PostgreSQLErrorResponsePacket.FIELD_TYPE_MESSAGE, ex.getMessage());
            postgreSQLErrorResponsePacket.write(payload);
        } finally {
            if (message instanceof PostgreSQLIdentifierPacket) {
                out.writeByte(((PostgreSQLIdentifierPacket) message).getMessageType());
                out.writeInt(payload.getByteBuf().readableBytes() + PostgreSQLPacket.PAYLOAD_LENGTH);
            }
            out.writeBytes(payload.getByteBuf());
            payload.close();
        }
    }
    
    @Override
    public PostgreSQLPacketPayload createPacketPayload(final ByteBuf message) {
        return new PostgreSQLPacketPayload(message);
    }
}
