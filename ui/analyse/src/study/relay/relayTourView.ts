import AnalyseCtrl from '../../ctrl';
import RelayCtrl, { RelayTab } from './relayCtrl';
import * as licon from 'common/licon';
import { bind, dataIcon, onInsert, looseH as h } from 'common/snabbdom';
import { VNode } from 'snabbdom';
import { innerHTML } from 'common/richText';
import { RelayGroup, RelayRound } from './interfaces';
import { view as multiBoardView } from '../multiBoard';
import { defined } from 'common';
import StudyCtrl from '../studyCtrl';
import { toggle } from 'common/controls';
import * as xhr from 'common/xhr';
import { teamsView } from './relayTeams';
import { userTitle } from 'common/userLink';
import { videoPlayerView } from '../../view/videoPlayerView';
import { ChapterId } from '../interfaces';
import { makeChat } from '../../view/view';
import { gamesList } from './relayGames';

export default function (ctrl: AnalyseCtrl): VNode | undefined {
  const study = ctrl.study,
    relay = study?.relay;
  if (!study || !relay?.tourShow()) return undefined;
  const content =
    relay.tab() == 'overview'
      ? overview(relay, ctrl)
      : relay.tab() == 'boards'
      ? games(relay, study, ctrl)
      : relay.tab() == 'teams'
      ? teams(relay, ctrl)
      : leaderboard(relay, ctrl);
  return h('div.relay-tour', content);
}

export const tourSide = (ctrl: AnalyseCtrl, study: StudyCtrl, relay: RelayCtrl) =>
  h('aside.relay-tour__side', [
    h('div.relay-tour__side__header', [
      h(
        'button.relay-tour__side__name',
        { hook: bind('click', relay.tourShow.toggle, relay.redraw) },
        study.data.name,
      ),
      h('button.relay-tour__side__search', {
        attrs: { 'data-icon': licon.Search, title: 'Search' },
        hook: bind('click', study.search.open.toggle),
      }),
    ]),
    gamesList(study, relay),
    h('div.chat__members', {
      hook: onInsert(el => {
        makeChat(ctrl, chat => el.parentNode!.insertBefore(chat, el));
        site.watchers(el);
      }),
    }),
  ]);

const leaderboard = (relay: RelayCtrl, ctrl: AnalyseCtrl): VNode[] => {
  const players = relay.data.leaderboard || [];
  const withRating = !!players.find(p => p.rating);
  return [
    h('div.box.relay-tour__box', [
      ...header(relay, ctrl),
      h('div.relay-tour__leaderboard', [
        h('table.slist.slist-invert.slist-pad', [
          h(
            'thead',
            h('tr', [
              h('th'),
              h('th'),
              withRating ? h('th', 'Elo') : undefined,
              h('th', 'Score'),
              h('th', 'Games'),
            ]),
          ),
          h(
            'tbody',
            players.map(player =>
              h('tr', [
                h('th', userTitle(player)),
                h(
                  'th',
                  player.fideId
                    ? h('a', { attrs: { href: `/fide/${player.fideId}/redirect` } }, player.name)
                    : player.name,
                ),
                h('td', withRating && player.rating ? `${player.rating}` : undefined),
                h('td', `${player.score}`),
                h('td', `${player.played}`),
              ]),
            ),
          ),
        ]),
      ]),
    ]),
  ];
};

const overview = (relay: RelayCtrl, ctrl: AnalyseCtrl) => [
  h('div.box.relay-tour__box.relay-tour__overview', [
    ...header(relay, ctrl),
    relay.data.tour.markup
      ? h('div.relay-tour__markup', {
          hook: innerHTML(relay.data.tour.markup, () => relay.data.tour.markup!),
        })
      : h('div.relay-tour__markup', relay.data.tour.description),
  ]),
];

const groupSelect = (relay: RelayCtrl, group: RelayGroup) =>
  h('div.mselect.relay-tour__header__mselect.relay-tour__group-select', [
    h('input#mselect-relay-group.mselect__toggle.fullscreen-toggle', { attrs: { type: 'checkbox' } }),
    h(
      'label.mselect__label',
      { attrs: { for: 'mselect-relay-group' } },
      group.tours.find(t => t.id == relay.data.tour.id)?.name || relay.data.tour.name,
    ),
    h('label.fullscreen-mask', { attrs: { for: 'mselect-relay-group' } }),
    h(
      'nav.mselect__list',
      group.tours.map(tour =>
        h(
          `a${tour.id == relay.data.tour.id ? '.current' : ''}`,
          { attrs: { href: `/broadcast/-/${tour.id}` } },
          tour.name,
        ),
      ),
    ),
  ]);

const roundSelect = (relay: RelayCtrl, study: StudyCtrl) =>
  h('div.mselect.relay-tour__header__mselect.relay-tour__header__round-select', [
    h('input#mselect-relay-round.mselect__toggle.fullscreen-toggle', { attrs: { type: 'checkbox' } }),
    h(
      'label.mselect__label',
      { attrs: { for: 'mselect-relay-round' } },
      relay.data.rounds.find(r => r.id == study.data.id)?.name || study.data.name,
    ),
    h('label.fullscreen-mask', { attrs: { for: 'mselect-relay-round' } }),
    h(
      'table.mselect__list',
      {
        hook: bind('click', (e: MouseEvent) => {
          const target = e.target as HTMLElement;
          if (target.tagName != 'A') site.redirect($(target).parents('tr').find('a').attr('href')!);
        }),
      },
      relay.data.rounds.map(round =>
        h(`tr${round.id == study.data.id ? '.current-round' : ''}`, [
          h('td.name', h('a', { attrs: { href: relay.roundPath(round) } }, round.name)),
          h('td.time', round.startsAt ? site.dateFormat()(new Date(round.startsAt)) : '-'),
          h(
            'td.status',
            roundStateIcon(round) || (round.startsAt ? site.timeago(round.startsAt) : undefined),
          ),
        ]),
      ),
    ),
  ]);

export const gameLinkProps = (roundPath: () => string, game: { id: ChapterId }) => ({
  attrs: {
    href: `${roundPath()}/${game.id}`,
    'data-id': game.id,
  },
});
export const gameLinksListener = (setChapter: (id: ChapterId) => void) => (vnode: VNode) =>
  (vnode.elm as HTMLElement).addEventListener(
    'click',
    e => {
      e.preventDefault();
      let target = e.target as HTMLElement;
      while (target && target.tagName !== 'A') target = target.parentNode as HTMLElement;
      const id = target?.dataset['id'];
      if (id) setChapter(id);
    },
    { passive: false },
  );

const games = (relay: RelayCtrl, study: StudyCtrl, ctrl: AnalyseCtrl) => [
  h('div.box.relay-tour__box', [...header(relay, ctrl), multiBoardView(study.multiBoard, study)]),
];

const teams = (relay: RelayCtrl, ctrl: AnalyseCtrl) =>
  relay.teams ? [h('div.box.relay-tour__box', [...header(relay, ctrl), teamsView(relay.teams)])] : [];

const header = (relay: RelayCtrl, ctrl: AnalyseCtrl) => {
  const d = relay.data,
    study = ctrl.study!,
    group = relay.data.group;
  return [
    h('div.relay-tour__header', [
      h('div.relay-tour__header__image', [
        d.videoEmbedSrc
          ? videoPlayerView(ctrl)
          : d.tour.image
          ? h('img', { attrs: { src: d.tour.image } })
          : study.members.isOwner()
          ? h(
              'a.button.relay-tour__header__image-upload',
              { attrs: { href: `/broadcast/${relay.data.tour.id}/edit` } },
              'Upload tournament image',
            )
          : undefined,
      ]),
      h('div.relay-tour__header__content', [
        h('h1', group?.name || d.tour.name),
        h('div.relay-tour__header__selectors', [
          group && groupSelect(relay, group),
          roundSelect(relay, study),
        ]),
        defined(d.isSubscribed) &&
          toggle(
            {
              name: 'Subscribe',
              id: 'tour-subscribe',
              checked: d.isSubscribed,
              change: (v: boolean) => {
                xhr.text(`/broadcast/${d.tour.id}/subscribe?set=${v}`, { method: 'post' });
                d.isSubscribed = v;
                ctrl.redraw();
              },
            },
            ctrl.trans,
            ctrl.redraw,
          ),
      ]),
    ]),
    makeTabs(ctrl),
  ];
};

const makeTabs = (ctrl: AnalyseCtrl) => {
  const study = ctrl.study,
    relay = study?.relay;
  if (!relay) return undefined;

  const makeTab = (key: RelayTab, name: string) =>
    h(
      `span.relay-tour__tabs--${key}`,
      {
        class: { active: relay.tab() === key },
        attrs: { role: 'tab' },
        hook: bind('mousedown', () => relay.openTab(key)),
      },
      name,
    );
  return h('nav.relay-tour__tabs', { attrs: { role: 'tablist' } }, [
    makeTab('overview', 'Overview'),
    makeTab('boards', 'Boards'),
    relay.teams && makeTab('teams', 'Teams'),
    relay.data.leaderboard ? makeTab('leaderboard', 'Leaderboard') : undefined,
  ]);
};

const roundStateIcon = (round: RelayRound) =>
  round.ongoing
    ? h('ongoing', { attrs: { ...dataIcon(licon.DiscBig), title: 'Ongoing' } })
    : round.finished && h('finished', { attrs: { ...dataIcon(licon.Checkmark), title: 'Finished' } });
