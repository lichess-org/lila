import { Config as CgConfig } from 'chessground/config';
import { DrawShape } from 'chessground/draw';
import { prop, defined } from 'common';
import throttle, { throttlePromiseDelay } from 'common/throttle';
import debounce from 'common/debounce';
import AnalyseCtrl from '../ctrl';
import { ctrl as memberCtrl } from './studyMembers';
import practiceCtrl from './practice/studyPracticeCtrl';
import { StudyPracticeData, StudyPracticeCtrl } from './practice/interfaces';
import { ctrl as commentFormCtrl, CommentForm } from './commentForm';
import { ctrl as glyphFormCtrl, GlyphCtrl } from './studyGlyph';
import { ctrl as studyFormCtrl } from './studyForm';
import TopicsCtrl from './topics';
import { ctrl as notifCtrl } from './notif';
import { ctrl as shareCtrl } from './studyShare';
import { ctrl as tagsCtrl } from './studyTags';
import ServerEval from './serverEval';
import * as tours from './studyTour';
import * as xhr from './studyXhr';
import { path as treePath } from 'tree';
import {
  StudyCtrl,
  StudyVm,
  Tab,
  ToolTab,
  TagTypes,
  StudyData,
  StudyChapterMeta,
  ReloadData,
  WithWhoAndPos,
  WithChapterId,
  WithWhoAndChap,
  WithWho,
  WithPosition,
  TagArray,
  StudyChapterRelay,
} from './interfaces';
import GamebookPlayCtrl from './gamebook/gamebookPlayCtrl';
import { DescriptionCtrl } from './description';
import RelayCtrl from './relay/relayCtrl';
import { RelayData } from './relay/interfaces';
import { MultiBoardCtrl } from './multiBoard';
import { StudySocketSendParams } from '../socket';
import { Opening } from '../explorer/interfaces';
import { storedMap, storedBooleanProp } from 'common/storage';
import { opposite } from 'chessops/util';
import StudyChaptersCtrl from './studyChapters';
import { SearchCtrl } from './studySearch';

interface Handlers {
  path(d: WithWhoAndPos): void;
  addNode(
    d: WithWhoAndPos & { d: string; n: Tree.Node; o: Opening; s: boolean; relay?: StudyChapterRelay }
  ): void;
  deleteNode(d: WithWhoAndPos): void;
  promote(d: WithWhoAndPos & { toMainline: boolean }): void;
  liking(d: WithWho & { l: { likes: number; me: boolean } }): void;
  shapes(d: WithWhoAndPos & { s: DrawShape[] }): void;
  members(d: { [id: string]: { user: { name: string; id: string }; role: 'r' | 'w' } }): void;
  setComment(d: WithWhoAndPos & { c: Tree.Comment }): void;
  deleteComment(d: WithWhoAndPos & { id: string }): void;
  glyphs(d: WithWhoAndPos & { g: Tree.Glyph[] }): void;
  clock(d: WithWhoAndPos & { c?: number }): void;
  forceVariation(d: WithWhoAndPos & { force: boolean }): void;
  chapters(d: StudyChapterMeta[]): void;
  reload(d: null | WithChapterId): void;
  changeChapter(d: WithWhoAndPos): void;
  updateChapter(d: WithWhoAndChap): void;
  descChapter(d: WithWhoAndChap & { desc?: string }): void;
  descStudy(d: WithWho & { desc?: string }): void;
  setTopics(d: WithWho & { topics: string[] }): void;
  addChapter(d: WithWhoAndPos & { s: boolean }): void;
  conceal(d: WithPosition & { ply: Ply }): void;
  setTags(d: WithWhoAndChap & { tags: TagArray[] }): void;
  validationError(d: { error: string }): void;
  error(msg: string): void;
}

// data.position.path represents the server state
// ctrl.path is the client state
export default function (
  data: StudyData,
  ctrl: AnalyseCtrl,
  tagTypes: TagTypes,
  practiceData?: StudyPracticeData,
  relayData?: RelayData
): StudyCtrl {
  const send = ctrl.socket.send;
  const redraw = ctrl.redraw;

  const relayRecProp = storedBooleanProp('analyse.relay.rec', true);
  const nonRelayRecMapProp = storedMap<boolean>('study.rec', 100, () => true);
  const chapterFlipMapProp = storedMap<boolean>('chapter.flip', 400, () => false);

  const vm: StudyVm = (() => {
    const isManualChapter = data.chapter.id !== data.position.chapterId;
    const sticked = data.features.sticky && !ctrl.initialPath && !isManualChapter && !practiceData;
    return {
      loading: false,
      tab: prop<Tab>(relayData || data.chapters.length > 1 ? 'chapters' : 'members'),
      toolTab: prop<ToolTab>('tags'),
      chapterId: sticked ? data.position.chapterId : data.chapter.id,
      // path is at ctrl.path
      mode: {
        sticky: sticked,
        write: relayData ? relayRecProp() : nonRelayRecMapProp(data.id),
      },
      // how many events missed because sync=off
      behind: 0,
      // how stale is the study
      updatedAt: Date.now() - data.secondsSinceUpdate * 1000,
      gamebookOverride: undefined,
    };
  })();

  const notif = notifCtrl(redraw);

  const startTour = () => tours.study(ctrl);

  const setTab = (tab: Tab) => {
    relay?.tourShow.disable();
    vm.tab(tab);
    redraw();
  };

  const members = memberCtrl({
    initDict: data.members,
    myId: practiceData ? undefined : ctrl.opts.userId,
    ownerId: data.ownerId,
    send,
    tab: vm.tab,
    startTour,
    notif,
    onBecomingContributor() {
      vm.mode.write = !relayData || relayRecProp();
    },
    admin: data.admin,
    redraw,
    trans: ctrl.trans,
  });

  const chapters = new StudyChaptersCtrl(
    data.chapters,
    send,
    () => setTab('chapters'),
    chapterId => xhr.chapterConfig(data.id, chapterId),
    ctrl
  );

  const currentChapter = (): StudyChapterMeta => chapters.get(vm.chapterId)!;

  const isChapterOwner = (): boolean => ctrl.opts.userId === data.chapter.ownerId;

  const multiBoard = new MultiBoardCtrl(data.id, redraw, ctrl.trans);

  const relay = relayData
    ? new RelayCtrl(data.id, relayData, send, redraw, members, data.chapter)
    : undefined;

  const form = studyFormCtrl(
    (d, isNew) => {
      send('editStudy', d);
      if (
        isNew &&
        data.chapter.setup.variant.key === 'standard' &&
        ctrl.mainline.length === 1 &&
        !data.chapter.setup.fromFen &&
        !relay
      )
        chapters.newForm.openInitial();
    },
    () => data,
    ctrl.trans,
    redraw,
    relay
  );

  const isWriting = (): boolean => vm.mode.write && !isGamebookPlay();

  function makeChange(...args: StudySocketSendParams): boolean {
    if (isWriting()) {
      send(...args);
      return true;
    }
    return (vm.mode.sticky = false);
  }

  const commentForm: CommentForm = commentFormCtrl(ctrl);
  const glyphForm: GlyphCtrl = glyphFormCtrl(ctrl);
  const tags = tagsCtrl(ctrl, () => data.chapter, tagTypes);
  const studyDesc = new DescriptionCtrl(
    data.description,
    debounce(t => {
      data.description = t;
      send('descStudy', t);
    }, 500),
    redraw
  );
  const chapterDesc = new DescriptionCtrl(
    data.chapter.description,
    debounce(t => {
      data.chapter.description = t;
      send('descChapter', { id: vm.chapterId, desc: t });
    }, 500),
    redraw
  );

  const serverEval = new ServerEval(ctrl, () => vm.chapterId);

  const search = new SearchCtrl(relay?.fullRoundName() || data.name, chapters.list, setChapter, redraw);

  const topics: TopicsCtrl = new TopicsCtrl(
    topics => send('setTopics', topics),
    () => data.topics || [],
    ctrl.trans,
    redraw
  );

  function addChapterId<T>(req: T): T & { ch: string } {
    return {
      ...req,
      ch: vm.chapterId,
    };
  }

  const isGamebookPlay = () =>
    data.chapter.gamebook &&
    vm.gamebookOverride !== 'analyse' &&
    (vm.gamebookOverride === 'play' || !members.canContribute());

  if (vm.mode.sticky && !isGamebookPlay()) ctrl.userJump(data.position.path);
  else if (data.chapter.relay && !defined(ctrl.requestInitialPly)) ctrl.userJump(data.chapter.relay.path);

  function configureAnalysis() {
    const canContribute = members.canContribute();
    // unwrite if member lost privileges
    vm.mode.write = vm.mode.write && canContribute;
    lichess.pubsub.emit('chat.writeable', data.features.chat);
    lichess.pubsub.emit('chat.permissions', { local: canContribute });
    lichess.pubsub.emit('palantir.toggle', data.features.chat && !!members.myMember());
    const computer: boolean =
      !isGamebookPlay() && !!(data.chapter.features.computer || data.chapter.practice);
    if (!computer) ctrl.getCeval().enabled(false);
    ctrl.getCeval().allowed(computer);
    if (!data.chapter.features.explorer) ctrl.explorer.disable();
    ctrl.explorer.allowed(data.chapter.features.explorer);
  }
  configureAnalysis();

  function configurePractice() {
    if (!data.chapter.practice && ctrl.practice) ctrl.togglePractice();
    if (data.chapter.practice) ctrl.restartPractice();
    if (practice) practice.onLoad();
  }

  function onReload(d: ReloadData) {
    const s = d.study!;
    const prevPath = ctrl.path;
    const sameChapter = data.chapter.id === s.chapter.id;
    vm.mode.sticky = (vm.mode.sticky && s.features.sticky) || (!data.features.sticky && s.features.sticky);
    if (vm.mode.sticky) vm.behind = 0;
    data.position = s.position;
    data.name = s.name;
    data.visibility = s.visibility;
    data.features = s.features;
    data.settings = s.settings;
    data.chapter = s.chapter;
    data.likes = s.likes;
    data.liked = s.liked;
    data.description = s.description;
    chapterDesc.set(data.chapter.description);
    studyDesc.set(data.description);
    document.title = data.name;
    members.dict(s.members);
    chapters.list(s.chapters);
    ctrl.flipped = chapterFlipMapProp(data.chapter.id);

    const merge = !vm.mode.write && sameChapter;
    ctrl.reloadData(d.analysis, merge);
    vm.gamebookOverride = undefined;
    configureAnalysis();
    vm.loading = false;

    instanciateGamebookPlay();
    if (relay) relay.applyChapterRelay(data.chapter, s.chapter.relay);

    let nextPath: Tree.Path;

    if (vm.mode.sticky) {
      vm.chapterId = data.position.chapterId;
      nextPath =
        (vm.justSetChapterId === vm.chapterId && chapters.localPaths[vm.chapterId]) || data.position.path;
    } else {
      nextPath = sameChapter
        ? prevPath
        : data.chapter.relay
        ? data.chapter.relay!.path
        : chapters.localPaths[vm.chapterId] || treePath.root;
    }

    // path could be gone (because of subtree deletion), go as far as possible
    ctrl.userJump(ctrl.tree.longestValidPath(nextPath));

    vm.justSetChapterId = undefined;

    configurePractice();
    serverEval.reset();
    commentForm.onSetPath(data.chapter.id, ctrl.path, ctrl.node);
    redraw();
    ctrl.startCeval();
  }

  const xhrReload = throttlePromiseDelay(
    () => 700,
    () => {
      vm.loading = true;
      return xhr
        .reload(practice ? 'practice/load' : 'study', data.id, vm.mode.sticky ? undefined : vm.chapterId)
        .then(onReload, lichess.reload);
    }
  );

  const onSetPath = throttle(300, (path: Tree.Path) => {
    if (vm.mode.sticky && path !== data.position.path)
      makeChange(
        'setPath',
        addChapterId({
          path,
        })
      );
  });

  ctrl.flipped = chapterFlipMapProp(data.chapter.id);
  if (members.canContribute()) form.openIfNew();

  const currentNode = () => ctrl.node;
  const onMainline = () => ctrl.tree.pathIsMainline(ctrl.path);
  const bottomColor = () =>
    ctrl.flipped ? opposite(data.chapter.setup.orientation) : data.chapter.setup.orientation;

  const share = shareCtrl(
    data,
    currentChapter,
    currentNode,
    onMainline,
    bottomColor,
    relay,
    redraw,
    ctrl.trans
  );

  const practice: StudyPracticeCtrl | undefined = practiceData && practiceCtrl(ctrl, data, practiceData);

  let gamebookPlay: GamebookPlayCtrl | undefined;

  function instanciateGamebookPlay() {
    if (!isGamebookPlay()) return (gamebookPlay = undefined);
    if (gamebookPlay && gamebookPlay.chapterId === vm.chapterId) return;
    gamebookPlay = new GamebookPlayCtrl(ctrl, vm.chapterId, ctrl.trans, redraw);
    vm.mode.sticky = false;
    return undefined;
  }
  instanciateGamebookPlay();

  function mutateCgConfig(config: Required<Pick<CgConfig, 'drawable'>>) {
    config.drawable.onChange = (shapes: Tree.Shape[]) => {
      if (vm.mode.write) {
        ctrl.tree.setShapes(shapes, ctrl.path);
        makeChange(
          'shapes',
          addChapterId({
            path: ctrl.path,
            shapes,
          })
        );
      }
      gamebookPlay && gamebookPlay.onShapeChange(shapes);
    };
  }

  function wrongChapter(serverData: WithPosition & { s?: boolean }): boolean {
    if (serverData.p.chapterId !== vm.chapterId) {
      // sticky should really be on the same chapter
      if (vm.mode.sticky && serverData.s) xhrReload();
      return true;
    }
    return false;
  }

  function setMemberActive(who?: { u: string }) {
    who && members.setActive(who.u);
    vm.updatedAt = Date.now();
  }

  function withPosition<T>(obj: T): T & { ch: string; path: string } {
    return { ...obj, ch: vm.chapterId, path: ctrl.path };
  }

  const likeToggler = debounce(() => send('like', { liked: data.liked }), 1000);

  function setChapter(id: string, force?: boolean) {
    const alreadySet = id === vm.chapterId && !force;
    if (relay?.tourShow.active) {
      relay.tourShow.disable();
      if (alreadySet) redraw();
    }
    if (alreadySet) return;
    if (!vm.mode.sticky || !makeChange('setChapter', id)) {
      vm.mode.sticky = false;
      if (!vm.behind) vm.behind = 1;
      vm.chapterId = id;
      xhrReload();
    }
    vm.loading = true;
    vm.nextChapterId = id;
    vm.justSetChapterId = id;
    redraw();
  }

  const [prevChapter, nextChapter] = [-1, +1].map(delta => (): StudyChapterMeta | undefined => {
    const chs = chapters.list();
    const i = chs.findIndex(ch => ch.id === vm.chapterId);
    return i < 0 ? undefined : chs[i + delta];
  });
  const hasNextChapter = () => {
    const chs = chapters.list();
    return chs[chs.length - 1].id != vm.chapterId;
  };

  const socketHandlers: Handlers = {
    path(d) {
      const position = d.p,
        who = d.w;
      setMemberActive(who);
      if (!vm.mode.sticky) {
        vm.behind++;
        return redraw();
      }
      if (position.chapterId !== data.position.chapterId || !ctrl.tree.pathExists(position.path)) {
        return xhrReload();
      }
      data.position.path = position.path;
      if (who && who.s === lichess.sri) return;
      ctrl.userJump(position.path);
      redraw();
    },
    addNode(d) {
      const position = d.p,
        node = d.n,
        who = d.w,
        sticky = d.s;
      setMemberActive(who);
      if (vm.toolTab() == 'multiBoard' || (relay && relay.tourShow.active)) multiBoard.addNode(d.p, d.n);
      if (sticky && !vm.mode.sticky) vm.behind++;
      if (wrongChapter(d)) {
        if (sticky && !vm.mode.sticky) redraw();
        return;
      }
      if (sticky && who && who.s === lichess.sri) {
        data.position.path = position.path + node.id;
        return;
      }
      if (relay) relay.applyChapterRelay(data.chapter, d.relay);
      const newPath = ctrl.tree.addNode(node, position.path);
      if (!newPath) return xhrReload();
      ctrl.tree.addDests(d.d, newPath);
      if (sticky) data.position.path = newPath;
      if (
        (sticky && vm.mode.sticky) ||
        (position.path === ctrl.path && position.path === treePath.fromNodeList(ctrl.mainline))
      )
        ctrl.jump(newPath);
      redraw();
    },
    deleteNode(d) {
      const position = d.p,
        who = d.w;
      setMemberActive(who);
      if (wrongChapter(d)) return;
      // deleter already has it done
      if (who && who.s === lichess.sri) return;
      if (!ctrl.tree.pathExists(d.p.path)) return xhrReload();
      ctrl.tree.deleteNodeAt(position.path);
      if (vm.mode.sticky) ctrl.jump(ctrl.path);
      redraw();
    },
    promote(d) {
      const position = d.p,
        who = d.w;
      setMemberActive(who);
      if (wrongChapter(d)) return;
      if (who && who.s === lichess.sri) return;
      if (!ctrl.tree.pathExists(d.p.path)) return xhrReload();
      ctrl.tree.promoteAt(position.path, d.toMainline);
      if (vm.mode.sticky) ctrl.jump(ctrl.path);
      ctrl.treeVersion++;
      redraw();
    },
    reload: xhrReload,
    changeChapter(d) {
      setMemberActive(d.w);
      if (!vm.mode.sticky) vm.behind++;
      data.position = d.p;
      if (vm.mode.sticky) xhrReload();
      else redraw();
    },
    updateChapter(d) {
      setMemberActive(d.w);
      xhrReload();
    },
    descChapter(d) {
      setMemberActive(d.w);
      if (d.w && d.w.s === lichess.sri) return;
      if (data.chapter.id === d.chapterId) {
        data.chapter.description = d.desc;
        chapterDesc.set(d.desc);
      }
      redraw();
    },
    descStudy(d) {
      setMemberActive(d.w);
      if (d.w && d.w.s === lichess.sri) return;
      data.description = d.desc;
      studyDesc.set(d.desc);
      redraw();
    },
    setTopics(d) {
      setMemberActive(d.w);
      data.topics = d.topics;
      redraw();
    },
    addChapter(d) {
      setMemberActive(d.w);
      if (d.s && !vm.mode.sticky) vm.behind++;
      if (d.s) data.position = d.p;
      else if (d.w && d.w.s === lichess.sri) {
        vm.mode.write = relayData ? relayRecProp() : nonRelayRecMapProp(data.id);
        vm.chapterId = d.p.chapterId;
      }
      xhrReload();
    },
    members(d) {
      members.update(d);
      configureAnalysis();
      redraw();
    },
    chapters(d) {
      chapters.list(d);
      if (vm.toolTab() == 'multiBoard' || (relay && relay.tourShow.active)) multiBoard.addResult(d);
      if (!currentChapter()) {
        vm.chapterId = d[0].id;
        if (!vm.mode.sticky) xhrReload();
      }
      redraw();
    },
    shapes(d) {
      const position = d.p,
        who = d.w;
      setMemberActive(who);
      if (d.p.chapterId !== vm.chapterId) return;
      if (who && who.s === lichess.sri) return redraw(); // update shape indicator in column move view
      ctrl.tree.setShapes(d.s, ctrl.path);
      if (ctrl.path === position.path) ctrl.withCg(cg => cg.setShapes(d.s));
      redraw();
    },
    validationError(d) {
      alert(d.error);
    },
    setComment(d) {
      const position = d.p,
        who = d.w;
      setMemberActive(who);
      if (wrongChapter(d)) return;
      ctrl.tree.setCommentAt(d.c, position.path);
      redraw();
    },
    setTags(d) {
      setMemberActive(d.w);
      if (d.chapterId !== vm.chapterId) return;
      data.chapter.tags = d.tags;
      redraw();
    },
    deleteComment(d) {
      const position = d.p,
        who = d.w;
      setMemberActive(who);
      if (wrongChapter(d)) return;
      ctrl.tree.deleteCommentAt(d.id, position.path);
      redraw();
    },
    glyphs(d) {
      const position = d.p,
        who = d.w;
      setMemberActive(who);
      if (wrongChapter(d)) return;
      ctrl.tree.setGlyphsAt(d.g, position.path);
      if (ctrl.path === position.path) ctrl.setAutoShapes();
      redraw();
    },
    clock(d) {
      const position = d.p,
        who = d.w;
      setMemberActive(who);
      if (wrongChapter(d)) return;
      ctrl.tree.setClockAt(d.c, position.path);
      redraw();
    },
    forceVariation(d) {
      const position = d.p,
        who = d.w;
      setMemberActive(who);
      if (wrongChapter(d)) return;
      ctrl.tree.forceVariationAt(position.path, d.force);
      redraw();
    },
    conceal(d) {
      if (wrongChapter(d)) return;
      data.chapter.conceal = d.ply;
      redraw();
    },
    liking(d) {
      data.likes = d.l.likes;
      if (d.w && d.w.s === lichess.sri) data.liked = d.l.me;
      redraw();
    },
    error(msg: string) {
      alert(msg);
    },
  };

  return {
    data,
    form,
    setTab,
    members,
    chapters,
    notif,
    commentForm,
    glyphForm,
    serverEval,
    share,
    tags,
    studyDesc,
    chapterDesc,
    topics,
    search,
    vm,
    relay,
    multiBoard,
    isUpdatedRecently() {
      return Date.now() - vm.updatedAt < 300 * 1000;
    },
    toggleLike() {
      data.liked = !data.liked;
      redraw();
      likeToggler();
    },
    position() {
      return data.position;
    },
    currentChapter,
    isChapterOwner,
    canJumpTo(path: Tree.Path) {
      if (gamebookPlay) return gamebookPlay.canJumpTo(path);
      return (
        data.chapter.conceal === undefined ||
        isChapterOwner() ||
        treePath.contains(ctrl.path, path) || // can always go back
        ctrl.tree.lastMainlineNode(path).ply <= data.chapter.conceal!
      );
    },
    onJump() {
      if (gamebookPlay) gamebookPlay.onJump();
      else chapters.localPaths[vm.chapterId] = ctrl.path; // don't remember position on gamebook
      if (practice) practice.onJump();
    },
    onFlip() {
      chapterFlipMapProp(data.chapter.id, ctrl.flipped);
    },
    withPosition,
    setPath(path, node) {
      onSetPath(path);
      commentForm.onSetPath(vm.chapterId, path, node);
    },
    deleteNode(path) {
      makeChange(
        'deleteNode',
        addChapterId({
          path,
          jumpTo: ctrl.path,
        })
      );
    },
    promote(path, toMainline) {
      makeChange(
        'promote',
        addChapterId({
          toMainline,
          path,
        })
      );
    },
    forceVariation(path, force) {
      makeChange(
        'forceVariation',
        addChapterId({
          force,
          path,
        })
      );
    },
    setChapter,
    toggleSticky() {
      vm.mode.sticky = !vm.mode.sticky && data.features.sticky;
      xhrReload();
    },
    toggleWrite() {
      vm.mode.write = !vm.mode.write && members.canContribute();
      if (relayData) relayRecProp(vm.mode.write);
      else nonRelayRecMapProp(data.id, vm.mode.write);
      xhrReload();
    },
    isWriting,
    makeChange,
    startTour,
    userJump: ctrl.userJump,
    currentNode,
    practice,
    gamebookPlay: () => gamebookPlay,
    prevChapter,
    nextChapter,
    hasNextChapter,
    goToPrevChapter() {
      const chapter = prevChapter();
      if (chapter) setChapter(chapter.id);
    },
    goToNextChapter() {
      const chapter = nextChapter();
      if (chapter) setChapter(chapter.id);
    },
    setGamebookOverride(o) {
      vm.gamebookOverride = o;
      instanciateGamebookPlay();
      configureAnalysis();
      ctrl.userJump(ctrl.path);
      if (!o) xhrReload();
    },
    mutateCgConfig,
    explorerGame(gameId: string, insert: boolean) {
      makeChange('explorerGame', withPosition({ gameId, insert }));
    },
    onPremoveSet() {
      if (gamebookPlay) gamebookPlay.onPremoveSet();
    },
    looksNew() {
      const cs = chapters.list();
      return cs.length == 1 && cs[0].name == 'Chapter 1' && !currentChapter().ongoing;
    },
    redraw,
    trans: ctrl.trans,
    socketHandler: (t: string, d: any) => {
      const handler = (socketHandlers as any as SocketHandlers)[t];
      if (handler) {
        handler(d);
        return true;
      }
      return !!relay && relay.socketHandler(t, d);
    },
  };
}
