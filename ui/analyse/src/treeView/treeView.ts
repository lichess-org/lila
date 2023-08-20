import AnalyseCtrl from '../ctrl';
import column from './columnView';
import inline from './inlineView';
import isCol1 from 'common/isCol1';
import { VNode } from 'snabbdom';
import { ConcealOf } from '../interfaces';
import { storedProp, StoredProp } from 'common/storage';

export type TreeViewKey = 'column' | 'inline';

export interface TreeView {
  get: StoredProp<TreeViewKey>;
  set(inline: boolean): void;
  toggle(): void;
  inline(): boolean;
}

export function ctrl(initialValue: TreeViewKey = 'column'): TreeView {
  const value = storedProp<TreeViewKey>(
    'treeView',
    initialValue,
    str => str as TreeViewKey,
    v => v,
  );
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
  return (ctrl.treeView.inline() || isCol1()) && !concealOf ? inline(ctrl) : column(ctrl, concealOf);
}
