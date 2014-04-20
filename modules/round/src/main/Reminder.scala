package lila.round

import lila.db.Types._
import lila.game.{ Game, Pov, GameRepo }
import lila.user.User
import reactivemongo.bson._

private final class Reminder(coll: Coll) {

  private case class Remind(_id: String, g: List[String])

  private implicit val remindBSONHandler = Macros.handler[Remind]

  val nowPlaying: (User, Int) => Fu[List[Pov]] = (user, max) =>
    coll.find(BSONDocument("_id" -> user.id)).one[Remind] flatMap {
      case None => fuccess(Nil)
      case Some(r) =>
        val ids = scala.util.Random.shuffle(r.g).take(max * 2)
        GameRepo games ids map2 { (g: Game) =>
          if (remindable(g) && g.playable && g.turnOf(user)) Some(g)
          else {
            remove(user.id, g.id)
            none
          }
        } map {
          _.flatten flatMap { Pov(_, user) } sortBy {
            -_.game.updatedAt.fold(0l)(_.getMillis)
          } take max
        }

    }

  private[round] def remind(game: Game) {
    if (remindable(game)) {
      game.players foreach { player =>
        player.userId foreach { userId =>
          if (game.playable && game.turnOf(player)) add(userId, game.id)
          else remove(userId, game.id)
        }
      }
    }
  }

  private def remindable(game: Game) =
    !game.hasAi && game.clock.fold(true)(chess.Speed.isUnlimited)

  private def add(userId: String, gameId: String) = coll.update(
    BSONDocument("_id" -> userId),
    BSONDocument("$addToSet" -> BSONDocument("g" -> gameId)),
    upsert = true)

  private def remove(userId: String, gameId: String) = coll.update(
    BSONDocument("_id" -> userId),
    BSONDocument("$pull" -> BSONDocument("g" -> gameId)))
}
