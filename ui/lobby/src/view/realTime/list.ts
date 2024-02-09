import { bind } from 'common/snabbdom';
import { getPerfIcon } from 'common/perfIcons';
import { h } from 'snabbdom';
import LobbyController from '../../ctrl';
import * as hookRepo from '../../hookRepo';
import { Hook } from '../../interfaces';
import { tds } from '../util';
import { capitalize } from 'common/string';

function renderHook(ctrl: LobbyController, hook: Hook) {
  const noarg = ctrl.trans.noarg,
    hookAction = hookRepo.action(hook);
  return h(
    'tr.hook.' + hookAction,
    {
      key: hook.id,
      class: { disabled: !!hook.disabled },
      attrs: {
        title: hook.disabled
          ? ''
          : hookAction === 'join'
            ? noarg('joinTheGame') + ' | ' + capitalize(noarg((hook.perf || hook.variant) as I18nKey))
            : noarg('cancel'),
        'data-id': hook.id,
      },
    },
    tds([
      h('span.is.is2.color-icon.' + (hook.c || 'random')),
      hook.rating
        ? h(
            'span.ulink.ulpt',
            {
              attrs: { 'data-href': '/@/' + hook.u },
            },
            hook.u
          )
        : 'Anonymous',
      (hook.rating ? hook.rating : '') + (hook.prov ? '?' : ''),
      hook.clock,
      h(
        'span',
        {
          attrs: { 'data-icon': getPerfIcon(hook.perf || hook.variant || 'standard') },
        },
        noarg(hook.ra ? 'rated' : 'casual')
      ),
    ])
  );
}

function isStandard(value: boolean) {
  return function (hook: Hook) {
    return !hook.variant === value;
  };
}

function isMine(hook: Hook) {
  return hookRepo.action(hook) === 'cancel';
}

function isNotMine(hook: Hook) {
  return !isMine(hook);
}

export function toggle(ctrl: LobbyController) {
  return h('i.toggle', {
    key: 'set-mode-chart',
    attrs: { title: ctrl.trans.noarg('graph'), 'data-icon': '9' },
    hook: bind('mousedown', _ => ctrl.setMode('chart'), ctrl.redraw),
  });
}

export function render(ctrl: LobbyController, allHooks: Hook[]) {
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
      ? h(
          'tr.variants',
          {
            key: 'variants',
          },
          [
            h(
              'td',
              {
                attrs: { colspan: 5 },
              },
              '— ' + ctrl.trans('variant') + ' —'
            ),
          ]
        )
      : null,
    ...variants.map(render),
  ];
  if (mine) renderedHooks.unshift(render(mine));
  return h('table.hooks__list', [
    h(
      'thead',
      h('tr', [
        h('th'),
        h('th', ctrl.trans('player')),
        h(
          'th',
          {
            class: {
              sortable: true,
              sort: ctrl.sort === 'rating',
            },
            hook: bind('click', _ => ctrl.setSort('rating'), ctrl.redraw),
          },
          [h('i.is'), ctrl.trans('rating')]
        ),
        h(
          'th',
          {
            class: {
              sortable: true,
              sort: ctrl.sort === 'time',
            },
            hook: bind('click', _ => ctrl.setSort('time'), ctrl.redraw),
          },
          [h('i.is'), ctrl.trans('time')]
        ),
        h('th', ctrl.trans('mode')),
      ])
    ),
    h(
      'tbody',
      {
        class: { stepping: ctrl.stepping },
        hook: bind(
          'click',
          e => {
            let el = e.target as HTMLElement;
            do {
              el = el.parentNode as HTMLElement;
              if (el.nodeName === 'TR') return ctrl.clickHook(el.getAttribute('data-id')!);
            } while (el.nodeName !== 'TABLE');
          },
          ctrl.redraw
        ),
      },
      renderedHooks
    ),
  ]);
}
