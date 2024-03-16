package lila.fide

export lila.Lila.{ *, given }

private val logger = lila.log("player")

type PlayerToken = String

enum FideTC:
  case standard, rapid, blitz
