import { Hook, Seek } from './interfaces';

export function isHook(x: Seek | Hook): x is Hook {
  return 'clock' in x;
}

export function action(x: Seek | Hook): 'cancel' | 'join' {
  if (isHook(x)) return x.sri === window.lishogi.sri ? 'cancel' : 'join';
  else return x.username.toLowerCase() === document.body.dataset.user ? 'cancel' : 'join';
}
