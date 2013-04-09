package lila.analyse

import chess.Color

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

  def summary: List[(Color, List[(CpSeverity, Int)])] = Color.all map { color ⇒
    color -> (CpSeverity.all map { sev ⇒
      sev -> (advices count { adv ⇒
        adv.color == color && adv.severity == sev
      })
    })
  }
}

object Analysis {

  val separator = " "

  def make(str: String, done: Boolean): Option[String ⇒ Analysis] =
    Analysis.decodeInfos(str) map { infos ⇒
      (id: String) ⇒ new Analysis(id, infos, done, none)
    }

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

// TODO
// this belongs to the Analysis object
// but was moved here because of scala 2.10.1 compiler bug
object AnalysisMaker {
  def apply(str: String, done: Boolean): Option[String ⇒ Analysis] =
    Analysis.make(str, done)
}

final class AnalysisBuilder(infos: List[Info]) {

  def size = infos.size

  def +(info: Int ⇒ Info) = new AnalysisBuilder(info(infos.size + 1) :: infos)

  def done: String ⇒ Analysis = id ⇒ new Analysis(id, infos.reverse.zipWithIndex map {
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

  def decode: Option[Analysis] = Analysis.decodeInfos(encoded) map { infos ⇒
    new Analysis(id, infos, done, none)
  }
}

private[analyse] object RawAnalysis {

  import lila.db.Tube
  import play.api.libs.json._

  private[analyse] lazy val tube = Tube(Json.reads[RawAnalysis], Json.writes[RawAnalysis])
}
