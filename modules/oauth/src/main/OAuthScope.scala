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

  object Email {
    case object Read extends OAuthScope("email:read", "Read email address")
  }

  case class Scoped(user: lila.user.User, scopes: List[OAuthScope])

  type Selector = OAuthScope.type => OAuthScope

  val all = List(
    Game.Read,
    Preference.Read, Preference.Write,
    Email.Read
  )

  val byKey: Map[String, OAuthScope] = all.map { s => s.key -> s } toMap

  def keyList(scopes: Iterable[OAuthScope]) = scopes.map(_.key) mkString ", "

  def select(selectors: Iterable[OAuthScope.type => OAuthScope]) = selectors.map(_(OAuthScope)).toList

  import reactivemongo.bson._
  import lila.db.dsl._
  private[oauth] implicit val scopeHandler = new BSONHandler[BSONString, OAuthScope] {
    def read(b: BSONString): OAuthScope = OAuthScope.byKey.get(b.value) err s"No such scope: ${b.value}"
    def write(s: OAuthScope) = BSONString(s.key)
  }
}
