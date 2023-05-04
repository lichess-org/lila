import CevalCtrl from './ctrl';
import * as view from './view';
import * as winningChances from './winningChances';

export type { NodeEvals, Eval, EvalMeta, CevalOpts } from './types';
export type { ExternalEngine } from './worker';
export { isEvalBetter, renderEval, sanIrreversible } from './util';
export { CevalCtrl, view, winningChances };

// stop when another tab starts. Listen only once here,
// as the ctrl can be instantiated several times.
lichess.storage.make('ceval.disable').listen(() => {
  for (const select of document.getElementsByClassName('engine-choice')) {
    (select as HTMLSelectElement).value = 'disabled';
    select.dispatchEvent(new CustomEvent('change'));
  }
});
