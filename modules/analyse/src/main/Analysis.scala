package lila.analyse

import chess.Color
import chess.format.Nag

import org.joda.time.DateTime

case class Analysis(
    id: String,
    infos: List[Info],
    startPly: Int,
    uid: Option[String], // requester lichess ID
    by: Option[String], // analyser lichess ID
    date: DateTime) {

  def requestedBy = uid | "lichess"

  def providedBy = by | "lichess"

  def providedByLichess = by exists (_ startsWith "lichess-")

  lazy val infoAdvices: InfoAdvices = {
    (Info.start(startPly) :: infos) sliding 2 collect {
      case List(prev, info) => info -> {
        info.hasVariation ?? Advice(prev, info)
      }
    }
  }.toList

  lazy val advices: List[Advice] = infoAdvices.map(_._2).flatten

  // ply -> UCI
  def bestMoves: Map[Int, String] = (infos map { i =>
    i.best map { b => i.ply -> b.keys }
  }).flatten.toMap

  def summary: List[(Color, List[(Nag, Int)])] = Color.all map { color =>
    color -> (Nag.badOnes map { nag =>
      nag -> (advices count { adv =>
        adv.color == color && adv.nag == nag
      })
    })
  }

  def valid = infos.nonEmpty

  def nbEmptyInfos = infos.count(_.isEmpty)
  def emptyRatio: Double = nbEmptyInfos.toDouble / infos.size
}

object Analysis {

  import lila.db.BSON
  import lila.db.BSON.BSONJodaDateTimeHandler
  import reactivemongo.bson._

  private[analyse] implicit val analysisBSONHandler = new BSON[Analysis] {
    def reads(r: BSON.Reader) = {
      val startPly = r intD "ply"
      val raw = r str "data"
      Analysis(
        id = r str "_id",
        infos = Info.decodeList(raw, startPly) err s"Invalid analysis data $raw",
        startPly = startPly,
        uid = r strO "uid",
        by = r strO "by",
        date = r date "date")
    }
    def writes(w: BSON.Writer, o: Analysis) = BSONDocument(
      "_id" -> o.id,
      "data" -> Info.encodeList(o.infos),
      "ply" -> w.intO(o.startPly),
      "uid" -> o.uid,
      "by" -> o.by,
      "date" -> w.date(o.date))
  }

  private[analyse] lazy val tube = lila.db.BsTube(analysisBSONHandler)
}
