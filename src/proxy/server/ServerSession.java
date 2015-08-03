package proxy.server;

import io.DispatchPacket;
import io.Packet;

import org.apache.mina.common.IoSession;

public class ServerSession {
	
	private static final byte DISPATCH_PACKET_ID = -1;
	
	private IoSession session;
	
	private GameServer serverInfo;
	
	public ServerSession(IoSession session, GameServer serverInfo) {
		this.session = session;
		this.serverInfo = serverInfo;
	}

	public IoSession getSession() {
		return session;
	}

	public GameServer getServerInfo() {
		return serverInfo;
	}

	public void send(Packet packet) {
		session.write(new DispatchPacket(DISPATCH_PACKET_ID, packet));
	}

}
