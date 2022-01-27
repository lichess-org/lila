package lila.analyse

import chess.Color

import org.joda.time.DateTime
import lila.user.User

case class Analysis(
    id: Analysis.ID, // game ID, or chapter ID if studyId is set
    studyId: Option[String],
    infos: List[Info],
    startPly: Int,
    date: DateTime,
    fk: Option[Analysis.FishnetKey]
) {

  lazy val infoAdvices: InfoAdvices = {
    (Info.start(startPly) :: infos) sliding 2 collect { case List(prev, info) =>
      info -> {
        info.hasVariation ?? Advice(prev, info)
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
}

object Analysis {

  import lila.db.BSON
  import reactivemongo.api.bson._

  case class Analyzed(game: lila.game.Game, analysis: Analysis)

  type ID         = String
  type FishnetKey = String

  implicit val analysisBSONHandler = new BSON[Analysis] {
    def reads(r: BSON.Reader) = {
      val startPly = r intD "ply"
      val raw      = r str "data"
      Analysis(
        id = r str "_id",
        studyId = r strO "studyId",
        infos = Info.decodeList(raw, startPly) err s"Invalid analysis data $raw",
        startPly = startPly,
        date = r date "date",
        fk = r strO "fk"
      )
    }
    def writes(w: BSON.Writer, a: Analysis) =
      BSONDocument(
        "_id"     -> a.id,
        "studyId" -> a.studyId,
        "data"    -> Info.encodeList(a.infos),
        "ply"     -> w.intO(a.startPly),
        "date"    -> w.date(a.date),
        "fk"      -> a.fk
      )
  }
}
