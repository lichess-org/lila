package lila.puzzle

import org.joda.time.DateTime
import reactivemongo.api.bson.BSONNull
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext

import lila.db.dsl._
import lila.memo.CacheApi
import lila.user.User

case class PuzzleDashboard(
    global: PuzzleDashboard.Results,
    byTheme: Map[PuzzleTheme.Key, PuzzleDashboard.Results]
) {

  import PuzzleDashboard._
  import BsonHandlers._

  lazy val clearThemes = byTheme.view.filter { case (_, results) =>
    results.clear
  }.toList

  lazy val (weakThemes, strongThemes) = {
    val all = byTheme.toList.sortBy { case (_, res) =>
      (res.performance, -res.nb)
    }
    val (clear, unclear) = all.partition(_._2.clear)
    if (clear.size >= topThemesNb * 2)
      (
        clear take topThemesNb,
        clear takeRight topThemesNb
      )
    else
      (
        clear take topThemesNb,
        clear takeRight topThemesNb
      )
  }
}

object PuzzleDashboard {

  type Days = Int

  val dayChoices = List(1, 2, 3, 7, 10, 14, 21, 30, 60, 90)

  val topThemesNb = 5

  case class Results(nb: Int, wins: Int, fixed: Int, puzzleRatingAvg: Int) {

    def firstWins = wins - fixed
    def unfixed   = nb - wins
    def failed    = fixed + unfixed

    def winPercent      = wins * 100 / nb
    def fixedPercent    = fixed * 100 / nb
    def firstWinPercent = firstWins * 100 / nb

    def performance = puzzleRatingAvg - 500 + math.round(1000 * (firstWins.toFloat / nb))

    def clear   = wins >= 3 && failed >= 3
    def unclear = !clear
  }
}

final class PuzzleDashboardApi(
    colls: PuzzleColls,
    cacheApi: CacheApi
)(implicit ec: ExecutionContext) {

  import PuzzleDashboard._

  def apply(u: User, days: Days): Fu[Option[PuzzleDashboard]] = cache.get(u.id -> days)

  private val cache =
    cacheApi[(User.ID, Days), Option[PuzzleDashboard]](1024, "puzzle.dashboard") {
      _.expireAfterWrite(10 seconds).buildAsyncFuture { case (userId, days) =>
        compute(userId, days)
      }
    }

  private def compute(userId: User.ID, days: Days): Fu[Option[PuzzleDashboard]] =
    colls.round {
      _.aggregateOne() { framework =>
        import framework._
        val resultsGroup = List(
          "nb"     -> SumAll,
          "wins"   -> Sum(countField("w")),
          "fixes"  -> Sum(countField("f")),
          "rating" -> AvgField("puzzle.rating")
        )
        Match($doc("u" -> userId, "d" $gt DateTime.now.minusDays(days))) -> List(
          Sort(Descending("d")),
          Limit(10_000),
          PipelineOperator(puzzleLookup),
          Unwind("puzzle"),
          Facet(
            List(
              "global" -> List(Group(BSONNull)(resultsGroup: _*)),
              "byTheme" -> List(
                Unwind("puzzle.themes"),
                Match(irrelevantThemes),
                GroupField("puzzle.themes")(resultsGroup: _*)
              )
            )
          )
        )
      }
        .map { r =>
          for {
            result     <- r
            globalDocs <- result.getAsOpt[List[Bdoc]]("global")
            globalDoc  <- globalDocs.headOption
            global     <- readResults(globalDoc)
            themeDocs  <- result.getAsOpt[List[Bdoc]]("byTheme")
            byTheme = for {
              doc      <- themeDocs
              themeStr <- doc.string("_id")
              theme    <- PuzzleTheme find themeStr
              results  <- readResults(doc)
            } yield theme.key -> results
          } yield PuzzleDashboard(
            global = global,
            byTheme = byTheme.toMap
          )
        }
        .dmap(_.filter(_.global.nb > 0))
    }

  private def countField(field: String) = $doc("$cond" -> $arr("$" + field, 1, 0))

  private val puzzleLookup =
    $doc(
      "$lookup" -> $doc(
        "from" -> colls.puzzle.name.value,
        "as"   -> "puzzle",
        "let" -> $doc(
          "pid" -> $doc("$arrayElemAt" -> $arr($doc("$split" -> $arr("$_id", ":")), 1))
        ),
        "pipeline" -> $arr(
          $doc(
            "$match" -> $doc(
              "$expr" -> $doc(
                $doc("$eq" -> $arr("$_id", "$$pid"))
              )
            )
          ),
          $doc("$project" -> $doc("themes" -> true, "rating" -> "$glicko.r"))
        )
      )
    )

  private def readResults(doc: Bdoc) = for {
    nb     <- doc.int("nb")
    wins   <- doc.int("wins")
    fixes  <- doc.int("fixes")
    rating <- doc.double("rating")
  } yield Results(nb, wins, fixes, rating.toInt)

  val irrelevantThemes = $doc(
    "puzzle.themes" $nin List(
      PuzzleTheme.oneMove,
      PuzzleTheme.short,
      PuzzleTheme.long,
      PuzzleTheme.veryLong,
      PuzzleTheme.mateIn1,
      PuzzleTheme.mateIn2,
      PuzzleTheme.mateIn3,
      PuzzleTheme.mateIn4,
      PuzzleTheme.mateIn5,
      PuzzleTheme.equality,
      PuzzleTheme.advantage,
      PuzzleTheme.crushing
    ).map(_.key.value)
  )
}
