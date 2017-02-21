interface Lichess {
  storage: any
  pubsub: Pubsub
  trans: any
  fp: any
  spinnerHtml: string
  numberFormat(n: number): string
}

interface Pubsub {
  on(msg: string, f: (data: any) => void): void
  emit(msg: string): (...args: any[]) => void
}

interface Window {
  lichess: Lichess
  moment: any
  Mousetrap: any
}
