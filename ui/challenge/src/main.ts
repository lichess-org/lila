import { init, VNode, classModule, attributesModule } from 'snabbdom';
import Ctrl from './ctrl';
import { loaded, loading } from './view';
import { json } from 'common/xhr';
import { ChallengeOpts, ChallengeData } from './interfaces';

const patch = init([classModule, attributesModule]);

export function initModule(opts: ChallengeOpts) {
  let vnode: VNode, ctrl: Ctrl;

  function redraw() {
    vnode = patch(vnode || opts.el, ctrl ? loaded(ctrl) : loading());
  }

  function update(d: ChallengeData) {
    if (ctrl) ctrl.update(d);
    else {
      ctrl = new Ctrl(opts, d, redraw);
      opts.el.innerHTML = '';
    }
    redraw();
  }

  if (opts.data) update(opts.data);
  else json('/challenge').then(update, _ => lichess.announce({ msg: 'Failed to load challenges' }));

  return {
    update,
  };
}
