// import { h } from 'snabbdom'
import AnalyseController from '../../ctrl';
import { StudyController } from '../interfaces';
import { VNode } from 'snabbdom/vnode'
import renderEditor from './editor/gamebookEditorView';

export function view(root: AnalyseController): VNode | undefined {
  const study: StudyController = root.study;
  if (!study || !study.data.chapter.gamebook) return;
  if (study.members.canContribute()) return renderEditor(root);
}
