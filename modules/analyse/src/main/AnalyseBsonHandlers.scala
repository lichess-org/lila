package lila.analyse

import lila.db.BSON
import lila.db.dsl._
import reactivemongo.api.bson._

object AnalyseBsonHandlers {

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

  implicit val winPercentHandler = percentAsIntHandler[WinPercent](_.value, WinPercent.apply)

  implicit val accuracyPercentHandler = percentAsIntHandler[AccuracyPercent](_.value, AccuracyPercent.apply)
}
