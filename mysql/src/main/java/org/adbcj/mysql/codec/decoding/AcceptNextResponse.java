package org.adbcj.mysql.codec.decoding;

import org.adbcj.mysql.codec.BoundedInputStream;
import org.adbcj.mysql.codec.MySqlConnection;
import org.adbcj.mysql.codec.MySqlRequest;
import org.jboss.netty.channel.Channel;

import java.io.IOException;

/**
 * @author roman.stoffel@gamlor.info
 */
public class AcceptNextResponse extends DecoderState {
    private final MySqlConnection connection;

    public AcceptNextResponse(MySqlConnection connection) {
        this.connection = connection;
    }

    @Override
    public ResultAndState parse(int length, int packetNumber, BoundedInputStream in, Channel channel) throws IOException {
        final MySqlRequest request = connection.dequeRequest();
        return request.getDecoderState().parse(length, packetNumber, in, channel);
    }
}
