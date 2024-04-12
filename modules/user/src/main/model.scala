package lila.user

import lila.core.LightUser

final class GetBotIds(f: () => Fu[Set[UserId]]) extends (() => Fu[Set[UserId]]):
  def apply() = f()

case class LightCount(user: LightUser, count: Int)
