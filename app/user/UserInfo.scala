package lila
package user

import chess.{ EloCalculator, Color }
import game.{ GameRepo, DbGame }
import http.Context

import scalaz.effects._

case class UserInfo(
    user: User,
    rank: Option[(Int, Int)],
    eloWithMe: Option[List[(String, Int)]],
    eloChart: Option[EloChart]) {
}

object UserInfo {

  def apply(
    userRepo: UserRepo,
    gameRepo: GameRepo,
    eloCalculator: EloCalculator,
    eloChartBuilder: User ⇒ IO[Option[EloChart]])(
      user: User,
      ctx: Context): IO[UserInfo] = for {
    rank ← (user.elo >= 1500).fold(
      userRepo rank user flatMap { rank ⇒
        userRepo.countEnabled map { nbUsers ⇒
          Some(rank -> nbUsers)
        }
      },
      io(None))
    eloChart ← eloChartBuilder(user)
    eloWithMe = ctx.me.filter(user!=) map { me ⇒
      List(
        "win" -> eloCalculator.diff(me, user, Color.White.some),
        "draw" -> eloCalculator.diff(me, user, None),
        "loss" -> eloCalculator.diff(me, user, Color.Black.some))
    }
  } yield new UserInfo(
    user = user,
    rank = rank,
    eloWithMe = eloWithMe,
    eloChart = eloChart)
}
