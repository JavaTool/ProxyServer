package proxy;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.List;

import net.io.mina.server.HandlerDispatch;
import net.io.mina.server.HandlerDispatchManager;
import net.io.mina.server.IOLog;

import org.apache.commons.configuration.SubnodeConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import proxy.client.ClientManager;
import proxy.server.ServerManager;

public class ProxyServer implements Runnable {
	
	private static final Log log = LogFactory.getLog(ProxyServer.class);
	
	private static final int SYSTEM_SLEEP_TIME = 50;
	
//	private static EntityManager entityManager;
	
	private static XMLConfiguration configuration;
	
	private static String password;
	
	private static ClientManager clientManager;
	
	private static ServerManager serverManager;
	
	private static OpcodeInfo[] opcodes;
	
	private DatagramSocket localSocket;
	
	public static void main(String[] args0) {
		log.info("ProxyServer load.");
//		entityManager = new EntityManagerImpl();
		new Thread(new ProxyServer(), "ProxyServer").start();
	}
	
	@SuppressWarnings("unchecked")
	private ProxyServer() {
		try {
			IOLog log = new CIOLog();
			HandlerDispatch playerDispatch = new HandlerDispatch(HandlerDispatch.PLAYER, log);
			HandlerDispatchManager.add(playerDispatch);
			configuration = new XMLConfiguration("data/config.xml");
			
			List<SubnodeConfiguration> list = configuration.configurationsAt("opcodes.opcode");
			opcodes = new OpcodeInfo[list.size()];
			for (int i = 0;i < list.size();i++) {
				SubnodeConfiguration snc = list.get(i);
				opcodes[i] = new OpcodeInfo(snc.getInt("id"), snc.getInt("return"), snc.getString("method"));
			}
			
			SubnodeConfiguration sub = configuration.configurationAt("proxy");
			clientManager = new ClientManager(sub);
			password = sub.getString("password");
			serverManager = new ServerManager(sub.getInt("opcode"));
			serverManager.load(configuration.configurationsAt("server"));
			clientManager.bind();
			SubnodeConfiguration subnodeConfiguration = configuration.configurationAt("proxy");
//			localSocket = new DatagramSocket(configuration.configurationAt("proxy").getInt("port"), InetAddress.getLocalHost());
			localSocket = new DatagramSocket(ClientManager.createSocketAddress(subnodeConfiguration.getString("address"), subnodeConfiguration.getInt("port")));
			log.info("Proxy start finish.");
		} catch (Exception e) {
			log.error("configuration = " + configuration, e);
			System.exit(0);
		}
	}

//	public static EntityManager getEntityManager() {
//		return entityManager;
//	}
	
	public static String getPassword() {
		return password;
	}
	
	public static ServerManager getServerManager() {
		return serverManager;
	}
	
	public static ClientManager getClientManager() {
		return clientManager;
	}
	
	public static OpcodeInfo[] getOpcodes() {
		return opcodes;
	}

	@Override
	public void run() {
		while (true) {
			try {
				long time = System.currentTimeMillis();
				
				try {
					byte[] buf = new byte[localSocket.getReceiveBufferSize()];
					DatagramPacket getPacket = new DatagramPacket(buf, buf.length);
					localSocket.receive(getPacket);
					String cmd = new String(buf, 0, getPacket.getLength());
					serverManager.processCMD(cmd);
				} catch (Exception e) {
					log.error("UDP has exception.", e);
				}
				
				time = System.currentTimeMillis() - time;
				
				if (time < SYSTEM_SLEEP_TIME) {
					synchronized (this) {
						wait(SYSTEM_SLEEP_TIME - time);
					}
				} else {
					Thread.yield();
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	private static class CIOLog implements IOLog {

		@Override
		public void info(String paramString) {
			log.info(paramString);
		}

		@Override
		public void error(Exception e) {
			log.error(e.getMessage(), e);
		}

		@Override
		public void debug(String paramString, Throwable paramThrowable) {
			log.debug(paramString, paramThrowable);
		}
		
	}
	
	public static class OpcodeInfo {
		
		public final int opcode, returnCode;
		
		public final String method;
		
		public OpcodeInfo(int opcode, int returnCode, String method) {
			this.opcode = opcode;
			this.method = method;
			this.returnCode = returnCode;
		}
		
	}

}
