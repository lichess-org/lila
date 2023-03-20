// *************************** this file is generated. see ui/input/@build/README.md ***************************

export type Sub = { to: string; cost: number };

export type Entry = { in: string; tok: string; tags: string[]; val?: string; subs?: Sub[] };

export const lexicon: Entry[] = [
  {
    in: 'a',
    tok: 'a',
    tags: ['file', 'move'],
    subs: [
      {
        to: '',
        cost: 0.3724137931034483,
      },
      {
        to: 'e',
        cost: 0.4646551724137931,
      },
      {
        to: 'f',
        cost: 0.46896551724137936,
      },
      {
        to: '8',
        cost: 0.47327586206896555,
      },
      {
        to: 'a8',
        cost: 0.5336206896551724,
      },
      {
        to: 'h',
        cost: 0.5681034482758621,
      },
      {
        to: '8a',
        cost: 0.753448275862069,
      },
    ],
  },
  {
    in: 'b',
    tok: 'b',
    tags: ['file', 'move'],
    subs: [
      {
        to: '',
        cost: 0.5,
      },
      {
        to: 'd',
        cost: 0.4818965517241379,
      },
      {
        to: 'e',
        cost: 0.5896551724137931,
      },
      {
        to: 'b8',
        cost: 0.6801724137931034,
      },
    ],
  },
  {
    in: 'c',
    tok: 'c',
    tags: ['file', 'move'],
    subs: [
      {
        to: '',
        cost: 0.5,
      },
      {
        to: '3',
        cost: 0.710344827586207,
      },
      {
        to: '6',
        cost: 0.7362068965517241,
      },
      {
        to: 'g',
        cost: 0.7405172413793104,
      },
      {
        to: 'c8',
        cost: 0.7577586206896552,
      },
    ],
  },
  {
    in: 'd',
    tok: 'd',
    tags: ['file', 'move'],
    subs: [
      {
        to: '',
        cost: 0.5,
      },
      {
        to: 'g',
        cost: 0.48620689655172417,
      },
      {
        to: 'b',
        cost: 0.4991379310344828,
      },
      {
        to: 'e',
        cost: 0.6068965517241379,
      },
      {
        to: 'd8',
        cost: 0.7146551724137931,
      },
      {
        to: '3',
        cost: 0.7275862068965517,
      },
      {
        to: '8d',
        cost: 0.8439655172413794,
      },
      {
        to: 'a',
        cost: 0.856896551724138,
      },
      {
        to: 'c',
        cost: 0.8913793103448275,
      },
    ],
  },
  {
    in: 'e',
    tok: 'e',
    tags: ['file', 'move'],
    subs: [
      {
        to: '',
        cost: 0.4672413793103448,
      },
      {
        to: 'a',
        cost: 0.47758620689655173,
      },
      {
        to: 'b',
        cost: 0.5077586206896552,
      },
      {
        to: 'd',
        cost: 0.5120689655172415,
      },
      {
        to: 'g',
        cost: 0.5293103448275862,
      },
      {
        to: 'c',
        cost: 0.5508620689655173,
      },
      {
        to: 'e8',
        cost: 0.6974137931034483,
      },
    ],
  },
  {
    in: 'f',
    tok: 'f',
    tags: ['file', 'move'],
    subs: [
      {
        to: '',
        cost: 0.5,
      },
      {
        to: 'a',
        cost: 0.6758620689655173,
      },
      {
        to: 'h',
        cost: 0.7060344827586207,
      },
      {
        to: 'e',
        cost: 0.8310344827586207,
      },
      {
        to: '8f',
        cost: 0.8612068965517241,
      },
    ],
  },
  {
    in: 'g',
    tok: 'g',
    tags: ['file', 'move'],
    subs: [
      {
        to: '',
        cost: 0.5,
      },
      {
        to: 'd',
        cost: 0.503448275862069,
      },
      {
        to: 'g8',
        cost: 0.5637931034482759,
      },
      {
        to: 'c',
        cost: 0.835344827586207,
      },
      {
        to: '8g',
        cost: 0.8482758620689655,
      },
    ],
  },
  {
    in: 'h',
    tok: 'h',
    tags: ['file', 'move'],
    subs: [
      {
        to: '',
        cost: 0.5,
      },
      {
        to: '8',
        cost: 0.5163793103448276,
      },
      {
        to: 'a',
        cost: 0.6413793103448276,
      },
      {
        to: '8h',
        cost: 0.7879310344827586,
      },
      {
        to: 'e',
        cost: 0.878448275862069,
      },
    ],
  },
  {
    in: 'one',
    tok: '1',
    tags: ['rank', 'move'],
    subs: [
      {
        to: '',
        cost: 0.5,
      },
      {
        to: 'P',
        cost: 0.6025862068965517,
      },
      {
        to: 'e1',
        cost: 0.6155172413793104,
      },
      {
        to: 'f1',
        cost: 0.8137931034482759,
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
        cost: 0.33793103448275863,
      },
      {
        to: 'g',
        cost: 0.628448275862069,
      },
      {
        to: 'e2',
        cost: 0.6370689655172415,
      },
      {
        to: '3',
        cost: 0.693103448275862,
      },
      {
        to: 'd',
        cost: 0.8655172413793104,
      },
      {
        to: 'f2',
        cost: 0.8827586206896552,
      },
    ],
  },
  {
    in: 'three',
    tok: '3',
    tags: ['rank', 'move'],
    subs: [
      {
        to: '',
        cost: 0.5,
      },
      {
        to: '3e',
        cost: 0.5206896551724138,
      },
      {
        to: 'e3',
        cost: 0.7017241379310345,
      },
      {
        to: '3a',
        cost: 0.7663793103448275,
      },
      {
        to: 'f3',
        cost: 0.775,
      },
    ],
  },
  {
    in: 'four',
    tok: '4',
    tags: ['rank', 'move'],
    subs: [
      {
        to: '',
        cost: 0.5,
      },
      {
        to: 'f4',
        cost: 0.6112068965517241,
      },
      {
        to: 'e4',
        cost: 0.7189655172413794,
      },
      {
        to: '4f',
        cost: 0.8008620689655173,
      },
      {
        to: '4a',
        cost: 0.8870689655172415,
      },
    ],
  },
  {
    in: 'five',
    tok: '5',
    tags: ['rank', 'move'],
    subs: [
      {
        to: '',
        cost: 0.5,
      },
      {
        to: 'f5',
        cost: 0.585344827586207,
      },
      {
        to: '5f',
        cost: 0.6241379310344828,
      },
      {
        to: 'e5',
        cost: 0.7232758620689655,
      },
      {
        to: '5a',
        cost: 0.8267241379310345,
      },
    ],
  },
  {
    in: 'six',
    tok: '6',
    tags: ['rank', 'move'],
    subs: [
      {
        to: '',
        cost: 0.5,
      },
      {
        to: 'f6',
        cost: 0.6887931034482759,
      },
      {
        to: 'e6',
        cost: 0.7706896551724138,
      },
      {
        to: '6a',
        cost: 0.7922413793103449,
      },
      {
        to: 'h6',
        cost: 0.8094827586206896,
      },
      {
        to: 'c6',
        cost: 0.8956896551724138,
      },
    ],
  },
  {
    in: 'seven',
    tok: '7',
    tags: ['rank', 'move'],
    subs: [
      {
        to: '',
        cost: 0.5,
      },
      {
        to: 'h7',
        cost: 0.7793103448275862,
      },
      {
        to: 'e7',
        cost: 0.8224137931034483,
      },
      {
        to: '7f',
        cost: 0.8396551724137931,
      },
      {
        to: '7a',
        cost: 0.8698275862068966,
      },
      {
        to: 'f7',
        cost: 0.8741379310344828,
      },
    ],
  },
  {
    in: 'eight',
    tok: '8',
    tags: ['rank', 'move'],
    subs: [
      {
        to: '',
        cost: 0.5,
      },
      {
        to: 'a',
        cost: 0.4431034482758621,
      },
      {
        to: 'a8',
        cost: 0.4517241379310345,
      },
      {
        to: 'h',
        cost: 0.46034482758620693,
      },
      {
        to: 'e',
        cost: 0.5551724137931034,
      },
      {
        to: '8a',
        cost: 0.65,
      },
      {
        to: 'e8',
        cost: 0.731896551724138,
      },
    ],
  },
  {
    in: 'pawn',
    tok: 'P',
    tags: ['role', 'move'],
    subs: [
      {
        to: '',
        cost: 0.5,
      },
      {
        to: 'Pa',
        cost: 0.5810344827586207,
      },
      {
        to: 'Pe',
        cost: 0.7620689655172415,
      },
      {
        to: 'Pf',
        cost: 0.7965517241379311,
      },
    ],
  },
  {
    in: 'knight',
    tok: 'N',
    tags: ['role', 'move'],
    subs: [
      {
        to: '',
        cost: 0.44568965517241377,
      },
      {
        to: 'K',
        cost: 0.5594827586206896,
      },
      {
        to: 'Na',
        cost: 0.6844827586206896,
      },
      {
        to: 'Nd',
        cost: 0.818103448275862,
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
        cost: 0.5,
      },
      {
        to: 'Bf',
        cost: 0.7491379310344828,
      },
      {
        to: 'Ba',
        cost: 0.7836206896551725,
      },
    ],
  },
  {
    in: 'rook',
    tok: 'R',
    tags: ['role', 'move'],
    subs: [
      {
        to: '',
        cost: 0.47155172413793106,
      },
      {
        to: 'Ra',
        cost: 0.6198275862068966,
      },
    ],
  },
  {
    in: 'queen',
    tok: 'Q',
    tags: ['role', 'move'],
    subs: [
      {
        to: '',
        cost: 0.5,
      },
      {
        to: 'Qe',
        cost: 0.6543103448275862,
      },
      {
        to: '8Q',
        cost: 0.6586206896551725,
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
        cost: 0.3422413793103448,
      },
      {
        to: 'Ka',
        cost: 0.5465517241379311,
      },
      {
        to: 'Kf',
        cost: 0.5939655172413794,
      },
      {
        to: 'Ke',
        cost: 0.5982758620689655,
      },
      {
        to: 'N',
        cost: 0.6629310344827586,
      },
    ],
  },
  {
    in: 'castle',
    val: 'castle',
    tags: ['move', 'exact'],
    tok: '!',
    subs: [
      {
        to: '',
        cost: 0.5,
      },
    ],
  },
  {
    in: 'short castle',
    val: 'O-O',
    tags: ['move', 'exact'],
    tok: 'p!',
  },
  {
    in: 'king side castle',
    val: 'O-O',
    tags: ['move', 'exact'],
    tok: 'Kq!',
  },
  {
    in: 'castle king side',
    val: 'O-O',
    tags: ['move', 'exact'],
    tok: '!Kq',
  },
  {
    in: 'long castle',
    val: 'O-O-O',
    tags: ['move', 'exact'],
    tok: 'r!',
  },
  {
    in: 'castle queen side',
    val: 'O-O-O',
    tags: ['move', 'exact'],
    tok: '!Qq',
  },
  {
    in: 'queen side castle',
    val: 'O-O-O',
    tags: ['move', 'exact'],
    tok: 'Qq!',
  },
  {
    in: 'take',
    val: 'x',
    tags: ['move'],
    tok: '#',
    subs: [
      {
        to: '',
        cost: 0.5,
      },
    ],
  },
  {
    in: 'takes',
    val: 'x',
    tags: ['move'],
    tok: '$',
    subs: [
      {
        to: '',
        cost: 0.5,
      },
      {
        to: '6',
        cost: 0.4948275862068966,
      },
      {
        to: 'h',
        cost: 0.6327586206896552,
      },
    ],
  },
  {
    in: 'capture',
    val: 'x',
    tags: ['move'],
    tok: '%',
    subs: [
      {
        to: '',
        cost: 0.5,
      },
    ],
  },
  {
    in: 'captures',
    val: 'x',
    tags: ['move'],
    tok: '&',
    subs: [
      {
        to: '',
        cost: 0.5,
      },
      {
        to: '&R',
        cost: 0.5767241379310345,
      },
    ],
  },
  {
    in: 'promote',
    val: '=',
    tags: ['move'],
    tok: '(',
    subs: [
      {
        to: '',
        cost: 0.5,
      },
      {
        to: '8(',
        cost: 0.49051724137931035,
      },
    ],
  },
  {
    in: 'promotes',
    val: '=',
    tags: ['move'],
    tok: ')',
    subs: [
      {
        to: '',
        cost: 0.5,
      },
    ],
  },
  {
    in: 'promotion',
    val: '=',
    tags: ['move'],
    tok: '*',
    subs: [
      {
        to: '',
        cost: 0.5,
      },
    ],
  },
  {
    in: 'oops',
    val: 'takeback',
    tags: ['command', 'rounds', 'exact'],
    tok: '+',
    subs: [
      {
        to: '',
        cost: 0.5,
      },
    ],
  },
  {
    in: 'undo',
    val: 'takeback',
    tags: ['command', 'rounds', 'exact'],
    tok: '-',
    subs: [
      {
        to: '',
        cost: 0.5,
      },
    ],
  },
  {
    in: 'draw',
    val: 'draw',
    tags: ['command', 'rounds', 'exact'],
    tok: '.',
    subs: [
      {
        to: '',
        cost: 0.5,
      },
    ],
  },
  {
    in: 'offer draw',
    val: 'draw',
    tags: ['command', 'rounds', 'exact'],
    tok: 's.',
  },
  {
    in: 'accept draw',
    val: 'draw',
    tags: ['command', 'rounds', 'exact'],
    tok: 't.',
  },
  {
    in: 'resign',
    val: 'resign',
    tags: ['command', 'rounds', 'exact'],
    tok: '/',
    subs: [
      {
        to: '',
        cost: 0.5,
      },
    ],
  },
  {
    in: 'rematch',
    val: 'rematch',
    tags: ['command', 'rounds', 'exact'],
    tok: '0',
    subs: [
      {
        to: '',
        cost: 0.5,
      },
    ],
  },
  {
    in: 'clock',
    val: 'clock',
    tags: ['command', 'rounds', 'exact'],
    tok: '9',
    subs: [
      {
        to: '',
        cost: 0.5,
      },
    ],
  },
  {
    in: 'opponent',
    val: 'who',
    tags: ['command', 'rounds', 'exact'],
    tok: ':',
    subs: [
      {
        to: '',
        cost: 0.5,
      },
    ],
  },
  {
    in: 'next',
    val: 'next',
    tags: ['command', 'puzzle', 'exact'],
    tok: ';',
    subs: [
      {
        to: '',
        cost: 0.5,
      },
    ],
  },
  {
    in: 'help',
    val: '?',
    tags: ['command', 'exact'],
    tok: '<',
    subs: [
      {
        to: '',
        cost: 0.5,
      },
    ],
  },
  {
    in: 'stop',
    val: 'stop',
    tags: ['command', 'exact'],
    tok: '=',
    subs: [
      {
        to: '',
        cost: 0.5,
      },
    ],
  },
  {
    in: 'yes',
    val: 'yes',
    tags: ['choice', 'exact'],
    tok: '>',
    subs: [
      {
        to: '',
        cost: 0.23017241379310346,
      },
      {
        to: 'e',
        cost: 0.42155172413793107,
      },
      {
        to: 'e8',
        cost: 0.4560344827586207,
      },
    ],
  },
  {
    in: 'confirm',
    val: 'yes',
    tags: ['choice', 'exact'],
    tok: '?',
    subs: [
      {
        to: '',
        cost: 0.5,
      },
    ],
  },
  {
    in: 'no',
    val: 'no',
    tags: ['choice', 'exact'],
    tok: '@',
    subs: [
      {
        to: '',
        cost: 0.5,
      },
      {
        to: 'N',
        cost: 0.41724137931034483,
      },
    ],
  },
  {
    in: 'clear',
    val: 'no',
    tags: ['choice', 'exact'],
    tok: 'A',
    subs: [
      {
        to: '',
        cost: 0.5,
      },
    ],
  },
  {
    in: 'close',
    val: 'no',
    tags: ['choice', 'exact'],
    tok: 'C',
    subs: [
      {
        to: '',
        cost: 0.5,
      },
    ],
  },
  {
    in: 'cancel',
    val: 'no',
    tags: ['choice', 'exact'],
    tok: 'D',
    subs: [
      {
        to: '',
        cost: 0.5,
      },
    ],
  },
  {
    in: 'abort',
    val: 'no',
    tags: ['choice', 'exact'],
    tok: 'E',
    subs: [
      {
        to: '',
        cost: 0.5,
      },
    ],
  },
  {
    in: 'blue',
    val: 'blue',
    tags: ['choice', 'exact'],
    tok: 'F',
    subs: [
      {
        to: '',
        cost: 0.5,
      },
      {
        to: '2',
        cost: 0.41293103448275864,
      },
      {
        to: 'b',
        cost: 0.4344827586206897,
      },
    ],
  },
  {
    in: 'green',
    val: 'green',
    tags: ['choice', 'exact'],
    tok: 'G',
    subs: [
      {
        to: '',
        cost: 0.5,
      },
      {
        to: 'Q',
        cost: 0.4,
      },
      {
        to: '3',
        cost: 0.42586206896551726,
      },
    ],
  },
  {
    in: 'yellow',
    val: 'yellow',
    tags: ['choice', 'exact'],
    tok: 'H',
    subs: [
      {
        to: '',
        cost: 0.5,
      },
    ],
  },
  {
    in: 'red',
    val: 'red',
    tags: ['choice', 'exact'],
    tok: 'I',
    subs: [
      {
        to: '',
        cost: 0.5,
      },
      {
        to: '8',
        cost: 0.4387931034482759,
      },
    ],
  },
  {
    in: 'pink',
    val: 'pink',
    tags: ['choice', 'exact'],
    tok: 'J',
    subs: [
      {
        to: '',
        cost: 0.5,
      },
    ],
  },
  {
    in: 'purple',
    val: 'purple',
    tags: ['choice', 'exact'],
    tok: 'L',
    subs: [
      {
        to: '',
        cost: 0.5,
      },
    ],
  },
  {
    in: 'orange',
    val: 'orange',
    tags: ['choice', 'exact'],
    tok: 'M',
    subs: [
      {
        to: '',
        cost: 0.5,
      },
    ],
  },
  {
    in: 'grey',
    val: 'paleGrey',
    tags: ['choice', 'exact'],
    tok: 'O',
    subs: [
      {
        to: '',
        cost: 0.5,
      },
    ],
  },
  {
    in: 'alfa',
    val: 'a',
    tags: ['file', 'move', 'nato'],
    tok: 'S',
    subs: [
      {
        to: '',
        cost: 0.5,
      },
    ],
  },
  {
    in: 'bravo',
    val: 'b',
    tags: ['file', 'move', 'nato'],
    tok: 'T',
    subs: [
      {
        to: '',
        cost: 0.5,
      },
    ],
  },
  {
    in: 'charlie',
    val: 'c',
    tags: ['file', 'move', 'nato'],
    tok: 'U',
    subs: [
      {
        to: '',
        cost: 0.5,
      },
    ],
  },
  {
    in: 'delta',
    val: 'd',
    tags: ['file', 'move', 'nato'],
    tok: 'V',
    subs: [
      {
        to: '',
        cost: 0.5,
      },
    ],
  },
  {
    in: 'echo',
    val: 'e',
    tags: ['file', 'move', 'nato'],
    tok: 'W',
    subs: [
      {
        to: '',
        cost: 0.5,
      },
    ],
  },
  {
    in: 'foxtrot',
    val: 'f',
    tags: ['file', 'move', 'nato'],
    tok: 'X',
    subs: [
      {
        to: '',
        cost: 0.5,
      },
    ],
  },
  {
    in: 'golf',
    val: 'g',
    tags: ['file', 'move', 'nato'],
    tok: 'Y',
    subs: [
      {
        to: '',
        cost: 0.5,
      },
    ],
  },
  {
    in: 'hotel',
    val: 'h',
    tags: ['file', 'move', 'nato'],
    tok: 'Z',
    subs: [
      {
        to: '',
        cost: 0.5,
      },
    ],
  },
  {
    in: 'mate',
    val: '',
    tags: ['move', 'ignore'],
    tok: '[',
    subs: [
      {
        to: '',
        cost: 0,
      },
      {
        to: 'N',
        cost: 0.4474137931034483,
      },
      {
        to: 'b',
        cost: 0.525,
      },
    ],
  },
  {
    in: 'check',
    val: '',
    tags: ['move', 'ignore'],
    tok: '\\',
    subs: [
      {
        to: '',
        cost: 0,
      },
    ],
  },
  {
    in: 'puzzle',
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
    in: 'and',
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
    in: 'oh',
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
    in: 'um',
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
    in: 'uh',
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
  {
    in: 'hmm',
    val: '',
    tags: ['ignore'],
    tok: 'j',
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
    tok: 'k',
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
    tok: 'l',
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
    tok: 'm',
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
    tok: 'n',
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
    tok: 'o',
    subs: [
      {
        to: '',
        cost: 0,
      },
    ],
  },
  {
    in: 'short',
    tok: 'p',
    tags: ['part'],
    subs: [
      {
        to: '',
        cost: 0.5,
      },
    ],
  },
  {
    in: 'side',
    tok: 'q',
    tags: ['part'],
    subs: [
      {
        to: '',
        cost: 0.5,
      },
    ],
  },
  {
    in: 'long',
    tok: 'r',
    tags: ['part'],
    subs: [
      {
        to: '',
        cost: 0.5,
      },
      {
        to: '1',
        cost: 0.4043103448275862,
      },
      {
        to: 'P',
        cost: 0.40862068965517245,
      },
    ],
  },
  {
    in: 'offer',
    tok: 's',
    tags: ['part'],
    subs: [
      {
        to: '',
        cost: 0.5,
      },
    ],
  },
  {
    in: 'accept',
    tok: 't',
    tags: ['part'],
    subs: [
      {
        to: '',
        cost: 0.5,
      },
    ],
  },
];
