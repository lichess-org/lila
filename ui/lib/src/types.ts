import type { Icon } from '@/icons';

export interface QuestionChoice {
  // file://./../../round/src/ctrl.ts
  action: () => void;
  icon?: Icon;
  text?: string;
}

export interface QuestionOpts {
  prompt: string; // TODO i18nkey, or just always pretranslate
  yes?: QuestionChoice;
  no?: QuestionChoice;
}

export interface LobbyShortcut {
  id: string;
  name?: string;
  iconKey?: Icon;
  iconUrl?: string;
  iconMaskUrl?: string;
  url?: string;
}
