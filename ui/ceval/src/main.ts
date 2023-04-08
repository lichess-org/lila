import CevalCtrl from './ctrl';
import * as view from './view';
import * as winningChances from './winningChances';

export type { NodeEvals, Eval, EvalMeta, CevalOpts } from './types';
export type { ExternalEngine } from './worker';
export { isEvalBetter, renderEval, sanIrreversible } from './util';
export { CevalCtrl, view, winningChances };

// stop when another tab starts. Listen only once here,
// as the ctrl can be instantiated several times.
// gotta do the click on the toggle to have it visually change.
lichess.storage.make('ceval.disable').listen(() => {
  const toggle = document.getElementById('analyse-toggle-ceval') as HTMLInputElement | undefined;
  if (toggle?.checked) toggle.click();
});
