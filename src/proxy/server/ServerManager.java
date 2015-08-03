package proxy.server;

import io.DispatchUADecoder;
import io.Packet;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.configuration.SubnodeConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.mina.common.IoServiceConfig;
import org.apache.mina.common.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.transport.socket.nio.SocketAcceptorConfig;
import org.apache.mina.transport.socket.nio.SocketConnector;

import proxy.DispatchUAEncoder;
import proxy.ProxyServer;

public class ServerManager {
	
	private static final Log log = LogFactory.getLog(ServerManager.class);
	
	private static final String COMMAND_ADD_SERVER = "addserver";
	
	private Map<String, GameServer> servers;
	
	private SocketConnector connector;
	
	private IoServiceConfig config;
	
	private int proxyLoginCode;
	
	public ServerManager(int proxyLoginCode) {
		servers = new HashMap<String, GameServer>();
		connector = new SocketConnector();
		config = new SocketAcceptorConfig();
		config.getFilterChain().addLast("codec", new ProtocolCodecFilter(DispatchUAEncoder.class, DispatchUADecoder.class));
	}
	
	public void load(List<SubnodeConfiguration> configs) {
		synchronized (servers) {
			servers.clear();
//			List<GameServer> list = ProxyServer.getEntityManager().query(GameServer.class, "from GameServer");
			for (SubnodeConfiguration config : configs) {
				GameServer server = new GameServer();
				server.setId(config.getInt("id"));
				server.setName(config.getString("name"));
				server.setIp(config.getString("ip"));
				servers.put(server.getIp(), server);
				log.info("Server : " + server.getName());
			}
		}
	}
	
//	public void add(String name, String ip) {
//		GameServer server = new GameServer();
//		server.setName(name);
//		server.setIp(ip);
//		ProxyServer.getEntityManager().createSync(server);
//		servers.put(ip, server);
//	}
	
//	public void delete(String ip, String password) {
//		GameServer server = ProxyServer.getEntityManager().fetch(GameServer.class, "from GameServer where ip=? and password=?", ip, password);
//		if (server != null) {
//			ProxyServer.getEntityManager().deleteSync(server);
//			servers.remove(ip);
//		}
//	}
	
//	public void update(GameServer server) {
//		ProxyServer.getEntityManager().updateSync(server);
//		servers.remove(server.getIp());
//		servers.put(server.getIp(), server);
//	}
	
	public boolean check(String ip, String password) {
		GameServer server = get(ip);
		return server != null && ProxyServer.getPassword().equals(password);
	}
	
	public GameServer get(String ip) {
		return servers.get(ip);
	}
	
	public void processCMD(String cmd) {
		if (cmd == null || cmd.length() == 0) {
			return;
		}
		
		String[] infos = cmd.split(":");
		if (infos.length == 5) {
			String password = infos[0];
			if (password.equals(ProxyServer.getPassword())) {
				String command = infos[1];
				String ip = infos[2];
				try {
					int port = Integer.parseInt(infos[3]);
					if (command.equals(COMMAND_ADD_SERVER)) {
						if (ProxyServer.getServerManager().check(ip, password)) {
							SocketAddress address = new InetSocketAddress(ip, port);
							connector.connect(address, new ServerHandler(), config);
						} else {
							log.warn("A wrong connect want to add server, ip : " + ip);
						}
					}
				} catch (Exception e) {
					return;
				}
			}
		}
	}
	
	public void registerServer(IoSession session) {
		String ip = session.getRemoteAddress().toString().replace("/", "").split(":")[0];
		GameServer server = get(ip);
		if (server.getSession() == null) {
			server.setSession(session);
			Packet packet = new Packet(proxyLoginCode);
			packet.putInt(ipToInt(ProxyServer.getClientManager().getAddress()));
			packet.putInt(ProxyServer.getClientManager().getPort());
			server.send(packet);
			log.info("registerServer, ip = " + ip);
		}
	}
	
	public static int ipToInt(String ip) {
		int ret = 0;
		String[] infos = ip.replace(".", ":").split(":");
		for (int i = 0;i < infos.length;i++) {
			ret += Integer.parseInt(infos[i]) << (24 - 8 * i);
		}
		return ret;
	}
	
	public void unregisterServer(IoSession session) {
		String ip = session.getRemoteAddress().toString().replace("/", "").split(":")[0];
		GameServer server = get(ip);
		server.setSession(null);
		ProxyServer.getClientManager().serverShutdown(server);
		log.info("unregisterServer, ip = " + ip);
	}
	
	public GameServer[] getServerList() {
		return servers.values().toArray(new GameServer[servers.size()]);
	}

}
