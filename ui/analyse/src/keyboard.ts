import * as control from './control';
import * as xhr from 'common/xhr';
import AnalyseCtrl from './ctrl';
import { h, VNode } from 'snabbdom';
import { snabModal } from 'common/modal';
import { spinnerVdom as spinner } from 'common/spinner';

export const bind = (ctrl: AnalyseCtrl) => {
  const kbd = window.Mousetrap;
  if (!kbd) return;
  kbd
    .bind(['left', 'k'], () => {
      control.prev(ctrl);
      ctrl.redraw();
    })
    .bind(['shift+left', 'shift+k'], () => {
      control.exitVariation(ctrl);
      ctrl.redraw();
    })
    .bind(['right', 'j'], () => {
      if (!ctrl.fork.proceed()) control.next(ctrl);
      ctrl.redraw();
    })
    .bind(['shift+right', 'shift+j'], () => {
      control.enterVariation(ctrl);
      ctrl.redraw();
    })
    .bind(['up', '0', 'home'], () => {
      if (!ctrl.fork.prev()) control.first(ctrl);
      ctrl.redraw();
    })
    .bind(['down', '$', 'end'], () => {
      if (!ctrl.fork.next()) control.last(ctrl);
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

  if (ctrl.embed) return;

  kbd.bind('space', () => {
    const gb = ctrl.gamebookPlay();
    if (gb) gb.onSpace();
    else if (ctrl.practice || ctrl.studyPractice) return;
    else if (ctrl.ceval.enabled()) ctrl.playBestMove();
    else ctrl.ceval.enable();
  });

  if (ctrl.studyPractice) return;

  kbd
    .bind('f', ctrl.flip)
    .bind('?', () => {
      ctrl.keyboardHelp = !ctrl.keyboardHelp;
      if (ctrl.keyboardHelp) lichess.pubsub.emit('analyse.close-all');
      ctrl.redraw();
    })
    .bind('z', () => {
      if (!ctrl.ceval.enabled())
        ctrl.ceval.enable();
      else
        ctrl.ceval.setEngineType('disabled');
    })
    .bind('a', () => {
      ctrl.ceval.toggleAutoShapes(!ctrl.ceval.showAutoShapes());
      ctrl.redraw();
    })
    .bind('x', ctrl.ceval.toggleThreatMode)
    .bind('e', () => {
      ctrl.toggleExplorer();
      ctrl.redraw();
    });
  if (ctrl.study) {
    const keyToMousedown = (key: string, selector: string) => {
      kbd.bind(key, () => {
        $(selector).each(function (this: HTMLElement) {
          this.dispatchEvent(new Event('mousedown'));
        });
      });
    };
    keyToMousedown('d', '.study__buttons .comments');
    keyToMousedown('g', '.study__buttons .glyphs');

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
