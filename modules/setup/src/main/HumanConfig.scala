package lila.setup

import chess.Mode

private[setup] trait HumanConfig extends Config:

  // casual or rated
  val mode: Mode

  def isRatedUnlimited = mode.rated && !hasClock && makeDaysPerTurn.isEmpty

private[setup] trait BaseHumanConfig extends BaseConfig:

  val modes = Mode.all.map(_.id)
