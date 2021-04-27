import AnalyseCtrl from '../../ctrl';
import { h, VNode } from 'snabbdom';
import { dataIcon, iconTag, innerHTML } from '../../util';
import { view as multiBoardView } from '../multiBoard';
import { StudyCtrl } from '../interfaces';

export default function (ctrl: AnalyseCtrl): VNode | undefined {
  const study = ctrl.study;
  const relay = study && study.relay;
  if (study && relay?.tourShow.active)
    return h('div.relay-tour', [
      h('div.relay-tour__text', [
        h('h1', study.data.name),
        relay.data.tour.markup
          ? h('div', {
              hook: innerHTML(relay.data.tour.markup, () => relay.data.tour.markup!),
            })
          : h('div', relay.data.tour.description),
      ]),
      multiBoardView(study.multiBoard, study),
    ]);
  return undefined;
}

export function rounds(ctrl: StudyCtrl): VNode {
  const canContribute = ctrl.members.canContribute();
  return h(
    'div.study__relay__rounds',
    ctrl
      .relay!.data.rounds.map(round =>
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
                attrs: { href: round.path },
              },
              round.name
            ),
            round.ongoing
              ? h('ongoing', { attrs: { ...dataIcon('J'), title: 'Ongoing' } })
              : round.finished
              ? h('finished', { attrs: { ...dataIcon('E'), title: 'Finished' } })
              : null,
            canContribute
              ? h('a.act', {
                  attrs: {
                    ...dataIcon('%'),
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
                      href: `/broadcast/${ctrl.relay!.data.tour.id}/new`,
                      'data-icon': 'O',
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
