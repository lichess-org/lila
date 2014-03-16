package lila.game

import lila.db.Types._
import lila.user.{ User, UserRepo }
import reactivemongo.core.commands.Count

import reactivemongo.bson.{ BSONDocument, BSONArray }

private[game] final class Crosstable(coll: Coll) {

  def apply(u1: User, u2: User): Fu[List[Game]] =
    coll.find(select(u1.id, u2.id)).one[BSONDocument] flatMap {
      case None      => create(u1, u2) flatMap GameRepo.games
      case Some(doc) => doc.getAs[List[String]]("d") ?? GameRepo.games
    }

  def add(game: Game): Funit = (game.rated && game.userIds.distinct.size == 2) ?? {
    exists(game.userIds).flatMap {
      case false => UserRepo byIds game.userIds flatMap {
        case u1 :: u2 :: Nil => create(u1, u2)
        case _               => funit
      }
      case true => funit
    } >> coll.update(
      select(game.userIds),
      BSONDocument("$push" -> BSONDocument("d" -> BSONDocument(
        "$each" -> List(game.id),
        "$slice" -> -Crosstable.maxGames
      )))).void
  }

  private def exists(us: List[String]) =
    coll.db command Count(coll.name, select(us).some) map (0 !=)

  private def create(u1: User, u2: User): Fu[List[String]] =
    GameRepo.recentOpponentGameIds(u1, u2, Crosstable.maxGames) flatMap { ids =>
      coll.insert(BSONDocument(
        "_id" -> makeKey(u1.id, u2.id),
        "d" -> ids
      )) inject ids
    }

  private def select(u1: String, u2: String) = BSONDocument("_id" -> makeKey(u1, u2))
  private def select(us: List[String]) = BSONDocument("_id" -> makeKey(us))

  private def makeKey(u1: String, u2: String): String = makeKey(List(u1, u2))
  private def makeKey(us: List[String]): String = us.sorted mkString "/"
}

private object Crosstable {

  val maxGames = 20

  case class Data(games: List[Game])
}
