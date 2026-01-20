import * as licon from 'lib/licon';
import { type VNode, onInsert, hl } from 'lib/view';
import type AnalyseCtrl from '../ctrl';
import * as studyView from '../study/studyView';
import { patch, nodeFullName } from '../view/util';
import { renderVariationPgn } from '../pgnExport';
import { isTouchDevice } from 'lib/device';
import type { TreePath } from 'lib/tree/types';

export function renderContextMenu(e: MouseEvent, ctrl: AnalyseCtrl, path: TreePath): void {
  let pos = getPosition(e);
  if (pos === null) {
    if (ctrl.contextMenuPath) return;
    pos = { x: 0, y: 0 };
  }

  const el = ($('#' + elementId)[0] ||
    $('<div id="' + elementId + '">').appendTo($('body'))[0]) as HTMLElement;
  ctrl.contextMenuPath = path;
  function close(e: MouseEvent) {
    if (e.button === 2) return; // right click
    ctrl.contextMenuPath = undefined;
    document.removeEventListener('click', close, false);
    $('#' + elementId).removeClass('visible');
    ctrl.redraw();
  }
  document.addEventListener('click', close, false);
  el.innerHTML = '';
  patch(el, view(ctrl, path, pos));
}

interface Coords {
  x: number;
  y: number;
}

interface PageOrClientPos {
  pageX?: number;
  pageY?: number;
  clientX?: number;
  clientY?: number;
}

const elementId = 'analyse-cm';

function getPosition(e: MouseEvent | TouchEvent): Coords | null {
  let pos = e as PageOrClientPos;
  if ('touches' in e && e.touches.length > 0) pos = e.touches[0];
  if (pos.pageX || pos.pageY) return { x: pos.pageX!, y: pos.pageY! };
  else if (pos.clientX || pos.clientY)
    return {
      x: pos.clientX! + document.body.scrollLeft + document.documentElement.scrollLeft,
      y: pos.clientY! + document.body.scrollTop + document.documentElement.scrollTop,
    };
  else return null;
}

function positionMenu(menu: HTMLElement, coords: Coords): void {
  const menuWidth = menu.offsetWidth + 4,
    menuHeight = menu.offsetHeight + 4,
    windowWidth = window.innerWidth,
    windowHeight = window.innerHeight;

  menu.style.left =
    windowWidth - coords.x < menuWidth ? windowWidth - menuWidth + 'px' : (menu.style.left = coords.x + 'px');

  menu.style.top =
    windowHeight - coords.y < menuHeight
      ? windowHeight - menuHeight + 'px'
      : (menu.style.top = coords.y + 'px');
}

function action(
  icon: string,
  text: string,
  onClick: () => void,
  onHover?: () => void,
  onLeave?: () => void,
): VNode {
  return hl(
    'a',
    {
      attrs: { 'data-icon': icon },
      hook: {
        insert: vnode => {
          const elm = vnode.elm as HTMLElement;
          elm.addEventListener('click', onClick);
          if (onHover && !isTouchDevice())
            elm.addEventListener('mouseover', () => {
              onHover();
              // If there is a special action for hover, make the menu transparent so that effects
              // on the move list can be fully seen:
              $('#' + elementId).addClass('transparent');
            });
          if (onLeave)
            elm.addEventListener('mouseout', () => {
              onLeave();
              $('#' + elementId).removeClass('transparent');
            });
        },
      },
    },
    text,
  );
}

function view(ctrl: AnalyseCtrl, path: TreePath, coords: Coords): VNode {
  const { tree, idbTree } = ctrl;
  const node = tree.nodeAtPath(path),
    onMainline = tree.pathIsMainline(path) && !tree.pathIsForcedVariation(path),
    extendedPath = tree.extendPath(path, onMainline);
  let canPromote = !onMainline;
  for (let iter = tree.lastMainlineNode(path).children[1]; canPromote && iter; iter = iter.children[0]) {
    if (iter === node) canPromote = false;
  }

  return hl(
    'div#' + elementId + '.visible',
    {
      hook: {
        ...onInsert(elm => {
          elm.addEventListener('contextmenu', e => (e.preventDefault(), false));
          positionMenu(elm, coords);
        }),
        postpatch: (_, vnode) => positionMenu(vnode.elm as HTMLElement, coords),
      },
    },
    [
      hl('p.title', nodeFullName(node)),

      canPromote && action(licon.UpTriangle, i18n.site.promoteVariation, () => ctrl.promote(path, false)),

      !onMainline && action(licon.Checkmark, i18n.site.makeMainLine, () => ctrl.promote(path, true)),

      path && ctrl.study && studyView.contextMenu(ctrl.study, path, node),

      path &&
        onMainline &&
        action(licon.InternalArrow, i18n.site.forceVariation, () => ctrl.forceVariation(path, true)),

      idbTree.someCollapsedOf(false) &&
        action(licon.MinusButton, 'Collapse all', () => idbTree.setCollapsedFrom('', true)),

      idbTree.someCollapsedOf(true) &&
        action(licon.PlusButton, 'Expand all', () => idbTree.setCollapsedFrom('', false)),

      action(
        licon.Clipboard,
        onMainline ? i18n.site.copyMainLinePgn : i18n.site.copyVariationPgn,
        () =>
          navigator.clipboard.writeText(
            renderVariationPgn(ctrl.data.game, ctrl.tree.getNodeList(extendedPath)),
          ),
        () => ctrl.pendingCopyPath(extendedPath),
        () => ctrl.pendingCopyPath(null),
      ),

      !ctrl.moveRangeEnd() &&
        onMainline &&
        action(licon.Target, ctrl.moveRangeStart() ? i18n.site.setRangeEnd : i18n.site.setRangeStart, () =>
          ctrl.setRangePoint(path),
        ),

      (ctrl.moveRangeStart() || ctrl.moveRangeEnd()) &&
        action(licon.X, i18n.site.clearRange, () => {
          ctrl.moveRangeStart(null);
          ctrl.moveRangeEnd(null);
        }),

      path &&
        action(
          licon.Trash,
          i18n.site.deleteFromHere,
          () => ctrl.deleteNode(path),
          () => ctrl.pendingDeletionPath(path),
          () => ctrl.pendingDeletionPath(null),
        ),
    ],
  );
}
