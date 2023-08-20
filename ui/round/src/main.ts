import { attributesModule, classModule, init } from 'snabbdom';
import boot from './boot';
import menuHover from 'common/menuHover';
import RoundController from './ctrl';
import { main as view } from './view/main';
import { RoundOpts, NvuiPlugin } from './interfaces';

const patch = init([classModule, attributesModule]);

export function initModule(opts: RoundOpts) {
  boot(opts, app);
}

function app(opts: RoundOpts, nvui?: NvuiPlugin) {
  const ctrl = new RoundController(opts, redraw, nvui);

  const blueprint = view(ctrl);
  opts.element.innerHTML = '';
  let vnode = patch(opts.element, blueprint);

  function redraw() {
    vnode = patch(vnode, view(ctrl));
  }

  window.addEventListener('resize', redraw); // col1 / col2+ transition

  if (ctrl.isPlaying()) menuHover();

  lichess.sound.preloadBoardSounds();

  return {
    socketReceive: ctrl.socket.receive,
    moveOn: ctrl.moveOn,
  };
}
