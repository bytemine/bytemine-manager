/**************************************************************************
 * http://www.javaworld.com/javaworld/javatips/jw-javatip111.html		  *
 * http://www.bytemine.net/                                               *
 *************************************************************************/

package net.bytemine.manager.update;

import java.net.*;
import java.util.logging.Logger;
import java.io.*;
import javax.net.ssl.*;


/**
 * SSLSocketFactory for tunneling sslsockets through a proxy
 */
public class SSLTunnelSocketFactory extends SSLSocketFactory {
    private static Logger logger = Logger.getLogger(SSLTunnelSocketFactory.class.getName());

    private SSLSocketFactory dfactory;

    private String tunnelHost;

    private int tunnelPort;

    SSLTunnelSocketFactory(String proxyhost, String proxyport) {
        tunnelHost = proxyhost;
        tunnelPort = Integer.parseInt(proxyport);
        dfactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
    }

    public Socket createSocket(String host, int port) throws IOException,
            UnknownHostException {
        return createSocket(null, host, port, true);
    }

    public Socket createSocket(String host, int port, InetAddress clientHost,
                               int clientPort) throws IOException {
        return createSocket(null, host, port, true);
    }

    public Socket createSocket(InetAddress host, int port) throws IOException {
        return createSocket(null, host.getHostName(), port, true);
    }

    public Socket createSocket(InetAddress address, int port,
                               InetAddress clientAddress, int clientPort) throws IOException {
        return createSocket(null, address.getHostName(), port, true);
    }

    public Socket createSocket(Socket s, String host, int port,
                               boolean autoClose) throws IOException {

        Socket tunnel = new Socket(tunnelHost, tunnelPort);

        doTunnelHandshake(tunnel, host, port);

        SSLSocket result = (SSLSocket) dfactory.createSocket(tunnel, host,
                port, autoClose);

        result.addHandshakeCompletedListener(event -> {
            logger.fine("Handshake finished!");
            logger.fine("\t CipherSuite:" + event.getCipherSuite());
            logger.fine("\t SessionId " + event.getSession());
            logger.fine("\t PeerHost "
                    + event.getSession().getPeerHost());
        });

        result.startHandshake();

        return result;
    }

    private void doTunnelHandshake(Socket tunnel, String host, int port)
            throws IOException {
        OutputStream out = tunnel.getOutputStream();
        String javaVersion = "Java/" + System.getProperty("java.version");
        String userAgent = System.getProperty("http.agent") == null ? javaVersion : System.getProperty("http.agent") + " " + javaVersion;
        String msg = "CONNECT " + host + ":" + port + " HTTP/1.0\n"
                + "User-Agent: "
                + userAgent
                + "\r\n\r\n";
        byte b[];
        try {
            /*
                * We really do want ASCII7 -- the http protocol doesn't change
                * with locale.
                */
            b = msg.getBytes("ASCII7");
        } catch (UnsupportedEncodingException ignored) {
            /*
                * If ASCII7 isn't there, something serious is wrong, but
                * Paranoia Is Good (tm)
                */
            b = msg.getBytes();
        }
        out.write(b);
        out.flush();

        /*
           * We need to store the reply so we can create a detailed
           * error message to the user.
           */
        byte reply[] = new byte[200];
        int replyLen = 0;
        int newlinesSeen = 0;
        boolean headerDone = false; /* Done on first newline */

        InputStream in = tunnel.getInputStream();

        while (newlinesSeen < 2) {
            int i = in.read();
            if (i < 0) {
                throw new IOException("Unexpected EOF from proxy");
            }
            if (i == '\n') {
                headerDone = true;
                ++newlinesSeen;
            } else if (i != '\r') {
                newlinesSeen = 0;
                if (!headerDone && replyLen < reply.length) {
                    reply[replyLen++] = (byte) i;
                }
            }
        }

        /*
           * Converting the byte array to a string is slightly wasteful
           * in the case where the connection was successful, but it's
           * insignificant compared to the network overhead.
           */
        String replyStr;
        try {
            replyStr = new String(reply, 0, replyLen, "ASCII7");
        } catch (UnsupportedEncodingException ignored) {
            replyStr = new String(reply, 0, replyLen);
        }

        /* Look for 200 connection established */
        if (!replyStr.toLowerCase().contains("200 connection established")) {
            throw new IOException("Unable to tunnel through " + tunnelHost
                    + ":" + tunnelPort + ".  Proxy returns \"" + replyStr
                    + "\"");
        }

        /* tunneling Handshake was successful! */
    }

    public String[] getDefaultCipherSuites() {
        return dfactory.getDefaultCipherSuites();
    }

    public String[] getSupportedCipherSuites() {
        return dfactory.getSupportedCipherSuites();
    }
}