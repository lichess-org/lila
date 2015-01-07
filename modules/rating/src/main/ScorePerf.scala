package lila.rating

import org.joda.time.DateTime
import reactivemongo.bson.BSONDocument

import lila.db.BSON

case class ScorePerf(
    nb: Int,
    recent: List[Int],
    latest: Option[DateTime]) {

  def averageScore: Option[Int] = recent.nonEmpty option {
    scala.math.round {
      recent.sum.toFloat / recent.size
    }
  }

  def progress: Int = ~recent.headOption.flatMap { head =>
    recent.lastOption map (head-)
  }

  def add(rating: Int, date: DateTime): ScorePerf = copy(
    nb = nb + 1,
    recent = (rating :: recent) take ScorePerf.recentMaxSize,
    latest = date.some)

  def nonEmpty = nb > 0
}

case object ScorePerf {

  val default = ScorePerf(0, Nil, None)

  val recentMaxSize = 12

  implicit val scorePerfBSONHandler = new BSON[ScorePerf] {

    import Glicko.glickoBSONHandler

    def reads(r: BSON.Reader): ScorePerf = ScorePerf(
      nb = r intD "nb",
      latest = r dateO "la",
      recent = r intsD "re")

    def writes(w: BSON.Writer, o: ScorePerf) = BSONDocument(
      "nb" -> w.int(o.nb),
      "re" -> w.intsO(o.recent),
      "la" -> o.latest.map(w.date))
  }
}
