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

  private implicit val scheduleHandler = new BSON[Schedule] {
    def reads(r: BSON.Reader) = Schedule(
      freq = Schedule.Freq(r str "freq") err "tournament freq",
      speed = Schedule.Speed(r str "speed") err "tournament freq",
      variant = Variant.orDefault(r intD "variant"),
      at = r date "at")
    def writes(w: BSON.Writer, o: Schedule) = BSONDocument(
      "freq" -> o.freq.name,
      "speed" -> o.speed.name,
      "variant" -> o.variant.id,
      "at" -> w.date(o.at))
  }

  private def readSystem(r: BSON.Reader) =
    r.intO("system").fold[System](System.default)(System.orDefault)

  implicit val tournamentHandler = new BSON[Tournament] {
    def reads(r: BSON.Reader) = Tournament(
      id = r str "_id",
      name = r str "name",
      status = r.get[Status]("status"),
      system = readSystem(r),
      clock = r.get[TournamentClock]("clock"),
      minutes = r int "minutes",
      variant = r.intO("variant").fold[chess.variant.Variant](chess.variant.Variant.default)(chess.variant.Variant.orDefault),
      position = r.strO("eco").flatMap(StartingPosition.byEco) | StartingPosition.initial,
      mode = r.intO("mode").fold[Mode](Mode.default)(Mode.orDefault),
      `private` = r boolD "private",
      schedule = r.getO[Schedule]("schedule"),
      createdAt = r date "createdAt",
      createdBy = r str "createdBy",
      startsAt = r date "startsAt")
    def writes(w: BSON.Writer, o: Tournament) = BSONDocument(
      "_id" -> o.id,
      "name" -> o.name,
      "status" -> o.status,
      "system" -> o.system.id,
      "clock" -> o.clock,
      "minutes" -> o.minutes,
      "variant" -> o.variant.id,
      "eco" -> o.position.eco,
      "mode" -> o.mode.id,
      "private" -> w.boolO(o.`private`),
      "schedule" -> o.schedule,
      "createdAt" -> w.date(o.createdAt),
      "createdBy" -> w.str(o.createdBy),
      "startsAt" -> w.date(o.startsAt))
  }

  implicit val playerBSONHandler = new BSON[Player] {
    def reads(r: BSON.Reader) = Player(
      id = r str "_id",
      tourId = r str "tid",
      userId = r str "uid",
      rating = r int "r",
      provisional = r boolD "pr",
      withdraw = r boolD "w",
      score = r int "s",
      perf = r int "p",
      magicScore = r int "m")
    def writes(w: BSON.Writer, o: Player) = BSONDocument(
      "_id" -> o.id,
      "tid" -> o.tourId,
      "uid" -> o.userId,
      "r" -> o.rating,
      "pr" -> w.boolO(o.provisional),
      "w" -> w.boolO(o.withdraw),
      "s" -> o.score,
      "p" -> o.perf,
      "m" -> o.magicScore)
  }

  implicit val pairingHandler = new BSON[Pairing] {
    def reads(r: BSON.Reader) = {
      val users = r strsD "u"
      Pairing(
        id = r str "_id",
        tourId = r str "tid",
        status = chess.Status(r int "s") err "tournament pairing status",
        user1 = users.headOption err "tournament pairing first user",
        user2 = users lift 1 err "tournament pairing second user",
        winner = r strO "w",
        turns = r intO "t",
        date = r date "d",
        berserk1 = r intD "b1",
        berserk2 = r intD "b2",
        perf1 = r intD "p1",
        perf2 = r intD "p2")
    }
    def writes(w: BSON.Writer, o: Pairing) = BSONDocument(
      "_id" -> o.id,
      "tid" -> o.tourId,
      "s" -> o.status.id,
      "u" -> BSONArray(o.user1, o.user2),
      "w" -> o.winner,
      "t" -> o.turns,
      "d" -> w.date(o.date),
      "b1" -> w.intO(o.berserk1),
      "b2" -> w.intO(o.berserk2),
      "p1" -> w.intO(o.perf1),
      "p2" -> w.intO(o.perf2))
  }

  private implicit val eventHandler = new BSON[Event] {
    def reads(r: BSON.Reader): Event = r int "i" match {
      case 1  => RoundEnd(timestamp = r date "t")
      case 10 => Bye(user = r str "u", timestamp = r date "t")
      case x  => sys error s"tournament event id $x"
    }
    def writes(w: BSON.Writer, o: Event) = o match {
      case RoundEnd(timestamp)  => BSONDocument("i" -> o.id, "t" -> w.date(timestamp))
      case Bye(user, timestamp) => BSONDocument("i" -> o.id, "u" -> user, "t" -> w.date(timestamp))
    }
  }
}
