package lila.analyse

import chess.Color
import chess.format.Nag

import org.joda.time.DateTime

case class Analysis(
    id: String,
    infos: List[Info],
    done: Boolean,
    date: DateTime,
    old: Boolean = false) {

  lazy val infoAdvices: InfoAdvices = {
    (Info.start :: infos) sliding 2 collect {
      case List(prev, info) => info -> {
        (old || info.hasVariation) ?? Advice(prev, info)
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
    done = true,
    old = false)

  def encode: RawAnalysis = RawAnalysis(id, encodeInfos, done, date, old)
  private def encodeInfos = Info encodeList infos

  def summary: List[(Color, List[(Nag, Int)])] = Color.all map { color =>
    color -> (Nag.badOnes map { nag =>
      nag -> (advices count { adv =>
        adv.color == color && adv.nag == nag
      })
    })
  }

  def valid = encodeInfos.replace(";", "").nonEmpty

  def stalled = (done && !valid) || (!done && date.isBefore(DateTime.now minusMinutes 31))
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
    done: Boolean,
    date: DateTime,
    old: Boolean = false) {

  def decode: Option[Analysis] = (done, data) match {
    case (true, "") => new Analysis(id, Nil, false, date, old).some
    case (true, d)  => Info decodeList d map { new Analysis(id, _, done, date, old) }
    case (false, _) => new Analysis(id, Nil, false, date, old).some
  }
}

private[analyse] object RawAnalysis {

  import lila.db.JsTube
  import JsTube.Helpers._
  import play.api.libs.json._

  private def defaults = Json.obj(
    "data" -> "",
    "done" -> false,
    "old" -> false)

  private[analyse] lazy val tube = JsTube(
    (__.json update merge(defaults)) andThen Json.reads[RawAnalysis],
    Json.writes[RawAnalysis])
}
