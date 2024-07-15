package lila.rating

import reactivemongo.api.bson.{ BSONDocument, BSONDocumentHandler, Macros }

import lila.core.perf.{ Perf, PuzPerf }
import lila.core.rating.Glicko
import lila.db.BSON
import lila.db.dsl.given
import lila.rating.GlickoExt.*

object PerfExt:

  extension (p: Perf)

    def addRating(g: Glicko, date: Instant): Perf =
      val capped = g.cap
      p.copy(
        glicko = capped,
        nb = p.nb + 1,
        recent = p.updateRecentWith(capped),
        latest = date.some
      )

    def addRating(r: glicko2.Rating, date: Instant): Option[Perf] =
      val newGlicko = Glicko(
        rating = r.rating
          .atMost(p.glicko.rating + lila.rating.Glicko.maxRatingDelta)
          .atLeast(p.glicko.rating - lila.rating.Glicko.maxRatingDelta),
        deviation = r.ratingDeviation,
        volatility = r.volatility
      )
      newGlicko.sanityCheck.option(p.addRating(newGlicko, date))

    def addOrReset(
        monitor: lila.mon.CounterPath,
        msg: => String
    )(r: glicko2.Rating, date: Instant): Perf =
      p.addRating(r, date) | {
        lila.log("rating").error(s"Crazy Glicko2 $msg")
        monitor(lila.mon).increment()
        p.addRating(lila.rating.Glicko.default, date)
      }

    def refund(points: Int): Perf =
      val newGlicko = p.glicko.copy(rating = p.glicko.rating + points)
      p.copy(
        glicko = newGlicko,
        recent = p.updateRecentWith(newGlicko)
      )

    private def updateRecentWith(glicko: Glicko) =
      if p.nb < 10 then p.recent
      else (glicko.intRating :: p.recent).take(Perf.recentMaxSize)

    def clearRecent = p.copy(recent = Nil)

    def toRating =
      glicko2.Rating(
        math.max(lila.rating.Glicko.minRating.value, p.glicko.rating),
        p.glicko.deviation,
        p.glicko.volatility,
        p.nb,
        p.latest
      )

    def showRatingProvisional = p.glicko.display
    def established           = p.glicko.established

object Perf:

  val default = new Perf(lila.rating.Glicko.default, 0, Nil, None)

  /* Set a latest date as a hack so that these are written to the db even though there are no games */
  val defaultManaged       = new Perf(lila.rating.Glicko.defaultManaged, 0, Nil, nowInstant.some)
  val defaultManagedPuzzle = new Perf(lila.rating.Glicko.defaultManagedPuzzle, 0, Nil, nowInstant.some)
  val defaultBot           = new Perf(lila.rating.Glicko.defaultBot, 0, Nil, nowInstant.some)

  val recentMaxSize = 12

  given perfHandler: BSONDocumentHandler[Perf] = new BSON[Perf]:

    import lila.rating.Glicko.glickoHandler

    def reads(r: BSON.Reader): Perf =
      val p = new Perf(
        glicko = r.getO[Glicko]("gl") | lila.rating.Glicko.default,
        nb = r.intD("nb"),
        latest = r.dateO("la"),
        recent = ~r.getO[List[IntRating]]("re")
      )
      p.copy(glicko = p.glicko.copy(deviation = lila.rating.Glicko.liveDeviation(p, reverse = false)))

    def writes(w: BSON.Writer, o: Perf) =
      BSONDocument(
        "gl" -> o.glicko.copy(deviation = lila.rating.Glicko.liveDeviation(o, reverse = true)),
        "nb" -> w.int(o.nb),
        "re" -> w.listO(o.recent),
        "la" -> o.latest.map(w.date)
      )

  given BSONDocumentHandler[PuzPerf] = Macros.handler[PuzPerf]
