import { Role, Rules } from 'shogiops/types';

export interface EditorData {
  baseUrl: string;
  sfen: string;
  variant: Rules;
  options: EditorOptions;
  pref: any;
  embed: boolean;
  i18n: any;
}

export interface EditorOptions {
  orientation?: Color;
  onChange?: (sfen: string, variant: VariantKey, orientation: Color) => void;
}

export interface EditorState {
  sfen: string;
  legalSfen: string | undefined;
  playable: boolean;
}

export type Redraw = () => void;

export type SpecialSelected = 'pointer' | 'trash';
export function isSpecialSelected(s: string): s is SpecialSelected {
  return ['pointer', 'trash'].includes(s);
}

export type Selected = SpecialSelected | [Color, Role];
