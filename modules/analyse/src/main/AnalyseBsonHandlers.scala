package lila.analyse

import chess.Ply
import reactivemongo.api.bson.*

import lila.db.BSON
import lila.db.dsl.given
import lila.tree.{ Analysis, Info }

object AnalyseBsonHandlers:

  given BSONWriter[Analysis.Id] = BSONWriter(id => BSONString(id.value))

  given BSON[Analysis] with
    def reads(r: BSON.Reader) =
      val startPly = Ply(r.intD("ply"))
      val raw      = r.str("data")
      def id =
        def getId[Id: BSONReader]: Id = r.get[Id]("_id")
        r.getO[StudyId]("studyId") match
          case Some(studyId) => Analysis.Id(studyId, getId[StudyChapterId])
          case None          => Analysis.Id(getId[GameId])
      Analysis(
        id = id,
        infos = Info.decodeList(raw, startPly).err(s"Invalid analysis data $raw"),
        startPly = startPly,
        date = r.date("date"),
        fk = r.strO("fk"),
        nodesPerMove = r.intO("npm")
      )
    def writes(w: BSON.Writer, a: Analysis) =
      BSONDocument(
        "_id"     -> a.id,
        "studyId" -> a.studyId,
        "data"    -> Info.encodeList(a.infos),
        "ply"     -> w.intO(a.startPly.value),
        "date"    -> w.date(a.date),
        "fk"      -> a.fk,
        "npm"     -> a.nodesPerMove
      )

  given engineHandler: BSONDocumentHandler[ExternalEngine] = Macros.handler
