import { h, VNode } from 'snabbdom';
import TournamentController from '../ctrl';
import { TournamentData, MaybeVNodes } from '../interfaces';
import * as pagination from '../pagination';
import { controls, standing, podium } from './arena';
import { teamStanding } from './battle';
import header from './header';
import playerInfo from './playerInfo';
import teamInfo from './teamInfo';
import { numberRow } from './util';

function confetti(data: TournamentData): VNode | undefined {
  if (data.me && data.isRecentlyFinished && lichess.once('tournament.end.canvas.' + data.id))
    return h('canvas#confetti', {
      hook: {
        insert: _ => lichess.loadScript('javascripts/confetti.js'),
      },
    });
  return undefined;
}

function stats(ctrl: TournamentController): VNode | undefined {
  const data = ctrl.data,
    trans = ctrl.trans,
    noarg = trans.noarg;
  if (!data.stats) return undefined;
  const tableData = [
    ctrl.opts.showRatings ? numberRow(noarg('averageElo'), data.stats.averageRating, 'raw') : null,
    numberRow(noarg('gamesPlayed'), data.stats.games),
    numberRow(noarg('movesPlayed'), data.stats.moves),
    numberRow(noarg('whiteWins'), [data.stats.whiteWins, data.stats.games], 'percent'),
    numberRow(noarg('blackWins'), [data.stats.blackWins, data.stats.games], 'percent'),
    numberRow(noarg('draws'), [data.stats.draws, data.stats.games], 'percent'),
  ];

  if (data.berserkable) {
    tableData.push(numberRow(noarg('berserkRate'), [data.stats.berserks / 2, data.stats.games], 'percent'));
  }

  return h('div.tour__stats', [
    h('h2', noarg('tournamentComplete')),
    h('table', tableData),
    h('div.tour__stats__links', [
      ...(data.teamBattle
        ? [
            h(
              'a',
              {
                attrs: {
                  href: `/tournament/${data.id}/teams`,
                },
              },
              trans('viewAllXTeams', Object.keys(data.teamBattle.teams).length)
            ),
            h('br'),
          ]
        : []),
      h(
        'a.text',
        {
          attrs: {
            'data-icon': '',
            href: `/api/tournament/${data.id}/games`,
            download: true,
          },
        },
        'Download all games'
      ),
      data.me &&
        h(
          'a.text',
          {
            attrs: {
              'data-icon': '',
              href: `/api/tournament/${data.id}/games?player=${ctrl.opts.userId}`,
              download: true,
            },
          },
          'Download my games'
        ),
      h(
        'a.text',
        {
          attrs: {
            'data-icon': '',
            href: `/api/tournament/${data.id}/results`,
            download: true,
          },
        },
        'Download results as NDJSON'
      ),
      h(
        'a.text',
        {
          attrs: {
            'data-icon': '',
            href: `/api/tournament/${data.id}/results?as=csv`,
            download: true,
          },
        },
        'Download results as CSV'
      ),
      h('br'),
      h(
        'a.text',
        {
          attrs: {
            'data-icon': '',
            href: 'https://lichess.org/api#tag/Arena-tournaments',
          },
        },
        'Arena API documentation'
      ),
    ]),
  ]);
}

export const name = 'finished';

export function main(ctrl: TournamentController): MaybeVNodes {
  const pag = pagination.players(ctrl);
  const teamS = teamStanding(ctrl, 'finished');
  return [
    ...(teamS ? [header(ctrl), teamS] : [h('div.podium-wrap', [confetti(ctrl.data), header(ctrl), podium(ctrl)])]),
    controls(ctrl, pag),
    standing(ctrl, pag),
  ];
}

export function table(ctrl: TournamentController): VNode | undefined {
  return ctrl.playerInfo.id ? playerInfo(ctrl) : ctrl.teamInfo.requested ? teamInfo(ctrl) : stats(ctrl);
}
