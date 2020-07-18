import ctrl from './ctrl';
import * as view from './view';
import * as winningChances from './winningChances';

export { CevalCtrl, NodeEvals, Eval, Work, CevalOpts } from './types';
export { isEvalBetter, renderEval, sanIrreversible } from './util';
export { ctrl, view, winningChances };

// stop when another tab starts. Listen only once here,
// as the ctrl can be instanciated several times.
// gotta do the click on the toggle to have it visually change.
window.lichess.storage.make('ceval.pool.start').listen(_ => {
  const toggle = document.getElementById('analyse-toggle-ceval');
  if (toggle && (toggle as HTMLInputElement).checked) toggle.click();
});
