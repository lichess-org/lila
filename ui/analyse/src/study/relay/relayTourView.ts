import AnalyseCtrl from '../../ctrl';
import RelayCtrl, { RelayTab } from './relayCtrl';
import * as licon from 'common/licon';
import { bind, dataIcon, looseH as h } from 'common/snabbdom';
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

export default function (ctrl: AnalyseCtrl): VNode | undefined {
  const study = ctrl.study,
    relay = study?.relay;
  if (!study || !relay?.tourShow()) return undefined;
  const content =
    relay.tab() == 'overview'
      ? overview(relay, ctrl)
      : relay.tab() == 'games'
      ? games(relay, study, ctrl)
      : relay.tab() == 'teams'
      ? teams(relay, ctrl)
      : leaderboard(relay, ctrl);

  return h('div.relay-tour', content);
}

export const tourTabs = (ctrl: AnalyseCtrl) => {
  const study = ctrl.study,
    relay = study?.relay;
  if (!relay) return undefined;

  const makeTab = (key: RelayTab, name: string) =>
    h(
      'a.' + key,
      {
        class: { active: relay.tab() === key },
        attrs: { role: 'tab' },
        hook: bind('mousedown', () => relay.openTab(key)),
      },
      name,
    );

  return h('aside.relay-tour__side', [
    h(
      'div.subnav.relay-tour__tabs',
      h('nav.subnav__inner', { attrs: { role: 'tablist' } }, [
        makeTab('overview', 'Overview'),
        makeTab('games', 'Games'),
        relay.teams && makeTab('teams', 'Teams'),
        relay.data.leaderboard ? makeTab('leaderboard', 'Leaderboard') : undefined,
      ]),
    ),
    gamesList(),
  ]);
};

const leaderboard = (relay: RelayCtrl, ctrl: AnalyseCtrl): VNode[] => {
  const players = relay.data.leaderboard || [];
  const withRating = !!players.find(p => p.rating);
  return [
    h('div.box.relay-tour__box', [
      header(relay, ctrl),
      h('div.relay-tour__leaderboard', [
        h('table.slist.slist-invert.slist-pad', [
          h(
            'thead',
            h('tr', [h('th'), withRating ? h('th', 'Elo') : undefined, h('th', 'Score'), h('th', 'Games')]),
          ),
          h(
            'tbody',
            players.map(player => {
              const fullName = [userTitle(player), player.name];
              return h('tr', [
                h(
                  'th',
                  player.fideId
                    ? h('a', { attrs: { href: `/fide/${player.fideId}/redirect` } }, fullName)
                    : fullName,
                ),
                withRating && player.rating ? h('td', `${player.rating}`) : undefined,
                h('td', `${player.score}`),
                h('td', `${player.played}`),
              ]);
            }),
          ),
        ]),
      ]),
    ]),
  ];
};

const overview = (relay: RelayCtrl, ctrl: AnalyseCtrl) => [
  h('div.box.relay-tour__box.relay-tour__overview', [
    header(relay, ctrl),
    // h(
    //   'a.relay-tour__round',
    //   {
    //     class: { ongoing: !!round.ongoing },
    //     attrs: { tabindex: 0 },
    //     hook: bind('click', () => $('span.chapters[role="tab"]').trigger('mousedown')),
    //   },
    //   [
    //     h('strong', round.name),
    //     ' ',
    //     round.ongoing
    //       ? study.trans.noarg('playingRightNow')
    //       : !!round.startsAt &&
    //         h(
    //           'time.timeago',
    //           { hook: onInsert(el => el.setAttribute('datetime', '' + round.startsAt)) },
    //           site.timeago(round.startsAt),
    //         ),
    //   ],
    // ),
    relay.data.tour.markup
      ? h('div.relay-tour__markup.box-pad', {
          hook: innerHTML(relay.data.tour.markup, () => relay.data.tour.markup!),
        })
      : h('div.relay-tour__markup.box-pad', relay.data.tour.description),
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
          h('td.time', !!round.startsAt ? site.dateFormat()(new Date(round.startsAt)) : '-'),
          h(
            'td.status',
            roundStateIcon(round) || (round.startsAt ? site.timeago(round.startsAt) : undefined),
          ),
        ]),
      ),
    ),
  ]);

const gamesList = () => {
  return h('div.relay-tour__games-list', []);
};

const games = (relay: RelayCtrl, study: StudyCtrl, ctrl: AnalyseCtrl) => [
  h('div.box.relay-tour__box', [header(relay, ctrl), multiBoardView(study.multiBoard, study)]),
];

const teams = (relay: RelayCtrl, ctrl: AnalyseCtrl) =>
  relay.teams ? [h('div.box.relay-tour__box', [header(relay, ctrl), teamsView(relay.teams)])] : [];

const header = (relay: RelayCtrl, ctrl: AnalyseCtrl) => {
  const d = relay.data,
    study = ctrl.study!,
    group = relay.data.group;
  return h('div.relay-tour__header', [
    h('div.relay-tour__header__image', [
      d.tour.image
        ? h('img', { attrs: { src: d.tour.image } })
        : study.members.isOwner()
        ? h(
            'a.button.relay-tour__image-upload',
            { attrs: { href: `/broadcast/${relay.data.tour.id}/edit` } },
            'Upload tournament image',
          )
        : undefined,
    ]),
    h('div.relay-tour__header__content', [
      h('h1', group?.name || d.tour.name),
      h('div.relay-tour__header__selectors', [group && groupSelect(relay, group), roundSelect(relay, study)]),
      ...(defined(d.isSubscribed)
        ? [
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
          ]
        : []),
    ]),
  ]);
};

const roundStateIcon = (round: RelayRound) =>
  round.ongoing
    ? h('ongoing', { attrs: { ...dataIcon(licon.DiscBig), title: 'Ongoing' } })
    : round.finished && h('finished', { attrs: { ...dataIcon(licon.Checkmark), title: 'Finished' } });
