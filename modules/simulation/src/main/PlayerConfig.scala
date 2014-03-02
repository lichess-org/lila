package lila.simulation

import scala.util.Random

private[simulation] case class PlayerConfig(
    useClock: Option[Boolean], // none = random
    randomClock: Boolean,
    thinkDelay: Int) { // maximum wait in millis

  val rematchProbability = 4d / 4

  private val defaultClockConfig = (5, 8)

  private def randomClockConfig = Random.nextInt(5) match {
    case 0 => (1, 0)
    case 1 => (0, 1)
    case 2 => (3, 3)
    case 3 => (5, 8)
    case _ => (2, 12)
  }

  def hookConfig = {
    val clock = randomClock ? randomClockConfig | defaultClockConfig
    lila.setup.HookConfig.default.copy(
      clock = useClock | Random.nextBoolean,
      time = clock._1,
      increment = clock._2,
      color = lila.lobby.Color.random,
      mode = chess.Mode.Casual)
  }
}
