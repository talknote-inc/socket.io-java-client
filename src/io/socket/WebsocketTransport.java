package io.socket;

import java.io.IOException;
import java.lang.String;
import java.net.URI;
import java.net.URL;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

class WebsocketTransport extends WebSocketClient implements IOTransport {
    static final Logger logger = Logger.getLogger("io.socket");

    private final static Pattern PATTERN_HTTP = Pattern.compile("^http");
    public static final String TRANSPORT_NAME = "websocket";
    private IOConnection connection;
    public static IOTransport create(URL url, String query, IOConnection connection) {
        URI uri = URI.create(
                PATTERN_HTTP.matcher(url.toString()).replaceFirst("ws")
                + IOConnection.SOCKET_IO_1 + TRANSPORT_NAME
                + "/" + connection.getSessionId() + "?" + query);
        logger.info( "websocket uri:" + uri.toString() );
        return new WebsocketTransport(uri, connection);
    }

    public WebsocketTransport(URI uri, IOConnection connection) {
        super(uri);
        this.connection = connection;
        SSLContext context = IOConnection.getSslContext();

        if("wss".equals(uri.getScheme()) && context != null) {
            SSLSocketFactory factory = context.getSocketFactory();
            try {
                this.setSocket( factory.createSocket() );
            } catch (IOException e ) {
                throw new RuntimeException(e);
            }
        }
    }

    /* (non-Javadoc)
     * @see io.socket.IOTransport#disconnect()
     */
    @Override
    public void disconnect() {
        try {
            this.close();
        } catch (Exception e) {
            connection.transportError(e);
        }
    }

    /* (non-Javadoc)
     * @see io.socket.IOTransport#canSendBulk()
     */
    @Override
    public boolean canSendBulk() {
        return false;
    }

    /* (non-Javadoc)
     * @see io.socket.IOTransport#sendBulk(java.lang.String[])
     */
    @Override
    public void sendBulk(String[] texts) throws IOException {
        throw new RuntimeException("Cannot send Bulk!");
    }

    /* (non-Javadoc)
     * @see io.socket.IOTransport#invalidate()
     */
    @Override
    public void invalidate() {
        connection = null;
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        logger.info( "onClose: code=" + String.valueOf(code) + ", reason=" + reason + ", remote=" + String.valueOf(remote));
        if(connection != null)
            connection.transportDisconnected();
    }

    @Override
    public void onMessage(String text) {
        logger.info( "onMessage: " + text );
        if(connection != null)
            connection.transportMessage(text);
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        logger.info("onOpen: ");
        if(connection != null)
            connection.transportConnected();
    }

    @Override
    public String getName() {
        return TRANSPORT_NAME;
    }

    @Override
    public void onError(Exception ex) {
        // TODO Auto-generated method stub
        logger.warning( "onError: " + ex.toString() );
    }
}