package lila.insight

import lila.pref.Pref
import lila.security.Granter
import lila.user.User

final class Share(
    prefApi: lila.pref.PrefApi,
    relationApi: lila.relation.RelationApi
)(using Executor):

  def getPrefId(insighted: User) = prefApi.getPref(insighted.id, _.insightShare)

  def grant(insighted: User, to: Option[User]): Fu[Boolean] =
    if (to so Granter(_.SeeInsight)) fuTrue
    else
      getPrefId(insighted) flatMap {
        case _ if to.contains(insighted) => fuTrue
        case Pref.InsightShare.EVERYBODY => fuTrue
        case Pref.InsightShare.FRIENDS =>
          to so { t =>
            relationApi.fetchAreFriends(insighted.id, t.id)
          }
        case Pref.InsightShare.NOBODY => fuFalse
      }
