import { prop } from 'common';
import throttle from 'common/throttle';
import debounce from 'common/debounce';
import AnalyseCtrl from '../ctrl';
import { ctrl as memberCtrl } from './studyMembers';
import { ctrl as chapterCtrl } from './studyChapters';
import practiceCtrl from './practice/studyPracticeCtrl';
import { StudyPracticeData, StudyPracticeCtrl } from './practice/interfaces';
import { ctrl as commentFormCtrl, CommentForm } from './commentForm';
import { ctrl as glyphFormCtrl, GlyphCtrl } from './studyGlyph';
import { ctrl as studyFormCtrl, StudyFormCtrl } from './studyForm';
import { ctrl as topicsCtrl, TopicsCtrl } from './topics';
import { ctrl as notifCtrl } from './notif';
import { ctrl as shareCtrl } from './studyShare';
import { ctrl as tagsCtrl } from './studyTags';
import { ctrl as serverEvalCtrl } from './serverEval';
import * as tours from './studyTour';
import * as xhr from './studyXhr';
import { path as treePath } from 'tree';
import { StudyCtrl, StudyVm, Tab, ToolTab, TagTypes, StudyData, StudyChapterMeta, ReloadData } from './interfaces';
import GamebookPlayCtrl from './gamebook/gamebookPlayCtrl';
import { DescriptionCtrl } from './description';
import RelayCtrl from './relay/relayCtrl';
import { RelayData } from './relay/interfaces';
import { MultiBoardCtrl } from './multiBoard';
import { Req } from '../socket';

// data.position.path represents the server state
// ctrl.path is the client state
export default function(data: StudyData, ctrl: AnalyseCtrl, tagTypes: TagTypes, practiceData?: StudyPracticeData, relayData?: RelayData): StudyCtrl {

  const send = ctrl.socket.send;
  const redraw = ctrl.redraw;

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
        write: true
      },
      // how many events missed because sync=off
      behind: 0,
      // how stale is the study
      updatedAt: Date.now() - data.secondsSinceUpdate * 1000,
      gamebookOverride: undefined
    };
  })();

  const notif = notifCtrl(redraw);

  function startTour() {
    tours.study(ctrl);
  };

  const members = memberCtrl({
    initDict: data.members,
    myId: practiceData ? null : ctrl.opts.userId,
    ownerId: data.ownerId,
    send,
    tab: vm.tab,
    startTour,
    notif,
    onBecomingContributor() {
      vm.mode.write = true;
    },
    admin: data.admin,
    redraw,
    trans: ctrl.trans
  });

  const chapters = chapterCtrl(
    data.chapters,
    send,
    () => vm.tab('chapters'),
    chapterId => xhr.chapterConfig(data.id, chapterId),
    ctrl);

  function currentChapter(): StudyChapterMeta {
    return chapters.get(vm.chapterId)!;
  };
  function isChapterOwner() {
    return ctrl.opts.userId === data.chapter.ownerId;
  };

  const multiBoard = new MultiBoardCtrl(data.id, redraw, ctrl.trans);

  const relay = relayData ? new RelayCtrl(relayData, send, redraw, members, data.chapter) : undefined;

  const form: StudyFormCtrl = studyFormCtrl((d, isNew) => {
    send("editStudy", d);
    if (isNew && data.chapter.setup.variant.key === 'standard' && ctrl.mainline.length === 1 && !data.chapter.setup.fromFen && !relay)
      chapters.newForm.openInitial();
  }, () => data, ctrl.trans, redraw, relay);

  function isWriting(): boolean {
    return vm.mode.write && !isGamebookPlay();
  }

  function makeChange(t: string, d: any): boolean {
    if (isWriting()) {
      send(t, d);
      return true;
    }
    return vm.mode.sticky = false;
  };

  const commentForm: CommentForm = commentFormCtrl(ctrl);
  const glyphForm: GlyphCtrl = glyphFormCtrl(ctrl);
  const tags = tagsCtrl(ctrl, () => data.chapter, tagTypes);
  const studyDesc = new DescriptionCtrl(data.description, t => {
    data.description = t;
    send("descStudy", t);
  }, redraw);
  const chapterDesc = new DescriptionCtrl(data.chapter.description, t => {
    data.chapter.description = t;
    send("descChapter", { id: vm.chapterId, desc: t });
  }, redraw);

  const serverEval = serverEvalCtrl(ctrl, () => vm.chapterId);

  const topics: TopicsCtrl = topicsCtrl(
    topics => send("setTopics", topics),
    () => data.topics || [], ctrl.trans, redraw);

  function addChapterId(req: Req) {
    req.ch = vm.chapterId;
    return req;
  }

  function isGamebookPlay() {
    return data.chapter.gamebook && vm.gamebookOverride !== 'analyse' &&
      (vm.gamebookOverride === 'play' || !members.canContribute());
  }

  if (vm.mode.sticky && !isGamebookPlay()) ctrl.userJump(data.position.path);
  else if (data.chapter.relay && !ctrl.initialPath) ctrl.userJump(data.chapter.relay.path);

  function configureAnalysis() {
    if (ctrl.embed) return;
    const canContribute = members.canContribute();
    // unwrite if member lost privileges
    vm.mode.write = vm.mode.write && canContribute;
    lichess.pubsub.emit('chat.writeable', data.features.chat);
    lichess.pubsub.emit('chat.permissions', {local: canContribute});
    lichess.pubsub.emit('palantir.toggle', data.features.chat && !!members.myMember());
    const computer: boolean = !isGamebookPlay() && !!(data.chapter.features.computer || data.chapter.practice);
    if (!computer) ctrl.getCeval().enabled(false);
    ctrl.getCeval().allowed(computer);
    if (!data.chapter.features.explorer) ctrl.explorer.disable();
    ctrl.explorer.allowed(data.chapter.features.explorer);
  };
  configureAnalysis();

  function configurePractice() {
    if (!data.chapter.practice && ctrl.practice) ctrl.togglePractice();
    if (data.chapter.practice) ctrl.restartPractice();
    if (practice) practice.onLoad();
  };

  function onReload(d: ReloadData) {
    const s = d.study!;
    const prevPath = ctrl.path;
    const sameChapter = data.chapter.id === s.chapter.id;
    vm.mode.sticky = (vm.mode.sticky && s.features.sticky) || (!data.features.sticky && s.features.sticky);
    if (vm.mode.sticky) vm.behind = 0;
    'position name visibility features settings chapter likes liked description'.split(' ').forEach(key => {
      data[key] = s[key];
    });
    chapterDesc.set(data.chapter.description);
    studyDesc.set(data.description);
    document.title = data.name;
    members.dict(s.members);
    chapters.list(s.chapters);
    ctrl.flipped = false;

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
      nextPath = (
        (vm.justSetChapterId === vm.chapterId) && chapters.localPaths[vm.chapterId]
      ) || data.position.path;
    } else {
      nextPath = sameChapter ? prevPath : (
        data.chapter.relay ? data.chapter.relay!.path : (chapters.localPaths[vm.chapterId] || treePath.root)
      );
    }

    // path could be gone (because of subtree deletion), go as far as possible
    ctrl.userJump(ctrl.tree.longestValidPath(nextPath));

    vm.justSetChapterId = undefined;

    configurePractice();
    serverEval.reset();
    commentForm.onSetPath(data.chapter.id, ctrl.path, ctrl.node, false);

    redraw();
    ctrl.startCeval();
  };

  const xhrReload = throttle(700, () => {
    vm.loading = true;
    return xhr.reload(
      practice ? 'practice/load' : 'study',
      data.id,
      vm.mode.sticky ? undefined : vm.chapterId
    ).then(onReload, lichess.reload);
  });

  const onSetPath = throttle(300, (path: Tree.Path) => {
    if (vm.mode.sticky && path !== data.position.path) makeChange("setPath", addChapterId({
      path
    }));
  });

  if (members.canContribute()) form.openIfNew();

  function currentNode() {
    return ctrl.node;
  };

  const share = shareCtrl(data, currentChapter, currentNode, !!relay, redraw, ctrl.trans);

  const practice: StudyPracticeCtrl | undefined = practiceData && practiceCtrl(ctrl, data, practiceData);

  let gamebookPlay: GamebookPlayCtrl | undefined;

  function instanciateGamebookPlay() {
    if (!isGamebookPlay()) return gamebookPlay = undefined;
    if (gamebookPlay && gamebookPlay.chapterId === vm.chapterId) return;
    gamebookPlay = new GamebookPlayCtrl(ctrl, vm.chapterId, ctrl.trans, redraw);
    vm.mode.sticky = false;
    return undefined;
  }
  instanciateGamebookPlay();

  function mutateCgConfig(config) {
    config.drawable.onChange = (shapes: Tree.Shape[]) => {
      if (vm.mode.write) {
        ctrl.tree.setShapes(shapes, ctrl.path);
        makeChange("shapes", addChapterId({
          path: ctrl.path,
          shapes
        }));
      }
      gamebookPlay && gamebookPlay.onShapeChange(shapes);
    };
  }

  function wrongChapter(serverData) {
    if (serverData.p.chapterId !== vm.chapterId) {
      // sticky should really be on the same chapter
      if (vm.mode.sticky && serverData.sticky) xhrReload();
      return true;
    }
    return undefined;
  }

  function setMemberActive(who?: {u: string}) {
    who && members.setActive(who.u);
    vm.updatedAt = Date.now();
  }

  function withPosition(obj: any) {
    obj.ch = vm.chapterId;
    obj.path = ctrl.path;
    return obj;
  }

  const likeToggler = debounce(() => send("like", { liked: data.liked }), 1000);

  const socketHandlers = {
    path(d) {
      const position = d.p,
        who = d.w;
      setMemberActive(who);
      if (!vm.mode.sticky) {
        vm.behind++;
        return redraw();
      }
      if (position.chapterId !== data.position.chapterId ||
        !ctrl.tree.pathExists(position.path)) {
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
      if (vm.toolTab() == 'multiBoard' || relay && relay.intro.active) multiBoard.addNode(d.p, d.n);
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
      if ((sticky && vm.mode.sticky) || (
        position.path === ctrl.path &&
        position.path === treePath.fromNodeList(ctrl.mainline)
      )) ctrl.jump(newPath);
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
        vm.mode.write = true;
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
      if (wrongChapter(d)) return;
      if (who && who.s === lichess.sri) return;
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
    }
  };

  return {
    data,
    form,
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
      return data.chapter.conceal === undefined ||
        isChapterOwner() ||
        treePath.contains(ctrl.path, path) || // can always go back
        ctrl.tree.lastMainlineNode(path).ply <= data.chapter.conceal!;
    },
    onJump() {
      if (gamebookPlay) gamebookPlay.onJump();
      else chapters.localPaths[vm.chapterId] = ctrl.path; // don't remember position on gamebook
      if (practice) practice.onJump();
    },
    withPosition,
    setPath(path, node, playedMyself) {
      onSetPath(path);
      commentForm.onSetPath(vm.chapterId, path, node, playedMyself);
    },
    deleteNode(path) {
      makeChange("deleteNode", addChapterId({
        path,
        jumpTo: ctrl.path
      }));
    },
    promote(path, toMainline) {
      makeChange("promote", addChapterId({
        toMainline,
        path
      }));
    },
    forceVariation(path, force) {
      makeChange("forceVariation", addChapterId({
        force,
        path
      }));
    },
    setChapter(id, force) {
      const alreadySet = id === vm.chapterId && !force;
      if (relay && relay.intro.active) {
        relay.intro.disable();
        if (alreadySet) redraw();
      }
      if (alreadySet) return;
      if (!vm.mode.sticky || !makeChange("setChapter", id)) {
        vm.mode.sticky = false;
        if (!vm.behind) vm.behind = 1;
        vm.chapterId = id;
        xhrReload();
      }
      vm.loading = true;
      vm.nextChapterId = id;
      vm.justSetChapterId = id;
      redraw();
    },
    toggleSticky() {
      vm.mode.sticky = !vm.mode.sticky && data.features.sticky;
      xhrReload();
    },
    toggleWrite() {
      vm.mode.write = !vm.mode.write && members.canContribute();
      xhrReload();
    },
    isWriting,
    makeChange,
    startTour,
    userJump: ctrl.userJump,
    currentNode,
    practice,
    gamebookPlay: () => gamebookPlay,
    nextChapter(): StudyChapterMeta | undefined {
      const chapters = data.chapters,
        currentId = currentChapter().id;
      for (let i in chapters)
        if (chapters[i].id === currentId) return chapters[parseInt(i) + 1];
      return undefined;
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
    redraw,
    trans: ctrl.trans,
    socketHandler: (t: string, d: any) => {
      const handler = socketHandlers[t];
      if (handler) {
        handler(d);
        return true;
      }
      return !!relay && relay.socketHandler(t, d);
    },
  };
};
