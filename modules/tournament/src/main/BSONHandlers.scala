package lila.tournament

import chess.Clock.{ Config => ClockConfig }
import chess.variant.Variant
import chess.{ Mode, StartingPosition }
import lila.db.BSON
import lila.db.dsl._
import lila.rating.PerfType
import lila.user.User.lichessId
import reactivemongo.bson._

object BSONHandlers {

  private[tournament] implicit val statusBSONHandler: BSONHandler[BSONInteger, Status] = new BSONHandler[BSONInteger, Status] {
    def read(bsonInt: BSONInteger): Status = Status(bsonInt.value) err s"No such status: ${bsonInt.value}"
    def write(x: Status) = BSONInteger(x.id)
  }

  private[tournament] implicit val scheduleFreqHandler = new BSONHandler[BSONString, Schedule.Freq] {
    def read(bsonStr: BSONString) = Schedule.Freq(bsonStr.value) err s"No such freq: ${bsonStr.value}"
    def write(x: Schedule.Freq) = BSONString(x.name)
  }
  private implicit val scheduleSpeedHandler = new BSONHandler[BSONString, Schedule.Speed] {
    def read(bsonStr: BSONString) = Schedule.Speed(bsonStr.value) err s"No such speed: ${bsonStr.value}"
    def write(x: Schedule.Speed) = BSONString(x.name)
  }

  private implicit val tournamentClockBSONHandler = new BSONHandler[BSONDocument, ClockConfig] {
    def read(doc: BSONDocument) = ClockConfig(
      doc.getAs[Int]("limit").get,
      doc.getAs[Int]("increment").get
    )

    def write(config: ClockConfig) = BSONDocument(
      "limit" -> config.limitSeconds,
      "increment" -> config.incrementSeconds
    )
  }

  private implicit val spotlightBSONHandler = Macros.handler[Spotlight]

  private implicit val leaderboardRatio = new BSONHandler[BSONInteger, LeaderboardApi.Ratio] {
    def read(b: BSONInteger) = LeaderboardApi.Ratio(b.value.toDouble / 100000)
    def write(x: LeaderboardApi.Ratio) = BSONInteger((x.value * 100000).toInt)
  }

  import Condition.BSONHandlers.AllBSONHandler

  implicit val tournamentHandler = new BSON[Tournament] {
    def reads(r: BSON.Reader) = {
      val variant = r.intO("variant").fold[Variant](Variant.default)(Variant.orDefault)
      val position: StartingPosition = r.strO("fen").flatMap(Thematic.byFen) orElse
        r.strO("eco").flatMap(Thematic.byEco) getOrElse // for BC
        StartingPosition.initial
      val startsAt = r date "startsAt"
      val conditions = r.getO[Condition.All]("conditions") getOrElse Condition.All.empty
      Tournament(
        id = r str "_id",
        name = r str "name",
        status = r.get[Status]("status"),
        system = r.intO("system").fold[System](System.default)(System.orDefault),
        clock = r.get[chess.Clock.Config]("clock"),
        minutes = r int "minutes",
        variant = variant,
        position = position,
        mode = r.intO("mode") flatMap Mode.apply getOrElse Mode.Rated,
        `private` = r boolD "private",
        password = r.strO("password"),
        conditions = conditions,
        noBerserk = r boolD "noBerserk",
        schedule = for {
          doc <- r.getO[Bdoc]("schedule")
          freq <- doc.getAs[Schedule.Freq]("freq")
          speed <- doc.getAs[Schedule.Speed]("speed")
        } yield Schedule(freq, speed, variant, position, startsAt, conditions),
        nbPlayers = r int "nbPlayers",
        createdAt = r date "createdAt",
        createdBy = r strO "createdBy" getOrElse lichessId,
        startsAt = startsAt,
        winnerId = r strO "winner",
        featuredId = r strO "featured",
        spotlight = r.getO[Spotlight]("spotlight")
      )
    }
    def writes(w: BSON.Writer, o: Tournament) = $doc(
      "_id" -> o.id,
      "name" -> o.name,
      "status" -> o.status,
      "system" -> o.system.some.filterNot(_.default).map(_.id),
      "clock" -> o.clock,
      "minutes" -> o.minutes,
      "variant" -> o.variant.some.filterNot(_.standard).map(_.id),
      "fen" -> o.position.some.filterNot(_.initial).map(_.fen),
      "mode" -> o.mode.some.filterNot(_.rated).map(_.id),
      "private" -> w.boolO(o.`private`),
      "password" -> o.password,
      "conditions" -> o.conditions.ifNonEmpty,
      "noBerserk" -> w.boolO(o.noBerserk),
      "schedule" -> o.schedule.map { s =>
        $doc(
          "freq" -> s.freq,
          "speed" -> s.speed
        )
      },
      "nbPlayers" -> o.nbPlayers,
      "createdAt" -> w.date(o.createdAt),
      "createdBy" -> o.nonLichessCreatedBy,
      "startsAt" -> w.date(o.startsAt),
      "winner" -> o.winnerId,
      "featured" -> o.featuredId,
      "spotlight" -> o.spotlight
    )
  }

  implicit val playerBSONHandler = new BSON[Player] {
    def reads(r: BSON.Reader) = Player(
      _id = r str "_id",
      tourId = r str "tid",
      userId = r str "uid",
      rating = r int "r",
      provisional = r boolD "pr",
      withdraw = r boolD "w",
      score = r intD "s",
      fire = r boolD "f",
      performance = r intD "e"
    )
    def writes(w: BSON.Writer, o: Player) = $doc(
      "_id" -> o._id,
      "tid" -> o.tourId,
      "uid" -> o.userId,
      "r" -> o.rating,
      "pr" -> w.boolO(o.provisional),
      "w" -> w.boolO(o.withdraw),
      "s" -> w.intO(o.score),
      "m" -> o.magicScore,
      "f" -> w.boolO(o.fire),
      "e" -> o.performance
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
        status = chess.Status(r int "s") err "tournament pairing status",
        user1 = user1,
        user2 = user2,
        winner = r boolO "w" map (_.fold(user1, user2)),
        turns = r intO "t",
        berserk1 = r.intO("b1").fold(r.boolD("b1"))(1 ==), // it used to be int = 0/1
        berserk2 = r.intO("b2").fold(r.boolD("b2"))(1 ==)
      )
    }
    def writes(w: BSON.Writer, o: Pairing) = $doc(
      "_id" -> o.id,
      "tid" -> o.tourId,
      "s" -> o.status.id,
      "u" -> BSONArray(o.user1, o.user2),
      "w" -> o.winner.map(o.user1 ==),
      "t" -> o.turns,
      "b1" -> w.boolO(o.berserk1),
      "b2" -> w.boolO(o.berserk2)
    )
  }

  implicit val leaderboardEntryHandler = new BSON[LeaderboardApi.Entry] {
    def reads(r: BSON.Reader) = LeaderboardApi.Entry(
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

    def writes(w: BSON.Writer, o: LeaderboardApi.Entry) = $doc(
      "_id" -> o.id,
      "u" -> o.userId,
      "t" -> o.tourId,
      "g" -> o.nbGames,
      "s" -> o.score,
      "r" -> o.rank,
      "w" -> o.rankRatio,
      "f" -> o.freq.map(_.id),
      "p" -> o.speed.map(_.id),
      "v" -> o.perf.id,
      "d" -> w.date(o.date)
    )
  }

  import LeaderboardApi.ChartData.AggregationResult
  implicit val leaderboardAggregationResultBSONHandler =
    BSON.LoggingHandler(logger)(Macros.handler[AggregationResult])
}
