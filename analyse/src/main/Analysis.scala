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

  def builder = new AnalysisBuilder(Nil)

  val separator = " "

  import lila.db.Tube
  import play.api.libs.json._

  lazy val tube = Tube(
    reader = Reads[Analysis](js ⇒
      (for {
        obj ← js.asOpt[JsObject]
        rawAnalysis ← RawAnalysis.tube.read(obj).asOpt
        analysis ← rawAnalysis.decode
      } yield JsSuccess(analysis): JsResult[Analysis]) | JsError(Seq.empty)
    ),
    writer = Writes[Analysis](analysis ⇒
      RawAnalysis.tube.write(analysis.encode) getOrElse JsUndefined("[db] Can't write analysis " + analysis.id)
    )
  )
}

private[analyse] case class RawAnalysis(
    id: String,
    encoded: String,
    done: Boolean,
    fail: Option[String]) {

  def decode: Option[Analysis] = decodeInfos map { infos ⇒
    new Analysis(id, infos, done, none)
  }

  private def decodeInfos: Option[List[Info]] =
    (encoded.split(Analysis.separator).toList.zipWithIndex map {
      case (info, index) ⇒ Info.decode(index + 1, info)
    }).sequence.fold(
      err ⇒ { logger.warn("[analysis] " + err); none[List[Info]] },
      _.some
    )
}

private[analyse] object RawAnalysis {

  import lila.db.Tube
  import play.api.libs.json._

  lazy val tube = Tube(Json.reads[RawAnalysis], Json.writes[RawAnalysis])
}
