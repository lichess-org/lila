import { h } from 'snabbdom';
import LobbyController from '../ctrl';
import { bind, spinner } from './util';

function renderRange(range: string) {
  return h('div.range', range.replace('-', '–'));
}

export default function(ctrl: LobbyController) {
  const member = ctrl.poolMember;
  return ctrl.pools.map(function(pool) {
    const active = !!member && member.id === pool.id,
    transp = !!member && !active;
    return h('div.pool', {
      class: {
        active,
        transp: !active && transp
      },
      hook: bind('click', _ => ctrl.clickPool(pool.id), ctrl.redraw)
    }, [
      h('div.clock', pool.lim + '+' + pool.inc),
      (active && member!.range) ? renderRange(member!.range!) : h('div.perf', pool.perf),
      active ? spinner() : null
    ]);
  }).concat(
    h('div.custom', {
      class: { transp: !!member },
      hook: bind('click', _ => $('#start_buttons .config_hook').mousedown())
    }, ctrl.trans.noarg('custom'))
  );
}
