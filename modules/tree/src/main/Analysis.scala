package lila.tree

import chess.format.pgn.{ Pgn, PgnStr }
import chess.{ Color, Ply }
import play.api.libs.json.JsObject
import Analysis.EngineId

case class AnalysisProgress(gameId: GameId, payload: () => JsObject)
case class StudyAnalysisProgress(analysis: Analysis, complete: Boolean)
case class Engine(nodesPerMove: Int, id: EngineId = EngineId("fishnet"), userId: Option[UserId] = none)

trait Analyser:
  def byId(id: Analysis.Id): Fu[Option[Analysis]]

trait Annotator:
  def toPgnString(pgn: Pgn): PgnStr
  def addEvals(p: Pgn, analysis: Analysis): Pgn

trait AnalysisJson:
  def bothPlayers(startedAtPly: Ply, analysis: Analysis, withAccuracy: Boolean = true): JsObject

case class Analysis(
    id: Analysis.Id,
    infos: List[Info],
    startPly: Ply,
    date: Instant,
    fk: Option[Analysis.FishnetKey],
    engine: Engine
):
  lazy val infoAdvices: InfoAdvices =
    (Info.start(startPly) :: infos)
      .sliding(2)
      .collect:
        case List(prev, info) => info -> info.hasVariation.so(Advice(prev, info))
      .toList

  lazy val advices: List[Advice] = infoAdvices.flatMap(_._2)
  def nodesPerMove: Int = engine.nodesPerMove
  def summary: List[(Color, List[(Advice.Judgement, Int)])] =
    Color.all.map { color =>
      color -> (Advice.Judgement.all.map { judgment =>
        judgment -> (advices.count { adv =>
          adv.color == color && adv.judgment == judgment
        })
      })
    }

  def studyId = id.studyId
  def valid = infos.nonEmpty

  def nbEmptyInfos = infos.count(_.isEmpty)
  def emptyRatio: Double = nbEmptyInfos.toDouble / infos.size

object Analysis:

  opaque type EngineId = String
  object EngineId extends OpaqueString[EngineId]

  enum Id:
    case Game(id: GameId)
    case Study(study: StudyId, id: StudyChapterId)

  object Id:
    def apply(gameId: GameId): Id = Game(gameId)
    def apply(study: StudyId, chapter: StudyChapterId): Id = Study(study, chapter)
    def apply(study: Option[StudyId], id: String): Id =
      study.fold(Game(GameId(id)))(Study(_, StudyChapterId(id)))

    extension (id: Id)

      def value: String = id match
        case Game(gameId) => gameId.value
        case Study(_, id) => id.value

      def gameId: Option[GameId] = id match
        case Game(gameId) => Some(gameId)
        case _ => None

      def chapterId: Option[StudyChapterId] = id match
        case Study(_, chapterId) => Some(chapterId)
        case _ => None

      def studyId: Option[StudyId] = id match
        case Study(studyId, _) => Some(studyId)
        case _ => None

  type FishnetKey = String

  import play.api.libs.json.*
  import scalalib.json.Json.given

  given Reads[Engine] = Json.reads[Engine]
  given Writes[Engine] = Json.writes

  given Reads[Analysis] = Reads: js =>
    for
      rawId <- (js \ "id").validate[String]
      rawStudyIdOpt <- (js \ "studyId").validateOpt[String]
      infos <- (js \ "infos").validate[List[Info]]
      startPly <- (js \ "startPly").validate[Ply]
      date <- (js \ "date").validate[Instant]
      engine <- (js \ "engine").validate[Engine]
    yield Analysis(
      id = rawStudyIdOpt match
        case Some(sid) => Analysis.Id.Study(StudyId(sid), StudyChapterId(rawId))
        case None => Analysis.Id.Game(GameId(rawId)),
      infos = infos,
      startPly = startPly,
      date = date,
      fk = None,
      engine = engine
    )
