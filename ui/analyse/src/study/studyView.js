var m = require('mithril');
var partial = require('chessground').util.partial;
var nodeFullName = require('../util').nodeFullName;
var memberView = require('./studyMembers').view;
var chapterView = require('./studyChapters').view;
var chapterFormView = require('./chapterForm').view;
var commentFormView = require('./commentForm').view;
var glyphFormView = require('./studyGlyph').view;
var inviteFormView = require('./inviteForm').view;
var studyFormView = require('./studyForm').view;

function contextAction(icon, text, handler) {
  return m('a.action', {
    'data-icon': icon,
    onclick: handler
  }, text);
}

function currentComments(ctrl, includingMine) {
  var path = ctrl.vm.path;
  var node = ctrl.vm.node;
  var chapter = ctrl.study.currentChapter();
  var comments = node.comments || [];
  if (!comments.length) return;
  return m('div.study_comments', comments.map(function(comment) {
    var isMine = ctrl.userId === comment.by.toLowerCase();
    if (!includingMine && isMine) return;
    var canDelete = isMine || ctrl.study.members.isOwner();
    return m('div.comment', [
      canDelete ? m('a.edit[data-icon=q][title=Delete]', {
        onclick: function() {
          if (confirm('Delete ' + comment.by + '\'s comment?'))
            ctrl.study.commentForm.delete(chapter.id, path, comment.by);
        }
      }) : null,
      isMine ? m('a.edit[data-icon=m][title=Edit]', {
        onclick: ctrl.study.commentForm.open
      }) : null,
      m('span.user_link.ulpt', {
        'data-href': '/@/' + comment.by
      }, comment.by),
      ' about ',
      m('span.node', nodeFullName(node)),
      ': ',
      m('span.text', comment.text.replace(/\n/g, '<br>'))
    ]);
  }));
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
    ]),
    m('div', [
      ctrl.members.isOwner() ?
      m('button.button.hint--top', {
        class: ctrl.members.inviteForm.open() ? 'active' : '',
        'data-hint': 'Invite someone',
        onclick: ctrl.members.inviteForm.toggle
      }, m('i[data-icon=r]')) : null,
      ctrl.members.canContribute() ? m('button.button.hint--top', {
        class: ctrl.chapters.form.vm.open ? 'active' : '',
        'data-hint': 'Add a chapter',
        onclick: ctrl.chapters.form.toggle
      }, m('i[data-icon=O]')) : null
    ])
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
      currentComments(ctrl, !commentForm),
      commentForm,
      buttons(ctrl)
    ];
  }
};
