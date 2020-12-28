package lila.puzzle

import scala.concurrent.ExecutionContext
import lila.user.User

case class PuzzleDashboard(
    themeResults: Map[PuzzleTheme.Key, PuzzleDashboard.ThemeResult],
    nb: Int,
    nbWins: Int,
    nbRetriable: Int,
    nbRetried: Int,
    nbUpvote: Int,
    nbDownvote: Int,
    themesSet: Vector[PuzzleTheme.Key]
) {
  def nbFails = nb - nbWins
}

object PuzzleDashboard {

  case class ThemeResult(nb: Int, wins: Int, puzzleRatingAvg: Int) {

    def performance = puzzleRatingAvg - 500 + 1000 * (wins / nb)

    def losses = nb - wins

    def unclear = wins < 3 || losses < 3
  }
}

final class PuzzleDashboardApi(
    colls: PuzzleColls
)(implicit ec: ExecutionContext) {

  /*
   * {$lookup:{from:'puzzle2_puzzle',as:'puzzle',
   * let:{pid:{$arrayElemAt:[{$split:['$_id',':']},1]}},
   * pipeline:[{$match:{$expr:{$eq:['$_id','$$pid']}}},{$project:{themes:1,rating:{$round:'$glicko.r'}}}]}},
   * {$unwind:'$puzzle'},
   * {$unwind:'$puzzle.themes'},
   * {$group:{_id:'$puzzle.themes',nb:{$sum:1},w:{$sum:{$cond:['$w',1,0]}},l:{$sum:{$cond:['$w',0,1]}},pr:{$avg:'$puzzle.rating'}}},
   * {$addFields:{ratio:{$divide:['$w',{$sum:['$w','$l']}]}}},
   * {$sort:{ratio:-1}}
   */
  def apply(u: User): Fu[PuzzleDashboard] =
    colls.round.aggregateList(10_000) { framework =>
      import framework._
      Project($doc("u" -> u.id)) -> List(
        PipelineOperator(
          $doc(
            "$lookup" -> $doc(
              "from" -> colls.puzzle.name,
              "as"   -> "puzzle",
              "let"  -> $doc(
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
        ),
        Unwind("puzzle"),
        Unwind("puzzle.themes"),
        GroupField("puzzle.themes")(
          "nb" -> SumAll,
          "wins" -> Sum($doc("$cond" -> $arr("w", 1, 0))),
          "rating" -> Avg("puzzle.rating")
        )
      )
      }.map { docs =>
        for {
          doc <- docs
          themeStr <- doc.string("_id")
          themeKey <- PuzzleTheme find themeStr
          nb <- doc.int("nb")
          wins <- doc.int("wins")
          rating <- doc.double("rating").toInt
        } yield themeKey -> ThemeResult(nb, wins, rating)
      }.map(_.toMap)
        .map { themeResults =>
          PuzzleDashboard(themeResults

}
