// import { h } from 'snabbdom'
import AnalyseController from '../../ctrl';
import { VNode } from 'snabbdom/vnode'
import renderEditor from './editor/gamebookEditorView';
import renderPlayer from './player/gamebookPlayerView';

export function view(root: AnalyseController): VNode | undefined {
  if (!isGamebook(root)) return;
  if (isEditor(root)) return renderEditor(root);
  return renderPlayer(root);
}

export function isEditor(root: AnalyseController): boolean {
  return isGamebook(root) && root.study!.members.canContribute();
}

export function isPlayer(root: AnalyseController): boolean {
  return isGamebook(root) && !isEditor(root);
}

export function isGamebook(root: AnalyseController): boolean {
  return !!root.study && root.study.data.chapter.gamebook;
}
