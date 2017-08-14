import { h } from 'snabbdom'
import AnalyseController from '../../ctrl';
import { StudyController } from '../interfaces';
import { VNode } from 'snabbdom/vnode'

export function view(root: AnalyseController): VNode | undefined {
  const study = root.study;
  if (!study || !study.data.chapter.gamebook) return;
  if (study.members.canContribute()) return builder(root, study);
}

function builder(root: AnalyseController, study: StudyController): VNode {

  return h('div.gamebook.builder', 'builder here');
}
