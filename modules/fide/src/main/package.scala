package lila.fide

export lila.Lila.{ *, given }

private val logger = lila.log("player")

type PlayerName  = String
type PlayerToken = String

enum FideTC:
  case standard, rapid, blitz
