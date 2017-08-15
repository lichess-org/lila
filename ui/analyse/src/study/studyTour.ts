import AnalyseController from '../ctrl';
import { Tab } from './interfaces';

export function study(ctrl: AnalyseController) {
  window.lichess.loadScript('/assets/javascripts/study/tour.js').then(() => {
    window.lichess['studyTour']({
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
  window.lichess.loadScript('/assets/javascripts/study/tour-chapter.js').then(() => {
    window.lichess['studyTourChapter']({
      setTab
    });
  });
}
