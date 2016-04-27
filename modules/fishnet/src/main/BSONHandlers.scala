package lila.fishnet

import lila.db.BSON
import lila.db.BSON.{BSONJodaDateTimeHandler, stringAnyValHandler}
import reactivemongo.bson._

import chess.format.{ Uci, FEN }
import chess.variant.Variant

private object BSONHandlers {

  implicit val ClientKeyBSONHandler = stringAnyValHandler[Client.Key](_.value, Client.Key.apply)
  implicit val ClientVersionBSONHandler = stringAnyValHandler[Client.Version](_.value, Client.Version.apply)
  implicit val ClientPythonBSONHandler = stringAnyValHandler[Client.Python](_.value, Client.Python.apply)
  implicit val ClientUserIdBSONHandler = stringAnyValHandler[Client.UserId](_.value, Client.UserId.apply)
  implicit val ClientIpAddressBSONHandler = stringAnyValHandler[Client.IpAddress](_.value, Client.IpAddress.apply)

  implicit val ClientSkillBSONHandler = new BSONHandler[BSONString, Client.Skill] {
    def read(x: BSONString) = Client.Skill byKey x.value err s"Invalid client skill ${x.value}"
    def write(x: Client.Skill) = BSONString(x.key)
  }

  import Client.Engine
  implicit val EngineBSONHandler = Macros.handler[Engine]

  import Client.Instance
  implicit val InstanceBSONHandler = Macros.handler[Instance]

  implicit val ClientBSONHandler = Macros.handler[Client]

  implicit val VariantBSONHandler = new BSONHandler[BSONInteger, Variant] {
    def read(b: BSONInteger): Variant = Variant(b.value) err s"No such variant: ${b.value}"
    def write(x: Variant) = BSONInteger(x.id)
  }
  private implicit val FENBSONHandler = stringAnyValHandler[FEN](_.value, FEN.apply)

  implicit val WorkIdBSONHandler = stringAnyValHandler[Work.Id](_.value, Work.Id.apply)
  import Work.Acquired
  implicit val MoveAcquiredHandler = Macros.handler[Acquired]
  import Work.Game
  implicit val GameHandler = Macros.handler[Game]
  import Work.Move
  implicit val MoveHandler = Macros.handler[Move]
  import Work.Sender
  implicit val SenderHandler = Macros.handler[Sender]
  import Work.Analysis
  implicit val AnalysisHandler = Macros.handler[Analysis]
}
