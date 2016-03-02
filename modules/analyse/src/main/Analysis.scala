package lila.analyse

import chess.Color
import chess.format.Nag

import org.joda.time.DateTime

case class Analysis(
    id: String,
    infos: List[Info],
    startPly: Int,
    done: Boolean,
    date: DateTime,
    serverIp: Option[String]) {

  lazy val infoAdvices: InfoAdvices = {
    (Info.start(startPly) :: infos) sliding 2 collect {
      case List(prev, info) => info -> {
        info.hasVariation ?? Advice(prev, info)
      }
    }
  }.toList

  lazy val advices: List[Advice] = infoAdvices.flatMap(_._2)

  // ply -> UCI
  def bestMoves: Map[Int, String] = infos.flatMap { i =>
    i.best map { b => i.ply -> b.keys }
  }.toMap

  def complete(infos: List[Info]) = copy(
    infos = infos,
    done = true)

  def summary: List[(Color, List[(Nag, Int)])] = Color.all map { color =>
    color -> (Nag.badOnes map { nag =>
      nag -> (advices count { adv =>
        adv.color == color && adv.nag == nag
      })
    })
  }

  def valid = infos.nonEmpty

  def stalled = (done && !valid) || (!done && date.isBefore(DateTime.now minusHours 12))

  def nbEmptyInfos = infos.count(_.isEmpty)
  def emptyRatio: Double = nbEmptyInfos.toDouble / infos.size
}

object Analysis {

  import lila.db.BSON
  import lila.db.BSON.BSONJodaDateTimeHandler
  import reactivemongo.bson._

  private implicit val analysisBSONHandler = new BSON[Analysis] {
    def reads(r: BSON.Reader) = {
      val id = r str "_id"
      val ply = r intO "ply"
      val date = r date "date"
      val ip = r strO "ip"
      (r strD "data", r boolD "done") match {
        case ("", true) => new Analysis(id, Nil, ~ply, false, date, ip)
        case (d, true) => Info.decodeList(d, ~ply) map {
          new Analysis(id, _, ~ply, true, date, ip)
        } err s"Invalid analysis data $d"
        case (_, false) => new Analysis(id, Nil, ~ply, false, date, ip)
      }
    }
    def writes(w: BSON.Writer, o: Analysis) = BSONDocument(
      "_id" -> o.id,
      "data" -> Info.encodeList(o.infos),
      "ply" -> w.intO(o.startPly),
      "done" -> o.done,
      "date" -> w.date(o.date),
      "ip" -> o.serverIp)
  }

  private[analyse] lazy val tube = lila.db.BsTube(analysisBSONHandler)
}
