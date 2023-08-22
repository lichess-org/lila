import * as control from './control';
import * as xhr from 'common/xhr';
import { isTouchDevice } from 'common/mobile';
import AnalyseCtrl from './ctrl';
import { h, VNode } from 'snabbdom';
import { snabModal } from 'common/modal';
import { spinnerVdom as spinner } from 'common/spinner';

export const bind = (ctrl: AnalyseCtrl) => {
  document.addEventListener('keydown', (e: KeyboardEvent) => {
    if (e.key !== 'Shift') return;
    if ((e.location === 1 && ctrl.fork.prev()) || (e.location === 2 && ctrl.fork.next())) {
      ctrl.setAutoShapes();
      ctrl.redraw();
    }
  });
  const kbd = window.lichess.mousetrap;
  kbd
    .bind(['left', 'k'], () => {
      control.prev(ctrl);
      ctrl.redraw();
    })
    .bind(['shift+left', 'shift+k'], () => {
      control.exitVariation(ctrl);
      ctrl.redraw();
    })
    .bind(['shift+right', 'shift+j'], () => {})
    .bind(['right', 'j'], () => {
      control.next(ctrl);
      ctrl.redraw();
    })
    .bind(['up', '0', 'home'], () => {
      control.first(ctrl);
      ctrl.redraw();
    })
    .bind(['down', '$', 'end'], () => {
      control.last(ctrl);
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
      if (ctrl.keyboardHelp) lichess.pubsub.emit('analyse.close-all');
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
    for (let i = 1; i < 7; i++) kbd.bind(i.toString(), () => ctrl.study?.glyphForm.toggleGlyph(i));
  }
};

export function view(ctrl: AnalyseCtrl): VNode {
  return snabModal({
    class: 'keyboard-help',
    onInsert: async ($wrap: Cash) => {
      const [, html] = await Promise.all([
        lichess.loadCssPath('analyse.keyboard'),
        xhr.text(xhr.url('/analysis/help', { study: !!ctrl.study })),
      ]);
      $wrap.find('.scrollable').html(html);
    },
    onClose() {
      ctrl.keyboardHelp = false;
      ctrl.redraw();
    },
    content: [h('div.scrollable', spinner())],
  });
}

export function maybeShowShiftKeyHelp() {
  // we can probably delete this after a month or so
  if (isTouchDevice() || !lichess.once('help.analyse.shift-key')) return;
  Promise.all([lichess.loadCssPath('analyse.keyboard'), xhr.text('/help/analyse/shift-key')]).then(
    ([, html]) => {
      $('.cg-wrap').append($(html).attr('id', 'analyse-shift-key-tooltip'));
      const cb = () => {
        $(document).off('mousedown keydown wheel', cb);
        $('#analyse-shift-key-tooltip').remove();
      };
      $(document).on('mousedown keydown wheel', cb);
    },
  );
}
