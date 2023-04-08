package lila.pref

enum PrefCateg(val slug: String):

  case Display      extends PrefCateg("display")
  case ChessClock   extends PrefCateg("chess-clock")
  case GameBehavior extends PrefCateg("game-behavior")
  case Privacy      extends PrefCateg("privacy")

object PrefCateg:

  def apply(slug: String) = values.find(_.slug == slug)
