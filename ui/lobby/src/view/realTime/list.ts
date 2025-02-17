import { h } from 'snabbdom';
import type LobbyController from '../../ctrl';
import * as licon from 'common/licon';
import { bind } from 'common/snabbdom';
import { tds, perfNames } from '../util';
import perfIcons from 'common/perfIcons';
import * as hookRepo from '../../hookRepo';
import type { Hook } from '../../interfaces';

function renderHook(ctrl: LobbyController, hook: Hook) {
  return h(
    'tr.hook.' + hook.action,
    {
      key: hook.id,
      class: { disabled: !!hook.disabled },
      attrs: {
        role: 'button',
        title: hook.disabled
          ? ''
          : hook.action === 'join'
            ? i18n.site.joinTheGame + ' | ' + perfNames[hook.perf]
            : i18n.site.cancel,
        'data-id': hook.id,
      },
    },
    tds([
      hook.rating
        ? h('span.ulink.ulpt.mobile-powertip', { attrs: { 'data-href': '/@/' + hook.u } }, hook.u)
        : i18n.site.anonymous,
      hook.rating && ctrl.opts.showRatings ? hook.rating + (hook.prov ? '?' : '') : '',
      hook.clock,
      h('span', { attrs: { 'data-icon': perfIcons[hook.perf] } }, i18n.site[hook.ra ? 'rated' : 'casual']),
    ]),
  );
}

const isStandard = (value: boolean) => (hook: Hook) => (hook.variant === 'standard') === value;

const isMine = (hook: Hook) => hook.action === 'cancel';

const isNotMine = (hook: Hook) => !isMine(hook);

export const toggle = (ctrl: LobbyController) =>
  h('i.toggle', {
    key: 'set-mode-chart',
    attrs: { title: i18n.site.graph, 'data-icon': licon.LineGraph },
    hook: bind('mousedown', _ => ctrl.setMode('chart'), ctrl.redraw),
  });

export const render = (ctrl: LobbyController, allHooks: Hook[]) => {
  const mine = allHooks.find(isMine),
    max = mine ? 13 : 14,
    hooks = allHooks.slice(0, max),
    render = (hook: Hook) => renderHook(ctrl, hook),
    standards = hooks.filter(isNotMine).filter(isStandard(true));
  hookRepo.sort(ctrl, standards);
  const variants = hooks
    .filter(isNotMine)
    .filter(isStandard(false))
    .slice(0, Math.max(0, max - standards.length - 1));
  hookRepo.sort(ctrl, variants);
  const renderedHooks = [
    ...standards.map(render),
    variants.length
      ? h('tr.variants', { key: 'variants' }, [
          h('td', { attrs: { colspan: 5 } }, '— ' + i18n.site.variant + ' —'),
        ])
      : null,
    ...variants.map(render),
  ];
  if (mine) renderedHooks.unshift(render(mine));
  return h('table.hooks__list', [
    h(
      'thead',
      h('tr', [
        h('th'),
        h(
          'th',
          {
            class: { sortable: true, sort: ctrl.sort === 'rating' },
            hook: bind('click', _ => ctrl.setSort('rating'), ctrl.redraw),
          },
          [h('i.is'), i18n.site.rating],
        ),
        h(
          'th',
          {
            class: { sortable: true, sort: ctrl.sort === 'time' },
            hook: bind('click', _ => ctrl.setSort('time'), ctrl.redraw),
          },
          [h('i.is'), i18n.site.time],
        ),
        h('th', [h('i.is'), i18n.site.mode]),
      ]),
    ),
    h(
      'tbody',
      {
        class: { stepping: ctrl.stepping },
        hook: bind(
          'click',
          async e => {
            let el = e.target as HTMLElement;
            do {
              el = el.parentNode as HTMLElement;
              if (el.nodeName === 'TR') return ctrl.clickHook(el.dataset['id']!);
            } while (el.nodeName !== 'TABLE');
          },
          ctrl.redraw,
        ),
      },
      renderedHooks,
    ),
  ]);
};
