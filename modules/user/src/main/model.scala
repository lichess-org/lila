package lila.user

final class GetBotIds(f: () => Fu[Set[User.ID]]) extends (() => Fu[Set[User.ID]]) {
  def apply() = f()
}
