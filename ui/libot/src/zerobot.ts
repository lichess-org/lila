import { type Zerofish, type PV } from 'zerofish';
import * as Chops from 'chessops';
import { Libot, BotInfo, ZeroBotConfig } from './interfaces';

let ordinal = 0;

export class ZeroBot implements Libot {
  readonly name: string;
  readonly uid: string;
  readonly description: string;
  readonly image: string;
  readonly zfcfg: ZeroBotConfig;
  readonly netName?: string;
  ratings = new Map();
  ordinal: number;
  zf: Zerofish;

  get imageUrl() {
    return lichess.assetUrl(`lifat/bots/images/${this.image}`, { noVersion: true });
  }

  constructor(info: BotInfo, zf: Zerofish) {
    const infoCfg = info.zbcfg;
    Object.assign(this, info);
    this.zfcfg = infoCfg ? Object.assign({}, defaultCfg, infoCfg) : defaultCfg;
    this.zf = zf;
    this.ordinal = ordinal++;
  }

  weigh(material: Chops.Material) {
    let score = 0;
    for (const [role, price] of Object.entries(prices) as [Chops.Role, number][]) {
      score += price * material.count(role);
    }
    return score;
  }

  async move(fen: string) {
    const zeroMove = this.zfcfg.fishMix > 0 ? this.zf.goZero(fen) : Promise.resolve(undefined);
    const fishMove =
      this.zfcfg.fishMix < 1
        ? this.zf.goFish(fen, {
            depth: this.zfcfg.searchDepth,
            ms: !this.zfcfg.searchDepth ? this.zfcfg.searchMs : undefined,
            pvs: this.zfcfg.searchWidth,
          })
        : Promise.resolve([]);
    const [zero, fishPvs] = await Promise.all([zeroMove, fishMove]);
    const chess = Chops.Chess.fromSetup(Chops.fen.parseFen(fen).unwrap()).unwrap();
    const aggression: [number, PV][] = [];
    //const before = this.weigh(Chops.Material.fromBoard(chess.board));
    for (const pv of fishPvs) {
      const pvChess = chess.clone();
      for (const move of pv.moves) pvChess.play(Chops.parseUci(move)!);
      const after = this.weigh(Chops.Material.fromBoard(pvChess.board));
      aggression.push([after, pv]);
    }
    const [low, high] = fishPvs.reduce(
      ([low, high], pv) => {
        const score = pv.score;
        if (score < low) low = score;
        if (score > high) high = score;
        return [low, high];
      },
      [Number.MAX_SAFE_INTEGER, Number.MIN_SAFE_INTEGER],
    );
    aggression.sort((a, b) => b[0] - a[0]);
    const zeroIndex = aggression.findIndex(([_, pv]) => pv.moves[0] === zero);
    const zeroScore = zeroIndex >= 0 ? aggression[zeroIndex]?.[0] : -0.1;
    return zero ?? '';
  }
}

const dimensions = {
  aggression: 0,
  zeroFit: 1,
  survival: 2,
  threshhold: 3,
};

const defaultCfg: ZeroBotConfig = {
  fishMix: 0, // [0 zero, 1 fish]
  cpBias: 0,
  cpThreshold: 0.4,
  searchMs: 800,
  searchWidth: 8, // multiPV
  aggression: 0.5, // [0 passive, 1 aggressive]
};

const prices: { [role in Chops.Role]?: number } = {
  pawn: 1,
  knight: 2.8,
  bishop: 3,
  rook: 5,
  queen: 9,
};

function sq2key(sq: number): Key {
  return Chops.makeSquare(sq);
}

function splitUci(uci: Uci): { from: Key; to: Key; role?: Chops.Role } {
  return { from: uci.slice(0, 2) as Key, to: uci.slice(2, 4) as Key, role: Chops.charToRole(uci.slice(4)) };
}

function linesWithin(move: string, lines: PV[], bias = 0, threshold = 50) {
  const zeroScore = lines.find(line => line.moves[0] === move)?.score ?? Number.NaN;
  return lines.filter(fish => Math.abs(fish.score - bias - zeroScore) < threshold && fish.moves.length);
}

function randomSprinkle(move: string, lines: PV[]) {
  lines = linesWithin(move, lines, 0, 20);
  if (!lines.length) return move;
  return lines[Math.floor(Math.random() * lines.length)].moves[0] ?? move;
}

/*
function occurs(chance: number) {
  return Math.random() < chance;
}*/
