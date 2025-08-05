package lila.chat

import reactivemongo.api.bson.BSONDocumentHandler

sealed trait AnyChat:
  def id: ChatId
  def lines: List[Line]

  val loginRequired: Boolean

  def forMe(using Option[Me], AllMessages): AnyChat

  def isEmpty = lines.isEmpty

  def flairUserIds: List[UserId]

sealed trait Chat[L <: Line] extends AnyChat:
  def id: ChatId
  def lines: List[L]
  def nonEmpty = lines.exists(_.isHuman)

case class UserChat(
    id: ChatId,
    lines: List[UserLine]
) extends Chat[UserLine]:

  val loginRequired = true

  def forMe(using me: Option[Me], all: AllMessages): UserChat =
    if all.yes || me.exists(_.marks.troll) then this
    else copy(lines = lines.filterNot(_.troll))

  def markDeleted(u: User) = copy(
    lines = lines.map: l =>
      if l.userId.is(u.id) then l.delete else l
  )

  def hasLinesOf(u: User) = lines.exists(_.userId == u.id)

  def add(line: UserLine) = copy(lines = lines :+ line)

  def mapLines(f: UserLine => UserLine) = copy(lines = lines.map(f))

  def filterLines(f: UserLine => Boolean) = copy(lines = lines.filter(f))

  def flairUserIds = lines.collect:
    case l if l.flair => l.userId

  def truncate(max: Int) = copy(lines = lines.drop((lines.size - max).atLeast(0)))

  def hasRecentLine(u: User): Boolean = lines.reverse.take(12).exists(_.userId == u.id)

object UserChat:
  case class Mine(chat: UserChat, lines: JsonChatLines, timeout: Boolean, locked: Boolean = false):
    def truncate(max: Int) = copy(chat = chat.truncate(max))

case class MixedChat(
    id: ChatId,
    lines: List[Line]
) extends Chat[Line]:

  val loginRequired = false

  def forMe(using me: Option[Me], all: AllMessages): MixedChat =
    if all.yes || me.exists(_.marks.troll) then this
    else
      copy(lines = lines.filter:
        case l: UserLine => !l.troll
        case _: PlayerLine => true)

  def mapLines(f: Line => Line) = copy(lines = lines.map(f))

  def flairUserIds = lines.collect:
    case l: UserLine if l.flair => l.userId

object Chat:

  opaque type ResourceId = String
  object ResourceId extends OpaqueString[ResourceId]

  import lila.core.shutup.PublicSource

  case class Setup(id: ChatId, publicSource: PublicSource)

  def tournamentSetup(tourId: TourId) = Setup(tourId.into(ChatId), PublicSource.Tournament(tourId))
  def simulSetup(simulId: SimulId) = Setup(simulId.into(ChatId), PublicSource.Simul(simulId))

  // if restricted, only presets are available
  case class Restricted(chat: MixedChat, lines: JsonChatLines, restricted: Boolean)

  // left: game chat
  // right: tournament/simul chat
  case class GameOrEvent(either: Either[Restricted, (UserChat.Mine, ResourceId)]):
    def game = either.left.toOption

  import lila.db.BSON

  def makeUser(id: ChatId) = UserChat(id, Nil)
  def makeMixed(id: ChatId) = MixedChat(id, Nil)

  def chanOf(id: ChatId) = s"chat:$id"

  object BSONFields:
    val id = "_id"
    val lines = "l"

  import BSONFields.*
  import reactivemongo.api.bson.BSONDocument
  import lila.db.dsl.given

  given BSONDocumentHandler[MixedChat] = new BSON[MixedChat]:
    import lila.chat.Line.given
    def reads(r: BSON.Reader): MixedChat =
      MixedChat(
        id = r.get[ChatId](id),
        lines = r.get[List[Line]](lines)
      )
    def writes(w: BSON.Writer, o: MixedChat) =
      BSONDocument(
        id -> o.id,
        lines -> o.lines
      )

  given BSONDocumentHandler[UserChat] = new BSON[UserChat]:
    def reads(r: BSON.Reader): UserChat =
      UserChat(
        id = r.get[ChatId](id),
        lines = r.get[List[UserLine]](lines)
      )
    def writes(w: BSON.Writer, o: UserChat) =
      BSONDocument(
        id -> o.id,
        lines -> o.lines
      )
