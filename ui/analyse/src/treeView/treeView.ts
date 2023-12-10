import AnalyseCtrl from '../ctrl';
import column from './columnView';
import inline from './inlineView';
import isCol1 from 'common/isCol1';
import { VNode } from 'snabbdom';
import { ConcealOf } from '../interfaces';
import { storedProp, StoredProp } from 'common/storage';

export type TreeViewKey = 'column' | 'inline';

export class TreeView {
  value: StoredProp<TreeViewKey>;

  constructor(initialValue: TreeViewKey = 'column') {
    this.value = storedProp<TreeViewKey>(
      'treeView',
      initialValue,
      str => str as TreeViewKey,
      v => v,
    );
  }
  inline = () => this.value() === 'inline';
  set = (inline: boolean) => this.value(inline ? 'inline' : 'column');
  toggle = () => this.set(!this.inline());
}

// entry point, dispatching to selected view
export const render = (ctrl: AnalyseCtrl, concealOf?: ConcealOf): VNode =>
  (ctrl.treeView.inline() || isCol1()) && !concealOf ? inline(ctrl) : column(ctrl, concealOf);
