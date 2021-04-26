import { h, VNode } from 'snabbdom';
import AnalyseCtrl from '../../ctrl';
import { innerHTML } from '../../util';
import { view as multiBoardView } from '../multiBoard';

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
  return h('div.study__relay__rounds', 'rounds!');
}
