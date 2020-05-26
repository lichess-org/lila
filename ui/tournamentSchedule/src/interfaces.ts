export interface TournamentPerf {
  icon: string
  key: Perf
  name: string
  position: number
}

export interface Tournament {
  battle: boolean
  clock: {
    limit: number
    increment: number
  }
  createdBy: string
  finishesAt: number
  fullName: string
  hasMaxRating: boolean
  id: string
  major: boolean
  minutes: number
  nbPlayers: number
  perf: TournamentPerf
  position: number
  rated: boolean
  schedule: {
    freq: string
    speed: Speed
  }
  secondsToStart: number
  startsAt: number
  status: number
  system: string
  variant: Variant
}

export interface Data {
  finished: Tournament[]
  started: Tournament[]
  created: Tournament[]
  perfs: TournamentPerf[]
}