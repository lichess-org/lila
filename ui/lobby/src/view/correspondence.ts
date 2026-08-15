import perfIcons from 'lib/game/perfIcons';
import { bind, confirm, tr, td, span, div, button, table, thead, tbody, th, icon } from 'lib/view';

import type LobbyController from '@/ctrl';
import type { Seek } from '@/interfaces';

import { perfNames } from './util';

function renderSeek(ctrl: LobbyController, seek: Seek) {
  const isJoinAction = seek.action === 'joinSeek';
  return tr(
    `.seek.${isJoinAction ? 'join' : 'cancel'}`,
    {
      key: seek.id,
      role: 'button',
      title: isJoinAction ? i18n.site.joinTheGame + ' - ' + perfNames[seek.perf.key] : i18n.site.cancel,
      'data-id': seek.id,
    },
    [
      td(seek.rating ? span('.ulpt', { 'data-href': '/@/' + seek.username }, seek.username) : 'Anonymous'),
      td(seek.rating && ctrl.opts.showRatings ? seek.rating + (seek.provisional ? '?' : '') : ''),
      td(seek.days ? i18n.site.nbDays(seek.days) : '∞'),
      td([icon(perfIcons[seek.perf.key])('.varicon'), seek.mode === 1 ? i18n.site.rated : i18n.site.casual]),
    ],
  );
}

function createSeek(ctrl: LobbyController) {
  if (ctrl.me && ctrl.data.seeks.length >= 8) return undefined;

  return div('.create', [
    button(
      '.button',
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
}

export default function (ctrl: LobbyController) {
  return [
    table('.hooks__list', [
      thead(tr((['player', 'rating', 'time', 'mode'] as const).map(k => th(i18n.site[k])))),
      tbody(
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
