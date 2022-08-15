function loadShepherd(f) {
  var theme = 'shepherd-theme-' + ($('body').hasClass('dark') ? 'dark' : 'default');
  lishogi.loadCss('vendor/shepherd/dist/css/' + theme + '.css');
  lishogi.loadScript('vendor/shepherd/dist/js/tether.js', { noVersion: true }).done(function () {
    lishogi
      .loadScript('vendor/shepherd/dist/js/shepherd.min.js', {
        noVersion: true,
      })
      .done(function () {
        f(theme);
      });
  });
}
lishogi.studyTourChapter = function (study) {
  loadShepherd(function (theme) {
    var onTab = function (tab) {
      return {
        'before-show': function () {
          study.setTab(tab);
        },
      };
    };
    var tour = new Shepherd.Tour({
      defaults: {
        classes: theme,
        scrollTo: false,
        showCancelLink: true,
      },
    });
    [
      {
        title: "Let's create a study chapter",
        text:
          'A study can have several chapters.<br>' +
          'Each chapter has a distinct move tree,<br>' +
          'and can be created in various ways.',
        attachTo: '.study__modal label[for=chapter-name] left',
      },
      {
        title: 'From initial position',
        text: 'Just a board setup for a new game.<br>' + 'Suited to explore openings.',
        attachTo: '.study__modal .tabs-horiz .init top',
        when: onTab('init'),
      },
      {
        title: 'Custom position',
        text: 'Setup the board your way.<br>' + 'Suited to explore endgames.',
        attachTo: '.study__modal .tabs-horiz .edit bottom',
        when: onTab('edit'),
      },
      {
        title: 'Load an existing lishogi game',
        text:
          'Paste a lishogi game URL<br>' + '(like lishogi.org/7fHIU0XI)<br>' + 'to load the game moves in the chapter.',
        attachTo: '.study__modal .tabs-horiz .game top',
        when: onTab('game'),
      },
      {
        title: 'From a SFEN string',
        text:
          'Paste a position in SFEN format<br>' +
          '<i>9/3k5/9/9/9/9/9/5K3/9 b</i><br>' +
          'to start the chapter from a position.',
        attachTo: '.study__modal .tabs-horiz .sfen top',
        when: onTab('sfen'),
      },
      {
        title: 'Studies support variants',
        text: 'Yes, you can study all lishogi variants!',
        attachTo: '.study__modal label[for=chapter-variant] left',
        when: onTab('init'),
      },
      {
        title: 'Thanks for your time',
        text: 'Chapters are saved forever.<br>' + 'Have fun organizing your shogi content!',
        buttons: [
          {
            text: 'Done',
            action: tour.next,
          },
        ],
        attachTo: '.study__modal .help bottom',
      },
    ].forEach(function (s) {
      tour.addStep(s.title, s);
    });
    tour.start();
  });
};
