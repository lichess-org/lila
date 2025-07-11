import type AnalyseCtrl from '../ctrl';
import { renderColumnView as column } from './columnView';
import { renderInlineView as inline } from './inlineView';
import { displayColumns } from 'lib/device';
import type { VNode } from 'snabbdom';
import type { ConcealOf } from '../interfaces';
import { storedProp, type StoredProp } from 'lib/storage';

export type TreeViewKey = 'column' | 'inline';

export class TreeView {
  value: StoredProp<TreeViewKey>;
  hidden = true;
  private treeVersion = 1;

  constructor(initialValue: TreeViewKey = 'column') {
    this.value = storedProp<TreeViewKey>(
      'treeView',
      initialValue,
      str => str as TreeViewKey,
      v => v,
    );
  }
  inline = () => this.value() === 'inline';
  toggle = () => this.set(!this.inline());
  set = (inline: boolean) => {
    this.value(inline ? 'inline' : 'column');
    this.update();
  };
  update = () => {
    this.treeVersion++;
  };
  get version() {
    return this.treeVersion;
  }
}

// entry point, dispatching to selected view
export const render = (ctrl: AnalyseCtrl, concealOf?: ConcealOf): VNode =>
  (ctrl.treeView.inline() || displayColumns() === 1) && !concealOf ? inline(ctrl) : column(ctrl, concealOf);
