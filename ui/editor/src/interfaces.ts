import { Role } from 'shogiground/types';

export interface OpeningPosition {
  japanese?: string;
  english: string;
  sfen: string;
  epd?: string;
}

export interface EditorConfig {
  baseUrl: string;
  sfen: string;
  options?: EditorOptions;
  pieceNotation: number;
  animation: {
    duration: number;
  };
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
