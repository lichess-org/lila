import { Prop } from 'common';
import { StudyChapterMeta } from '../interfaces';

export interface Goal {
  result: string;
  moves?: number;
  cp?: number
}

export interface PracticeData {
  study: {
    id: string;
    name: string;
    desc: string;
  }
  url: string;
  completion: {
    [key: string]: number;
  }
  structure: PracticeSection[];
}

export interface PracticeSection {
  id: string;
  name: string;
  studies: PracticeStudy[];
}

export interface PracticeStudy {
  id: string;
  slug: string;
  name: string;
}

export interface PracticeCtrl {
  onReload(): void;
  onJump(): void;
  onCeval(): void;
  data: PracticeData;
  goal: Prop<Goal>;
  success: Prop<boolean | null>;
  comment: Prop<string | undefined>;
  nbMoves: Prop<number>;
  reset(): void;
  isWhite(): boolean;
  analysisUrl: Prop<string>;
  nextChapter(): StudyChapterMeta | undefined
}
