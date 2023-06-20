import AnalyseCtrl from '../ctrl';
import { Tab } from './interfaces';

export function study(ctrl: AnalyseCtrl) {
  if (!ctrl.study?.data.chapter.gamebook)
    lichess.loadIife('javascripts/study/tour.js').then(() => {
      window.lichess.studyTour({
        userId: ctrl.opts.userId,
        isContrib: ctrl.study!.members.canContribute(),
        isOwner: ctrl.study!.members.isOwner(),
        setTab: (tab: Tab) => {
          ctrl.study!.vm.tab(tab);
          ctrl.redraw();
        },
        closeActionMenu: () => {
          ctrl.actionMenu(false);
          ctrl.redraw();
        },
      });
    });
}

export const chapter = (setTab: (tab: string) => void) =>
  lichess.loadIife('javascripts/study/tour-chapter.js').then(() => {
    window.lichess.studyTourChapter({
      setTab,
    });
  });
