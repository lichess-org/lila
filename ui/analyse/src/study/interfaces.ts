import { Prop } from 'common';
import { NotifController } from './notif';
import { AnalyseData } from '../interfaces';
import { StudyPracticeCtrl } from './practice/interfaces';

export interface StudyController {
  data: StudyData;
  currentChapter(): StudyChapterMeta;
  socketHandlers: { [key: string]: any };
  vm: StudyVm;
  form: any;
  members: any;
  chapters: any;
  notif: NotifController;
  commentForm: any;
  glyphForm: any;
  share: any;
  tags: any;
  toggleLike(): void;
  position(): Position;
  isChapterOwner(): boolean;
  canJumpTo(path: Tree.Path): boolean;
  onJump(): void;
  withPosition(obj: any): any;
  setPath(path: Tree.Path, node: Tree.Node): void;
  deleteNode(path: Tree.Path): void;
  promote(path: Tree.Path, toMainline: boolean): void;
  setChapter(id: string, force?: boolean): void;
  toggleSticky(): void;
  toggleWrite(): void;
  makeChange(t: string, d: any): boolean;
  startTour(): void;
  userJump(path: Tree.Path): void;
  currentNode(): Tree.Node;
  practice?: StudyPracticeCtrl;
  mutateCgConfig(config: any): void;
  redraw(): void;
}

export type Tab = 'members' | 'chapters';

export interface StudyVm {
  loading: boolean;
  nextChapterId?: string;
  tab: Prop<Tab>;
  chapterId: string;
  mode: {
    sticky: boolean;
    write: boolean;
  };
  behind: number;
}


export interface StudyData {
  id: string;
  name: string;
  members: StudyMemberMap;
  position: Position;
  ownerId: string;
  settings: StudySettings;
  visibility: 'public' | 'private';
  createdAt: number;
  from: string;
  likes: number;
  isNew?: boolean
  liked: boolean;
  features: StudyFeatures;
  chapters: StudyChapterMeta[]
  chapter: StudyChapter;
}

export interface StudySettings {
  computer: string;
  explorer: string;
  cloneable: string;
  chat: string;
  sticky: Boolean;
}

export interface ReloadData {
  analysis: AnalyseData;
  study: StudyData;
}

interface Position {
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
}

export interface StudyChapter {
  id: string;
  name: string;
  ownerId: string;
  setup: StudyChapterSetup;
  tags: TagArray[]
  practice: boolean;
  conceal?: number;
  gamebook: boolean;
  features: StudyChapterFeatures;
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

export type StudyMember = any;

export interface StudyMemberMap {
  [id: string]: StudyMember;
}

export type TagTypes = string[];
export type TagArray = [string, string];
