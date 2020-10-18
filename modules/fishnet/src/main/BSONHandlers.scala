package lila.fishnet

import lila.db.dsl._
import reactivemongo.api.bson._

import chess.variant.Variant

private object BSONHandlers {

  implicit val ClientKeyBSONHandler     = stringAnyValHandler[Client.Key](_.value, Client.Key.apply)
  implicit val ClientVersionBSONHandler = stringAnyValHandler[Client.Version](_.value, Client.Version.apply)
  implicit val ClientPythonBSONHandler  = stringAnyValHandler[Client.Python](_.value, Client.Python.apply)
  implicit val ClientUserIdBSONHandler  = stringAnyValHandler[Client.UserId](_.value, Client.UserId.apply)

  implicit val ClientSkillBSONHandler = tryHandler[Client.Skill](
    { case BSONString(v) => Client.Skill byKey v toTry s"Invalid client skill $v" },
    x => BSONString(x.key)
  )

  import Client.{ Engine, Engines }
  implicit val EngineBSONHandler  = Macros.handler[Engine]
  implicit val EnginesBSONHandler = Macros.handler[Engines]

  import Client.Instance
  implicit val InstanceBSONHandler = Macros.handler[Instance]

  implicit val ClientBSONHandler = Macros.handler[Client]

  implicit val VariantBSONHandler = tryHandler[Variant](
    { case BSONInteger(v) => Variant(v) toTry s"Invalid variant $v" },
    x => BSONInteger(x.id)
  )

  implicit val WorkIdBSONHandler = stringAnyValHandler[Work.Id](_.value, Work.Id.apply)
  import Work.Acquired
  implicit val MoveAcquiredHandler = Macros.handler[Acquired]
  import Work.Clock
  implicit val ClockHandler = Macros.handler[Clock]
  import Work.Game
  implicit val GameHandler = Macros.handler[Game]
  import Work.Sender
  implicit val SenderHandler = Macros.handler[Sender]
  import Work.Analysis
  implicit val AnalysisHandler = Macros.handler[Analysis]
}
