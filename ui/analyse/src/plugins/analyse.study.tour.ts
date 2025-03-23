import type AnalyseCtrl from '../ctrl';
import Shepherd from 'shepherd.js';
import type { ChapterTab, StudyTour, Tab } from '../study/interfaces';
import { pubsub } from 'common/pubsub';
import * as licon from 'common/licon';

export function initModule(): StudyTour {
  return {
    study,
    chapter,
  };

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
        text: i18n.study.sharedAndSaveText,
        attachTo: { element: 'main.analyse .areplay', on: 'left' },
        when: closeActionMenu,
      },
      {
        title: i18n.study.studyMembersTitle,
        text: i18n.study.studyMembersText(
          `<i data-icon='${licon.Eye}'></i>`,
          `<i data-icon='${licon.User}'></i>`,
        ),
        attachTo: { element: '.study__members', on: 'right' },
        when: onTab('members'),
      },
    ];

    if (ctrl.study?.members.isOwner()) {
      steps.push({
        title: i18n.study.inviteMembersTitle,
        text: i18n.study.inviteMembersText(`<i data-icon='${licon.PlusButton}'></i>`),
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
        title: i18n.study.createNewChaptersTitle,
        text: i18n.study.createNewChaptersText(`<i data-icon='${licon.PlusButton}'></i>`),
        attachTo: { element: '.study__chapters .add', on: 'right' },
        when: onTab('chapters'),
        scrollTo: true,
      });
      steps.push({
        title: i18n.study.commentPositionTitle,
        text: i18n.study.commentPositionText(`<i data-icon='${licon.BubbleSpeech}'></i>`),
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
          text: 'Done',
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
        title: "Let's create a study chapter",
        text:
          'A study can have several chapters.<br>' +
          'Each chapter has a distinct move tree,<br>' +
          'and can be created in various ways.',
        attachTo: { element: `${viewSel} label[for=chapter-name]`, on: 'left' },
      },
      {
        title: 'From initial position',
        text: 'Just a board setup for a new game.<br>' + 'Suited to explore openings.',
        attachTo: { element: `${viewSel} .tabs-horiz .init`, on: 'top' },
        when: onTab('init'),
      },
      {
        title: 'Custom position',
        text: 'Setup the board your way.<br>' + 'Suited to explore endgames.',
        attachTo: { element: `${viewSel} .tabs-horiz .edit`, on: 'bottom' },
        when: onTab('edit'),
      },
      {
        title: 'Load an existing lichess game',
        text:
          'Paste a lichess game URL<br>' +
          '(like lichess.org/7fHIU0XI)<br>' +
          'to load the game moves in the chapter.',
        attachTo: { element: `${viewSel} .tabs-horiz .game`, on: 'top' },
        when: onTab('game'),
      },
      {
        title: 'From a FEN string',
        text:
          'Paste a position in FEN format<br>' +
          '<i>4k3/4rb2/8/7p/8/5Q2/1PP5/1K6 w</i><br>' +
          'to start the chapter from a position.',
        attachTo: { element: `${viewSel} .tabs-horiz .fen`, on: 'top' },
        when: onTab('fen'),
      },
      {
        title: 'From a PGN game',
        text: 'Paste a game in PGN format.<br>' + 'to load moves, comments and variations in the chapter.',
        attachTo: { element: `${viewSel} .tabs-horiz .pgn`, on: 'top' },
        when: onTab('pgn'),
      },
      {
        title: 'Studies support variants',
        text: 'Yes, you can study crazyhouse,<br>' + 'and all lichess variants!',
        attachTo: { element: `${viewSel} label[for=chapter-variant]`, on: 'left' },
        when: onTab('init'),
      },
      {
        title: 'Thanks for your time',
        text: 'Chapters are saved forever.<br>' + 'Have fun organizing your chess content!',
        buttons: [
          {
            text: 'Done',
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
        text: 'Next',
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
