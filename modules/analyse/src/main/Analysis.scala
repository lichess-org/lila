package lila.analyse

import chess.{ Ply, Color }

case class Analysis(
    id: Analysis.Id,
    infos: List[Info],
    startPly: Ply,
    date: Instant,
    fk: Option[Analysis.FishnetKey]
):

  lazy val infoAdvices: InfoAdvices =
    (Info.start(startPly) :: infos)
      .sliding(2)
      .collect:
        case List(prev, info) => info -> info.hasVariation.so(Advice(prev, info))
      .toList

  lazy val advices: List[Advice] = infoAdvices.flatMap(_._2)

  def summary: List[(Color, List[(Advice.Judgement, Int)])] =
    Color.all map { color =>
      color -> (Advice.Judgement.all map { judgment =>
        judgment -> (advices count { adv =>
          adv.color == color && adv.judgment == judgment
        })
      })
    }

  def studyId = id.studyId
  def valid   = infos.nonEmpty

  def nbEmptyInfos       = infos.count(_.isEmpty)
  def emptyRatio: Double = nbEmptyInfos.toDouble / infos.size

object Analysis:

  enum Id:
    case Game(id: GameId)
    case Study(study: StudyId, id: StudyChapterId)

  object Id:
    def apply(gameId: GameId): Id                          = Game(gameId)
    def apply(study: StudyId, chapter: StudyChapterId): Id = Study(study, chapter)
    def apply(study: Option[StudyId], id: String): Id =
      study.fold(Game(GameId(id)))(Study(_, StudyChapterId(id)))

    extension (id: Id)

      def value: String = id match
        case Game(gameId) => gameId.value
        case Study(_, id) => id.value

      def gameId: Option[GameId] = id match
        case Game(gameId) => Some(gameId)
        case _            => None

      def chapterId: Option[StudyChapterId] = id match
        case Study(_, chapterId) => Some(chapterId)
        case _                   => None

      def studyId: Option[StudyId] = id match
        case Study(studyId, _) => Some(studyId)
        case _                 => None

  case class Analyzed(game: lila.game.Game, analysis: Analysis)

  type FishnetKey = String
