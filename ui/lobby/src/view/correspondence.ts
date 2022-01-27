import { h, VNode } from 'snabbdom';
import { bind } from 'common/snabbdom';
import { tds, perfNames } from './util';
import LobbyController from '../ctrl';
import { Seek, MaybeVNodes } from '../interfaces';
import perfIcons from 'common/perfIcons';

function renderSeek(ctrl: LobbyController, seek: Seek): VNode {
  const klass = seek.action === 'joinSeek' ? 'join' : 'cancel',
    noarg = ctrl.trans.noarg;
  return h(
    'tr.seek.' + klass,
    {
      key: seek.id,
      attrs: {
        title: seek.action === 'joinSeek' ? noarg('joinTheGame') + ' - ' + perfNames[seek.perf.key] : noarg('cancel'),
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
      seek.rating && ctrl.opts.showRatings ? seek.rating + (seek.provisional ? '?' : '') : '',
      seek.days ? ctrl.trans.plural('nbDays', seek.days) : '∞',
      h('span', [
        h('span.varicon', {
          attrs: { 'data-icon': perfIcons[seek.perf.key] },
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
  return [
    h('table.hooks__list', [
      h('thead', [
        h(
          'tr',
          ['', 'player', 'rating', 'time', 'mode'].map(header => h('th', ctrl.trans(header)))
        ),
      ]),
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
