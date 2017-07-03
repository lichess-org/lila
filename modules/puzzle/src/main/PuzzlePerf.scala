package lila.puzzle

import org.goochjs.glicko2.Rating
import org.joda.time.DateTime

import lila.rating.Glicko

case class PuzzlePerf(glicko: Glicko, nb: Int, latest: Option[DateTime]) {

  def intRating = glicko.rating.toInt

  def add(g: Glicko, date: DateTime): PuzzlePerf = copy(
    glicko = g.cap,
    nb = nb + 1,
    latest = date.some
  )

  def add(r: Rating, date: DateTime): Option[PuzzlePerf] = {
    val glicko = Glicko(r.getRating, r.getRatingDeviation, r.getVolatility)
    glicko.sanityCheck option add(glicko, date)
  }

  def addOrReset(monitor: lila.mon.IncPath, msg: => String)(r: Rating, date: DateTime): PuzzlePerf = add(r, date) | {
    lila.log("rating").error(s"Crazy Glicko2 $msg")
    lila.mon.incPath(monitor)()
    add(Glicko.default, date)
  }

  def toRating(date: DateTime) = new Rating(
    math.max(Glicko.minRating, glicko.rating),
    glicko.age(latest, date).cap.deviation,
    glicko.volatility,
    nb
  )

  def isEmpty = nb == 0
}

case object PuzzlePerf {

  val default = PuzzlePerf(Glicko.default, 0, None)

  import lila.db.BSON

  implicit val puzzlePerfBSONHandler = new BSON[PuzzlePerf] {

    import Glicko.glickoBSONHandler
    import reactivemongo.bson.BSONDocument

    def reads(r: BSON.Reader): PuzzlePerf = PuzzlePerf(
      glicko = r.getO[Glicko]("gl") | Glicko.default,
      nb = r intD "nb",
      latest = r dateO "la"
    )

    def writes(w: BSON.Writer, o: PuzzlePerf) = BSONDocument(
      "gl" -> o.glicko,
      "nb" -> w.int(o.nb),
      "la" -> o.latest.map(w.date)
    )
  }
}
