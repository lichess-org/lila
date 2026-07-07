package lila
package appeal

object AppealTopicApi:

  def select(u: User): Option[AppealTopic] =
    import AppealTopic.*
    if u.enabled.no then close.some
    else if u.marks.engine then cheat.some
    else if u.marks.boost then boost.some
    else if u.marks.troll then comm.some
    else if u.marks.rankban then rank.some
    else if u.marks.arenaBan then arena.some
    else if u.marks.prizeban then prize.some
    else none

  // def userMessage(topic
