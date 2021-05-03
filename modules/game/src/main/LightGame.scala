package lila.game

import shogi.{ Color, Status }

import lila.user.User

case class LightGame(
    id: Game.ID,
    sentePlayer: Player,
    gotePlayer: Player,
    status: Status
) {
  def playable                                        = status < Status.Aborted
  def player(color: Color): Player                    = color.fold(sentePlayer, gotePlayer)
  def player(playerId: Player.ID): Option[Player]     = players find (_.id == playerId)
  def players                                         = List(sentePlayer, gotePlayer)
  def playerByUserId(userId: User.ID): Option[Player] = players.find(_.userId contains userId)
  def winner                                          = players find (_.wins)
  def wonBy(c: Color): Option[Boolean]                = winner.map(_.color == c)
  def finished                                        = status >= Status.Mate
}

object LightGame {

  import Game.{ BSONFields => F }

  def projection =
    lila.db.dsl.$doc(
      F.sentePlayer -> true,
      F.gotePlayer  -> true,
      F.playerUids  -> true,
      F.winnerColor -> true,
      F.status      -> true
    )
}
