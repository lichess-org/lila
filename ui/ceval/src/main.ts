import ctrl from './ctrl';
import * as view from './view';
import * as winningChances from './winningChances';

export { CevalCtrl, NodeEvals, Eval, Work, CevalOpts } from './types';
export { isEvalBetter, renderEval, sanIrreversible } from './util';
export { ctrl, view, winningChances };

// stop when another tab starts. Listen only once here,
// as the ctrl can be instantiated several times.
// gotta do the click on the toggle to have it visually change.
lichess.storage.make('ceval.disable').listen(_ => {
  const toggle = document.getElementById('analyse-toggle-ceval') as HTMLInputElement | undefined;
  if (toggle?.checked) toggle.click();
});
