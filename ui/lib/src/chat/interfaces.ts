import type { VNode } from 'snabbdom';
import type { Prop } from '@/index';

import type { EnhanceOpts } from '@/richText';
export type { ChatCtrl } from './chatCtrl';

export interface ChatOpts {
  data: ChatData;
  writeable: boolean;
  kobold: boolean; // a.k.a. troll
  blind: boolean;
  timeout: boolean;
  enhance?: EnhanceOpts;
  public: boolean;
  permissions: Permissions;
  timeoutReasons?: ModerationReason[];
  preset?: string;
  noteId?: string;
  noteText?: string;
  plugin?: ChatPlugin;
  kidMode: boolean;
}

export type Tab = { key: string; isDisabled?: () => boolean };

export interface ChatPlugin extends Tab {
  name: string;
  view(): VNode;
  kidSafe?: boolean; // default false
  redraw?: () => void; // populated by chat module, not you.
}

export interface ChatData {
  id: string;
  name: string;
  lines: Line[];
  userId?: string;
  resourceType: string; // team, swiss, tournament, watcher, etc.
  resourceId: string; // team:<id>
  loginRequired: boolean;
  restricted: boolean;
  voiceChat: boolean;
  hostIds?: string[];
  opponentId?: string;
}

export interface Line {
  u?: string; // username
  t: string; // text
  d: boolean; // deleted
  c?: Color;
  r?: boolean; // troll
  pc?: PatronColor;
  f?: Flair;
  title?: string;
}

export interface Permissions {
  local?: boolean;
  broadcast?: boolean;
  timeout?: boolean;
  shadowban?: boolean;
}

export interface VoiceChatData {
  instance?: VoiceChat;
  loaded: boolean;
  enabled: Prop<boolean>;
}

export interface ViewModel {
  loading: boolean;
  autofocus: boolean;
  timeout: boolean;
  writeable: boolean;
  domVersion: number;
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

export interface VoiceChat {
  render(): VNode | undefined;
}
