var m = require('mithril');
var partial = require('chessground').util.partial;
var nodeFullName = require('../util').nodeFullName;
var memberView = require('./studyMembers').view;
var chapterView = require('./studyChapters').view;
var chapterFormView = require('./chapterForm').view;
var commentFormView = require('./commentForm').view;
var currentCommentsView = require('./studyComments').currentComments;
var glyphFormView = require('./studyGlyph').view;
var inviteFormView = require('./inviteForm').view;
var studyFormView = require('./studyForm').view;

function contextAction(icon, text, handler) {
  return m('a.action', {
    'data-icon': icon,
    onclick: handler
  }, text);
}

function buttons(root) {
  var ctrl = root.study;
  var canContribute = ctrl.members.canContribute();
  return m('div.study_buttons', [
    m('div.member_buttons', [
      m('span#study-sync.hint--top', {
        'data-hint': ctrl.vm.behind !== false ? 'Synchronize with other players' : 'Disconnect to play local moves'
      }, m('a', (function() {
        var attrs = {
          onclick: ctrl.toggleSync
        };
        var classes = ['button'];
        if (ctrl.vm.behind > 0) {
          attrs['data-count'] = ctrl.vm.behind;
          classes.push('data-count');
        }
        if (ctrl.vm.behind !== false) classes.push('glowed');
        attrs.class = classes.join(' ');
        return attrs;
      })(), m('i', {
        'data-icon': ctrl.vm.behind !== false ? 'G' : 'Z'
      }))),
      m('a.button.hint--top', {
        'data-hint': 'Download as PGN',
        href: '/study/' + ctrl.data.id + '.pgn'
      }, m('i[data-icon=x]')),
      canContribute ? [
        m('a.button.hint--top', {
          class: ctrl.commentForm.current() ? 'active' : '',
          'data-hint': 'Comment this position',
          disabled: ctrl.vm.behind !== false,
          onclick: function() {
            ctrl.commentForm.toggle(ctrl.currentChapter().id, root.vm.path, root.vm.node)
          }
        }, m('i[data-icon=c]')),
        m('a.button.hint--top', {
          class: ctrl.glyphForm.isOpen() ? 'active' : '',
          'data-hint': 'Annotate with symbols',
          disabled: ctrl.vm.behind !== false,
          onclick: ctrl.glyphForm.toggle
        }, m('i.glyph-icon'))
      ] : null
    ])
  ]);
}

function tags(tags) {
  return m('table.tags.slist', m('tbody', tags.map(function(tag) {
    return m('tr', [
      m('th', tag.name),
      m('td', tag.value)
    ]);
  })));
}

function metadata(ctrl) {
  var chapter = ctrl.currentChapter();
  var fromPgn = ctrl.data.setup.fromPgn;
  return m('div.metadata', [
    m('h2', [
      ctrl.data.name,
      ': ' +
      chapter.name
    ]),
    fromPgn ? tags(fromPgn.tags) : null
  ]);
}

module.exports = {

  main: function(ctrl) {

    var activeTab = ctrl.vm.tab();

    var makeTab = function(key, name) {
      return m('a', {
        class: key + (activeTab === key ? ' active' : ''),
        onclick: partial(ctrl.vm.tab, key),
      }, name);
    };

    var tabs = m('div.study_tabs', [
      makeTab('members', 'Members'),
      makeTab('chapters', 'Chapters'),
      ctrl.members.isOwner() ? m('a.more', {
        onclick: function() {
          ctrl.form.open(!ctrl.form.open());
        }
      }, m('i', {
        'data-icon': '['
      })) : null
    ]);

    var panel;
    if (activeTab === 'members') panel = memberView(ctrl);
    else if (activeTab === 'chapters') panel = chapterView.main(ctrl);

    return [
      tabs,
      panel
    ];
  },

  contextMenu: function(ctrl, path, node) {
    return [
      m('a.action', {
        'data-icon': 'c',
        onclick: function() {
          ctrl.commentForm.open(ctrl.currentChapter().id, path, node);
        }
      }, 'Comment this move'),
      m('a.action.glyph-icon', {
        onclick: function() {
          ctrl.glyphForm.open();
          ctrl.userJump(path);
        }
      }, 'Annotate with symbols')
    ];
  },

  overboard: function(ctrl) {
    if (ctrl.chapters.form.vm.open) return chapterFormView(ctrl.chapters.form);
    if (ctrl.members.inviteForm.open()) return inviteFormView(ctrl.members.inviteForm);
    if (ctrl.form.open()) return studyFormView(ctrl.form);
  },

  underboard: function(ctrl) {
    var glyphForm = glyphFormView(ctrl.study.glyphForm);
    var commentForm = commentFormView(ctrl.study.commentForm);
    return [
      glyphForm,
      currentCommentsView(ctrl, !commentForm),
      commentForm,
      buttons(ctrl),
      metadata(ctrl.study)
    ];
  }
};
