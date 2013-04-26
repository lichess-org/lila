package lila.app
package mashup

import chess.{ EloCalculator, Color }
import lila.game.{ GameRepo, Game }
import lila.user.{ User, UserRepo, Context, EloChart }
import lila.bookmark.BookmarkApi
import lila.security.UserSpy

case class UserInfo(
    user: User,
    rank: Option[(Int, Int)],
    nbPlaying: Int,
    nbWithMe: Option[Int],
    nbBookmark: Int,
    eloWithMe: Option[List[(String, Int)]],
    eloChart: Option[EloChart],
    spy: Option[UserSpy]) {

  def nbRated = user.nbRatedGames

  def percentRated: Int = math.round(nbRated / user.nbGames.toFloat * 100)
}

object UserInfo {

  private val rankMinElo = 1800

  def apply(
    countUsers: () ⇒ Fu[Int],
    eloCalculator: EloCalculator,
    eloChartBuilder: User ⇒ Fu[Option[EloChart]])(
      user: User,
      bookmarkApi: BookmarkApi,
      userSpy: Option[String ⇒ Fu[UserSpy]],
      ctx: Context): Fu[UserInfo] = for {
    rank ← (user.elo >= rankMinElo) ?? {
      UserRepo rank user flatMap { rank ⇒
        countUsers() map { nbUsers ⇒ (rank -> nbUsers).some }
      }
    }
    nbPlaying ← (ctx is user) ?? {
      GameRepo count (_ notFinished user.id) map (_.some)
    }
    nbWithMe ← ctx.me.filter(user!=) zmap { me ⇒
      GameRepo count (_.opponents(user, me)) map (_.some)
    }
    nbBookmark ← bookmarkApi countByUser user
    eloChart ← eloChartBuilder(user)
    eloWithMe = ctx.me.filter(user !=) map { me ⇒
      List(
        "win" -> eloCalculator.diff(me, user, Color.White.some),
        "draw" -> eloCalculator.diff(me, user, None),
        "loss" -> eloCalculator.diff(me, user, Color.Black.some))
    }
    spy ← (userSpy zmap (_(user.id) map (_.some)))
  } yield new UserInfo(
    user = user,
    rank = rank,
    nbPlaying = ~nbPlaying,
    nbWithMe = nbWithMe,
    nbBookmark = nbBookmark,
    eloWithMe = eloWithMe,
    eloChart = eloChart,
    spy = spy)
}
