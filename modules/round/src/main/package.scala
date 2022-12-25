package lila.round

import lila.game.Event

export lila.Lila.{ *, given }

private type Events = List[Event]
private val logger = lila.log("round")

trait BenignError                        extends lila.base.LilaException
case class ClientError(message: String)  extends BenignError
case class FishnetError(message: String) extends BenignError
case class GameIsFinishedError(pov: lila.game.Pov) extends BenignError:
  val message = s"$pov game is finished"

enum OnTv:
  case Lichess(channel: String, flip: Boolean)
  case User(userId: UserId)
