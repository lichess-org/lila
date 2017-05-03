export type DasherData = any

export interface PingData {
  ping: number | undefined
  server: number | undefined
}

export interface PingCtrl {
  data: PingData
  trans: Trans
}

export interface DasherOpts {
  playing: boolean
}

export interface Ctrl {
  data(): DasherData | undefined
  initiating(): boolean
  trans(): Trans
  ping: PingCtrl
  opts: DasherOpts
}

export type Redraw = () => void
