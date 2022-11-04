package lila.swiss

import com.softwaremill.tagging._
import org.joda.time.DateTime
import scala.concurrent.ExecutionContext

import lila.user.User
import lila.db.dsl._
import lila.swiss.BsonHandlers.SwissBanBSONHandler
import lila.game.Game

case class SwissBan(_id: User.ID, until: DateTime, hours: Int)

/*
 * Failure to play a swiss game results in a 24h ban from swiss events.
 * Consecutive failures result in doubling ban duration.
 * Playing a swiss game resets the duration.
 */
final class SwissBanApi(coll: Coll @@ BanColl)(implicit ec: ExecutionContext) {

  def bannedUntil(user: User.ID): Fu[Option[DateTime]] =
    coll.primitiveOne[DateTime]($id(user) ++ $doc("until" $gt DateTime.now), "until")

  def get(user: User.ID): Fu[Option[SwissBan]] = coll.byId[SwissBan](user)

  def onGameFinish(game: Game) =
    game.userIds
      .map { userId =>
        if (game.playerWhoDidNotMove.exists(_.userId has userId)) onStall(userId)
        else onGoodGame(userId)
      }
      .sequenceFu
      .void

  private def onStall(user: User.ID): Funit = get(user) flatMap { prev =>
    val hours = prev.fold(24)(_.hours * 2)
    coll.update
      .one(
        $id(user),
        SwissBan(user, DateTime.now plusHours hours, hours),
        upsert = true
      )
      .void
  }

  private def onGoodGame(user: User.ID) = coll.delete.one($id(user))
}
