package lila.socket

final class IsOnline(f: (String => Boolean)) extends (String => Boolean) {
  def apply(id: String) = f(id)
}

final class OnlineIds(f: () => Set[String]) extends (() => Set[String]) {
  def apply() = f()
}
