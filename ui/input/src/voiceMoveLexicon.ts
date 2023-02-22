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
        cost: 0.4513157894736842,
      },
      {
        to: 'f',
        cost: 0.45526315789473687,
      },
      {
        to: '8',
        cost: 0.4592105263157895,
      },
      {
        to: 'a8',
        cost: 0.5144736842105263,
      },
      {
        to: '',
        cost: 0.3421052631578947,
      },
      {
        to: 'h',
        cost: 0.55,
      },
    ],
  },
  {
    in: 'b',
    tok: 'b',
    subs: [
      {
        to: 'd',
        cost: 0.4631578947368421,
      },
      {
        to: 'e',
        cost: 0.5618421052631579,
      },
      {
        to: 'b8',
        cost: 0.6605263157894736,
      },
    ],
  },
  {
    in: 'c',
    tok: 'c',
    subs: [
      {
        to: '3',
        cost: 0.6842105263157894,
      },
    ],
  },
  {
    in: 'd',
    tok: 'd',
    subs: [
      {
        to: 'g',
        cost: 0.4710526315789474,
      },
      {
        to: 'b',
        cost: 0.4868421052631579,
      },
      {
        to: 'e',
        cost: 0.5776315789473684,
      },
      {
        to: 'd8',
        cost: 0.6960526315789474,
      },
    ],
  },
  {
    in: 'e',
    tok: 'e',
    subs: [
      {
        to: 'a',
        cost: 0.46710526315789475,
      },
      {
        to: 'b',
        cost: 0.49078947368421055,
      },
      {
        to: 'd',
        cost: 0.49473684210526314,
      },
      {
        to: 'g',
        cost: 0.5105263157894737,
      },
      {
        to: 'c',
        cost: 0.5381578947368422,
      },
      {
        to: '',
        cost: 0.4447368421052631,
      },
      {
        to: 'e8',
        cost: 0.6763157894736842,
      },
    ],
  },
  {
    in: 'f',
    tok: 'f',
    subs: [
      {
        to: 'a',
        cost: 0.6526315789473685,
      },
      {
        to: 'h',
        cost: 0.6881578947368421,
      },
    ],
  },
  {
    in: 'g',
    tok: 'g',
    subs: [
      {
        to: 'd',
        cost: 0.48289473684210527,
      },
      {
        to: 'g8',
        cost: 0.5460526315789473,
      },
    ],
  },
  {
    in: 'h',
    tok: 'h',
    subs: [
      {
        to: '8',
        cost: 0.4986842105263158,
      },
      {
        to: 'a',
        cost: 0.6289473684210526,
      },
    ],
  },
  {
    in: '1',
    tok: '1',
    subs: [
      {
        to: 'P',
        cost: 0.5736842105263158,
      },
      {
        to: 'e1',
        cost: 0.5894736842105263,
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
        cost: 0.3302631578947368,
      },
      {
        to: 'g',
        cost: 0.6052631578947368,
      },
      {
        to: 'e2',
        cost: 0.6210526315789473,
      },
      {
        to: '3',
        cost: 0.656578947368421,
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
        cost: 0.5026315789473684,
      },
      {
        to: 'e3',
        cost: 0.6802631578947368,
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
        cost: 0.581578947368421,
      },
      {
        to: 'e4',
        cost: 0.6921052631578948,
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
        cost: 0.5578947368421052,
      },
      {
        to: '5f',
        cost: 0.6131578947368421,
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
        cost: 0.6684210526315789,
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
        cost: 0.4355263157894737,
      },
      {
        to: 'a8',
        cost: 0.43947368421052635,
      },
      {
        to: 'h',
        cost: 0.4473684210526316,
      },
      {
        to: 'e',
        cost: 0.5342105263157895,
      },
      {
        to: '8a',
        cost: 0.6328947368421052,
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
        cost: 0.5539473684210526,
      },
    ],
  },
  {
    in: 'knight',
    tok: 'N',
    subs: [
      {
        to: 'K',
        cost: 0.5263157894736842,
      },
      {
        to: '',
        cost: 0.425,
      },
      {
        to: 'Na',
        cost: 0.6486842105263158,
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
        cost: 0.5855263157894737,
      },
      {
        to: '',
        cost: 0.43684210526315786,
      },
    ],
  },
  {
    in: 'queen',
    tok: 'Q',
    subs: [
      {
        to: '8Q',
        cost: 0.6171052631578947,
      },
      {
        to: 'Qe',
        cost: 0.6407894736842106,
      },
    ],
  },
  {
    in: 'king',
    tok: 'K',
    subs: [
      {
        to: 'Ka',
        cost: 0.5184210526315789,
      },
      {
        to: '',
        cost: 0.3223684210526315,
      },
      {
        to: 'Kf',
        cost: 0.5657894736842105,
      },
      {
        to: 'Ke',
        cost: 0.5697368421052631,
      },
      {
        to: 'N',
        cost: 0.6723684210526315,
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
        cost: 0.475,
      },
      {
        to: 'h',
        cost: 0.5973684210526315,
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
        cost: 0.6092105263157894,
      },
      {
        to: '!Q',
        cost: 0.6644736842105263,
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
        cost: 0.4789473684210526,
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
        cost: 0.43157894736842106,
      },
      {
        to: 'b',
        cost: 0.506578947368421,
      },
      {
        to: '',
        cost: 0.4013157894736842,
      },
    ],
  },
  {
    in: 'check',
    out: '',
    tok: '-',
    subs: [
      {
        to: '',
        cost: 0.3934210526315789,
      },
    ],
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
        cost: 0.4276315789473684,
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
        cost: 0.41578947368421054,
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
        cost: 0.40394736842105267,
      },
      {
        to: 'b',
        cost: 0.42368421052631583,
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
        cost: 0.4078947368421053,
      },
      {
        to: '',
        cost: 0.21973684210526317,
      },
      {
        to: 'e8',
        cost: 0.44342105263157894,
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
        cost: 0.4118421052631579,
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
