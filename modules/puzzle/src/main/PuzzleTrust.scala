package lila.puzzle

import lila.core.perf.UserWithPerfs
import lila.db.dsl.{ *, given }
import lila.core.perm.Granter

final private class PuzzleTrustApi(colls: PuzzleColls, userApi: lila.core.user.UserApi)(using Executor):

  def vote(user: User, round: PuzzleRound, vote: Boolean): Fu[Option[Int]] =
    userApi
      .withPerfs(user)
      .flatMap: user =>
        val w = base(user) + {
          // more trust when vote != win
          if vote == round.win.value then -2 else 2
        }
        // distrust provisional ratings and distant ratings
        (w > 0)
          .so:
            user.perfs.puzzle.glicko.establishedIntRating.fold(fuccess(-2)): userRating =>
              colls
                .puzzle(_.primitiveOne[Float]($id(round.id.puzzleId), s"${Puzzle.BSONFields.glicko}.r"))
                .map:
                  _.fold(-2): puzzleRating =>
                    (math.abs(puzzleRating - userRating.value) > 300).so(-4)
          .dmap(w + _)
      .dmap(_.some.filter(0 <))

  def theme(user: User): Fu[Option[Int]] =
    userApi
      .withPerfs(user)
      .map: user =>
        base(user).some.filter(0 <)

  private def base(user: UserWithPerfs): Int = {
    seniorityBonus(user.user) +
      ratingBonus(user) +
      titleBonus(user.user) +
      patronBonus(user.user) +
      modBonus(user.user) +
      lameBonus(user.user)
  }.toInt

  // 0 days = 0
  // 1 month = 1
  // 1 year = 3.46
  // 2 years = 4.89
  private def seniorityBonus(user: User) =
    math.sqrt(daysBetween(user.createdAt, nowInstant).toDouble / 30).atMost(5)

  private def titleBonus(user: User) = user.hasTitle.so(20)

  // 1000 = 0
  // 1500 = 0
  // 1800 = 1
  // 3000 = 5
  private def ratingBonus(user: UserWithPerfs) = user.perfs.standard.glicko.establishedIntRating
    .so { rating =>
      (rating.value - 1500) / 300
    }
    .atLeast(0)

  private def patronBonus(user: User) =
    val planMonths: Option[Int] = user.plan.active.option(user.plan.months)
    (~planMonths * 5).atMost(15)

  private def modBonus(user: User) =
    if Granter.ofUser(_.PuzzleCurator)(user) then 100
    else if user.isAdmin then 50
    else if user.isVerified then 30
    else 0

  private def lameBonus(user: User) =
    if user.lameOrTroll then -30 else 0
