package lila.game

import chess.{ Color, Status }

import lila.user.User

case class LightGame(
    id: Game.ID,
    whitePlayer: Player,
    blackPlayer: Player,
    status: Status
) {
  def playable                                        = status < Status.Aborted
  def player(color: Color): Player                    = color.fold(whitePlayer, blackPlayer)
  def player(playerId: Player.ID): Option[Player]     = players find (_.id == playerId)
  def players                                         = List(whitePlayer, blackPlayer)
  def playerByUserId(userId: User.ID): Option[Player] = players.find(_.userId contains userId)
  def winner                                          = players find (_.wins)
  def wonBy(c: Color): Option[Boolean]                = winner.map(_.color == c)
  def finished                                        = status >= Status.Mate
}

object LightGame {

  import Game.{ BSONFields => F }

  def projection =
    lila.db.dsl.$doc(
      F.whitePlayer -> true,
      F.blackPlayer -> true,
      F.playerUids  -> true,
      F.winnerColor -> true,
      F.status      -> true
    )
}
