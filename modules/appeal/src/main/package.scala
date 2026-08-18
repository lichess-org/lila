package lila.appeal

export lila.core.lilaism.Lilaism.{ *, given }
export lila.common.extensions.*
export lila.core.misc.AppealTopic

case class UserStatus(
    user: User,
    playban: Boolean,
    ublogHidden: Boolean,
    modActions: List[String]
):
  export user.{ id, enabled, marks }

  def isClean = AppealTopicApi.candidatesFor(this).isEmpty

  def modMessage = modActions.contains("modMessage")
  def chatTimeout = modActions.contains("chatTimeout")
