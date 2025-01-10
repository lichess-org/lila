import { type VNode, attributesModule, classModule, init } from 'snabbdom';
import makeCtrl from '../ctrl';
import type { ChallengeData, ChallengeOpts, Ctrl } from '../interfaces';
import { loaded, loading } from '../view';

const patch = init([classModule, attributesModule]);

function main(opts: ChallengeOpts): {
  update: (d: ChallengeData) => void;
} {
  const element = document.getElementById('challenge-app')!;

  let vnode: VNode, ctrl: Ctrl;

  function redraw() {
    vnode = patch(vnode || element, ctrl ? loaded(ctrl) : loading());
  }

  function update(d: ChallengeData) {
    if (ctrl) ctrl.update(d);
    else {
      ctrl = makeCtrl(opts, d, redraw);
      element.innerHTML = '';
    }
    redraw();
  }

  if (opts.data) update(opts.data);
  else
    window.lishogi.xhr
      .json('GET', '/challenge')
      .then(update, () => window.lishogi.announce({ msg: 'Failed to load challenges' }));

  return {
    update,
  };
}

window.lishogi.registerModule(__bundlename__, main);
