import AnalyseCtrl from '../ctrl';
import { Tab } from './interfaces';

export function study(ctrl: AnalyseCtrl) {
  lichess.loadScript('javascripts/study/tour.js').then(() => {
    window.lichess['studyTour']({
      userId: ctrl.opts.userId,
      isContrib: ctrl.study!.members.canContribute(),
      isOwner: ctrl.study!.members.isOwner(),
      setTab: (tab: Tab) => {
        ctrl.study!.vm.tab(tab);
        ctrl.redraw();
      },
    });
  });
}

export function chapter(setTab: (tab: string) => void) {
  lichess.loadScript('javascripts/study/tour-chapter.js').then(() => {
    window.lichess['studyTourChapter']({
      setTab,
    });
  });
}
