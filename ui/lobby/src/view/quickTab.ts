import { h, VNodeData } from 'snabbdom';
import { spinnerVdom as spinner } from 'common/spinner';
import { bind, LooseVNodes } from 'common/snabbdom';
import LobbyController from '../ctrl';

export function renderQuickTab(ctrl: LobbyController): { body: LooseVNodes; data: VNodeData } {
  const member = ctrl.poolMember;
  return {
    body: ctrl.pools
      .map(pool => {
        const active = member?.id === pool.id,
          transp = !!member && !active;
        return h(
          'div',
          {
            class: { active, transp },
            attrs: { role: 'button', 'data-id': pool.id },
          },
          [
            h('div.clock', `${pool.lim}+${pool.inc}`),
            active && member.range && ctrl.opts.showRatings
              ? h('div.range', member.range.replace('-', '–'))
              : h('div.perf', pool.perf),
            active ? spinner() : null,
          ],
        );
      })
      .concat(
        h(
          'div.open',
          { class: { transp: !!member }, attrs: { role: 'button', 'data-id': 'custom' } },
          ctrl.trans.noarg('custom'),
        ),
      ),
    data: {
      hook: bind(
        'click',
        e => {
          if (ctrl.redirecting) return;
          const id =
            (e.target as HTMLElement).dataset['id'] ||
            ((e.target as HTMLElement).parentNode as HTMLElement).dataset['id'];
          if (id === 'open') ctrl.setupCtrl.openModal('hook');
          else if (id) ctrl.clickPool(id);
        },
        ctrl.redraw,
      ),
    },
  };
}
