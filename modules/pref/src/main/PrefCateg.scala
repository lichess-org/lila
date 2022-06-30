package lila.pref

sealed abstract class PrefCateg(val slug: String)

object PrefCateg {

  case object GameDisplay  extends PrefCateg("game-display")
  case object ShogiClock   extends PrefCateg("shogi-clock")
  case object GameBehavior extends PrefCateg("game-behavior")
  case object Privacy      extends PrefCateg("privacy")

  val all: List[PrefCateg] = List(GameDisplay, ShogiClock, GameBehavior, Privacy)

  def apply(slug: String) = all find (_.slug == slug)
}
