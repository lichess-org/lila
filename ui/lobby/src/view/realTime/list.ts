import perfIcons from 'lib/game/perfIcons';
import { licon } from 'lib/licon';
import { bind, dataIcon, tr, span, td, button, th, thead, tbody, icon, table } from 'lib/view';

import type LobbyController from '@/ctrl';
import * as hookRepo from '@/hookRepo';
import type { Hook } from '@/interfaces';

import { perfNames } from '../util';

function renderHook(ctrl: LobbyController, hook: Hook) {
  return tr(
    `.hook.${hook.action}`,
    {
      key: hook.id,
      class: { disabled: !!hook.disabled },
      role: 'button',
      title: hook.disabled
        ? ''
        : hook.action === 'join'
          ? i18n.site.joinTheGame + ' | ' + perfNames[hook.perf]
          : i18n.site.cancel,
      'data-id': hook.id,
    },
    [
      td(
        ctrl.me
          ? span('.ulink.ulpt.mobile-powertip', { 'data-href': '/@/' + hook.u }, hook.u)
          : i18n.site.anonymous,
      ),
      !ctrl.me ? null : td(!ctrl.opts.showRatings ? '' : [hook.rating + (hook.prov ? '?' : '')]),
      td(hook.clock),
      td(span({ ...dataIcon(perfIcons[hook.perf]) }, i18n.site[hook.ra ? 'rated' : 'casual'])),
    ],
  );
}

const isStandard = (value: boolean) => (hook: Hook) => (hook.variant === 'standard') === value;

const isMine = (hook: Hook) => hook.action === 'cancel';

const isNotMine = (hook: Hook) => !isMine(hook);

export const toggle = (ctrl: LobbyController) =>
  button('.toggle', {
    key: 'set-mode-chart',
    title: i18n.site.graph,
    ...dataIcon(licon.LineGraph),
    hook: bind('click', _ => ctrl.setMode('chart'), ctrl.redraw),
  });

export const render = (ctrl: LobbyController, allHooks: Hook[]) => {
  const mine = allHooks.find(isMine);
  const max = mine ? 13 : 14;
  const hooks = allHooks.slice(0, max);
  const render = (hook: Hook) => renderHook(ctrl, hook);
  const standards = hooks.filter(isNotMine).filter(isStandard(true));
  hookRepo.sort(ctrl, standards);

  const variants = hooks
    .filter(isNotMine)
    .filter(isStandard(false))
    .slice(0, Math.max(0, max - standards.length - 1));
  hookRepo.sort(ctrl, variants);

  const renderedHooks = [
    ...standards.map(render),
    variants.length
      ? tr('.variants', { key: 'variants' }, td({ attrs: { colspan: 5 } }, '— ' + i18n.site.variant + ' —'))
      : null,
    ...variants.map(render),
  ];

  if (mine) renderedHooks.unshift(render(mine));

  return table('.hooks__list', [
    thead(
      tr([
        th(),
        ctrl.me
          ? th(
              {
                class: { sortable: true, sort: ctrl.sort === 'rating' },
                hook: bind('click', _ => ctrl.setSort('rating'), ctrl.redraw),
              },
              [icon(licon.DownTriangle)('.is'), i18n.site.rating],
            )
          : null,
        th(
          ctrl.me
            ? {
                key: 'time-header-with-rating',
                class: { sortable: true, sort: ctrl.sort === 'time' },
                hook: bind('click', _ => ctrl.setSort('time'), ctrl.redraw),
              }
            : {
                key: 'time-header-without-rating',
              },
          [icon(licon.DownTriangle)('.is'), i18n.site.time],
        ),
        th(i18n.site.mode),
      ]),
    ),
    tbody(
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
