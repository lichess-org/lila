import { h } from 'snabbdom';
import { VNode } from 'snabbdom/vnode';
import AnalyseCtrl from '../../ctrl';
import { innerHTML, enrichText } from '../../util';
import { view as multiBoardView } from '../multiBoard';
import { view as keyboardView } from '../../keyboard';
import * as studyView from '../studyView';

export default function(ctrl: AnalyseCtrl): VNode | undefined {
  const study = ctrl.study;
  const relay = study && study.relay;
  if (study && relay && relay.intro.active) return h('div.intro', [
    h('div.content_box', [
      h('h1', study.data.name),
      h('div', {
        hook: innerHTML(relay.data.description || '', function(t) { return enrichText(t, false) })
      })
    ]),
    ctrl.keyboardHelp ? keyboardView(ctrl) : null,
    ctrl.study ? studyView.overboard(ctrl.study) : null,
    multiBoardView(study.multiBoard, study)
  ]);
}
