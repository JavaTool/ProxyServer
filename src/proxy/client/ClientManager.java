package proxy.client;

import io.DispatchPacket;
import io.DispatchUADecoder;
import io.Packet;
import io.SyncInteger;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.mina.common.IoHandlerAdapter;
import org.apache.mina.common.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.transport.socket.nio.SocketAcceptor;
import org.apache.mina.transport.socket.nio.SocketAcceptorConfig;

import proxy.DispatchUAEncoder;
import proxy.ProxyServer;
import proxy.ProxyServer.OpcodeInfo;
import proxy.server.GameServer;

public final class ClientManager {
	
	private static final Log log = LogFactory.getLog(ClientManager.class);
	
	private static final AtomicInteger id_gen = new AtomicInteger(0);

	private static final String ADDRESS = "address";
	
	private static final String PORT = "port";
	
	private static final String SESSION_COUNTER = "SessionCounter";

	private static final String SESSION_ID = "SESSION_ID";

	private SocketAcceptor acceptor;
	
	private String address;
	
	private int port, clientDisconnectCode;
	
	private Map<Integer, ClientSession> freeSessions;
	/**
	 * 代理ID生成器
	 */
	private SyncInteger ids = new SyncInteger(0);
	
	private Map<String, Map<Integer, Integer>> sessions;
	
	public ClientManager(Configuration config) {
		address = config.getString(ADDRESS);
		port = config.getInt(PORT);
		clientDisconnectCode = config.getInt("clientDisconnect");
		freeSessions = new ConcurrentHashMap<Integer, ClientSession>();
		sessions = new HashMap<String, Map<Integer, Integer>>();
	}

	public void close() {
		if (acceptor != null) {
			acceptor.unbindAll();
		}
	}

	public void bind() throws IOException {
		for (GameServer server : ProxyServer.getServerManager().getServerList()) {
			sessions.put(server.getIp(), new ConcurrentHashMap<Integer, Integer>());
		}
		
		acceptor = new SocketAcceptor();
		SocketAcceptorConfig cfg = new SocketAcceptorConfig();
		cfg.getFilterChain().addLast("codec", new ProtocolCodecFilter(DispatchUAEncoder.class, DispatchUADecoder.class));
		acceptor.bind(createSocketAddress(address, port),  new ClientSessionHandler(), cfg);
	}
	
	public static SocketAddress createSocketAddress(String address, int port) throws IOException {
		return address == null || address.length() == 0 ? new InetSocketAddress(InetAddress.getLocalHost(), port) : new InetSocketAddress(address, port);
	}
	
	public ClientSession getSession(int id) {
		return freeSessions.get(id);
	}
	
	public int getPort() {
		return port;
	}
	
	public String getAddress() {
		return address;
	}
	
	private void addSession(ClientSession session) {
		freeSessions.put(session.getId(), session);
	}
	
	private void removeFreeSession(ClientSession session) {
		freeSessions.remove(session.getId());
	}
	
	private void addToServer(ClientSession session, String ip) {
		sessions.get(ip).put(session.getId(), session.getId());
		session.setServerIp(ip);
	}
	
	private void removeSession(ClientSession session) {
		String ip = session.getServerIp();
		removeFreeSession(session);
		if (ip != null) {
			sessions.get(ip).remove(session.getId());
		}
	}
	
	public void serverShutdown(GameServer server) {
		Map<Integer, Integer> clients = sessions.get(server.getIp());
		for (Integer id : clients.values()) {
			ClientSession session = getSession(id);
			if (session != null) {
				session.getSession().close();
				removeFreeSession(session);
			}
		}
		clients.clear();
	}

	protected class ClientSessionHandler extends IoHandlerAdapter {
		
		private Map<Integer, OpcodeInfo> methods;
		
		public ClientSessionHandler() {
			methods = new HashMap<Integer, OpcodeInfo>();
			for (OpcodeInfo opcode : ProxyServer.getOpcodes()) {
				methods.put(opcode.opcode, opcode);
			}
		}
		
		@Override
		public void exceptionCaught(IoSession session, Throwable t) throws Exception {
			log.debug(t.getMessage(), t);
		}

		@Override
		public void messageReceived(IoSession session, Object msg) throws Exception {
			if (msg instanceof DispatchPacket) {
				DispatchPacket dp = (DispatchPacket) msg;
				int sessionId = (Integer) session.getAttribute(SESSION_ID); // 这个是dispatch跟proxy连接的Id，每多一个proxy就增1
				ClientSession cs = getSession(sessionId);
				if (cs == null) {
					sessionId = id_gen.incrementAndGet(); // 这个是ClientSession全局唯一的标记，所有的ClientSession，不论是如何建立的，都有一个唯一的Id
					cs = new ClientSession(session, sessionId);
					addSession(cs);
				}
				
				OpcodeInfo info = methods.get(dp.getPacket().getOpCode());
				if (info != null) {
					getClass().getMethod(info.method, DispatchPacket.class, ClientSession.class, int.class).invoke(this, dp, cs, info.returnCode);
				} else {
					GameServer server = ProxyServer.getServerManager().get(cs.getServerIp());
					if (server != null) {
						server.send(sessionId, dp.getPacket());
					} else {
						log.debug("No server[" + cs.getServerIp() + "].");
					}
				}
			}
		}
		
		public void sendServerList(DispatchPacket dp, ClientSession session, int returnCode) {
			Packet packet = new Packet(returnCode);
			GameServer[] serverList = ProxyServer.getServerManager().getServerList();
			packet.putInt(serverList.length);
			for (GameServer server : serverList) {
				packet.putUTF(server.getName());
				packet.putUTF(server.getIp());
				packet.putInt(server.getSession() == null ? 0 : 1);
			}
			session.send(packet);
		}
		
		public void selectServer(DispatchPacket dp, ClientSession session, int returnCode) {
			String ip = dp.getPacket().getString();
			GameServer server = ProxyServer.getServerManager().get(ip);
			if (server != null) {
				addToServer(session, ip);
				Packet packet = new Packet(returnCode);
				session.send(packet);
			}
		}

		@Override
		public void sessionCreated(IoSession session) throws Exception {
			session.setAttribute(SESSION_ID, ids.incrementAndGet());
			session.setAttribute(SESSION_COUNTER, new SyncInteger(0));
		}

		@Override
		public void sessionClosed(IoSession session) throws Exception {
			super.sessionClosed(session);
			int sessionId = (Integer) session.getAttribute(SESSION_ID);
			ClientSession cs = getSession(sessionId);
			if (cs != null) {
				GameServer server = ProxyServer.getServerManager().get(cs.getServerIp());
				if (server != null) {
					server.send(sessionId, new Packet(clientDisconnectCode));
				}
				removeSession(cs);
			}
		}
		
	}

}
