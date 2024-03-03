import { Rules } from 'shogiops';
import { isHandicap } from 'shogiops/handicaps';
import { initialSfen } from 'shogiops/sfen';

const useJp = document.documentElement.lang === 'ja-JP';

// modules/game/src/main/EngineConfig.scala
export function engineName(
  rules: Rules,
  sfen: Sfen | undefined,
  level?: number,
  withLevel?: boolean,
  trans?: Trans
): string {
  const code: EngineCode =
    rules === 'standard' &&
    (!level || level > 1) &&
    (!sfen || initialSfen(rules) === sfen || isHandicap({ sfen, rules }))
      ? 'yn'
      : 'fs';
  return engineNameFromCode(code, withLevel ? level : undefined, trans);
}

export function engineNameFromCode(code?: EngineCode, level?: number, trans?: Trans): string {
  const name = code === 'fs' ? 'Fairy Stockfish' : useJp ? 'やねうら王' : 'YaneuraOu';
  if (level && trans) return name + ' - ' + trans('levelX', level);
  else return name;
}
