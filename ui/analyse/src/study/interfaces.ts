export interface StudyController {
  setChapter(id: string): void;
  currentChapter(): StudyChapter;
  data: StudyData;
  socketHandlers: { [key: string]: any };
  vm: any;
}

export interface StudyData {
  id: string;
}

export interface StudyChapter {
  id: string;
}

export interface StudyPractice {
}

export type StudyMember = any;

export interface StudyMemberMap {
  [id: string]: StudyMember;
}
