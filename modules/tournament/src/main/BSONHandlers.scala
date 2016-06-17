package lila.tournament

import chess.variant.Variant
import chess.{ Speed, Mode, StartingPosition }
import lila.db.BSON
import lila.db.BSON.BSONJodaDateTimeHandler
import lila.rating.PerfType
import reactivemongo.bson._

object BSONHandlers {

  private implicit val startingPositionBSONHandler = new BSONHandler[BSONString, StartingPosition] {
    def read(bsonStr: BSONString): StartingPosition = StartingPosition.byEco(bsonStr.value) err s"No such starting position: ${bsonStr.value}"
    def write(x: StartingPosition) = BSONString(x.eco)
  }

  private implicit val statusBSONHandler = new BSONHandler[BSONInteger, Status] {
    def read(bsonInt: BSONInteger): Status = Status(bsonInt.value) err s"No such status: ${bsonInt.value}"
    def write(x: Status) = BSONInteger(x.id)
  }

  private implicit val tournamentClockBSONHandler = Macros.handler[TournamentClock]

  private implicit val spotlightBSONHandler = Macros.handler[Spotlight]

  private implicit val leaderboardRatio = new BSONHandler[BSONInteger, LeaderboardApi.Ratio] {
    def read(b: BSONInteger) = LeaderboardApi.Ratio(b.value.toDouble / 100000)
    def write(x: LeaderboardApi.Ratio) = BSONInteger((x.value * 100000).toInt)
  }

  import Condition.BSONHandlers.AllBSONHandler

  implicit val tournamentHandler = new BSON[Tournament] {
    def reads(r: BSON.Reader) = {
      val variant = r.intO("variant").fold[Variant](Variant.default)(Variant.orDefault)
      val position = r.strO("eco").flatMap(StartingPosition.byEco) | StartingPosition.initial
      val startsAt = r date "startsAt"
      Tournament(
        id = r str "_id",
        name = r str "name",
        status = r.get[Status]("status"),
        system = r.intO("system").fold[System](System.default)(System.orDefault),
        clock = r.get[TournamentClock]("clock"),
        minutes = r int "minutes",
        variant = variant,
        position = position,
        mode = r.intO("mode") flatMap Mode.apply getOrElse Mode.Rated,
        `private` = r boolD "private",
        conditions = r.getO[Condition.All]("conditions") getOrElse Condition.All.empty,
        schedule = for {
          doc <- r.getO[BSONDocument]("schedule")
          freq <- doc.getAs[String]("freq") flatMap Schedule.Freq.apply
          speed <- doc.getAs[String]("speed") flatMap Schedule.Speed.apply
        } yield Schedule(freq, speed, variant, position, startsAt),
        nbPlayers = r int "nbPlayers",
        createdAt = r date "createdAt",
        createdBy = r str "createdBy",
        startsAt = startsAt,
        winnerId = r strO "winner",
        featuredId = r strO "featured",
        spotlight = r.getO[Spotlight]("spotlight"))
    }
    def writes(w: BSON.Writer, o: Tournament) = BSONDocument(
      "_id" -> o.id,
      "name" -> o.name,
      "status" -> o.status,
      "system" -> o.system.some.filterNot(_.default).map(_.id),
      "clock" -> o.clock,
      "minutes" -> o.minutes,
      "variant" -> o.variant.some.filterNot(_.standard).map(_.id),
      "eco" -> o.position.some.filterNot(_.initial).map(_.eco),
      "mode" -> o.mode.some.filterNot(_.rated).map(_.id),
      "private" -> w.boolO(o.`private`),
      "conditions" -> o.conditions.ifNonEmpty,
      "schedule" -> o.schedule.map { s =>
        BSONDocument(
          "freq" -> s.freq.name,
          "speed" -> s.speed.name)
      },
      "nbPlayers" -> o.nbPlayers,
      "createdAt" -> w.date(o.createdAt),
      "createdBy" -> w.str(o.createdBy),
      "startsAt" -> w.date(o.startsAt),
      "winner" -> o.winnerId,
      "featured" -> o.featuredId,
      "spotlight" -> o.spotlight)
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
      ratingDiff = r intD "p",
      magicScore = r int "m",
      fire = r boolD "f",
      performance = r intO "e")
    def writes(w: BSON.Writer, o: Player) = BSONDocument(
      "_id" -> o._id,
      "tid" -> o.tourId,
      "uid" -> o.userId,
      "r" -> o.rating,
      "pr" -> w.boolO(o.provisional),
      "w" -> w.boolO(o.withdraw),
      "s" -> w.intO(o.score),
      "p" -> w.intO(o.ratingDiff),
      "m" -> o.magicScore,
      "f" -> w.boolO(o.fire),
      "e" -> o.performance)
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
        berserk1 = r intD "b1",
        berserk2 = r intD "b2")
    }
    def writes(w: BSON.Writer, o: Pairing) = BSONDocument(
      "_id" -> o.id,
      "tid" -> o.tourId,
      "s" -> o.status.id,
      "u" -> BSONArray(o.user1, o.user2),
      "w" -> o.winner.map(o.user1 ==),
      "t" -> o.turns,
      "b1" -> w.intO(o.berserk1),
      "b2" -> w.intO(o.berserk2))
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
      date = r date "d")

    def writes(w: BSON.Writer, o: LeaderboardApi.Entry) = BSONDocument(
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
      "d" -> w.date(o.date))
  }

  import LeaderboardApi.ChartData.AggregationResult
  implicit val leaderboardAggregationResultBSONHandler =
    BSON.LoggingHandler(logger)(Macros.handler[AggregationResult])
}
