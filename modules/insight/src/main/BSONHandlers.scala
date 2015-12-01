package lila.insight

import reactivemongo.bson._
import reactivemongo.bson.Macros

import chess.{ Role, Color }
import lila.db.BSON
import lila.db.BSON._
import lila.db.Implicits._
import lila.game.BSONHandlers.StatusBSONHandler
import lila.rating.PerfType

private object BSONHandlers {

  implicit val ColorBSONHandler = new BSONHandler[BSONBoolean, Color] {
    def read(b: BSONBoolean) = Color(b.value)
    def write(c: Color) = BSONBoolean(c.white)
  }
  implicit val PerfTypeBSONHandler = new BSONHandler[BSONInteger, PerfType] {
    def read(b: BSONInteger) = PerfType.byId get b.value err s"Invalid perf type id ${b.value}"
    def write(p: PerfType) = BSONInteger(p.id)
  }
  implicit val EcopeningBSONHandler = new BSONHandler[BSONString, Ecopening] {
    def read(b: BSONString) = EcopeningDB.allByEco get b.value err s"Invalid ECO ${b.value}"
    def write(e: Ecopening) = BSONString(e.eco)
  }
  implicit val RelativeStrengthBSONHandler = new BSONHandler[BSONInteger, RelativeStrength] {
    def read(b: BSONInteger) = RelativeStrength.byId get b.value err s"Invalid relative strength ${b.value}"
    def write(e: RelativeStrength) = BSONInteger(e.id)
  }
  implicit val ResultBSONHandler = new BSONHandler[BSONInteger, Result] {
    def read(b: BSONInteger) = Result.byId get b.value err s"Invalid result ${b.value}"
    def write(e: Result) = BSONInteger(e.id)
  }
  implicit val PhaseBSONHandler = new BSONHandler[BSONInteger, Phase] {
    def read(b: BSONInteger) = Phase.byId get b.value err s"Invalid phase ${b.value}"
    def write(e: Phase) = BSONInteger(e.id)
  }
  implicit val RoleBSONHandler = new BSONHandler[BSONString, Role] {
    def read(b: BSONString) = Role.allByForsyth get b.value.head err s"Invalid role ${b.value}"
    def write(e: Role) = BSONString(e.forsyth.toString)
  }
  implicit val TerminationBSONHandler = new BSONHandler[BSONInteger, Termination] {
    def read(b: BSONInteger) = Termination.byId get b.value err s"Invalid termination ${b.value}"
    def write(e: Termination) = BSONInteger(e.id)
  }
  implicit val MovetimeRangeBSONHandler = new BSONHandler[BSONInteger, MovetimeRange] {
    def read(b: BSONInteger) = MovetimeRange.byId get b.value err s"Invalid movetime range ${b.value}"
    def write(e: MovetimeRange) = BSONInteger(e.id)
  }
  implicit def MoveBSONHandler = new BSON[Move] {
    def reads(r: Reader) = Move(
      phase = r.get[Phase]("p"),
      tenths = r.get[Int]("t"),
      role = r.get[Role]("r"),
      eval = r.intO("e"),
      mate = r.intO("m"),
      cpl = r.intO("c"),
      opportunism = r.boolO("o"),
      luck = r.boolO("l"))
    def writes(w: Writer, b: Move) = BSONDocument(
      "p" -> b.phase,
      "t" -> b.tenths,
      "r" -> b.role,
      "e" -> b.eval,
      "m" -> b.mate,
      "c" -> b.cpl,
      "o" -> b.opportunism,
      "l" -> b.luck)
  }

  implicit def EntryBSONHandler = new BSON[Entry] {
    import Entry.BSONFields._
    def reads(r: Reader) = Entry(
      id = r.str(id),
      userId = r.str(userId),
      color = r.get[Color](color),
      perf = r.get[PerfType](perf),
      eco = r.getO[Ecopening](eco),
      opponentRating = r.int(opponentRating),
      opponentStrength = r.get[RelativeStrength](opponentStrength),
      moves = r.get[List[Move]](moves),
      result = r.get[Result](result),
      termination = r.get[Termination](termination),
      ratingDiff = r.int(ratingDiff),
      analysed = r.boolD(analysed),
      provisional = r.boolD(provisional),
      date = r.date(date))
    def writes(w: Writer, e: Entry) = BSONDocument(
      id -> e.id,
      userId -> e.userId,
      color -> e.color,
      perf -> e.perf,
      eco -> e.eco,
      opponentRating -> e.opponentRating,
      opponentStrength -> e.opponentStrength,
      moves -> e.moves,
      result -> e.result,
      termination -> e.termination,
      ratingDiff -> e.ratingDiff,
      analysed -> w.boolO(e.analysed),
      provisional -> w.boolO(e.provisional),
      date -> e.date)
  }
}
