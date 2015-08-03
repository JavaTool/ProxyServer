package proxy.server;

import io.Packet;

import org.apache.mina.common.IoSession;

import proxy.DispatchPacketEx;

public class GameServer {
	
	private static final byte DISPATCH_PACKET_ID = -1;
	
	private IoSession session;
	
	private int id;
	
	private String name, ip;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public IoSession getSession() {
		return session;
	}
	
	public void setSession(IoSession session) {
		this.session = session;
	}

	public void send(Packet packet) {
		send(DISPATCH_PACKET_ID, packet);
	}

	public void send(int dpId, Packet packet) {
		session.write(new DispatchPacketEx(dpId, packet, false));
	}

}
