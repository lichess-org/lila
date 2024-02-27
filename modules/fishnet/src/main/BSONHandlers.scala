package lila.fishnet

import lila.db.dsl._
import reactivemongo.api.bson._

import shogi.variant.Variant

private object BSONHandlers {

  implicit val ClientKeyBSONHandler     = stringAnyValHandler[Client.Key](_.value, Client.Key.apply)
  implicit val ClientVersionBSONHandler = stringAnyValHandler[Client.Version](_.value, Client.Version.apply)
  implicit val ClientPythonBSONHandler  = stringAnyValHandler[Client.Python](_.value, Client.Python.apply)
  implicit val ClientUserIdBSONHandler  = stringAnyValHandler[Client.UserId](_.value, Client.UserId.apply)

  implicit val ClientSkillBSONHandler = quickHandler[Client.Skill](
    { case BSONString(v) => Client.Skill.byKey(v).getOrElse(Client.Skill.Analysis) },
    x => BSONString(x.key)
  )

  import Client.{ Engine, Engines }
  implicit val EngineBSONHandler  = Macros.handler[Engine]
  implicit val EnginesBSONHandler = Macros.handler[Engines]

  import Client.Instance
  implicit val InstanceBSONHandler = Macros.handler[Instance]

  implicit val ClientBSONHandler = Macros.handler[Client]

  implicit val VariantBSONHandler = quickHandler[Variant](
    { case BSONInteger(v) => Variant.orDefault(v) },
    x => BSONInteger(x.id)
  )

  implicit val WorkIdBSONHandler = stringAnyValHandler[Work.Id](_.value, Work.Id.apply)
  import Work.Acquired
  implicit val MoveAcquiredHandler = Macros.handler[Acquired]
  import Work.Clock
  implicit val ClockHandler = Macros.handler[Clock]
  import Work.Game
  implicit val GameHandler = Macros.handler[Game]
  import Work.Move
  implicit val MoveHandler = Macros.handler[Move]
  import lila.analyse.Analysis.PostGameStudy
  implicit val PostGameStudyHandler = Macros.handler[PostGameStudy]
  import Work.Sender
  implicit val SenderHandler = Macros.handler[Sender]
  import Work.Analysis
  implicit val AnalysisHandler = Macros.handler[Analysis]
  import Work.Puzzle.Source.FromGame
  implicit val SourceGameHandler = Macros.handler[FromGame]
  import Work.Puzzle.Source.FromUser
  implicit val SourceUserHandler = Macros.handler[FromUser]
  import Work.Puzzle.Source
  implicit val SourceHandler = Macros.handler[Source]
  import Work.Puzzle
  implicit val PuzzleHandler = Macros.handler[Puzzle]
}
