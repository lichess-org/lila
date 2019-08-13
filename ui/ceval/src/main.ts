import ctrl from './ctrl';
import * as view from './view';
import * as winningChances from './winningChances';
import pv2san from './pv2san';

export { CevalCtrl, NodeEvals, Eval, Work, CevalOpts } from './types';

export { ctrl, view, winningChances, pv2san };

export function isEvalBetter(a: Tree.ClientEval, b?: Tree.ClientEval): boolean {
  return !b || a.depth > b.depth || (a.depth === b.depth && a.nodes > b.nodes);
}

export function scan2uci(san: string): string {
  if (!san)
    return san;
  else if (san.indexOf('x') !== -1)
    return san.split('x').map(m => (m.length == 1 ? "0" + m : m)).join('');
  else if (san.indexOf('-') !== -1)
    return san.split('-').map(m => (m.length == 1 ? "0" + m : m)).join('');
  else
    return san;
}

// stop when another tab starts. Listen only once here,
// as the ctrl can be instanciated several times.
// gotta do the click on the toggle to have it visually change.
window.lidraughts.storage.make('ceval.pool.start').listen(() => {
  const toggle = document.getElementById('analyse-toggle-ceval');
  if (toggle && (toggle as HTMLInputElement).checked) toggle.click();
});
