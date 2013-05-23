package lila.app
package mashup

import chess.{ EloCalculator, Color }
import lila.game.{ GameRepo, Game }
import lila.user.{ User, UserRepo, Context, EloChart }
import lila.bookmark.BookmarkApi
import lila.relation.RelationApi
import lila.forum.PostApi

case class UserInfo(
    user: User,
    rank: Option[(Int, Int)],
    nbPlaying: Int,
    nbWithMe: Option[Int],
    nbBookmark: Int,
    eloWithMe: Option[List[(String, Int)]],
    eloChart: Option[EloChart],
    nbFollowing: Int,
    nbFollowers: Int,
    nbPosts: Int) {

  def nbRated = user.nbRatedGames

  def percentRated: Int = math.round(nbRated / user.nbGames.toFloat * 100)
}

object UserInfo {

  private val rankMinElo = 1800

  def apply(
    countUsers: () ⇒ Fu[Int],
    bookmarkApi: BookmarkApi,
    eloCalculator: EloCalculator,
    relationApi: RelationApi,
    postApi: PostApi)(user: User, ctx: Context): Fu[UserInfo] =
    ((user.elo >= rankMinElo) ?? {
      UserRepo rank user flatMap { rank ⇒
        countUsers() map { nbUsers ⇒ (rank -> nbUsers).some }
      }
    }) zip
      ((ctx is user) ?? {
        GameRepo count (_ notFinished user.id) map (_.some)
      }) zip
      (ctx.me.filter(user!=) ?? { me ⇒
        GameRepo count (_.opponents(user, me)) map (_.some)
      }) zip
      (bookmarkApi countByUser user) zip
      EloChart(user) zip
      relationApi.nbFollowing(user.id) zip
      relationApi.nbFollowers(user.id) zip 
      postApi.nbByUser(user.id) map {
        case (((((((rank, nbPlaying), nbWithMe), nbBookmark), eloChart), nbFollowing), nbFollowers), nbPosts) ⇒ new UserInfo(
          user = user,
          rank = rank,
          nbPlaying = ~nbPlaying,
          nbWithMe = nbWithMe,
          nbBookmark = nbBookmark,
          eloWithMe = ctx.me.filter(user !=) map { me ⇒
            List(
              "win" -> eloCalculator.diff(me, user, Color.White.some),
              "draw" -> eloCalculator.diff(me, user, None),
              "loss" -> eloCalculator.diff(me, user, Color.Black.some))
          },
          eloChart = eloChart,
          nbFollowing = nbFollowing,
          nbFollowers = nbFollowers,
          nbPosts = nbPosts)
      }
}
