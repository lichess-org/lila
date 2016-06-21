var m = require('mithril');
var moderationView = require('./moderation').view;
var discussionView = require('./discussion').view;
var noteView = require('./note').view;

function tabName(ctrl, t) {
  switch (t) {
    case 'discussion':
      return [
        m('span', ctrl.data.name),
        m('input', {
          type: 'checkbox',
          class: 'toggle_chat',
          title: ctrl.trans('toggleTheChat'),
          onchange: m.withAttr('checked', ctrl.setEnabled),
          checked: ctrl.vm.enabled()
        })
      ];
    case 'note':
      return ctrl.trans('notes');
  }
}

function tabContent(ctrl, t) {
  if (t === 'note' && ctrl.note)
    return noteView(ctrl.note);
  return discussionView(ctrl);
}

function normalView(ctrl) {
  var tab = ctrl.vm.tab();
  var tabs = ['discussion'];
  if (ctrl.note) tabs.push('note');
  return [
    m('div', {
      class: 'chat_tabs nb_' + tabs.length
    }, tabs.map(function(t) {
      return m('div', {
        onclick: function() {
          ctrl.vm.tab(t);
        },
        class: 'tab ' + t + (tab === t ? ' active' : '')
      }, tabName(ctrl, t));
    })),
    m('div.content.' + tab, tabContent(ctrl, tab))
  ];
}

module.exports = function(ctrl) {
  return m('div', {
      class: 'mchat' + (ctrl.vm.isMod ? ' mod' : '')
    },
    moderationView.ui(ctrl.moderation) || normalView(ctrl));
};
