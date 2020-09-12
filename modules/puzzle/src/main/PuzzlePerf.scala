package lila.puzzle

import org.goochjs.glicko2.Rating

import lila.rating.Glicko

case class PuzzlePerf(glicko: Glicko, nb: Int) {

  def intRating = glicko.rating.toInt

  def add(g: Glicko): PuzzlePerf =
    copy(
      glicko = g.cap,
      nb = nb + 1
    )

  def add(r: Rating): Option[PuzzlePerf] = {
    val glicko = Glicko(r.getRating, r.getRatingDeviation, r.getVolatility)
    glicko.sanityCheck option add(glicko)
  }

  def addOrReset(monitor: lila.mon.CounterPath, msg: => String)(r: Rating): PuzzlePerf =
    add(r) | {
      lila.log("rating").error(s"Crazy Glicko2 $msg")
      monitor(lila.mon).increment()
      add(Glicko.default)
    }

  def toRating =
    new Rating(
      math.max(Glicko.minRating, glicko.rating),
      glicko.deviation,
      glicko.volatility,
      nb
    )

  def isEmpty = nb == 0
}

case object PuzzlePerf {

  val default = PuzzlePerf(Glicko.default, 0)

  import lila.db.BSON

  implicit val puzzlePerfBSONHandler = new BSON[PuzzlePerf] {

    import Glicko.glickoBSONHandler
    import reactivemongo.api.bson.BSONDocument

    def reads(r: BSON.Reader): PuzzlePerf =
      PuzzlePerf(
        glicko = r.getO[Glicko]("gl") | Glicko.default,
        nb = r intD "nb"
      )

    def writes(w: BSON.Writer, o: PuzzlePerf) =
      BSONDocument(
        "gl" -> o.glicko,
        "nb" -> w.int(o.nb)
      )
  }
}
