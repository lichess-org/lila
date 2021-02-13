import { Prop } from 'common';
import { StoredBooleanProp } from 'common/storage';

export interface Goal {
  result: string;
  moves?: number;
  cp?: number;
}

export interface StudyPracticeData {
  study: {
    id: string;
    name: string;
    desc: string;
  };
  url: string;
  completion: {
    [key: string]: number;
  };
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

export interface StudyPracticeCtrl {
  onLoad(): void;
  onJump(): void;
  onCeval(): void;
  data: StudyPracticeData;
  goal: Prop<Goal>;
  success: Prop<boolean | null>;
  nbMoves: Prop<number>;
  reset(): void;
  isWhite(): boolean;
  analysisUrl: Prop<string>;
  autoNext: StoredBooleanProp;
  goToNext(): void;
}
