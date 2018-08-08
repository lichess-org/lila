package lidraughts.pref

sealed abstract class PrefCateg(val slug: String)

object PrefCateg {

  case object GameDisplay extends PrefCateg("game-display")
  case object DraughtsClock extends PrefCateg("draughts-clock")
  case object GameBehavior extends PrefCateg("game-behavior")
  case object Privacy extends PrefCateg("privacy")

  val all: List[PrefCateg] = List(GameDisplay, DraughtsClock, GameBehavior, Privacy)

  def apply(slug: String) = all find (_.slug == slug)
}
