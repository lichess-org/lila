package lila.coach

import lila.pref.Pref
import lila.user.User

final class Share(
    getPref: String => Fu[Pref],
    areFriends: (String, String) => Fu[Boolean]) {

  def grant(coached: User, to: Option[User]): Fu[Boolean] = getPref(coached.id) flatMap { pref =>
    pref.coachShare match {
      case _ if to.contains(coached) => fuccess(true)
      case Pref.CoachShare.EVERYBODY => fuccess(true)
      case Pref.CoachShare.FRIENDS   => to ?? { t => areFriends(coached.id, t.id) }
      case Pref.CoachShare.NOBODY    => fuccess(false)
    }
  }
}
