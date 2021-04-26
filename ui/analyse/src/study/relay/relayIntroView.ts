import { h, VNode } from 'snabbdom';
import AnalyseCtrl from '../../ctrl';
import { innerHTML } from '../../util';
import { view as multiBoardView } from '../multiBoard';

export default function (ctrl: AnalyseCtrl): VNode | undefined {
  const study = ctrl.study;
  const relay = study && study.relay;
  if (study && relay && relay.tourShow.active)
    return h('div.intro', [
      h('div.intro__text', [
        h('h1', study.data.name),
        h('div', {
          hook: innerHTML(relay.data.tour.markup, () => relay.data.tour.markup!),
        }),
      ]),
      multiBoardView(study.multiBoard, study),
    ]);
  return undefined;
}
