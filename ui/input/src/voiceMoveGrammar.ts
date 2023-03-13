// *************************** this file is generated. see ui/input/@build/README.md ***************************

export type Sub = { to: string; cost: number };

export type Tag = 'file' | 'rank' | 'role' | 'move' | 'choice' | 'command' | 'ignore' | 'exact' | 'rounds';

export type Entry = { in: string; tok: string; tags: Tag[]; val?: string; subs?: Sub[] };

export const lexicon: Entry[] = [
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
        cost: 0.40555555555555556,
      },
      {
        to: 'h',
        cost: 0.6111111111111112,
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
        cost: 0.6000000000000001,
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
    in: 'one',
    tok: '1',
    tags: ['rank', 'move'],
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
    in: 'two',
    tok: '2',
    tags: ['rank', 'move'],
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
    in: 'three',
    tok: '3',
    tags: ['rank', 'move'],
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
    in: 'four',
    tok: '4',
    tags: ['rank', 'move'],
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
    in: 'five',
    tok: '5',
    tags: ['rank', 'move'],
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
    in: 'six',
    tok: '6',
    tags: ['rank', 'move'],
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
    in: 'seven',
    tok: '7',
    tags: ['rank', 'move'],
    subs: [
      {
        to: 'h7',
        cost: 0.8888888888888888,
      },
    ],
  },
  {
    in: 'eight',
    tok: '8',
    tags: ['rank', 'move'],
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
        cost: 0.5888888888888889,
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
    in: 'pawn',
    tok: 'P',
    tags: ['role', 'move'],
    subs: [
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
        cost: 0.5944444444444444,
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
    in: 'castle',
    val: 'O-O',
    tags: ['move', 'exact'],
    tok: '!',
  },
  {
    in: 'short castle',
    val: 'O-O',
    tags: ['move', 'exact'],
    tok: '"',
  },
  {
    in: 'king side castle',
    val: 'O-O',
    tags: ['move', 'exact'],
    tok: '#',
  },
  {
    in: 'castle king side',
    val: 'O-O',
    tags: ['move', 'exact'],
    tok: '$',
  },
  {
    in: 'long castle',
    val: 'O-O-O',
    tags: ['move', 'exact'],
    tok: '%',
  },
  {
    in: 'castle queen side',
    val: 'O-O-O',
    tags: ['move', 'exact'],
    tok: '&',
  },
  {
    in: 'queen side castle',
    val: 'O-O-O',
    tags: ['move', 'exact'],
    tok: "'",
  },
  {
    in: 'takes',
    val: 'x',
    tags: ['move'],
    tok: '(',
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
    val: 'x',
    tags: ['move'],
    tok: ')',
    subs: [
      {
        to: ')R',
        cost: 0.6166666666666667,
      },
      {
        to: 'P)',
        cost: 0.7166666666666667,
      },
    ],
  },
  {
    in: 'promote',
    val: '=',
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
    val: '=',
    tags: ['move'],
    tok: '+',
  },
  {
    in: 'mate',
    val: '',
    tags: ['move', 'ignore'],
    tok: '-',
    subs: [
      {
        to: '',
        cost: 0,
      },
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
    val: '',
    tags: ['move', 'ignore'],
    tok: '.',
    subs: [
      {
        to: '',
        cost: 0,
      },
    ],
  },
  {
    in: 'takeback',
    val: 'takeback',
    tags: ['command', 'rounds', 'exact'],
    tok: '/',
  },
  {
    in: 'draw',
    val: 'draw',
    tags: ['command', 'rounds', 'exact'],
    tok: '0',
  },
  {
    in: 'offer draw',
    val: 'draw',
    tags: ['command', 'rounds', 'exact'],
    tok: '9',
  },
  {
    in: 'accept draw',
    val: 'draw',
    tags: ['command', 'rounds', 'exact'],
    tok: ':',
  },
  {
    in: 'resign',
    val: 'resign',
    tags: ['command', 'rounds', 'exact'],
    tok: ';',
  },
  {
    in: 'rematch',
    val: 'rematch',
    tags: ['command', 'exact'],
    tok: '<',
  },
  {
    in: 'next',
    val: 'next',
    tags: ['command', 'exact'],
    tok: '=',
  },
  {
    in: 'back',
    val: 'back',
    tags: ['command', 'exact'],
    tok: '>',
  },
  {
    in: 'up vote',
    val: 'upv',
    tags: ['command', 'exact'],
    tok: '?',
  },
  {
    in: 'down vote',
    val: 'downv',
    tags: ['command', 'exact'],
    tok: '@',
  },
  {
    in: 'help',
    val: '?',
    tags: ['command', 'exact'],
    tok: 'A',
  },
  {
    in: 'clock',
    val: 'clock',
    tags: ['command', 'exact'],
    tok: 'C',
  },
  {
    in: 'opponent',
    val: 'who',
    tags: ['command', 'exact'],
    tok: 'D',
  },
  {
    in: 'stop',
    val: 'stop',
    tags: ['command', 'exact'],
    tok: 'E',
  },
  {
    in: 'red',
    val: 'red',
    tags: ['choice', 'exact'],
    tok: 'F',
    subs: [
      {
        to: '8',
        cost: 0.4388888888888889,
      },
    ],
  },
  {
    in: 'yellow',
    val: 'yellow',
    tags: ['choice', 'exact'],
    tok: 'G',
  },
  {
    in: 'green',
    val: 'green',
    tags: ['choice', 'exact'],
    tok: 'H',
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
    val: 'blue',
    tags: ['choice', 'exact'],
    tok: 'I',
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
    val: 'yes',
    tags: ['choice', 'exact'],
    tok: 'J',
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
    val: 'yes',
    tags: ['choice', 'exact'],
    tok: 'L',
  },
  {
    in: 'confirm',
    val: 'yes',
    tags: ['choice', 'exact'],
    tok: 'M',
  },
  {
    in: 'no',
    val: 'no',
    tags: ['choice', 'exact'],
    tok: 'O',
    subs: [
      {
        to: 'N',
        cost: 0.41111111111111115,
      },
    ],
  },
  {
    in: 'clear',
    val: 'no',
    tags: ['choice', 'exact'],
    tok: 'S',
  },
  {
    in: 'close',
    val: 'no',
    tags: ['choice', 'exact'],
    tok: 'T',
  },
  {
    in: 'cancel',
    val: 'no',
    tags: ['choice', 'exact'],
    tok: 'U',
  },
  {
    in: 'abort',
    val: 'no',
    tags: ['choice', 'exact'],
    tok: 'V',
  },
  {
    in: 'puzzle',
    val: '',
    tags: ['ignore'],
    tok: 'W',
    subs: [
      {
        to: '',
        cost: 0,
      },
    ],
  },
  {
    in: 'and',
    val: '',
    tags: ['ignore'],
    tok: 'X',
    subs: [
      {
        to: '',
        cost: 0,
      },
    ],
  },
  {
    in: 'oh',
    val: '',
    tags: ['ignore'],
    tok: 'Y',
    subs: [
      {
        to: '',
        cost: 0,
      },
    ],
  },
  {
    in: 'um',
    val: '',
    tags: ['ignore'],
    tok: 'Z',
    subs: [
      {
        to: '',
        cost: 0,
      },
    ],
  },
  {
    in: 'uh',
    val: '',
    tags: ['ignore'],
    tok: '[',
    subs: [
      {
        to: '',
        cost: 0,
      },
    ],
  },
  {
    in: 'hmm',
    val: '',
    tags: ['ignore'],
    tok: '\\',
    subs: [
      {
        to: '',
        cost: 0,
      },
    ],
  },
  {
    in: 'huh',
    val: '',
    tags: ['ignore'],
    tok: ']',
    subs: [
      {
        to: '',
        cost: 0,
      },
    ],
  },
  {
    in: 'his',
    val: '',
    tags: ['ignore'],
    tok: '^',
    subs: [
      {
        to: '',
        cost: 0,
      },
    ],
  },
  {
    in: 'her',
    val: '',
    tags: ['ignore'],
    tok: '_',
    subs: [
      {
        to: '',
        cost: 0,
      },
    ],
  },
  {
    in: 'the',
    val: '',
    tags: ['ignore'],
    tok: '`',
    subs: [
      {
        to: '',
        cost: 0,
      },
    ],
  },
  {
    in: 'their',
    val: '',
    tags: ['ignore'],
    tok: 'i',
    subs: [
      {
        to: '',
        cost: 0,
      },
    ],
  },
];
