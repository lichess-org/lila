package lila.fishnet

import lila.db.BSON
import lila.db.BSON.{BSONJodaDateTimeHandler, stringAnyValHandler}
import reactivemongo.bson._

import chess.format.{ Uci, FEN }
import chess.variant.Variant

private object BSONHandlers {

  implicit val ClientKeyBSONHandler = stringAnyValHandler[Client.Key](_.value, Client.Key.apply)
  implicit val ClientVersionBSONHandler = stringAnyValHandler[Client.Version](_.value, Client.Version.apply)
  implicit val ClientUserIdBSONHandler = stringAnyValHandler[Client.UserId](_.value, Client.UserId.apply)
  implicit val ClientUUIDBSONHandler = stringAnyValHandler[Client.UUID](_.value, Client.UUID.apply)

  implicit val ClientSkillBSONHandler = new BSONHandler[BSONString, Client.Skill] {
    def read(x: BSONString) = Client.Skill byKey x.value err s"Invalid client skill ${x.value}"
    def write(x: Client.Skill) = BSONString(x.key)
  }

  import Client.Engine
  implicit val EngineBSONHandler = Macros.handler[Engine]

  import Client.Instance
  implicit val InstanceBSONHandler = Macros.handler[Instance]

  import Stats.Result
  implicit val StatsResultBSONHandler = Macros.handler[Result]
  implicit val StatsBSONHandler = Macros.handler[Stats]

  implicit val ClientBSONHandler = Macros.handler[Client]

  implicit val VariantBSONHandler = new BSONHandler[BSONInteger, Variant] {
    def read(b: BSONInteger): Variant = Variant(b.value) err s"No such variant: ${b.value}"
    def write(x: Variant) = BSONInteger(x.id)
  }
  implicit val FENBSONHandler = new BSONHandler[BSONString, FEN] {
    def read(b: BSONString) = FEN(b.value)
    def write(x: FEN) = BSONString(x.value)
  }

  implicit val WorkIdBSONHandler = stringAnyValHandler[Work.Id](_.value, Work.Id.apply)
  import Work.Acquired
  implicit val MoveAcquiredHandler = Macros.handler[Acquired]
  import Work.Game
  implicit val GameHandler = Macros.handler[Game]
  import Work.Move
  implicit val MoveHandler = Macros.handler[Move]
  import Work.Analysis
  implicit val AnalysisHandler = Macros.handler[Analysis]
}
