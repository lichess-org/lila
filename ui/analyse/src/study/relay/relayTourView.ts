import AnalyseCtrl from '../../ctrl';
import RelayCtrl from './relayCtrl';
import { dataIcon } from 'common/snabbdom';
import { h, VNode } from 'snabbdom';
import { innerHTML } from 'common/richText';
import { LeadPlayer, RelayRound } from './interfaces';
import { StudyCtrl } from '../interfaces';
import { view as multiBoardView } from '../multiBoard';
import { scrollToInnerSelector } from 'common';

export default function (ctrl: AnalyseCtrl): VNode | undefined {
  const study = ctrl.study;
  const relay = study?.relay;
  if (study && relay?.tourShow.active) {
    const round = relay?.currentRound();
    return h('div.relay-tour', [
      h('div.relay-tour__text', [
        h('h1', relay.data.tour.name),
        h('div.relay-tour__round', [
          h('strong', round.name),
          ' ',
          round.ongoing
            ? ctrl.trans.noarg('playingRightNow')
            : round.startsAt
            ? h(
                'time.timeago',
                {
                  hook: {
                    insert(vnode) {
                      (vnode.elm as HTMLElement).setAttribute('datetime', '' + round.startsAt);
                    },
                  },
                },
                lichess.timeago(round.startsAt)
              )
            : null,
        ]),
        relay.data.tour.markup
          ? h('div', {
              hook: innerHTML(relay.data.tour.markup, () => relay.data.tour.markup!),
            })
          : h('div', relay.data.tour.description),
        roundsTable(relay),
        relay.data.leaderboard?.length ? leaderboard(relay.data.leaderboard) : undefined,
      ]),
      study.looksNew() ? null : multiBoardView(study.multiBoard, study),
    ]);
  }
  return undefined;
}

const leaderboard = (players: LeadPlayer[]): VNode => {
  const withRating = players.find(p => p.rating);
  return h('div.relay-tour__text__leaderboard', [
    h('table.slist.slist-invert', [
      h(
        'thead',
        h('tr', [
          h('th', h('h2', 'Leaderboard')),
          withRating ? h('th', 'Elo') : undefined,
          h('th', 'Score'),
          h('th', 'Games'),
        ])
      ),
      h(
        'tbody',
        players.map(player =>
          h('tr', [
            h('th', player.name),
            withRating ? h('td', player.rating) : undefined,
            h('td', player.score),
            h('td', player.played),
          ])
        )
      ),
    ]),
  ]);
};

const roundsTable = (relay: RelayCtrl): VNode =>
  h('div.relay-tour__text__schedule', [
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
                round.name
              )
            ),
            h('td', round.startsAt ? lichess.dateFormat()(new Date(round.startsAt)) : undefined),
            h('td', roundStateIcon(round) || (round.startsAt ? lichess.timeago(round.startsAt) : undefined)),
          ])
        )
      )
    ),
  ]);

const roundStateIcon = (round: RelayRound) =>
  round.ongoing
    ? h('ongoing', { attrs: { ...dataIcon(''), title: 'Ongoing' } })
    : round.finished
    ? h('finished', { attrs: { ...dataIcon(''), title: 'Finished' } })
    : null;

export function rounds(ctrl: StudyCtrl): VNode {
  const canContribute = ctrl.members.canContribute();
  const relay = ctrl.relay!;
  return h(
    'div.study__relay__rounds',
    {
      hook: {
        update(vnode: VNode) {
          scrollToInnerSelector(vnode.elm as HTMLElement, '.active');
        },
      },
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
              round.name
            ),
            roundStateIcon(round),
            canContribute
              ? h('a.act', {
                  attrs: {
                    ...dataIcon(''),
                    href: `/broadcast/round/${round.id}/edit`,
                  },
                })
              : null,
          ]
        )
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
                      'data-icon': '',
                    },
                  },
                  ctrl.trans.noarg('addRound')
                )
              ),
            ]
          : []
      )
  );
}
