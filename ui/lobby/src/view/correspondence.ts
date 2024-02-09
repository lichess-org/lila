import { MaybeVNodes, bind } from 'common/snabbdom';
import { capitalize } from 'common/string';
import { VNode, h } from 'snabbdom';
import LobbyController from '../ctrl';
import { Seek } from '../interfaces';
import { tds } from './util';
import { getPerfIcon } from 'common/perfIcons';
import { action } from '../seekRepo';

function renderSeek(ctrl: LobbyController, seek: Seek): VNode {
  const seekAction = action(seek, ctrl),
    klass = seekAction === 'joinSeek' ? 'join' : 'cancel',
    noarg = ctrl.trans.noarg;
  return h(
    'tr.seek.' + klass,
    {
      key: seek.id,
      attrs: {
        title:
          seekAction === 'joinSeek'
            ? `${noarg('joinTheGame')} - ${capitalize(noarg(seek.variant || 'standard'))} - ${noarg('correspondence')}`
            : noarg('cancel'),
        'data-id': seek.id,
      },
    },
    tds([
      h('span.is.is2.color-icon.' + (seek.color || 'random')),
      seek.rating
        ? h(
            'span.ulpt',
            {
              attrs: { 'data-href': '/@/' + seek.username },
            },
            seek.username
          )
        : 'Anonymous',
      seek.rating + (seek.provisional ? '?' : ''),
      seek.days ? ctrl.trans.plural('nbDays', seek.days) : '∞',
      h('span', [
        h('span.varicon', {
          attrs: { 'data-icon': getPerfIcon(seek.perf || seek.variant || 'standard') },
        }),
        noarg(seek.mode === 1 ? 'rated' : 'casual'),
      ]),
    ])
  );
}

function createSeek(ctrl: LobbyController): VNode | undefined {
  if (ctrl.data.me && ctrl.data.seeks.length < 8)
    return h('div.create', [
      h(
        'a.button',
        {
          hook: bind('click', () => {
            $('.lobby__start .config_hook')
              .each(function (this: HTMLElement) {
                this.dataset.hrefAddon = '?time=correspondence';
              })
              .trigger('mousedown')
              .trigger('click');
          }),
        },
        ctrl.trans('createAGame')
      ),
    ]);
  return;
}

export default function (ctrl: LobbyController): MaybeVNodes {
  const headers: I18nKey[] = ['player', 'rating', 'time', 'mode'];
  return [
    h('table.hooks__list', [
      h('thead', [h('tr', [h('th', '')].concat(headers.map(header => h('th', ctrl.trans(header)))))]),
      h(
        'tbody',
        {
          hook: bind('click', e => {
            let el = e.target as HTMLElement;
            do {
              el = el.parentNode as HTMLElement;
              if (el.nodeName === 'TR') {
                if (!ctrl.data.me) {
                  if (confirm(ctrl.trans('youNeedAnAccountToDoThat'))) location.href = '/signup';
                  return;
                }
                return ctrl.clickSeek(el.getAttribute('data-id')!);
              }
            } while (el.nodeName !== 'TABLE');
          }),
        },
        ctrl.data.seeks.map(s => renderSeek(ctrl, s))
      ),
    ]),
    createSeek(ctrl),
  ];
}
