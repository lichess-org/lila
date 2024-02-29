import AnalyseCtrl from '../../ctrl';
import RelayCtrl, { RelayTab } from './relayCtrl';
import * as licon from 'common/licon';
import { bind, dataIcon, onInsert, looseH as h, MaybeVNodes } from 'common/snabbdom';
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
      ? overview(relay, study, ctrl)
      : relay.tab() == 'games'
      ? games(relay, study, ctrl)
      : relay.tab() == 'teams'
      ? teams(relay, ctrl)
      : relay.tab() == 'schedule'
      ? schedule(relay, ctrl)
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

  return h(
    'aside.subnav.relay-tour__tabs',
    h('nav.subnav__inner', { attrs: { role: 'tablist' } }, [
      makeTab('overview', 'Overview'),
      !study.looksNew() && makeTab('games', 'Games'),
      relay.teams && makeTab('teams', 'Teams'),
      makeTab('schedule', 'Schedule'),
      relay.data.leaderboard ? makeTab('leaderboard', 'Leaderboard') : undefined,
    ]),
  );
};

const leaderboard = (relay: RelayCtrl, ctrl: AnalyseCtrl): VNode[] => {
  const players = relay.data.leaderboard || [];
  const withRating = !!players.find(p => p.rating);
  return [
    h('div.box.relay-tour__box', [
      header(relay, ctrl, true),
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

const overview = (relay: RelayCtrl, study: StudyCtrl, ctrl: AnalyseCtrl) => {
  const round = relay.currentRound();
  return [
    h('div.box.relay-tour__box.relay-tour__overview', [
      relay.data.tour.image
        ? h('img.relay-tour__image', { attrs: { src: relay.data.tour.image } })
        : study.members.isOwner()
        ? h(
            'div.relay-tour__image-upload',
            h(
              'a.button',
              { attrs: { href: `/broadcast/${relay.data.tour.id}/edit` } },
              'Upload tournament image',
            ),
          )
        : undefined,
      header(relay, ctrl, true),
      h(
        'a.relay-tour__round',
        {
          class: { ongoing: !!round.ongoing },
          attrs: { tabindex: 0 },
          hook: bind('click', () => $('span.chapters[role="tab"]').trigger('mousedown')),
        },
        [
          h('strong', round.name),
          ' ',
          round.ongoing
            ? study.trans.noarg('playingRightNow')
            : !!round.startsAt &&
              h(
                'time.timeago',
                { hook: onInsert(el => el.setAttribute('datetime', '' + round.startsAt)) },
                site.timeago(round.startsAt),
              ),
        ],
      ),
      relay.data.tour.markup
        ? h('div.relay-tour__markup.box-pad', {
            hook: innerHTML(relay.data.tour.markup, () => relay.data.tour.markup!),
          })
        : h('div.relay-tour__markup.box-pad', relay.data.tour.description),
    ]),
  ];
};

const groupSelect = (relay: RelayCtrl, group: RelayGroup) =>
  h('div.relay-tour__group-title', [
    h('h1', group.name),
    h('div.mselect.relay-tour__group-select', [
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
    ]),
  ]);

const games = (relay: RelayCtrl, study: StudyCtrl, ctrl: AnalyseCtrl) => [
  h('div.box.relay-tour__box', [header(relay, ctrl, true), multiBoardView(study.multiBoard, study)]),
];

const teams = (relay: RelayCtrl, ctrl: AnalyseCtrl) =>
  relay.teams ? [h('div.box.relay-tour__box', [header(relay, ctrl, true), teamsView(relay.teams)])] : [];

const header = (relay: RelayCtrl, ctrl: AnalyseCtrl, pad: boolean = false) => {
  const d = relay.data;
  return h(`div.relay-tour__header${pad ? '.box-pad' : ''}`, [
    relay.data.group ? groupSelect(relay, relay.data.group) : h('h1', d.tour.name),
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
  ]);
};

const schedule = (relay: RelayCtrl, ctrl: AnalyseCtrl): MaybeVNodes => [
  h('div.box.relay-tour__box', [
    h('div.relay-tour__schedule', [
      header(relay, ctrl, true),
      h(
        'table.slist.slist-invert.slist-pad',
        h(
          'tbody',
          relay.data.rounds.map(round =>
            h('tr', [
              h('th', h('a.link', { attrs: { href: relay.roundPath(round) } }, round.name)),
              h('td', round.startsAt ? site.dateFormat()(new Date(round.startsAt)) : undefined),
              h('td', roundStateIcon(round) || (round.startsAt ? site.timeago(round.startsAt) : undefined)),
            ]),
          ),
        ),
      ),
    ]),
  ]),
];

const roundStateIcon = (round: RelayRound) =>
  round.ongoing
    ? h('ongoing', { attrs: { ...dataIcon(licon.DiscBig), title: 'Ongoing' } })
    : round.finished && h('finished', { attrs: { ...dataIcon(licon.Checkmark), title: 'Finished' } });
