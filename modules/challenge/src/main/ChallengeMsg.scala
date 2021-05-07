package lila.challenge

import scala.concurrent.ExecutionContext

import lila.common.{ LightUser, Template }
import lila.user.{ LightUserApi, User }

final class ChallengeMsg(msgApi: lila.msg.MsgApi, lightUserApi: LightUserApi)(implicit
    ec: ExecutionContext
) {

  def onApiPair(challenge: Challenge)(managedBy: User, template: Option[Template]): Funit =
    challenge.userIds.map(lightUserApi.async).sequenceFu.flatMap {
      _.flatten match {
        case List(u1, u2) => onApiPair(challenge.id, u1, u2)(managedBy.id, template)
        case _            => funit
      }
    }

  def onApiPair(gameId: lila.game.Game.ID, u1: LightUser, u2: LightUser)(
      managedById: User.ID,
      template: Option[Template]
  ): Funit =
    List(u1 -> u2, u2 -> u1)
      .map { case (u1, u2) =>
        val msg = template
          .fold("Your game with {opponent} is ready: {game}.")(_.value)
          .replace("{player}", s"@${u1.name}")
          .replace("{opponent}", s"@${u2.name}")
          .replace("{game}", s"#${gameId}")
        msgApi.post(managedById, u1.id, msg, multi = true)
      }
      .sequenceFu
      .void
}
