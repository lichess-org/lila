package lila.app
package setup

import chess.Mode

trait HumanConfig extends Config {

  // casual or rated
  val mode: Mode
}

trait BaseHumanConfig extends BaseConfig {

  val modes = Mode.all map (_.id)
  val modeChoices = Mode.all map { e ⇒ e.id.toString -> e.toString }
}
