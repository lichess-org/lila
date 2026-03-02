import * as control from './control';
import type AnalyseCtrl from './ctrl';
import * as xhr from 'lib/xhr';
import { snabDialog } from 'lib/view';
import type { VNode } from 'snabbdom';
import { pubsub } from 'lib/pubsub';

export const keyToMouseEvent = (key: string, eventName: string, selector: string) =>
  window.site.mousetrap.bind(key, () =>
    $(selector).each(function (this: HTMLElement) {
      this.dispatchEvent(new MouseEvent(eventName));
    }),
  );

export const bind = (ctrl: AnalyseCtrl) => {
  addModifierKeyListeners(ctrl);
  const kbd = window.site.mousetrap;
  kbd
    .bind(['left', 'k'], () => {
      control.prev(ctrl);
      ctrl.redraw();
    })
    .bind(['right', 'j'], () => {
      control.next(ctrl);
      ctrl.redraw();
    })
    .bind(['up', '0', 'home'], e => {
      if (e.key === 'ArrowUp' && ctrl.fork.select('prev')) ctrl.setAutoShapes();
      else control.first(ctrl);
      ctrl.redraw();
    })
    .bind(['down', '$', 'end'], e => {
      if (e.key === 'ArrowDown' && ctrl.fork.select('next')) ctrl.setAutoShapes();
      else control.last(ctrl);
      ctrl.redraw();
    })
    .bind('shift+c', () => {
      ctrl.showComments = !ctrl.showComments;
      ctrl.treeView.requestAutoScroll('smooth');
      ctrl.redraw();
    })
    .bind('shift+i', () => {
      ctrl.treeView.toggleModePreference();
      ctrl.redraw();
    });
  kbd.bind('space', () => {
    const gb = ctrl.gamebookPlay();
    if (gb) gb.onSpace();
    else if (ctrl.practice || ctrl.study?.practice) return;
    else if (ctrl.cevalEnabled()) ctrl.playBestMove();
    else if (ctrl.isCevalAllowed() && ctrl.ceval.analysable) ctrl.cevalEnabled(!ctrl.cevalEnabled());
  });

  if (ctrl.study?.practice) return;

  kbd
    .bind('h', () => {
      ctrl.toggleActionMenu();
      ctrl.redraw();
    })
    .bind('f', ctrl.flip)
    .bind('?', () => {
      ctrl.keyboardHelp = !ctrl.keyboardHelp;
      if (ctrl.keyboardHelp) pubsub.emit('analysis.closeAll');
      ctrl.redraw();
    })
    .bind('l', () => {
      if (ctrl.isCevalAllowed() && ctrl.ceval.analysable) ctrl.cevalEnabled(!ctrl.cevalEnabled());
    })
    .bind('z', () => {
      ctrl.toggleFishnetAnalysis();
      ctrl.redraw();
    })
    .bind('a', () => {
      ctrl.showBestMoveArrowsProp(!ctrl.showBestMoveArrowsProp());
      ctrl.redraw();
    })
    .bind('v', () => {
      ctrl.toggleVariationArrows();
      ctrl.setAutoShapes();
      ctrl.redraw();
    })
    .bind('x', () => ctrl.toggleThreatMode())
    .bind('e', () => {
      ctrl.toggleExplorer();
      ctrl.redraw();
    });
  kbd
    .bind(['shift+left', 'shift+k'], () => {
      control.previousBranch(ctrl);
      ctrl.redraw();
    })
    .bind(['shift+right', 'shift+j'], () => {
      control.nextBranch(ctrl);
      ctrl.redraw();
    })
    .bind('shift+down', () => {
      ctrl.userJumpIfCan(ctrl.idbTree.stepLine(ctrl.path, 'next'), true);
      ctrl.redraw();
    })
    .bind('shift+up', () => {
      ctrl.userJumpIfCan(ctrl.idbTree.stepLine(ctrl.path, 'prev'), true);
      ctrl.redraw();
    });

  //First explorer move
  kbd.bind('shift+space', () => {
    const move = document
      .querySelector('.explorer-box:not(.loading) tbody tr[data-uci]')
      ?.getAttribute('data-uci');
    if (move) ctrl.explorerMove(move);
  });
};

export const view = (ctrl: AnalyseCtrl): VNode =>
  snabDialog({
    class: 'help.keyboard-help',
    htmlUrl: xhr.url('/analysis/help', { study: !!ctrl.study }),
    modal: true,
    onClose() {
      ctrl.keyboardHelp = false;
      ctrl.redraw();
    },
  });

function addModifierKeyListeners(ctrl: AnalyseCtrl) {
  let modifierOnly = false;

  window.addEventListener('mousedown', () => (modifierOnly = false), { capture: true });

  document.addEventListener('keydown', e => {
    if (e.key === 'Shift' || e.key === 'Control') modifierOnly = !modifierOnly;
    else modifierOnly = false;
  });

  document.addEventListener('keyup', e => {
    if (!modifierOnly) return;
    modifierOnly = false;
    const isShift = e.key === 'Shift' && !document.activeElement?.classList.contains('mchat__say');

    if (isShift && ctrl.fork.select('next')) ctrl.setAutoShapes();
    else if (e.key === 'Control') ctrl.toggleDiscloseOf();
    ctrl.redraw();
  });
}
