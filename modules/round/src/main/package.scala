package lila

import lila.game.Event

package object round extends PackageObject {

  private[round] type Events = List[Event]

  private[round] def logger = lila.log("round")
}

package round {

  trait BenignError                        extends lila.base.LilaException
  case class ClientError(message: String)  extends BenignError
  case class FishnetError(message: String) extends BenignError
  case class GameIsFinishedError(pov: lila.game.Pov) extends BenignError {
    val message = s"$pov game is finished"
  }

  sealed trait OnTv

  case class OnLichessTv(channel: String, flip: Boolean) extends OnTv
  case class OnUserTv(userId: String)                    extends OnTv
}
