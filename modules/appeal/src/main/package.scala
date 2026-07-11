package lila.appeal

export lila.core.lilaism.Lilaism.{ *, given }
export lila.common.extensions.*
export lila.core.misc.AppealTopic

case class UserStatus(user: User, playban: Boolean, ublogHidden: Boolean):
  export user.{ id, enabled, marks }
