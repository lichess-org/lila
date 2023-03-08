// *************************** this file is generated. see ui/input/@build/README.md ***************************

export type Sub = { to: string; cost: number };

export type Tag = 'file' | 'rank' | 'role' | 'move' | 'choice' | 'command' | 'ignore' | 'exact' | 'rounds';

export type Token = { in: string; tok: string; tags: Tag[]; out?: string; subs?: Sub[] };

export const lexicon: Token[] = [
  {
    in: 'a',
    tok: 'a',
    tags: ['file', 'move'],
    subs: [
      {
        to: 'e',
        cost: 0.4722222222222222,
      },
      {
        to: 'f',
        cost: 0.4777777777777778,
      },
      {
        to: '8',
        cost: 0.48333333333333334,
      },
      {
        to: 'a8',
        cost: 0.5611111111111111,
      },
      {
        to: '',
        cost: 0.4111111111111111,
      },
      {
        to: 'h',
        cost: 0.6166666666666667,
      },
      {
        to: '8a',
        cost: 0.8500000000000001,
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
        cost: 0.49444444444444446,
      },
      {
        to: 'e',
        cost: 0.6333333333333333,
      },
      {
        to: 'b8',
        cost: 0.7555555555555555,
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
        cost: 0.7944444444444445,
      },
      {
        to: '6',
        cost: 0.8277777777777777,
      },
      {
        to: 'g',
        cost: 0.8333333333333334,
      },
      {
        to: 'c8',
        cost: 0.8555555555555556,
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
        cost: 0.5,
      },
      {
        to: 'b',
        cost: 0.5166666666666667,
      },
      {
        to: 'e',
        cost: 0.6555555555555556,
      },
      {
        to: 'd8',
        cost: 0.8,
      },
      {
        to: '3',
        cost: 0.8166666666666667,
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
        cost: 0.48888888888888893,
      },
      {
        to: 'b',
        cost: 0.5277777777777778,
      },
      {
        to: 'd',
        cost: 0.5333333333333333,
      },
      {
        to: 'g',
        cost: 0.5555555555555556,
      },
      {
        to: 'c',
        cost: 0.5833333333333334,
      },
      {
        to: '',
        cost: 0.538888888888889,
      },
      {
        to: 'e8',
        cost: 0.7777777777777778,
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
        cost: 0.75,
      },
      {
        to: 'h',
        cost: 0.788888888888889,
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
        cost: 0.5222222222222223,
      },
      {
        to: 'g8',
        cost: 0.6055555555555556,
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
        cost: 0.538888888888889,
      },
      {
        to: 'a',
        cost: 0.7,
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
        cost: 0.65,
      },
      {
        to: 'e1',
        cost: 0.6666666666666667,
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
        cost: 0.3666666666666667,
      },
      {
        to: 'g',
        cost: 0.6833333333333333,
      },
      {
        to: 'e2',
        cost: 0.6944444444444444,
      },
      {
        to: '3',
        cost: 0.7722222222222223,
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
        cost: 0.5444444444444445,
      },
      {
        to: 'e3',
        cost: 0.7833333333333334,
      },
      {
        to: '3a',
        cost: 0.8722222222222222,
      },
      {
        to: 'f3',
        cost: 0.8833333333333333,
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
        cost: 0.6611111111111112,
      },
      {
        to: 'e4',
        cost: 0.8055555555555556,
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
        cost: 0.6277777777777778,
      },
      {
        to: '5f',
        cost: 0.6777777777777778,
      },
      {
        to: 'e5',
        cost: 0.8111111111111111,
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
        cost: 0.7666666666666666,
      },
      {
        to: 'e6',
        cost: 0.8777777777777778,
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
        cost: 0.8888888888888888,
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
        cost: 0.4444444444444445,
      },
      {
        to: 'a8',
        cost: 0.4555555555555556,
      },
      {
        to: 'h',
        cost: 0.4666666666666667,
      },
      {
        to: 'e',
        cost: 0.5944444444444444,
      },
      {
        to: '8a',
        cost: 0.7111111111111111,
      },
      {
        to: 'e8',
        cost: 0.8222222222222222,
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
        to: '',
        cost: 0,
      },
      {
        to: 'Pa',
        cost: 0.6222222222222222,
      },
      {
        to: 'Pe',
        cost: 0.8611111111111112,
      },
      {
        to: 'Pf',
        cost: 0.8666666666666667,
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
        cost: 0.6000000000000001,
      },
      {
        to: '',
        cost: 0.5055555555555555,
      },
      {
        to: 'Na',
        cost: 0.7611111111111111,
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
        cost: 0.6388888888888888,
      },
      {
        to: 'Bf',
        cost: 0.8444444444444444,
      },
      {
        to: 'Ba',
        cost: 0.8944444444444445,
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
        cost: 0.6722222222222223,
      },
      {
        to: '',
        cost: 0.5444444444444445,
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
        cost: 0.7222222222222223,
      },
      {
        to: '8Q',
        cost: 0.7277777777777779,
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
        cost: 0.37222222222222223,
      },
      {
        to: 'Ka',
        cost: 0.5777777777777778,
      },
      {
        to: 'Kf',
        cost: 0.638888888888889,
      },
      {
        to: 'Ke',
        cost: 0.6444444444444445,
      },
      {
        to: 'N',
        cost: 0.7333333333333334,
      },
    ],
  },
  {
    in: 'takes',
    out: 'x',
    tags: ['move'],
    tok: '!',
    subs: [
      {
        to: '6',
        cost: 0.5111111111111111,
      },
      {
        to: 'h',
        cost: 0.6888888888888889,
      },
    ],
  },
  {
    in: 'captures',
    out: 'x',
    tags: ['move'],
    tok: '"',
    subs: [
      {
        to: '"R',
        cost: 0.5888888888888889,
      },
      {
        to: 'P"',
        cost: 0.7166666666666667,
      },
    ],
  },
  {
    in: 'castle',
    out: 'O-O',
    tags: ['move', 'exact'],
    tok: '#',
  },
  {
    in: 'short castle',
    out: 'O-O',
    tags: ['move', 'exact'],
    tok: '$',
  },
  {
    in: 'king-side castle',
    out: 'O-O',
    tags: ['move', 'exact'],
    tok: '%',
  },
  {
    in: 'castle king-side',
    out: 'O-O',
    tags: ['move', 'exact'],
    tok: '&',
  },
  {
    in: 'long castle',
    out: 'O-O-O',
    tags: ['move', 'exact'],
    tok: "'",
  },
  {
    in: 'castle queen-side',
    out: 'O-O-O',
    tags: ['move', 'exact'],
    tok: '(',
  },
  {
    in: 'queen-side castle',
    out: 'O-O-O',
    tags: ['move', 'exact'],
    tok: ')',
  },
  {
    in: 'promote',
    out: '=',
    tags: ['move'],
    tok: '*',
    subs: [
      {
        to: '8*',
        cost: 0.5055555555555555,
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
        cost: 0.45,
      },
      {
        to: 'b',
        cost: 0.55,
      },
    ],
  },
  {
    in: 'check',
    out: '',
    tags: ['move'],
    tok: '-',
  },
  {
    in: 'takeback',
    out: 'takeback',
    tags: ['move', 'rounds', 'exact'],
    tok: '.',
  },
  {
    in: 'draw',
    out: 'draw',
    tags: ['move', 'rounds', 'exact'],
    tok: '/',
  },
  {
    in: 'offer draw',
    out: 'draw',
    tags: ['move', 'rounds', 'exact'],
    tok: '0',
  },
  {
    in: 'accept draw',
    out: 'draw',
    tags: ['move', 'rounds', 'exact'],
    tok: '9',
  },
  {
    in: 'resign',
    out: 'resign',
    tags: ['move', 'rounds', 'exact'],
    tok: ':',
  },
  {
    in: 'next',
    out: 'next',
    tags: ['command', 'exact'],
    tok: ';',
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
        cost: 0.4388888888888889,
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
        cost: 0.4222222222222222,
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
        cost: 0.40555555555555556,
      },
      {
        to: 'b',
        cost: 0.43333333333333335,
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
        cost: 0.4166666666666667,
      },
      {
        to: '',
        cost: 0.2277777777777778,
      },
      {
        to: 'e8',
        cost: 0.46111111111111114,
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
        cost: 0.41111111111111115,
      },
    ],
  },
  {
    in: 'cancel',
    out: 'no',
    tags: ['choice', 'exact'],
    tok: 'M',
  },
  {
    in: 'abort',
    out: 'no',
    tags: ['choice', 'exact'],
    tok: 'O',
  },
  {
    in: 'puzzle',
    out: '',
    tags: ['ignore'],
    tok: 'S',
  },
  {
    in: 'and',
    out: '',
    tags: ['ignore'],
    tok: 'T',
  },
];
