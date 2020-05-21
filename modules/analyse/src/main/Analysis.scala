package lidraughts.analyse

import draughts.Color
import draughts.format.Uci

import org.joda.time.DateTime

case class Analysis(
    id: String, // game ID, or chapter ID if studyId is set
    studyId: Option[String],
    infos: List[Info],
    startPly: Int,
    uid: Option[String], // requester lidraughts ID
    by: Option[String], // analyser lidraughts ID
    date: DateTime
) {

  def requestedBy = uid | "lidraughts"

  def providedBy = by | "lidraughts"

  def providedByLidraughts = by exists (_ startsWith "lidraughts-")

  lazy val infoAdvices: InfoAdvices = {
    (Info.start(startPly) :: infos) sliding 2 collect {
      case List(prev, info) => info -> {
        info.hasVariation ?? Advice(prev, info)
      }
    }
  }.toList

  lazy val advices: List[Advice] = infoAdvices.flatMap(_._2)

  def summary: List[(Color, List[(Advice.Judgement, Int)])] = Color.all map { color =>
    color -> (Advice.Judgement.all map { judgment =>
      judgment -> (advices count { adv =>
        adv.color == color && adv.judgment == judgment
      })
    })
  }

  def valid = infos.nonEmpty

  def nbEmptyInfos = infos.count(_.isEmpty)
  def emptyRatio: Double = nbEmptyInfos.toDouble / infos.size
}

object Analysis {

  import lidraughts.db.BSON
  import reactivemongo.bson._

  case class Analyzed(game: lidraughts.game.Game, analysis: Analysis)

  type ID = String

  private[analyse] implicit val analysisBSONHandler = new BSON[Analysis] {
    def reads(r: BSON.Reader) = {
      val startPly = r intD "ply"
      val raw = r str "data"
      Analysis(
        id = r str "_id",
        studyId = r strO "studyId",
        infos = Info.decodeList(raw, startPly) err s"Invalid analysis data $raw",
        startPly = startPly,
        uid = r strO "uid",
        by = r strO "by",
        date = r date "date"
      )
    }
    def writes(w: BSON.Writer, o: Analysis) = BSONDocument(
      "_id" -> o.id,
      "studyId" -> o.studyId,
      "data" -> Info.encodeList(o.infos),
      "ply" -> w.intO(o.startPly),
      "uid" -> o.uid,
      "by" -> o.by,
      "date" -> w.date(o.date)
    )
  }
}
