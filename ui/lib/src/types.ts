import type { LiconValue } from '@/licon';

export interface QuestionChoice {
  // file://./../../round/src/ctrl.ts
  action: () => void;
  icon?: LiconValue;
  text?: string;
}

export interface QuestionOpts {
  prompt: string; // TODO i18nkey, or just always pretranslate
  yes?: QuestionChoice;
  no?: QuestionChoice;
}
