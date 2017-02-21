interface Lichess {
  storage: any
  pubsub: Pubsub
  trans: Trans
  fp: any
  spinnerHtml: string
  numberFormat(n: number): string
  sound: any
  once(key: string): boolean
  quietMode: boolean
  desktopNotification(txt: string): void
}

declare type Trans = any; // todo

interface Pubsub {
  on(msg: string, f: (data: any) => void): void
  emit(msg: string): (...args: any[]) => void
}

interface Window {
  lichess: Lichess
  moment: any
  Mousetrap: any
}

declare type VariantKey = 'standard' | 'chess960' | 'antichess' | 'fromPosition' | 'kingOfTheHill' | 'threeCheck' | 'atomic' | 'horde' | 'racingKings' | 'crazyhouse'
declare type Speed = 'bullet' | 'blitz' | 'classical' | 'correspondence' | 'unlimited'
declare type Perf = 'bullet' | 'blitz' | 'classical' | 'correspondence' | 'chess960' | 'antichess' | 'fromPosition' | 'kingOfTheHill' | 'threeCheck' | 'atomic' | 'horde' | 'racingKings' | 'crazyhouse'
declare type Color = 'white' | 'black';

interface Variant {
  key: VariantKey
  name: string
  short: string
  title?: string
}
