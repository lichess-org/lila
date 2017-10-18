package lila.playban

import reactivemongo.bson._

import chess.{ Status, Color }
import lila.db.BSON._
import lila.db.dsl._
import lila.game.{ Pov, Game, Player, Source }
import lila.user.UserRepo

final class PlaybanApi(
    coll: Coll,
    sandbag: SandbagWatch,
    isRematch: String => Boolean
) {

  import lila.db.BSON.BSONJodaDateTimeHandler
  import reactivemongo.bson.Macros
  private implicit val OutcomeBSONHandler = new BSONHandler[BSONInteger, Outcome] {
    def read(bsonInt: BSONInteger): Outcome = Outcome(bsonInt.value) err s"No such playban outcome: ${bsonInt.value}"
    def write(x: Outcome) = BSONInteger(x.id)
  }
  private implicit val banBSONHandler = Macros.handler[TempBan]
  private implicit val UserRecordBSONHandler = Macros.handler[UserRecord]

  private case class Blame(player: Player, outcome: Outcome)

  private val blameableSources: Set[Source] = Set(Source.Lobby, Source.Pool, Source.Tournament)

  private def blameable(game: Game): Fu[Boolean] =
    (game.source.exists(s => blameableSources(s)) && game.hasClock && !isRematch(game.id)) ?? {
      if (game.rated) fuccess(true)
      else UserRepo.containsEngine(game.userIds) map (!_)
    }

  private def IfBlameable[A: ornicar.scalalib.Zero](game: Game)(f: => Fu[A]): Fu[A] =
    blameable(game) flatMap { _ ?? f }

  def abort(pov: Pov, isOnGame: Set[Color]): Funit = IfBlameable(pov.game) {
    {
      if (pov.game olderThan 30) pov.game.playerWhoDidNotMove map { Blame(_, Outcome.NoPlay) }
      else if (pov.game olderThan 15) none
      else if (isOnGame(pov.opponent.color)) pov.player.some map { Blame(_, Outcome.Abort) }
      else none
    } ?? {
      case Blame(player, outcome) => player.userId.??(save(outcome))
    }
  }

  def rageQuit(game: Game, quitterColor: Color): Funit =
    sandbag(game, quitterColor) >> IfBlameable(game) {
      game.player(quitterColor).userId ?? save(Outcome.RageQuit)
    }

  def flag(game: Game, flaggerColor: Color): Funit = {

    def unreasonableTime = game.clock map { c =>
      (c.estimateTotalSeconds / 8) atLeast 15 atMost (3 * 60)
    }

    // flagged after waiting a long time
    def sitting = for {
      userId <- game.player(flaggerColor).userId
      seconds = nowSeconds - game.movedAt.getSeconds
      limit <- unreasonableTime
      if seconds >= limit
    } yield save(Outcome.Sitting)(userId)

    // flagged after waiting a short time;
    // but the previous move used a long time.
    // assumes game was already checked for sitting
    def sitMoving = for {
      userId <- game.player(flaggerColor).userId
      movetimes <- game moveTimes flaggerColor
      lastMovetime <- movetimes.lastOption
      limit <- unreasonableTime
      if lastMovetime.toSeconds >= limit
    } yield save(Outcome.SitMoving)(userId)

    sandbag(game, flaggerColor) flatMap { isSandbag =>
      IfBlameable(game) {
        goodOrSandbag(game, !flaggerColor, false) >> { // winner gets a good game result
          sitting orElse
            sitMoving getOrElse
            goodOrSandbag(game, flaggerColor, isSandbag)
        }
      }
    }
  }

  def other(game: Game, status: Status.type => Status, winner: Option[Color]): Funit =
    winner.?? { w => sandbag(game, !w) } flatMap { isSandbag =>
      IfBlameable(game) {
        ((for {
          w <- winner
          loserId <- game.player(!w).userId
          if Status.NoStart is status
        } yield List(
          save(Outcome.NoPlay)(loserId),
          goodOrSandbag(game, w, false)
        )) | Color.all.map { c =>
          goodOrSandbag(game, c, winner.exists(c!=))
        }).sequenceFu.void
      }
    }

  private def goodOrSandbag(game: Game, color: Color, isSandbag: Boolean): Funit =
    game.player(color).userId ?? {
      save(if (isSandbag) Outcome.Sandbag else Outcome.Good)
    }

  def currentBan(userId: String): Fu[Option[TempBan]] = coll.find(
    $doc("_id" -> userId, "b.0" $exists true),
    $doc("_id" -> false, "b" -> $doc("$slice" -> -1))
  ).uno[Bdoc].map {
      _.flatMap(_.getAs[List[TempBan]]("b")).??(_.find(_.inEffect))
    }

  def hasCurrentBan(userId: String): Fu[Boolean] = currentBan(userId).map(_.isDefined)

  def completionRate(userId: String): Fu[Option[Double]] =
    coll.primitiveOne[List[Outcome]]($id(userId), "o").map(~_) map { outcomes =>
      outcomes.collect {
        case Outcome.RageQuit | Outcome.Sitting | Outcome.NoPlay => false
        case Outcome.Good => true
      } match {
        case c if c.size >= 5 => Some(c.count(identity).toDouble / c.size)
        case _ => none
      }
    }

  def bans(userId: String): Fu[List[TempBan]] =
    coll.primitiveOne[List[TempBan]]($doc("_id" -> userId, "b.0" $exists true), "b").map(~_)

  def bans(userIds: List[String]): Fu[Map[String, Int]] = coll.find(
    $inIds(userIds),
    $doc("b" -> true)
  ).cursor[Bdoc]().gather[List]().map {
      _.flatMap { obj =>
        obj.getAs[String]("_id") flatMap { id =>
          obj.getAs[Barr]("b") map { id -> _.stream.size }
        }
      }(scala.collection.breakOut)
    }

  private def save(outcome: Outcome): String => Funit = userId => {
    coll.findAndUpdate(
      selector = $id(userId),
      update = $doc("$push" -> $doc(
        "o" -> $doc(
          "$each" -> List(outcome),
          "$slice" -> -20
        )
      )),
      fetchNewObject = true,
      upsert = true
    ).map(_.value)
  } map2 UserRecordBSONHandler.read flatMap {
    case None => fufail(s"can't find record for user $userId")
    case Some(record) => legiferate(record)
  } logFailure lila.log("playban")

  private def legiferate(record: UserRecord): Funit = record.newBan ?? { ban =>
    coll.update(
      $id(record.userId),
      $unset("o") ++
        $push(
          "b" -> $doc(
            "$each" -> List(ban),
            "$slice" -> -30
          )
        )
    ).void
  }
}
