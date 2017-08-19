import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import { bind, dataIcon } from '../../util';
import AnalyseCtrl from '../../ctrl';
import { shareButton } from '../studyView';

export default function(root: AnalyseCtrl): VNode {
  const study = root.study!,
  gb: Tree.Gamebook = root.node.gamebook || {};
  return h('div.study_buttons', [
    shareButton(study),
    h('div.gb_buttons', [
      gb.hint ? h('a.button.text', {
        attrs: dataIcon('')
      }, 'Get a hint') : null,
      h('a.button.text', {
        attrs: dataIcon('G')
      }, 'View the solution')
    ])
  ]);
}
