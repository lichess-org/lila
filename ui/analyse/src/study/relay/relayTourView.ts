import AnalyseCtrl from '../../ctrl';
import { h, VNode } from 'snabbdom';
import { dataIcon, innerHTML } from '../../util';
import { view as multiBoardView } from '../multiBoard';
import { StudyCtrl } from '../interfaces';

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
      ]),
      study.multiBoard.looksEmpty() ? null : multiBoardView(study.multiBoard, study),
    ]);
  }
  return undefined;
}

export function rounds(ctrl: StudyCtrl): VNode {
  const canContribute = ctrl.members.canContribute();
  const relay = ctrl.relay!;
  return h(
    'div.study__relay__rounds',
    relay!.data.rounds
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
            round.ongoing
              ? h('ongoing', { attrs: { ...dataIcon(''), title: 'Ongoing' } })
              : round.finished
              ? h('finished', { attrs: { ...dataIcon(''), title: 'Finished' } })
              : null,
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
