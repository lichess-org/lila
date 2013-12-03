package lila.analyse

import chess.Color
import chess.format.Nag

case class Analysis(
    id: String,
    infos: List[Info],
    done: Boolean,
    old: Boolean = false) {

  lazy val infoAdvices: InfoAdvices = {
    (Info.start :: infos) sliding 2 collect {
      case List(prev, info) ⇒ info -> {
        (old || info.hasVariation) ??  Advice(prev, info)
      }
    }
  }.toList

  lazy val advices: List[Advice] = infoAdvices.map(_._2).flatten

  // ply -> UCI
  def bestMoves: Map[Int, String] = (infos map { i ⇒
    i.best map { b ⇒ i.ply -> b.keys }
  }).flatten.toMap

  def encode: RawAnalysis = RawAnalysis(id, Info encodeList infos, done)

  def summary: List[(Color, List[(Nag, Int)])] = Color.all map { color ⇒
    color -> (Nag.badOnes map { nag ⇒
      nag -> (advices count { adv ⇒
        adv.color == color && adv.nag == nag
      })
    })
  }
}

object Analysis {

  import lila.db.JsTube
  import play.api.libs.json._

  private[analyse] lazy val tube = JsTube(
    reader = Reads[Analysis](js ⇒
      ~(for {
        obj ← js.asOpt[JsObject]
        rawAnalysis ← RawAnalysis.tube.read(obj).asOpt
        analysis ← rawAnalysis.decode
      } yield JsSuccess(analysis): JsResult[Analysis])
    ),
    writer = Writes[Analysis](analysis ⇒
      RawAnalysis.tube.write(analysis.encode) getOrElse JsUndefined("[db] Can't write analysis " + analysis.id)
    )
  )
}

private[analyse] case class RawAnalysis(id: String, data: String, done: Boolean, old: Boolean = false) {

  def decode: Option[Analysis] = (done, data) match {
    case (true, "") ⇒ new Analysis(id, Nil, false, old).some
    case (true, d)  ⇒ Info decodeList d map { new Analysis(id, _, done, old) }
    case (false, _) ⇒ new Analysis(id, Nil, false, old).some
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
