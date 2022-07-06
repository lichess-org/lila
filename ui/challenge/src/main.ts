import { init, attributesModule, classModule, VNode } from 'snabbdom';

import makeCtrl from './ctrl';
import { loaded, loading } from './view';
import { load } from './xhr';
import { ChallengeOpts, ChallengeData, Ctrl } from './interfaces';

const patch = init([classModule, attributesModule]);

export default function LishogiChallenge(element: Element, opts: ChallengeOpts) {
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
    load()
      .then(update)
      .fail(() => window.lishogi.announce({ msg: 'Failed to load challenges' }));

  return {
    update,
  };
}
