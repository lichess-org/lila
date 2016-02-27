package lila.study

import chess.format.Uci
import chess.{ Pos, Role, PromotableRole }
import reactivemongo.bson._

import lila.db.BSON
import lila.db.BSON._
import lila.db.BSON.BSONJodaDateTimeHandler

private object BSONHandlers {

  import Chapter._

  private implicit val PosBSONHandler = new BSONHandler[BSONString, Pos] {
    def read(bsonStr: BSONString): Pos = Pos.posAt(bsonStr.value) err s"No such pos: ${bsonStr.value}"
    def write(x: Pos) = BSONString(x.key)
  }

  implicit val ShapeBSONHandler = new BSON[Shape] {
    def reads(r: Reader) = {
      val brush = r str "b"
      r.getO[Pos]("p") map { pos =>
        Shape.Circle(brush, pos)
      } getOrElse Shape.Arrow(brush, r.get[Pos]("o"), r.get[Pos]("d"))
    }
    def writes(w: Writer, t: Shape) = t match {
      case Shape.Circle(brush, pos)       => BSONDocument("b" -> brush, "p" -> pos.key)
      case Shape.Arrow(brush, orig, dest) => BSONDocument("b" -> brush, "o" -> orig.key, "d" -> dest.key)
    }
  }

  implicit val PromotableRoleHandler = new BSONHandler[BSONString, PromotableRole] {
    def read(bsonStr: BSONString): PromotableRole = bsonStr.value.headOption flatMap Role.allPromotableByForsyth.get err s"No such role: ${bsonStr.value}"
    def write(x: PromotableRole) = BSONString(x.forsyth.toString)
  }

  implicit val RoleHandler = new BSONHandler[BSONString, Role] {
    def read(bsonStr: BSONString): Role = bsonStr.value.headOption flatMap Role.allByForsyth.get err s"No such role: ${bsonStr.value}"
    def write(x: Role) = BSONString(x.forsyth.toString)
  }

  implicit val UciBSONHandler = new BSON[Uci] {
    def reads(r: Reader) = {
      r.getO[Pos]("o") map { orig =>
        Uci.Move(orig, r.get[Pos]("d"), r.getO[PromotableRole]("p"))
      } getOrElse Uci.Drop(r.get[Role]("r"), r.get[Pos]("p"))
    }
    def writes(w: Writer, u: Uci) = u match {
      case Uci.Move(orig, dest, prom) => BSONDocument("o" -> orig, "d" -> dest, "p" -> prom)
      case Uci.Drop(role, pos)        => BSONDocument("r" -> role, "p" -> pos)
    }
  }

  import Step.Move
  private implicit val MoveBSONHandler = Macros.handler[Move]

  private implicit def StepBSONHandler: BSON[Step] = new BSON[Step] {
    def reads(r: Reader) = Step(
      ply = r int "p",
      move = r.getO[Move]("m"),
      fen = r str "f",
      check = r boolD "c",
      variations = r.getsD[List[Step]]("v"))
    def writes(w: Writer, s: Step) = BSONDocument(
      "p" -> s.ply,
      "m" -> s.move,
      "f" -> s.fen,
      "c" -> w.boolO(s.check),
      "v" -> s.variations)
  }

  private implicit val CrumbBSONHandler = Macros.handler[Crumb]

  implicit val PathBSONHandler = Macros.handler[Path]

  private implicit val ChapterBSONHandler = Macros.handler[Chapter]

  private implicit val ChaptersMap = MapDocument.MapHandler[Chapter]

  implicit val StudyBSONHandler = Macros.handler[Study]
}
