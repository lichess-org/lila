package lila
package user

import chess.{ EloCalculator, Color }
import game.{ GameRepo, DbGame }
import http.Context
import bookmark.BookmarkApi
import security.UserSpy

import scalaz.effects._

case class UserInfo(
    user: User,
    rank: Option[(Int, Int)],
    nbWin: Int,
    nbDraw: Int,
    nbLoss: Int,
    nbPlaying: Int,
    nbWithMe: Option[Int],
    nbBookmark: Int,
    eloWithMe: Option[List[(String, Int)]],
    eloChart: Option[EloChart],
    winChart: Option[WinChart],
    spy: Option[UserSpy]) {

  def nbRated = user.nbRatedGames

  def percentRated: Int = math.round(nbRated / user.nbGames.toFloat * 100)
}

object UserInfo {

  private val rankMinElo = 1700

  def apply(
    userRepo: UserRepo,
    countUsers: () => Int,
    gameRepo: GameRepo,
    eloCalculator: EloCalculator,
    eloChartBuilder: User ⇒ IO[Option[EloChart]])(
      user: User,
      bookmarkApi: BookmarkApi,
      userSpy: Option[String ⇒ IO[UserSpy]],
      ctx: Context): IO[UserInfo] = for {
    rank ← (user.elo >= rankMinElo).fold(
      userRepo rank user map { rank ⇒
        Some(rank -> countUsers())
      },
      io(None))
    nbWin = user.nbWins
    nbLoss = user.nbLosses
    nbDraw = user.nbDraws
    nbPlaying ← (ctx is user).fold(
      gameRepo count (_ notFinished user) map (_.some),
      io(none)
    )
    nbWithMe ← ctx.me.filter(user!=).fold(
      me ⇒ gameRepo count (_.opponents(user, me)) map (_.some),
      io(none)
    )
    nbBookmark = bookmarkApi countByUser user
    eloChart ← eloChartBuilder(user)
    winChart = (user.nbRatedGames > 0) option {
      new WinChart(nbWin, nbDraw, nbLoss)
    }
    eloWithMe = ctx.me.filter(user!=) map { me ⇒
      List(
        "win" -> eloCalculator.diff(me, user, Color.White.some),
        "draw" -> eloCalculator.diff(me, user, None),
        "loss" -> eloCalculator.diff(me, user, Color.Black.some))
    }
    spy ← userSpy.fold(_(user.id) map (_.some), io(none[UserSpy])): IO[Option[UserSpy]]
  } yield new UserInfo(
    user = user,
    rank = rank,
    nbWin = nbWin,
    nbDraw = nbDraw,
    nbLoss = nbLoss,
    nbPlaying = nbPlaying | 0,
    nbWithMe = nbWithMe,
    nbBookmark = nbBookmark,
    eloWithMe = eloWithMe,
    eloChart = eloChart,
    winChart = winChart,
    spy = spy)
}
