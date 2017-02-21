export interface ChatOpts {
  data: ChatData
  writeable: boolean
  kobold: boolean
  timeout: boolean
  parseMoves: boolean
  public: boolean
  permissions: Permissions
  i18n: Object
  preset?: string
}

interface ChatData {
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
}

interface Permissions {
  timeout?: boolean
  shadowban?: boolean
}

export type Tab = 'discussion' | 'note'

export interface Ctrl {
  data: ChatData
  opts: ChatOpts
  vm: ViewModel
  preset: PresetCtrl,
  post(text: string): boolean
  trans: any
  setTab(tab: Tab): void
  setEnabled(v: boolean): void
}

export interface ViewModel {
  tab: Tab
  enabled: boolean
  placeholderKey: string
  loading: boolean
  timeout: boolean
  writeable: boolean
}

export interface PresetCtrl {
  group(): string | undefined
  said(): string[]
  setGroup(group: string): void
  post(preset: Preset): void
}

type PresetKey = string
type PresetText = string

export interface Preset {
  key: PresetKey
  text: PresetText
}

export interface PresetGroups {
  start: Preset[]
  end: Preset[]
  [key: string]: Preset[]
}

export interface PresetOpts {
  initialGroup?: string
  redraw(): void
  post(text: string): boolean
}
