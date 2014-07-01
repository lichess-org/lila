package lila.pool

import org.joda.time.DateTime

object Wave {

  def calculateNext(pool: Pool) = {
    val nbPlayers = pool.playingPlayers.size
    val id = pool.setup.id
    val seconds = math.min(
      upperBound(id),
      math.max(
        lowerBound(id),
        equation(id)(nbPlayers).toInt
      )
    )
    DateTime.now plusSeconds seconds
  }

  def equation(id: String) = id match {
    case "1-0" => (x: Int) => 106.72 * math.exp(-0.04 * x)
    case "3-0" => (x: Int) => 155.65 * math.exp(-0.04 * x)
    case "5-0" => (x: Int) => 370.29 * math.exp(-0.05 * x)
    case "5-5" | _ => (x: Int) => 495.45 * math.exp(-0.04 * x)
  }
  def lowerBound(id: String) = 20
  def upperBound(id: String) = id match {
    case "1-0" => 60
    case _     => 80
  }
}
