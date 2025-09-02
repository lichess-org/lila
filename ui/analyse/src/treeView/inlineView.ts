import type AnalyseCtrl from '../ctrl';
import { type VNode, type LooseVNodes, hl } from 'lib/snabbdom';
import { type Classes } from 'snabbdom';
import { ops as treeOps, path as treePath } from 'lib/tree/tree';
import { isSafari } from 'lib/device';
import { enrichText, innerHTML } from 'lib/richText';
import { authorText } from '../study/studyComments';
import { playable } from 'lib/game/game';
import type { Conceal } from '../interfaces';
import type { DiscloseState } from '../idbTree';
import { moveNodes, renderIndex } from '../view/components';

export function renderInlineView(ctrl: AnalyseCtrl): VNode {
  const renderer = new InlineView(ctrl);
  const parentNode = ctrl.tree.root;
  const parentDisclose = ctrl.idbTree.discloseOf(parentNode, true);
  return hl(
    'div.tview2.tview2-inline',
    { class: { hidden: ctrl.treeView.hidden, anchor: !!parentDisclose } },
    [
      renderer.commentNodes(parentNode),
      renderer.renderNodes(renderer.filterNodes(parentNode.children), {
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
  parentPath: Tree.Path;
  parentNode: Tree.Node;
  parentDisclose?: DiscloseState;
  conceal?: Conceal;
}

export class InlineView {
  readonly inline: boolean = true;
  private glyphs = ['good', 'mistake', 'brilliant', 'blunder', 'interesting', 'inaccuracy'];

  constructor(readonly ctrl: AnalyseCtrl) {}

  filterNodes(nodes: Tree.Node[]): Tree.Node[] {
    return nodes.filter(
      node => !node.comp || (this.ctrl.showFishnetAnalysis() && !this.ctrl.retro?.isSolving()),
    );
  }

  renderNodes([child, ...siblings]: Tree.Node[], args: Args): LooseVNodes {
    if (!child) return;
    const { isMainline, parentDisclose, parentPath } = args;
    return child.forceVariation && isMainline
      ? hl('interrupt', this.lines([child, ...siblings], args))
      : [
          this.moveNode(child, args),
          parentDisclose !== 'collapsed' && [
            this.commentNodes(child),
            siblings[0] && hl('interrupt', this.lines(siblings, args)),
          ],
          this.renderNodes(this.filterNodes(child.children), {
            parentPath: parentPath + child.id,
            parentNode: child,
            parentDisclose: this.ctrl.idbTree.discloseOf(child, true),
            isMainline,
          }),
        ];
  }

  commentNodes(node: Tree.Node, classes: Classes = {}): LooseVNodes[] {
    if (!this.ctrl.showComments || !node.comments) return [];
    return node.comments
      .map(comment =>
        this.commentNode(comment, node.comments!, {
          inaccuracy: comment.text.startsWith('Inaccuracy.'),
          mistake: comment.text.startsWith('Mistake.'),
          blunder: comment.text.startsWith('Blunder.'),
          ...classes,
        }),
      )
      .filter(Boolean);
  }

  protected lines(lines: Tree.Node[], args: Args, prepend?: LooseVNodes): LooseVNodes {
    const { parentDisclose, parentPath, parentNode, isMainline } = args;
    if (!lines.length || parentDisclose === 'collapsed') return;
    const anchor = parentDisclose === 'expanded' && (this.inline || !isMainline);
    const lineArgs = { parentPath, parentNode, isMainline: false };

    if ((!isMainline || this.inline) && this.inOrderLine(parentNode))
      return hl('inline', this.retroLine(lines[0]) || this.sidelineNodes(lines, lineArgs));
    else
      return hl('lines', { class: { anchor } }, [
        parentDisclose === 'expanded' && this.disclosureConnector(parentPath),
        prepend && hl('line', prepend),
        lines.map(
          line =>
            this.retroLine(line) ||
            hl('line', [parentDisclose && hl('branch'), this.sidelineNodes([line], lineArgs)]),
        ),
      ]);
  }

  private sidelineNodes([child, ...siblings]: Tree.Node[], args: Args): LooseVNodes {
    if (!child) return;
    const { parentDisclose, parentPath, parentNode } = args;
    const inOrder = this.inOrderLine(parentNode);
    const nodes = [
      this.moveNode(child, args),
      this.commentNodes(child),
      inOrder && this.lines(siblings, args),
      this.sidelineNodes(child.children, {
        isMainline: false,
        parentPath: parentPath + child.id,
        parentNode: child,
        parentDisclose: this.ctrl.idbTree.discloseOf(child, false),
      }),
      !inOrder && this.lines(siblings, args),
    ];
    return parentDisclose === 'expanded' ? hl('interrupt', nodes) : nodes;
  }

  private inOrderLine(node: Tree.Node): boolean {
    const [, second, third] = node.children;
    return !third && second && !treeOps.hasBranching(second, 6);
  }

  protected moveNode(node: Tree.Node, opts: Args): VNode {
    const { conceal, isMainline, parentPath, parentNode, parentDisclose } = opts;
    const { ctrl } = this;
    const p = parentPath + node.id;
    const currentPath =
      (!ctrl.synthetic && playable(ctrl.data) && ctrl.initialPath) ||
      ctrl.retro?.current()?.prev.path ||
      ctrl.study?.data.chapter.relayPath;
    const classes: Classes = {
      mainline: isMainline,
      conceal: conceal === 'conceal',
      hide: conceal === 'hide',
      active: p === ctrl.path,
      current: p === currentPath,
      nongame: !currentPath && !!ctrl.gamePath && treePath.contains(p, ctrl.gamePath) && p !== ctrl.gamePath,
      'context-menu': p === ctrl.contextMenuPath,
      'pending-deletion': p.startsWith(ctrl.pendingDeletionPath() || ' '),
      'pending-copy': !!ctrl.pendingCopyPath()?.startsWith(p),
    };
    if ((!!ctrl.study && !ctrl.study?.relay) || ctrl.showFishnetAnalysis())
      node.glyphs
        ?.map(g => this.glyphs[g.id - 1])
        .filter(Boolean)
        .forEach(cls => (classes[cls] = true));

    return hl('move', { attrs: { p }, class: classes }, [
      parentDisclose && this.disclosureBtn(parentNode, parentPath),
      (!isMainline || this.inline) &&
        (node.ply % 2 === 1 ||
          (parentNode.children.length > 1 &&
            (!this.inOrderLine(parentNode) || parentNode.children[0] !== node))) &&
        renderIndex(node.ply, true),
      moveNodes(
        node,
        ctrl.showFishnetAnalysis() && isMainline && !this.inline,
        (!!ctrl.study && !ctrl.study.relay) || ctrl.showFishnetAnalysis(),
      ),
    ]);
  }

  protected disclosureConnector(parentPath: Tree.Path): VNode {
    const callback = (vnode: VNode) => this.connectToDisclosureBtn(vnode, parentPath);

    return hl(
      'div.disclosure-connector',
      { hook: { insert: callback, update: v => setTimeout(() => callback(v)) } },
      hl('div.disclosure-connector.riser'),
    );
  }

  private disclosureBtn(node: Tree.Node, path: Tree.Path): VNode | undefined {
    return hl('a.disclosure', {
      class: { expanded: !node.collapsed },
      attrs: { 'data-path': path },
      on: { click: () => this.ctrl.idbTree.setCollapsed(path, !node.collapsed) },
    });
  }

  private connectToDisclosureBtn(v: VNode, path: Tree.Path): void {
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

  private findDisclosureBtn(el: Node | null | undefined, path: Tree.Path): HTMLElement | undefined {
    while (el && (el.nodeName !== 'A' || (el as HTMLElement).dataset.path !== path)) {
      if (!el.previousSibling) el = el.parentNode;
      else {
        el = el.previousSibling;
        while (el.lastChild) el = el.lastChild;
      }
    }
    return el as HTMLElement;
  }

  private commentNode(comment: Tree.Comment, others: Tree.Comment[], classes: Classes) {
    if (comment.by === 'lichess' && !this.ctrl.showFishnetAnalysis()) return;
    const by = !others[1] ? '' : `<span class="by">${authorText(comment.by)}</span> `,
      htmlHook = innerHTML(comment.text, text => by + enrichText(text));
    return hl('comment', { class: classes, hook: htmlHook });
  }

  private retroLine(node: Tree.Node): VNode | undefined {
    return node.comp && this.ctrl.retro && this.ctrl.retro.hideComputerLine(node)
      ? hl('comment', i18n.site.learnFromThisMistake)
      : undefined;
  }
}
