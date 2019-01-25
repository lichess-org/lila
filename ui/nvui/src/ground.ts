import { start, Api } from 'chessground/api';
import { configure, Config } from 'chessground/config';
import { defaults, State } from 'chessground/state';

export function makeChessground(config: Config): Api {
  const state = defaults() as State;
  configure(state, config);
  return start(state, () => {});
}
