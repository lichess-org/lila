package lila.fishnet

import lila.db.BSON
import lila.db.BSON.BSONJodaDateTimeHandler
import reactivemongo.bson._

import chess.format.{ Uci, FEN }
import chess.variant.Variant

private object BSONHandlers {

  implicit val ClientKeyBSONHandler = new BSONHandler[BSONString, Client.Key] {
    def read(x: BSONString) = Client.Key(x.value)
    def write(x: Client.Key) = BSONString(x.value)
  }
  implicit val ClientVersionBSONHandler = new BSONHandler[BSONString, Client.Version] {
    def read(x: BSONString) = Client.Version(x.value)
    def write(x: Client.Version) = BSONString(x.value)
  }
  implicit val ClientUserIdBSONHandler = new BSONHandler[BSONString, Client.UserId] {
    def read(x: BSONString) = Client.UserId(x.value)
    def write(x: Client.UserId) = BSONString(x.value)
  }
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

  import Work.Acquired
  implicit val MoveAcquiredHandler = Macros.handler[Acquired]
  import Work.Game
  implicit val GameHandler = Macros.handler[Game]
  import Work.Move
  implicit val MoveHandler = Macros.handler[Move]
  import Work.Analysis
  implicit val AnalysisHandler = Macros.handler[Analysis]
}
