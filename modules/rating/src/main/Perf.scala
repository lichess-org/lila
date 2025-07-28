package lila.rating

import reactivemongo.api.bson.{ BSONDocument, BSONDocumentHandler, Macros }

import chess.IntRating
import chess.rating.glicko.Glicko
import lila.core.perf.{ Perf, PuzPerf }
import lila.db.BSON
import lila.db.dsl.given
import lila.rating.GlickoExt.*

object PerfExt:

  extension (p: Perf)

    def addOrReset(monitor: lila.mon.CounterPath, msg: => String)(player: Glicko, date: Instant): Perf =
      val newGlicko = player.copy(
        rating = player.rating
          .atMost(p.glicko.rating + lila.rating.Glicko.maxRatingDelta)
          .atLeast(p.glicko.rating - lila.rating.Glicko.maxRatingDelta)
      )
      def append(g: Glicko): Perf =
        val capped = g.cap
        p.copy(
          glicko = capped,
          nb = p.nb + 1,
          recent = p.updateRecentWith(capped),
          latest = date.some
        )
      if newGlicko.sanityCheck then append(newGlicko)
      else
        lila.log("rating").error(s"Crazy Glicko2 $msg")
        monitor(lila.mon).increment()
        append(lila.rating.Glicko.default)

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

    def toGlickoPlayer = chess.rating.glicko.Player(p.glicko.cap, p.nb, p.latest)

    def showRatingProvisional = p.glicko.display
    def established = p.glicko.established

object Perf:

  val default = new Perf(lila.rating.Glicko.default, 0, Nil, None)

  /* Set a latest date as a hack so that these are written to the db even though there are no games */
  val defaultManaged = new Perf(lila.rating.Glicko.defaultManaged, 0, Nil, nowInstant.some)
  val defaultManagedPuzzle = new Perf(lila.rating.Glicko.defaultManagedPuzzle, 0, Nil, nowInstant.some)
  val defaultBot = new Perf(lila.rating.Glicko.defaultBot, 0, Nil, nowInstant.some)

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
