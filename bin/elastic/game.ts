import type { JsonDoc, IndexSchema, Properties, Mapping, MongoDoc } from './types.ts';

export function getIndexSchema(): IndexSchema {
  return {
    name: 'game',
    esPath: 'game',
    batchSize: 1000,
    source: false,
    settings: {
      number_of_shards: 25,
      number_of_replicas: 0,
      refresh_interval: '300s',
      index: {
        codec: 'best_compression',
        sort: { field: 'd', order: 'desc' },
      },
    },
    properties,
    mapping,
  };
}

const properties: Properties = {
  s: { type: 'keyword', doc_values: false }, // status
  t: { type: 'short' }, // turns
  r: { type: 'boolean', doc_values: false }, // rated
  p: { type: 'keyword', doc_values: false }, // perf
  c: { type: 'keyword', doc_values: false }, // winner color
  a: { type: 'short' }, // average rating
  i: { type: 'byte', doc_values: false }, // AI level
  d: { type: 'date', format: 'epoch_second' }, // date
  l: { type: 'integer', doc_values: false }, // duration
  ct: { type: 'short' }, // clock initial time
  ci: { type: 'short' }, // clock increment
  n: { type: 'boolean', doc_values: false }, // analysed
  wu: { type: 'keyword', doc_values: false }, // white user
  bu: { type: 'keyword', doc_values: false }, // black user
  so: { type: 'keyword', doc_values: false }, // source
  wr: { type: 'short', doc_values: false }, // white rating
  br: { type: 'short', doc_values: false }, // black rating
  c9: { type: 'keyword', doc_values: false }, // Chess960 position
  wb: { type: 'boolean', doc_values: false }, // white bot
  bb: { type: 'boolean', doc_values: false }, // black bot
};

const mapping: Mapping = {
  collection: 'game5',
  projection: {
    _id: 1,
    us: 1, // player user IDs
    wid: 1, // winner user ID
    ca: 1, // created at
    ua: 1, // updated at
    t: 1, // ply
    an: 1, // analysed
    'p0.e': 1, // white rating
    'p0.ai': 1, // white AI level
    'p1.e': 1, // black rating
    'p1.ai': 1, // black AI level
    is: 1, // player IDs search string
    s: 1, // status
    c: 1, // encoded clock
    ra: 1, // rated
    v: 1, // variant
    so: 1, // source
    w: 1, // winner color
    if: 1, // initial FEN
  },
  mongoFilter: args => ({
    ca: { $gte: args.from, ...(args.to ? { $lt: args.to } : {}) },
    s: { $gte: 30 },
    so: { $ne: 7 },
    us: { $exists: true, $ne: [] },
  }),
  mongoSort: { ca: -1 },
  operations: async (docs, { mongo: lichess }) => {
    const playerIds = [
      ...new Set(
        docs.flatMap(game => game.us ?? []).filter((id): id is string => typeof id === 'string' && id !== ''),
      ),
    ];
    const botIds = new Set(
      await lichess
        .collection<MongoDoc>('user4')
        .find({ _id: { $in: playerIds }, title: 'BOT' }, { projection: { _id: 1 } })
        .map(user => user._id)
        .toArray(),
    );
    return {
      toUpsert: docs.map(game => ({ id: game._id, doc: toDoc(game, botIds) })),
      toDelete: [],
    };
  },
};

function toDoc(game: JsonDoc, botIds: Set<string>) {
  const clock = decodeClock(game.c);
  const whiteUser = game.us?.[0] ?? '';
  const blackUser = game.us?.[1] ?? '';
  const whiteRating = game.p0?.e ?? 0;
  const blackRating = game.p1?.e ?? 0;
  const date = game.ua ?? game.ca;
  return {
    s: game.s,
    t: Math.floor(((game.t ?? 0) + 1) / 2),
    r: game.ra ?? false,
    p: perfId(game.v, clock),
    c: winnerColor(game),
    a: whiteRating > 0 && blackRating > 0 ? Math.floor((whiteRating + blackRating) / 2) : 0,
    i: game.p0?.ai ?? game.p1?.ai ?? 0,
    d: Math.floor(date.getTime() / 1000),
    l: durationSeconds(game.ca, date, clock),
    ct: clock?.limit ?? -1,
    ci: clock?.increment ?? -1,
    n: game.an ?? false,
    wu: whiteUser,
    bu: blackUser,
    so: game.so ?? 0,
    wr: whiteRating,
    br: blackRating,
    c9: chess960Position(game.if) ?? 1000,
    wb: whiteUser !== '' && botIds.has(whiteUser),
    bb: blackUser !== '' && botIds.has(blackUser),
  };
}

function decodeClock(value: any) {
  const bytes = value?.buffer ?? value;
  if (!bytes || bytes.length < 2) return undefined;
  const limitByte = bytes[0];
  return { limit: limitByte < 181 ? limitByte * 60 : (limitByte - 180) * 15, increment: bytes[1] };
}

function perfId(variant: number | undefined, clock: { limit: number; increment: number } | undefined) {
  const variantToPerf: Record<number, number> = { 2: 11, 4: 12, 5: 15, 6: 13, 7: 14, 8: 16, 9: 17, 10: 18 };
  if (variant && variantToPerf[variant]) return variantToPerf[variant];
  const estimated = clock ? clock.limit + 40 * clock.increment : undefined;
  if (estimated === undefined) return 4;
  if (estimated < 30) return 0;
  if (estimated < 180) return 1;
  if (estimated < 480) return 2;
  if (estimated < 1500) return 6;
  return 3;
}

function winnerColor(game: JsonDoc) {
  if (game.w === true) return 1;
  if (game.w === false) return 2;
  return game.s > 32 && game.s !== 38 ? 3 : 0;
}

function durationSeconds(
  createdAt: Date,
  date: Date,
  clock: { limit: number; increment: number } | undefined,
) {
  if (!clock) return 0;
  const seconds = Math.floor((date.getTime() - createdAt.getTime()) / 1000);
  if (seconds <= 0) return 0;
  return seconds < 60 * 60 * 12 ? seconds : 60 * 60 * 12 + 1;
}

// not seeing any chess960 position selector in the advanced search UI, but this was in lila-search so
function chess960Position(fen: string | undefined) {
  if (!fen) return undefined;
  const row = fen.split(' ')[0]?.split('/')[0]?.toLowerCase();
  if (row?.length !== 8) return undefined;
  const pieces = [...row];
  const bishopSquares = pieces.flatMap((piece, i) => (piece === 'b' ? [i] : []));
  const evenBishop = bishopSquares.find(i => i % 2 === 0);
  const oddBishop = bishopSquares.find(i => i % 2 === 1);
  if (evenBishop === undefined || oddBishop === undefined) return undefined;
  let remaining = [0, 1, 2, 3, 4, 5, 6, 7].filter(i => i !== evenBishop && i !== oddBishop);
  const queenIndex = remaining.indexOf(pieces.indexOf('q'));
  remaining = remaining.filter(i => pieces[i] !== 'q');
  const knightSlots = remaining.flatMap((square, i) => (pieces[square] === 'n' ? [i] : []));
  const combos = [
    [0, 1],
    [0, 2],
    [0, 3],
    [0, 4],
    [1, 2],
    [1, 3],
    [1, 4],
    [2, 3],
    [2, 4],
    [3, 4],
  ];
  const knightIndex = combos.findIndex(([a, b]) => knightSlots[0] === a && knightSlots[1] === b);
  if (queenIndex < 0 || knightIndex < 0) return undefined;
  return ((knightIndex * 6 + queenIndex) * 4 + evenBishop / 2) * 4 + (oddBishop - 1) / 2;
}
