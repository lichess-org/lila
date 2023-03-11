// *************************** this file is generated. see ui/input/@build/README.md ***************************

export type Sub = { to: string; cost: number };

export type Tag = 'file' | 'rank' | 'role' | 'move' | 'choice' | 'command' | 'ignore' | 'exact' | 'rounds';

export type Entry = { in: string; tok: string; tags: Tag[]; out?: string; subs?: Sub[] };

export const lexicon: Entry[] = [
  {
    in: 'a',
    tok: 'a',
    tags: ['file', 'move'],
    subs: [
      {
        to: 'e',
        cost: 0.4404761904761905,
      },
      {
        to: 'f',
        cost: 0.44761904761904764,
      },
      {
        to: '8',
        cost: 0.4547619047619048,
      },
      {
        to: 'a8',
        cost: 0.4928571428571429,
      },
      {
        to: '',
        cost: 0.3142857142857143,
      },
      {
        to: 'h',
        cost: 0.5166666666666667,
      },
      {
        to: '8a',
        cost: 0.6309523809523809,
      },
      {
        to: '8e',
        cost: 0.7642857142857142,
      },
      {
        to: 'b',
        cost: 0.7857142857142858,
      },
    ],
  },
  {
    in: 'b',
    tok: 'b',
    tags: ['file', 'move'],
    subs: [
      {
        to: 'd',
        cost: 0.45952380952380956,
      },
      {
        to: 'e',
        cost: 0.5238095238095238,
      },
      {
        to: 'b8',
        cost: 0.580952380952381,
      },
      {
        to: '8b',
        cost: 0.7523809523809524,
      },
      {
        to: 'a',
        cost: 0.7785714285714286,
      },
      {
        to: 'Q',
        cost: 0.8428571428571429,
      },
      {
        to: 'Pb',
        cost: 0.8761904761904762,
      },
    ],
  },
  {
    in: 'c',
    tok: 'c',
    tags: ['file', 'move'],
    subs: [
      {
        to: '3',
        cost: 0.6047619047619048,
      },
      {
        to: '6',
        cost: 0.6214285714285714,
      },
      {
        to: 'g',
        cost: 0.6238095238095238,
      },
      {
        to: 'c8',
        cost: 0.6333333333333333,
      },
      {
        to: '',
        cost: 0.5404761904761906,
      },
      {
        to: 'e',
        cost: 0.8738095238095238,
      },
    ],
  },
  {
    in: 'd',
    tok: 'd',
    tags: ['file', 'move'],
    subs: [
      {
        to: 'g',
        cost: 0.4666666666666667,
      },
      {
        to: 'b',
        cost: 0.47380952380952385,
      },
      {
        to: 'e',
        cost: 0.5333333333333333,
      },
      {
        to: 'd8',
        cost: 0.6071428571428572,
      },
      {
        to: '3',
        cost: 0.6166666666666667,
      },
      {
        to: '8d',
        cost: 0.7,
      },
      {
        to: 'a',
        cost: 0.7047619047619048,
      },
      {
        to: 'c',
        cost: 0.7333333333333334,
      },
      {
        to: '',
        cost: 0.6047619047619048,
      },
    ],
  },
  {
    in: 'e',
    tok: 'e',
    tags: ['file', 'move'],
    subs: [
      {
        to: 'a',
        cost: 0.4571428571428572,
      },
      {
        to: 'b',
        cost: 0.4785714285714286,
      },
      {
        to: 'd',
        cost: 0.48095238095238096,
      },
      {
        to: 'g',
        cost: 0.4904761904761905,
      },
      {
        to: 'c',
        cost: 0.5023809523809524,
      },
      {
        to: '',
        cost: 0.3738095238095238,
      },
      {
        to: 'e8',
        cost: 0.5976190476190476,
      },
      {
        to: 'h',
        cost: 0.8285714285714285,
      },
      {
        to: 'f',
        cost: 0.8595238095238096,
      },
      {
        to: '8',
        cost: 0.8904761904761904,
      },
      {
        to: 'e1',
        cost: 0.8928571428571429,
      },
    ],
  },
  {
    in: 'f',
    tok: 'f',
    tags: ['file', 'move'],
    subs: [
      {
        to: 'a',
        cost: 0.5785714285714286,
      },
      {
        to: 'h',
        cost: 0.6023809523809525,
      },
      {
        to: 'e',
        cost: 0.6880952380952381,
      },
      {
        to: '8f',
        cost: 0.7095238095238096,
      },
      {
        to: '',
        cost: 0.5833333333333334,
      },
      {
        to: 'f1',
        cost: 0.8404761904761905,
      },
      {
        to: 'f2',
        cost: 0.8666666666666667,
      },
      {
        to: 'f8',
        cost: 0.8690476190476191,
      },
    ],
  },
  {
    in: 'g',
    tok: 'g',
    tags: ['file', 'move'],
    subs: [
      {
        to: 'd',
        cost: 0.4761904761904762,
      },
      {
        to: 'g8',
        cost: 0.511904761904762,
      },
      {
        to: 'c',
        cost: 0.6904761904761905,
      },
      {
        to: '8g',
        cost: 0.7023809523809523,
      },
      {
        to: '',
        cost: 0.5571428571428572,
      },
      {
        to: '3',
        cost: 0.8166666666666667,
      },
      {
        to: 'e',
        cost: 0.819047619047619,
      },
      {
        to: 'b',
        cost: 0.8809523809523809,
      },
      {
        to: 'g2',
        cost: 0.8833333333333333,
      },
    ],
  },
  {
    in: 'h',
    tok: 'h',
    tags: ['file', 'move'],
    subs: [
      {
        to: '8',
        cost: 0.48333333333333334,
      },
      {
        to: 'a',
        cost: 0.5571428571428572,
      },
      {
        to: '8h',
        cost: 0.6642857142857144,
      },
      {
        to: 'e',
        cost: 0.719047619047619,
      },
      {
        to: '',
        cost: 0.5761904761904761,
      },
    ],
  },
  {
    in: '1',
    tok: '1',
    tags: [],
    subs: [
      {
        to: 'P',
        cost: 0.530952380952381,
      },
      {
        to: 'e1',
        cost: 0.5380952380952382,
      },
      {
        to: 'f1',
        cost: 0.6666666666666667,
      },
      {
        to: '',
        cost: 0.49523809523809526,
      },
      {
        to: '1a',
        cost: 0.7904761904761906,
      },
      {
        to: '1f',
        cost: 0.7928571428571429,
      },
      {
        to: 'a1',
        cost: 0.7952380952380953,
      },
      {
        to: '1e',
        cost: 0.7976190476190477,
      },
      {
        to: 'g1',
        cost: 0.8214285714285714,
      },
      {
        to: 'b1',
        cost: 0.8547619047619048,
      },
      {
        to: 'd1',
        cost: 0.8857142857142857,
      },
      {
        to: 'c1',
        cost: 0.888095238095238,
      },
    ],
  },
  {
    in: 'one',
    tok: '1',
    tags: ['rank', 'move'],
  },
  {
    in: '2',
    tok: '2',
    tags: [],
    subs: [
      {
        to: '',
        cost: 0.29523809523809524,
      },
      {
        to: 'g',
        cost: 0.55,
      },
      {
        to: 'e2',
        cost: 0.5547619047619048,
      },
      {
        to: '3',
        cost: 0.5880952380952381,
      },
      {
        to: 'd',
        cost: 0.7119047619047619,
      },
      {
        to: 'f2',
        cost: 0.7214285714285715,
      },
      {
        to: '2a',
        cost: 0.8071428571428572,
      },
      {
        to: '2e',
        cost: 0.8095238095238095,
      },
      {
        to: 'd2',
        cost: 0.830952380952381,
      },
      {
        to: 'g2',
        cost: 0.8333333333333334,
      },
      {
        to: 'a2',
        cost: 0.8357142857142857,
      },
      {
        to: '2f',
        cost: 0.861904761904762,
      },
    ],
  },
  {
    in: 'two',
    tok: '2',
    tags: ['rank', 'move'],
  },
  {
    in: '3',
    tok: '3',
    tags: [],
    subs: [
      {
        to: '3e',
        cost: 0.48571428571428577,
      },
      {
        to: 'e3',
        cost: 0.6000000000000001,
      },
      {
        to: '3a',
        cost: 0.6452380952380953,
      },
      {
        to: 'f3',
        cost: 0.65,
      },
      {
        to: '3f',
        cost: 0.7452380952380953,
      },
      {
        to: 'g',
        cost: 0.75,
      },
      {
        to: 'a3',
        cost: 0.8119047619047619,
      },
      {
        to: '3d',
        cost: 0.8142857142857143,
      },
    ],
  },
  {
    in: 'three',
    tok: '3',
    tags: ['rank', 'move'],
  },
  {
    in: '4',
    tok: '4',
    tags: [],
    subs: [
      {
        to: 'f4',
        cost: 0.5357142857142857,
      },
      {
        to: 'e4',
        cost: 0.6095238095238096,
      },
      {
        to: '4f',
        cost: 0.6714285714285715,
      },
      {
        to: '4a',
        cost: 0.7261904761904763,
      },
      {
        to: '4e',
        cost: 0.7428571428571429,
      },
      {
        to: '5',
        cost: 0.7666666666666666,
      },
      {
        to: 'd4',
        cost: 0.8023809523809524,
      },
      {
        to: 'a4',
        cost: 0.8500000000000001,
      },
      {
        to: 'P',
        cost: 0.8523809523809525,
      },
    ],
  },
  {
    in: 'four',
    tok: '4',
    tags: ['rank', 'move'],
  },
  {
    in: '5',
    tok: '5',
    tags: [],
    subs: [
      {
        to: 'f5',
        cost: 0.5214285714285715,
      },
      {
        to: '5f',
        cost: 0.5452380952380953,
      },
      {
        to: 'e5',
        cost: 0.611904761904762,
      },
      {
        to: '5a',
        cost: 0.6857142857142857,
      },
      {
        to: 'a5',
        cost: 0.7595238095238095,
      },
      {
        to: '5e',
        cost: 0.7880952380952382,
      },
      {
        to: '4',
        cost: 0.8380952380952381,
      },
      {
        to: 'c5',
        cost: 0.8642857142857143,
      },
    ],
  },
  {
    in: 'five',
    tok: '5',
    tags: ['rank', 'move'],
  },
  {
    in: '6',
    tok: '6',
    tags: [],
    subs: [
      {
        to: 'f6',
        cost: 0.5857142857142857,
      },
      {
        to: 'e6',
        cost: 0.6476190476190476,
      },
      {
        to: '6a',
        cost: 0.6690476190476191,
      },
      {
        to: 'h6',
        cost: 0.6785714285714286,
      },
      {
        to: 'c6',
        cost: 0.7380952380952381,
      },
      {
        to: 'a6',
        cost: 0.7619047619047619,
      },
      {
        to: 'g6',
        cost: 0.8714285714285714,
      },
    ],
  },
  {
    in: 'six',
    tok: '6',
    tags: ['rank', 'move'],
  },
  {
    in: '7',
    tok: '7',
    tags: [],
    subs: [
      {
        to: 'h7',
        cost: 0.6523809523809524,
      },
      {
        to: 'e7',
        cost: 0.6833333333333333,
      },
      {
        to: '7f',
        cost: 0.6976190476190476,
      },
      {
        to: '7a',
        cost: 0.7142857142857143,
      },
      {
        to: 'f7',
        cost: 0.7166666666666667,
      },
      {
        to: 'd7',
        cost: 0.8,
      },
      {
        to: '6',
        cost: 0.8238095238095238,
      },
      {
        to: 'b7',
        cost: 0.8261904761904761,
      },
      {
        to: '7e',
        cost: 0.8571428571428572,
      },
      {
        to: 'a7',
        cost: 0.8952380952380953,
      },
      {
        to: '7-',
        cost: 0.8976190476190476,
      },
    ],
  },
  {
    in: 'seven',
    tok: '7',
    tags: ['rank', 'move'],
  },
  {
    in: '8',
    tok: '8',
    tags: [],
    subs: [
      {
        to: 'a',
        cost: 0.4285714285714286,
      },
      {
        to: 'a8',
        cost: 0.43333333333333335,
      },
      {
        to: 'h',
        cost: 0.4380952380952381,
      },
      {
        to: 'e',
        cost: 0.5071428571428571,
      },
      {
        to: '8a',
        cost: 0.5619047619047619,
      },
      {
        to: 'e8',
        cost: 0.6190476190476191,
      },
      {
        to: '',
        cost: 0.4761904761904762,
      },
      {
        to: 'a2',
        cost: 0.7547619047619047,
      },
      {
        to: 'f8',
        cost: 0.780952380952381,
      },
      {
        to: 'f',
        cost: 0.8452380952380952,
      },
      {
        to: 'h8',
        cost: 0.8476190476190477,
      },
      {
        to: '8e',
        cost: 0.8785714285714286,
      },
    ],
  },
  {
    in: 'eight',
    tok: '8',
    tags: ['rank', 'move'],
  },
  {
    in: 'pawn',
    tok: 'P',
    tags: ['role', 'move'],
    subs: [
      {
        to: 'Pa',
        cost: 0.5190476190476191,
      },
      {
        to: 'Pe',
        cost: 0.6404761904761905,
      },
      {
        to: 'Pf',
        cost: 0.6428571428571429,
      },
      {
        to: '1',
        cost: 0.7285714285714286,
      },
      {
        to: '4',
        cost: 0.730952380952381,
      },
    ],
  },
  {
    in: 'knight',
    tok: 'N',
    tags: ['role', 'move'],
    subs: [
      {
        to: 'K',
        cost: 0.5095238095238095,
      },
      {
        to: '',
        cost: 0.3595238095238095,
      },
      {
        to: 'Na',
        cost: 0.5833333333333334,
      },
      {
        to: 'Nd',
        cost: 0.680952380952381,
      },
      {
        to: 'Nf',
        cost: 0.7071428571428572,
      },
    ],
  },
  {
    in: 'bishop',
    tok: 'B',
    tags: ['role', 'move'],
    subs: [
      {
        to: '',
        cost: 0.42619047619047623,
      },
      {
        to: 'Bf',
        cost: 0.6285714285714286,
      },
      {
        to: 'Ba',
        cost: 0.6547619047619048,
      },
      {
        to: 'Bg',
        cost: 0.6928571428571428,
      },
      {
        to: 'Be',
        cost: 0.7357142857142858,
      },
    ],
  },
  {
    in: 'rook',
    tok: 'R',
    tags: ['role', 'move'],
    subs: [
      {
        to: 'Ra',
        cost: 0.5404761904761906,
      },
      {
        to: '',
        cost: 0.3761904761904762,
      },
      {
        to: 'Rf',
        cost: 0.7476190476190476,
      },
    ],
  },
  {
    in: 'queen',
    tok: 'Q',
    tags: ['role', 'move'],
    subs: [
      {
        to: 'Qe',
        cost: 0.5666666666666667,
      },
      {
        to: '8Q',
        cost: 0.569047619047619,
      },
      {
        to: 'Qa',
        cost: 0.7238095238095239,
      },
      {
        to: 'Qd',
        cost: 0.7690476190476191,
      },
      {
        to: 'Qf',
        cost: 0.7714285714285715,
      },
      {
        to: '1Q',
        cost: 0.7738095238095238,
      },
    ],
  },
  {
    in: 'king',
    tok: 'K',
    tags: ['role', 'move'],
    subs: [
      {
        to: '',
        cost: 0.2976190476190476,
      },
      {
        to: 'Ka',
        cost: 0.5,
      },
      {
        to: 'Kf',
        cost: 0.5261904761904762,
      },
      {
        to: 'Ke',
        cost: 0.5285714285714286,
      },
      {
        to: 'N',
        cost: 0.5714285714285714,
      },
      {
        to: 'Q',
        cost: 0.6738095238095239,
      },
    ],
  },
  {
    in: 'castle',
    out: 'O-O',
    tags: ['move', 'exact'],
    tok: '!',
  },
  {
    in: 'short castle',
    out: 'O-O',
    tags: ['move', 'exact'],
    tok: '"',
  },
  {
    in: 'king side castle',
    out: 'O-O',
    tags: ['move', 'exact'],
    tok: '#',
  },
  {
    in: 'castle king side',
    out: 'O-O',
    tags: ['move', 'exact'],
    tok: '$',
  },
  {
    in: 'long castle',
    out: 'O-O-O',
    tags: ['move', 'exact'],
    tok: '%',
  },
  {
    in: 'castle queen side',
    out: 'O-O-O',
    tags: ['move', 'exact'],
    tok: '&',
  },
  {
    in: 'queen side castle',
    out: 'O-O-O',
    tags: ['move', 'exact'],
    tok: "'",
  },
  {
    in: 'takes',
    out: 'x',
    tags: ['move'],
    tok: '(',
    subs: [
      {
        to: '6',
        cost: 0.4714285714285714,
      },
      {
        to: 'h',
        cost: 0.5523809523809524,
      },
      {
        to: 'P(',
        cost: 0.6142857142857143,
      },
      {
        to: 'a',
        cost: 0.6571428571428571,
      },
      {
        to: '(N',
        cost: 0.6595238095238096,
      },
      {
        to: 'R(',
        cost: 0.661904761904762,
      },
    ],
  },
  {
    in: 'captures',
    out: 'x',
    tags: ['move'],
    tok: ')',
    subs: [
      {
        to: ')R',
        cost: 0.5047619047619047,
      },
      {
        to: 'P)',
        cost: 0.5642857142857143,
      },
      {
        to: ')Q',
        cost: 0.5928571428571429,
      },
      {
        to: 'B)',
        cost: 0.5952380952380952,
      },
      {
        to: 'R)',
        cost: 0.6357142857142857,
      },
      {
        to: 'N)',
        cost: 0.638095238095238,
      },
    ],
  },
  {
    in: 'promote',
    out: '=',
    tags: ['move'],
    tok: '*',
    subs: [
      {
        to: '8*',
        cost: 0.46904761904761905,
      },
    ],
  },
  {
    in: 'promotes',
    out: '=',
    tags: ['move'],
    tok: '+',
  },
  {
    in: 'mate',
    out: '',
    tags: ['move'],
    tok: ',',
    subs: [
      {
        to: 'N',
        cost: 0.430952380952381,
      },
      {
        to: 'b',
        cost: 0.48809523809523814,
      },
      {
        to: '',
        cost: 0.3476190476190476,
      },
      {
        to: 'd',
        cost: 0.5904761904761905,
      },
    ],
  },
  {
    in: 'check',
    out: '',
    tags: ['move'],
    tok: '-',
    subs: [
      {
        to: '',
        cost: 0.34285714285714286,
      },
    ],
  },
  {
    in: 'takeback',
    out: 'takeback',
    tags: ['command', 'rounds', 'exact'],
    tok: '.',
  },
  {
    in: 'draw',
    out: 'draw',
    tags: ['command', 'rounds', 'exact'],
    tok: '/',
    subs: [
      {
        to: '2',
        cost: 0.42142857142857143,
      },
    ],
  },
  {
    in: 'offer draw',
    out: 'draw',
    tags: ['command', 'rounds', 'exact'],
    tok: '0',
  },
  {
    in: 'accept draw',
    out: 'draw',
    tags: ['command', 'rounds', 'exact'],
    tok: '9',
  },
  {
    in: 'resign',
    out: 'resign',
    tags: ['command', 'rounds', 'exact'],
    tok: ':',
    subs: [
      {
        to: 'g7',
        cost: 0.42380952380952386,
      },
    ],
  },
  {
    in: 'next',
    out: 'next',
    tags: ['command', 'exact'],
    tok: ';',
    subs: [
      {
        to: '',
        cost: 0.20952380952380953,
      },
      {
        to: '(',
        cost: 0.41190476190476194,
      },
    ],
  },
  {
    in: 'back',
    out: 'back',
    tags: ['command', 'exact'],
    tok: '<',
  },
  {
    in: 'up vote',
    out: 'upv',
    tags: ['command', 'exact'],
    tok: '=',
  },
  {
    in: 'down vote',
    out: 'downv',
    tags: ['command', 'exact'],
    tok: '>',
  },
  {
    in: 'help',
    out: '?',
    tags: ['command', 'exact'],
    tok: '?',
  },
  {
    in: 'clock',
    out: 'clock',
    tags: ['command', 'exact'],
    tok: '@',
  },
  {
    in: 'opponent',
    out: 'who',
    tags: ['command', 'exact'],
    tok: 'A',
  },
  {
    in: 'stop',
    out: 'stop',
    tags: ['command', 'exact'],
    tok: 'C',
  },
  {
    in: 'red',
    out: 'red',
    tags: ['choice', 'exact'],
    tok: 'D',
    subs: [
      {
        to: '8',
        cost: 0.42619047619047623,
      },
      {
        to: 'R',
        cost: 0.4428571428571429,
      },
      {
        to: 'N',
        cost: 0.44523809523809527,
      },
    ],
  },
  {
    in: 'yellow',
    out: 'yellow',
    tags: ['choice', 'exact'],
    tok: 'E',
  },
  {
    in: 'green',
    out: 'green',
    tags: ['choice', 'exact'],
    tok: 'F',
    subs: [
      {
        to: 'Q',
        cost: 0.4,
      },
      {
        to: '3',
        cost: 0.4142857142857143,
      },
    ],
  },
  {
    in: 'blue',
    out: 'blue',
    tags: ['choice', 'exact'],
    tok: 'G',
    subs: [
      {
        to: '2',
        cost: 0.4023809523809524,
      },
      {
        to: 'b',
        cost: 0.41904761904761906,
      },
      {
        to: '3',
        cost: 0.45,
      },
      {
        to: 'd',
        cost: 0.4523809523809524,
      },
    ],
  },
  {
    in: 'yes',
    out: 'yes',
    tags: ['choice', 'exact'],
    tok: 'H',
    subs: [
      {
        to: 'e',
        cost: 0.40714285714285714,
      },
      {
        to: '',
        cost: 0.21666666666666667,
      },
      {
        to: 'e8',
        cost: 0.4357142857142857,
      },
      {
        to: 'f',
        cost: 0.46190476190476193,
      },
      {
        to: 'h',
        cost: 0.4642857142857143,
      },
    ],
  },
  {
    in: 'okay',
    out: 'yes',
    tags: ['choice', 'exact'],
    tok: 'I',
  },
  {
    in: 'confirm',
    out: 'yes',
    tags: ['choice', 'exact'],
    tok: 'J',
  },
  {
    in: 'no',
    out: 'no',
    tags: ['choice', 'exact'],
    tok: 'L',
    subs: [
      {
        to: 'N',
        cost: 0.40476190476190477,
      },
    ],
  },
  {
    in: 'clear',
    out: 'no',
    tags: ['choice', 'exact'],
    tok: 'M',
  },
  {
    in: 'close',
    out: 'no',
    tags: ['choice', 'exact'],
    tok: 'O',
  },
  {
    in: 'cancel',
    out: 'no',
    tags: ['choice', 'exact'],
    tok: 'S',
  },
  {
    in: 'abort',
    out: 'no',
    tags: ['choice', 'exact'],
    tok: 'T',
  },
  {
    in: 'puzzle',
    out: '',
    tags: ['ignore'],
    tok: 'U',
  },
  {
    in: 'and',
    out: '',
    tags: ['ignore'],
    tok: 'V',
  },
  {
    in: 'oh',
    out: '',
    tags: ['ignore'],
    tok: 'W',
  },
  {
    in: 'ah',
    out: '',
    tags: ['ignore'],
    tok: 'X',
  },
  {
    in: 'um',
    out: '',
    tags: ['ignore'],
    tok: 'Y',
  },
  {
    in: 'uh',
    out: '',
    tags: ['ignore'],
    tok: 'Z',
  },
  {
    in: 'hmm',
    out: '',
    tags: ['ignore'],
    tok: '[',
  },
  {
    in: 'huh',
    out: '',
    tags: ['ignore'],
    tok: '\\',
  },
  {
    in: 'ha',
    out: '',
    tags: ['ignore'],
    tok: ']',
  },
  {
    in: 'his',
    out: '',
    tags: ['ignore'],
    tok: '^',
  },
  {
    in: 'her',
    out: '',
    tags: ['ignore'],
    tok: '_',
  },
  {
    in: 'the',
    out: '',
    tags: ['ignore'],
    tok: '`',
  },
  {
    in: 'their',
    out: '',
    tags: ['ignore'],
    tok: 'i',
  },
];
