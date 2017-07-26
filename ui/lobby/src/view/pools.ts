import { h } from 'snabbdom';
import LobbyController from '../ctrl';
import { bind, spinner } from './util';

function renderRange(range: string) {
  return h('div.range', range.replace('-', ' - '));
}

export default function(ctrl: LobbyController) {
  return ctrl.data.pools.map(pool => {
    const isMember = pool.id in ctrl.poolMembers,
    range = ctrl.poolMembers[pool.id];
    return h('div.pool', {
      class: { active: isMember },
      hook: bind('click', _ => ctrl.clickPool(pool.id), ctrl.redraw)
    }, [
      h('div.clock', pool.lim + '+' + pool.inc),
      range ? renderRange(range) : h('div.perf', pool.perf),
      isMember ? spinner() : null
    ]);
  }).concat(
    h('div.custom', {
      hook: bind('click', _ => $('#start_buttons .config_hook').mousedown())
    }, ctrl.trans.noarg('custom'))
  );
}
