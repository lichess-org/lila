package lila
package appeal

object AppealTopicApi:

  private def candidates(u: UserStatus): List[AppealTopic] =
    import AppealTopic.*
    List(
      u.marks.engine.option(cheat),
      u.marks.boost.option(boost),
      u.enabled.no.option(close),
      u.marks.troll.option(comm),
      u.playban.option(play),
      u.ublogHidden.option(blog),
      u.marks.rankban.option(rank),
      u.marks.arenaBan.option(arena),
      u.marks.prizeban.option(prize)
    ).flatten

  def select(u: UserStatus, appeals: Appeal.ByTopic): Option[AppealTopic] =
    candidates(u).find: topic =>
      appeals.get(topic).forall(_.isOpen)
