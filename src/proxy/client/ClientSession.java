package proxy.client;

import io.Packet;

import org.apache.mina.common.IoSession;

import proxy.DispatchPacketEx;

public class ClientSession {
	
	private static final byte DISPATCH_PACKET_ID = -1;
	
	private IoSession session;
	
	private String serverIp;
	
	private int id;
	
	public ClientSession(IoSession session, int id) {
		this.session = session;
		this.id = id;
	}

	public IoSession getSession() {
		return session;
	}

	public String getServerIp() {
		return serverIp;
	}

	public void setServerIp(String serverIp) {
		this.serverIp = serverIp;
	}

	public void send(Packet packet) {
		send(new DispatchPacketEx(DISPATCH_PACKET_ID, packet, true));
	}

	public void send(DispatchPacketEx packet) {
		session.write(packet);
	}
	
	public int getId() {
		return id;
	}

}
