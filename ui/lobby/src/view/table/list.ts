import { bind } from 'common/snabbdom';
import { getPerfIcon } from 'common/perfIcons';
import { h } from 'snabbdom';
import LobbyController from '../../ctrl';
import * as hookRepo from '../../hookRepo';
import * as seekRepo from '../../seekRepo';
import { Hook, Seek } from '../../interfaces';
import { tds } from '../util';
import { capitalize } from 'common/string';
import { action, isHook } from '../../util';

function renderHookOrSeek(ctrl: LobbyController, hs: Hook | Seek) {
  const noarg = ctrl.trans.noarg,
    act = action(hs),
    disabled = isHook(hs) && !!hs.disabled,
    username = isHook(hs) ? hs.u : hs.username,
    isRated = isHook(hs) ? hs.ra : hs.mode === 1;
  return h(
    'tr.hook.' + act,
    {
      key: hs.id,
      class: { disabled },
      attrs: {
        title: disabled
          ? ''
          : act === 'join'
            ? noarg('joinTheGame') + ' | ' + capitalize(noarg((hs.perf || hs.variant) as I18nKey))
            : noarg('cancel'),
        'data-id': hs.id,
      },
    },
    tds([
      h('span.is.is2.color-icon.' + ((isHook(hs) ? hs.c : hs.color) || 'random')),
      hs.rating
        ? h(
            'span.ulink.ulpt',
            {
              attrs: { 'data-href': '/@/' + username },
            },
            username
          )
        : 'Anonymous',
      (hs.rating ? hs.rating : '-') + ((isHook(hs) ? hs.prov : hs.provisional) ? '?' : ''),
      isHook(hs) ? hs.clock : hs.days ? ctrl.trans.plural('nbDays', hs.days) : '∞',
      h(
        'span',
        {
          attrs: { 'data-icon': getPerfIcon(hs.perf || hs.variant || 'standard') },
        },
        noarg(isRated ? 'rated' : 'casual')
      ),
    ])
  );
}

function isStandard(value: boolean) {
  return function (hs: Hook | Seek) {
    return !hs.variant === value;
  };
}

function isMine(hs: Hook | Seek) {
  return action(hs) === 'cancel';
}

function isNotMine(hs: Hook | Seek) {
  return !isMine(hs);
}

export function toggle(ctrl: LobbyController) {
  return h('i.toggle', {
    key: 'set-mode-chart',
    attrs: { title: ctrl.trans.noarg('graph'), 'data-icon': '9' },
    hook: bind('mousedown', _ => ctrl.setMode('chart'), ctrl.redraw),
  });
}

export function render(tab: 'seeks' | 'real_time', ctrl: LobbyController, allHs: Seek[] | Hook[]) {
  const mine = allHs.filter(isMine),
    render = (hs: Hook | Seek) => renderHookOrSeek(ctrl, hs),
    standards = allHs.filter(isNotMine).filter(isStandard(true)),
    variants = allHs.filter(isNotMine).filter(isStandard(false));

  if (tab === 'seeks') {
    seekRepo.sort(ctrl, mine as Seek[]);
    seekRepo.sort(ctrl, standards as Seek[]);
    seekRepo.sort(ctrl, variants as Seek[]);
  } else {
    hookRepo.sort(ctrl, mine as Hook[]);
    hookRepo.sort(ctrl, standards as Hook[]);
    hookRepo.sort(ctrl, variants as Hook[]);
  }

  const renderedHss = [
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
  if (mine) renderedHss.unshift(...mine.map(m => render(m)));
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
              sort: ctrl.sort.startsWith('rating'),
              reverse: ctrl.sort === 'rating-reverse',
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
              sort: ctrl.sort.startsWith('time'),
              reverse: ctrl.sort === 'time-reverse',
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
        class: { stepping: ctrl.tab === 'real_time' && ctrl.stepping },
        hook: bind(
          'click',
          e => {
            let el = e.target as HTMLElement;
            do {
              el = el.parentNode as HTMLElement;
              if (el.nodeName === 'TR') {
                const elId = el.getAttribute('data-id')!;
                if (ctrl.tab === 'seeks') return ctrl.clickSeek(elId);
                return ctrl.clickHook(elId);
              }
            } while (el.nodeName !== 'TABLE');
          },
          ctrl.redraw
        ),
      },
      renderedHss
    ),
  ]);
}
