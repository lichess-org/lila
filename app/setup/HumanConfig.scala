package lila
package setup

import chess.{ Mode, PausedClock }

trait HumanConfig extends Config {

  // Whether or not to use a clock
  val clock: Boolean

  // Clock time in minutes
  val time: Int

  // Clock increment in seconds
  val increment: Int

  // casual or rated
  val mode: Mode

  def validClock = clock.fold(time + increment > 0, true)

  def makeClock = clock option PausedClock(time * 60, increment)
}

trait BaseHumanConfig extends BaseConfig {

  val modes = Mode.all map (_.id)
  val modeChoices = Mode.all map { e ⇒ e.id.toString -> e.toString }

  val timeMin = 0
  val timeMax = 30
  val times = (timeMin to timeMax).toList

  val incrementMin = 0
  val incrementMax = 30
  val increments = (incrementMin to incrementMax).toList
}

//case class HookConfig(eloRange: Option[String]) 
//extends HumanConfig with EloRange 
