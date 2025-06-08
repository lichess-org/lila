package lila.setup

import chess.Rated

private[setup] trait HumanConfig extends Config:

  val rated: Rated

  def isRatedUnlimited = rated.yes && !hasClock && makeDaysPerTurn.isEmpty
