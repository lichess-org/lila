var m = require('mithril');
var partial = require('chessground').util.partial;
var classSet = require('chessground').util.classSet;
var memberView = require('./studyMembers').view;
var chapterView = require('./studyChapters').view;

module.exports = {

  main: function(ctrl) {

    var activeTab = ctrl.vm.tab();

    var makeTab = function(key, name) {
      return m('a', {
        class: activeTab === key ? 'active' : '',
        onclick: partial(ctrl.setTab, key),
      }, name);
    };

    var tabs = m('div.tabs', [
      makeTab('members', 'Members'),
      makeTab('chapters', 'Chapters'),
      makeTab('settings', 'Settings')
    ]);

    var panel;
    if (activeTab === 'members') panel = memberView(ctrl);
    else if (activeTab === 'chapters') panel = chapterView.main(ctrl);

    return [
      tabs,
      panel
    ];
  },

  overboard: function(ctrl) {
    if (ctrl.chapters.vm.creating)
      return chapterView.form(ctrl.chapters, ctrl.chapters.vm.creating);
  }
};
