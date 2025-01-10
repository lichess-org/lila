import type { Prop } from 'common/common';
import type { VNode } from 'snabbdom';
import type { PresetCtrl } from './preset';

export interface ChatOpts {
  data: ChatData;
  writeable: boolean;
  kobold: boolean;
  blind: boolean;
  timeout: boolean;
  parseMoves: boolean;
  public: boolean;
  permissions: Permissions;
  timeoutReasons?: ModerationReason[];
  preset?: string;
  noteId?: string;
  noteText?: string;
  plugin?: ChatPlugin;
  alwaysEnabled: boolean;
}

export interface ChatPlugin {
  tab: {
    key: string;
    name: string;
  };
  view(): VNode;
}

export interface ChatData {
  id: string;
  name: string;
  lines: Array<Line>;
  userId?: string;
  resourceId: string;
  loginRequired: boolean;
  restricted: boolean;
  palantir: boolean;
  domVersion: number;
}

export interface Line {
  u?: string; // username
  t: string; // text
  d: boolean; // deleted
  c?: string; // color
  r?: boolean; // troll
  title?: string;
}

export interface Permissions {
  local?: boolean;
  timeout?: boolean;
  shadowban?: boolean;
}

export type Tab = string;

export interface ChatCtrl {
  data: ChatData;
  opts: ChatOpts;
  vm: ViewModel;
  allTabs: Tab[];
  preset: PresetCtrl;
  note?: NoteCtrl;
  moderation(): ModerationCtrl | undefined;
  post(text: string): void;
  setTab(tab: Tab): void;
  setEnabled(v: boolean): void;
  plugin?: ChatPlugin;
  palantir: ChatPalantir;
  redraw: Redraw;
  destroy(): void;
}

export interface ChatPalantir {
  instance?: any;
  loaded: boolean;
  enabled: Prop<boolean>;
}

export interface ViewModel {
  tab: Tab;
  enabled: boolean;
  loading: boolean;
  timeout: boolean;
  writeable: boolean;
}

export interface NoteOpts {
  id: string;
  text?: string;
  redraw: Redraw;
}

export interface NoteCtrl {
  id: string;
  text(): string | undefined;
  fetch(): void;
  post(text: string): void;
}

export interface ModerationOpts {
  reasons: ModerationReason[];
  permissions: Permissions;
  redraw: Redraw;
}

export interface ModerationCtrl {
  loading(): boolean;
  data(): ModerationData | undefined;
  reasons: ModerationReason[];
  permissions(): Permissions;
  open(line: HTMLElement): void;
  close(): void;
  timeout(reason: ModerationReason, text: string): void;
}

export interface ModerationData {
  id: string;
  username: string;
  text: string;
  games?: number;
  tos?: boolean;
  history?: ModerationHistoryEntry[];
}

export interface ModerationReason {
  key: string;
  name: string;
}

export interface ModerationHistoryEntry {
  reason: ModerationReason;
  mod: string;
  date: number;
}

export type Redraw = () => void;
