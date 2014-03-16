package lila.game

import lila.db.Types._
import lila.user.UserRepo
import reactivemongo.core.commands.Count

import reactivemongo.bson.{ BSONDocument, BSONArray }

private[game] final class Crosstable(coll: Coll) {

  import Crosstable._

  def apply(game: Game): Fu[Option[Summary]] = game.userIds.distinct match {
    case List(u1, u2) => apply(u1, u2)
    case _            => fuccess(none)
  }

  def apply(u1: String, u2: String): Fu[Option[Summary]] =
    coll.find(select(u1, u2)).one[BSONDocument].flatMap {
      case None      => create(u1, u2) flatMap toResults
      case Some(doc) => doc.getAs[List[String]]("d") ?? toResults
    } map { Summary(u1, u2, _).nonEmpty }

  def add(game: Game): Funit = game.userIds.distinct match {
    case List(u1, u2) if game.rated =>
      exists(u1, u2).flatMap {
        case false => create(u1, u2)
        case true  => funit
      } >> coll.update(
        select(u1, u2),
        BSONDocument("$push" -> BSONDocument("d" -> BSONDocument(
          "$each" -> List(game.id),
          "$slice" -> -maxGames
        )))).void
    case _ => funit
  }

  private def toResults(gameIds: List[String]): Fu[List[Result]] = tube.gameTube.coll.find(
    BSONDocument("_id" -> BSONDocument("$in" -> gameIds)),
    BSONDocument(Game.BSONFields.winnerId -> true)
  ).cursor[BSONDocument].collect[List](maxGames) map { docs =>
      docs.map { doc =>
        doc.getAs[String]("_id") map { id =>
          Result(id, doc.getAs[String](Game.BSONFields.winnerId))
        }
      }.flatten
    }

  private def exists(u1: String, u2: String) =
    coll.db command Count(coll.name, select(u1, u2).some) map (0 !=)

  private def create(x1: String, x2: String): Fu[List[String]] =
    UserRepo.orderByGameCount(x1, x2) flatMap {
      case Some((u1, u2)) =>
        GameRepo.recentOpponentGameIds(u1, u2, maxGames) flatMap { ids =>
          coll.insert(BSONDocument(
            "_id" -> makeKey(u1, u2),
            "d" -> ids
          )) inject ids
        }
      case _ => fuccess(Nil)
    }

  private def select(u1: String, u2: String) = BSONDocument("_id" -> makeKey(u1, u2))

  private def makeKey(u1: String, u2: String): String = makeKey(List(u1, u2))
  private def makeKey(us: List[String]): String = us.sorted mkString "/"
}

object Crosstable {

  val maxGames = 20

  case class Result(gameId: String, winnerId: Option[String])

  case class Summary(u1: String, u2: String, results: List[Result]) {

    def nonEmpty = results.nonEmpty option this

    def userIds = List(u1, u2)

    def score(u: String) = if (u == u1) score1 else score2

    private lazy val score1 = computeScore(u1)
    private lazy val score2 = computeScore(u2)

    // multiplied by ten
    private def computeScore(userId: String): Int = results.foldLeft(0) {
      case (s, Result(_, Some(w))) if w == userId => s + 10
      case (s, Result(_, None))                   => s + 5
      case (s, _)                                 => s
    }

    def winnerId =
      if (score1 > score2) Some(u1)
      else if (score1 < score2) Some(u2)
      else None

    def showScore(byTen: Int) = s"${byTen / 10}${(byTen % 10 != 0).??("½")}"
  }
}
