// this file is generated. see ui/input/@build/README.md

export type Sub = { to: string; cost: number };

export type Token = { in: string; tok?: string; out?: string; subs?: Sub[] };

export const lexicon: Token[] = [
  {
    in: 'a',
    tok: 'a',
    subs: [
      {
        to: 'e',
        cost: 0.4534246575342466,
      },
      {
        to: 'f',
        cost: 0.4575342465753425,
      },
      {
        to: '8',
        cost: 0.46164383561643835,
      },
      {
        to: 'a8',
        cost: 0.5191780821917809,
      },
      {
        to: '',
        cost: 0.347945205479452,
      },
      {
        to: 'h',
        cost: 0.5561643835616439,
      },
    ],
  },
  {
    in: 'b',
    tok: 'b',
    subs: [
      {
        to: 'd',
        cost: 0.4657534246575342,
      },
      {
        to: 'e',
        cost: 0.5684931506849316,
      },
      {
        to: 'b8',
        cost: 0.6630136986301369,
      },
    ],
  },
  {
    in: 'c',
    tok: 'c',
    subs: [
      {
        to: '3',
        cost: 0.6835616438356165,
      },
    ],
  },
  {
    in: 'd',
    tok: 'd',
    subs: [
      {
        to: 'g',
        cost: 0.473972602739726,
      },
      {
        to: 'b',
        cost: 0.4904109589041096,
      },
      {
        to: 'e',
        cost: 0.584931506849315,
      },
      {
        to: 'd8',
        cost: 0.695890410958904,
      },
    ],
  },
  {
    in: 'e',
    tok: 'e',
    subs: [
      {
        to: 'a',
        cost: 0.46986301369863015,
      },
      {
        to: 'b',
        cost: 0.4945205479452055,
      },
      {
        to: 'd',
        cost: 0.4986301369863014,
      },
      {
        to: 'g',
        cost: 0.5150684931506849,
      },
      {
        to: 'c',
        cost: 0.5438356164383562,
      },
      {
        to: '',
        cost: 0.44657534246575337,
      },
      {
        to: 'e8',
        cost: 0.6753424657534246,
      },
    ],
  },
  {
    in: 'f',
    tok: 'f',
    subs: [
      {
        to: 'a',
        cost: 0.6547945205479451,
      },
      {
        to: 'h',
        cost: 0.6876712328767123,
      },
    ],
  },
  {
    in: 'g',
    tok: 'g',
    subs: [
      {
        to: 'd',
        cost: 0.4863013698630137,
      },
      {
        to: 'g8',
        cost: 0.552054794520548,
      },
    ],
  },
  {
    in: 'h',
    tok: 'h',
    subs: [
      {
        to: '8',
        cost: 0.5027397260273972,
      },
      {
        to: 'a',
        cost: 0.6301369863013698,
      },
    ],
  },
  {
    in: '1',
    tok: '1',
    subs: [
      {
        to: 'P',
        cost: 0.5808219178082192,
      },
      {
        to: 'e1',
        cost: 0.5972602739726027,
      },
    ],
  },
  {
    in: 'one',
    tok: '1',
  },
  {
    in: '2',
    tok: '2',
    subs: [
      {
        to: '',
        cost: 0.3356164383561644,
      },
      {
        to: 'g',
        cost: 0.6054794520547945,
      },
      {
        to: 'e2',
        cost: 0.6219178082191781,
      },
      {
        to: '3',
        cost: 0.6589041095890411,
      },
    ],
  },
  {
    in: 'two',
    tok: '2',
  },
  {
    in: '3',
    tok: '3',
    subs: [
      {
        to: '3e',
        cost: 0.5068493150684932,
      },
      {
        to: 'e3',
        cost: 0.6794520547945204,
      },
    ],
  },
  {
    in: 'three',
    tok: '3',
  },
  {
    in: '4',
    tok: '4',
    subs: [
      {
        to: 'f4',
        cost: 0.589041095890411,
      },
      {
        to: 'e4',
        cost: 0.6917808219178081,
      },
    ],
  },
  {
    in: 'four',
    tok: '4',
  },
  {
    in: '5',
    tok: '5',
    subs: [
      {
        to: 'f5',
        cost: 0.5643835616438356,
      },
      {
        to: '5f',
        cost: 0.6136986301369862,
      },
    ],
  },
  {
    in: 'five',
    tok: '5',
  },
  {
    in: '6',
    tok: '6',
    subs: [
      {
        to: 'f6',
        cost: 0.6671232876712329,
      },
    ],
  },
  {
    in: 'six',
    tok: '6',
  },
  {
    in: '7',
    tok: '7',
  },
  {
    in: 'seven',
    tok: '7',
  },
  {
    in: '8',
    tok: '8',
    subs: [
      {
        to: 'a',
        cost: 0.43698630136986305,
      },
      {
        to: 'a8',
        cost: 0.4410958904109589,
      },
      {
        to: 'h',
        cost: 0.4493150684931507,
      },
      {
        to: 'e',
        cost: 0.5397260273972603,
      },
      {
        to: '8a',
        cost: 0.6342465753424658,
      },
    ],
  },
  {
    in: 'eight',
    tok: '8',
  },
  {
    in: 'pawn',
    tok: 'P',
    subs: [
      {
        to: 'Pa',
        cost: 0.5602739726027397,
      },
    ],
  },
  {
    in: 'knight',
    tok: 'N',
    subs: [
      {
        to: 'K',
        cost: 0.5315068493150685,
      },
      {
        to: '',
        cost: 0.42602739726027394,
      },
      {
        to: 'Na',
        cost: 0.6506849315068493,
      },
    ],
  },
  {
    in: 'bishop',
    tok: 'B',
  },
  {
    in: 'rook',
    tok: 'R',
    subs: [
      {
        to: 'Ra',
        cost: 0.5931506849315068,
      },
      {
        to: '',
        cost: 0.4383561643835616,
      },
    ],
  },
  {
    in: 'queen',
    tok: 'Q',
    subs: [
      {
        to: '8Q',
        cost: 0.6178082191780822,
      },
      {
        to: 'Qe',
        cost: 0.6424657534246575,
      },
    ],
  },
  {
    in: 'king',
    tok: 'K',
    subs: [
      {
        to: 'Ka',
        cost: 0.5232876712328767,
      },
      {
        to: '',
        cost: 0.32739726027397253,
      },
      {
        to: 'Kf',
        cost: 0.5726027397260274,
      },
      {
        to: 'Ke',
        cost: 0.5767123287671233,
      },
      {
        to: 'N',
        cost: 0.6712328767123288,
      },
    ],
  },
  {
    in: 'takes',
    tok: 'x',
    out: 'x',
    subs: [
      {
        to: '6',
        cost: 0.4780821917808219,
      },
      {
        to: 'h',
        cost: 0.6013698630136987,
      },
    ],
  },
  {
    in: 'captures',
    out: 'x',
    tok: '!',
    subs: [
      {
        to: '!R',
        cost: 0.6095890410958904,
      },
    ],
  },
  {
    in: 'castle',
    out: 'o-o',
    tok: '"',
  },
  {
    in: 'short castle',
    out: 'o-o',
    tok: '#',
  },
  {
    in: 'king side castle',
    out: 'o-o',
    tok: '$',
  },
  {
    in: 'castle king side',
    out: 'o-o',
    tok: '%',
  },
  {
    in: 'long castle',
    out: 'o-o-o',
    tok: '&',
  },
  {
    in: 'castle queen side',
    out: 'o-o-o',
    tok: "'",
  },
  {
    in: 'queen side castle',
    out: 'o-o-o',
    tok: '(',
  },
  {
    in: 'promote',
    tok: '=',
    out: '=',
    subs: [
      {
        to: '8=',
        cost: 0.4821917808219178,
      },
    ],
  },
  {
    in: 'promotion',
    out: '=',
    tok: ')',
  },
  {
    in: 'promote two',
    out: '=',
    tok: '*',
  },
  {
    in: 'promotes two',
    out: '=',
    tok: '+',
  },
  {
    in: 'mate',
    out: '',
    tok: ',',
    subs: [
      {
        to: 'N',
        cost: 0.4328767123287671,
      },
      {
        to: 'b',
        cost: 0.510958904109589,
      },
    ],
  },
  {
    in: 'check',
    out: '',
    tok: '-',
  },
  {
    in: 'takeback',
    out: 'takeback',
    tok: '.',
  },
  {
    in: 'draw',
    out: 'draw',
    tok: '/',
  },
  {
    in: 'offer draw',
    out: 'draw',
    tok: '0',
  },
  {
    in: 'accept draw',
    out: 'draw',
    tok: '9',
  },
  {
    in: 'resign',
    out: 'resign',
    tok: ':',
  },
  {
    in: 'rematch',
    out: 'rematch',
    tok: ';',
  },
  {
    in: 'red',
    out: 'red',
    tok: '<',
    subs: [
      {
        to: '8',
        cost: 0.42876712328767125,
      },
    ],
  },
  {
    in: 'yellow',
    out: 'yellow',
    tok: '>',
  },
  {
    in: 'green',
    out: 'green',
    tok: '?',
    subs: [
      {
        to: 'Q',
        cost: 0.4,
      },
      {
        to: '3',
        cost: 0.41643835616438357,
      },
    ],
  },
  {
    in: 'blue',
    out: 'blue',
    tok: '@',
    subs: [
      {
        to: '2',
        cost: 0.4041095890410959,
      },
      {
        to: 'b',
        cost: 0.4246575342465754,
      },
    ],
  },
  {
    in: 'next',
    out: 'next',
    tok: 'A',
  },
  {
    in: 'skip',
    out: 'next',
    tok: 'C',
  },
  {
    in: 'continue',
    out: 'next',
    tok: 'D',
  },
  {
    in: 'back',
    out: 'back',
    tok: 'E',
  },
  {
    in: 'last',
    out: 'last',
    tok: 'F',
  },
  {
    in: 'first',
    out: 'first',
    tok: 'G',
  },
  {
    in: 'yes',
    out: 'yes',
    tok: 'H',
    subs: [
      {
        to: 'e',
        cost: 0.4082191780821918,
      },
      {
        to: '',
        cost: 0.22054794520547946,
      },
      {
        to: 'e8',
        cost: 0.4452054794520548,
      },
    ],
  },
  {
    in: 'okay',
    out: 'yes',
    tok: 'I',
  },
  {
    in: 'confirm',
    out: 'yes',
    tok: 'J',
  },
  {
    in: 'no',
    out: 'no',
    tok: 'L',
    subs: [
      {
        to: 'N',
        cost: 0.4123287671232877,
      },
    ],
  },
  {
    in: 'cancel',
    out: 'no',
    tok: 'M',
  },
  {
    in: 'abort',
    out: 'no',
    tok: 'O',
  },
  {
    in: 'up vote',
    out: 'upv',
    tok: 'S',
  },
  {
    in: 'down vote',
    out: 'downv',
    tok: 'T',
  },
  {
    in: 'help',
    out: '?',
    tok: 'U',
  },
  {
    in: 'clock',
    out: 'clock',
    tok: 'V',
  },
  {
    in: 'opponent',
    out: 'who',
    tok: 'W',
  },
  {
    in: 'puzzle',
    out: '',
    tok: 'X',
  },
  {
    in: 'and',
    out: '',
    tok: 'Y',
  },
];
