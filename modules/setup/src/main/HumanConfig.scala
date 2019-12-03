package lila.setup

import chess.Mode

private trait HumanConfig extends Config {

  // casual or rated
  val mode: Mode
}

private trait BaseHumanConfig extends BaseConfig {

  val modes = Mode.all map (_.id)
  val modeChoices = Mode.all map { e => e.id.toString -> e.toString }
}
