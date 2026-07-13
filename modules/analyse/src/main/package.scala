package lila.analyse

export lila.core.lilaism.Lilaism.{ *, given }
export lila.common.extensions.*
export lila.tree.{ Advice, Analysis, Info }

type GamePhase = "opening" | "middlegame" | "endgame"
val phaseNames: List[GamePhase] = List("opening", "middlegame", "endgame")
