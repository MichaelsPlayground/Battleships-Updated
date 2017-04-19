// Author: Jose Perez <josegperez@mail.com> and Diego Reynoso
package edu.utep.cs.cs4330.battleship.network.packet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class PacketHit extends Packet {
    public int X;
    public int Y;

    public PacketHit(DataInputStream input) throws IOException {
        super(PacketID.HIT);
        X = input.readInt();
        Y = input.readInt();
    }

    public PacketHit(int X, int Y) {
        super(PacketID.HIT);
        this.X = X;
        this.X = Y;
    }

    @Override
    public void sendPacket(DataOutputStream output) throws IOException {
        output.writeInt(X);
        output.writeInt(Y);
    }
}