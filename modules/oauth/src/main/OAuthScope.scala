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

  case class Scoped(user: lila.user.User, scopes: List[OAuthScope])

  type Selector = OAuthScope.type => OAuthScope

  val all = List(
    Game.Read,
    Preference.Read, Preference.Write
  )

  val byKey: Map[String, OAuthScope] = all.map { s => s.key -> s } toMap

  def keyList(scopes: Iterable[OAuthScope]) = scopes.map(_.key) mkString ", "

  def select(selectors: Iterable[OAuthScope.type => OAuthScope]) = selectors.map(_(OAuthScope)).toList
}
