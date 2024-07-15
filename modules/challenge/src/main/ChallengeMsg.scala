package lila.challenge

import chess.ByColor

import lila.core.LightUser
import lila.core.data.Template

final class ChallengeMsg(msgApi: lila.core.msg.MsgApi)(using Executor):

  // bulk
  def onApiPair(gameId: GameId, users: ByColor[LightUser])(
      managedById: UserId,
      template: Option[Template]
  ): Funit =
    List(users, users.swap)
      .map(_.toPair)
      .sequentiallyVoid { (u1, u2) =>
        sendGameMessage(gameId, u1, u2, managedById, template)
      }

  private def sendGameMessage(
      gameId: GameId,
      u1: LightUser,
      u2: LightUser,
      sender: UserId,
      template: Option[Template]
  ) =
    val msg = template
      .fold("Your game with {opponent} is ready: {game}.")(_.value)
      .replace("{player}", s"@${u1.name}")
      .replace("{opponent}", s"@${u2.name}")
      .replace("{game}", s"#${gameId}")
    msgApi.post(sender, u1.id, msg, multi = true, ignoreSecurity = true)
