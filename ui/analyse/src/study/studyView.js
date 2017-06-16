var m = require('mithril');
var classSet = require('common').classSet;
var bindOnce = require('../util').bindOnce;
var plural = require('../util').plural;
var memberView = require('./studyMembers').view;
var chapterView = require('./studyChapters').view;
var chapterNewFormView = require('./chapterNewForm').view;
var chapterEditFormView = require('./chapterEditForm').view;
var commentFormView = require('./commentForm').view;
var currentCommentsView = require('./studyComments').currentComments;
var glyphFormView = require('./studyGlyph').view;
var inviteFormView = require('./inviteForm').view;
var studyFormView = require('./studyForm').view;
var studyShareView = require('./studyShare').view;
var notifView = require('./notif').view;
var tagsView = require('./studyTags').view;
var practiceView = require('./practice/studyPracticeView');

function buttons(root) {
  var ctrl = root.study;
  var canContribute = ctrl.members.canContribute();
  return m('div.study_buttons', [
    m('div.member_buttons', [
      ctrl.data.features.sticky ? m('a.button.mode.hint--top', {
        'data-hint': 'All sync members remain on the same position',
        class: classSet({on: ctrl.vm.mode.sticky }),
        onclick: ctrl.toggleSticky
      }, [ m('i.is'), 'Sync']) : null,
      ctrl.members.canContribute() ? m('a.button.mode.hint--top', {
        'data-hint': 'Write changes to the server',
        class: classSet({on: ctrl.vm.mode.write }),
        onclick: ctrl.toggleWrite
      }, [ m('i.is'), 'Record']) : null,
      m('a.button.share.hint--top', {
        class: classSet({
          active: ctrl.share.open()
        }),
        'data-hint': 'Share & export',
        config: bindOnce('click', ctrl.share.toggle)
      },
      m('i.[data-icon=z]')),
      canContribute ? [
        m('a.button.comment.hint--top', {
          class: classSet({
            active: ctrl.commentForm.current(),
            disabled: !ctrl.vm.mode.write
          }),
          'data-hint': 'Comment this position',
          config: bindOnce('click', function() {
            if (ctrl.vm.mode.write) ctrl.commentForm.toggle(ctrl.currentChapter().id, root.vm.path, root.vm.node);
          })
        }, m('i[data-icon=c]')),
        m('a.button.glyph.hint--top', {
          class: classSet({
            active: ctrl.glyphForm.isOpen(),
            disabled: !(root.vm.path && ctrl.vm.write)
          }),
          'data-hint': 'Annotate with symbols',
          config: bindOnce('click', function() {
            if (root.vm.path && ctrl.vm.mode.write) ctrl.glyphForm.toggle();
          })
        },
        m('i.glyph-icon'))
      ] : null
    ]),
    m('span.button.help.hint--top', {
      'data-hint': 'Need help? Get the tour!',
      onclick: ctrl.startTour
    }, m('i.text', {
      'data-icon': ''
    }, 'help'))
  ]);
}

function metadata(ctrl) {
  var chapter = ctrl.currentChapter();
  if (!chapter) return;
  var d = ctrl.data;
  return m('div.study_metadata.undertable', [
    m('h2.undertable_top', [
      m('span.name', {
        'data-icon': ''
      }, d.name, ': ' + chapter.name),
      m('span', {
        class: 'liking text' + (d.liked ? ' liked' : ''),
        'data-icon': d.liked ? '' : '',
        title: 'Like',
        onclick: ctrl.toggleLike
      }, d.likes)
    ]),
    tagsView(ctrl)
  ]);
}

module.exports = {

  main: function(ctrl) {

    var activeTab = ctrl.vm.tab();

    var makeTab = function(key, name) {
      return m('a', {
        class: key + (activeTab === key ? ' active' : ''),
        config: bindOnce('mousedown', lichess.partial(ctrl.vm.tab, key)),
      }, name);
    };

    var tabs = m('div.study_tabs', [
      makeTab('members', plural('Member', ctrl.members.size())),
      makeTab('chapters', plural('Chapter', ctrl.chapters.size())),
      ctrl.members.isOwner() ? m('a.more', {
        config: bindOnce('click', function() {
          ctrl.form.open(!ctrl.form.open());
        })
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
    if (ctrl.vm.mode.write) return [
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
    if (ctrl.chapters.newForm.vm.open) return chapterNewFormView(ctrl.chapters.newForm);
    if (ctrl.chapters.editForm.current()) return chapterEditFormView(ctrl.chapters.editForm);
    if (ctrl.members.inviteForm.open()) return inviteFormView(ctrl.members.inviteForm);
    if (ctrl.form.open()) return studyFormView(ctrl.form);
    if (ctrl.share.open()) return studyShareView(ctrl.share);
  },

  underboard: function(ctrl) {
    if (ctrl.studyPractice) return practiceView.underboard(ctrl.study);
    if (ctrl.embed) return;
    var glyphForm = glyphFormView(ctrl.study.glyphForm);
    var commentForm = commentFormView(ctrl.study.commentForm);
    return [
      notifView(ctrl.study.notif),
      glyphForm,
      currentCommentsView(ctrl, !commentForm),
      commentForm,
      buttons(ctrl),
      metadata(ctrl.study, ctrl.tree.root)
    ];
  }
};
