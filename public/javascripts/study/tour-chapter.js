function loadShepherd(f) {
  var theme = 'shepherd-theme-' + ($('body').hasClass('dark') ? 'default' : 'dark');
  lichess.loadCss('vendor/shepherd/dist/css/' + theme + '.css');
  lichess.loadScript('vendor/shepherd/dist/js/tether.js', { noVersion: true }).then(function () {
    lichess.loadScript('vendor/shepherd/dist/js/shepherd.min.js', { noVersion: true }).then(function () {
      f(theme);
    });
  });
}
lichess.studyTourChapter = function (study) {
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
        attachTo: '#modal-wrap label[for=chapter-name] left',
      },
      {
        title: 'From initial position',
        text: 'Just a board setup for a new game.<br>' + 'Suited to explore openings.',
        attachTo: '#modal-wrap .tabs-horiz .init top',
        when: onTab('init'),
      },
      {
        title: 'Custom position',
        text: 'Setup the board your way.<br>' + 'Suited to explore endgames.',
        attachTo: '#modal-wrap .tabs-horiz .edit bottom',
        when: onTab('edit'),
      },
      {
        title: 'Load an existing lichess game',
        text:
          'Paste a lichess game URL<br>' + '(like lichess.org/7fHIU0XI)<br>' + 'to load the game moves in the chapter.',
        attachTo: '#modal-wrap .tabs-horiz .game top',
        when: onTab('game'),
      },
      {
        title: 'From a FEN string',
        text:
          'Paste a position in FEN format<br>' +
          '<i>4k3/4rb2/8/7p/8/5Q2/1PP5/1K6 w</i><br>' +
          'to start the chapter from a position.',
        attachTo: '#modal-wrap .tabs-horiz .fen top',
        when: onTab('fen'),
      },
      {
        title: 'From a PGN game',
        text: 'Paste a game in PGN format.<br>' + 'to load moves, comments and variations in the chapter.',
        attachTo: '#modal-wrap .tabs-horiz .pgn top',
        when: onTab('pgn'),
      },
      {
        title: 'Studies support variants',
        text: 'Yes, you can study crazyhouse,<br>' + 'and all lichess variants!',
        attachTo: '#modal-wrap label[for=chapter-variant] left',
        when: onTab('init'),
      },
      {
        title: 'Thanks for your time',
        text: 'Chapters are saved forever.<br>' + 'Have fun organizing your chess content!',
        buttons: [
          {
            text: 'Done',
            action: tour.next,
          },
        ],
        attachTo: '#modal-wrap .help bottom',
      },
    ].forEach(function (s) {
      tour.addStep(s.title, s);
    });
    tour.start();
  });
};
