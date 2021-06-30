package lila.user

final class GetBotIds(f: () => Fu[Set[User.ID]]) extends (() => Fu[Set[User.ID]]) {
  def apply() = f()
}

final class RankingsOf(f: User.ID => Fu[lila.rating.UserRankMap])
    extends (User.ID => Fu[lila.rating.UserRankMap]) {
  def apply(u: User.ID) = f(u)
}

// permission holder
case class Holder(user: User) extends AnyVal {
  def id = user.id
}
