package proxy;

import io.Packet;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IoSession;
import org.apache.mina.filter.codec.ProtocolEncoderAdapter;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;

public class DispatchUAEncoder extends ProtocolEncoderAdapter {

	@Override
	public void encode(IoSession session, Object obj, ProtocolEncoderOutput out) throws Exception {
		if (obj instanceof DispatchPacketEx) {
			DispatchPacketEx dp = (DispatchPacketEx) obj;
			Packet packet = dp.getPacket();
			ByteBuffer data = packet.getData();
			data.flip();
			if (!dp.isCreate()) {
				data.limit(data.buf().hasArray() ? data.array().length : 0);
			}
			int len = 20 + data.remaining();
			ByteBuffer buf = ByteBuffer.allocate(len);
			buf.put(DispatchPacketEx.HEAD);
			buf.putInt(len);
			buf.putInt(dp.getId());
			buf.put(Packet.HEAD);
			buf.putInt(len - 10);
			buf.putInt(packet.getOpCode());
			buf.put(data);
			buf.flip();
			out.write(buf);
		}
	}
	
}
