import { h } from 'snabbdom';
import { VNode } from 'snabbdom';
import { tds, bind } from './util';
import LobbyController from '../ctrl';
import { Seek, MaybeVNodes } from '../interfaces';

function renderSeek(ctrl: LobbyController, seek: Seek): VNode {
  const klass = seek.action === 'joinSeek' ? 'join' : 'cancel',
    noarg = ctrl.trans.noarg;
  return h(
    'tr.seek.' + klass,
    {
      key: seek.id,
      attrs: {
        title: seek.action === 'joinSeek' ? noarg('joinTheGame') + ' - ' + seek.perf.name : noarg('cancel'),
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
          attrs: { 'data-icon': seek.perf.icon },
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
          attrs: { href: '/?time=correspondence#hook' },
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
