import type { DrawShape } from '@lichess-org/chessground/draw';
import { prop, defined } from 'lib';
import { debounce, throttle, throttlePromiseDelay } from 'lib/async';
import type AnalyseCtrl from '../ctrl';
import { StudyMemberCtrl } from './studyMembers';
import StudyPracticeCtrl from './practice/studyPracticeCtrl';
import type { StudyPracticeData } from './practice/interfaces';
import { CommentForm } from './commentForm';
import { GlyphForm } from './studyGlyph';
import { StudyForm } from './studyForm';
import TopicsCtrl from './topics';
import { NotifCtrl } from './notif';
import { StudyShare } from './studyShare';
import { TagsForm } from './studyTags';
import ServerEval from './serverEval';
import * as xhr from './studyXhr';
import { path as treePath, ops as treeOps } from 'lib/tree/tree';
import type {
  StudyVm,
  Tab,
  ToolTab,
  TagTypes,
  ReloadData,
  WithWhoAndPos,
  WithWhoAndChap,
  WithWho,
  WithPosition,
  TagArray,
  StudyTour,
  ChapterId,
  ServerNodeMsg,
  ServerClockMsg,
  ChapterPreview,
  StudyDataFromServer,
  StudyData,
  ChapterPreviewFromServer,
  ChapterSelect,
} from './interfaces';
import GamebookPlayCtrl from './gamebook/gamebookPlayCtrl';
import { DescriptionCtrl } from './description';
import RelayCtrl from './relay/relayCtrl';
import type { RelayData } from './relay/interfaces';
import { MultiBoardCtrl } from './multiBoard';
import type { StudySocketSendParams } from '../socket';
import { storedMap } from 'lib/storage';
import { opposite } from 'chessops/util';
import StudyChaptersCtrl, { isFinished } from './studyChapters';
import { SearchCtrl } from './studySearch';
import type { GamebookOverride } from './gamebook/interfaces';
import type { EvalHitMulti, EvalHitMultiArray } from '../interfaces';
import { MultiCloudEval } from './multiCloudEval';
import { pubsub } from 'lib/pubsub';
import { alert } from 'lib/view';
import { displayColumns } from 'lib/device';
import type { Glyph, Shape, TreeComment, TreeNode, TreePath } from 'lib/tree/types';
import { completeNode } from 'lib/tree/node';

interface Handlers {
  path(d: WithWhoAndPos): void;
  addNode(d: ServerNodeMsg): void;
  deleteNode(d: WithWhoAndPos): void;
  promote(d: WithWhoAndPos & { toMainline: boolean }): void;
  liking(d: WithWho & { l: { likes: number; me: boolean } }): void;
  shapes(d: WithWhoAndPos & { s: DrawShape[] }): void;
  members(d: { [id: string]: { user: { name: string; id: string }; role: 'r' | 'w' } }): void;
  setComment(d: WithWhoAndPos & { c: TreeComment }): void;
  deleteComment(d: WithWhoAndPos & { id: string }): void;
  glyphs(d: WithWhoAndPos & { g: Glyph[] }): void;
  clock(d: ServerClockMsg): void;
  forceVariation(d: WithWhoAndPos & { force: boolean }): void;
  chapters(d: ChapterPreviewFromServer[]): void;
  reload(d: { reason?: 'overweight' }): void;
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
  evalHitMulti(e: EvalHitMulti | EvalHitMultiArray): void;
}

// data.position.path represents the server state
// ctrl.path is the client state
export default class StudyCtrl {
  relayRecProp = prop(false);
  nonRelayRecMapProp = storedMap<boolean>('study.rec', 100, () => true);
  chapterFlipMapProp = storedMap<boolean>('chapter.flip', 400, () => false);
  arrowHistory: Shape[][] = [];
  data: StudyData;
  vm: StudyVm;
  notif: NotifCtrl;
  members: StudyMemberCtrl;
  chapters: StudyChaptersCtrl;
  relay?: RelayCtrl;
  multiCloudEval?: MultiCloudEval;
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
    data: StudyDataFromServer,
    readonly ctrl: AnalyseCtrl,
    tagTypes: TagTypes,
    practiceData?: StudyPracticeData,
    relayData?: RelayData,
  ) {
    this.data = data;
    this.notif = new NotifCtrl(ctrl.redraw);
    const isManualChapter = data.chapter.id !== data.position.chapterId;
    const sticked =
      data.features.sticky &&
      !ctrl.initialPath &&
      ctrl.requestInitialPly === undefined &&
      !isManualChapter &&
      !practiceData;
    this.vm = {
      loading: false,
      tab: prop<Tab>(!relayData && data.chapters?.[1] ? 'chapters' : 'members'),
      toolTab: prop<ToolTab>(relayData ? 'multiBoard' : 'tags'),
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
    });
    this.chapters = new StudyChaptersCtrl(
      data.chapters!,
      this.send,
      defined(relayData),
      () => this.setTab('chapters'),
      chapterId => xhr.chapterConfig(data.id, chapterId),
      () => this.data.federations,
      this.ctrl,
    );
    this.multiCloudEval = this.isCevalAllowed()
      ? new MultiCloudEval(this.redraw, this.chapters.list, this.send)
      : undefined;
    if (relayData) this.relay = new RelayCtrl(this, relayData);
    this.multiBoard = new MultiBoardCtrl(
      this.chapters.list,
      defined(this.relay),
      this.multiCloudEval,
      this.redraw,
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
    );

    this.practice = practiceData && new StudyPracticeCtrl(ctrl, data, practiceData);

    if (this.vm.mode.sticky && !this.isGamebookPlay()) this.ctrl.userJump(this.data.position.path);
    else if (
      this.data.chapter.relayPath &&
      !defined(this.ctrl.requestInitialPly) &&
      !(this.relay && !this.multiBoard.showResults())
    )
      this.ctrl.userJump(this.data.chapter.relayPath);

    this.configureAnalysis();

    this.ctrl.flipped = this.chapterFlipMapProp(this.data.chapter.id);
    if (this.members.canContribute()) this.form.openIfNew();

    this.instantiateGamebookPlay();

    window.addEventListener('popstate', () => window.location.reload());
  }

  send = this.ctrl.socket.send;
  redraw = this.ctrl.redraw;

  startTour = async () => {
    const [tour] = await Promise.all([
      site.asset.loadEsm<StudyTour>('analyse.study.tour'),
      site.asset.loadCssPath('bits.shepherd'),
    ]);

    tour.study(this.ctrl);
  };

  setTab = (tab: Tab) => {
    if (tab === 'chapters') this.chapters.scroller.request = 'instant';
    this.vm.tab(tab);
    this.redraw();
  };

  currentChapter = (): ChapterPreview => this.chapters.list.get(this.vm.chapterId)!;

  isChapterOwner = (): boolean => this.ctrl.opts.userId === this.data.chapter.ownerId;

  isWriting = (): boolean => this.vm.mode.write && !this.isGamebookPlay();

  private updateShapes = (shapes: Shape[]) => {
    this.ctrl.tree.setShapes(shapes, this.ctrl.path);
    this.makeChange(
      'shapes',
      this.addChapterId({
        path: this.ctrl.path,
        shapes: shapes,
      }),
    );
  };

  undoShapeChange = () => {
    if (!this.vm.mode.write) return;
    const last = this.arrowHistory.pop();
    if (!last) return;
    this.updateShapes(last);
    this.ctrl.withCg(cg => cg.setShapes(last.slice() as DrawShape[]));
  };

  makeChange = <K extends keyof StudySocketSendParams>(
    event: K,
    ...args: Parameters<StudySocketSendParams[K]>
  ): boolean => {
    if (this.isWriting()) {
      this.send(event, ...args);
      return true;
    }
    return (this.vm.mode.sticky = false);
  };

  addChapterId = <T>(req: T): T & { ch: ChapterId } => ({
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
    pubsub.emit('chat.writeable', this.data.features.chat);
    // official broadcasts cannot have local mods
    pubsub.emit('chat.permissions', { local: canContribute && !this.relay?.isOfficial() });
    pubsub.emit('voiceChat.toggle', this.data.features.chat && !!this.members.myMember());
    if (!this.data.chapter.features.explorer) this.ctrl.explorer.disable();
    this.ctrl.explorer.allowed(this.data.chapter.features.explorer);
  };

  isCevalAllowed = () =>
    !this.relay?.tourShow() &&
    !this.isGamebookPlay() &&
    !!(this.data.chapter.features.computer || this.data.chapter.practice);

  configurePractice = () => {
    if (!this.data.chapter.practice && this.ctrl.practice) this.ctrl.togglePractice();
    if (this.data.chapter.practice) this.ctrl.togglePractice(true);
    this.practice?.onLoad();
  };

  onReload = (d: ReloadData) => {
    const s = d.study;
    const prevPath = this.ctrl.path;
    const sameChapter = this.data.chapter.id === s.chapter.id;
    this.vm.mode.sticky =
      (this.vm.mode.sticky && s.features.sticky) || (!this.data.features.sticky && s.features.sticky);
    if (this.vm.mode.sticky) this.vm.behind = 0;
    this.data.position = s.position;
    this.data.name = s.name;
    this.data.flair = s.flair;
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
    if (s.chapters) this.chapters.loadFromServer(s.chapters);
    this.ctrl.flipped = this.chapterFlipMapProp(this.data.chapter.id);

    const merge = !this.vm.mode.write && sameChapter;
    this.ctrl.reloadData(d.analysis, merge);
    this.vm.gamebookOverride = undefined;
    this.configureAnalysis();
    this.vm.loading = false;

    this.instantiateGamebookPlay();

    let nextPath: TreePath;

    if (this.vm.mode.sticky) {
      this.vm.chapterId = this.data.position.chapterId;
      nextPath =
        (this.vm.justSetChapterId === this.vm.chapterId && this.chapters.localPaths[this.vm.chapterId]) ||
        this.data.position.path;
    } else {
      nextPath = sameChapter
        ? prevPath
        : this.relay && !this.multiBoard.showResults()
          ? treePath.root
          : this.data.chapter.relayPath || this.chapters.localPaths[this.vm.chapterId] || treePath.root;
    }

    // path could be gone (because of subtree deletion), go as far as possible
    this.ctrl.userJump(this.ctrl.tree.longestValidPath(nextPath));

    this.vm.justSetChapterId = undefined;

    this.configurePractice();
    this.serverEval.reset();
    this.commentForm.onSetPath(this.data.chapter.id, this.ctrl.path, this.ctrl.node);
    this.redraw();
    this.ctrl.startCeval();
    this.updateHistoryAndAddressBar();
  };

  xhrReload = throttlePromiseDelay(
    () => 400,
    (withChapters: boolean = false, callback: () => void = () => {}) => {
      this.vm.loading = true;
      return xhr
        .reload(
          this.practice ? 'practice/load' : 'study',
          this.data.id,
          this.vm.mode.sticky ? undefined : this.vm.chapterId,
          withChapters,
        )
        .then(this.onReload, site.reload)
        .then(callback);
    },
  );

  onSetPath = throttle(300, (path: TreePath) => {
    if (this.vm.mode.sticky && path !== this.data.position.path)
      this.makeChange('setPath', this.addChapterId({ path }));
  });

  currentNode = () => this.ctrl.node;
  onMainline = () => this.ctrl.tree.pathIsMainline(this.ctrl.path);
  bottomColor = () =>
    this.ctrl.flipped ? opposite(this.data.chapter.setup.orientation) : this.data.chapter.setup.orientation;

  instantiateGamebookPlay = () => {
    if (!this.isGamebookPlay()) return (this.gamebookPlay = undefined);
    // ensure all original nodes have a gamebook entry,
    // so we can differentiate original nodes from user-made ones
    treeOps.updateAll(this.ctrl.tree.root, n => {
      n.gamebook = n.gamebook || {};
      if (n.shapes) n.gamebook.shapes = n.shapes.slice(0);
    });
    if (this.gamebookPlay?.chapterId === this.vm.chapterId) return;
    this.gamebookPlay = new GamebookPlayCtrl(this.ctrl, this.vm.chapterId, this.redraw);
    this.vm.mode.sticky = false;
    return undefined;
  };

  mutateCgConfig = (config: Required<Pick<CgConfig, 'drawable'>>) => {
    config.drawable.onChange = (shapes: Shape[]) => {
      if (this.vm.mode.write) {
        this.arrowHistory.push(this.ctrl.node.shapes?.slice() ?? []);
        this.updateShapes(shapes);
      }
      this.gamebookPlay?.onShapeChange(shapes);
    };
  };

  wrongChapter = (serverData: WithPosition & { s?: boolean }): boolean => {
    // #TODO why vm.chapterId when we have data.chapter.id
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

  setChapter = async (idOrNumber: ChapterId | number, force?: boolean): Promise<boolean> => {
    const prev = this.chapters.list.get(idOrNumber);
    const id = prev?.id;
    if (!id) {
      console.warn(`Chapter ${idOrNumber} not found`);
      return false;
    }
    const componentCallbacks = (id: ChapterId) => {
      this.relay?.onChapterChange(id);
    };
    const alreadySet = id === this.vm.chapterId && !force;
    if (alreadySet) {
      componentCallbacks(this.data.chapter.id);
      this.redraw();
      return true;
    }
    this.chapters.scroller.request = 'smooth';
    this.vm.nextChapterId = id;
    this.vm.justSetChapterId = id;
    if (this.vm.mode.sticky && this.makeChange('setChapter', id)) {
      this.vm.loading = true;
      this.relay?.onChapterChange(id);
      this.redraw();
    } else {
      // not sticky, not sending the chapter change to the server
      // so we need to apply the change locally immediately
      // instead of awaiting the server chapter change event
      this.vm.mode.sticky = false;
      if (!this.vm.behind) this.vm.behind = 1;
      this.vm.chapterId = id;
      this.relay?.liveboardPlugin?.reset();
      await this.xhrReload(false, () => componentCallbacks(id));
    }
    if (displayColumns() > 2) window.scrollTo(0, 0);
    return true;
  };

  chapterSelect: ChapterSelect = {
    is: (idOrNumber: ChapterId | number) => defined(this.chapters.list.get(idOrNumber)),
    set: this.setChapter,
    get: () => this.data.chapter.id,
  };

  private deltaChapter = (delta: number): ChapterPreview | undefined => {
    const chs = this.chapters.list.all();
    const i = chs.findIndex(ch => ch.id === this.vm.chapterId);
    return i < 0 ? undefined : chs[i + delta];
  };
  prevChapter = () => this.deltaChapter(-1);
  nextChapter = () => this.deltaChapter(+1);
  hasNextChapter = () => {
    const chs = this.chapters.list.all();
    return chs[chs.length - 1].id !== this.vm.chapterId;
  };

  isUpdatedRecently = () => Date.now() - this.vm.updatedAt < 300 * 1000;
  toggleLike = () => {
    this.data.liked = !this.data.liked;
    this.redraw();
    this.likeToggler();
  };
  position = () => this.data.position;
  canJumpTo = (path: TreePath) =>
    this.gamebookPlay
      ? this.gamebookPlay.canJumpTo(path)
      : this.data.chapter.conceal === undefined ||
        this.isChapterOwner() ||
        treePath.contains(this.ctrl.path, path) || // can always go back
        this.ctrl.tree.lastMainlineNode(path).ply <= this.data.chapter.conceal;
  onJump = () => {
    if (this.gamebookPlay) this.gamebookPlay.onJump();
    else this.chapters.localPaths[this.vm.chapterId] = this.ctrl.path; // don't remember position on gamebook
    this.practice?.onJump();
  };
  onFlip = (flipped: boolean) => {
    if (this.chapters.newForm.isOpen()) return false;
    this.chapterFlipMapProp(this.data.chapter.id, flipped);
    return true;
  };

  isClockTicking = (path: TreePath) =>
    path !== '' && this.data.chapter.relayPath === path && !isFinished(this.data.chapter);

  isRelayAwayFromLive = (): boolean =>
    !!this.relay &&
    !isFinished(this.data.chapter) &&
    defined(this.data.chapter.relayPath) &&
    this.ctrl.path !== this.data.chapter.relayPath;

  isRelayAndInVariation = (): boolean =>
    this.isRelayAwayFromLive() && !treePath.contains(this.data.chapter.relayPath!, this.ctrl.path);

  setPath = (path: TreePath, node: TreeNode) => {
    this.arrowHistory = [];
    this.onSetPath(path);
    this.commentForm.onSetPath(this.vm.chapterId, path, node);
  };
  deleteNode = (path: TreePath) =>
    this.makeChange(
      'deleteNode',
      this.addChapterId({
        path,
        jumpTo: this.ctrl.path,
      }),
    );
  promote = (path: TreePath, toMainline: boolean) =>
    this.makeChange(
      'promote',
      this.addChapterId({
        toMainline,
        path,
      }),
    );
  forceVariation = (path: TreePath, force: boolean) =>
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
    if (this.relay) this.relayRecProp(this.vm.mode.write);
    else this.nonRelayRecMapProp(this.data.id, this.vm.mode.write);
    this.xhrReload();
  };
  goToPrevChapter = () => {
    const chapter = this.prevChapter();
    if (chapter) this.setChapter(chapter.id);
  };
  goToNextChapter = () => {
    this.practice?.onComplete();
    const chapter = this.nextChapter();
    if (chapter) this.setChapter(chapter.id);
  };
  setGamebookOverride = (o: GamebookOverride) => {
    this.vm.gamebookOverride = o;
    this.instantiateGamebookPlay();
    this.configureAnalysis();
    this.ctrl.userJump(this.ctrl.path);
    if (!o) this.xhrReload();
    else if (o === 'analyse') this.ctrl.startCeval();
  };
  explorerGame = (gameId: string, insert: boolean) =>
    this.makeChange('explorerGame', this.withPosition({ gameId, insert }));
  onPremoveSet = () => this.gamebookPlay?.onPremoveSet();
  baseUrl = () => {
    const current = location.href;
    const studyIdOffset = current.indexOf(`/${this.data.id}`);
    return studyIdOffset === -1 ? `/study/${this.data.id}` : current.slice(0, studyIdOffset + 9);
  };
  updateHistoryAndAddressBar = () => {
    if (this.ctrl.isEmbed) return;
    const studyUrl = this.baseUrl();
    const chapterUrl = `${studyUrl}/${this.vm.chapterId}`;
    if (this.relay) this.relay.updateAddressBar(studyUrl, chapterUrl);
    else if (chapterUrl !== location.href) history.replaceState({}, '', chapterUrl);
  };
  socketSendNodeData = () => {
    if (!this.isWriting()) return false;
    const data: { ch: string; sticky?: false } = { ch: this.vm.chapterId };
    if (!this.vm.mode.sticky) data.sticky = false;
    return data;
  };
  socketHandler = (t: string, d: any) => {
    const handler = (this.socketHandlers as any as SocketHandlers)[t];
    if (handler) {
      handler(d);
      return true;
    }
    return !!this.relay?.socketHandler(t, d);
  };
  embeddablePath = (path: string) => {
    if (!this.ctrl.isEmbed) return path;
    const p = `${path.startsWith('/embed/') ? '' : '/embed'}${path}`;
    if (!location.search) return p;
    const s = p.split('#');
    return `${s[0]}${location.search}${s[1] ? `#${s[1]}` : ''}`;
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
      if (position.chapterId !== this.data.position.chapterId || !this.ctrl.tree.pathExists(position.path))
        return this.xhrReload();
      this.data.position.path = position.path;
      if (who && who.s === site.sri) return;
      this.ctrl.userJump(position.path);
      this.redraw();
    },
    addNode: d => {
      const position = d.p,
        node = completeNode(this.ctrl.variantKey)(d.n),
        who = d.w,
        sticky = d.s;
      if (d.relayPath === '!') d.relayPath = d.p.path + d.n.id;
      this.setMemberActive(who);
      this.chapters.addNode(d);
      this.multiCloudEval?.addNode(d);
      this.relay?.onAddNode();
      if (sticky && !this.vm.mode.sticky) this.vm.behind++;
      if (this.wrongChapter(d)) {
        if (sticky && !this.vm.mode.sticky) this.redraw();
        return;
      }
      if (sticky && who?.s === site.sri) {
        this.data.position.path = position.path + node.id;
        return;
      }
      this.data.chapter.relayPath = d.relayPath;
      const newPath = this.ctrl.tree.addNode(node, position.path);
      if (!newPath) return this.xhrReload();
      if (d.relayPath && !this.ctrl.tree.pathIsMainline(d.relayPath))
        this.ctrl.tree.promoteAt(d.relayPath, true);
      if (sticky) this.data.position.path = newPath;
      if (
        (sticky && this.vm.mode.sticky) ||
        (position.path === this.ctrl.path &&
          (position.path === treePath.fromNodeList(this.ctrl.mainline) || d.relayPath === newPath))
      )
        this.ctrl.jump(newPath);
      return this.redraw();
    },
    deleteNode: d => {
      const position = d.p,
        who = d.w;
      this.setMemberActive(who);
      if (this.wrongChapter(d)) return;
      // deleter already has it done
      if (who && who.s === site.sri) return;
      if (!this.ctrl.tree.pathExists(d.p.path)) return this.xhrReload();
      this.ctrl.tree.deleteNodeAt(position.path);
      if (this.vm.mode.sticky) this.ctrl.jump(this.ctrl.path);
      return this.redraw();
    },
    promote: d => {
      const position = d.p,
        who = d.w;
      this.setMemberActive(who);
      if (this.wrongChapter(d)) return;
      if (who && who.s === site.sri) return;
      if (!this.ctrl.tree.pathExists(d.p.path)) return this.xhrReload();
      this.ctrl.tree.promoteAt(position.path, d.toMainline);
      if (this.vm.mode.sticky) this.ctrl.jump(this.ctrl.path);
      else if (this.relay) this.ctrl.jump(d.p.path);
      return this.redraw();
    },
    reload: d => {
      if (d?.reason === 'overweight') alert('This chapter is too big to add moves.');
      this.xhrReload();
    },
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
      if (d.w && d.w.s === site.sri) return;
      if (this.data.chapter.id === d.chapterId) {
        this.data.chapter.description = d.desc;
        this.chapterDesc.set(d.desc);
      }
      this.redraw();
    },
    descStudy: d => {
      this.setMemberActive(d.w);
      if (d.w && d.w.s === site.sri) return;
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
      if (d.w?.s === site.sri) {
        this.vm.mode.write = this.relay ? this.relayRecProp() : this.nonRelayRecMapProp(this.data.id);
        this.vm.chapterId = d.p.chapterId;
        this.vm.nextChapterId = d.p.chapterId;
        this.chapters.scroller.request = 'instant';
      }
      this.xhrReload(true);
    },
    members: d => {
      this.members.update(d);
      this.configureAnalysis();
      this.redraw();
    },
    chapters: d => {
      const prevChapters = this.chapters.list.all();
      this.chapters.loadFromServer(d);
      if (!this.currentChapter()) {
        const prevIndex = prevChapters.findIndex(ch => ch.id === this.vm.chapterId);
        const newIndex = prevIndex === -1 ? 0 : prevIndex >= d.length ? d.length - 1 : prevIndex;
        this.vm.chapterId = d[newIndex].id;
        if (!this.vm.mode.sticky) this.xhrReload();
      }
      this.redraw();
    },
    shapes: d => {
      const position = d.p,
        who = d.w;
      this.setMemberActive(who);
      if (d.p.chapterId !== this.vm.chapterId) return;
      if (who && who.s === site.sri) return this.redraw(); // update shape indicator in column move view
      if (this.ctrl.path === position.path) {
        this.arrowHistory.push(this.ctrl.node.shapes?.slice() ?? []);
        this.ctrl.withCg(cg => cg.setShapes(d.s));
      }
      this.ctrl.tree.setShapes(d.s, position.path);
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
      this.chapters.setTags(d.chapterId, d.tags);
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
      if (d.relayClocks) this.relay?.setClockToChapterPreview(d, d.relayClocks);
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
      if (d.w && d.w.s === site.sri) this.data.liked = d.l.me;
      this.redraw();
    },
    error(msg: string) {
      alert(msg);
    },
    evalHitMulti: (e: EvalHitMulti | EvalHitMultiArray) => {
      ('multi' in e ? e.multi : [e]).forEach(ev => {
        this.multiBoard.multiCloudEval?.onCloudEval(ev);
      });
    },
  };
}
