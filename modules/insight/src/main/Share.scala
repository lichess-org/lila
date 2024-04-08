package lila.insight

import lila.pref.Pref
import lila.core.perm.{ Granter, Grantable }
import lila.core.user.User

final class Share(
    prefApi: lila.pref.PrefApi,
    relationApi: lila.core.relation.RelationApi
)(using Executor):

  def getPrefId(insighted: User) = prefApi.get(insighted.id, _.insightShare)

  def grant(insighted: User)(using to: Option[User]): Fu[Boolean] =
    if to.soUse(Granter[User](_.SeeInsight)) then fuTrue
    else
      getPrefId(insighted).flatMap {
        case _ if to.contains(insighted) => fuTrue
        case Pref.InsightShare.EVERYBODY => fuTrue
        case Pref.InsightShare.FRIENDS =>
          to.so: t =>
            relationApi.fetchAreFriends(insighted.id, t.id)
        case Pref.InsightShare.NOBODY => fuFalse
      }
