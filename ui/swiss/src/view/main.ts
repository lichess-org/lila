import * as licon from 'common/licon';
import { spinnerVdom as spinner } from 'common/spinner';
import { type VNode, dataIcon, bind, onInsert, type LooseVNodes, looseH as h } from 'common/snabbdom';
import { numberRow } from './util';
import type SwissCtrl from '../ctrl';
import { players, renderPager } from '../pagination';
import type { SwissData, Pager } from '../interfaces';
import header from './header';
import standing from './standing';
import * as boards from './boards';
import podium from './podium';
import playerInfo from './playerInfo';
import flatpickr from 'flatpickr';
import { use24h } from 'common/i18n';
import { once } from 'common/storage';
import { initMiniGames } from 'common/miniBoard';
import { watchers } from 'common/watchers';
import { makeChat } from 'chat';
import { prompt } from 'common/dialog';

export default function (ctrl: SwissCtrl) {
  const d = ctrl.data;
  const content =
    d.status === 'created' ? created(ctrl) : d.status === 'started' ? started(ctrl) : finished(ctrl);
  return h('main.' + ctrl.opts.classes, { hook: { postpatch: () => initMiniGames() } }, [
    h('aside.swiss__side', {
      hook: onInsert(el => {
        $(el).replaceWith(ctrl.opts.$side);
        ctrl.opts.chat && makeChat(ctrl.opts.chat);
      }),
    }),
    h('div.swiss__underchat', {
      hook: onInsert(el => $(el).replaceWith($('.swiss__underchat.none').removeClass('none'))),
    }),
    playerInfo(ctrl) || stats(ctrl) || boards.top(d.boards, ctrl.opts),
    h('div.swiss__main', [h('div.box.swiss__main-' + d.status, content), boards.many(d.boards, ctrl.opts)]),
    ctrl.opts.chat && h('div.chat__members.none', { hook: onInsert(watchers) }),
  ]);
}

function created(ctrl: SwissCtrl): LooseVNodes {
  const pag = players(ctrl);
  return [
    header(ctrl),
    nextRound(ctrl),
    controls(ctrl, pag),
    standing(ctrl, pag, 'created'),
    ctrl.data.quote &&
      h('blockquote.pull-quote', [h('p', ctrl.data.quote.text), h('footer', ctrl.data.quote.author)]),
  ];
}

const notice = (ctrl: SwissCtrl) => {
  const d = ctrl.data;
  return (
    d.me &&
    !d.me.absent &&
    d.status === 'started' &&
    d.nextRound &&
    h('div.swiss__notice.bar-glider', i18n.site.standByX(d.me.name))
  );
};

function started(ctrl: SwissCtrl): LooseVNodes {
  const pag = players(ctrl);
  return [
    header(ctrl),
    joinTheGame(ctrl) || notice(ctrl),
    nextRound(ctrl),
    controls(ctrl, pag),
    standing(ctrl, pag, 'started'),
  ];
}

function finished(ctrl: SwissCtrl): LooseVNodes {
  const pag = players(ctrl);
  return [
    h('div.podium-wrap', [confetti(ctrl.data), header(ctrl), podium(ctrl)]),
    controls(ctrl, pag),
    standing(ctrl, pag, 'finished'),
  ];
}

function controls(ctrl: SwissCtrl, pag: Pager): VNode {
  return h('div.swiss__controls', [h('div.pager', renderPager(ctrl, pag)), joinButton(ctrl)]);
}

function nextRound(ctrl: SwissCtrl): VNode | undefined {
  if (!ctrl.opts.schedule || ctrl.data.nbOngoing || ctrl.data.round === 0) return;
  return h(
    'form.schedule-next-round',
    {
      class: { required: !ctrl.data.nextRound },
      attrs: { action: `/api/swiss/${ctrl.data.id}/schedule-next-round`, method: 'post' },
    },
    [
      h('input', {
        attrs: { name: 'date', placeholder: 'Schedule the next round', value: ctrl.data.nextRound?.at || '' },
        hook: onInsert((el: HTMLInputElement) =>
          flatpickr(el, {
            minDate: 'today',
            maxDate: new Date(Date.now() + 1000 * 3600 * 24 * 31),
            dateFormat: 'Z',
            altInput: true,
            altFormat: 'Y-m-d h:i K',
            enableTime: true,
            monthSelectorType: 'static',
            onClose() {
              (el.parentNode as HTMLFormElement).submit();
            },
            time_24hr: use24h(),
          }),
        ),
      }),
    ],
  );
}

function joinButton(ctrl: SwissCtrl): VNode | undefined {
  const d = ctrl.data;
  if (!ctrl.opts.userId)
    return h(
      'a.fbt.text.highlight',
      { attrs: { href: '/login?referrer=' + window.location.pathname, 'data-icon': licon.PlayTriangle } },
      i18n.site.signIn,
    );

  if (d.joinTeam)
    return h(
      'a.fbt.text.highlight',
      { attrs: { href: `/team/${d.joinTeam}`, 'data-icon': licon.Group } },
      i18n.team.joinTeam,
    );

  if (d.canJoin)
    return ctrl.joinSpinner
      ? spinner()
      : h(
          'button.fbt.text.highlight',
          {
            attrs: dataIcon(licon.PlayTriangle),
            hook: bind(
              'click',
              async () => {
                if (d.password) {
                  const p = await prompt(i18n.site.tournamentEntryCode);
                  if (p !== null) ctrl.join(p);
                } else ctrl.join();
              },
              ctrl.redraw,
            ),
          },
          i18n.site.join,
        );

  if (d.me && d.status != 'finished')
    return d.me.absent
      ? ctrl.joinSpinner
        ? spinner()
        : h(
            'button.fbt.text.highlight',
            { attrs: dataIcon(licon.PlayTriangle), hook: bind('click', _ => ctrl.join(), ctrl.redraw) },
            i18n.site.join,
          )
      : ctrl.joinSpinner
        ? spinner()
        : h(
            'button.fbt.text',
            { attrs: dataIcon(licon.FlagOutline), hook: bind('click', ctrl.withdraw, ctrl.redraw) },
            i18n.site.withdraw,
          );

  return;
}

function joinTheGame(ctrl: SwissCtrl) {
  const gameId = ctrl.data.me?.gameId;
  return (
    gameId &&
    h('a.swiss__ur-playing.button.is.is-after', { attrs: { href: '/' + gameId } }, [
      i18n.site.youArePlaying,
      h('br'),
      i18n.site.joinTheGame,
    ])
  );
}

function confetti(data: SwissData) {
  return (
    data.me &&
    data.isRecentlyFinished &&
    once('tournament.end.canvas.' + data.id) &&
    h('canvas#confetti', {
      hook: {
        insert: _ => site.asset.loadEsm('bits.confetti'),
      },
    })
  );
}

function stats(ctrl: SwissCtrl) {
  const s = ctrl.data.stats,
    slots = ctrl.data.round * ctrl.data.nbPlayers;
  if (!s) return undefined;
  return h('div.swiss__stats', [
    h('h2', i18n.site.tournamentComplete),
    h('table', [
      ctrl.opts.showRatings ? numberRow(i18n.site.averageElo, s.averageRating, 'raw') : null,
      numberRow(i18n.site.gamesPlayed, s.games),
      numberRow(i18n.site.whiteWins, [s.whiteWins, slots], 'percent'),
      numberRow(i18n.site.blackWins, [s.blackWins, slots], 'percent'),
      numberRow(i18n.site.drawRate, [s.draws, slots], 'percent'),
      numberRow(i18n.swiss.byes, [s.byes, slots], 'percent'),
      numberRow(i18n.swiss.absences, [s.absences, slots], 'percent'),
    ]),
    h('div.swiss__stats__links', [
      h(
        'a',
        { attrs: { href: `/swiss/${ctrl.data.id}/round/1` } },
        i18n.swiss.viewAllXRounds(ctrl.data.round),
      ),
      h('br'),
      h(
        'a.text',
        { attrs: { 'data-icon': licon.Download, href: `/swiss/${ctrl.data.id}.trf`, download: true } },
        'Download TRF file',
      ),
      h(
        'a.text',
        { attrs: { 'data-icon': licon.Download, href: `/api/swiss/${ctrl.data.id}/games`, download: true } },
        i18n.study.downloadAllGames,
      ),
      h(
        'a.text',
        {
          attrs: { 'data-icon': licon.Download, href: `/api/swiss/${ctrl.data.id}/results`, download: true },
        },
        'Download results as NDJSON',
      ),
      h(
        'a.text',
        {
          attrs: {
            'data-icon': licon.Download,
            href: `/api/swiss/${ctrl.data.id}/results?as=csv`,
            download: true,
          },
        },
        'Download results as CSV',
      ),
      h('br'),
      h(
        'a.text',
        { attrs: { 'data-icon': licon.InfoCircle, href: 'https://lichess.org/api#tag/Swiss-tournaments' } },
        'Swiss API documentation',
      ),
    ]),
  ]);
}
