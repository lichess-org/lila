import { StoredProp, storedProp } from 'common/storage';
import { VNode } from 'snabbdom';
import AnalyseCtrl from '../ctrl';
import { ConcealOf } from '../interfaces';
import column from './columnView';
import inline from './inlineView';

export type TreeViewKey = 'column' | 'inline';

export interface TreeView {
  get: StoredProp<TreeViewKey>;
  set(inline: boolean): void;
  toggle(): void;
  inline(): boolean;
}

export function ctrl(initialValue: TreeViewKey = 'column'): TreeView {
  const value = storedProp<TreeViewKey>('treeView', initialValue);
  function inline() {
    return value() === 'inline';
  }
  function set(i: boolean) {
    value(i ? 'inline' : 'column');
  }
  return {
    get: value,
    set,
    toggle() {
      set(!inline());
    },
    inline,
  };
}

// entry point, dispatching to selected view
export function render(ctrl: AnalyseCtrl, concealOf?: ConcealOf): VNode {
  return (ctrl.treeView.inline() || window.lishogi.isCol1()) && !concealOf ? inline(ctrl) : column(ctrl, concealOf);
}
