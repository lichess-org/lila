package lila.tournament

import org.joda.time.DateTime
import reactivemongo.api.bson._

import shogi.format.forsyth.Sfen
import shogi.Mode
import shogi.variant.Variant

import lila.db.BSON
import lila.db.dsl._
import lila.rating.PerfType
import lila.user.User.lishogiId

object BSONHandlers {

  implicit private[tournament] val formatBSONHandler = tryHandler[Format](
    { case BSONString(v) => Format.byKey(v) toTry s"No such format: $v" },
    x => BSONString(x.key)
  )

  implicit private[tournament] val statusBSONHandler = tryHandler[Status](
    { case BSONInteger(v) => Status(v) toTry s"No such status: $v" },
    x => BSONInteger(x.id)
  )

  implicit private[tournament] val pointsBSONHandler = {
    val intReader = collectionReader[List, Int]
    tryHandler[Arrangement.Points](
      { case arr: BSONArray =>
        intReader.readTry(arr).filter(_.length == 3) map { p =>
          Arrangement.Points(p(0), p(1), p(2))
        }
      },
      points => BSONArray(points.lose, points.draw, points.win)
    )
  }

  implicit private[tournament] val scheduleFreqHandler = tryHandler[Schedule.Freq](
    { case BSONString(v) => Schedule.Freq(v) toTry s"No such freq: $v" },
    x => BSONString(x.name)
  )

  implicit private[tournament] val scheduleSpeedHandler = tryHandler[Schedule.Speed](
    { case BSONString(v) => Schedule.Speed(v) toTry s"No such speed: $v" },
    x => BSONString(x.key)
  )

  implicit val timeControlBSONHandler = tryHandler[TimeControl](
    { case doc: BSONDocument =>
      doc.getAsOpt[Int]("days") match {
        case Some(d) =>
          scala.util.Success(TimeControl.Correspondence(d))
        case None =>
          for {
            limit <- doc.getAsTry[Int]("limit")
            inc   <- doc.getAsTry[Int]("increment")
            byo   <- doc.getAsTry[Int]("byoyomi")
            per   <- doc.getAsTry[Int]("periods")
          } yield TimeControl.RealTime(shogi.Clock.Config(limit, inc, byo, per))
      }
    },
    {
      case TimeControl.RealTime(c) =>
        BSONDocument(
          "limit"     -> c.limitSeconds,
          "increment" -> c.incrementSeconds,
          "byoyomi"   -> c.byoyomiSeconds,
          "periods"   -> c.periodsTotal
        )
      case TimeControl.Correspondence(d) =>
        BSONDocument("days" -> d)
    }
  )

  implicit private val spotlightBSONHandler = Macros.handler[Spotlight]

  implicit val battleBSONHandler = Macros.handler[TeamBattle]

  implicit private val leaderboardRatio =
    BSONIntegerHandler.as[LeaderboardApi.Ratio](
      i => LeaderboardApi.Ratio(i.toDouble / 100_000),
      r => (r.value * 100_000).toInt
    )

  import Condition.BSONHandlers.AllBSONHandler

  implicit val tournamentHandler = new BSON[Tournament] {
    def reads(r: BSON.Reader) = {
      val format                 = r.getO[Format]("format").getOrElse(Format.Arena)
      val variant                = r.intO("variant").fold[Variant](Variant.default)(Variant.orDefault)
      val position: Option[Sfen] = r.getO[Sfen]("sfen").filterNot(_.initialOf(variant))
      val startsAt               = r date "startsAt"
      val conditions             = r.getO[Condition.All]("conditions") getOrElse Condition.All.empty
      Tournament(
        id = r str "_id",
        name = r str "name",
        format = format,
        status = r.get[Status]("status"),
        timeControl = r.get[TimeControl]("clock"),
        minutes = r int "minutes",
        variant = variant,
        position = position,
        mode = r.intO("mode") flatMap Mode.apply getOrElse Mode.Rated,
        password = r.strO("password"),
        candidates = r strsD "candidates",
        conditions = conditions,
        closed = r boolD "closed",
        denied = r strsD "denied",
        teamBattle = r.getO[TeamBattle]("teamBattle"),
        candidatesOnly = r boolD "candidatesOnly",
        noBerserk = r boolD "noBerserk",
        noStreak = r boolD "noStreak",
        schedule = for {
          doc   <- r.getO[Bdoc]("schedule")
          freq  <- doc.getAsOpt[Schedule.Freq]("freq")
          speed <- doc.getAsOpt[Schedule.Speed]("speed")
        } yield Schedule(format, freq, speed, variant, position, startsAt, conditions),
        nbPlayers = r int "nbPlayers",
        createdAt = r date "createdAt",
        createdBy = r strO "createdBy" getOrElse lishogiId,
        startsAt = startsAt,
        winnerId = r strO "winner",
        featuredId = r strO "featured",
        spotlight = r.getO[Spotlight]("spotlight"),
        description = r strO "description",
        hasChat = r boolO "chat" getOrElse true
      )
    }
    def writes(w: BSON.Writer, o: Tournament) =
      $doc(
        "_id"            -> o.id,
        "name"           -> o.name,
        "format"         -> o.format.some.filterNot(_ == Format.Arena),
        "status"         -> o.status,
        "clock"          -> o.timeControl,
        "minutes"        -> o.minutes,
        "endsAt"         -> (o.hasArrangements option o.finishesAt),
        "variant"        -> o.variant.some.filterNot(_.standard).map(_.id),
        "sfen"           -> o.position.map(_.value),
        "mode"           -> o.mode.some.filterNot(_.rated).map(_.id),
        "password"       -> o.password,
        "candidates"     -> w.strListO(o.candidates),
        "conditions"     -> o.conditions.ifNonEmpty,
        "closed"         -> w.boolO(o.closed),
        "denied"         -> w.strListO(o.denied),
        "teamBattle"     -> o.teamBattle,
        "candidatesOnly" -> o.candidatesOnly,
        "noBerserk"      -> w.boolO(o.noBerserk),
        "noStreak"       -> w.boolO(o.noStreak),
        "schedule" -> o.schedule.map { s =>
          $doc(
            "freq"  -> s.freq,
            "speed" -> s.speed
          )
        },
        "nbPlayers"   -> o.nbPlayers,
        "createdAt"   -> w.date(o.createdAt),
        "createdBy"   -> o.nonLishogiCreatedBy,
        "startsAt"    -> w.date(o.startsAt),
        "winner"      -> o.winnerId,
        "featured"    -> o.featuredId,
        "spotlight"   -> o.spotlight,
        "description" -> o.description,
        "chat"        -> (!o.hasChat).option(false)
      )
  }

  implicit val playerBSONHandler = new BSON[Player] {
    def reads(r: BSON.Reader) =
      Player(
        _id = r str "_id",
        tourId = r str "tid",
        userId = r str "uid",
        order = r intO "o",
        rating = r int "r",
        provisional = r boolD "pr",
        withdraw = r boolD "w",
        score = r intD "s",
        fire = r boolD "f",
        performance = r intD "e",
        team = r strO "t"
      )
    def writes(w: BSON.Writer, o: Player) =
      $doc(
        "_id" -> o._id,
        "tid" -> o.tourId,
        "uid" -> o.userId,
        "o"   -> o.order,
        "r"   -> o.rating,
        "pr"  -> w.boolO(o.provisional),
        "w"   -> w.boolO(o.withdraw),
        "s"   -> w.intO(o.score),
        "m"   -> o.magicScore,
        "f"   -> w.boolO(o.fire),
        "e"   -> o.performance,
        "t"   -> o.team
      )
  }

  implicit val pairingHandler = new BSON[Pairing] {
    def reads(r: BSON.Reader) = {
      val users = r strsD "u"
      val user1 = users.headOption err "tournament pairing first user"
      val user2 = users lift 1 err "tournament pairing second user"
      Pairing(
        id = r str "_id",
        tourId = r str "tid",
        status = shogi.Status(r int "s") err "tournament pairing status",
        user1 = user1,
        user2 = user2,
        winner = r boolO "w" map {
          case true => user1
          case _    => user2
        },
        plies = r intO "t",
        berserk1 = r.intO("b1").fold(r.boolD("b1"))(1 ==), // it used to be int = 0/1
        berserk2 = r.intO("b2").fold(r.boolD("b2"))(1 ==)
      )
    }
    def writes(w: BSON.Writer, o: Pairing) =
      $doc(
        "_id" -> o.id,
        "tid" -> o.tourId,
        "s"   -> o.status.id,
        "u"   -> BSONArray(o.user1, o.user2),
        "w"   -> o.winner.map(o.user1 ==),
        "t"   -> o.plies,
        "b1"  -> w.boolO(o.berserk1),
        "b2"  -> w.boolO(o.berserk2)
      )
  }

  implicit val arrangementHandler = new BSON[Arrangement] {
    def reads(r: BSON.Reader) = {
      val users   = r strsD "u"
      val user1Id = users.headOption err "tournament arrangement first user"
      val user2Id = users lift 1 err "tournament arrangement second user"
      Arrangement(
        id = r str "_id",
        order = r intD "o",
        tourId = r str "t",
        user1 = Arrangement.User(
          id = user1Id,
          readyAt = r dateO "r1",
          scheduledAt = r dateO "d1"
        ),
        user2 = Arrangement.User(
          id = user2Id,
          readyAt = r dateO "r2",
          scheduledAt = r dateO "d2"
        ),
        name = r strO "n",
        color = r.getO[shogi.Color]("c"),
        points = r.getO[Arrangement.Points]("pt"),
        gameId = r strO "g",
        status = r.intO("s") flatMap shogi.Status.apply,
        winner = r boolO "w" map {
          case true => user1Id
          case _    => user2Id
        },
        plies = r intO "p",
        scheduledAt = r dateO "d",
        history = Arrangement.History(r strsD "h")
      )
    }
    def writes(w: BSON.Writer, o: Arrangement) =
      $doc(
        "_id" -> o.id,
        "o"   -> o.order.some.filter(_ > 0),
        "t"   -> o.tourId,
        "u"   -> BSONArray(o.user1.id, o.user2.id),
        "r1"  -> o.user1.readyAt,
        "r2"  -> o.user2.readyAt,
        "d1"  -> o.user1.scheduledAt,
        "d2"  -> o.user2.scheduledAt,
        "n"   -> o.name,
        "c"   -> o.color,
        "pt"  -> o.points.filterNot(_ == Arrangement.Points.default),
        "g"   -> o.gameId,
        "s"   -> o.status.map(_.id),
        "w"   -> o.winner.map(o.user1 ==),
        "p"   -> o.plies,
        "d"   -> o.scheduledAt,
        "h"   -> o.history.list,
        "ua"  -> o.gameId.isEmpty ?? DateTime.now.some // updated at
      )
  }

  implicit val leaderboardEntryHandler = new BSON[LeaderboardApi.Entry] {
    def reads(r: BSON.Reader) =
      LeaderboardApi.Entry(
        id = r str "_id",
        userId = r str "u",
        tourId = r str "t",
        nbGames = r int "g",
        score = r int "s",
        rank = r int "r",
        rankRatio = r.get[LeaderboardApi.Ratio]("w"),
        freq = r intO "f" flatMap Schedule.Freq.byId,
        speed = r intO "p" flatMap Schedule.Speed.byId,
        perf = PerfType.byId get r.int("v") err "Invalid leaderboard perf",
        date = r date "d"
      )

    def writes(w: BSON.Writer, o: LeaderboardApi.Entry) =
      $doc(
        "_id" -> o.id,
        "u"   -> o.userId,
        "t"   -> o.tourId,
        "g"   -> o.nbGames,
        "s"   -> o.score,
        "r"   -> o.rank,
        "w"   -> o.rankRatio,
        "f"   -> o.freq.map(_.id),
        "p"   -> o.speed.map(_.id),
        "v"   -> o.perf.id,
        "d"   -> w.date(o.date)
      )
  }

  import LeaderboardApi.ChartData.AggregationResult
  implicit val leaderboardAggregationResultBSONHandler = Macros.handler[AggregationResult]
}
