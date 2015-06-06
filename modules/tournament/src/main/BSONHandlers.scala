package lila.tournament

import chess.variant.Variant
import chess.{ Speed, Mode }
import lila.db.BSON
import reactivemongo.bson._

object BSONHandlers {

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

  private implicit val dataHandler = new BSON[Data] {
    def reads(r: BSON.Reader) = Data(
      name = r str "name",
      system = r.intO("system").fold[System](System.default)(System.orDefault),
      clock = r.get[TournamentClock]("clock"),
      minutes = r int "minutes",
      variant = r.intO("variant").fold[chess.variant.Variant](chess.variant.Variant.default)(chess.variant.Variant.orDefault),
      mode = r.intO("mode").fold[Mode](Mode.default)(Mode.orDefault),
      `private` = r boolD "private",
      schedule = r.getO[Schedule]("schedule"),
      createdAt = r date "createdAt",
      createdBy = r str "createdBy")
    def writes(w: BSON.Writer, o: Data) = BSONDocument(
      "name" -> o.name,
      "system" -> o.system.id,
      "clock" -> o.clock,
      "minutes" -> o.minutes,
      "variant" -> o.variant.id,
      "mode" -> o.mode.id,
      "private" -> w.boolO(o.`private`),
      "schedule" -> o.schedule,
      "createdAt" -> w.date(o.createdAt),
      "createdBy" -> w.str(o.createdBy))
  }

  private implicit val playerBSONHandler = new BSON[Player] {
    def reads(r: BSON.Reader) = Player(
      id = r str "id",
      rating = r int "rating",
      provisional = r boolD "prov",
      withdraw = r boolD "withdraw",
      score = r int "score",
      perf = r intD "perf")
    def writes(w: BSON.Writer, o: Player) = BSONDocument(
      "id" -> o.id,
      "rating" -> o.rating,
      "prov" -> w.boolO(o.provisional),
      "withdraw" -> w.boolO(o.withdraw),
      "score" -> o.score,
      "perf" -> o.perf)
  }

  private implicit val pairingHandler = new BSON[Pairing] {
    def reads(r: BSON.Reader) = {
      val users = r strsD "u"
      Pairing(
        gameId = r str "g",
        status = chess.Status(r int "s") err "tournament pairing status",
        user1 = users.headOption err "tournament pairing first user",
        user2 = users.lift(1) err "tournament pairing second user",
        winner = r strO "w",
        turns = r intO "t",
        pairedAt = r dateO "p",
        berserk1 = r intD "b1",
        berserk2 = r intD "b2",
        perf1 = r intD "p1",
        perf2 = r intD "p2")
    }
    def writes(w: BSON.Writer, o: Pairing) = BSONDocument(
      "g" -> o.gameId,
      "s" -> o.status.id,
      "u" -> BSONArray(o.user1, o.user2),
      "w" -> o.winner,
      "t" -> o.turns,
      "p" -> o.pairedAt.map(w.date),
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

  private[tournament] implicit val createdHandler = new BSON[Created] {
    def reads(r: BSON.Reader) = assertStatus(r, Status.Created) {
      Created(
        id = r str "_id",
        data = r.doc.as[Data],
        players = r.get[Players]("players"),
        waitMinutes = r.intO("wait"))
    }
    def writes(w: BSON.Writer, o: Created) = dataHandler.write(o.data) ++ BSONDocument(
      "_id" -> o.id,
      "status" -> Status.Created.id,
      "players" -> o.players,
      "wait" -> o.waitMinutes)
  }

  private[tournament] implicit val startedHandler = new BSON[Started] {
    def reads(r: BSON.Reader) = assertStatus(r, Status.Started) {
      Started(
        id = r str "_id",
        data = r.doc.as[Data],
        startedAt = r date "startedAt",
        players = r.get[Players]("players"),
        pairings = r.get[Pairings]("pairings"),
        events = ~r.getO[Events]("events"))
    }
    def writes(w: BSON.Writer, o: Started) = dataHandler.write(o.data) ++ BSONDocument(
      "_id" -> o.id,
      "status" -> Status.Started.id,
      "players" -> o.players,
      "pairings" -> o.pairings,
      "events" -> o.events,
      "startedAt" -> w.date(o.startedAt))
  }

  private[tournament] implicit val finishedHandler = new BSON[Finished] {
    def reads(r: BSON.Reader) = assertStatus(r, Status.Finished) {
      Finished(
        id = r str "_id",
        data = r.doc.as[Data],
        startedAt = r date "startedAt",
        players = r.get[Players]("players"),
        pairings = r.get[Pairings]("pairings"),
        events = ~r.getO[Events]("events"))
    }
    def writes(w: BSON.Writer, o: Finished) = dataHandler.write(o.data) ++ BSONDocument(
      "_id" -> o.id,
      "status" -> Status.Finished.id,
      "players" -> o.players,
      "pairings" -> o.pairings,
      "events" -> o.events,
      "startedAt" -> w.date(o.startedAt))
  }

  private[tournament] implicit val enterableHandler = new BSONHandler[BSONDocument, Enterable] with BSONDocumentReader[Enterable] with BSONDocumentWriter[Enterable] {
    def read(doc: BSONDocument): Enterable = ~doc.getAs[Int]("status") match {
      case Status.Created.id => doc.as[Created]
      case Status.Started.id => doc.as[Started]
      case _                 => sys error "tournament is not enterable"
    }
    def write(o: Enterable): BSONDocument = o match {
      case x: Created => createdHandler write x
      case x: Started => startedHandler write x
    }
  }

  private[tournament] implicit val anyHandler = new BSONHandler[BSONDocument, Tournament] with BSONDocumentReader[Tournament] with BSONDocumentWriter[Tournament] {
    def read(doc: BSONDocument): Tournament = ~doc.getAs[Int]("status") match {
      case Status.Created.id  => doc.as[Created]
      case Status.Started.id  => doc.as[Started]
      case Status.Finished.id => doc.as[Finished]
      case x                  => sys error s"tournament invalid status: $x"
    }
    def write(o: Tournament): BSONDocument = o match {
      case x: Created  => createdHandler write x
      case x: Started  => startedHandler write x
      case x: Finished => finishedHandler write x
    }
  }

  def assertStatus[A](r: BSON.Reader, status: Status)(f: => A): A =
    if (r.int("status") != status.id) sys error "invalid tournament status"
    else f
}
