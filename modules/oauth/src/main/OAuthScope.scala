package lila.oauth

import lila.core.i18n.I18nKey
import lila.core.i18n.I18nKey.oauthScope as trans
import lila.core.misc.oauth.AccessTokenId

sealed abstract class OAuthScope(val key: String, val name: I18nKey):
  override def toString = s"Scope($key)"

opaque type OAuthScopes = List[OAuthScope]
object OAuthScopes extends TotalWrapper[OAuthScopes, List[OAuthScope]]:
  extension (e: OAuthScopes)
    def has(s: OAuthScope): Boolean = e contains s
    def has(s: OAuthScope.Selector): Boolean = has(s(OAuthScope))
    def keyList: String = e.map(_.key).mkString(",")
    def intersects(other: OAuthScopes): Boolean = e.exists(other.has)
    def isEmpty = e.isEmpty

opaque type TokenScopes = List[OAuthScope]
object TokenScopes extends TotalWrapper[TokenScopes, List[OAuthScope]]:
  extension (e: TokenScopes)
    def intersects(other: OAuthScopes): Boolean = e.exists(other.contains)
    def has(s: OAuthScope.Selector): Boolean = e.contains(s(OAuthScope))

opaque type EndpointScopes = List[OAuthScope]
object EndpointScopes extends TotalWrapper[EndpointScopes, List[OAuthScope]]:
  extension (e: EndpointScopes)
    def isEmpty = e.isEmpty
    def compatible(token: TokenScopes): Boolean = e.exists(token.has)
    def show = e.map(_.key).mkString(" || ")

object OAuthScope:

  given Eq[OAuthScope] = Eq.fromUniversalEquals

  object Preference:
    case object Read extends OAuthScope("preference:read", lila.core.i18n.I18nKey.oauthScope.preferenceRead)
    case object Write extends OAuthScope("preference:write", trans.preferenceWrite)

  object Email:
    case object Read extends OAuthScope("email:read", trans.emailRead)

  object Challenge:
    case object Read extends OAuthScope("challenge:read", trans.challengeRead)
    case object Write extends OAuthScope("challenge:write", trans.challengeWrite)
    case object Bulk extends OAuthScope("challenge:bulk", trans.challengeBulk)

  object Study:
    case object Read extends OAuthScope("study:read", trans.studyRead)
    case object Write extends OAuthScope("study:write", trans.studyWrite)

  object Tournament:
    case object Write extends OAuthScope("tournament:write", trans.tournamentWrite)

  object Racer:
    case object Write extends OAuthScope("racer:write", trans.racerWrite)

  object Puzzle:
    case object Read extends OAuthScope("puzzle:read", trans.puzzleRead)
    case object Write extends OAuthScope("puzzle:write", I18nKey("Solve puzzles"))

  object Team:
    case object Read extends OAuthScope("team:read", trans.teamRead)
    case object Write extends OAuthScope("team:write", trans.teamWrite)
    case object Lead extends OAuthScope("team:lead", trans.teamLead)

  object Follow:
    case object Read extends OAuthScope("follow:read", trans.followRead)
    case object Write extends OAuthScope("follow:write", trans.followWrite)

  object Msg:
    case object Write extends OAuthScope("msg:write", trans.msgWrite)

  object Board:
    case object Play extends OAuthScope("board:play", trans.boardPlay)

  object Bot:
    case object Play extends OAuthScope("bot:play", trans.botPlay)

  object Engine:
    case object Read extends OAuthScope("engine:read", trans.engineRead)
    case object Write extends OAuthScope("engine:write", trans.engineWrite)

  object Web:
    case object Login extends OAuthScope("web:login", trans.webLogin)
    case object Mobile extends OAuthScope("web:mobile", I18nKey("Official Lichess mobile app"))
    case object Mod extends OAuthScope("web:mod", trans.webMod)

  case class Scoped(me: Me, scopes: TokenScopes):
    def user: User = me.value

  case class Access(scoped: Scoped, tokenId: AccessTokenId):
    export scoped.*

  type Selector = OAuthScope.type => OAuthScope

  val all: List[OAuthScope] = List(
    Preference.Read,
    Preference.Write,
    Email.Read,
    Challenge.Read,
    Challenge.Write,
    Challenge.Bulk,
    Study.Read,
    Study.Write,
    Tournament.Write,
    Racer.Write,
    Puzzle.Read,
    Puzzle.Write,
    Team.Read,
    Team.Write,
    Team.Lead,
    Follow.Read,
    Follow.Write,
    Msg.Write,
    Board.Play,
    Bot.Play,
    Engine.Read,
    Engine.Write,
    Web.Login,
    Web.Mobile,
    Web.Mod
  )

  val classified: List[(I18nKey, List[OAuthScope])] = List(
    I18nKey("User account") -> List(Email.Read, Preference.Read, Preference.Write, Web.Mod),
    I18nKey("Interactions") -> List(Follow.Read, Follow.Write, Msg.Write),
    I18nKey("Play games") -> List(Challenge.Read, Challenge.Write, Challenge.Bulk, Tournament.Write),
    I18nKey("Teams") -> List(Team.Read, Team.Write, Team.Lead),
    I18nKey("Puzzles") -> List(Puzzle.Read, Racer.Write),
    I18nKey("Studies & Broadcasts") -> List(Study.Read, Study.Write),
    I18nKey("External play") -> List(Board.Play, Bot.Play),
    I18nKey("External engine") -> List(Engine.Read, Engine.Write)
  )

  val dangerList: OAuthScopes = OAuthScope.select(
    _.Team.Lead,
    _.Web.Login,
    _.Web.Mod,
    _.Web.Mobile,
    _.Msg.Write
  )

  val relevantToMods: OAuthScopes = OAuthScope.select(
    _.Team.Lead,
    _.Web.Login,
    _.Web.Mobile,
    _.Msg.Write,
    _.Board.Play
  )

  val byKey: Map[String, OAuthScope] = all.mapBy(_.key)

  def select(selectors: Iterable[Selector]): OAuthScopes =
    OAuthScopes(selectors.map(_(OAuthScope)).toList)
  def select(selectors: Selector*): OAuthScopes = select(selectors)

  def canUseWebMod(using Option[Me]) =
    import lila.core.perm.*
    List[Permission.Selector](_.Shusher, _.BoostHunter, _.CheatHunter, _.StudyAdmin, _.ApiChallengeAdmin)
      .exists(Granter.opt)

  import reactivemongo.api.bson.*
  import lila.db.dsl.*
  private[oauth] given BSONHandler[OAuthScope] = tryHandler[OAuthScope](
    { case b: BSONString => OAuthScope.byKey.get(b.value).toTry(s"No such scope: ${b.value}") },
    s => BSONString(s.key)
  )
