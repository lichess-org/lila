package org.lichess.compression.game;

import org.lichess.compression.BitReader;
import org.lichess.compression.BitWriter;

class Huffman {
    public static void write(int value, BitWriter writer) {
        Symbol symbol = CODES[value];
        writer.writeBits(symbol.code, symbol.bits);
    }

    public static int read(BitReader reader) {
        Node node = ROOT;
        while (node.zero != null && node.one != null) {
            int bit = reader.readBits(1);
            if (bit == 0) node = node.zero;
            else node = node.one;
        }
        return node.leaf;
    }

    private static class Symbol {
        public final int code;
        public final int bits;

        public Symbol(int code, int bits) {
            this.code = code;
            this.bits = bits;
        }
    }

    private static class Node {
        public Node zero;
        public Node one;
        public int leaf;

        public Node(int leaf) {
            this.leaf = leaf;
        }

        public Node(Node zero, Node one) {
            this.zero = zero;
            this.one = one;
            this.leaf = -1;
        }
    }

    private static Node buildTree(int code, int bits) {
        assert bits <= 32;

        for (int i = 0; i <= 0xff; i++) {
            if (CODES[i].code == code && CODES[i].bits == bits) {
                return new Node(i);
            }
        }

        return new Node(
            buildTree(code << 1, bits + 1),
            buildTree((code << 1) | 1, bits + 1));
    }

    private static final Symbol CODES[] = {
        new Symbol(0b111, 3), // 0
        new Symbol(0b100, 3), // 1
        new Symbol(0b000, 3), // 2
        new Symbol(0b1100, 4), // 3
        new Symbol(0b0101, 4), // 4
        new Symbol(0b0010, 4), // 5
        new Symbol(0b10110, 5), // 6
        new Symbol(0b01101, 5), // 7
        new Symbol(0b01000, 5), // 8
        new Symbol(0b00110, 5), // 9
        new Symbol(0b110101, 6), // 10
        new Symbol(0b101111, 6), // 11
        new Symbol(0b101011, 6), // 12
        new Symbol(0b110100, 6), // 13
        new Symbol(0b101010, 6), // 14
        new Symbol(0b101000, 6), // 15
        new Symbol(0b011111, 6), // 16
        new Symbol(0b011110, 6), // 17
        new Symbol(0b011100, 6), // 18
        new Symbol(0b010011, 6), // 19
        new Symbol(0b011000, 6), // 20
        new Symbol(0b010010, 6), // 21
        new Symbol(0b001110, 6), // 22
        new Symbol(0b1101111, 7), // 23
        new Symbol(0b1101110, 7), // 24
        new Symbol(0b1101101, 7), // 25
        new Symbol(0b1101100, 7), // 26
        new Symbol(0b1011100, 7), // 27
        new Symbol(0b1010011, 7), // 28
        new Symbol(0b0111011, 7), // 29
        new Symbol(0b0110011, 7), // 30
        new Symbol(0b0110010, 7), // 31
        new Symbol(0b0011110, 7), // 32
        new Symbol(0b10111010, 8), // 33
        new Symbol(0b10100101, 8), // 34
        new Symbol(0b01110101, 8), // 35
        new Symbol(0b00111111, 8), // 36
        new Symbol(0b101110111, 9), // 37
        new Symbol(0b101001001, 9), // 38
        new Symbol(0b011101000, 9), // 39
        new Symbol(0b001111100, 9), // 40
        new Symbol(0b1010010001, 10), // 41
        new Symbol(0b0111010011, 10), // 42
        new Symbol(0b10111011011, 11), // 43
        new Symbol(0b10100100001, 11), // 44
        new Symbol(0b00111110110, 11), // 45
        new Symbol(0b101110110010, 12), // 46
        new Symbol(0b011101001010, 12), // 47
        new Symbol(0b001111101000, 12), // 48
        new Symbol(0b1011101100010, 13), // 49
        new Symbol(0b0011111011110, 13), // 50
        new Symbol(0b0011111010010, 13), // 51
        new Symbol(0b10111011000000, 14), // 52
        new Symbol(0b10100100000000, 14), // 53
        new Symbol(0b00111110101101, 14), // 54
        new Symbol(0b101110110101011, 15), // 55
        new Symbol(0b101110110011110, 15), // 56
        new Symbol(0b101110110001110, 15), // 57
        new Symbol(0b101110110011010, 15), // 58
        new Symbol(0b101110110000100, 15), // 59
        new Symbol(0b101110110000011, 15), // 60
        new Symbol(0b101001000001011, 15), // 61
        new Symbol(0b101001000001000, 15), // 62
        new Symbol(0b101001000001001, 15), // 63
        new Symbol(0b101001000000100, 15), // 64
        new Symbol(0b101001000000110, 15), // 65
        new Symbol(0b101001000000011, 15), // 66
        new Symbol(0b011101001011111, 15), // 67
        new Symbol(0b011101001011110, 15), // 68
        new Symbol(0b011101001011100, 15), // 69
        new Symbol(0b011101001011011, 15), // 70
        new Symbol(0b011101001011010, 15), // 71
        new Symbol(0b011101001011000, 15), // 72
        new Symbol(0b011101001001111, 15), // 73
        new Symbol(0b011101001001110, 15), // 74
        new Symbol(0b011101001001100, 15), // 75
        new Symbol(0b011101001001011, 15), // 76
        new Symbol(0b011101001001010, 15), // 77
        new Symbol(0b011101001001000, 15), // 78
        new Symbol(0b011101001000111, 15), // 79
        new Symbol(0b011101001000101, 15), // 80
        new Symbol(0b011101001000100, 15), // 81
        new Symbol(0b011101001000010, 15), // 82
        new Symbol(0b011101001000001, 15), // 83
        new Symbol(0b011101001000000, 15), // 84
        new Symbol(0b001111101111111, 15), // 85
        new Symbol(0b001111101111101, 15), // 86
        new Symbol(0b001111101111100, 15), // 87
        new Symbol(0b001111101110110, 15), // 88
        new Symbol(0b001111101110101, 15), // 89
        new Symbol(0b001111101110100, 15), // 90
        new Symbol(0b001111101110010, 15), // 91
        new Symbol(0b001111101110001, 15), // 92
        new Symbol(0b001111101110000, 15), // 93
        new Symbol(0b001111101011110, 15), // 94
        new Symbol(0b001111101011101, 15), // 95
        new Symbol(0b001111101011100, 15), // 96
        new Symbol(0b001111101011001, 15), // 97
        new Symbol(0b001111101010111, 15), // 98
        new Symbol(0b001111101010110, 15), // 99
        new Symbol(0b001111101010100, 15), // 100
        new Symbol(0b001111101010011, 15), // 101
        new Symbol(0b001111101010010, 15), // 102
        new Symbol(0b001111101010001, 15), // 103
        new Symbol(0b001111101001111, 15), // 104
        new Symbol(0b001111101001110, 15), // 105
        new Symbol(0b001111101001100, 15), // 106
        new Symbol(0b1011101101011111, 16), // 107
        new Symbol(0b1011101101011110, 16), // 108
        new Symbol(0b1011101101011100, 16), // 109
        new Symbol(0b1011101101011011, 16), // 110
        new Symbol(0b1011101101011010, 16), // 111
        new Symbol(0b1011101101011000, 16), // 112
        new Symbol(0b1011101101010101, 16), // 113
        new Symbol(0b1011101101010100, 16), // 114
        new Symbol(0b1011101101010010, 16), // 115
        new Symbol(0b1011101101010001, 16), // 116
        new Symbol(0b1011101101010000, 16), // 117
        new Symbol(0b1011101101001110, 16), // 118
        new Symbol(0b1011101101001101, 16), // 119
        new Symbol(0b1011101101001100, 16), // 120
        new Symbol(0b1011101101001010, 16), // 121
        new Symbol(0b1011101101001001, 16), // 122
        new Symbol(0b1011101101001000, 16), // 123
        new Symbol(0b1011101101000110, 16), // 124
        new Symbol(0b1011101101000101, 16), // 125
        new Symbol(0b1011101101000100, 16), // 126
        new Symbol(0b1011101101000011, 16), // 127
        new Symbol(0b1011101101000001, 16), // 128
        new Symbol(0b1011101101000000, 16), // 129
        new Symbol(0b1011101100111111, 16), // 130
        new Symbol(0b1011101100111011, 16), // 131
        new Symbol(0b1011101100111010, 16), // 132
        new Symbol(0b1011101100111001, 16), // 133
        new Symbol(0b1011101100110111, 16), // 134
        new Symbol(0b1011101100110110, 16), // 135
        new Symbol(0b1011101100110011, 16), // 136
        new Symbol(0b1011101100110001, 16), // 137
        new Symbol(0b1011101100110000, 16), // 138
        new Symbol(0b1011101100011111, 16), // 139
        new Symbol(0b1011101100011011, 16), // 140
        new Symbol(0b1011101100011010, 16), // 141
        new Symbol(0b1011101100011001, 16), // 142
        new Symbol(0b1011101100001111, 16), // 143
        new Symbol(0b1011101100001110, 16), // 144
        new Symbol(0b1011101100001101, 16), // 145
        new Symbol(0b1011101100001011, 16), // 146
        new Symbol(0b1011101100001010, 16), // 147
        new Symbol(0b1011101100000101, 16), // 148
        new Symbol(0b1010010000011111, 16), // 149
        new Symbol(0b1010010000011110, 16), // 150
        new Symbol(0b1010010000011101, 16), // 151
        new Symbol(0b1010010000011011, 16), // 152
        new Symbol(0b1010010000011010, 16), // 153
        new Symbol(0b1010010000011001, 16), // 154
        new Symbol(0b1010010000010101, 16), // 155
        new Symbol(0b1010010000010100, 16), // 156
        new Symbol(0b1010010000001111, 16), // 157
        new Symbol(0b1010010000001011, 16), // 158
        new Symbol(0b1010010000001010, 16), // 159
        new Symbol(0b1010010000000101, 16), // 160
        new Symbol(0b0111010010111011, 16), // 161
        new Symbol(0b0111010010111010, 16), // 162
        new Symbol(0b0111010010110011, 16), // 163
        new Symbol(0b0111010010011011, 16), // 164
        new Symbol(0b0111010010011010, 16), // 165
        new Symbol(0b0111010010010011, 16), // 166
        new Symbol(0b0111010010001101, 16), // 167
        new Symbol(0b0111010010001100, 16), // 168
        new Symbol(0b0111010010000111, 16), // 169
        new Symbol(0b0011111011111101, 16), // 170
        new Symbol(0b0011111011111100, 16), // 171
        new Symbol(0b0011111011101111, 16), // 172
        new Symbol(0b0011111011100111, 16), // 173
        new Symbol(0b0011111011100110, 16), // 174
        new Symbol(0b0011111010111111, 16), // 175
        new Symbol(0b0011111010110001, 16), // 176
        new Symbol(0b0011111010110000, 16), // 177
        new Symbol(0b0011111010101011, 16), // 178
        new Symbol(0b0011111010100001, 16), // 179
        new Symbol(0b0011111010100000, 16), // 180
        new Symbol(0b0011111010011011, 16), // 181
        new Symbol(0b10111011010111011, 17), // 182
        new Symbol(0b10111011010111010, 17), // 183
        new Symbol(0b10111011010110011, 17), // 184
        new Symbol(0b10111011010100111, 17), // 185
        new Symbol(0b10111011010100110, 17), // 186
        new Symbol(0b10111011010011111, 17), // 187
        new Symbol(0b10111011010010111, 17), // 188
        new Symbol(0b10111011010010110, 17), // 189
        new Symbol(0b10111011010001111, 17), // 190
        new Symbol(0b10111011010000101, 17), // 191
        new Symbol(0b10111011010000100, 17), // 192
        new Symbol(0b10111011001111101, 17), // 193
        new Symbol(0b10111011001110001, 17), // 194
        new Symbol(0b10111011001110000, 17), // 195
        new Symbol(0b10111011001100101, 17), // 196
        new Symbol(0b10111011000111101, 17), // 197
        new Symbol(0b10111011000111100, 17), // 198
        new Symbol(0b10111011000110001, 17), // 199
        new Symbol(0b10111011000011001, 17), // 200
        new Symbol(0b10111011000011000, 17), // 201
        new Symbol(0b10111011000001001, 17), // 202
        new Symbol(0b10100100000111001, 17), // 203
        new Symbol(0b10100100000111000, 17), // 204
        new Symbol(0b10100100000110001, 17), // 205
        new Symbol(0b10100100000011101, 17), // 206
        new Symbol(0b10100100000011100, 17), // 207
        new Symbol(0b10100100000001001, 17), // 208
        new Symbol(0b01110100101100101, 17), // 209
        new Symbol(0b01110100101100100, 17), // 210
        new Symbol(0b01110100100100101, 17), // 211
        new Symbol(0b01110100100001101, 17), // 212
        new Symbol(0b01110100100001100, 17), // 213
        new Symbol(0b00111110111011101, 17), // 214
        new Symbol(0b00111110101111101, 17), // 215
        new Symbol(0b00111110101111100, 17), // 216
        new Symbol(0b00111110101010101, 17), // 217
        new Symbol(0b00111110100110101, 17), // 218
        new Symbol(0b00111110100110100, 17), // 219
        new Symbol(0b101110110101100101, 18), // 220
        new Symbol(0b101110110100111101, 18), // 221
        new Symbol(0b101110110100111100, 18), // 222
        new Symbol(0b101110110100011101, 18), // 223
        new Symbol(0b101110110011111001, 18), // 224
        new Symbol(0b101110110011111000, 18), // 225
        new Symbol(0b101110110011001001, 18), // 226
        new Symbol(0b101110110001100001, 18), // 227
        new Symbol(0b101110110001100000, 18), // 228
        new Symbol(0b101110110000010001, 18), // 229
        new Symbol(0b101001000001100001, 18), // 230
        new Symbol(0b101001000001100000, 18), // 231
        new Symbol(0b101001000000010001, 18), // 232
        new Symbol(0b011101001001001001, 18), // 233
        new Symbol(0b011101001001001000, 18), // 234
        new Symbol(0b001111101110111001, 18), // 235
        new Symbol(0b001111101010101001, 18), // 236
        new Symbol(0b001111101010101000, 18), // 237
        new Symbol(0b1011101101011001001, 19), // 238
        new Symbol(0b1011101101000111001, 19), // 239
        new Symbol(0b1011101101000111000, 19), // 240
        new Symbol(0b1011101100110010001, 19), // 241
        new Symbol(0b1011101100000100001, 19), // 242
        new Symbol(0b1011101100000100000, 19), // 243
        new Symbol(0b1010010000000100001, 19), // 244
        new Symbol(0b0011111011101110001, 19), // 245
        new Symbol(0b0011111011101110000, 19), // 246
        new Symbol(0b10111011010110010000, 20), // 247
        new Symbol(0b10111011001100100001, 20), // 248
        new Symbol(0b10111011001100100000, 20), // 249
        new Symbol(0b10100100000001000000, 20), // 250
        new Symbol(0b101110110101100100011, 21), // 251
        new Symbol(0b101110110101100100010, 21), // 252
        new Symbol(0b101001000000010000010, 21), // 253
        new Symbol(0b1010010000000100000111, 22), // 254
        new Symbol(0b1010010000000100000110, 22), // 255
    };

    private static final Node ROOT = buildTree(0, 0);
}
