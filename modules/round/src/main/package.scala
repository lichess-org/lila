package lila

import lila.game.Event

package object round extends PackageObject {

  private[round] type Events = List[Event]

  private[round] def logger = lila.log("round")

  type TellRound = (lila.game.Game.ID, Any) => Unit
}

package round {

  private[round] sealed trait BenignError extends lila.base.LilaException
  private[round] case class ClientError(message: String) extends BenignError
  private[round] case class FishnetError(message: String) extends BenignError

  sealed trait OnTv

  case class OnLichessTv(channel: String, flip: Boolean) extends OnTv
  case class OnUserTv(userId: String) extends OnTv
}
