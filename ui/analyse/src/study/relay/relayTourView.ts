import AnalyseCtrl from '../../ctrl';
import RelayCtrl from './relayCtrl';
import * as licon from 'common/licon';
import { bind, dataIcon, onInsert } from 'common/snabbdom';
import { h, VNode } from 'snabbdom';
import { innerHTML } from 'common/richText';
import { RelayRound } from './interfaces';
import { RelayTab, StudyCtrl } from '../interfaces';
import { view as multiBoardView } from '../multiBoard';
import { scrollToInnerSelector } from 'common';

export default function (ctrl: AnalyseCtrl): VNode | undefined {
  const study = ctrl.study;
  const relay = study?.relay;
  if (!study || !relay?.tourShow.active) return undefined;

  const makeTab = (key: RelayTab, name: string) =>
    h(
      'span.' + key,
      {
        class: { active: relay.tab() === key },
        attrs: { role: 'tab' },
        hook: bind('mousedown', () => relay.tab(key), relay.redraw),
      },
      name,
    );

  const tabs = h('div.tabs-horiz', { attrs: { role: 'tablist' } }, [
    makeTab('overview', 'Overview'),
    makeTab('schedule', 'Schedule'),
    relay.data.leaderboard ? makeTab('leaderboard', 'Leaderboard') : undefined,
  ]);
  const content =
    relay.tab() == 'overview'
      ? overview(relay, study)
      : relay.tab() == 'schedule'
      ? schedule(relay)
      : leaderboard(relay);

  return h('div.relay-tour', [tabs, ...content]);
}

const leaderboard = (relay: RelayCtrl): VNode[] => {
  const players = relay.data.leaderboard || [];
  const withRating = players.find(p => p.rating);
  return [
    h('div.relay-tour__text', [
      h('h1', relay.data.tour.name),
      h('div.relay-tour__text__leaderboard', [
        h('table.slist.slist-invert', [
          h(
            'thead',
            h('tr', [
              h('th', h('h2', 'Leaderboard')),
              withRating ? h('th', 'Elo') : undefined,
              h('th', 'Score'),
              h('th', 'Games'),
            ]),
          ),
          h(
            'tbody',
            players.map(player =>
              h('tr', [
                h('th', player.name),
                withRating ? h('td', player.rating) : undefined,
                h('td', player.score),
                h('td', player.played),
              ]),
            ),
          ),
        ]),
      ]),
    ]),
  ];
};

const overview = (relay: RelayCtrl, study: StudyCtrl) => {
  const round = relay.currentRound();
  return [
    h('div.relay-tour__text', [
      h('h1', relay.data.tour.name),
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
            : round.startsAt
            ? h(
                'time.timeago',
                {
                  hook: onInsert(el => el.setAttribute('datetime', '' + round.startsAt)),
                },
                lichess.timeago(round.startsAt),
              )
            : null,
        ],
      ),
      relay.data.tour.markup
        ? h('div', {
            hook: innerHTML(relay.data.tour.markup, () => relay.data.tour.markup!),
          })
        : h('div', relay.data.tour.description),
    ]),
    study.looksNew() ? null : multiBoardView(study.multiBoard, study),
  ];
};

const schedule = (relay: RelayCtrl): VNode[] => [
  h('div.relay-tour__text', [
    h('div.relay-tour__text__schedule', [
      h('h1', relay.data.tour.name),
      h('h2', 'Schedule'),
      h(
        'table.slist.slist-invert',
        h(
          'tbody',
          relay.data.rounds.map(round =>
            h('tr', [
              h(
                'th',
                h(
                  'a.link',
                  {
                    attrs: { href: relay.roundPath(round) },
                  },
                  round.name,
                ),
              ),
              h('td', round.startsAt ? lichess.dateFormat()(new Date(round.startsAt)) : undefined),
              h(
                'td',
                roundStateIcon(round) || (round.startsAt ? lichess.timeago(round.startsAt) : undefined),
              ),
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
    : round.finished
    ? h('finished', { attrs: { ...dataIcon(licon.Checkmark), title: 'Finished' } })
    : null;

export function rounds(ctrl: StudyCtrl): VNode {
  const canContribute = ctrl.members.canContribute();
  const relay = ctrl.relay!;
  return h(
    'div.study__relay__rounds',
    {
      hook: onInsert(el => scrollToInnerSelector(el, '.active')),
    },
    relay.data.rounds
      .map(round =>
        h(
          'div',
          {
            key: round.id,
            class: { active: ctrl.data.id == round.id },
          },
          [
            h(
              'a.link',
              {
                attrs: { href: relay.roundPath(round) },
              },
              round.name,
            ),
            roundStateIcon(round),
            canContribute
              ? h('a.act', {
                  attrs: {
                    ...dataIcon(licon.Gear),
                    href: `/broadcast/round/${round.id}/edit`,
                  },
                })
              : null,
          ],
        ),
      )
      .concat(
        canContribute
          ? [
              h(
                'div.add',
                h(
                  'a.text',
                  {
                    attrs: {
                      href: `/broadcast/${relay.data.tour.id}/new`,
                      'data-icon': licon.PlusButton,
                    },
                  },
                  ctrl.trans.noarg('addRound'),
                ),
              ),
            ]
          : [],
      ),
  );
}
