package lila.round

import lila.game.Event

export lila.Lila.{ *, given }

private type Events = List[Event]
private def logger = lila.log("round")

trait BenignError                        extends lila.base.LilaException
case class ClientError(message: String)  extends BenignError
case class FishnetError(message: String) extends BenignError
case class GameIsFinishedError(pov: lila.game.Pov) extends BenignError:
  val message = s"$pov game is finished"

sealed trait OnTv

case class OnLichessTv(channel: String, flip: Boolean) extends OnTv
case class OnUserTv(userId: String)                    extends OnTv
