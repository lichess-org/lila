interface Lichess {
  storage: any
  pubsub: any
  trans: any
  fp: any
  spinnerHtml: string
}

interface Window {
  lichess: Lichess
  Mousetrap: any
}
