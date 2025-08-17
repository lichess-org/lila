import type AnalyseCtrl from '../ctrl';
import Shepherd from 'shepherd.js';
import type { ChapterTab, StudyTour, Tab } from './interfaces';
import { pubsub } from 'lib/pubsub';
import * as licon from 'lib/licon';

export function initModule(): StudyTour {
  return {
    study,
    chapter,
  };

  function iconTag(i: string) {
    return `<i data-icon='${i}'></i>`;
  }

  function study(ctrl: AnalyseCtrl) {
    if (ctrl.study?.data.chapter.gamebook) return;

    const helpButtonSelector = 'main.analyse .study__buttons .help';
    if (!$(helpButtonSelector).length) return;

    const onTab = (tab: Tab): Shepherd.Step.StepOptionsWhen => ({
      'before-show': () => ctrl.study?.setTab(tab),
    });

    const closeActionMenu: Shepherd.Step.StepOptionsWhen = {
      'before-show': () => {
        ctrl.actionMenu(false);
        ctrl.redraw();
      },
    };

    const tourCtrl = new TourCtrl();

    const steps: Shepherd.Step.StepOptions[] = [
      {
        title: i18n.study.welcomeToLichessStudyTitle,
        text: i18n.study.welcomeToLichessStudyText,
        attachTo: { element: helpButtonSelector, on: 'top' },
      },
      {
        title: i18n.study.sharedAndSaveTitle,
        text: i18n.study.sharedAndSavedText,
        attachTo: { element: 'main.analyse .areplay', on: 'left' },
        when: closeActionMenu,
      },
      {
        title: i18n.study.studyMembersTitle,
        text: i18n.study.studyMembersText(iconTag(licon.Eye), iconTag(licon.User)),
        attachTo: { element: '.study__members', on: 'right' },
        when: onTab('members'),
      },
    ];

    if (ctrl.study?.members.isOwner()) {
      steps.push({
        title: i18n.study.addMembers,
        text: i18n.study.addMembersText(iconTag(licon.PlusButton)),
        attachTo: { element: '.study__members .add', on: 'right' },
        when: onTab('members'),
      });
    }

    steps.push({
      title: i18n.study.studyChaptersTitle,
      text: i18n.study.studyChaptersText,
      attachTo: { element: '.study__chapters', on: 'right' },
      when: onTab('chapters'),
    });

    if (ctrl.study?.members.canContribute()) {
      steps.push({
        title: i18n.study.commentPositionTitle,
        text: i18n.study.commentPositionText(iconTag(licon.BubbleSpeech)),
        attachTo: { element: '.study__buttons .left-buttons .comments', on: 'top' },
      });
      steps.push({
        title: i18n.study.annotatePositionTitle,
        text: i18n.study.annotatePositionText,
        attachTo: { element: '.study__buttons .left-buttons .glyphs', on: 'top' },
      });
    }

    steps.push({
      title: i18n.study.conclusionTitle,
      text: i18n.study.conclusionText,
      attachTo: { element: helpButtonSelector, on: 'top' },
      buttons: [
        {
          text: iconTag(licon.Checkmark),
          action: tourCtrl.tour.next,
        },
      ],
    });

    tourCtrl.toggleTour(steps);
  }

  function chapter(setTab: (tab: ChapterTab) => void) {
    const viewSel = 'dialog div.dialog-content';

    const tourCtrl = new TourCtrl();

    const onTab = (tab: ChapterTab): Shepherd.Step.StepOptionsWhen => ({
      'before-show': () => setTab(tab),
    });

    const steps: Shepherd.Step.StepOptions[] = [
      {
        title: i18n.study.createChapterTitle,
        text: i18n.study.createChapterText,
        attachTo: { element: `${viewSel} label[for=chapter-name]`, on: 'left' },
      },
      {
        title: i18n.study.fromInitialPositionTitle,
        text: i18n.study.fromInitialPositionText,
        attachTo: { element: `${viewSel} .tabs-horiz .init`, on: 'top' },
        when: onTab('init'),
      },
      {
        title: i18n.study.customPositionTitle,
        text: i18n.study.customPositionText,
        attachTo: { element: `${viewSel} .tabs-horiz .edit`, on: 'bottom' },
        when: onTab('edit'),
      },
      {
        title: i18n.study.loadExistingLichessGameTitle,
        text: i18n.study.loadExistingLichessGameText,
        attachTo: { element: `${viewSel} .tabs-horiz .game`, on: 'top' },
        when: onTab('game'),
      },
      {
        title: i18n.study.fromFenStringTitle,
        text: i18n.study.fromFenStringText,
        attachTo: { element: `${viewSel} .tabs-horiz .fen`, on: 'top' },
        when: onTab('fen'),
      },
      {
        title: i18n.study.fromPgnGameTitle,
        text: i18n.study.fromPgnGameText,
        attachTo: { element: `${viewSel} .tabs-horiz .pgn`, on: 'top' },
        when: onTab('pgn'),
      },
      {
        title: i18n.study.variantsAreSupportedTitle,
        text: i18n.study.variantsAreSupportedText,
        attachTo: { element: `${viewSel} label[for=chapter-variant]`, on: 'left' },
        when: onTab('init'),
      },
      {
        title: i18n.study.conclusionTitle,
        text: i18n.study.chapterConclusionText,
        buttons: [
          {
            text: iconTag(licon.Checkmark),
            action: tourCtrl.tour.next,
          },
        ],
        attachTo: { element: `${viewSel} .help`, on: 'bottom' },
      },
    ];

    tourCtrl.toggleTour(steps);
  }
}

class TourCtrl {
  tour: Shepherd.Tour;
  constructor() {
    this.tour = new Shepherd.Tour({
      defaultStepOptions: {
        scrollTo: false,
        classes: 'force-ltr',
        cancelIcon: {
          enabled: true,
        },
      },
      exitOnEsc: true,
    });
    pubsub.on('analysis.closeAll', this.tour.cancel);
  }

  buildTour(steps: Shepherd.Step.StepOptions[]) {
    const buttons: Shepherd.Step.StepOptionsButton[] = [
      {
        text: i18n.study.next,
        action: this.tour.next,
      },
    ];
    steps.forEach(s =>
      this.tour.addStep({
        ...s,
        buttons: s.buttons ?? buttons,
      }),
    );
  }

  toggleTour(steps: Shepherd.Step.StepOptions[]) {
    if (Shepherd.activeTour) Shepherd.activeTour.cancel();
    else {
      this.buildTour(steps);
      this.tour.start();
    }
  }
}
