import { Role, Rules } from 'shogiops/types';

export interface OpeningPosition {
  japanese?: string;
  english: string;
  sfen: string;
}

export interface EditorData {
  baseUrl: string;
  sfen: string;
  variant: Rules;
  options?: EditorOptions;
  pref: any;
  embed: boolean;
  positions?: OpeningPosition[];
  i18n: any;
}

export interface EditorOptions {
  orientation?: Color;
  onChange?: (sfen: string) => void;
}

export interface EditorState {
  sfen: string;
  legalSfen: string | undefined;
  playable: boolean;
}

export type Redraw = () => void;

export type Selected = 'pointer' | 'trash' | [Color, Role];
