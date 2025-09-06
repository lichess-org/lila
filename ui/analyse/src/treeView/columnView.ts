import { type LooseVNodes, hl, type VNode } from 'lib/snabbdom';
import type AnalyseCtrl from '../ctrl';
import type { ConcealOf } from '../interfaces';
import { renderIndex } from '../view/components';
import { InlineView, type Args } from './inlineView';

export function renderColumnView(ctrl: AnalyseCtrl, concealOf: ConcealOf = () => () => null): VNode {
  const renderer = new ColumnView(ctrl, concealOf);
  const node = ctrl.tree.root;
  const commentTags = renderer.commentNodes(node);
  const blackStarts = (node.ply & 1) === 1;
  return hl('div.tview2.tview2-column', { class: { hidden: ctrl.treeView.hidden } }, [
    commentTags.length > 0 && hl('interrupt', commentTags),
    blackStarts && [renderIndex(node.ply, false), hl('move.empty', '...')],
    renderer.renderNodes(renderer.filterNodes(node.children), {
      parentPath: '',
      parentDisclose: ctrl.idbTree.discloseOf(node, true),
      parentNode: node,
      isMainline: true,
    }),
  ]);
}

class ColumnView extends InlineView {
  override readonly inline: boolean = false;
  constructor(
    ctrl: AnalyseCtrl,
    readonly concealOf: ConcealOf,
  ) {
    super(ctrl);
  }

  renderNodes([child, ...siblings]: Tree.Node[], opts: Args): LooseVNodes {
    if (!child) return;
    const { parentPath, parentDisclose } = opts;
    const childPath = parentPath + child.id;
    const conceal = opts.conceal ?? this.concealOf(true)(childPath, child);
    if (conceal === 'hide') return;
    const emptyMove = () => hl('move.empty', { class: { conceal: conceal === 'conceal' } }, '...');
    const isWhite = child.ply % 2 === 1;
    const comments = this.commentNodes(child, { conceal: conceal === 'conceal' });
    const interruptData = { class: { anchor: parentDisclose === 'expanded' } };
    return child.forceVariation
      ? hl('interrupt', interruptData, this.lines([child, ...siblings], opts))
      : [
          isWhite && renderIndex(child.ply, false),
          this.moveNode(child, { ...opts, conceal }),
          parentDisclose !== 'collapsed' &&
            (siblings.length > 0 || comments.length > 0) && [
              isWhite && emptyMove(),
              hl('interrupt', interruptData, [
                comments,
                siblings.length > 0
                  ? this.lines(siblings, opts)
                  : parentDisclose && this.disclosureConnector(parentPath),
              ]),
              isWhite && child.children.length > 0 && [renderIndex(child.ply, false), emptyMove()],
            ],
          this.renderNodes(this.filterNodes(child.children), {
            parentPath: childPath,
            parentNode: child,
            parentDisclose: this.ctrl.idbTree.discloseOf(child, true),
            isMainline: true,
          }),
        ];
  }
}
