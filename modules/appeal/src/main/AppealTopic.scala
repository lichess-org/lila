package lila
package appeal

import play.api.mvc.Call
import lila.core.i18n.I18nKey
import lila.core.i18n.I18nKey.appeal as trans

object AppealTopicApi:

  val irrelevant = Set(AppealTopic.play)
  val relevant = AppealTopic.values.filterNot(irrelevant).toList

  private[appeal] def candidatesFor(u: UserStatus): List[AppealTopic] =
    import AppealTopic.*
    List(
      u.marks.engine.option(cheat),
      u.marks.boost.option(boost),
      u.enabled.no.option(close),
      u.marks.troll.option(comm),
      u.marks.rankban.option(rank),
      u.marks.arenaBan.option(arena),
      u.marks.prizeban.option(prize),
      u.playban.option(play),
      u.ublogHidden.option(blog)
    ).flatten

  def select(u: UserStatus, appeals: Appeal.ByTopic): Option[AppealTopic] =
    candidatesFor(u)
      .find: topic =>
        appeals.get(topic).forall(_.isOpen)
      .orElse:
        List(AppealTopic.warning, AppealTopic.legacy)
          .find(t => appeals.get(t).exists(_.isOpen))

  def unmark(user: UserStatus, topic: AppealTopic): Option[(String, Call)] =
    candidatesFor(user)
      .contains(topic)
      .so:
        topic match
          case AppealTopic.cheat => ("Undo the engine mark", routes.Mod.engine(user.id, false)).some
          case AppealTopic.boost => ("Undo the boost mark", routes.Mod.booster(user.id, false)).some
          case AppealTopic.close => ("Reopen the account", routes.Mod.reopenAccount(user.id)).some
          case AppealTopic.comm => ("Unmute the account", routes.Mod.troll(user.id, false)).some
          case AppealTopic.rank => ("Unban from leaderboards", routes.Mod.rankban(user.id, false)).some
          case AppealTopic.arena => ("Unban from arena", routes.Mod.arenaBan(user.id, false)).some
          case AppealTopic.prize => ("Unban from prize", routes.Mod.prizeban(user.id, false)).some
          case _ => none

  def markMsg(status: UserStatus, topic: AppealTopic): Option[I18nKey] =
    candidatesFor(status)
      .contains(topic)
      .so:
        topic match
          case AppealTopic.cheat => trans.engineMarked.some
          case AppealTopic.boost => trans.boosterMarked.some
          case AppealTopic.close => trans.closedByModerators.some
          case AppealTopic.comm => trans.accountMuted.some
          case AppealTopic.rank => trans.excludedFromLeaderboards.some
          case AppealTopic.arena => trans.arenaBanned.some
          case AppealTopic.prize => trans.prizeBanned.some
          case _ => none

  object topicFilter:
    private var store = Map.empty[UserId, AppealTopic]
    def apply(str: Option[String])(using me: Me): Option[AppealTopic] =
      if str.contains("all") then store = store - me.userId
      else
        str
          .flatMap(AppealTopic.byKey.get)
          .foreach: topic =>
            store = store + (me.userId -> topic)
      store.get(me.userId)
