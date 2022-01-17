package lila.insight

import lila.pref.Pref
import lila.security.Granter
import lila.user.User

final class Share(
    prefApi: lila.pref.PrefApi,
    relationApi: lila.relation.RelationApi
)(implicit ec: scala.concurrent.ExecutionContext) {

  def getPrefId(insighted: User) = prefApi.getPref(insighted.id, _.insightShare)

  def grant(insighted: User, to: Option[User]): Fu[Boolean] =
    if (to ?? Granter(_.SeeInsight)) fuTrue
    else
      getPrefId(insighted) flatMap {
        case _ if to.contains(insighted) => fuTrue
        case Pref.InsightShare.EVERYBODY => fuTrue
        case Pref.InsightShare.FRIENDS =>
          to ?? { t =>
            relationApi.fetchAreFriends(insighted.id, t.id)
          }
        case Pref.InsightShare.NOBODY => fuFalse
      }
}
