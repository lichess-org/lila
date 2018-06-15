import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import { bind, plural, dataIcon, iconTag } from '../util';
import { view as memberView } from './studyMembers';
import { view as chapterView } from './studyChapters';
import { view as chapterNewFormView } from './chapterNewForm';
import { view as chapterEditFormView } from './chapterEditForm';
import * as commentForm from './commentForm';
import * as glyphForm from './studyGlyph';
import { view as inviteFormView } from './inviteForm';
import { view as studyFormView } from './studyForm';
import { view as studyShareView } from './studyShare';
import { view as notifView } from './notif';
import { view as tagsView } from './studyTags';
import { view as serverEvalView } from './serverEval';
import * as practiceView from './practice/studyPracticeView';
import { playButtons as gbPlayButtons, overrideButton as gbOverrideButton } from './gamebook/gamebookButtons';
import { view as descView } from './chapterDescription';
import AnalyseCtrl from '../ctrl';
import { StudyCtrl, Tab, ToolTab } from './interfaces';
import { MaybeVNodes } from '../interfaces';


interface ToolButtonOpts {
  ctrl: StudyCtrl;
  tab: ToolTab;
  hint: string;
  icon: VNode;
  onClick?: () => void;
  count?: number | string;
}

function toolButton(opts: ToolButtonOpts): VNode {
  return h('a.fbt.hint--top.' + opts.tab, {
    attrs: { 'data-hint': opts.hint },
    class: { active: opts.tab === opts.ctrl.vm.toolTab() },
    hook: bind('mousedown', () => {
      if (opts.onClick) opts.onClick();
      opts.ctrl.vm.toolTab(opts.tab);
    }, opts.ctrl.redraw)
  }, [
    opts.count ? h('count.data-count', { attrs: { 'data-count': opts.count } }) : null,
    opts.icon
  ]);
}

function buttons(root: AnalyseCtrl): VNode {
  const ctrl: StudyCtrl = root.study!,
  canContribute = ctrl.members.canContribute(),
  showSticky = ctrl.data.features.sticky && (canContribute || (ctrl.vm.behind && ctrl.isUpdatedRecently()));
  return h('div.study_buttons', [
    h('div.member_buttons', [
      // distinct classes (sync, write) allow snabbdom to differentiate buttons
      showSticky ? h('a.mode.sync.hint--top', {
        attrs: { 'data-hint': 'All sync members remain on the same position' },
        class: { on: ctrl.vm.mode.sticky },
        hook: bind('click', ctrl.toggleSticky)
      }, [
        ctrl.vm.behind ? h('span.behind', '' + ctrl.vm.behind) : h('i.is'),
        'Sync'
      ]) : null,
      ctrl.members.canContribute() ? h('a.mode.write.hint--top', {
        attrs: { 'data-hint': 'Write changes to the server' },
        class: { on: ctrl.vm.mode.write },
        hook: bind('click', ctrl.toggleWrite)
      }, [ h('i.is'), 'Record' ]) : null,
      toolButton({
        ctrl,
        tab: 'tags',
        hint: 'PGN tags',
        icon: iconTag('o'),
      }),
      toolButton({
        ctrl,
        tab: 'comments',
        hint: 'Comment this position',
        icon: iconTag('c'),
        onClick() {
          ctrl.commentForm.start(ctrl.vm.chapterId, root.path, root.node);
        },
        count: (root.node.comments || []).length
      }),
      canContribute ?  toolButton({
        ctrl,
        tab: 'glyphs',
        hint: 'Annotate with glyphs',
        icon: h('i.glyph-icon'),
        count: (root.node.glyphs || []).length
      }) : null,
      toolButton({
        ctrl,
        tab: 'serverEval',
        hint: root.trans.noarg('computerAnalysis'),
        icon: iconTag(''),
        count: root.data.analysis && '✓'
      }),
      toolButton({
        ctrl,
        tab: 'share',
        hint: 'Share & export',
        icon: iconTag('z')
      })
    ]),
    gbOverrideButton(ctrl) || helpButton(ctrl)
  ]);
}

function helpButton(ctrl: StudyCtrl) {
  return h('span.fbt.help.hint--top', {
    attrs: { 'data-hint': 'Need help? Get the tour!' },
    hook: bind('click', ctrl.startTour)
  }, [ iconTag('') ]);
}

function metadata(ctrl: StudyCtrl): VNode {
  const d = ctrl.data;
  return h('div.study_metadata.undertable', [
    h('h2.undertable_top', [
      h('span.name', [
        d.name,
        ': ' + ctrl.currentChapter().name
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

export function main(ctrl: StudyCtrl): VNode {

  const activeTab = ctrl.vm.tab();

  const makeTab = function(key: Tab, name: string) {
    return h('a.' + key, {
      class: { active: activeTab === key },
      hook: bind('mousedown', () => ctrl.vm.tab(key), ctrl.redraw)
    }, name);
  };

  const tabs = h('div.study_tabs', [
    makeTab('members', plural('Member', ctrl.members.size())),
    makeTab('chapters', plural(ctrl.relay ? 'Game' : 'Chapter', ctrl.chapters.size())),
    ctrl.members.isOwner() ? h('a.more', {
      hook: bind('click', () => ctrl.form.open(!ctrl.form.open()), ctrl.redraw)
    }, [ iconTag('[') ]) : null
    ]);

  let panel;
  if (activeTab === 'members') panel = memberView(ctrl);
  else if (activeTab === 'chapters') panel = chapterView(ctrl);

  return h('div.side_box.study_box', [
    tabs,
    panel
  ]);
}

export function contextMenu(ctrl: StudyCtrl, path: Tree.Path, node: Tree.Node): VNode[] {
  return ctrl.vm.mode.write ? [
    h('a.action', {
      attrs: dataIcon('c'),
      hook: bind('click', () => {
        ctrl.vm.toolTab('comments');
        ctrl.commentForm.set(ctrl.currentChapter()!.id, path, node);
      })
    }, 'Comment this move'),
    h('a.action.glyph-icon', {
      hook: bind('click', () => {
        ctrl.vm.toolTab('glyphs');
        ctrl.userJump(path);
      })
    }, 'Annotate with glyphs')
  ] : [];
}

export function overboard(ctrl: StudyCtrl) {
  if (ctrl.chapters.newForm.vm.open) return chapterNewFormView(ctrl.chapters.newForm);
  if (ctrl.chapters.editForm.current()) return chapterEditFormView(ctrl.chapters.editForm);
  if (ctrl.members.inviteForm.open()) return inviteFormView(ctrl.members.inviteForm);
  if (ctrl.form.open()) return studyFormView(ctrl.form);
}

export function underboard(ctrl: AnalyseCtrl): MaybeVNodes {
  if (ctrl.embed) return [];
  if (ctrl.studyPractice) return [practiceView.underboard(ctrl.study!)];
  const study = ctrl.study!, toolTab = study.vm.toolTab();
  if (study.gamebookPlay()) return [
    gbPlayButtons(ctrl),
    descView(study),
    metadata(study)
  ];
  let panel;
  switch(toolTab) {
    case 'tags':
      panel = metadata(study);
      break;
    case 'comments':
      panel = study.vm.mode.write ?
        commentForm.view(ctrl) : (
          commentForm.viewDisabled(ctrl, study.members.canContribute() ?
            'Press RECORD to comment moves' :
            'Only the study members can comment on moves')
        );
        break;
    case 'glyphs':
      panel = ctrl.path ? (
        study.vm.mode.write ?
        glyphForm.view(study.glyphForm) :
        glyphForm.viewDisabled('Press RECORD to annotate moves')
      ) : glyphForm.viewDisabled('Select a move to annotate');
      break;
    case 'serverEval':
      panel = serverEvalView(study.serverEval);
      break;
    case 'share':
      panel = studyShareView(study.share);
      break;
  }
  return [
    notifView(study.notif),
    descView(study),
    buttons(ctrl),
    panel
  ];
}
