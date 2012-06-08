package lila
package user

import chess.{ EloCalculator, Color }
import game.{ GameRepo, DbGame }
import http.Context
import bookmark.BookmarkApi

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
    winChart: Option[WinChart]) {

  def nbRated = user.nbRatedGames

  def percentRated: Int = math.round(nbRated / user.nbGames.toFloat * 100)
}

object UserInfo {

  def apply(
    userRepo: UserRepo,
    gameRepo: GameRepo,
    eloCalculator: EloCalculator,
    eloChartBuilder: User ⇒ IO[Option[EloChart]])(
      user: User,
      bookmarkApi: BookmarkApi,
      ctx: Context): IO[UserInfo] = for {
    rank ← (user.elo >= 1500).fold(
      userRepo rank user flatMap { rank ⇒
        userRepo.countEnabled map { nbUsers ⇒
          Some(rank -> nbUsers)
        }
      },
      io(None))
    nbWin ← gameRepo count (_ win user)
    nbDraw ← gameRepo count (_ draw user)
    nbLoss ← gameRepo count (_ loss user)
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
    winChart = winChart)
}
