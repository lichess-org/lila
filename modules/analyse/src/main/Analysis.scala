package lila.analyse

import chess.Color
import scalaz.NonEmptyList

import org.joda.time.DateTime

sealed trait AnyAnalysis {
  val id: Analysis.ID
  val startPly: Int
  val uid: Option[String] // requester lichess ID
  val by: Option[String] // analyser lichess ID
  val date: DateTime

  def infoOptions: NonEmptyList[Option[Info]]

  def requestedBy = uid | "lichess"

  def providedBy = by | "lichess"

  def providedByLichess = by exists (_ startsWith "lichess-")
}

case class Analysis(
    id: Analysis.ID,
    infos: NonEmptyList[Info],
    startPly: Int,
    uid: Option[String],
    by: Option[String],
    date: DateTime
) extends AnyAnalysis {

  lazy val infoOptions = infos map Some.apply

  lazy val infoAdvices: InfoAdvices = {
    (Info.start(startPly) :: infos.list) sliding 2 collect {
      case List(prev, info) => info -> {
        info.hasVariation ?? Advice(prev, info)
      }
    }
  }.toList

  lazy val advices: List[Advice] = infoAdvices.flatMap(_._2)

  // ply -> UCI
  def bestMoves: Map[Int, String] = infos.list.flatMap { i =>
    i.best map { b => i.ply -> b.keys }
  }.toMap

  def summary: List[(Color, List[(Advice.Judgment, Int)])] = Color.all map { color =>
    color -> (Advice.Judgment.all map { judgment =>
      judgment -> (advices count { adv =>
        adv.color == color && adv.judgment == judgment
      })
    })
  }
}

case class PartialAnalysis(
    id: Analysis.ID,
    infoOptions: NonEmptyList[Option[Info]],
    startPly: Int,
    uid: Option[String],
    by: Option[String],
    date: DateTime
) extends AnyAnalysis {

  def nbEmptyInfos = infoOptions.list.count(_.isEmpty)
  def emptyRatio: Double = nbEmptyInfos.toDouble / infoOptions.size
}

object Analysis {

  import lila.db.BSON
  import reactivemongo.bson._

  type ID = String

  // analysis that is not done computing, some infos are missing
  // case class Partial(
  //   id: Analysis.ID,
  //   infos: List[Option[Info]],
  //   startPly: Int
  // )

  private[analyse] implicit val analysisBSONHandler = new BSON[Analysis] {
    def reads(r: BSON.Reader) = {
      val startPly = r intD "ply"
      val raw = r str "data"
      Analysis(
        id = r str "_id",
        infos = Info.decodeList(raw, startPly) err s"Invalid analysis data $raw",
        startPly = startPly,
        uid = r strO "uid",
        by = r strO "by",
        date = r date "date"
      )
    }
    def writes(w: BSON.Writer, o: Analysis) = BSONDocument(
      "_id" -> o.id,
      "data" -> Info.encodeList(o.infos),
      "ply" -> w.intO(o.startPly),
      "uid" -> o.uid,
      "by" -> o.by,
      "date" -> w.date(o.date)
    )
  }
}
