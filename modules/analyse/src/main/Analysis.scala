package lila.analyse

import chess.Color
import chess.format.Nag

import org.joda.time.DateTime

case class Analysis(
    id: String,
    infos: List[Info],
    startPly: Int,
    done: Boolean,
    date: DateTime) {

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

  def complete(infos: List[Info]) = copy(
    infos = infos,
    done = true)

  def encode: RawAnalysis = RawAnalysis(id, encodeInfos, startPly.some.filterNot(0 ==), done, date)
  private def encodeInfos = Info encodeList infos

  def summary: List[(Color, List[(Nag, Int)])] = Color.all map { color =>
    color -> (Nag.badOnes map { nag =>
      nag -> (advices count { adv =>
        adv.color == color && adv.nag == nag
      })
    })
  }

  def valid = encodeInfos.replace(";", "").nonEmpty

  def stalled = (done && !valid) || (!done && date.isBefore(DateTime.now minusHours 2))

  def nbEmptyInfos = infos.count(_.isEmpty)
  def emptyRatio: Double = nbEmptyInfos.toDouble / infos.size
}

object Analysis {

  import lila.db.JsTube, JsTube.Helpers._
  import play.api.libs.json._

  private[analyse] lazy val tube = JsTube(
    reader = (__.json update readDate('date)) andThen Reads[Analysis](js =>
      ~(for {
        obj ← js.asOpt[JsObject]
        rawAnalysis ← RawAnalysis.tube.read(obj).asOpt
        analysis ← rawAnalysis.decode
      } yield JsSuccess(analysis): JsResult[Analysis])
    ),
    writer = Writes[Analysis](analysis =>
      RawAnalysis.tube.write(analysis.encode) getOrElse JsUndefined("[db] Can't write analysis " + analysis.id)
    ) andThen (__.json update writeDate('date))
  )
}

private[analyse] case class RawAnalysis(
    id: String,
    data: String,
    ply: Option[Int],
    done: Boolean,
    date: DateTime) {

  def decode: Option[Analysis] = (done, data) match {
    case (true, "") => new Analysis(id, Nil, ~ply, false, date).some
    case (true, d)  => Info.decodeList(d, ~ply) map { new Analysis(id, _, ~ply, done, date) }
    case (false, _) => new Analysis(id, Nil, ~ply, false, date).some
  }
}

private[analyse] object RawAnalysis {

  import lila.db.JsTube
  import JsTube.Helpers._
  import play.api.libs.json._

  private def defaults = Json.obj(
    "data" -> "",
    "done" -> false)

  private[analyse] lazy val tube = JsTube(
    (__.json update merge(defaults)) andThen Json.reads[RawAnalysis],
    Json.writes[RawAnalysis])
}
