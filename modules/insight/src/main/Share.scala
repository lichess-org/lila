package lila.insight

import lila.core.perm.{ Granter, Grantable }

final class Share(
    prefApi: lila.core.pref.PrefApi,
    relationApi: lila.core.relation.RelationApi
)(using Executor):

  def getPrefId(insighted: User) = prefApi.getInsightShare(insighted.id)

  def grant(insighted: User)(using to: Option[User]): Fu[Boolean] =
    if to.soUse(Granter[User](_.SeeInsight)) then fuTrue
    else
      getPrefId(insighted).flatMap:
        case _ if to.contains(insighted)           => fuTrue
        case lila.core.pref.InsightShare.EVERYBODY => fuTrue
        case lila.core.pref.InsightShare.FRIENDS =>
          to.so: t =>
            relationApi.fetchAreFriends(insighted.id, t.id)
        case lila.core.pref.InsightShare.NOBODY => fuFalse
