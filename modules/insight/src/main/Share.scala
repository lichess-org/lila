package lila.insight

import lila.pref.Pref
import lila.core.perm.Granter
import lila.user.{ Me, User }

final class Share(
    prefApi: lila.pref.PrefApi,
    relationApi: lila.core.relation.RelationApi
)(using Executor):

  def getPrefId(insighted: User) = prefApi.get(insighted.id, _.insightShare)

  def grant(insighted: User)(using to: Option[Me]): Fu[Boolean] =
    if to.soUse(Granter[Me](_.SeeInsight)) then fuTrue
    else
      getPrefId(insighted).flatMap {
        case _ if to.contains(insighted) => fuTrue
        case Pref.InsightShare.EVERYBODY => fuTrue
        case Pref.InsightShare.FRIENDS =>
          to.so: t =>
            relationApi.fetchAreFriends(insighted.id, t.userId)
        case Pref.InsightShare.NOBODY => fuFalse
      }
