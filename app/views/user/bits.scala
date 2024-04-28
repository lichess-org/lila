package views.user

import lila.app.templating.Environment.{ *, given }
import lila.rating.UserPerfsExt.best8Perfs
import lila.core.perf.UserWithPerfs
import lila.user.Profile.flagInfo

lazy val bits   = lila.user.ui.UserBits(helpers)
lazy val noteUi = lila.user.ui.NoteUi(helpers)

def mini(
    u: UserWithPerfs,
    playingGame: Option[Pov],
    blocked: Boolean,
    followable: Boolean,
    relation: Option[lila.relation.Relation],
    ping: Option[Int],
    ct: Option[lila.game.Crosstable]
)(using ctx: Context) =
  val rel = views.relation.mini(u.id, blocked, followable, relation)
  def crosstable(myId: UserId) = ct
    .flatMap(_.nonEmpty)
    .map: cross =>
      a(
        cls   := "upt__score",
        href  := s"${routes.User.games(u.username, "me")}#games",
        title := trans.site.nbGames.pluralTxt(cross.nbGames, cross.nbGames.localize)
      ):
        trans.site.yourScore(raw:
          val opponent = ~cross.showOpponentScore(myId)
          s"""<strong>${cross.showScore(myId)}</strong> - <strong>$opponent</strong>"""
        )
  val playing   = playingGame.map(views.game.mini(_))
  def userMarks = views.mod.user.userMarks(u.user, None)
  val flag      = u.profileOrDefault.flagInfo
  val perfs     = u.perfs.best8Perfs
  show.ui.mini(u, playing, blocked, followable, ping, rel, crosstable, flag, perfs, userMarks)
