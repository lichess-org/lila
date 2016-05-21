lichess.studyTour = function(study) {
  lichess.hopscotch(function() {
    var tour = {
      id: "study",
      showPrevButton: true,
      scrollDuration: 500,
      steps: [{
        title: "Welcome to lichess study!",
        content: "This is a shared analysis board.<br><br>" +
          "Use it to analyse and annotate games,<br>" +
          "discuss positions with friends,<br>" +
          "and of course for chess lessons!<br><br>" +
          "It's a powerful tool, let's take some time to see how it works.",
        target: "#lichess .analyse .help",
        placement: "top"
      }, {
        title: "Shared and saved",
        content: "Other members can see your moves in real time!<br>" +
          "Plus, everything is saved forever.",
        target: "#lichess .analyse .replay",
        placement: "left"
      }, {
        title: "Study members",
        content: "<br><i data-icon='v'></i> Viewers can spectate the study and talk in the chat.<br>" +
          "<br><i data-icon=''></i> Contributors can make moves and update the study.",
        target: "#site_header .study_box",
        placement: "right",
        onShow: function() {
          study.setTab('members');
        }
      }, {
        title: "Invite members",
        content: "By clicking the <i data-icon='O'></i> button.<br>" +
          "Then decide who can contribute or not.",
        target: "#site_header .study_box .add",
        placement: "right",
        yOffset: -17,
        onShow: function() {
          study.setTab('members');
        }
      }, {
        title: "Study chapters",
        content: "A study can contain several chapters.<br>" +
          "Each chapter has a distinct initial position and move tree.",
        target: "#site_header .study_box",
        placement: "right",
        onShow: function() {
          study.setTab('chapters');
        }
      }, {
        title: "Create new chapters",
        content: "By clicking the <i data-icon='O'></i> button.",
        target: "#site_header .study_box .add",
        placement: "right",
        yOffset: -17,
        onShow: function() {
          study.setTab('chapters');
        }
      }, {
        title: "Comment on a position",
        content: "With the <i data-icon='c'></i> button, or a right click on the move list on the right.<br>" +
          "Comments are shared and persisted.",
        target: "#lichess .member_buttons .comment",
        placement: "top"
      }, {
        title: "Annotate a position",
        content: "With the !? button, or a right click on the move list on the right.<br>" +
          "Annotation glyphs are shared and persisted.",
        target: "#lichess .member_buttons .glyph",
        placement: "top"
      }, {
        title: "Connect / disconnect",
        content: "Choose whether or not your moves are shared and persisted.<br>" +
          "Useful to try out variations before sharing them.",
        target: "#study-sync",
        placement: "top"
      }, {
        title: "Thanks for your time",
        content: "You can find your <a href='/@/" + study.userId + "/study'>previous studies</a> from your profile page.<br>" +
          "Have fun!",
        target: "#lichess .analyse .help",
        placement: "top"
      }]
    };
    hopscotch.startTour(tour);
  });
};
