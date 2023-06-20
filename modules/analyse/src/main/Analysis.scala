package lila.analyse

import chess.{ Ply, Color }

case class Analysis(
    id: Analysis.Id, // game ID, or chapter ID if studyId is set
    studyId: Option[StudyId],
    infos: List[Info],
    startPly: Ply,
    date: Instant,
    fk: Option[Analysis.FishnetKey]
):

  lazy val infoAdvices: InfoAdvices = {
    (Info.start(startPly) :: infos) sliding 2 collect { case List(prev, info) =>
      info -> {
        info.hasVariation so Advice(prev, info)
      }
    }
  }.toList

  lazy val advices: List[Advice] = infoAdvices.flatMap(_._2)

  def summary: List[(Color, List[(Advice.Judgement, Int)])] =
    Color.all map { color =>
      color -> (Advice.Judgement.all map { judgment =>
        judgment -> (advices count { adv =>
          adv.color == color && adv.judgment == judgment
        })
      })
    }

  def valid = infos.nonEmpty

  def nbEmptyInfos       = infos.count(_.isEmpty)
  def emptyRatio: Double = nbEmptyInfos.toDouble / infos.size

object Analysis:

  opaque type Id = String
  object Id extends OpaqueString[Id]

  case class Analyzed(game: lila.game.Game, analysis: Analysis)

  type FishnetKey = String
