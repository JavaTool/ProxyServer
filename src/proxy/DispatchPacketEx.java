package proxy;

import net.io.mina.DispatchPacket;
import net.io.mina.Packet;

public class DispatchPacketEx extends DispatchPacket {
	
	private final boolean isCreate;

	public DispatchPacketEx(int id, Packet packet, boolean isCreate) {
		super(id, packet);
		this.isCreate = isCreate;
	}

	public boolean isCreate() {
		return isCreate;
	}

}
