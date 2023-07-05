package lila.rating

import reactivemongo.api.bson.{ BSONDocument, Macros }

import lila.db.BSON
import lila.db.dsl.given
import reactivemongo.api.bson.BSONDocumentHandler

case class Perf(
    glicko: Glicko,
    nb: Int,
    recent: List[IntRating],
    latest: Option[Instant]
):

  export glicko.{ intRating, intDeviation, rankable, clueless, provisional, established }

  def progress: IntRatingDiff = {
    for
      head <- recent.headOption
      last <- recent.lastOption
    yield IntRatingDiff(head.value - last.value)
  } | IntRatingDiff(0)

  def add(g: Glicko, date: Instant): Perf =
    val capped = g.cap
    copy(
      glicko = capped,
      nb = nb + 1,
      recent = updateRecentWith(capped),
      latest = date.some
    )

  def add(r: glicko2.Rating, date: Instant): Option[Perf] =
    val newGlicko = Glicko(
      rating = r.rating
        .atMost(glicko.rating + Glicko.maxRatingDelta)
        .atLeast(glicko.rating - Glicko.maxRatingDelta),
      deviation = r.ratingDeviation,
      volatility = r.volatility
    )
    newGlicko.sanityCheck option add(newGlicko, date)

  def addOrReset(
      monitor: lila.mon.CounterPath,
      msg: => String
  )(r: glicko2.Rating, date: Instant): Perf =
    add(r, date) | {
      lila.log("rating").error(s"Crazy Glicko2 $msg")
      monitor(lila.mon).increment()
      add(Glicko.default, date)
    }

  def refund(points: Int): Perf =
    val newGlicko = glicko refund points
    copy(
      glicko = newGlicko,
      recent = updateRecentWith(newGlicko)
    )

  private def updateRecentWith(glicko: Glicko) =
    if nb < 10 then recent
    else (glicko.intRating :: recent) take Perf.recentMaxSize

  def clearRecent = copy(recent = Nil)

  def toRating =
    glicko2.Rating(
      math.max(Glicko.minRating.value, glicko.rating),
      glicko.deviation,
      glicko.volatility,
      nb,
      latest
    )

  def isEmpty  = latest.isEmpty
  def nonEmpty = !isEmpty

  def showRatingProvisional = glicko.display

case object Perf:

  opaque type Key = String
  object Key extends OpaqueString[Key]

  opaque type Id = Int
  object Id extends OpaqueInt[Id]

  case class Typed(perf: Perf, perfType: PerfType)
  def typed(pt: PerfType, perf: Perf) = new Typed(perf, pt)

  val default = Perf(Glicko.default, 0, Nil, None)

  /* Set a latest date as a hack so that these are written to the db even though there are no games */
  val defaultManaged       = Perf(Glicko.defaultManaged, 0, Nil, nowInstant.some)
  val defaultManagedPuzzle = Perf(Glicko.defaultManagedPuzzle, 0, Nil, nowInstant.some)
  val defaultBot           = Perf(Glicko.defaultBot, 0, Nil, nowInstant.some)

  val recentMaxSize = 12

  trait PuzPerf:
    val score: Int
    val runs: Int
    def nonEmpty = runs > 0
    def option   = nonEmpty option this

  case class Storm(score: Int, runs: Int) extends PuzPerf
  object Storm:
    val default = Storm(0, 0)

  case class Racer(score: Int, runs: Int) extends PuzPerf
  object Racer:
    val default = Racer(0, 0)

  case class Streak(score: Int, runs: Int) extends PuzPerf
  object Streak:
    val default = Streak(0, 0)

  given BSONDocumentHandler[Perf] = new BSON[Perf]:

    import Glicko.given

    def reads(r: BSON.Reader): Perf =
      val p = Perf(
        glicko = r.getO[Glicko]("gl") | Glicko.default,
        nb = r intD "nb",
        latest = r dateO "la",
        recent = ~r.getO[List[IntRating]]("re")
      )
      p.copy(glicko = p.glicko.copy(deviation = Glicko.liveDeviation(p, reverse = false)))

    def writes(w: BSON.Writer, o: Perf) =
      BSONDocument(
        "gl" -> o.glicko.copy(deviation = Glicko.liveDeviation(o, reverse = true)),
        "nb" -> w.int(o.nb),
        "re" -> w.listO(o.recent),
        "la" -> o.latest.map(w.date)
      )

  given BSONDocumentHandler[Storm]  = Macros.handler[Storm]
  given BSONDocumentHandler[Racer]  = Macros.handler[Racer]
  given BSONDocumentHandler[Streak] = Macros.handler[Streak]
