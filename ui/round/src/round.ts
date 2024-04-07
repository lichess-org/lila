import { RoundData, Step, RoundOpts, NvuiPlugin } from './interfaces';
import { attributesModule, classModule, init } from 'snabbdom';
import boot from './boot';
import menuHover from 'common/menuHover';
import RoundController from './ctrl';
import { main as view } from './view/main';

const patch = init([classModule, attributesModule]);

export function initModule(opts: RoundOpts) {
  boot(opts, app);
}

export const firstPly = (d: RoundData): number => d.steps[0].ply;

export const lastPly = (d: RoundData): number => lastStep(d).ply;

export const lastStep = (d: RoundData): Step => d.steps[d.steps.length - 1];

export const plyStep = (d: RoundData, ply: number): Step => d.steps[ply - firstPly(d)];

export const massage = (d: RoundData): void => {
  if (d.clock) {
    d.clock.showTenths = d.pref.clockTenths;
    d.clock.showBar = d.pref.clockBar;
  }

  if (d.correspondence) d.correspondence.showBar = d.pref.clockBar;

  if (['horde', 'crazyhouse'].includes(d.game.variant.key)) d.pref.showCaptured = false;

  if (d.expiration) d.expiration.movedAt = Date.now() - d.expiration.idleMillis;
};

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

  site.sound.preloadBoardSounds();

  return {
    socketReceive: ctrl.socket.receive,
    moveOn: ctrl.moveOn,
  };
}
