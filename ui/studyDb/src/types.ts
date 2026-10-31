import type { TreePath } from 'lib/tree/types';

import type { Rect } from './windowBounds';

declare module 'lib/pubsub' {
  interface PubsubEvents {
    'study.navigate': (request: StudyNavigationRequest) => void;
    'studyDb.updated': (update: StudyUpdated) => void;
  }
}

export interface StudyNavigationRequest {
  studyId: string;
  chapterId?: string;
  path?: TreePath;
  orientation?: Color;
  complete?: (error?: Error) => void;
}

export type TabStorage = { active: boolean } & (
  | { kind: 'historyTab' | 'memberTab' | 'bookmarkTab' | 'searchTab' }
  | { kind: 'resultTab'; search: StudySearchQuery }
);

export type StoredTab = TabStorage & { geometry: Rect };

export type StudySearchOrder =
  | 'relevant'
  | 'hot'
  | 'newest'
  | 'oldest'
  | 'updated'
  | 'popular'
  | 'alphabetical';

export interface StudySearchQuery {
  order: StudySearchOrder;
  study?: {
    text?: string;
    name?: string;
    description?: string;
    topics?: string;
    owner?: string;
    member?: string;
  };
  chapter?: {
    text?: string;
    name?: string;
    description?: string;
    tags?: {
      variant?: string;
      eco?: string;
      opening?: string;
      player1?: string;
      player2?: string;
      fideId1?: string;
      fideId2?: string;
      color1?: Color;
      color2?: Color;
      event?: string;
    };
  };
}

export interface ChapterData {
  id: string;
  name: string;
  description?: string;
  variant?: VariantKey;
  setup?: {
    variant: { key: VariantKey; name: string };
    orientation: Color;
    gameId?: string;
    fromFen?: boolean;
  };
  tags?: [string, string][];
  order?: number;
  ownerId?: string;
  createdAt?: number;
  conceal?: number;
  practice?: boolean;
  gamebook?: boolean;
}

export interface StudyData {
  studyId: string;
  studyName: string;
  metadataRevision?: number;
  role?: 'owner' | 'contributor' | 'member';
  visibility: 'public' | 'unlisted' | 'private';
  updatedAt: number;
  createdAt?: number;
  description?: string;
  ownerId?: string;
  public?: boolean;
  settings?: Record<string, unknown>;
  from?: string | Record<string, unknown>;
  topics: string[];
  chapterCount?: number;
  studyDbExcluded?: boolean;
  liked?: boolean;
  likes?: number;
  flair?: string;
  owner?: LightUser | null;
  members?: { user: LightUser; role: 'r' | 'w' }[];
  chapters?: ChapterData[];
  matchingChapterIds?: string[];
  matchingChapterFields?: Record<string, string[]>;
  visit?: StudyVisit;
}

export interface StudyPosition {
  chapterId: string;
  orientation?: Color;
  path?: TreePath;
}

export interface StudyVisit extends StudyPosition {
  at: number;
}

export interface StudyUpdated extends StudyPosition {
  study: StudyData;
}

export type SyncRecord = { type: 'upsert'; study: StudyData } | { type: 'complete' };

export type SyncUpdate = SyncRecord | { type: 'delete'; studyId: string };

export type StudyDbRequest =
  | { mode: 'sync'; manifest: Record<string, number> }
  | { mode: 'fetch'; ids: string[] };

export interface SyncCache {
  studies: StudyData[];
  complete: boolean;
}

export interface Bookmark {
  name: string;
  studyId: string;
  studyName: string;
}

export interface BookmarkFolder {
  name: string;
  children: BookmarkEntry[];
}

export type BookmarkEntry = Bookmark | BookmarkFolder;

export type StudyDrag =
  | { kind: 'studies'; entries: { studyId: string; studyName: string }[] }
  | { kind: 'bookmarks'; paths: number[][] };

export const studyDragType = 'application/x-lichess-study-db';
