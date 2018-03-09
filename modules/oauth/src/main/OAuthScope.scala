package lila.oauth

sealed abstract class OAuthScope(val key: String, val name: String)

object OAuthScope {

  object Game {
    case object Read extends OAuthScope("game:read", "Download all games")
  }

  object Preference {
    case object Read extends OAuthScope("preference:read", "Read preferences")
    case object Write extends OAuthScope("preference:write", "Write preferences")
  }

  val all = List(
    Game.Read,
    Preference.Read, Preference.Write
  )

  val byKey: Map[String, OAuthScope] = all.map { s => s.key -> s } toMap
}
