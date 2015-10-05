package lila.tournament

import chess.variant.Variant
import chess.{ Speed, Mode, StartingPosition }
import lila.db.BSON
import reactivemongo.bson._

object BSONHandlers {

  private implicit val StartingPositionBSONHandler = new BSONHandler[BSONString, StartingPosition] {
    def read(bsonStr: BSONString): StartingPosition = StartingPosition.byEco(bsonStr.value) err s"No such starting position: ${bsonStr.value}"
    def write(x: StartingPosition) = BSONString(x.eco)
  }

  private implicit val StatusBSONHandler = new BSONHandler[BSONInteger, Status] {
    def read(bsonInt: BSONInteger): Status = Status(bsonInt.value) err s"No such status: ${bsonInt.value}"
    def write(x: Status) = BSONInteger(x.id)
  }

  private implicit val tournamentClockBSONHandler = Macros.handler[TournamentClock]

  implicit val tournamentHandler = new BSON[Tournament] {
    def reads(r: BSON.Reader) = {
      val variant = r.intO("variant").fold[chess.variant.Variant](chess.variant.Variant.default)(chess.variant.Variant.orDefault)
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
        schedule = for {
          doc <- r.getO[BSONDocument]("schedule")
          freq <- doc.getAs[String]("freq") flatMap Schedule.Freq.apply
          speed <- doc.getAs[String]("speed") flatMap Schedule.Speed.apply
        } yield Schedule(freq, speed, variant, position, startsAt),
        nbPlayers = r int "nbPlayers",
        createdAt = r date "createdAt",
        createdBy = r str "createdBy",
        startsAt = startsAt,
        pairsAt = r dateO "pairsAt",
        winnerId = r strO "winner")
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
      "schedule" -> o.schedule.map { s =>
        BSONDocument(
          "freq" -> s.freq.name,
          "speed" -> s.speed.name)
      },
      "nbPlayers" -> o.nbPlayers,
      "createdAt" -> w.date(o.createdAt),
      "createdBy" -> w.str(o.createdBy),
      "startsAt" -> w.date(o.startsAt),
      "pairsAt" -> o.pairsAt.map(w.date),
      "winner" -> o.winnerId)
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
      perf = r intD "p",
      magicScore = r int "m",
      fire = r boolD "f")
    def writes(w: BSON.Writer, o: Player) = BSONDocument(
      "_id" -> o._id,
      "tid" -> o.tourId,
      "uid" -> o.userId,
      "r" -> o.rating,
      "pr" -> w.boolO(o.provisional),
      "w" -> w.boolO(o.withdraw),
      "s" -> w.intO(o.score),
      "p" -> w.intO(o.perf),
      "m" -> o.magicScore,
      "f" -> w.boolO(o.fire))
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
}
