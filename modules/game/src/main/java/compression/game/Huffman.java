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
        new Symbol(0b1100, 4), // 0
        new Symbol(0b0110, 4), // 1
        new Symbol(0b0011, 4), // 2
        new Symbol(0b11110, 5), // 3
        new Symbol(0b11010, 5), // 4
        new Symbol(0b10010, 5), // 5
        new Symbol(0b10100, 5), // 6
        new Symbol(0b01111, 5), // 7
        new Symbol(0b10011, 5), // 8
        new Symbol(0b01110, 5), // 9
        new Symbol(0b01001, 5), // 10
        new Symbol(0b10000, 5), // 11
        new Symbol(0b01010, 5), // 12
        new Symbol(0b11100, 5), // 13
        new Symbol(0b00101, 5), // 14
        new Symbol(0b01000, 5), // 15
        new Symbol(0b00010, 5), // 16
        new Symbol(0b00001, 5), // 17
        new Symbol(0b00011, 5), // 18
        new Symbol(0b111111, 6), // 19
        new Symbol(0b111011, 6), // 20
        new Symbol(0b110111, 6), // 21
        new Symbol(0b111110, 6), // 22
        new Symbol(0b110110, 6), // 23
        new Symbol(0b101111, 6), // 24
        new Symbol(0b101110, 6), // 25
        new Symbol(0b101101, 6), // 26
        new Symbol(0b101011, 6), // 27
        new Symbol(0b100011, 6), // 28
        new Symbol(0b101010, 6), // 29
        new Symbol(0b100010, 6), // 30
        new Symbol(0b010111, 6), // 31
        new Symbol(0b001001, 6), // 32
        new Symbol(0b001000, 6), // 33
        new Symbol(0b000000, 6), // 34
        new Symbol(0b1110100, 7), // 35
        new Symbol(0b1011001, 7), // 36
        new Symbol(0b0101101, 7), // 37
        new Symbol(0b0000011, 7), // 38
        new Symbol(0b11101011, 8), // 39
        new Symbol(0b11101010, 8), // 40
        new Symbol(0b10110000, 8), // 41
        new Symbol(0b01011000, 8), // 42
        new Symbol(0b101100011, 9), // 43
        new Symbol(0b010110011, 9), // 44
        new Symbol(0b000001011, 9), // 45
        new Symbol(0b000001000, 9), // 46
        new Symbol(0b0101100101, 10), // 47
        new Symbol(0b0000010100, 10), // 48
        new Symbol(0b10110001010, 11), // 49
        new Symbol(0b00000101011, 11), // 50
        new Symbol(0b101100010111, 12), // 51
        new Symbol(0b010110010010, 12), // 52
        new Symbol(0b000001001100, 12), // 53
        new Symbol(0b1011000100010, 13), // 54
        new Symbol(0b0000010011010, 13), // 55
        new Symbol(0b10110001011010, 14), // 56
        new Symbol(0b10110001000010, 14), // 57
        new Symbol(0b00000101010011, 14), // 58
        new Symbol(0b00000100101100, 14), // 59
        new Symbol(0b00000100100101, 14), // 60
        new Symbol(0b101100010110010, 15), // 61
        new Symbol(0b101100010011111, 15), // 62
        new Symbol(0b101100010011011, 15), // 63
        new Symbol(0b101100010011001, 15), // 64
        new Symbol(0b101100010010110, 15), // 65
        new Symbol(0b101100010010011, 15), // 66
        new Symbol(0b101100010010100, 15), // 67
        new Symbol(0b101100010010000, 15), // 68
        new Symbol(0b101100010001110, 15), // 69
        new Symbol(0b101100010001111, 15), // 70
        new Symbol(0b101100010001100, 15), // 71
        new Symbol(0b101100010000110, 15), // 72
        new Symbol(0b101100010000011, 15), // 73
        new Symbol(0b101100010000010, 15), // 74
        new Symbol(0b101100010000000, 15), // 75
        new Symbol(0b010110010011111, 15), // 76
        new Symbol(0b010110010011101, 15), // 77
        new Symbol(0b010110010011100, 15), // 78
        new Symbol(0b010110010011011, 15), // 79
        new Symbol(0b010110010011001, 15), // 80
        new Symbol(0b010110010011000, 15), // 81
        new Symbol(0b010110010001111, 15), // 82
        new Symbol(0b010110010001101, 15), // 83
        new Symbol(0b010110010001100, 15), // 84
        new Symbol(0b010110010001011, 15), // 85
        new Symbol(0b010110010001001, 15), // 86
        new Symbol(0b010110010001000, 15), // 87
        new Symbol(0b010110010000110, 15), // 88
        new Symbol(0b010110010000101, 15), // 89
        new Symbol(0b010110010000100, 15), // 90
        new Symbol(0b010110010000010, 15), // 91
        new Symbol(0b010110010000001, 15), // 92
        new Symbol(0b010110010000000, 15), // 93
        new Symbol(0b000001010101111, 15), // 94
        new Symbol(0b000001010101101, 15), // 95
        new Symbol(0b000001010101100, 15), // 96
        new Symbol(0b000001010101011, 15), // 97
        new Symbol(0b000001010101001, 15), // 98
        new Symbol(0b000001010101000, 15), // 99
        new Symbol(0b000001010100100, 15), // 100
        new Symbol(0b000001010100011, 15), // 101
        new Symbol(0b000001010100010, 15), // 102
        new Symbol(0b000001010100001, 15), // 103
        new Symbol(0b000001001111111, 15), // 104
        new Symbol(0b000001001111110, 15), // 105
        new Symbol(0b000001001111100, 15), // 106
        new Symbol(0b000001001111011, 15), // 107
        new Symbol(0b000001001111010, 15), // 108
        new Symbol(0b000001001111000, 15), // 109
        new Symbol(0b000001001110111, 15), // 110
        new Symbol(0b000001001110110, 15), // 111
        new Symbol(0b000001001110100, 15), // 112
        new Symbol(0b000001001110011, 15), // 113
        new Symbol(0b000001001110010, 15), // 114
        new Symbol(0b000001001110000, 15), // 115
        new Symbol(0b000001001101111, 15), // 116
        new Symbol(0b000001001101110, 15), // 117
        new Symbol(0b000001001101100, 15), // 118
        new Symbol(0b000001001011111, 15), // 119
        new Symbol(0b000001001011110, 15), // 120
        new Symbol(0b000001001011100, 15), // 121
        new Symbol(0b000001001011011, 15), // 122
        new Symbol(0b000001001011010, 15), // 123
        new Symbol(0b000001001010110, 15), // 124
        new Symbol(0b000001001010101, 15), // 125
        new Symbol(0b000001001010100, 15), // 126
        new Symbol(0b000001001010011, 15), // 127
        new Symbol(0b000001001010001, 15), // 128
        new Symbol(0b000001001010000, 15), // 129
        new Symbol(0b000001001001111, 15), // 130
        new Symbol(0b000001001001101, 15), // 131
        new Symbol(0b000001001001100, 15), // 132
        new Symbol(0b000001001001001, 15), // 133
        new Symbol(0b000001001000111, 15), // 134
        new Symbol(0b000001001000110, 15), // 135
        new Symbol(0b000001001000101, 15), // 136
        new Symbol(0b000001001000011, 15), // 137
        new Symbol(0b000001001000010, 15), // 138
        new Symbol(0b000001001000001, 15), // 139
        new Symbol(0b1011000101101111, 16), // 140
        new Symbol(0b1011000101101110, 16), // 141
        new Symbol(0b1011000101101101, 16), // 142
        new Symbol(0b1011000101100111, 16), // 143
        new Symbol(0b1011000101100110, 16), // 144
        new Symbol(0b1011000101100011, 16), // 145
        new Symbol(0b1011000101100001, 16), // 146
        new Symbol(0b1011000101100000, 16), // 147
        new Symbol(0b1011000100111101, 16), // 148
        new Symbol(0b1011000100111011, 16), // 149
        new Symbol(0b1011000100111010, 16), // 150
        new Symbol(0b1011000100111001, 16), // 151
        new Symbol(0b1011000100110101, 16), // 152
        new Symbol(0b1011000100110100, 16), // 153
        new Symbol(0b1011000100110001, 16), // 154
        new Symbol(0b1011000100101111, 16), // 155
        new Symbol(0b1011000100101110, 16), // 156
        new Symbol(0b1011000100101011, 16), // 157
        new Symbol(0b1011000100100101, 16), // 158
        new Symbol(0b1011000100100100, 16), // 159
        new Symbol(0b1011000100100011, 16), // 160
        new Symbol(0b1011000100011011, 16), // 161
        new Symbol(0b1011000100011010, 16), // 162
        new Symbol(0b1011000100001111, 16), // 163
        new Symbol(0b1011000100000011, 16), // 164
        new Symbol(0b1011000100000010, 16), // 165
        new Symbol(0b0101100100111101, 16), // 166
        new Symbol(0b0101100100110101, 16), // 167
        new Symbol(0b0101100100110100, 16), // 168
        new Symbol(0b0101100100011101, 16), // 169
        new Symbol(0b0101100100010101, 16), // 170
        new Symbol(0b0101100100010100, 16), // 171
        new Symbol(0b0101100100001111, 16), // 172
        new Symbol(0b0101100100000111, 16), // 173
        new Symbol(0b0101100100000110, 16), // 174
        new Symbol(0b0000010101011101, 16), // 175
        new Symbol(0b0000010101010101, 16), // 176
        new Symbol(0b0000010101010100, 16), // 177
        new Symbol(0b0000010101001011, 16), // 178
        new Symbol(0b0000010101000001, 16), // 179
        new Symbol(0b0000010101000000, 16), // 180
        new Symbol(0b0000010011111011, 16), // 181
        new Symbol(0b0000010011110011, 16), // 182
        new Symbol(0b0000010011110010, 16), // 183
        new Symbol(0b0000010011101011, 16), // 184
        new Symbol(0b0000010011100011, 16), // 185
        new Symbol(0b0000010011100010, 16), // 186
        new Symbol(0b0000010011011011, 16), // 187
        new Symbol(0b0000010010111011, 16), // 188
        new Symbol(0b0000010010111010, 16), // 189
        new Symbol(0b0000010010101111, 16), // 190
        new Symbol(0b0000010010100101, 16), // 191
        new Symbol(0b0000010010100100, 16), // 192
        new Symbol(0b0000010010011101, 16), // 193
        new Symbol(0b0000010010010001, 16), // 194
        new Symbol(0b0000010010010000, 16), // 195
        new Symbol(0b0000010010001001, 16), // 196
        new Symbol(0b0000010010000001, 16), // 197
        new Symbol(0b0000010010000000, 16), // 198
        new Symbol(0b10110001011011001, 17), // 199
        new Symbol(0b10110001011000101, 17), // 200
        new Symbol(0b10110001011000100, 17), // 201
        new Symbol(0b10110001001111001, 17), // 202
        new Symbol(0b10110001001110001, 17), // 203
        new Symbol(0b10110001001110000, 17), // 204
        new Symbol(0b10110001001100001, 17), // 205
        new Symbol(0b10110001001010101, 17), // 206
        new Symbol(0b10110001001010100, 17), // 207
        new Symbol(0b10110001001000101, 17), // 208
        new Symbol(0b10110001000011101, 17), // 209
        new Symbol(0b10110001000011100, 17), // 210
        new Symbol(0b01011001001111001, 17), // 211
        new Symbol(0b01011001000111001, 17), // 212
        new Symbol(0b01011001000111000, 17), // 213
        new Symbol(0b01011001000011101, 17), // 214
        new Symbol(0b00000101010111001, 17), // 215
        new Symbol(0b00000101010111000, 17), // 216
        new Symbol(0b00000101010010101, 17), // 217
        new Symbol(0b00000100111110101, 17), // 218
        new Symbol(0b00000100111110100, 17), // 219
        new Symbol(0b00000100111010101, 17), // 220
        new Symbol(0b00000100110110101, 17), // 221
        new Symbol(0b00000100110110100, 17), // 222
        new Symbol(0b00000100101011101, 17), // 223
        new Symbol(0b00000100100111001, 17), // 224
        new Symbol(0b00000100100111000, 17), // 225
        new Symbol(0b00000100100010001, 17), // 226
        new Symbol(0b101100010110110001, 18), // 227
        new Symbol(0b101100010110110000, 18), // 228
        new Symbol(0b101100010011110001, 18), // 229
        new Symbol(0b101100010011000001, 18), // 230
        new Symbol(0b101100010011000000, 18), // 231
        new Symbol(0b101100010010001001, 18), // 232
        new Symbol(0b010110010011110001, 18), // 233
        new Symbol(0b010110010011110000, 18), // 234
        new Symbol(0b010110010000111001, 18), // 235
        new Symbol(0b000001010100101001, 18), // 236
        new Symbol(0b000001010100101000, 18), // 237
        new Symbol(0b000001001110101001, 18), // 238
        new Symbol(0b000001001010111001, 18), // 239
        new Symbol(0b000001001010111000, 18), // 240
        new Symbol(0b000001001000100001, 18), // 241
        new Symbol(0b1011000100111100001, 19), // 242
        new Symbol(0b1011000100111100000, 19), // 243
        new Symbol(0b1011000100100010001, 19), // 244
        new Symbol(0b0101100100001110001, 19), // 245
        new Symbol(0b0101100100001110000, 19), // 246
        new Symbol(0b0000010011101010000, 19), // 247
        new Symbol(0b0000010010001000001, 19), // 248
        new Symbol(0b0000010010001000000, 19), // 249
        new Symbol(0b10110001001000100000, 20), // 250
        new Symbol(0b00000100111010100011, 20), // 251
        new Symbol(0b00000100111010100010, 20), // 252
        new Symbol(0b101100010010001000010, 21), // 253
        new Symbol(0b1011000100100010000111, 22), // 254
        new Symbol(0b1011000100100010000110, 22), // 255
    };

    private static final Node ROOT = buildTree(0, 0);
}
