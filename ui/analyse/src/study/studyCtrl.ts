import { Config as CgConfig } from 'chessground/config';
import { DrawShape } from 'chessground/draw';
import { prop, defined } from 'common';
import throttle, { throttlePromiseDelay } from 'common/throttle';
import debounce from 'common/debounce';
import AnalyseCtrl from '../ctrl';
import { StudyMemberCtrl } from './studyMembers';
import StudyPractice from './practice/studyPracticeCtrl';
import { StudyPracticeData, StudyPracticeCtrl } from './practice/interfaces';
import { CommentForm } from './commentForm';
import { GlyphForm } from './studyGlyph';
import { StudyForm } from './studyForm';
import TopicsCtrl from './topics';
import { NotifCtrl } from './notif';
import { StudyShare } from './studyShare';
import { TagsForm } from './studyTags';
import ServerEval from './serverEval';
import * as xhr from './studyXhr';
import { path as treePath } from 'tree';
import {
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
  StudyTour,
  ChapterId,
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
import { GamebookOverride } from './gamebook/interfaces';

interface Handlers {
  path(d: WithWhoAndPos): void;
  addNode(
    d: WithWhoAndPos & { d: string; n: Tree.Node; o: Opening; s: boolean; relay?: StudyChapterRelay },
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
export default class StudyCtrl {
  relayRecProp = storedBooleanProp('analyse.relay.rec', true);
  nonRelayRecMapProp = storedMap<boolean>('study.rec', 100, () => true);
  chapterFlipMapProp = storedMap<boolean>('chapter.flip', 400, () => false);
  vm: StudyVm;
  notif: NotifCtrl;
  members: StudyMemberCtrl;
  chapters: StudyChaptersCtrl;
  relay?: RelayCtrl;
  multiBoard: MultiBoardCtrl;
  form: StudyForm;
  commentForm: CommentForm;
  glyphForm: GlyphForm;
  topics: TopicsCtrl;
  serverEval: ServerEval;
  share: StudyShare;
  tags: TagsForm;
  studyDesc: DescriptionCtrl;
  chapterDesc: DescriptionCtrl;
  search: SearchCtrl;
  practice?: StudyPracticeCtrl;
  gamebookPlay?: GamebookPlayCtrl;

  constructor(
    readonly data: StudyData,
    readonly ctrl: AnalyseCtrl,
    tagTypes: TagTypes,
    practiceData?: StudyPracticeData,
    private readonly relayData?: RelayData,
  ) {
    this.notif = new NotifCtrl(ctrl.redraw);
    const isManualChapter = data.chapter.id !== data.position.chapterId;
    const sticked = data.features.sticky && !ctrl.initialPath && !isManualChapter && !practiceData;
    this.vm = {
      loading: false,
      tab: prop<Tab>(relayData || data.chapters.length > 1 ? 'chapters' : 'members'),
      toolTab: prop<ToolTab>('tags'),
      chapterId: sticked ? data.position.chapterId : data.chapter.id,
      // path is at ctrl.path
      mode: {
        sticky: sticked,
        write: relayData ? this.relayRecProp() : this.nonRelayRecMapProp(data.id),
      },
      // how many events missed because sync=off
      behind: 0,
      // how stale is the study
      updatedAt: Date.now() - data.secondsSinceUpdate * 1000,
      gamebookOverride: undefined,
    };

    this.members = new StudyMemberCtrl({
      initDict: data.members,
      myId: practiceData ? undefined : ctrl.opts.userId,
      ownerId: data.ownerId,
      send: this.send,
      tab: this.vm.tab,
      startTour: this.startTour,
      notif: this.notif,
      onBecomingContributor: () => (this.vm.mode.write = !relayData || this.relayRecProp()),
      admin: data.admin,
      redraw: ctrl.redraw,
      trans: ctrl.trans,
    });
    this.chapters = new StudyChaptersCtrl(
      data.chapters,
      this.send,
      () => this.setTab('chapters'),
      chapterId => xhr.chapterConfig(data.id, chapterId),
      this.ctrl,
    );
    this.relay =
      relayData &&
      new RelayCtrl(
        this.data.id,
        relayData,
        this.send,
        this.redrawAndUpdateAddressBar,
        this.members,
        this.data.chapter,
        this.looksNew(),
        (id: ChapterId) => this.setChapter(id),
      );
    this.multiBoard = new MultiBoardCtrl(
      this.data.id,
      this.redraw,
      this.ctrl.trans,
      this.ctrl.socket.send,
      () => this.data.chapter.setup.variant.key,
    );
    this.form = new StudyForm(
      (d, isNew) => {
        this.send('editStudy', d);
        if (
          isNew &&
          data.chapter.setup.variant.key === 'standard' &&
          ctrl.mainline.length === 1 &&
          !data.chapter.setup.fromFen &&
          !this.relay
        )
          this.chapters.newForm.openInitial();
      },
      () => data,
      ctrl.trans,
      this.redraw,
      this.relay,
    );
    this.commentForm = new CommentForm(ctrl);
    this.glyphForm = new GlyphForm(ctrl);
    this.tags = new TagsForm(this, tagTypes);
    this.studyDesc = new DescriptionCtrl(
      data.description,
      debounce(t => {
        data.description = t;
        this.send('descStudy', t);
      }, 500),
      this.redraw,
    );
    this.chapterDesc = new DescriptionCtrl(
      data.chapter.description,
      debounce(t => {
        data.chapter.description = t;
        this.send('descChapter', { id: this.vm.chapterId, desc: t });
      }, 500),
      this.redraw,
    );

    this.serverEval = new ServerEval(ctrl, () => this.vm.chapterId);

    this.search = new SearchCtrl(
      this.relay?.fullRoundName() || data.name,
      this.chapters.list,
      this.setChapter,
      this.redraw,
    );

    this.topics = new TopicsCtrl(
      topics => this.send('setTopics', topics),
      () => data.topics || [],
      ctrl.trans,
      this.redraw,
    );

    this.share = new StudyShare(
      data,
      this.currentChapter,
      this.currentNode,
      this.onMainline,
      this.bottomColor,
      this.relay,
      this.redraw,
      ctrl.trans,
    );

    this.practice = practiceData && new StudyPractice(ctrl, data, practiceData);

    if (this.vm.mode.sticky && !this.isGamebookPlay()) this.ctrl.userJump(this.data.position.path);
    else if (this.data.chapter.relay && !defined(this.ctrl.requestInitialPly))
      this.ctrl.userJump(this.data.chapter.relay.path);

    this.configureAnalysis();

    this.ctrl.flipped = this.chapterFlipMapProp(this.data.chapter.id);
    if (this.members.canContribute()) this.form.openIfNew();

    this.instanciateGamebookPlay();
  }

  send = this.ctrl.socket.send;
  redraw = this.ctrl.redraw;

  startTour = async () => {
    const [tour] = await Promise.all([
      lichess.asset.loadEsm<StudyTour>('study.tour'),
      lichess.asset.loadCssPath('shepherd'),
    ]);

    tour.study(this.ctrl);
  };

  setTab = (tab: Tab) => {
    this.relay?.tourShow(false);
    this.vm.tab(tab);
    this.redraw();
  };

  currentChapter = (): StudyChapterMeta => this.chapters.get(this.vm.chapterId)!;

  isChapterOwner = (): boolean => this.ctrl.opts.userId === this.data.chapter.ownerId;

  isWriting = (): boolean => this.vm.mode.write && !this.isGamebookPlay();

  makeChange = (...args: StudySocketSendParams): boolean => {
    if (this.isWriting()) {
      this.send(...args);
      return true;
    }
    return (this.vm.mode.sticky = false);
  };

  addChapterId = <T>(req: T): T & { ch: string } => ({
    ...req,
    ch: this.vm.chapterId,
  });

  isGamebookPlay = () =>
    this.data.chapter.gamebook &&
    this.vm.gamebookOverride !== 'analyse' &&
    (this.vm.gamebookOverride === 'play' || !this.members.canContribute());

  configureAnalysis = () => {
    const canContribute = this.members.canContribute();
    // unwrite if member lost privileges
    this.vm.mode.write = this.vm.mode.write && canContribute;
    lichess.pubsub.emit('chat.writeable', this.data.features.chat);
    // official broadcasts cannot have local mods
    lichess.pubsub.emit('chat.permissions', { local: canContribute && !this.relayData?.tour.official });
    lichess.pubsub.emit('palantir.toggle', this.data.features.chat && !!this.members.myMember());
    const computer: boolean =
      !this.isGamebookPlay() && !!(this.data.chapter.features.computer || this.data.chapter.practice);
    if (!computer) this.ctrl.getCeval().enabled(false);
    this.ctrl.getCeval().allowed(computer);
    if (!this.data.chapter.features.explorer) this.ctrl.explorer.disable();
    this.ctrl.explorer.allowed(this.data.chapter.features.explorer);
  };

  configurePractice = () => {
    if (!this.data.chapter.practice && this.ctrl.practice) this.ctrl.togglePractice();
    if (this.data.chapter.practice) this.ctrl.restartPractice();
    this.practice?.onLoad();
  };

  onReload = (d: ReloadData) => {
    const s = d.study!;
    const prevPath = this.ctrl.path;
    const sameChapter = this.data.chapter.id === s.chapter.id;
    this.vm.mode.sticky =
      (this.vm.mode.sticky && s.features.sticky) || (!this.data.features.sticky && s.features.sticky);
    if (this.vm.mode.sticky) this.vm.behind = 0;
    this.data.position = s.position;
    this.data.name = s.name;
    this.data.visibility = s.visibility;
    this.data.features = s.features;
    this.data.settings = s.settings;
    this.data.chapter = s.chapter;
    this.data.likes = s.likes;
    this.data.liked = s.liked;
    this.data.description = s.description;
    this.chapterDesc.set(this.data.chapter.description);
    this.studyDesc.set(this.data.description);
    document.title = this.data.name;
    this.members.dict(s.members);
    this.chapters.list(s.chapters);
    this.ctrl.flipped = this.chapterFlipMapProp(this.data.chapter.id);

    const merge = !this.vm.mode.write && sameChapter;
    this.ctrl.reloadData(d.analysis, merge);
    this.vm.gamebookOverride = undefined;
    this.configureAnalysis();
    this.vm.loading = false;

    this.instanciateGamebookPlay();
    this.relay?.applyChapterRelay(this.data.chapter, s.chapter.relay);

    let nextPath: Tree.Path;

    if (this.vm.mode.sticky) {
      this.vm.chapterId = this.data.position.chapterId;
      nextPath =
        (this.vm.justSetChapterId === this.vm.chapterId && this.chapters.localPaths[this.vm.chapterId]) ||
        this.data.position.path;
    } else {
      nextPath = sameChapter
        ? prevPath
        : this.data.chapter.relay
        ? this.data.chapter.relay!.path
        : this.chapters.localPaths[this.vm.chapterId] || treePath.root;
    }

    // path could be gone (because of subtree deletion), go as far as possible
    this.ctrl.userJump(this.ctrl.tree.longestValidPath(nextPath));

    this.vm.justSetChapterId = undefined;

    this.configurePractice();
    this.serverEval.reset();
    this.commentForm.onSetPath(this.data.chapter.id, this.ctrl.path, this.ctrl.node);
    this.redraw();
    this.ctrl.startCeval();
    this.updateAddressBar();
  };

  xhrReload = throttlePromiseDelay(
    () => 700,
    () => {
      this.vm.loading = true;
      return xhr
        .reload(
          this.practice ? 'practice/load' : 'study',
          this.data.id,
          this.vm.mode.sticky ? undefined : this.vm.chapterId,
        )
        .then(this.onReload, lichess.reload);
    },
  );

  onSetPath = throttle(300, (path: Tree.Path) => {
    if (this.vm.mode.sticky && path !== this.data.position.path)
      this.makeChange('setPath', this.addChapterId({ path }));
  });

  currentNode = () => this.ctrl.node;
  onMainline = () => this.ctrl.tree.pathIsMainline(this.ctrl.path);
  bottomColor = () =>
    this.ctrl.flipped ? opposite(this.data.chapter.setup.orientation) : this.data.chapter.setup.orientation;

  instanciateGamebookPlay = () => {
    if (!this.isGamebookPlay()) return (this.gamebookPlay = undefined);
    if (this.gamebookPlay?.chapterId === this.vm.chapterId) return;
    this.gamebookPlay = new GamebookPlayCtrl(this.ctrl, this.vm.chapterId, this.ctrl.trans, this.redraw);
    this.vm.mode.sticky = false;
    return undefined;
  };

  mutateCgConfig = (config: Required<Pick<CgConfig, 'drawable'>>) => {
    config.drawable.onChange = (shapes: Tree.Shape[]) => {
      if (this.vm.mode.write) {
        this.ctrl.tree.setShapes(shapes, this.ctrl.path);
        this.makeChange(
          'shapes',
          this.addChapterId({
            path: this.ctrl.path,
            shapes,
          }),
        );
      }
      this.gamebookPlay?.onShapeChange(shapes);
    };
  };

  wrongChapter = (serverData: WithPosition & { s?: boolean }): boolean => {
    if (serverData.p.chapterId !== this.vm.chapterId) {
      // sticky should really be on the same chapter
      if (this.vm.mode.sticky && serverData.s) this.xhrReload();
      return true;
    }
    return false;
  };

  setMemberActive = (who?: { u: string }) => {
    who && this.members.setActive(who.u);
    this.vm.updatedAt = Date.now();
  };

  withPosition = <T>(obj: T): T & { ch: string; path: string } => ({
    ...obj,
    ch: this.vm.chapterId,
    path: this.ctrl.path,
  });

  likeToggler = debounce(() => this.send('like', { liked: this.data.liked }), 1000);

  setChapter = (id: string, force?: boolean) => {
    const alreadySet = id === this.vm.chapterId && !force;
    if (this.relay?.tourShow()) {
      this.relay.tourShow(false);
      if (alreadySet) this.redraw();
    }
    if (alreadySet) return;
    if (!this.vm.mode.sticky || !this.makeChange('setChapter', id)) {
      this.vm.mode.sticky = false;
      if (!this.vm.behind) this.vm.behind = 1;
      this.vm.chapterId = id;
      this.xhrReload();
    }
    this.vm.loading = true;
    this.vm.nextChapterId = id;
    this.vm.justSetChapterId = id;
    this.redraw();
  };

  private deltaChapter = (delta: number): StudyChapterMeta | undefined => {
    const chs = this.chapters.list();
    const i = chs.findIndex(ch => ch.id === this.vm.chapterId);
    return i < 0 ? undefined : chs[i + delta];
  };
  prevChapter = () => this.deltaChapter(-1);
  nextChapter = () => this.deltaChapter(+1);
  hasNextChapter = () => {
    const chs = this.chapters.list();
    return chs[chs.length - 1].id != this.vm.chapterId;
  };

  isUpdatedRecently = () => Date.now() - this.vm.updatedAt < 300 * 1000;
  toggleLike = () => {
    this.data.liked = !this.data.liked;
    this.redraw();
    this.likeToggler();
  };
  position = () => this.data.position;
  canJumpTo = (path: Tree.Path) =>
    this.gamebookPlay
      ? this.gamebookPlay.canJumpTo(path)
      : this.data.chapter.conceal === undefined ||
        this.isChapterOwner() ||
        treePath.contains(this.ctrl.path, path) || // can always go back
        this.ctrl.tree.lastMainlineNode(path).ply <= this.data.chapter.conceal!;
  onJump = () => {
    if (this.gamebookPlay) this.gamebookPlay.onJump();
    else this.chapters.localPaths[this.vm.chapterId] = this.ctrl.path; // don't remember position on gamebook
    this.practice?.onJump();
  };
  onFlip = () => this.chapterFlipMapProp(this.data.chapter.id, this.ctrl.flipped);

  setPath = (path: Tree.Path, node: Tree.Node) => {
    this.onSetPath(path);
    this.commentForm.onSetPath(this.vm.chapterId, path, node);
  };
  deleteNode = (path: Tree.Path) =>
    this.makeChange(
      'deleteNode',
      this.addChapterId({
        path,
        jumpTo: this.ctrl.path,
      }),
    );
  promote = (path: Tree.Path, toMainline: boolean) =>
    this.makeChange(
      'promote',
      this.addChapterId({
        toMainline,
        path,
      }),
    );
  forceVariation = (path: Tree.Path, force: boolean) =>
    this.makeChange(
      'forceVariation',
      this.addChapterId({
        force,
        path,
      }),
    );
  toggleSticky = () => {
    this.vm.mode.sticky = !this.vm.mode.sticky && this.data.features.sticky;
    this.xhrReload();
  };
  toggleWrite = () => {
    this.vm.mode.write = !this.vm.mode.write && this.members.canContribute();
    if (this.relayData) this.relayRecProp(this.vm.mode.write);
    else this.nonRelayRecMapProp(this.data.id, this.vm.mode.write);
    this.xhrReload();
  };
  goToPrevChapter = () => {
    const chapter = this.prevChapter();
    if (chapter) this.setChapter(chapter.id);
  };
  goToNextChapter = () => {
    const chapter = this.nextChapter();
    if (chapter) this.setChapter(chapter.id);
  };
  setGamebookOverride = (o: GamebookOverride) => {
    this.vm.gamebookOverride = o;
    this.instanciateGamebookPlay();
    this.configureAnalysis();
    this.ctrl.userJump(this.ctrl.path);
    if (!o) this.xhrReload();
  };
  explorerGame = (gameId: string, insert: boolean) =>
    this.makeChange('explorerGame', this.withPosition({ gameId, insert }));
  onPremoveSet = () => this.gamebookPlay?.onPremoveSet();
  looksNew = () => this.chapters.looksNew() && !this.currentChapter().ongoing;
  updateAddressBar = () => {
    const current = location.href;
    const studyIdOffset = current.indexOf(`/${this.data.id}`);
    if (studyIdOffset === -1) return;
    const studyUrl = current.slice(0, studyIdOffset + 9);
    const chapterUrl = `${studyUrl}/${this.vm.chapterId}`;
    if (this.relay) this.relay.updateAddressBar(studyUrl, chapterUrl);
    else if (chapterUrl !== current) history.replaceState({}, '', chapterUrl);
  };
  redrawAndUpdateAddressBar = () => {
    this.redraw();
    this.updateAddressBar();
  };
  trans = this.ctrl.trans;
  socketHandler = (t: string, d: any) => {
    const handler = (this.socketHandlers as any as SocketHandlers)[t];
    if (handler) {
      handler(d);
      return true;
    }
    return !!this.relay && this.relay.socketHandler(t, d);
  };

  socketHandlers: Handlers = {
    path: d => {
      const position = d.p,
        who = d.w;
      this.setMemberActive(who);
      if (!this.vm.mode.sticky) {
        this.vm.behind++;
        return this.redraw();
      }
      if (position.chapterId !== this.data.position.chapterId || !this.ctrl.tree.pathExists(position.path)) {
        return this.xhrReload();
      }
      this.data.position.path = position.path;
      if (who && who.s === lichess.sri) return;
      this.ctrl.userJump(position.path);
      this.redraw();
    },
    addNode: d => {
      const position = d.p,
        node = d.n,
        who = d.w,
        sticky = d.s;
      this.setMemberActive(who);
      if (this.vm.toolTab() == 'multiBoard' || this.relay?.tourShow()) this.multiBoard.addNode(d.p, d.n);
      if (sticky && !this.vm.mode.sticky) this.vm.behind++;
      if (this.wrongChapter(d)) {
        if (sticky && !this.vm.mode.sticky) this.redraw();
        return;
      }
      if (sticky && who && who.s === lichess.sri) {
        this.data.position.path = position.path + node.id;
        return;
      }
      this.relay?.applyChapterRelay(this.data.chapter, d.relay);
      const newPath = this.ctrl.tree.addNode(node, position.path);
      if (!newPath) return this.xhrReload();
      this.ctrl.tree.addDests(d.d, newPath);
      if (sticky) this.data.position.path = newPath;
      if (
        (sticky && this.vm.mode.sticky) ||
        (position.path === this.ctrl.path && position.path === treePath.fromNodeList(this.ctrl.mainline))
      )
        this.ctrl.jump(newPath);
      this.redraw();
    },
    deleteNode: d => {
      const position = d.p,
        who = d.w;
      this.setMemberActive(who);
      if (this.wrongChapter(d)) return;
      // deleter already has it done
      if (who && who.s === lichess.sri) return;
      if (!this.ctrl.tree.pathExists(d.p.path)) return this.xhrReload();
      this.ctrl.tree.deleteNodeAt(position.path);
      if (this.vm.mode.sticky) this.ctrl.jump(this.ctrl.path);
      this.redraw();
    },
    promote: d => {
      const position = d.p,
        who = d.w;
      this.setMemberActive(who);
      if (this.wrongChapter(d)) return;
      if (who && who.s === lichess.sri) return;
      if (!this.ctrl.tree.pathExists(d.p.path)) return this.xhrReload();
      this.ctrl.tree.promoteAt(position.path, d.toMainline);
      if (this.vm.mode.sticky) this.ctrl.jump(this.ctrl.path);
      this.ctrl.treeVersion++;
      this.redraw();
    },
    reload: this.xhrReload,
    changeChapter: d => {
      this.setMemberActive(d.w);
      if (!this.vm.mode.sticky) this.vm.behind++;
      this.data.position = d.p;
      if (this.vm.mode.sticky) this.xhrReload();
      else this.redraw();
    },
    updateChapter: d => {
      this.setMemberActive(d.w);
      this.xhrReload();
    },
    descChapter: d => {
      this.setMemberActive(d.w);
      if (d.w && d.w.s === lichess.sri) return;
      if (this.data.chapter.id === d.chapterId) {
        this.data.chapter.description = d.desc;
        this.chapterDesc.set(d.desc);
      }
      this.redraw();
    },
    descStudy: d => {
      this.setMemberActive(d.w);
      if (d.w && d.w.s === lichess.sri) return;
      this.data.description = d.desc;
      this.studyDesc.set(d.desc);
      this.redraw();
    },
    setTopics: d => {
      this.setMemberActive(d.w);
      this.data.topics = d.topics;
      this.redraw();
    },
    addChapter: d => {
      this.setMemberActive(d.w);
      if (d.s && !this.vm.mode.sticky) this.vm.behind++;
      if (d.s) this.data.position = d.p;
      else if (d.w && d.w.s === lichess.sri) {
        this.vm.mode.write = this.relayData ? this.relayRecProp() : this.nonRelayRecMapProp(this.data.id);
        this.vm.chapterId = d.p.chapterId;
      }
      this.xhrReload();
    },
    members: d => {
      this.members.update(d);
      this.configureAnalysis();
      this.redraw();
    },
    chapters: d => {
      this.chapters.list(d);
      if (this.vm.toolTab() == 'multiBoard' || this.relay?.tourShow()) this.multiBoard.addResult(d);
      if (!this.currentChapter()) {
        this.vm.chapterId = d[0].id;
        if (!this.vm.mode.sticky) this.xhrReload();
      }
      this.redraw();
    },
    shapes: d => {
      const position = d.p,
        who = d.w;
      this.setMemberActive(who);
      if (d.p.chapterId !== this.vm.chapterId) return;
      if (who && who.s === lichess.sri) return this.redraw(); // update shape indicator in column move view
      this.ctrl.tree.setShapes(d.s, this.ctrl.path);
      if (this.ctrl.path === position.path) this.ctrl.withCg(cg => cg.setShapes(d.s));
      this.redraw();
    },
    validationError: d => {
      alert(d.error);
    },
    setComment: d => {
      const position = d.p,
        who = d.w;
      this.setMemberActive(who);
      if (this.wrongChapter(d)) return;
      this.ctrl.tree.setCommentAt(d.c, position.path);
      this.redraw();
    },
    setTags: d => {
      this.setMemberActive(d.w);
      if (d.chapterId !== this.vm.chapterId) return;
      this.data.chapter.tags = d.tags;
      this.redraw();
    },
    deleteComment: d => {
      const position = d.p,
        who = d.w;
      this.setMemberActive(who);
      if (this.wrongChapter(d)) return;
      this.ctrl.tree.deleteCommentAt(d.id, position.path);
      this.redraw();
    },
    glyphs: d => {
      const position = d.p,
        who = d.w;
      this.setMemberActive(who);
      if (this.wrongChapter(d)) return;
      this.ctrl.tree.setGlyphsAt(d.g, position.path);
      if (this.ctrl.path === position.path) this.ctrl.setAutoShapes();
      this.redraw();
    },
    clock: d => {
      const position = d.p,
        who = d.w;
      this.setMemberActive(who);
      if (this.wrongChapter(d)) return;
      this.ctrl.tree.setClockAt(d.c, position.path);
      this.redraw();
    },
    forceVariation: d => {
      const position = d.p,
        who = d.w;
      this.setMemberActive(who);
      if (this.wrongChapter(d)) return;
      this.ctrl.tree.forceVariationAt(position.path, d.force);
      this.redraw();
    },
    conceal: d => {
      if (this.wrongChapter(d)) return;
      this.data.chapter.conceal = d.ply;
      this.redraw();
    },
    liking: d => {
      this.data.likes = d.l.likes;
      if (d.w && d.w.s === lichess.sri) this.data.liked = d.l.me;
      this.redraw();
    },
    error(msg: string) {
      alert(msg);
    },
  };
}
