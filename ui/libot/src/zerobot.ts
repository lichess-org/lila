import { type Zerofish, type Score } from 'zerofish';
import * as Chops from 'chessops';
import { Libot, BotInfo, ZeroBotConfig } from './interfaces';
import { deepScores, byDestruction, shallowScores, scores } from './behavior';
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

  async move(fen: string) {
    lichess.log("It's the spinner.gif. The spinner.gif is crashing chrome, ok?");
    const zeroMove = this.zfcfg.fishMix < 1 ? this.zf.goZero(fen) : Promise.resolve(undefined);
    const fishMove =
      this.zfcfg.fishMix > 0
        ? this.zf.goFish(fen, {
            depth: this.zfcfg.searchDepth,
            ms: !this.zfcfg.searchDepth ? this.zfcfg.searchMs : undefined,
            pvs: this.zfcfg.searchWidth,
          })
        : Promise.resolve([]);
    const [zero, fishResult] = await Promise.all([zeroMove, fishMove]);
    const aggression = byDestruction(fishResult, fen);
    aggression.sort((a, b) => b[0] - a[0]);
    const zeroIndex = aggression.findIndex(([_, pv]) => pv.moves[0] === zero);
    const zeroDestruction = zeroIndex >= 0 ? aggression[zeroIndex]?.[0] : -0.1;
    console.log(zeroDestruction, aggression, deepScores(fishResult, zero));
    return aggression[0]?.[1]?.moves[0] ?? zero;
  }
}

const dimensions = {
  aggression: 0,
  zeroFit: 1,
  survival: 2,
  threshhold: 3,
};

const defaultCfg: ZeroBotConfig = {
  fishMix: 0.5, // [0 zero, 1 fish]
  cpBias: 0,
  cpThreshold: 0.4,
  searchMs: 800,
  searchWidth: 8, // multiPV
  aggression: 0.5, // [0 passive, 1 aggressive]
};

function sq2key(sq: number): Key {
  return Chops.makeSquare(sq);
}

function splitUci(uci: Uci): { from: Key; to: Key; role?: Chops.Role } {
  return { from: uci.slice(0, 2) as Key, to: uci.slice(2, 4) as Key, role: Chops.charToRole(uci.slice(4)) };
}

function linesWithin(move: string, lines: Score[], bias = 0, threshold = 50) {
  const zeroScore = lines.find(line => line.moves[0] === move)?.score ?? Number.NaN;
  return lines.filter(fish => Math.abs(fish.score - bias - zeroScore) < threshold && fish.moves.length);
}

function randomSprinkle(move: string, lines: Score[]) {
  lines = linesWithin(move, lines, 0, 20);
  if (!lines.length) return move;
  return lines[Math.floor(Math.random() * lines.length)].moves[0] ?? move;
}

/*
function occurs(chance: number) {
  return Math.random() < chance;
}*/
