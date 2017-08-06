import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import { bind, plural, dataIcon } from '../util';
import { view as memberView } from './studyMembers';
import { view as chapterView } from './studyChapters';
import { view as chapterNewFormView } from './chapterNewForm';
import { view as chapterEditFormView } from './chapterEditForm';
import { view as commentFormView } from './commentForm';
import { currentComments as currentCommentsView } from './studyComments';
import { view as glyphFormView } from './studyGlyph';
import { view as inviteFormView } from './inviteForm';
import { view as studyFormView } from './studyForm';
import { view as studyShareView } from './studyShare';
import { view as notifView } from './notif';
import { view as tagsView } from './studyTags';
import * as practiceView from './practice/studyPracticeView';
import AnalyseController from '../ctrl';
import { StudyController, Tab } from './interfaces';
import { MaybeVNodes } from '../interfaces';

function buttons(root: AnalyseController): VNode {
  const ctrl = root.study;
  const canContribute = ctrl.members.canContribute();
  const showSticky = ctrl.data.features.sticky && (canContribute || ctrl.vm.behind);
  return h('div.study_buttons', [
    h('div.member_buttons', [
      // distinct classes (sync, write) allow snabbdom to differentiate buttons
      showSticky ? h('a.button.mode.sync.hint--top', {
        attrs: { 'data-hint': 'All sync members remain on the same position' },
        class: { on: ctrl.vm.mode.sticky },
        hook: bind('click', ctrl.toggleSticky)
      }, [
        ctrl.vm.behind ? h('span.behind', ctrl.vm.behind) : h('i.is'),
        'Sync'
      ]) : null,
      ctrl.members.canContribute() ? h('a.button.mode.write.hint--top', {
        attrs: { 'data-hint': 'Write changes to the server' },
        class: {on: ctrl.vm.mode.write },
        hook: bind('click', ctrl.toggleWrite)
      }, [ h('i.is'), 'Record' ]) : null,
      h('a.button.share.hint--top', {
        attrs: { 'data-hint': 'Share & export' },
        class: { active: ctrl.share.open() },
        hook: bind('click', ctrl.share.toggle, ctrl.redraw)
      }, [
        h('i', { attrs: dataIcon('z') })
      ]),
      ...(canContribute ? [
        h('a.button.comment.hint--top', {
          attrs: { 'data-hint': 'Comment this position' },
          class: {
            active: ctrl.commentForm.current(),
            disabled: !ctrl.vm.mode.write
          },
          hook: bind('click', function() {
            if (ctrl.vm.mode.write) ctrl.commentForm.toggle(ctrl.currentChapter().id, root.path, root.node);
          }, ctrl.redraw)
        }, [
          h('i', { attrs: dataIcon('c') })
        ]),
        h('a.button.glyph.hint--top', {
          attrs: { 'data-hint': 'Annotate with symbols' },
          class: {
            active: ctrl.glyphForm.isOpen(),
            disabled: !(root.path && ctrl.vm.mode.write)
          },
          hook: bind('click', function() {
            if (root.path && ctrl.vm.mode.write) ctrl.glyphForm.toggle();
          }, ctrl.redraw)
        }, [
          h('i.glyph-icon')
        ])
      ] : [])
    ]),
    h('span.button.help.hint--top', {
      attrs: { 'data-hint': 'Need help? Get the tour!' },
      hook: bind('click', ctrl.startTour)
    }, [
      h('i.text', { attrs: dataIcon('') }, 'help')
    ])
  ]);
}

function metadata(ctrl: StudyController): VNode | undefined {
  const chapter = ctrl.currentChapter();
  const d = ctrl.data;
  return h('div.study_metadata.undertable', [
    h('h2.undertable_top', [
      h('span.name', [
        d.name,
        ': ' + chapter.name
      ]),
      h('span.liking.text', {
        class: { liked: d.liked },
        attrs: {
          'data-icon': d.liked ? '' : '',
          title: 'Like'
        },
        hook: bind('click', ctrl.toggleLike)
      }, '' + d.likes)
    ]),
    tagsView(ctrl)
  ]);
}

export function main(ctrl: StudyController): VNode {

  const activeTab = ctrl.vm.tab();

  const makeTab = function(key: Tab, name: string) {
    return h('a.' + key, {
      class: { active: activeTab === key },
      hook: bind('mousedown', () => ctrl.vm.tab(key), ctrl.redraw)
    }, name);
  };

  const tabs = h('div.study_tabs', [
    makeTab('members', plural('Member', ctrl.members.size())),
    makeTab('chapters', plural('Chapter', ctrl.chapters.size())),
    ctrl.members.isOwner() ? h('a.more', {
      hook: bind('click', () => ctrl.form.open(!ctrl.form.open()), ctrl.redraw)
    }, [ h('i', { attrs: dataIcon('[') }) ]) : null
    ]);

  let panel;
  if (activeTab === 'members') panel = memberView(ctrl);
  else if (activeTab === 'chapters') panel = chapterView(ctrl);

  return h('div.side_box.study_box', [
    tabs,
    panel
  ]);
}

export function contextMenu(ctrl: StudyController, path: Tree.Path, node: Tree.Node): VNode[] {
  return ctrl.vm.mode.write ? [
    h('a.action', {
      attrs: dataIcon('c'),
      hook: bind('click', () => ctrl.commentForm.open(ctrl.currentChapter()!.id, path, node))
    }, 'Comment this move'),
    h('a.action.glyph-icon', {
      hook: bind('click', () => {
        ctrl.glyphForm.open();
        ctrl.userJump(path);
      })
    }, 'Annotate with symbols')
  ] : [];
}

export function overboard(ctrl: StudyController) {
  if (ctrl.chapters.newForm.vm.open) return chapterNewFormView(ctrl.chapters.newForm);
  if (ctrl.chapters.editForm.current()) return chapterEditFormView(ctrl.chapters.editForm);
  if (ctrl.members.inviteForm.open()) return inviteFormView(ctrl.members.inviteForm);
  if (ctrl.form.open()) return studyFormView(ctrl.form);
  if (ctrl.share.open()) return studyShareView(ctrl.share);
}

export function underboard(ctrl: AnalyseController): MaybeVNodes {
  if (ctrl.embed) return [];
  if (ctrl.studyPractice) return [practiceView.underboard(ctrl.study)];
  const commentForm = commentFormView(ctrl.study.commentForm);
  return [
    notifView(ctrl.study.notif),
    glyphFormView(ctrl.study.glyphForm),
    currentCommentsView(ctrl, !commentForm),
    commentForm,
    buttons(ctrl),
    metadata(ctrl.study)
  ];
}
