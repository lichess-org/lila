package lila.user

final class IsOnline(f: User.ID => Boolean) extends (User.ID => Boolean) {
  def apply(u: User.ID) = f(u)
}

final class GetBotIds(f: () => Fu[Set[User.ID]]) extends (() => Fu[Set[User.ID]]) {
  def apply() = f()
}
