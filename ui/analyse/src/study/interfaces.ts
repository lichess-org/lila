import * as cg from 'chessground/types';
import { Config as CgConfig } from 'chessground/config';
import { Prop } from 'common';
import { NotifCtrl } from './notif';
import { AnalyseData, Redraw } from '../interfaces';
import { StudyPracticeCtrl } from './practice/interfaces';
import { StudyChaptersCtrl } from './studyChapters';
import { DescriptionCtrl } from './description';
import GamebookPlayCtrl from './gamebook/gamebookPlayCtrl';
import { GamebookOverride } from './gamebook/interfaces';
import { GlyphCtrl } from './studyGlyph';
import { CommentForm } from './commentForm';
import { TopicsCtrl } from './topics';
import RelayCtrl from './relay/relayCtrl';
import ServerEval from './serverEval';
import { MultiBoardCtrl } from './multiBoard';
import { StudyShareCtrl } from './studyShare';
import { TagsCtrl } from './studyTags';
import { StudyFormCtrl } from './studyForm';
import { StudyMemberCtrl } from './studyMembers';
import { Opening } from '../explorer/interfaces';
import { StudySocketSendParams } from '../socket';

export interface StudyCtrl {
  data: StudyData;
  currentChapter(): StudyChapterMeta;
  socketHandler(t: string, d: any): boolean;
  vm: StudyVm;
  relay?: RelayCtrl;
  multiBoard: MultiBoardCtrl;
  form: StudyFormCtrl;
  members: StudyMemberCtrl;
  chapters: StudyChaptersCtrl;
  notif: NotifCtrl;
  commentForm: CommentForm;
  glyphForm: GlyphCtrl;
  topics: TopicsCtrl;
  serverEval: ServerEval;
  share: StudyShareCtrl;
  tags: TagsCtrl;
  studyDesc: DescriptionCtrl;
  chapterDesc: DescriptionCtrl;
  toggleLike(): void;
  position(): Position;
  isChapterOwner(): boolean;
  canJumpTo(path: Tree.Path): boolean;
  onJump(): void;
  withPosition<T>(obj: T): T & { ch: string; path: string };
  setPath(path: Tree.Path, node: Tree.Node): void;
  deleteNode(path: Tree.Path): void;
  promote(path: Tree.Path, toMainline: boolean): void;
  forceVariation(path: Tree.Path, force: boolean): void;
  setChapter(id: string, force?: boolean): void;
  toggleSticky(): void;
  toggleWrite(): void;
  isWriting(): boolean;
  makeChange(...args: StudySocketSendParams): boolean;
  startTour(): void;
  userJump(path: Tree.Path): void;
  currentNode(): Tree.Node;
  practice?: StudyPracticeCtrl;
  gamebookPlay(): GamebookPlayCtrl | undefined;
  prevChapter(): StudyChapterMeta | undefined;
  nextChapter(): StudyChapterMeta | undefined;
  hasNextChapter(): boolean;
  goToPrevChapter(): void;
  goToNextChapter(): void;
  mutateCgConfig(config: Required<Pick<CgConfig, 'drawable'>>): void;
  isUpdatedRecently(): boolean;
  setGamebookOverride(o: GamebookOverride): void;
  explorerGame(gameId: string, insert: boolean): void;
  onPremoveSet(): void;
  looksNew(): boolean;
  redraw: Redraw;
  trans: Trans;
}

export type Tab = 'intro' | 'members' | 'chapters';
export type ToolTab = 'tags' | 'comments' | 'glyphs' | 'serverEval' | 'share' | 'multiBoard';
export type Visibility = 'public' | 'unlisted' | 'private';

export interface StudyVm {
  loading: boolean;
  nextChapterId?: string;
  justSetChapterId?: string;
  tab: Prop<Tab>;
  toolTab: Prop<ToolTab>;
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
  visibility: Visibility;
  createdAt: number;
  from: string;
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
  hideRatings?: boolean;
}

export type Topic = string;

type UserSelection = 'nobody' | 'owner' | 'contributor' | 'member' | 'everyone';

export interface StudySettings {
  computer: UserSelection;
  explorer: UserSelection;
  cloneable: UserSelection;
  chat: UserSelection;
  sticky: boolean;
  description: boolean;
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
  ongoing?: boolean;
  res?: string;
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
  tags: TagArray[];
  practice: boolean;
  conceal?: number;
  gamebook: boolean;
  features: StudyChapterFeatures;
  description?: string;
  relay?: StudyChapterRelay;
}

export interface StudyChapterRelay {
  path: Tree.Path;
  secondsSinceLastMove?: number;
  lastMoveAt?: number;
}

interface StudyChapterSetup {
  gameId?: string;
  variant: {
    key: string;
    name: string;
  };
  orientation: Color;
  fromFen?: string;
}

interface StudyChapterFeatures {
  computer: boolean;
  explorer: boolean;
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

export type TagTypes = string[];
export type TagArray = [string, string];

export interface LocalPaths {
  [chapterId: string]: Tree.Path;
}

export interface ChapterPreview {
  id: string;
  name: string;
  players?: {
    white: ChapterPreviewPlayer;
    black: ChapterPreviewPlayer;
  };
  orientation: Color;
  fen: string;
  lastMove?: string;
  playing: boolean;
}

export interface ChapterPreviewPlayer {
  name: string;
  title?: string;
  rating?: number;
}

export type Orientation = 'black' | 'white' | 'auto';
export type ChapterMode = 'normal' | 'practice' | 'gamebook' | 'conceal';

export interface ChapterData {
  name: string;
  game?: string;
  variant?: VariantKey;
  fen?: Fen | null;
  pgn?: string;
  orientation: Orientation;
  mode: ChapterMode;
  initial: boolean;
  isDefaultName: boolean;
}

export interface EditChapterData {
  id: string;
  name: string;
  orientation: Orientation;
  mode: ChapterMode;
  description: string;
}

export interface AnaDests {
  dests: string;
  path: string;
  ch?: string;
  opening?: Opening;
}

export interface AnaMove {
  orig: string;
  dest: string;
  fen: Fen;
  path: string;
  variant?: VariantKey;
  ch?: string;
  promotion?: cg.Role;
}

export interface AnaDrop {
  role: cg.Role;
  pos: Key;
  variant?: VariantKey;
  fen: Fen;
  path: string;
  ch?: string;
}
export interface WithWho {
  w: {
    s: string;
    u: string;
  };
}

export interface WithPosition {
  p: Position;
}

export interface WithChapterId {
  chapterId: string;
}

export type WithWhoAndPos = WithWho & WithPosition;
export type WithWhoAndChap = WithWho & WithChapterId;
