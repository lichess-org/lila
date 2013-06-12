package lila.analyse

import chess.Color
import chess.format.Nag

case class Analysis(
    id: String,
    infos: List[Info],
    done: Boolean,
    fail: Option[String]) {

  lazy val infoAdvices: InfoAdvices = (infos sliding 2 collect {
    case info :: next :: Nil ⇒ info -> Advice(info, next)
  }).toList

  lazy val advices: List[Advice] = infoAdvices.map(_._2).flatten

  lazy val advantageChart = new AdvantageChart(infoAdvices)

  def encode: RawAnalysis = RawAnalysis(
    id = id,
    encoded = encodeInfos,
    done = done,
    fail = fail)

  def encodeInfos = infos map (_.encode) mkString Analysis.separator

  def summary: List[(Color, List[(Nag, Int)])] = Color.all map { color ⇒
    color -> (Nag.all map { nag ⇒
      nag -> (advices count { adv ⇒
        adv.color == color && adv.nag == nag
      })
    })
  }
}

object Analysis {

  val separator = " "

  def decodeInfos(enc: String): Option[List[Info]] =
    (enc.split(separator).toList.zipWithIndex map {
      case (info, index) ⇒ Info.decode(index + 1, info)
    }).sequence.fold(
      err ⇒ { logger.warn("[analysis] " + err); none[List[Info]] },
      _.some
    )

  import lila.db.Tube
  import play.api.libs.json._

  private[analyse] lazy val tube = Tube(
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

case class AnalysisMaker(infos: List[Info], done: Boolean, fail: Option[String]) {

  def apply(id: String) = Analysis(id, infos, done, fail)
}
object AnalysisMaker {

  def apply(str: String, done: Boolean): Option[AnalysisMaker] =
    Analysis.decodeInfos(str) map { AnalysisMaker(_, done, none) }
}

final class AnalysisBuilder(infos: List[Info]) {

  def size = infos.size

  def +(info: Int ⇒ Info) = new AnalysisBuilder(info(infos.size + 1) :: infos)

  def done: AnalysisMaker = AnalysisMaker(infos.reverse.zipWithIndex map {
    case (info, turn) ⇒ (turn % 2 == 0).fold(
      info,
      info.copy(score = info.score map (_.negate))
    )
  }, true, none)
}

private[analyse] case class RawAnalysis(
    id: String,
    encoded: String,
    done: Boolean,
    fail: Option[String]) {

  def decode: Option[Analysis] = (done, encoded.trim) match {
    case (true, "") ⇒ new Analysis(id, Nil, false, fail orElse "No move infos".some).some
    case (true, en) ⇒ Analysis.decodeInfos(en) map { infos ⇒
      new Analysis(id, infos, done, none)
    }
    case (false, _) ⇒ new Analysis(id, Nil, false, fail).some
  }
}

private[analyse] object RawAnalysis {

  import lila.db.Tube
  import Tube.Helpers._
  import play.api.libs.json._

  private def defaults = Json.obj(
    "encoded" -> "",
    "done" -> false,
    "fail" -> none[String])

  private[analyse] lazy val tube = Tube(
    (__.json update merge(defaults)) andThen Json.reads[RawAnalysis],
    Json.writes[RawAnalysis])
}
