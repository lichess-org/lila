package lila.analyse

import lila.db.BSON
import lila.db.dsl.given
import reactivemongo.api.bson.*
import chess.Ply

object AnalyseBsonHandlers:

  given BSON[Analysis] with
    def reads(r: BSON.Reader) =
      val startPly = Ply(r intD "ply")
      val raw      = r str "data"
      Analysis(
        id = r str "_id",
        studyId = r.getO[StudyId]("studyId"),
        infos = Info.decodeList(raw, startPly) err s"Invalid analysis data $raw",
        startPly = startPly,
        date = r date "date",
        fk = r strO "fk"
      )
    def writes(w: BSON.Writer, a: Analysis) =
      BSONDocument(
        "_id"     -> a.id,
        "studyId" -> a.studyId,
        "data"    -> Info.encodeList(a.infos),
        "ply"     -> w.intO(a.startPly.value),
        "date"    -> w.date(a.date),
        "fk"      -> a.fk
      )

  given engineHandler: BSONDocumentHandler[ExternalEngine] = Macros.handler
