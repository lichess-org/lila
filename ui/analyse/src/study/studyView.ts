import * as commentForm from './commentForm';
import * as glyphForm from './studyGlyph';
import * as practiceView from './practice/studyPracticeView';
import AnalyseCtrl from '../ctrl';
import { VNode } from 'snabbdom';
import * as licon from 'common/licon';
import { iconTag, bind, dataIcon, MaybeVNodes, looseH as h } from 'common/snabbdom';
import { playButtons as gbPlayButtons, overrideButton as gbOverrideButton } from './gamebook/gamebookButtons';
import { Tab, ToolTab } from './interfaces';
import { view as chapterEditFormView } from './chapterEditForm';
import { view as chapterNewFormView } from './chapterNewForm';
import { view as chapterView } from './studyChapters';
import { view as descView } from './description';
import { view as inviteFormView } from './inviteForm';
import { view as memberView } from './studyMembers';
import { view as multiBoardView } from './multiBoard';
import { view as notifView } from './notif';
import { view as serverEvalView } from './serverEval';
import { view as studyFormView } from './studyForm';
import { view as studyShareView } from './studyShare';
import { view as tagsView } from './studyTags';
import { view as topicsView, formView as topicsFormView } from './topics';
import { view as searchView } from './studySearch';
import { render as renderTrainingView } from '../view/roundTraining';
import StudyCtrl from './studyCtrl';

interface ToolButtonOpts {
  ctrl: StudyCtrl;
  tab: ToolTab;
  hint: string;
  icon: VNode;
  onClick?: () => void;
  count?: number | string;
}

function toolButton(opts: ToolButtonOpts): VNode {
  return h(
    'span.' + opts.tab,
    {
      attrs: { role: 'tab', title: opts.hint },
      class: { active: opts.tab === opts.ctrl.vm.toolTab() },
      hook: bind(
        'mousedown',
        () => {
          if (opts.onClick) opts.onClick();
          opts.ctrl.vm.toolTab(opts.tab);
        },
        opts.ctrl.redraw,
      ),
    },
    [!!opts.count && h('count.data-count', { attrs: { 'data-count': opts.count } }), opts.icon],
  );
}

function buttons(root: AnalyseCtrl): VNode {
  const ctrl: StudyCtrl = root.study!,
    canContribute = ctrl.members.canContribute(),
    showSticky = ctrl.data.features.sticky && (canContribute || (ctrl.vm.behind && ctrl.isUpdatedRecently())),
    noarg = root.trans.noarg,
    gbButton = gbOverrideButton(ctrl);
  return h('div.study__buttons', [
    h('div.left-buttons.tabs-horiz', { attrs: { role: 'tablist' } }, [
      // distinct classes (sync, write) allow snabbdom to differentiate buttons
      !!showSticky &&
        h(
          'a.mode.sync',
          {
            attrs: { title: noarg('allSyncMembersRemainOnTheSamePosition') },
            class: { on: ctrl.vm.mode.sticky },
            hook: bind('click', ctrl.toggleSticky),
          },
          [ctrl.vm.behind ? h('span.behind', '' + ctrl.vm.behind) : h('i.is'), 'SYNC'],
        ),
      canContribute &&
        h(
          'a.mode.write',
          {
            attrs: { title: noarg('shareChanges') },
            class: { on: ctrl.vm.mode.write },
            hook: bind('click', ctrl.toggleWrite),
          },
          [h('i.is'), 'REC'],
        ),
      toolButton({ ctrl, tab: 'tags', hint: noarg('pgnTags'), icon: iconTag(licon.Tag) }),
      canContribute &&
        toolButton({
          ctrl,
          tab: 'comments',
          hint: noarg('commentThisPosition'),
          icon: iconTag(licon.BubbleSpeech),
          onClick() {
            ctrl.commentForm.start(ctrl.vm.chapterId, root.path, root.node);
          },
          count: (root.node.comments || []).length,
        }),
      canContribute &&
        toolButton({
          ctrl,
          tab: 'glyphs',
          hint: noarg('annotateWithGlyphs'),
          icon: h('i.glyph-icon'),
          count: (root.node.glyphs || []).length,
        }),
      (canContribute || root.data.analysis) &&
        toolButton({
          ctrl,
          tab: 'serverEval',
          hint: noarg('computerAnalysis'),
          icon: iconTag(licon.BarChart),
          count: root.data.analysis && '✓',
        }),
      toolButton({ ctrl, tab: 'multiBoard', hint: 'Multiboard', icon: iconTag(licon.Multiboard) }),
      toolButton({ ctrl, tab: 'share', hint: noarg('shareAndExport'), icon: iconTag(licon.NodeBranching) }),
      !ctrl.relay &&
        !ctrl.data.chapter.gamebook &&
        h('span.help', {
          attrs: { title: 'Need help? Get the tour!', ...dataIcon(licon.InfoCircle) },
          hook: bind('click', ctrl.startTour),
        }),
    ]),
    gbButton && h('div.right', gbButton),
  ]);
}

function metadata(ctrl: StudyCtrl): VNode {
  const d = ctrl.data,
    title = `${d.name}: ${ctrl.currentChapter().name}`;
  return h('div.study__metadata', [
    h('h2', [
      h('span.name', { attrs: { title } }, [
        d.flair && h('img.icon-flair', { attrs: { src: site.asset.flairSrc(d.flair) } }),
        title,
      ]),
      h(
        'span.liking.text',
        {
          class: { liked: d.liked },
          attrs: {
            ...dataIcon(d.liked ? licon.Heart : licon.HeartOutline),
            title: ctrl.trans.noarg(d.liked ? 'unlike' : 'like'),
          },
          hook: bind('click', ctrl.toggleLike),
        },
        '' + d.likes,
      ),
    ]),
    topicsView(ctrl),
    tagsView(ctrl),
  ]);
}

export function side(ctrl: StudyCtrl, withSearch: boolean): VNode {
  const activeTab = ctrl.vm.tab();

  const makeTab = (key: Tab, name: string) =>
    h(
      `span.${key}`,
      {
        class: { active: activeTab === key },
        attrs: { role: 'tab' },
        hook: bind('mousedown', () => ctrl.setTab(key)),
      },
      name,
    );

  const chaptersTab =
    (ctrl.chapters.list.looksNew() && !ctrl.members.canContribute()) ||
    makeTab(
      'chapters',
      ctrl.trans.pluralSame(ctrl.relay ? 'nbGames' : 'nbChapters', ctrl.chapters.list.size()),
    );

  const tabs = h('div.tabs-horiz', { attrs: { role: 'tablist' } }, [
    chaptersTab,
    ctrl.members.size() > 0 && makeTab('members', ctrl.trans.pluralSame('nbMembers', ctrl.members.size())),
    withSearch &&
      h('span.search.narrow', {
        attrs: { ...dataIcon(licon.Search), title: 'Search' },
        hook: bind('click', () => ctrl.search.open(true)),
      }),
    ctrl.members.isOwner() &&
      h('span.more.narrow', {
        attrs: { ...dataIcon(licon.Hamburger), title: 'Edit study' },
        hook: bind('click', () => ctrl.form.open(!ctrl.form.open()), ctrl.redraw),
      }),
  ]);

  const content = (activeTab === 'members' ? memberView : chapterView)(ctrl);

  return h('div.study__side', [tabs, content]);
}

export function contextMenu(ctrl: StudyCtrl, path: Tree.Path, node: Tree.Node): VNode[] {
  return ctrl.vm.mode.write
    ? [
        h(
          'a',
          {
            attrs: dataIcon(licon.BubbleSpeech),
            hook: bind('click', () => {
              ctrl.vm.toolTab('comments');
              ctrl.commentForm.start(ctrl.currentChapter().id, path, node);
            }),
          },
          ctrl.trans.noarg('commentThisMove'),
        ),
        h(
          'a.glyph-icon',
          {
            hook: bind('click', () => {
              ctrl.vm.toolTab('glyphs');
              ctrl.ctrl.userJump(path);
            }),
          },
          ctrl.trans.noarg('annotateWithGlyphs'),
        ),
      ]
    : [];
}

export const overboard = (ctrl: StudyCtrl) =>
  ctrl.chapters.newForm.isOpen()
    ? chapterNewFormView(ctrl.chapters.newForm)
    : ctrl.chapters.editForm.current()
    ? chapterEditFormView(ctrl.chapters.editForm)
    : ctrl.members.inviteForm.open()
    ? inviteFormView(ctrl.members.inviteForm)
    : ctrl.topics.open()
    ? topicsFormView(ctrl.topics, ctrl.members.opts.myId)
    : ctrl.form.open()
    ? studyFormView(ctrl.form)
    : ctrl.search.open()
    ? searchView(ctrl.search)
    : undefined;

export function underboard(ctrl: AnalyseCtrl): MaybeVNodes {
  if (ctrl.studyPractice) return practiceView.underboard(ctrl.study!);
  const study = ctrl.study!,
    toolTab = study.vm.toolTab();
  if (study.gamebookPlay)
    return [gbPlayButtons(ctrl), descView(study, true), descView(study, false), metadata(study)];
  let panel;
  switch (toolTab) {
    case 'tags':
      panel = metadata(study);
      break;
    case 'comments':
      panel = study.vm.mode.write
        ? commentForm.view(ctrl)
        : commentForm.viewDisabled(
            ctrl,
            study.members.canContribute()
              ? 'Press REC to comment moves'
              : 'Only the study members can comment on moves',
          );
      break;
    case 'glyphs':
      panel = ctrl.path
        ? study.vm.mode.write
          ? glyphForm.view(study.glyphForm)
          : glyphForm.viewDisabled('Press REC to annotate moves')
        : glyphForm.viewDisabled('Select a move to annotate');
      break;
    case 'serverEval':
      panel = serverEvalView(study.serverEval);
      if (study?.relay) panel = h('div.eval-chart-and-training', [panel, renderTrainingView(ctrl)]);
      break;
    case 'share':
      panel = studyShareView(study.share);
      break;
    case 'multiBoard':
      panel = multiBoardView(study.multiBoard, study);
      break;
  }
  return [notifView(study.notif), descView(study, true), descView(study, false), buttons(ctrl), panel];
}
