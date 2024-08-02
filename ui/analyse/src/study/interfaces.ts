import * as cg from 'chessground/types';
import { Prop } from 'common';
import { AnalyseData } from '../interfaces';
import { GamebookOverride } from './gamebook/interfaces';
import { Opening } from '../explorer/interfaces';
import AnalyseCtrl from '../ctrl';

export type Tab = 'intro' | 'members' | 'chapters';
export type ChapterTab = 'init' | 'edit' | 'game' | 'fen' | 'pgn';
export type ToolTab = 'tags' | 'comments' | 'glyphs' | 'serverEval' | 'share' | 'multiBoard';
export type Visibility = 'public' | 'unlisted' | 'private';
export type ChapterId = string;
export type TeamName = string;
export type OutcomeStr = '1-0' | '0-1' | '½-½';
export type StatusStr = OutcomeStr | '*';
export type ClockCentis = number;
export type BothClocks = [ClockCentis?, ClockCentis?];

export interface StudyTour {
  study(ctrl: AnalyseCtrl): void;
  chapter(cb: (tab: ChapterTab) => void): void;
}

export interface StudyVm {
  loading: boolean;
  nextChapterId?: ChapterId;
  justSetChapterId?: ChapterId;
  tab: Prop<Tab>;
  toolTab: Prop<ToolTab>;
  chapterId: ChapterId;
  mode: {
    sticky: boolean;
    write: boolean;
  };
  behind: number;
  updatedAt: number;
  gamebookOverride: GamebookOverride;
}

export type Federations = { [key: string]: string };

export interface StudyData {
  id: string;
  name: string;
  flair?: Flair;
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
  chapter: StudyChapter;
  secondsSinceUpdate: number;
  description?: string;
  topics?: Topic[];
  admin: boolean;
  showRatings: boolean;
  federations?: Federations;
}

export interface StudyDataFromServer extends StudyData {
  chapters: ChapterPreviewFromServer[];
}

export type Topic = string;

type UserSelection = 'nobody' | 'owner' | 'contributor' | 'member' | 'everyone';

export interface StudySettings {
  computer: UserSelection;
  explorer: UserSelection;
  cloneable: UserSelection;
  shareable: UserSelection;
  chat: UserSelection;
  sticky?: boolean;
  description?: boolean;
}

export interface ReloadData {
  analysis: AnalyseData;
  study: StudyDataFromServer;
}

export interface Position {
  chapterId: ChapterId;
  path: Tree.Path;
}

export interface StudyFeatures {
  cloneable: boolean;
  shareable: boolean;
  chat: boolean;
  sticky: boolean;
}

export type RelayPlayer = [string?, string?, number?];

export interface StudyChapterConfig {
  id: string;
  name: string;
  orientation?: Color; // defaults to white
  description?: string;
  practice: boolean;
  gamebook: boolean;
  conceal?: number;
}

export interface StudyChapter {
  id: ChapterId;
  name: string;
  ownerId: string;
  setup: StudyChapterSetup;
  tags: TagArray[];
  practice: boolean;
  conceal?: number;
  gamebook: boolean;
  features: StudyChapterFeatures;
  description?: string;
  relayPath?: Tree.Path;
  serverEval?: StudyChapterServerEval;
}

export interface StudyChapterServerEval {
  done: boolean;
  path: string;
}

export interface StudyChapterRelay {
  path: Tree.Path;
  lastMoveAt?: number;
}

interface StudyChapterSetup {
  gameId?: string;
  variant: {
    key: VariantKey;
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

export interface ChapterPreviewBase {
  id: ChapterId;
  name: string;
  status?: StatusStr;
  lastMove?: string;
  check?: '+' | '#';
}

export interface ChapterPreviewFromServer extends ChapterPreviewBase {
  fen?: string; // defaults to initial
  players?: PairOf<ChapterPreviewPlayerFromServer>;
  thinkTime?: number; // seconds since last move
  orientation?: Color; // defaults to white
  variant?: VariantKey; // defaults to standard
}

export interface ChapterPreview extends ChapterPreviewBase {
  fen: string;
  players?: ChapterPreviewPlayers;
  lastMoveAt?: number;
  orientation: Color;
  variant: VariantKey;
  playing: boolean;
}

export interface ChapterPreviewPlayers {
  white: ChapterPreviewPlayer;
  black: ChapterPreviewPlayer;
}

export interface Federation {
  id: string;
  name: string;
}
export interface ChapterPreviewPlayerBase {
  name?: string;
  title?: string;
  rating?: number;
  clock?: ClockCentis;
  team?: string;
}
export interface ChapterPreviewPlayerFromServer extends ChapterPreviewPlayerBase {
  fed?: string;
}
export interface ChapterPreviewPlayer extends ChapterPreviewPlayerBase {
  fed?: Federation;
}

export type Orientation = 'black' | 'white' | 'auto';
export type ChapterMode = 'normal' | 'practice' | 'gamebook' | 'conceal';

export interface ChapterData {
  name: string;
  game?: string;
  variant?: VariantKey;
  fen?: cg.FEN | null;
  pgn?: string;
  orientation: Orientation;
  mode: ChapterMode;
  initial: boolean;
  isDefaultName: boolean;
}

export interface EditChapterData {
  id: ChapterId;
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
  fen: cg.FEN;
  path: string;
  variant?: VariantKey;
  ch?: string;
  promotion?: cg.Role;
}

export interface AnaDrop {
  role: cg.Role;
  pos: Key;
  variant?: VariantKey;
  fen: cg.FEN;
  path: string;
  ch?: string;
}
export interface ServerNodeMsg extends WithWhoAndPos {
  d: string;
  n: Tree.NodeFromServer;
  o: Opening;
  s: boolean;
  relayPath?: Tree.Path;
}
export interface ServerClockMsg extends WithWhoAndPos {
  c?: number;
  relayClocks?: PairOf<ClockCentis>;
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
  chapterId: ChapterId;
}

export type WithWhoAndPos = WithWho & WithPosition;
export type WithWhoAndChap = WithWho & WithChapterId;

export interface ChapterSelect {
  is: (idOrNumber: ChapterId | number) => boolean;
  set: (idOrNumber: ChapterId | number, force?: boolean) => Promise<boolean>;
}
