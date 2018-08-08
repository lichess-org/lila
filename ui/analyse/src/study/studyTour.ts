import AnalyseCtrl from '../ctrl';
import { Tab } from './interfaces';

export function study(ctrl: AnalyseCtrl) {
  window.lidraughts.loadScript('/assets/javascripts/study/tour.js').then(() => {
    window.lidraughts['studyTour']({
      userId: ctrl.opts.userId,
      isContrib: ctrl.study!.members.canContribute(),
      setTab: (tab: Tab) => {
        ctrl.study!.vm.tab(tab);
        ctrl.redraw();
      }
    });
  });
}

export function chapter(setTab: (tab: string) => void) {
  window.lidraughts.loadScript('/assets/javascripts/study/tour-chapter.js').then(() => {
    window.lidraughts['studyTourChapter']({
      setTab
    });
  });
}
