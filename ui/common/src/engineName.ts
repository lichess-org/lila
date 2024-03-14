import { Rules } from 'shogiops/types';
import { isHandicap } from 'shogiops/handicaps';
import { initialSfen, parseSfen } from 'shogiops/sfen';

const useJp = document.documentElement.lang === 'ja-JP';

export const enum EngineCode {
  YaneuraOu = 'yn',
  Fairy = 'fs',
}

// modules/game/src/main/EngineConfig.scala
export function engineCode(rules: Rules, sfen: Sfen | undefined, level?: number): EngineCode {
  return rules === 'standard' &&
    (!level || level > 1) &&
    (!sfen || initialSfen(rules) === sfen || isHandicap({ sfen, rules }) || isStandardMaterial(sfen))
    ? EngineCode.YaneuraOu
    : EngineCode.Fairy;
}

export function engineName(rules: Rules, sfen: Sfen | undefined, level?: number, trans?: Trans): string {
  const code = engineCode(rules, sfen, level);
  return engineNameFromCode(code, level, trans);
}

export function engineNameFromCode(code?: EngineCode, level?: number, trans?: Trans): string {
  const name = code === 'fs' ? 'Fairy Stockfish' : useJp ? 'やねうら王' : 'YaneuraOu';
  if (level && trans) return name + ' - ' + trans('levelX', level);
  else return name;
}

function isStandardMaterial(sfen: Sfen): boolean {
  const pos = parseSfen('standard', sfen);
  if (pos.isErr) return false;
  const board = pos.value.board,
    hands = pos.value.hands.color('sente').combine(pos.value.hands.color('gote'));
  return (
    board.role('pawn').size() + board.role('tokin').size() + hands.get('pawn') <= 18 &&
    board.role('lance').size() + board.role('promotedlance').size() + hands.get('lance') <= 4 &&
    board.role('knight').size() + board.role('promotedknight').size() + hands.get('knight') <= 4 &&
    board.role('silver').size() + board.role('promotedsilver').size() + hands.get('silver') <= 4 &&
    board.role('gold').size() + hands.get('gold') <= 4 &&
    board.role('rook').size() + board.role('dragon').size() + hands.get('rook') <= 2 &&
    board.role('bishop').size() + board.role('horse').size() + hands.get('bishop') <= 2 &&
    board.role('king').size() == 2 &&
    board.role('king').intersect(board.color('sente')).size() === 1
  );
}
