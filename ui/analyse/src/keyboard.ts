import * as control from './control';
import AnalyseCtrl from './ctrl';
import * as xhr from 'common/xhr';
import { snabDialog } from 'common/dialog';
import { VNode } from 'snabbdom';

export const bind = (ctrl: AnalyseCtrl) => {
  let shiftAlone = 0;
  document.addEventListener('keydown', e => e.key === 'Shift' && (shiftAlone = e.location));
  document.addEventListener('keyup', e => {
    if (e.key === 'Shift' && e.location === shiftAlone) {
      if (shiftAlone === 1 && ctrl.fork.prev()) ctrl.setAutoShapes();
      else if (shiftAlone === 2 && ctrl.fork.next()) ctrl.setAutoShapes();
      else if (shiftAlone === 0) return;
      ctrl.redraw();
    }
    shiftAlone = 0;
  });
  const kbd = window.site.mousetrap;
  kbd
    .bind(['left', 'k'], () => {
      control.prev(ctrl);
      ctrl.redraw();
    })
    .bind(['shift+left', 'shift+k'], () => {
      control.previousBranch(ctrl);
      ctrl.redraw();
    })
    .bind(['shift+right', 'shift+j'], () => {
      control.nextBranch(ctrl);
      ctrl.redraw();
    })
    .bind(['right', 'j'], () => {
      control.next(ctrl);
      ctrl.redraw();
    })
    .bind(['up', '0', 'home'], e => {
      if (e.key === 'ArrowUp' && ctrl.fork.prev()) ctrl.setAutoShapes();
      else control.first(ctrl);
      ctrl.redraw();
    })
    .bind(['down', '$', 'end'], e => {
      if (e.key === 'ArrowDown' && ctrl.fork.next()) ctrl.setAutoShapes();
      else control.last(ctrl);
      ctrl.redraw();
    })
    .bind('shift+c', () => {
      ctrl.showComments = !ctrl.showComments;
      ctrl.autoScroll();
      ctrl.redraw();
    })
    .bind('shift+i', () => {
      ctrl.treeView.toggle();
      ctrl.redraw();
    });

  kbd.bind('space', () => {
    const gb = ctrl.gamebookPlay();
    if (gb) gb.onSpace();
    else if (ctrl.practice || ctrl.studyPractice) return;
    else if (ctrl.ceval.enabled()) ctrl.playBestMove();
    else ctrl.toggleCeval();
  });

  if (ctrl.studyPractice) return;

  kbd
    .bind('f', ctrl.flip)
    .bind('?', () => {
      ctrl.keyboardHelp = !ctrl.keyboardHelp;
      if (ctrl.keyboardHelp) site.pubsub.emit('analyse.close-all');
      ctrl.redraw();
    })
    .bind('l', ctrl.toggleCeval)
    .bind('z', () => {
      ctrl.toggleComputer();
      ctrl.redraw();
    })
    .bind('a', () => {
      ctrl.toggleAutoShapes(!ctrl.showAutoShapes());
      ctrl.redraw();
    })
    .bind('v', () => {
      ctrl.toggleVariationArrows();
      ctrl.redraw();
    })
    .bind('x', ctrl.toggleThreatMode)
    .bind('e', () => {
      ctrl.toggleExplorer();
      ctrl.redraw();
    });

  const keyToMouseEvent = (key: string, eventName: string, selector: string) =>
    kbd.bind(key, () =>
      $(selector).each(function (this: HTMLElement) {
        this.dispatchEvent(new MouseEvent(eventName));
      }),
    );

  //'Request computer analysis' & 'Learn From Your Mistakes' (mutually exclusive)
  keyToMouseEvent(
    'r',
    'click',
    '.analyse__underboard__panels .computer-analysis button, .analyse__round-training .advice-summary a.button',
  );
  //'Next' button ("in Learn From Your Mistake")
  keyToMouseEvent('enter', 'click', '.analyse__tools .training-box a.continue');

  //First explorer move
  kbd.bind('shift+space', () => {
    const move = document
      .querySelector('.explorer-box:not(.loading) .moves tbody tr')
      ?.attributes.getNamedItem('data-uci')?.value;
    if (move) ctrl.explorerMove(move);
  });

  [
    ['b', '??'],
    ['m', '?'],
    ['i', '?!'],
  ].forEach(([key, symbol]) => kbd.bind(key, () => ctrl.jumpToGlyphSymbol(ctrl.bottomColor(), symbol)));

  if (ctrl.study) {
    keyToMouseEvent('d', 'mousedown', '.study__buttons .comments');
    keyToMouseEvent('g', 'mousedown', '.study__buttons .glyphs');

    // navigation for next and prev chapters
    kbd.bind('p', ctrl.study.goToPrevChapter);
    kbd.bind('n', ctrl.study.goToNextChapter);
    // ! ? !! ?? !? ?!
    for (let i = 1; i < 7; i++) kbd.bind(i.toString(), () => ctrl.study?.glyphForm.toggleGlyph(i));
    // = ∞ ⩲ ⩱ ± ∓ +- -+
    for (let i = 1; i < 9; i++)
      kbd.bind(`shift+${i}`, () => ctrl.study?.glyphForm.toggleGlyph(i == 1 ? 10 : 11 + i));
  }
};

export function view(ctrl: AnalyseCtrl): VNode {
  return snabDialog({
    class: 'help.keyboard-help',
    htmlUrl: xhr.url('/analysis/help', { study: !!ctrl.study }),
    onClose() {
      ctrl.keyboardHelp = false;
      ctrl.redraw();
    },
  });
}
