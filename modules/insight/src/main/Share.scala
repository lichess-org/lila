package lila.insight

import lila.pref.Pref
import lila.security.Granter
import lila.user.User

final class Share(
    getPref: String => Fu[Pref],
    areFriends: (String, String) => Fu[Boolean]
) {

  def getPrefId(insighted: User) = getPref(insighted.id) map (_.insightShare)

  def grant(insighted: User, to: Option[User]): Fu[Boolean] =
    if (to ?? Granter(_.SeeInsight)) fuTrue
    else getPref(insighted.id) flatMap { pref =>
      pref.insightShare match {
        case _ if to.contains(insighted) => fuTrue
        case Pref.InsightShare.EVERYBODY => fuTrue
        case Pref.InsightShare.FRIENDS => to ?? { t => areFriends(insighted.id, t.id) }
        case Pref.InsightShare.NOBODY => fuFalse
      }
    }
}
