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
