package lila.playban

import org.joda.time.DateTime
import reactivemongo.bson._
import reactivemongo.bson.Macros
import scala.concurrent.duration._

import chess.Color
import lila.db.BSON._
import lila.db.dsl._
import lila.game.{ Pov, Game, Player, Source }

final class PlaybanApi(
    coll: Coll,
    isRematch: String => Boolean) {

  import lila.db.BSON.BSONJodaDateTimeHandler
  import reactivemongo.bson.Macros
  private implicit val OutcomeBSONHandler = new BSONHandler[BSONInteger, Outcome] {
    def read(bsonInt: BSONInteger): Outcome = Outcome(bsonInt.value) err s"No such playban outcome: ${bsonInt.value}"
    def write(x: Outcome) = BSONInteger(x.id)
  }
  private implicit val banBSONHandler = Macros.handler[TempBan]
  private implicit val UserRecordBSONHandler = Macros.handler[UserRecord]

  private case class Blame(player: Player, outcome: Outcome)

  private def blameable(game: Game) =
    game.source.contains(Source.Lobby) &&
      game.hasClock &&
      !isRematch(game.id)

  def abort(pov: Pov): Funit = blameable(pov.game) ?? {
    if (pov.game olderThan 45) pov.game.playerWhoDidNotMove map { Blame(_, Outcome.NoPlay) }
    else if (pov.game olderThan 15) none
    else pov.player.some map { Blame(_, Outcome.Abort) }
  } ?? {
    case Blame(player, outcome) => player.userId.??(save(outcome))
  }

  def rageQuit(game: Game, quitterColor: Color): Funit = blameable(game) ?? {
    game.player(quitterColor).userId ?? save(Outcome.RageQuit)
  }

  def sittingOrGood(game: Game, sitterColor: Color): Funit = blameable(game) ?? {
    (for {
      userId <- game.player(sitterColor).userId
      lmt <- game.lastMoveTimeInSeconds
      seconds = nowSeconds - lmt
      clock <- game.clock
      // a tenth of the total time, at least 15s, at most 3 minutes
      limit = (clock.estimateTotalTime / 10) max 15 min (3 * 60)
      if seconds >= limit
    } yield save(Outcome.Sitting)(userId)) | goodFinish(game)
  }

  def goodFinish(game: Game): Funit = blameable(game) ?? {
    game.userIds.map(save(Outcome.Good)).sequenceFu.void
  }

  def currentBan(userId: String): Fu[Option[TempBan]] = coll.find(
    $doc("_id" -> userId, "b.0" $exists true),
    $doc("_id" -> false, "b" -> $doc("$slice" -> -1))
  ).uno[Bdoc].map {
      _.flatMap(_.getAs[List[TempBan]]("b")).??(_.find(_.inEffect))
    }

  def bans(userId: String): Fu[List[TempBan]] = coll.find(
    $doc("_id" -> userId, "b.0" $exists true),
    $doc("_id" -> false, "b" -> true)
  ).uno[Bdoc].map {
      ~_.flatMap(_.getAs[List[TempBan]]("b"))
    }

  def bans(userIds: List[String]): Fu[Map[String, Int]] = coll.find(
    $inIds(userIds),
    $doc("b" -> true)
  ).cursor[Bdoc]().gather[List]().map {
      _.flatMap { obj =>
        obj.getAs[String]("_id") flatMap { id =>
          obj.getAs[Barr]("b") map { id -> _.stream.size }
        }
      }.toMap
    }

  private def save(outcome: Outcome): String => Funit = userId => {
    coll.findAndUpdate(
      selector = $doc("_id" -> userId),
      update = $doc("$push" -> $doc(
        "o" -> $doc(
          "$each" -> List(outcome),
          "$slice" -> -20)
      )),
      fetchNewObject = true,
      upsert = true).map(_.value)
  } map2 UserRecordBSONHandler.read flatMap {
    case None         => fufail(s"can't find record for user $userId")
    case Some(record) => legiferate(record)
  } logFailure lila.log("playban")

  private def legiferate(record: UserRecord): Funit = record.newBan ?? { ban =>
    coll.update(
      $id(record.userId),
      $unset("o") ++
        $push(
          "b" -> $doc(
            "$each" -> List(ban),
            "$slice" -> -30)
        )
    ).void
  }
}
