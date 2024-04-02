import AnalyseCtrl from '../ctrl';
import Shepherd from 'shepherd.js';
import { ChapterTab, StudyTour, Tab } from '../study/interfaces';

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
        title: 'Welcome to Lichess Study!',
        text:
          'This is a shared analysis board.<br><br>' +
          'Use it to analyse and annotate games,<br>' +
          'discuss positions with friends,<br>' +
          'and of course for chess lessons!<br><br>' +
          "It's a powerful tool, let's take some time to see how it works.",
        attachTo: { element: helpButtonSelector, on: 'top' },
      },
      {
        title: 'Shared and saved',
        text: 'Other members can see your moves in real time!<br>' + 'Plus, everything is saved forever.',
        attachTo: { element: 'main.analyse .areplay', on: 'left' },
        when: closeActionMenu,
      },
      {
        title: 'Study members',
        text:
          "<i data-icon=''></i> Spectators can view the study and talk in the chat.<br>" +
          "<br><i data-icon=''></i> Contributors can make moves and update the study.",
        attachTo: { element: '.study__members', on: 'right' },
        when: onTab('members'),
      },
    ];

    if (ctrl.study?.members.isOwner()) {
      steps.push({
        title: 'Invite members',
        text: "By clicking the <i data-icon=''></i> button.<br>" + 'Then decide who can contribute or not.',
        attachTo: { element: '.study__members .add', on: 'right' },
        when: onTab('members'),
      });
    }

    steps.push({
      title: 'Study chapters',
      text:
        'A study can contain several chapters.<br>' +
        'Each chapter has a distinct initial position and move tree.',
      attachTo: { element: '.study__chapters', on: 'right' },
      when: onTab('chapters'),
    });

    if (ctrl.study?.members.canContribute()) {
      steps.push({
        title: 'Create new chapters',
        text: "By clicking the <i data-icon=''></i> button.",
        attachTo: { element: '.study__chapters .add', on: 'right' },
        when: onTab('chapters'),
        scrollTo: true,
      });
      steps.push({
        title: 'Comment on a position',
        text:
          "With the <i data-icon=''></i> button, or a right click on the move list on the right.<br>" +
          'Comments are shared and persisted.',
        attachTo: { element: '.study__buttons .left-buttons .comments', on: 'top' },
      });
      steps.push({
        title: 'Annotate a position',
        text:
          'With the !? button, or a right click on the move list on the right.<br>' +
          'Annotation glyphs are shared and persisted.',
        attachTo: { element: '.study__buttons .left-buttons .glyphs', on: 'top' },
      });
    }

    steps.push({
      title: 'Thanks for your time',
      text:
        "You can find your <a href='/study/mine/hot'>previous studies</a> from your profile page.<br>" +
        "There is also a <a href='//lichess.org/blog/V0KrLSkAAMo3hsi4/study-chess-the-lichess-way'>blog post about studies</a>.<br>" +
        'Power users might want to press "?" to see keyboard shortcuts.<br>' +
        'Have fun!',
      attachTo: { element: helpButtonSelector, on: 'top' },
      buttons: [
        {
          text: 'Done',
          action: tourCtrl.tour.next,
        },
      ],
    });

    tourCtrl.startTour(steps);
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

    tourCtrl.startTour(steps);
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
    site.pubsub.on('analyse.close-all', this.tour.cancel);
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

  startTour(steps: Shepherd.Step.StepOptions[]) {
    Shepherd.activeTour?.cancel();
    this.buildTour(steps);
    this.tour.start();
  }
}
