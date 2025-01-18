package lila.fishnet

import reactivemongo.api.bson._
import shogi.variant.Variant

import lila.db.dsl._

private object BSONHandlers {

  implicit val ClientKeyBSONHandler: BSONHandler[Client.Key]     = stringAnyValHandler[Client.Key](_.value, Client.Key.apply)
  implicit val ClientVersionBSONHandler: BSONHandler[Client.Version] = stringAnyValHandler[Client.Version](_.value, Client.Version.apply)
  implicit val ClientPythonBSONHandler: BSONHandler[Client.Python]  = stringAnyValHandler[Client.Python](_.value, Client.Python.apply)
  implicit val ClientUserIdBSONHandler: BSONHandler[Client.UserId]  = stringAnyValHandler[Client.UserId](_.value, Client.UserId.apply)

  implicit val ClientSkillBSONHandler: BSONHandler[Client.Skill] = quickHandler[Client.Skill](
    { case BSONString(v) => Client.Skill.byKey(v).getOrElse(Client.Skill.Analysis) },
    x => BSONString(x.key)
  )

  import Client.{ Engine, Engines }
  implicit val EngineBSONHandler: BSONDocumentHandler[Engine]  = Macros.handler[Engine]
  implicit val EnginesBSONHandler: BSONDocumentHandler[Engines] = Macros.handler[Engines]

  import Client.Instance
  implicit val InstanceBSONHandler: BSONDocumentHandler[Instance] = Macros.handler[Instance]

  implicit val ClientBSONHandler: BSONDocumentHandler[Client] = Macros.handler[Client]

  implicit val VariantBSONHandler: BSONHandler[Variant] = quickHandler[Variant](
    { case BSONInteger(v) => Variant.orDefault(v) },
    x => BSONInteger(x.id)
  )

  implicit val WorkIdBSONHandler: BSONHandler[Work.Id] = stringAnyValHandler[Work.Id](_.value, Work.Id.apply)
  import Work.Acquired
  implicit val MoveAcquiredHandler: BSONDocumentHandler[Acquired] = Macros.handler[Acquired]
  import Work.Clock
  implicit val ClockHandler: BSONDocumentHandler[Clock] = Macros.handler[Clock]
  import Work.Game
  implicit val GameHandler: BSONDocumentHandler[Game] = Macros.handler[Game]
  import Work.Move
  implicit val MoveHandler: BSONDocumentHandler[Move] = Macros.handler[Move]
  import lila.analyse.Analysis.PostGameStudy
  implicit val PostGameStudyHandler: BSONDocumentHandler[PostGameStudy] = Macros.handler[PostGameStudy]
  import Work.Sender
  implicit val SenderHandler: BSONDocumentHandler[Sender] = Macros.handler[Sender]
  import Work.Analysis
  implicit val AnalysisHandler: BSONDocumentHandler[Analysis] = Macros.handler[Analysis]
  import Work.Puzzle.Source.FromGame
  implicit val SourceGameHandler: BSONDocumentHandler[FromGame] = Macros.handler[FromGame]
  import Work.Puzzle.Source.FromUser
  implicit val SourceUserHandler: BSONDocumentHandler[FromUser] = Macros.handler[FromUser]
  import Work.Puzzle.Source
  implicit val SourceHandler: BSONDocumentHandler[Source] = Macros.handler[Source]
  import Work.Puzzle
  implicit val PuzzleHandler: BSONDocumentHandler[Puzzle] = Macros.handler[Puzzle]
}
