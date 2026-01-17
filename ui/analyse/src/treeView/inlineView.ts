import type AnalyseCtrl from '../ctrl';
import { type VNode, type LooseVNodes, hl } from 'lib/view';
import type { Classes, Hooks } from 'snabbdom';
import { ops as treeOps, path as treePath } from 'lib/tree/tree';
import { isSafari } from 'lib/device';
import { enrichText, innerHTML } from 'lib/richText';
import { authorText } from '../study/studyComments';
import { playable } from 'lib/game';
import type { Conceal } from '../interfaces';
import type { DiscloseState } from '../idbTree';
import { renderMoveNodes, renderIndex } from '../view/components';
import type { TreeComment, TreeNode, TreePath } from 'lib/tree/types';

export function renderInlineView(ctrl: AnalyseCtrl): VNode {
  const renderer = new InlineView(ctrl);
  const parentNode = ctrl.tree.root;
  const parentDisclose = ctrl.idbTree.discloseOf(parentNode, true);
  return hl(
    'div.tview2.tview2-inline',
    { class: { hidden: ctrl.treeView.hidden, anchor: !!parentDisclose } },
    [
      renderer.commentNodes(parentNode),
      renderer.renderNodes(ctrl.visibleChildren(parentNode), {
        parentPath: '',
        parentNode,
        parentDisclose,
        isMainline: true,
      }),
    ],
  );
}

export interface Args {
  isMainline: boolean;
  parentPath: TreePath;
  parentNode: TreeNode;
  parentDisclose?: DiscloseState;
  parenthetical?: boolean;
  conceal?: Conceal;
}

export class InlineView {
  readonly inline: boolean = true;
  private glyphs = ['good', 'mistake', 'brilliant', 'blunder', 'interesting', 'inaccuracy'];

  constructor(readonly ctrl: AnalyseCtrl) {}

  renderNodes([child, ...siblings]: TreeNode[], args: Args): LooseVNodes {
    if (!child) return;
    const { isMainline, parentDisclose } = args;
    return child.forceVariation && isMainline
      ? hl('interrupt', this.lines([child, ...siblings], args))
      : [
          this.moveNode(child, args),
          parentDisclose !== 'collapsed' && [
            this.commentNodes(child),
            siblings[0] && hl('interrupt', this.lines(siblings, args)),
          ],
          this.renderNodes(this.ctrl.visibleChildren(child), this.childArgs(child, args, true)),
        ];
  }

  commentNodes(node: TreeNode, classes: Classes = {}): LooseVNodes[] {
    if (!this.ctrl.showComments || !node.comments) return [];
    return node.comments
      .map(comment =>
        this.ctrl.retro?.hideComputerLine(node)
          ? hl('comment', i18n.site.learnFromThisMistake)
          : (!this.isFishnetComment(comment) || this.ctrl.showFishnetAnalysis()) &&
            hl('comment', {
              class: {
                inaccuracy: comment.text.startsWith('Inaccuracy.'),
                mistake: comment.text.startsWith('Mistake.'),
                blunder: comment.text.startsWith('Blunder.'),
                ...classes,
              },
              hook: innerHTML(comment.text, text =>
                node.comments?.[1]
                  ? `<span class="by">${authorText(comment.by)}</span> ` + enrichText(text)
                  : enrichText(text),
              ),
            }),
      )
      .filter(Boolean);
  }

  private isFishnetComment(comment: TreeComment): boolean {
    return comment.by === 'lichess' && comment.text.endsWith(' was best.');
  }

  protected lines(lines: TreeNode[], args: Args): LooseVNodes {
    const { parentDisclose, parentPath, parentNode, isMainline } = args;
    if (!lines.length || parentDisclose === 'collapsed') return;
    const anchor = parentDisclose === 'expanded' && (this.inline || !isMainline);
    const lineArgs = { parentPath, parentNode, isMainline: false };

    return (!isMainline || this.inline) && args.parenthetical
      ? hl('inline', this.sidelineNodes(lines, lineArgs))
      : hl('lines', { class: { anchor } }, [
          parentDisclose === 'expanded' && this.disclosureConnector(parentPath),
          lines.map(line =>
            hl('line', [parentDisclose && hl('branch'), this.sidelineNodes([line], lineArgs)]),
          ),
        ]);
  }

  private sidelineNodes([child, ...siblings]: TreeNode[], args: Args): LooseVNodes {
    if (!child) return;
    const childArgs = this.childArgs(child, args, false);
    const sideline = [
      this.moveNode(child, args),
      this.commentNodes(child),
      args.parenthetical && this.lines(siblings, args),
      this.ctrl.disclosureMode() || child.children.length < 2 || childArgs.parenthetical
        ? this.sidelineNodes(child.children, childArgs)
        : this.lines(child.children, childArgs),
      !args.parenthetical && this.lines(siblings, args),
    ];
    return this.ctrl.disclosureMode() && args.parentDisclose === 'expanded'
      ? hl('interrupt', sideline)
      : sideline;
  }

  private childArgs(child: TreeNode, args: Args, isMainline = false) {
    return {
      isMainline,
      parentPath: args.parentPath + child.id,
      parentNode: child,
      parentDisclose: this.ctrl.idbTree.discloseOf(child, false),
      parenthetical: this.parenthetical(child),
    };
  }

  private parenthetical(node: TreeNode): boolean {
    const [, second, third] = node.children;
    return !third && second && !treeOps.hasBranching(second, 6);
  }

  protected moveNode(node: TreeNode, args: Args): LooseVNodes {
    const { conceal, isMainline, parentPath, parentNode, parentDisclose, parenthetical } = args;
    const { ctrl } = this;
    const path = parentPath + node.id;
    const currentPath =
      (!ctrl.synthetic && playable(ctrl.data) && ctrl.initialPath) ||
      ctrl.retro?.current()?.prev.path ||
      ctrl.study?.data.chapter.relayPath;
    const withIndex =
      (!isMainline || this.inline) &&
      (node.ply % 2 === 1 ||
        (!isMainline &&
          parentNode.children.length > 1 &&
          (!parenthetical || parentNode.children[0] !== node))); // ugh
    const classes: Classes = {
      mainline: isMainline,
      conceal: conceal === 'conceal',
      hide: conceal === 'hide',
      active: path === ctrl.path,
      current: path === currentPath,
      nongame:
        !currentPath && !!ctrl.gamePath && treePath.contains(path, ctrl.gamePath) && path !== ctrl.gamePath,
      'context-menu': path === ctrl.contextMenuPath,
      'pending-deletion': path.startsWith(ctrl.pendingDeletionPath() || ' '),
      'pending-copy': !!ctrl.pendingCopyPath()?.startsWith(path),
    };
    if (ctrl.showMoveGlyphs())
      node.glyphs
        ?.map(g => this.glyphs[g.id - 1])
        .filter(Boolean)
        .forEach(cls => (classes[cls] = true));
    return hl('move', { attrs: { p: path }, class: classes }, [
      parentDisclose && this.disclosureBtn(parentNode, parentPath),
      withIndex && renderIndex(node.ply, true),
      renderMoveNodes(
        node,
        isMainline && !this.inline,
        ctrl.showMoveGlyphs(),
        ctrl.allowedEval(node) || false,
      ),
    ]);
  }

  protected disclosureConnector(parentPath: TreePath): LooseVNodes {
    const callback = (vnode: VNode) => this.connectToDisclosureBtn(vnode, parentPath);
    const hook: Hooks = { insert: callback, update: v => setTimeout(() => callback(v)) };
    return (
      this.ctrl.disclosureMode() &&
      hl('div.disclosure-connector', { hook }, hl('div.disclosure-connector.riser'))
    );
  }

  private disclosureBtn(node: TreeNode, path: TreePath): LooseVNodes {
    return (
      this.ctrl.disclosureMode() &&
      hl('a.disclosure', {
        class: { expanded: !node.collapsed },
        attrs: { 'data-path': path },
        on: { click: () => this.ctrl.idbTree.setCollapsed(path, !node.collapsed) },
      })
    );
  }

  private connectToDisclosureBtn(v: VNode, path: TreePath): void {
    const [el, btn] = [v.elm as HTMLElement, this.findDisclosureBtn(v.elm, path)];
    if (!el || !btn || isSafari({ below: '16' })) return;

    const btnRect = btn.getBoundingClientRect();
    const anchorRect = el.closest('.anchor')!.getBoundingClientRect();
    const isFirstOnRow = btnRect.left < anchorRect.left + 8;
    const distanceMinusRiser = anchorRect.top - btnRect.bottom + btnRect.height / 2;
    const baseHeight = Math.max(0, distanceMinusRiser - (isFirstOnRow ? 0 : 12));
    const btnCenter = btnRect.width / 2;
    const width = isFirstOnRow
      ? 3
      : document.documentElement.dir === 'rtl'
        ? anchorRect.right - btnRect.left - btnCenter
        : btnRect.right - anchorRect.left - btnCenter;

    el.style.width = `${width}px`;
    el.style.height = `${baseHeight}px`;
    el.style.top = `-${baseHeight}px`;
    (el.firstElementChild as HTMLElement).style.display = isFirstOnRow ? 'none' : 'block';
  }

  private findDisclosureBtn(el: Node | null | undefined, path: TreePath): HTMLElement | undefined {
    while (el && (el.nodeName !== 'A' || (el as HTMLElement).dataset.path !== path)) {
      if (!el.previousSibling) el = el.parentNode;
      else {
        el = el.previousSibling;
        while (el.lastChild) el = el.lastChild;
      }
    }
    return el as HTMLElement;
  }
}
