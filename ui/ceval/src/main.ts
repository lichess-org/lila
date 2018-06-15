import ctrl from './ctrl';
import * as view from './view';
import * as winningChances from './winningChances';
import pv2san from './pv2san';

export { CevalCtrl, NodeEvals, Eval, Work, CevalOpts } from './types';

export { ctrl, view, winningChances, pv2san };

export function isEvalBetter(a: Tree.ClientEval, b?: Tree.ClientEval): boolean {
  return !b || a.depth > b.depth || (a.depth === b.depth && a.nodes > b.nodes);
}

// stop when another tab starts. Listen only once here,
// as the ctrl can be instanciated several times.
// gotta do the click on the toggle to have it visually change.
window.lichess.storage.make('ceval.pool.start').listen(() => {
  const toggle = document.getElementById('analyse-toggle-ceval');
  if (toggle && (toggle as HTMLInputElement).checked) {
    console.log('Another tab runs the engine, closing this one.');
    toggle.click();
  }
});
