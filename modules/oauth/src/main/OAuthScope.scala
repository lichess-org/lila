package lila.oauth

sealed abstract class OAuthScope(val key: String, val name: String) {
  override def toString = s"Scope($key)"
}

object OAuthScope {

  object Preference {
    case object Read  extends OAuthScope("preference:read", "Read preferences")
    case object Write extends OAuthScope("preference:write", "Write preferences")
  }

  object Email {
    case object Read extends OAuthScope("email:read", "Read email address")
  }

  object Challenge {
    case object Read  extends OAuthScope("challenge:read", "Read incoming challenges")
    case object Write extends OAuthScope("challenge:write", "Create, accept, decline challenges")
    case object Bulk  extends OAuthScope("challenge:bulk", "Create many games at once for other players")
  }

  object Study {
    case object Read  extends OAuthScope("study:read", "Read private studies and broadcasts")
    case object Write extends OAuthScope("study:write", "Create, update, delete studies and broadcasts")
  }

  object Tournament {
    case object Write extends OAuthScope("tournament:write", "Create and update tournaments")
  }

  object Puzzle {
    case object Read extends OAuthScope("puzzle:read", "Read puzzle activity")
  }

  object Team {
    case object Read  extends OAuthScope("team:read", "Read private team information")
    case object Write extends OAuthScope("team:write", "Join, leave, and manage teams")
  }

  object Msg {
    case object Write extends OAuthScope("msg:write", "Send private messages to other players")
  }

  object Board {
    case object Play extends OAuthScope("board:play", "Play games with the board API")
  }

  object Bot {
    case object Play extends OAuthScope("bot:play", "Play games with the bot API")
  }

  object Web {
    case object Login
        extends OAuthScope("web:login", "Create authenticated website sessions (grants full access!)")
    case object Mod
        extends OAuthScope("web:mod", "Use moderator tools (within the bounds of your permissions)")
  }

  case class Scoped(user: lila.user.User, scopes: List[OAuthScope])

  type Selector = OAuthScope.type => OAuthScope

  val all = List(
    Preference.Read,
    Preference.Write,
    Email.Read,
    Challenge.Read,
    Challenge.Write,
    Challenge.Bulk,
    Study.Read,
    Study.Write,
    Tournament.Write,
    Puzzle.Read,
    Team.Read,
    Team.Write,
    Msg.Write,
    Board.Play,
    Bot.Play,
    Web.Login,
    Web.Mod
  )

  val byKey: Map[String, OAuthScope] = all.map { s =>
    s.key -> s
  } toMap

  def keyList(scopes: Iterable[OAuthScope]) = scopes.map(_.key) mkString ", "

  def select(selectors: Iterable[OAuthScope.type => OAuthScope]) = selectors.map(_(OAuthScope)).toList

  import reactivemongo.api.bson._
  import lila.db.dsl._
  implicit private[oauth] val scopeHandler = tryHandler[OAuthScope](
    { case b: BSONString => OAuthScope.byKey.get(b.value) toTry s"No such scope: ${b.value}" },
    s => BSONString(s.key)
  )
}
