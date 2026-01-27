package views.user

import lila.app.UiEnv.{ *, given }
import lila.core.data.SafeJsonStr
import lila.core.perf.UserWithPerfs
import lila.perfStat.PerfStatData
import lila.rating.UserPerfsExt.best8Perfs
import lila.user.Profile.flagInfo

val bits = lila.user.ui.UserBits(helpers)
val noteUi = lila.user.ui.NoteUi(helpers)
val download = lila.user.ui.UserGamesDownload(helpers)

def mini(
    u: UserWithPerfs,
    playingGame: Option[Pov],
    blocked: Boolean,
    followable: Boolean,
    relation: Option[lila.relation.Relation],
    ping: Option[Int],
    ct: Option[lila.game.Crosstable],
    realName: Option[String]
)(using ctx: Context) =
  val rel = views.relation.mini(u.id, blocked, followable, relation)
  def crosstable(myId: UserId) = ct
    .flatMap(_.nonEmpty)
    .map: cross =>
      a(
        cls := "upt__score",
        href := s"${routes.User.games(u.username, "me")}#games",
        title := trans.site.nbGames.pluralTxt(cross.nbGames, cross.nbGames.localize)
      ):
        trans.site.yourScore(raw:
          val opponent = ~cross.showOpponentScore(myId)
          s"""<strong>${cross.showScore(myId)}</strong> - <strong>$opponent</strong>""")
  val playing = playingGame.map(views.game.mini(_))
  def userMarks = views.mod.user.userMarks(u.user, None)
  val flag = u.profileOrDefault.flagInfo
  val perfs = u.perfs.best8Perfs
  val nameFrag = realName.map(frag(_))
  show.ui.mini(u, playing, blocked, ping, rel, crosstable, flag, nameFrag, perfs, userMarks)

val perfStat = lila.perfStat.PerfStatUi(helpers)(views.user.bits.communityMenu("ratings"))
def perfStatPage(data: PerfStatData, ratingChart: Option[SafeJsonStr])(using Context) =
  perfStat.page(
    data,
    ratingChart,
    side = show.page.side(data.user, data.ranks, data.perfKey.some),
    perfTrophies = bits.perfTrophies(data.user, data.ranks.view.filterKeys(data.perfKey == _).toMap)
  )
