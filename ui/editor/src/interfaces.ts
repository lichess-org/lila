import { Role, Rules } from 'shogiops/types';

export interface EditorData {
  baseUrl: string;
  sfen: string;
  variant: Rules;
  options?: EditorOptions;
  pref: any;
  embed: boolean;
  i18n: any;
}

export interface EditorOptions {
  orientation?: Color;
  onChange?: (sfen: string, variant: VariantKey) => void;
}

export interface EditorState {
  sfen: string;
  legalSfen: string | undefined;
  playable: boolean;
}

export type Redraw = () => void;

export type Selected = 'pointer' | 'trash' | [Color, Role];
