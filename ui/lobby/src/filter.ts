import { Hook } from './interfaces';
import LobbyController from './ctrl';

interface Filtered {
  visible: Hook[];
  hidden: number;
}

export default function(ctrl: LobbyController, hooks: Hook[]): Filtered {
  const f = ctrl.data.filter,
    seen: string[] = [],
    visible: Hook[] = [];
  let variant: string, hidden = 0;
  hooks.forEach(function(hook) {
    variant = hook.variant;
    if (hook.action === 'cancel') visible.push(hook);
    else {
      if (!f.variant.includes(variant) ||
        !f.mode.includes(hook.ra || 0) ||
        !f.speed.includes(hook.s || 1 /* ultrabullet = bullet */) ||
        (f.rating && (!hook.rating || (hook.rating < f.rating[0] || hook.rating > f.rating[1])))) {
        hidden++;
      } else {
        const hash = hook.ra + variant + hook.t + hook.rating;
        if (!seen.includes(hash)) visible.push(hook);
        seen.push(hash);
      }
    }
  });
  return {
    visible: visible,
    hidden: hidden
  };
}
