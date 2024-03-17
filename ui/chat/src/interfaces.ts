import { VNode } from 'snabbdom';
import { Prop } from 'common';

import type { Palantir } from 'palantir';
import { EnhanceOpts } from 'common/richText';

export interface ChatOpts {
  el: HTMLElement;
  data: ChatData;
  writeable: boolean;
  kobold: boolean;
  blind: boolean;
  timeout: boolean;
  enhance?: EnhanceOpts;
  public: boolean;
  permissions: Permissions;
  timeoutReasons?: ModerationReason[];
  i18n: I18nDict;
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
  lines: Line[];
  userId?: string;
  resourceId: string;
  loginRequired: boolean;
  restricted: boolean;
  palantir: boolean;
  hostIds?: string[];
}

export interface Line {
  u?: string; // username
  t: string; // text
  d: boolean; // deleted
  c?: string; // color
  r?: boolean; // troll
  p?: boolean; // patron
  f?: Flair;
  title?: string;
}

export interface Permissions {
  local?: boolean;
  broadcast?: boolean;
  timeout?: boolean;
  shadowban?: boolean;
}

export type Tab = string;

export interface ChatPalantir {
  instance?: Palantir;
  loaded: boolean;
  enabled: Prop<boolean>;
}

export interface ViewModel {
  tab: Tab;
  enabled: boolean;
  placeholderKey: string;
  loading: boolean;
  autofocus: boolean;
  timeout: boolean;
  writeable: boolean;
  domVersion: number;
}

export interface NoteOpts {
  id: string;
  text?: string;
  trans: Trans;
  redraw: Redraw;
}

export interface NoteCtrl {
  id: string;
  trans: Trans;
  text(): string | undefined;
  fetch(): void;
  post(text: string): void;
}

export interface ModerationOpts {
  reasons: ModerationReason[];
  permissions: Permissions;
  resourceId: string;
  redraw: Redraw;
}

export interface ModerationCtrl {
  loading(): boolean;
  data(): ModerationData | undefined;
  opts: ModerationOpts;
  open(line: HTMLElement): void;
  close(): void;
  timeout(reason: ModerationReason, text: string): void;
}

export interface ModerationData extends LightUser {
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
