import { Hook } from './interfaces';
import LobbyController from './ctrl';

interface Filtered {
  visible: Hook[];
  hidden: number;
}

export default function(ctrl: LobbyController, hooks: Hook[]): Filtered {
  const f = ctrl.data.filter,
  seen: string[] = [],
  visible: Hook[] = [],
  contains = window.lidraughts.fp.contains;
  let variant: string, hidden = 0;
  hooks.forEach(function(hook) {
    variant = hook.variant;
    if (hook.action === 'cancel') visible.push(hook);
    else {
      if (!contains(f.variant, variant) ||
        !contains(f.mode, hook.ra || 0) ||
        !contains(f.speed, hook.s || 1 /* ultrabullet = bullet */) ||
        (f.rating && (!hook.rating || (hook.rating < f.rating[0] || hook.rating > f.rating[1])))) {
        hidden++;
      } else {
        var hash = hook.ra + variant + hook.t + hook.rating;
        if (!contains(seen, hash)) visible.push(hook);
        seen.push(hash);
      }
    }
  });
  return {
    visible: visible,
    hidden: hidden
  };
}
