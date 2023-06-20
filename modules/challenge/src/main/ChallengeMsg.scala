package lila.challenge

import lila.common.{ LightUser, Template }
import lila.user.{ LightUserApi, User }

final class ChallengeMsg(msgApi: lila.msg.MsgApi, lightUserApi: LightUserApi)(using Executor):

  def onApiPair(challenge: Challenge)(managedBy: User, template: Option[Template]): Funit =
    challenge.userIds.map(lightUserApi.async).parallel.flatMap {
      _.flatten match
        case List(u1, u2) => onApiPair(challenge.id into GameId, u1, u2)(managedBy.id, template)
        case _            => funit
    }

  def onApiPair(gameId: GameId, u1: LightUser, u2: LightUser)(
      managedById: UserId,
      template: Option[Template]
  ): Funit =
    List(u1 -> u2, u2 -> u1)
      .map { case (u1, u2) =>
        val msg = template
          .fold("Your game with {opponent} is ready: {game}.")(_.value)
          .replace("{player}", s"@${u1.name}")
          .replace("{opponent}", s"@${u2.name}")
          .replace("{game}", s"#${gameId}")
        msgApi.post(managedById, u1.id, msg, multi = true, ignoreSecurity = true)
      }
      .parallel
      .void
