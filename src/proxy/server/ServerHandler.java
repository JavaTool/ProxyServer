package proxy.server;

import io.DispatchPacket;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.mina.common.IoHandlerAdapter;
import org.apache.mina.common.IoSession;

import proxy.DispatchPacketEx;
import proxy.ProxyServer;
import proxy.client.ClientSession;

public class ServerHandler extends IoHandlerAdapter {
	
	private static final Log log = LogFactory.getLog(ServerHandler.class);

	@Override  
	public void exceptionCaught(IoSession session, Throwable cause) throws Exception {  
		log.debug(cause.getMessage(), cause);
	}

	@Override  
	public void messageReceived(IoSession session, Object message) throws Exception {  
		if (message instanceof DispatchPacket) {
			DispatchPacket packet = (DispatchPacket) message;
			ClientSession cs = ProxyServer.getClientManager().getSession(packet.getId());
			if (cs != null) {
				cs.send(new DispatchPacketEx(packet.getId(), packet.getPacket(), false));
			}
		}
	}

	@Override  
	public void sessionClosed(IoSession session) throws Exception {
		ProxyServer.getServerManager().unregisterServer(session);
	}

	@Override  
	public void sessionCreated(IoSession session) throws Exception {
		ProxyServer.getServerManager().registerServer(session);
	}

}
