package lila.insight

import lila.core.perm.Granter

final class Share(
    prefApi: lila.core.pref.PrefApi,
    relationApi: lila.core.relation.RelationApi
)(using Executor):

  def getPrefId(insighted: User) = prefApi.getInsightShare(insighted.id)

  def grant(insighted: User)(using to: Option[User]): Fu[Boolean] =
    if to.exists(Granter.of(_.SeeInsight)) then fuTrue
    else if insighted.enabled.yes
    then
      getPrefId(insighted).flatMap:
        case _ if to.contains(insighted) => fuTrue
        case lila.core.pref.InsightShare.EVERYBODY => fuTrue
        case lila.core.pref.InsightShare.FRIENDS =>
          to.so: t =>
            relationApi.fetchAreFriends(insighted.id, t.id)
        case lila.core.pref.InsightShare.NOBODY => fuFalse
    else fuFalse
