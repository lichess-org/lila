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
}

interface Permissions {
  timeout?: boolean
  shadowban?: boolean
}

export interface ChatCtrl {
  data: ChatData
  preset: Preset
}

export interface Preset {
  setGroup(group: string): void
}
