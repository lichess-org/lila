lichess.studyTour = function(study) {
  lichess.shepherd(function(theme) {
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
        title: "Welcome to lichess study!",
        text: "This is a shared analysis board.<br><br>" +
          "Use it to analyse and annotate games,<br>" +
          "discuss positions with friends,<br>" +
          "and of course for chess lessons!<br><br>" +
          "It's a powerful tool, let's take some time to see how it works.",
        attachTo: "#lichess .analyse .help top"
      }, {
        title: "Shared and saved",
        text: "Other members can see your moves in real time!<br>" +
          "Plus, everything is saved forever.",
        attachTo: "#lichess .analyse .areplay left"
      }, {
        title: "Study members",
        text: "<i data-icon='v'></i> Spectators can view the study and talk in the chat.<br>" +
          "<br><i data-icon=''></i> Contributors can make moves and update the study.",
        attachTo: "#site_header .study_box right",
        when: onTab('members')
      },
      study.isContrib ? {
        title: "Invite members",
        text: "By clicking the <i data-icon='O'></i> button.<br>" +
          "Then decide who can contribute or not.",
        attachTo: "#site_header .study_box .add right",
        when: onTab('members')
      } : null, {
        title: "Study chapters",
        text: "A study can contain several chapters.<br>" +
          "Each chapter has a distinct initial position and move tree.",
        attachTo: "#site_header .study_box right",
        when: onTab('chapters')
      },
      study.isContrib ? {
        title: "Create new chapters",
        text: "By clicking the <i data-icon='O'></i> button.",
        attachTo: "#site_header .study_box .add right",
        when: onTab('chapters')
      } : null, study.isContrib ? {
        title: "Comment on a position",
        text: "With the <i data-icon='c'></i> button, or a right click on the move list on the right.<br>" +
          "Comments are shared and persisted.",
        attachTo: "#lichess .member_buttons .comment top"
      } : null, study.isContrib ? {
        title: "Annotate a position",
        text: "With the !? button, or a right click on the move list on the right.<br>" +
          "Annotation glyphs are shared and persisted.",
        attachTo: "#lichess .member_buttons .glyph top"
      } : null, study.isContrib ? {
        title: "Connect / disconnect",
        text: "Choose whether or not your moves are shared and persisted.<br>" +
          "Useful to try out variations before sharing them.",
        attachTo: "#study-sync top"
      } : null, {
        title: "Thanks for your time",
        text: "You can find your <a href='/study/mine/hot'>previous studies</a> from your profile page.<br>" +
          "There is also a <a href='//lichess.org/blog/V0KrLSkAAMo3hsi4/study-chess-the-lichess-way'>blog post about studies</a>.<br>" +
          "Have fun!",
        buttons: [{
          text: 'Done',
          action: tour.next
        }],
        attachTo: "#lichess .analyse .help top"
      }
    ].filter(function(v) {
      return v;
    }).forEach(function(s) {
      tour.addStep(s.title, s);
    });
    tour.start();
  });
};
