import AnalyseCtrl from '../ctrl';
import { Tab } from './interfaces';

export function study(ctrl: AnalyseCtrl) {
  window.lidraughts.loadScript('javascripts/study/tour.js').then(() => {
    window.lidraughts['studyTour']({
      userId: ctrl.opts.userId,
      isContrib: ctrl.study!.members.canContribute(),
      isOwner: ctrl.study!.members.isOwner(),
      setTab: (tab: Tab) => {
        ctrl.study!.vm.tab(tab);
        ctrl.redraw();
      }
    });
  });
}

export function chapter(setTab: (tab: string) => void) {
  window.lidraughts.loadScript('javascripts/study/tour-chapter.js').then(() => {
    window.lidraughts['studyTourChapter']({
      setTab
    });
  });
}
