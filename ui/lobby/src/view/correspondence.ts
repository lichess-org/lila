import { h, type VNode } from 'snabbdom';
import { bind, type MaybeVNodes, confirm } from 'lib/view';
import { tds, perfNames } from './util';
import type LobbyController from '../ctrl';
import type { Seek } from '../interfaces';
import perfIcons from 'lib/game/perfIcons';

function renderSeek(ctrl: LobbyController, seek: Seek): VNode {
  const klass = seek.action === 'joinSeek' ? 'join' : 'cancel';
  return h(
    'tr.seek.' + klass,
    {
      key: seek.id,
      attrs: {
        role: 'button',
        title:
          seek.action === 'joinSeek'
            ? i18n.site.joinTheGame + ' - ' + perfNames[seek.perf.key]
            : i18n.site.cancel,
        'data-id': seek.id,
      },
    },
    tds([
      seek.rating
        ? h('span.ulpt', { attrs: { 'data-href': '/@/' + seek.username } }, seek.username)
        : 'Anonymous',
      seek.rating && ctrl.opts.showRatings ? seek.rating + (seek.provisional ? '?' : '') : '',
      seek.days ? i18n.site.nbDays(seek.days) : '∞',
      h('span', [
        h('span.varicon', { attrs: { 'data-icon': perfIcons[seek.perf.key] } }),
        seek.mode === 1 ? i18n.site.rated : i18n.site.casual,
      ]),
    ]),
  );
}

function createSeek(ctrl: LobbyController): VNode | undefined {
  if (ctrl.me && ctrl.data.seeks.length < 8)
    return h('div.create', [
      h(
        'a.button',
        {
          hook: bind(
            'click',
            () => ctrl.setupCtrl.openModal('hook', { variant: 'standard', timeMode: 'correspondence' }),
            ctrl.redraw,
          ),
        },
        i18n.site.createAGame,
      ),
    ]);
  return;
}

export default function (ctrl: LobbyController): MaybeVNodes {
  return [
    h('table.hooks__list', [
      h(
        'thead',
        h(
          'tr',
          (['player', 'rating', 'time', 'mode'] as const).map(k => h('th', i18n.site[k])),
        ),
      ),

      h(
        'tbody',
        {
          hook: bind('click', async e => {
            let el = e.target as HTMLElement;
            do {
              el = el.parentNode as HTMLElement;
              if (el.nodeName === 'TR') {
                if (!ctrl.me) {
                  if (await confirm(i18n.site.youNeedAnAccountToDoThat, i18n.site.signUp, i18n.site.cancel))
                    location.href = '/signup';
                  return;
                }
                return ctrl.clickSeek(el.dataset['id']!);
              }
            } while (el.nodeName !== 'TABLE');
          }),
        },
        ctrl.data.seeks.map(s => renderSeek(ctrl, s)),
      ),
    ]),
    createSeek(ctrl),
  ];
}
