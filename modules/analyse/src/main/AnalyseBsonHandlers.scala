package lila.analyse

import chess.Ply
import reactivemongo.api.bson.*

import lila.db.BSON
import lila.db.dsl.{ *, given }
import lila.tree.{ Analysis, Engine, Info, Eval, evals }
import lila.tree.Analysis.EngineId

object AnalyseBsonHandlers:

  given BSONWriter[Analysis.Id] = BSONWriter(id => BSONString(id.value))
  given BSONDocumentHandler[Engine] = Macros.handler

  given BSON[Analysis] with
    def reads(r: BSON.Reader) =
      val startPly = Ply(r.intD("ply"))
      val raw = r.str("data")
      def id =
        def getId[Id: BSONReader]: Id = r.get[Id]("_id")
        r.getO[StudyId]("studyId") match
          case Some(studyId) => Analysis.Id(studyId, getId[StudyChapterId])
          case None => Analysis.Id(getId[GameId])
      val engine = r.getO[Engine]("engine") | Engine.default.copy(
        nodesPerMove = r.intO("npm") | Engine.default.nodesPerMove,
        fishnetKey = r.getO[Analysis.FishnetKey]("fk")
      )
      Analysis(
        id = id,
        infos = InfoBson.decodeList(raw, startPly).err(s"Invalid analysis data $raw"),
        startPly = startPly,
        date = r.date("date"),
        engine = engine
      )
    def writes(w: BSON.Writer, a: Analysis) =
      bdoc(
        "_id" -> a.id,
        "studyId" -> a.studyId,
        "data" -> a.infos,
        "ply" -> w.intO(a.startPly.value),
        "date" -> w.date(a.date),
        "engine" -> a.engine
      )

  given engineHandler: BSONDocumentHandler[ExternalEngine] = Macros.handler
  given BSONWriter[List[Info]] = BSONWriter: infos =>
    BSONString(InfoBson.encodeList(infos))

  object InfoBson:

    import chess.eval.{ Score, WhiteScore }
    import chess.eval.Eval.{ Cp, Mate }
    import chess.format.pgn.SanStr
    import chess.format.Uci

    private val separator = ","
    private val listSeparator = ";"

    def encodeList(infos: List[Info]): String = infos.map(encodeInfo).mkString(listSeparator)

    def decodeList(str: String, fromPly: Ply): Option[List[Info]] =
      str
        .split(listSeparator)
        .toList
        .zipWithIndex
        .traverse((infoStr, index) => decode(fromPly + index + 1, infoStr))

    private def encodeInfo(i: Info): String =
      List(
        i.best.so(_.chars),
        i.variation.take(Info.LineMaxPlies).mkString(" "),
        i.eval.score.so(_.white.fold(_ => "", encodeMate, mateGiven)),
        i.cp.so(_.value.toString)
      ).dropWhile(_.isEmpty).reverse.mkString(separator)

    // Always with explicit +/-. "0" is the legacy encoding of Mate(0).
    private val mateGiven = "+0"
    private def encodeMate(m: Mate) = if m.value == 0 then "-0" else m.value.toString

    private def strScore(cp: String, mate: String): Option[WhiteScore] =
      Cp.from(cp.toIntOption)
        .map(Score.Cp(_))
        .orElse:
          if mate == mateGiven then Score.MateGiven.some
          else Mate.from(mate.toIntOption).map(Score.Mate(_))
        .map(WhiteScore.fromWhite(_))

    private def decode(ply: Ply, str: String): Option[Info] =
      str.split(separator) match
        case Array() => Info(ply, evals.empty, Nil).some
        case Array(cp) => Info(ply, Eval(strScore(cp, ""), None), Nil).some
        case Array(cp, ma) => Info(ply, Eval(strScore(cp, ma), None), Nil).some
        case Array(cp, ma, va) =>
          Info(ply, Eval(strScore(cp, ma), None), SanStr.from(va.split(' ').toList)).some
        case Array(cp, ma, va, be) =>
          Info(
            ply,
            Eval(strScore(cp, ma), Uci.Move.fromChars(be)),
            SanStr.from(va.split(' ').toList)
          ).some
        case _ => none
