export interface ChatOpts {
  data: ChatData
  writeable: boolean
  kobold: boolean
  timeout: boolean
  parseMoves: boolean
  public: boolean
  permissions: Permissions
  i18n: Object
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
  u: string
  t: string
  d: boolean
  r?: boolean
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
  preset: Preset,
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

export interface Preset {
  setGroup(group: string): void
}
