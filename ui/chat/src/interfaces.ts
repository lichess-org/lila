import { VNode } from 'snabbdom/vnode'
import { PresetCtrl } from './preset'

export interface ChatOpts {
  data: ChatData
  writeable: boolean
  kobold: boolean
  blind: boolean
  timeout: boolean
  parseMoves: boolean
  public: boolean
  permissions: Permissions
  timeoutReasons?: ModerationReason[]
  i18n: { [key: string]: string | undefined }
  preset?: string
  noteId?: string
  loadCss: (url: string) => void
  plugin?: ChatPlugin
  alwaysEnabled: boolean;
}

export interface ChatPlugin {
  tab: {
    key: string;
    name: string;
  }
  view(): VNode;
}

export interface ChatData {
  id: string
  name: string
  lines: Array<Line>
  userId?: string
  loginRequired: boolean
  restricted: boolean
}

export interface Line {
  u?: string // username
  t: string // text
  d: boolean // deleted
  c?: string // color
  r?: boolean // troll
  title?: string
}

export interface Permissions {
  local?: boolean
  timeout?: boolean
  shadowban?: boolean
}

export type Tab = string;

export interface Ctrl {
  data: ChatData
  opts: ChatOpts
  vm: ViewModel
  allTabs: Tab[]
  preset: PresetCtrl
  note?: NoteCtrl
  moderation(): ModerationCtrl | undefined
  post(text: string): void
  trans: Trans
  setTab(tab: Tab): void
  setEnabled(v: boolean): void
  plugin?: ChatPlugin
  redraw: Redraw
  destroy(): void;
}

export interface ViewModel {
  tab: Tab
  enabled: boolean
  placeholderKey: string
  loading: boolean
  timeout: boolean
  writeable: boolean
}

export interface NoteOpts {
  id: string
  trans: Trans
  redraw: Redraw
}

export interface NoteCtrl {
  id: string
  trans: Trans
  text(): string
  fetch(): void
  post(text: string): void
}

export interface ModerationOpts {
  reasons: ModerationReason[]
  permissions: Permissions
  redraw: Redraw
}

export interface ModerationCtrl {
  loading(): boolean
  data(): ModerationData | undefined
  reasons: ModerationReason[]
  permissions(): Permissions
  open(username: string): void
  close(): void
  timeout(reason: ModerationReason): void
  shadowban(): void
}

export interface ModerationData {
  id: string
  username: string
  games?: number
  troll?: boolean
  engine?: boolean
  booster?: boolean
  history?: ModerationHistoryEntry[]
}

export interface ModerationReason {
  key: string
  name: string
}

export interface ModerationHistoryEntry {
  reason: ModerationReason
  mod: string
  date: number
}

export type Redraw = () => void
