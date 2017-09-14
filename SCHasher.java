package com.igio90;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.BitSet;

/**
 * I've spent some nice times understanding the logic, I'm not even sure if it's an hasher built by sc
 * nor if that code is the right representation of what is currently used on SC games.
 * The following implementation has been built by translating gdb assembly into java.
 * The tables are coming from Clash Of Clans, and the following code (not sure?) will probably work on
 * Boom Beach and HayDay too (with different tables). 
 * 
 * There are possible ways to patch the table to generate a different kay to match any custom ones
 * but Pinocchio is patching at an higher level which wouldn't need anything more.
 * 
 *
 * Created by igio90 on 14/09/17.
 */

public class SCHasher {
    private static final char[] HEX_TABLE = {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};

    private static final int[] SC_MAGIC_TABLE = new int[] {
            0x03D1, 0x1030, 0x1993, 0x3CBB, 0x3E1C, 0xBCB5, 0x7AB4,
            0xFB55, 0xCCE5, 0x3F07, 0xE5D0, 0x42E5, 0x0F09, 0xBD44,
            0xCCF7
    };

    private static final int[] SC_AND_TABLE = new int[] {
            0x8000, 0xC000, 0xE000, 0x8000, 0xF800, 0x0000, 0xC000,
            0x0000, 0x0000, 0xE000, 0xF800, 0xC000, 0x8000, 0xC000,
            0x0000
    };

    private static final int[] SC_SHIFT_TABLE = new int[] {
            15, 14, 13, 15, 11, 0, 14, 0, 0, 13, 11, 14, 15, 14, 1
    };

    private static final int[] SC_BIT_INDEX_TABLE = new int[] {
            1, 2, 3, 1, 5, 0, 2, 0, 0, 3, 5, 2, 1, 2, 0
    };

    private static final int SC_MAGIC_SEED = 0x7EB1;

    public static void main(String args[]) {
        String serverPubKey = unpack(SC_MAGIC_SEED);
    }
    
    static String unpack(int seed) {
        int a = littleToBig(seed);
        int b;
        int c;
        int d;

        BitSet bt;

        StringBuilder result = new StringBuilder();
        result.append(toHexString(fromInt16(SC_MAGIC_SEED)));

        for (int i=0;i<15;i++) {
            b = SC_MAGIC_TABLE[i];
            c = a ^ b;
            d = SC_AND_TABLE[i] & c;

            bt = BitSet.valueOf(fromInt16(ByteBuffer.wrap(fromInt16(c)).order(ByteOrder.LITTLE_ENDIAN).getShort()));

            if (SC_AND_TABLE[i] > 0) {
                int e = d >> SC_SHIFT_TABLE[i];

                BitSet bt2 = BitSet.valueOf(fromInt16(ByteBuffer.wrap(fromInt16(e)).order(ByteOrder.LITTLE_ENDIAN).getShort()));

                int k = 0;
                int s = SC_BIT_INDEX_TABLE[i];
                for (int m = s; m < 16; m++) {
                    bt2.set(m, bt.get(k));
                    k++;
                }

                a = toInt16(bt2.toByteArray());
            } else if (SC_SHIFT_TABLE[i] > 0) {
                b = SC_MAGIC_TABLE[i];
                c = a ^ b;
                d = c << SC_SHIFT_TABLE[i];
                bt = BitSet.valueOf(fromInt16(ByteBuffer.wrap(fromInt16(d)).order(ByteOrder.LITTLE_ENDIAN).getShort()));
                a = toInt16(bt.toByteArray());
            } else {
                a = toInt16(bt.toByteArray());
            }

            result.append(toHexString(fromInt16(a)));
            a = littleToBig(a);
        }
        
        return result.toString();
    }

    static int littleToBig(int i) {
        int b0,b1;

        b0 = (i & 0x000000ff);
        b1 = (i & 0x0000ff00) >> 8;

        return (b0 << 8) | b1;
    }

    static byte[] fromInt16(int i) {
        byte[] result = new byte[2];

        result[0] = (byte) (i >> 8);
        result[1] = (byte) (i);

        return result;
    }

    public static int toInt16(byte[] b) {
        return ByteBuffer.wrap(b).order(ByteOrder.BIG_ENDIAN).getShort();
    }

    public static String toHexString(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return "";
        }

        char[] hexChars = new char[bytes.length*2];
        int v;

        for(int j=0; j < bytes.length; j++) {
            v = bytes[j] & 0xFF;
            hexChars[j*2] = HEX_TABLE[v>>>4];
            hexChars[j*2 + 1] = HEX_TABLE[v & 0x0F];
        }

        return new String(hexChars);
    }
}
