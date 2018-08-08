lidraughts.studyTourChapter = function(study) {
  lidraughts.shepherd(function(theme) {
    var onTab = function(tab) {
      return {
        'before-show': function() {
          study.setTab(tab);
        }
      };
    };
    var tour = new Shepherd.Tour({
      defaults: {
        classes: theme,
        scrollTo: false,
        showCancelLink: true
      }
    });
    [{
      title: "Let's create a study chapter",
      text: "A study can have several chapters.<br>" +
        "Each chapter has a distinct move tree,<br>" +
        "and can be created in various ways.",
      attachTo: '.study_overboard label[for=chapter-name] left'
    }, {
      title: "From initial position",
      text: "Just a board setup for a new game.<br>" +
        "Suited to explore openings.",
      attachTo: '.study_overboard .study_tabs .init top',
      when: onTab('init')
    }, {
      title: "Custom position",
      text: "Setup the board your way.<br>" +
        "Suited to explore endgames.",
      attachTo: '.study_overboard .study_tabs .edit bottom',
      when: onTab('edit')
    }, {
      title: "Load an existing lidraughts game",
      text: "Paste a lidraughts game URL<br>" +
        "(like lidraughts.org/Xqch97QM)<br>" +
        "to load the game moves in the chapter.",
      attachTo: '.study_overboard .study_tabs .game top',
      when: onTab('game')
    }, {
      title: "From a FEN string",
      text: "Paste a position in FEN format<br>" +
        "<i>B:W10,29,33,34,39,44:B4,9,12,13,22,35</i><br>" +
        "to start the chapter from a position.",
      attachTo: '.study_overboard .study_tabs .fen top',
      when: onTab('fen')
    }, {
      title: "From a PDN game",
      text: "Paste a game in PDN format.<br>" +
        "to load moves, comments and variations in the chapter.",
      attachTo: '.study_overboard .study_tabs .pdn top',
      when: onTab('pdn')
    }, {
      title: "Studies support variants",
      text: "Yes, you can study frisian draughts,<br>" +
        "and all lidraughts variants!",
      attachTo: '.study_overboard label[for=chapter-variant] left',
      when: onTab('init')
    }, {
      title: "Thanks for your time",
      text: "Chapters are saved forever.<br>" +
        "Have fun organizing your chess content!",
      buttons: [{
        text: 'Done',
        action: tour.next
      }],
      attachTo: '.study_overboard .help bottom'
    }].forEach(function(s) {
      tour.addStep(s.title, s);
    });
    tour.start();
  });
};
