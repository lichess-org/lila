package lila.challenge

import lila.common.{ LightUser, Template }
import lila.user.{ LightUserApi, User }

final class ChallengeMsg(msgApi: lila.msg.MsgApi, lightUserApi: LightUserApi)(using Executor):

  // deprecated acceptByToken
  def onApiPair(challenge: Challenge)(template: Option[Template]): Funit =
    challenge.userIds.map(lightUserApi.async).parallel.flatMap {
      _.flatten match
        case List(u1, u2) =>
          val gameId = challenge.id into GameId
          sendGameMessage(gameId, u1, u2, u2.id, template) >>
            sendGameMessage(gameId, u2, u1, u1.id, template).void
        case _ => funit
    }

  // bulk
  def onApiPair(gameId: GameId, u1: LightUser, u2: LightUser)(
      managedById: UserId,
      template: Option[Template]
  ): Funit =
    List(u1 -> u2, u2 -> u1)
      .map: (u1, u2) =>
        sendGameMessage(gameId, u1, u2, managedById, template)
      .parallel
      .void

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
