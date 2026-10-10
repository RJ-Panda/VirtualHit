package com.virtualhit.app;

import android.util.Log;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class SocketClient {
    private static final String TAG = "VirtualHitSocket";
    
    // Header & Command Constants matching server protocol
    private static final byte[] MAGIC_HEADER = new byte[]{(byte) 0x56, (byte) 0x48}; // 'V', 'H'
    private static final byte PROTOCOL_VERSION = 1;
    private static final byte CMD_INPUT_UPDATE = 1;
    private static final byte CMD_SOCD_CHANGE = 3;

    private String host = "127.0.0.1";
    private int port = 8888;

    private Socket socket;
    private DataOutputStream outputStream;
    private final ExecutorService networkExecutor = Executors.newSingleThreadExecutor();
    private final AtomicBoolean isConnected = new AtomicBoolean(false);
    private int sequenceNumber = 0;

    public interface ConnectionListener {
        void onConnectionStateChanged(boolean connected, String message);
    }

    private ConnectionListener connectionListener;

    public SocketClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void setConnectionListener(ConnectionListener listener) {
        this.connectionListener = listener;
    }

    public void setHostAndPort(String host, int port) {
        this.host = host;
        this.port = port;
        disconnect();
        connect();
    }

    public void connect() {
        networkExecutor.execute(() -> {
            if (isConnected.get()) return;
            notifyState(false, "Connecting to " + host + ":" + port + "...");
            try {
                socket = new Socket();
                socket.setTcpNoDelay(true); // Disable Nagle's algorithm for sub-ms latency!
                socket.setSendBufferSize(1024);
                socket.connect(new InetSocketAddress(host, port), 2000);
                outputStream = new DataOutputStream(socket.getOutputStream());
                isConnected.set(true);
                notifyState(true, "Connected to " + host + ":" + port);
                Log.i(TAG, "Successfully connected to VirtualHit server");
            } catch (IOException e) {
                Log.e(TAG, "Connection failed: " + e.getMessage());
                notifyState(false, "Connection Failed: " + e.getMessage());
                closeQuietly();
            }
        });
    }

    public void disconnect() {
        networkExecutor.execute(() -> {
            closeQuietly();
            notifyState(false, "Disconnected");
        });
    }

    public boolean isConnected() {
        return isConnected.get();
    }

    public void sendButtonMask(final int mask) {
        sendInputState(mask, (short) 0, (short) 0, (short) 0, (short) 0);
    }

    public void sendInputState(final int mask, final short lx, final short ly, final short rx, final short ry) {
        if (!isConnected.get()) return;

        networkExecutor.execute(() -> {
            if (!isConnected.get() || outputStream == null) return;
            try {
                sequenceNumber++;
                // 24-byte Binary Packet:
                // Header (2B: 'VH') | Version (1B) | Cmd (1B) | Sequence (4B) | Mask (4B) | LX (2B) | LY (2B) | RX (2B) | RY (2B) | Timestamp (4B)
                outputStream.write(MAGIC_HEADER);
                outputStream.writeByte(PROTOCOL_VERSION);
                outputStream.writeByte(CMD_INPUT_UPDATE);
                outputStream.writeInt(sequenceNumber);
                outputStream.writeInt(mask);
                outputStream.writeShort(lx);
                outputStream.writeShort(ly);
                outputStream.writeShort(rx);
                outputStream.writeShort(ry);
                outputStream.writeInt((int) (System.currentTimeMillis() & 0xFFFFFFFFL));
                outputStream.flush();
            } catch (IOException e) {
                Log.e(TAG, "Error sending packet: " + e.getMessage());
                closeQuietly();
                notifyState(false, "Connection Lost");
            }
        });
    }

    public void sendSocdModeChange(final int mode) {
        if (!isConnected.get()) return;
        networkExecutor.execute(() -> {
            if (!isConnected.get() || outputStream == null) return;
            try {
                sequenceNumber++;
                outputStream.write(MAGIC_HEADER);
                outputStream.writeByte(PROTOCOL_VERSION);
                outputStream.writeByte(CMD_SOCD_CHANGE);
                outputStream.writeInt(sequenceNumber);
                outputStream.writeInt(mode);
                outputStream.writeInt(0);
                outputStream.flush();
            } catch (IOException e) {
                Log.e(TAG, "Error sending SOCD change: " + e.getMessage());
            }
        });
    }

    private void closeQuietly() {
        isConnected.set(false);
        try {
            if (outputStream != null) {
                outputStream.close();
                outputStream = null;
            }
            if (socket != null) {
                socket.close();
                socket = null;
            }
        } catch (IOException ignored) {}
    }

    private void notifyState(boolean connected, String message) {
        if (connectionListener != null) {
            connectionListener.onConnectionStateChanged(connected, message);
        }
    }
}
