import * as commentForm from './commentForm';
import * as glyphForm from './studyGlyph';
import * as practiceView from './practice/studyPracticeView';
import * as licon from 'lib/licon';
import type * as studyDeps from '../study/studyDeps';
import type AnalyseCtrl from '../ctrl';
import type { Tab, ToolTab } from './interfaces';
import { type VNode, iconTag, bind, dataIcon, type LooseVNodes, onInsert, hl } from 'lib/view';
import { playButtons as gbPlayButtons, overrideButton as gbOverrideButton } from './gamebook/gamebookButtons';
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
import { view as keyboardView } from '../keyboard';
import { view as cevalView } from 'lib/ceval';
import { render as trainingView } from '../view/roundTraining';
import { render as renderKeyboardMove } from 'keyboardMove';
import { renderChat } from 'lib/chat/renderChat';
import { wikiToggleBox } from '../wiki';
import crazyView from '../crazy/crazyView';
import { watchers } from 'lib/view/watchers';
import type StudyCtrl from './studyCtrl';
import { verticalResize } from 'lib/view/verticalResize';
import { displayColumns, shareIcon } from 'lib/device';
import { viewContext, renderBoard, renderMain, renderTools, renderUnderboard } from '../view/components';
import { renderControls } from '../view/controls';
import type { TreeNode, TreePath } from 'lib/tree/types';

export function studyView(ctrl: AnalyseCtrl, study: StudyCtrl, deps: typeof studyDeps): VNode {
  const ctx = viewContext(ctrl, deps);
  const { gamebookPlayView, gaugeOn } = ctx;
  return renderMain(
    ctx,
    ctrl.keyboardHelp && keyboardView(ctrl),
    deps.studyView.overboard(study),
    renderBoard(ctx),
    gaugeOn && cevalView.renderGauge(ctrl),
    crazyView(ctrl, ctrl.topColor(), 'top'),
    gamebookPlayView || renderTools(ctx),
    crazyView(ctrl, ctrl.bottomColor(), 'bottom'),
    !gamebookPlayView && renderControls(ctrl),
    renderUnderboard(ctx),
    ctrl.keyboardMove && renderKeyboardMove(ctrl.keyboardMove),
    trainingView(ctrl),
    ctrl.study?.practice
      ? deps?.studyPracticeView.side(study!)
      : hl(
          'aside.analyse__side',
          {
            hook: onInsert(elm => {
              if (ctrl.opts.$side && ctrl.opts.$side.length) {
                $(elm).replaceWith(ctrl.opts.$side);
                wikiToggleBox();
              }
            }),
          },
          deps?.studyView.studySideNodes(study, true),
        ),
  );
}

export function studySideNodes(ctrl: StudyCtrl, withSearch: boolean): LooseVNodes {
  const activeTab = ctrl.vm.tab();

  const makeTab = (key: Tab, name: string) =>
    hl(
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
    makeTab('chapters', i18n.study[ctrl.relay ? 'nbGames' : 'nbChapters'](ctrl.chapters.list.size()));

  const tabs = hl('div.tabs-horiz', { attrs: { role: 'tablist' } }, [
    chaptersTab,
    ctrl.members.size() > 0 && makeTab('members', i18n.study.nbMembers(ctrl.members.size())),
    withSearch &&
      hl('span.search.narrow', {
        attrs: { ...dataIcon(licon.Search) },
        hook: bind('click', () => ctrl.search.open(true)),
      }),
    ctrl.members.isOwner() &&
      hl('span.more.narrow', {
        attrs: { ...dataIcon(licon.Hamburger), title: i18n.study.editStudy },
        hook: bind('click', () => ctrl.form.open(!ctrl.form.open()), ctrl.redraw),
      }),
  ]);

  const content = (activeTab === 'members' ? memberView : chapterView)(ctrl);
  const trailer = withSearch && sideTrailerNodes(ctrl);
  return [
    hl('div.study__side', [tabs, content, displayColumns() > 2 && trailer]),
    displayColumns() < 3 && trailer,
  ];
}

export function contextMenu(ctrl: StudyCtrl, path: TreePath, node: TreeNode): VNode[] {
  return ctrl.vm.mode.write
    ? [
        hl(
          'a',
          {
            attrs: dataIcon(licon.BubbleSpeech),
            hook: bind('click', () => {
              ctrl.vm.toolTab('comments');
              ctrl.commentForm.start(ctrl.currentChapter().id, path, node);
            }),
          },
          i18n.study.commentThisMove,
        ),
        hl(
          'a.glyph-icon',
          {
            hook: bind('click', () => {
              ctrl.vm.toolTab('glyphs');
              ctrl.ctrl.userJump(path);
            }),
          },
          i18n.study.annotateWithGlyphs,
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

export function underboard(ctrl: AnalyseCtrl): LooseVNodes {
  if (ctrl.study?.practice) return practiceView.underboard(ctrl.study!);
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
      if (study?.relay) panel = hl('div.eval-chart-and-training', [panel, trainingView(ctrl)]);
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

export const resultTag = (s: string) => (s === '1' ? 'good' : s === '0' ? 'bad' : 'status');

interface ToolButtonOpts {
  ctrl: StudyCtrl;
  tab: ToolTab;
  hint: string;
  icon: VNode;
  onClick?: () => void;
  count?: number | string;
}

function toolButton(opts: ToolButtonOpts): VNode {
  return hl(
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
    [!!opts.count && hl('count.data-count', { attrs: { 'data-count': opts.count } }), opts.icon],
  );
}

function buttons(root: AnalyseCtrl): VNode {
  const ctrl: StudyCtrl = root.study!,
    canContribute = ctrl.members.canContribute(),
    showSticky = ctrl.data.features.sticky && (canContribute || (ctrl.vm.behind && ctrl.isUpdatedRecently())),
    gbButton = gbOverrideButton(ctrl);
  return hl('div.study__buttons', [
    hl('div.left-buttons.tabs-horiz', { attrs: { role: 'tablist' } }, [
      // distinct classes (sync, write) allow snabbdom to differentiate buttons
      !!showSticky &&
        hl(
          'a.mode.sync',
          {
            attrs: { title: i18n.study.allSyncMembersRemainOnTheSamePosition },
            class: { on: ctrl.vm.mode.sticky },
            hook: bind('click', ctrl.toggleSticky),
          },
          [ctrl.vm.behind ? hl('span.behind', '' + ctrl.vm.behind) : hl('i.is'), 'SYNC'],
        ),
      canContribute &&
        hl(
          'a.mode.write',
          {
            attrs: { title: i18n.study.shareChanges },
            class: { on: ctrl.vm.mode.write },
            hook: bind('click', ctrl.toggleWrite),
          },
          [hl('i.is'), 'REC'],
        ),
      toolButton({ ctrl, tab: 'tags', hint: i18n.study.pgnTags, icon: iconTag(licon.Tag) }),
      canContribute &&
        toolButton({
          ctrl,
          tab: 'comments',
          hint: i18n.study.commentThisPosition,
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
          hint: i18n.study.annotateWithGlyphs,
          icon: hl('i.glyph-icon'),
          count: (root.node.glyphs || []).length,
        }),
      (canContribute || root.data.analysis) &&
        toolButton({
          ctrl,
          tab: 'serverEval',
          hint: i18n.site.computerAnalysis,
          icon: iconTag(licon.BarChart),
          count: root.data.analysis && '✓',
        }),
      toolButton({ ctrl, tab: 'multiBoard', hint: 'Multiboard', icon: iconTag(licon.Multiboard) }),
      toolButton({ ctrl, tab: 'share', hint: i18n.study.shareAndExport, icon: iconTag(shareIcon()) }),
      !ctrl.relay &&
        !ctrl.data.chapter.gamebook &&
        hl('span.help', {
          attrs: { title: i18n.study.getTheTour, ...dataIcon(licon.InfoCircle) },
          hook: bind('click', ctrl.startTour),
        }),
    ]),
    gbButton && hl('div.right', gbButton),
  ]);
}

function metadata(ctrl: StudyCtrl): VNode {
  const d = ctrl.data,
    title = `${d.name}: ${ctrl.currentChapter().name}`;
  return hl('div.study__metadata', [
    hl('h2', [
      hl('span.name', { attrs: { title } }, [
        d.flair && hl('img.icon-flair', { attrs: { src: site.asset.flairSrc(d.flair) } }),
        title,
      ]),
      hl(
        'span.liking.text',
        {
          class: { liked: d.liked },
          attrs: {
            ...dataIcon(d.liked ? licon.Heart : licon.HeartOutline),
            title: d.liked ? i18n.study.unlike : i18n.study.like,
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

function sideTrailerNodes(study: StudyCtrl): LooseVNodes {
  const showChat = study.ctrl.chatCtrl && study?.data.settings.chat !== 'nobody';
  const resizeId = displayColumns() > 2 && `studySide/${study?.data.id}`;
  return [
    resizeId &&
      verticalResize({
        selector: '.study-list',
        key: 'study-list',
        id: resizeId,
        min: () => 28,
        max: () => 28 * 64,
        initialMaxHeight: () => 28 * 6,
      }),
    showChat && renderChat(study.ctrl.chatCtrl!),
    showChat &&
      resizeId &&
      verticalResize({
        key: 'study-chat',
        id: resizeId,
        min: () => 34,
        max: () => window.innerHeight,
        initialMaxHeight: () => window.innerHeight / 3,
      }),
    hl('div.chat__members.none', { hook: onInsert(watchers) }),
  ];
}
