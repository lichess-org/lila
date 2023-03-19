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
        cost: 0.3149425287356322,
      },
      {
        to: 'e',
        cost: 0.4431034482758621,
      },
      {
        to: 'f',
        cost: 0.4459770114942529,
      },
      {
        to: '8',
        cost: 0.4488505747126437,
      },
      {
        to: 'a8',
        cost: 0.48908045977011494,
      },
      {
        to: 'h',
        cost: 0.5120689655172415,
      },
      {
        to: '8a',
        cost: 0.635632183908046,
      },
      {
        to: '8e',
        cost: 0.7563218390804598,
      },
      {
        to: 'b',
        cost: 0.7764367816091955,
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
        cost: 0.4545977011494253,
      },
      {
        to: 'e',
        cost: 0.5264367816091955,
      },
      {
        to: 'b8',
        cost: 0.5867816091954023,
      },
      {
        to: '8b',
        cost: 0.7448275862068966,
      },
      {
        to: 'a',
        cost: 0.7649425287356322,
      },
      {
        to: 'Q',
        cost: 0.842528735632184,
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
        cost: 0.6068965517241379,
      },
      {
        to: '6',
        cost: 0.6241379310344828,
      },
      {
        to: 'g',
        cost: 0.6270114942528736,
      },
      {
        to: 'c8',
        cost: 0.6385057471264368,
      },
      {
        to: 'e',
        cost: 0.8770114942528735,
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
        cost: 0.4574712643678161,
      },
      {
        to: 'b',
        cost: 0.46609195402298853,
      },
      {
        to: 'e',
        cost: 0.5379310344827586,
      },
      {
        to: 'd8',
        cost: 0.6097701149425288,
      },
      {
        to: '3',
        cost: 0.6183908045977011,
      },
      {
        to: '8d',
        cost: 0.695977011494253,
      },
      {
        to: 'a',
        cost: 0.7045977011494253,
      },
      {
        to: 'c',
        cost: 0.7275862068965517,
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
        cost: 0.3781609195402299,
      },
      {
        to: 'a',
        cost: 0.4517241379310345,
      },
      {
        to: 'b',
        cost: 0.47183908045977013,
      },
      {
        to: 'd',
        cost: 0.47471264367816096,
      },
      {
        to: 'g',
        cost: 0.48620689655172417,
      },
      {
        to: 'c',
        cost: 0.5005747126436781,
      },
      {
        to: 'e8',
        cost: 0.5982758620689655,
      },
      {
        to: 'h',
        cost: 0.82816091954023,
      },
      {
        to: 'f',
        cost: 0.8626436781609196,
      },
      {
        to: '8',
        cost: 0.8913793103448275,
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
        cost: 0.5839080459770115,
      },
      {
        to: 'h',
        cost: 0.6040229885057471,
      },
      {
        to: 'e',
        cost: 0.6873563218390805,
      },
      {
        to: '8f',
        cost: 0.707471264367816,
      },
      {
        to: 'f8',
        cost: 0.871264367816092,
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
        cost: 0.46896551724137936,
      },
      {
        to: 'g8',
        cost: 0.5091954022988506,
      },
      {
        to: 'c',
        cost: 0.6902298850574713,
      },
      {
        to: '8g',
        cost: 0.6988505747126437,
      },
      {
        to: '3',
        cost: 0.8137931034482759,
      },
      {
        to: 'e',
        cost: 0.8166666666666667,
      },
      {
        to: 'b',
        cost: 0.8827586206896552,
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
        cost: 0.47758620689655173,
      },
      {
        to: 'a',
        cost: 0.5609195402298851,
      },
      {
        to: '8h',
        cost: 0.6586206896551725,
      },
      {
        to: 'e',
        cost: 0.7189655172413794,
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
        cost: 0.5350574712643679,
      },
      {
        to: 'e1',
        cost: 0.5436781609195402,
      },
      {
        to: 'f1',
        cost: 0.6758620689655173,
      },
      {
        to: '1a',
        cost: 0.782183908045977,
      },
      {
        to: '1f',
        cost: 0.7850574712643679,
      },
      {
        to: 'a1',
        cost: 0.7879310344827586,
      },
      {
        to: '1e',
        cost: 0.7908045977011495,
      },
      {
        to: 'g1',
        cost: 0.8195402298850575,
      },
      {
        to: 'b1',
        cost: 0.856896551724138,
      },
      {
        to: 'd1',
        cost: 0.885632183908046,
      },
      {
        to: 'c1',
        cost: 0.8885057471264368,
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
        cost: 0.29195402298850576,
      },
      {
        to: 'g',
        cost: 0.5522988505747126,
      },
      {
        to: 'e2',
        cost: 0.5580459770114943,
      },
      {
        to: '3',
        cost: 0.5954022988505747,
      },
      {
        to: 'd',
        cost: 0.710344827586207,
      },
      {
        to: 'f2',
        cost: 0.7218390804597701,
      },
      {
        to: '2a',
        cost: 0.8022988505747126,
      },
      {
        to: '2e',
        cost: 0.8051724137931034,
      },
      {
        to: 'd2',
        cost: 0.8310344827586207,
      },
      {
        to: 'g2',
        cost: 0.8339080459770115,
      },
      {
        to: 'a2',
        cost: 0.8367816091954023,
      },
      {
        to: '2f',
        cost: 0.8655172413793104,
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
        cost: 0.48045977011494256,
      },
      {
        to: 'e3',
        cost: 0.6011494252873564,
      },
      {
        to: '3a',
        cost: 0.6442528735632185,
      },
      {
        to: 'f3',
        cost: 0.65,
      },
      {
        to: '3f',
        cost: 0.7390804597701149,
      },
      {
        to: 'g',
        cost: 0.7419540229885058,
      },
      {
        to: 'a3',
        cost: 0.8080459770114943,
      },
      {
        to: '3d',
        cost: 0.8109195402298851,
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
        cost: 0.5408045977011494,
      },
      {
        to: 'e4',
        cost: 0.6126436781609196,
      },
      {
        to: '4f',
        cost: 0.6672413793103449,
      },
      {
        to: '4a',
        cost: 0.724712643678161,
      },
      {
        to: '4e',
        cost: 0.7362068965517241,
      },
      {
        to: '5',
        cost: 0.7591954022988505,
      },
      {
        to: 'd4',
        cost: 0.7965517241379311,
      },
      {
        to: 'a4',
        cost: 0.8511494252873564,
      },
      {
        to: 'P',
        cost: 0.8540229885057471,
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
        cost: 0.5235632183908047,
      },
      {
        to: '5f',
        cost: 0.5494252873563219,
      },
      {
        to: 'e5',
        cost: 0.6155172413793104,
      },
      {
        to: '5a',
        cost: 0.6844827586206896,
      },
      {
        to: 'a5',
        cost: 0.7505747126436781,
      },
      {
        to: '5e',
        cost: 0.7793103448275862,
      },
      {
        to: '4',
        cost: 0.8396551724137931,
      },
      {
        to: 'c5',
        cost: 0.8683908045977011,
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
        cost: 0.5925287356321839,
      },
      {
        to: 'e6',
        cost: 0.6471264367816092,
      },
      {
        to: '6a',
        cost: 0.6614942528735632,
      },
      {
        to: 'h6',
        cost: 0.6729885057471265,
      },
      {
        to: 'c6',
        cost: 0.7304597701149426,
      },
      {
        to: 'a6',
        cost: 0.753448275862069,
      },
      {
        to: 'g6',
        cost: 0.8741379310344828,
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
        cost: 0.6528735632183909,
      },
      {
        to: 'e7',
        cost: 0.6816091954022989,
      },
      {
        to: '7f',
        cost: 0.693103448275862,
      },
      {
        to: '7a',
        cost: 0.7132183908045977,
      },
      {
        to: 'f7',
        cost: 0.7160919540229885,
      },
      {
        to: 'd7',
        cost: 0.7936781609195402,
      },
      {
        to: '6',
        cost: 0.8224137931034483,
      },
      {
        to: 'b7',
        cost: 0.825287356321839,
      },
      {
        to: '7e',
        cost: 0.8597701149425288,
      },
      {
        to: 'a7',
        cost: 0.8942528735632185,
      },
      {
        to: '7^',
        cost: 0.8971264367816092,
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
        cost: 0.47011494252873565,
      },
      {
        to: 'a',
        cost: 0.4287356321839081,
      },
      {
        to: 'a8',
        cost: 0.4344827586206897,
      },
      {
        to: 'h',
        cost: 0.4402298850574713,
      },
      {
        to: 'e',
        cost: 0.503448275862069,
      },
      {
        to: '8a',
        cost: 0.5666666666666667,
      },
      {
        to: 'e8',
        cost: 0.621264367816092,
      },
      {
        to: 'a2',
        cost: 0.767816091954023,
      },
      {
        to: 'f8',
        cost: 0.7706896551724138,
      },
      {
        to: 'f',
        cost: 0.8454022988505747,
      },
      {
        to: 'h8',
        cost: 0.8482758620689655,
      },
      {
        to: '8e',
        cost: 0.8798850574712644,
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
        cost: 0.5206896551724138,
      },
      {
        to: 'Pe',
        cost: 0.6413793103448276,
      },
      {
        to: 'Pf',
        cost: 0.664367816091954,
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
        cost: 0.36379310344827587,
      },
      {
        to: 'K',
        cost: 0.5063218390804598,
      },
      {
        to: 'Na',
        cost: 0.5896551724137931,
      },
      {
        to: 'Nd',
        cost: 0.6787356321839081,
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
        cost: 0.4298850574712644,
      },
      {
        to: 'Bf',
        cost: 0.6327586206896552,
      },
      {
        to: 'Ba',
        cost: 0.6557471264367816,
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
        cost: 0.38103448275862073,
      },
      {
        to: 'Ra',
        cost: 0.5465517241379311,
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
        cost: 0.5695402298850575,
      },
      {
        to: '8Q',
        cost: 0.5724137931034483,
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
        cost: 0.29482758620689653,
      },
      {
        to: 'Ka',
        cost: 0.49770114942528737,
      },
      {
        to: 'Kf',
        cost: 0.5293103448275862,
      },
      {
        to: 'Ke',
        cost: 0.532183908045977,
      },
      {
        to: 'N',
        cost: 0.5752873563218391,
      },
    ],
  },
  {
    in: 'castle',
    val: 'O-O',
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
    tok: 'r!',
  },
  {
    in: 'king side castle',
    val: 'O-O',
    tags: ['move', 'exact'],
    tok: 'Ks!',
  },
  {
    in: 'castle king side',
    val: 'O-O',
    tags: ['move', 'exact'],
    tok: '!Ks',
  },
  {
    in: 'long castle',
    val: 'O-O-O',
    tags: ['move', 'exact'],
    tok: 't!',
  },
  {
    in: 'castle queen side',
    val: 'O-O-O',
    tags: ['move', 'exact'],
    tok: '!Qs',
  },
  {
    in: 'queen side castle',
    val: 'O-O-O',
    tags: ['move', 'exact'],
    tok: 'Qs!',
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
        cost: 0.4632183908045977,
      },
      {
        to: 'h',
        cost: 0.5551724137931034,
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
        cost: 0.517816091954023,
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
        cost: 0.46034482758620693,
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
    tok: 'u.',
  },
  {
    in: 'accept draw',
    val: 'draw',
    tags: ['command', 'rounds', 'exact'],
    tok: 'v.',
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
    in: 'up',
    val: 'upv',
    tags: ['command', 'puzzle', 'exact'],
    tok: '<',
    subs: [
      {
        to: '',
        cost: 0.5,
      },
    ],
  },
  {
    in: 'down',
    val: 'downv',
    tags: ['command', 'puzzle', 'exact'],
    tok: '=',
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
    tok: '>',
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
    tok: '?',
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
    tok: '@',
    subs: [
      {
        to: '',
        cost: 0.22011494252873565,
      },
      {
        to: 'e',
        cost: 0.41436781609195406,
      },
      {
        to: 'e8',
        cost: 0.43735632183908046,
      },
    ],
  },
  {
    in: 'confirm',
    val: 'yes',
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
    in: 'no',
    val: 'no',
    tags: ['choice', 'exact'],
    tok: 'C',
    subs: [
      {
        to: '',
        cost: 0.5,
      },
      {
        to: 'N',
        cost: 0.4114942528735632,
      },
    ],
  },
  {
    in: 'clear',
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
    in: 'close',
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
    in: 'cancel',
    val: 'no',
    tags: ['choice', 'exact'],
    tok: 'F',
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
    tok: 'G',
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
    tok: 'H',
    subs: [
      {
        to: '',
        cost: 0.5,
      },
      {
        to: '2',
        cost: 0.40862068965517245,
      },
      {
        to: 'b',
        cost: 0.42298850574712643,
      },
    ],
  },
  {
    in: 'green',
    val: 'green',
    tags: ['choice', 'exact'],
    tok: 'I',
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
        cost: 0.41724137931034483,
      },
    ],
  },
  {
    in: 'yellow',
    val: 'yellow',
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
    in: 'red',
    val: 'red',
    tags: ['choice', 'exact'],
    tok: 'L',
    subs: [
      {
        to: '',
        cost: 0.5,
      },
      {
        to: '8',
        cost: 0.42586206896551726,
      },
    ],
  },
  {
    in: 'pink',
    val: 'pink',
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
    in: 'purple',
    val: 'purple',
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
    in: 'orange',
    val: 'orange',
    tags: ['choice', 'exact'],
    tok: 'S',
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
    tok: 'T',
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
    tok: 'U',
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
    tok: 'V',
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
    tok: 'W',
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
    tok: 'X',
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
    tok: 'Y',
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
    tok: 'Z',
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
    tok: '[',
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
    tok: '\\',
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
    tok: ']',
    subs: [
      {
        to: '',
        cost: 0,
      },
      {
        to: 'N',
        cost: 0.43160919540229886,
      },
      {
        to: 'b',
        cost: 0.48333333333333334,
      },
    ],
  },
  {
    in: 'check',
    val: '',
    tags: ['move', 'ignore'],
    tok: '^',
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
    tok: '_',
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
    tok: '`',
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
    tok: 'i',
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
    tok: 'j',
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
    tok: 'k',
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
    tok: 'l',
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
    tok: 'm',
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
    tok: 'n',
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
    tok: 'o',
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
    tok: 'p',
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
    tok: 'q',
    subs: [
      {
        to: '',
        cost: 0,
      },
    ],
  },
  {
    in: 'short',
    tok: 'r',
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
    in: 'long',
    tok: 't',
    tags: ['part'],
    subs: [
      {
        to: '',
        cost: 0.5,
      },
      {
        to: '1',
        cost: 0.40287356321839085,
      },
      {
        to: 'P',
        cost: 0.4057471264367816,
      },
    ],
  },
  {
    in: 'offer',
    tok: 'u',
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
    tok: 'v',
    tags: ['part'],
    subs: [
      {
        to: '',
        cost: 0.5,
      },
    ],
  },
];
