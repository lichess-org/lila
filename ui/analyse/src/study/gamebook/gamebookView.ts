import AnalyseCtrl from '../../ctrl';
import { VNode } from 'snabbdom/vnode'
import renderEditor from './gamebookEditor';
import renderPlayer from './player/gamebookPlayerView';

export interface GamebookView {
  isEditor: boolean;
  view: VNode
}

export function view(root: AnalyseCtrl): GamebookView | undefined {
  if (!isGamebook(root)) return;
  const editor = isEditor(root);
  return {
    isEditor: editor,
    view: editor ? renderEditor(root) : renderPlayer(root)
  };
}

export function isEditor(root: AnalyseCtrl): boolean {
  return isGamebook(root) && root.study!.members.canContribute();
}

export function isPlayer(root: AnalyseCtrl): boolean {
  return isGamebook(root) && !isEditor(root);
}

export function isGamebook(root: AnalyseCtrl): boolean {
  return !!root.study && root.study.data.chapter.gamebook;
}
