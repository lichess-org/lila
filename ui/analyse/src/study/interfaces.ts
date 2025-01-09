import { Prop } from 'common/common';
import { Status } from 'game';
import { AnalyseData, Redraw } from '../interfaces';
import { CommentForm } from './comment-form';
import { DescriptionCtrl } from './description';
import GamebookPlayCtrl from './gamebook/gamebook-play-ctrl';
import { GamebookOverride } from './gamebook/interfaces';
import { MultiBoardCtrl } from './multi-board';
import { NotifCtrl } from './notif';
import { StudyPracticeCtrl } from './practice/interfaces';
import { ServerEval } from './server-eval';
import { StudyChaptersCtrl } from './study-chapters';
import { GlyphCtrl } from './study-glyph';
import { TopicsCtrl } from './topics';
import { StoredProp, StoredSet } from 'common/storage';

export interface StudyCtrl {
  data: StudyData;
  currentChapter(): StudyChapterMeta;
  socketHandler(t: string, d: any): boolean;
  vm: StudyVm;
  multiBoard: MultiBoardCtrl;
  form: any;
  members: any;
  chapters: StudyChaptersCtrl;
  notif: NotifCtrl;
  commentForm: CommentForm;
  glyphForm: GlyphCtrl;
  topics: TopicsCtrl;
  serverEval: ServerEval;
  share: any;
  tags: any;
  studyDesc: DescriptionCtrl;
  chapterDesc: DescriptionCtrl;
  toggleLike(): void;
  rematch(yes: boolean): void;
  position(): Position;
  isChapterOwner(): boolean;
  canJumpTo(path: Tree.Path): boolean;
  onJump(): void;
  withPosition(obj: any): any;
  setPath(path: Tree.Path, node: Tree.Node): void;
  deleteNode(path: Tree.Path): void;
  promote(path: Tree.Path, toMainline: boolean): void;
  forceVariation(path: Tree.Path, force: boolean): void;
  setChapter(id: string, force?: boolean): void;
  toggleSticky(): void;
  toggleWrite(): void;
  isWriting(): boolean;
  makeChange(t: string, d: any): boolean;
  userJump(path: Tree.Path): void;
  currentNode(): Tree.Node;
  practice?: StudyPracticeCtrl;
  gamebookPlay(): GamebookPlayCtrl | undefined;
  nextChapter(): StudyChapterMeta | undefined;
  mutateSgConfig(config: any): void;
  isUpdatedRecently(): boolean;
  setGamebookOverride(o: GamebookOverride): void;
  onPremoveSet(): void;
  redraw: Redraw;
}

export type Tab = 'intro' | 'members' | 'chapters';
export type ToolTab = 'tags' | 'comments' | 'glyphs' | 'serverEval' | 'share' | 'multiBoard';

export interface StudyVm {
  loading: boolean;
  nextChapterId?: string;
  justSetChapterId?: string;
  tab: Prop<Tab>;
  toolTab: StoredProp<ToolTab>;
  chapterId: string;
  mode: {
    sticky: boolean;
    write: boolean;
  };
  behind: number;
  updatedAt: number;
  gamebookOverride: GamebookOverride;
}

export interface StudyData {
  id: string;
  name: string;
  members: StudyMemberMap;
  position: Position;
  ownerId: string;
  settings: StudySettings;
  visibility: 'public' | 'unlisted' | 'private';
  createdAt: number;
  from: string;
  postGameStudy?: {
    gameId: string;
    players: {
      sente: GamePlayer;
      gote: GamePlayer;
    };
    rematches: {
      sente: boolean;
      gote: boolean;
    };
    withOpponent: boolean;
  };
  likes: number;
  isNew?: boolean;
  liked: boolean;
  features: StudyFeatures;
  chapters: StudyChapterMeta[];
  chapter: StudyChapter;
  secondsSinceUpdate: number;
  description?: string;
  topics?: Topic[];
  admin: boolean;
}

export type Topic = string;

type UserSelection = 'nobody' | 'owner' | 'contributor' | 'member' | 'everyone';

export interface StudySettings {
  computer: UserSelection;
  cloneable: UserSelection;
  chat: UserSelection;
  sticky: Boolean;
  description: Boolean;
}

export interface ReloadData {
  analysis: AnalyseData;
  study: StudyData;
}

export interface Position {
  chapterId: string;
  path: Tree.Path;
}

export interface StudyFeatures {
  cloneable: boolean;
  chat: boolean;
  sticky: boolean;
}

export interface StudyChapterMeta {
  id: string;
  name: string;
  variant: VariantKey;
}

export interface StudyChapterConfig extends StudyChapterMeta {
  orientation: Color;
  description?: string;
  practice: boolean;
  gamebook: boolean;
  conceal?: number;
}

export interface StudyChapter {
  id: string;
  name: string;
  ownerId: string;
  setup: StudyChapterSetup;
  initialSfen: Sfen;
  tags: TagArray[];
  practice: boolean;
  conceal?: number;
  gamebook: boolean;
  features: StudyChapterFeatures;
  description?: string;
  gameLength?: number;
  serverEval?: StudyChapterServerEval;
}

export interface StudyChapterServerEval {
  done: boolean;
  path: string;
}

interface StudyChapterSetup {
  gameId?: string;
  variant: {
    key: VariantKey;
    name: string;
  };
  orientation: Color;
  endStatus?: {
    status: Status;
    winner?: Color;
  };
  fromSfen?: string;
  fromNotation?: string;
}

interface StudyChapterFeatures {
  computer: boolean;
}

export type StudyMember = {
  user: {
    id: string;
    name: string;
    title?: string;
  };
  role: string;
};

export interface StudyMemberMap {
  [id: string]: StudyMember;
}

export interface StudyMembersCtrl {
  dict: Prop<StudyMemberMap>;
  confing: Prop<string | undefined>;
  myId: string | null;
  inviteForm: StudyInviteFormCtrl;
  update: (members: StudyMemberMap) => void;
  setActive: (id: string) => void;
  isActive: (id: string) => boolean;
  owner: () => StudyMember;
  myMember: () => StudyMember | null;
  isOwner: () => boolean;
  canContribute: () => boolean;
  max: number;
  setRole: (id: string, role: any) => void;
  kick: (id: string) => void;
  leave: () => void;
  ordered: () => StudyMember[];
  size: () => number;
  setSpectators: (usernames?: string[]) => void;
  isOnline: (userId: string) => boolean;
  hasOnlineContributor: () => boolean;
}

export type TagTypes = string[];
export type TagArray = [string, string];

export interface LocalPaths {
  [chapterId: string]: Tree.Path;
}

export interface ChapterPreview {
  id: string;
  name: string;
  players?: {
    sente: ChapterPreviewPlayer;
    gote: ChapterPreviewPlayer;
  };
  orientation: Color;
  variant: Variant;
  sfen: string;
  lastMove?: string;
  playing: boolean;
}

export interface ChapterPreviewPlayer {
  name: string;
  title?: string;
  rating?: number;
}

export interface GamePlayer {
  playerId: string;
  userId?: string;
}

export interface StudyInviteFormCtrl {
  open: Prop<boolean>;
  candidates(): string[];
  members: Prop<StudyMemberMap>;
  setSpectators: (usernames: string[]) => void;
  setFollowings: (usernames: string[]) => void;
  delFollowing: (username: string) => void;
  addFollowing: (username: string) => void;
  toggle(): void;
  invite: (titleName: string) => void;
  previouslyInvited: StoredSet<string>;
  redraw: () => void;
}

export interface StudyTagsCtrl {
  submit(name: string): (value: string) => void;
  getChapter: () => StudyChapter;
  types: TagTypes;
}
