package lila.puzzle

import org.joda.time.DateTime
import org.joda.time.Days

import lila.db.dsl._
import lila.user.User

final private class PuzzleTrustApi(colls: PuzzleColls)(implicit ec: scala.concurrent.ExecutionContext) {

  import BsonHandlers._

  def vote(user: User, round: PuzzleRound, vote: Boolean): Fu[Int] = base(user, round) map {
    _ + {
      if (vote == round.win) -2 else 2
    }
  }

  def theme(user: User, round: PuzzleRound, theme: PuzzleTheme.Key, vote: Boolean): Fu[Int] =
    base(user, round)

  private def base(user: User, round: PuzzleRound): Fu[Int] =
    colls
      .puzzle(_.byId[Puzzle](round.id.puzzleId.value))
      .map {
        _ ?? { puzzle =>
          seniorityBonus(user) +
            ratingBonus(user) +
            titleBonus(user) +
            patronBonus(user) +
            nbGamesBonus(user) +
            nbPuzzlesBonus(user) +
            modBonus(user) +
            lameBonus(user)
        }
      }
      .dmap(_.toInt)

  // 0 days = 0
  // 1 month = 1
  // 1 year = 3.46
  // 2 years = 4.89
  private def seniorityBonus(user: User) =
    math.sqrt(Days.daysBetween(user.createdAt, DateTime.now).getDays.toDouble / 30)

  private def titleBonus(user: User) = user.hasTitle ?? 20

  // 1000 = 0
  // 1500 = 0
  // 1800 = 1
  // 3000 = 5
  private def ratingBonus(user: User) = user.perfs.standard.glicko.establishedIntRating.?? { rating =>
    (rating - 1500) / 300
  } atLeast 0

  private def patronBonus(user: User) = (~user.planMonths * 5) atMost 20

  // 0 games    = 0
  // 100 games  = 1
  // 200 games  = 2.41
  // 1000 games = 3.16
  private def nbGamesBonus(user: User) =
    math.sqrt(user.count.game / 100)

  private def nbPuzzlesBonus(user: User) =
    math.sqrt(user.perfs.puzzle.nb / 100)

  private def modBonus(user: User) =
    if (user.roles.exists(_ contains "ROLE_PUZZLE_CURATOR")) 100
    else if (user.isAdmin) 50
    else if (user.isVerified) 30
    else 0

  private def lameBonus(user: User) =
    if (user.lame) -10 else 0
}
