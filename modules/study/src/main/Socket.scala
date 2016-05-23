package lila.study

import akka.actor._
import chess.format.pgn.Glyphs
import com.google.common.cache.LoadingCache
import play.api.libs.json._
import scala.concurrent.duration._

import lila.common.LightUser
import lila.hub.TimeBomb
import lila.socket.actorApi.{ Connected => _, _ }
import lila.socket.Socket.Uid
import lila.socket.tree.Node.{ Shape, Shapes, Comment }
import lila.socket.{ SocketActor, History, Historical, AnaDests }
import lila.user.User

private final class Socket(
    studyId: String,
    jsonView: JsonView,
    studyRepo: StudyRepo,
    lightUser: lila.common.LightUser.Getter,
    val history: History[Socket.Messadata],
    destCache: LoadingCache[AnaDests.Ref, AnaDests],
    uidTimeout: Duration,
    socketTimeout: Duration) extends SocketActor[Socket.Member](uidTimeout) with Historical[Socket.Member, Socket.Messadata] {

  import Socket._
  import JsonView._
  import jsonView.membersWrites
  import lila.socket.tree.Node.{ openingWriter, commentWriter, glyphsWriter, shapesWrites }

  private val timeBomb = new TimeBomb(socketTimeout)

  private var delayedCrowdNotification = false

  def receiveSpecific = {

    case SetPath(pos, uid) => notifyVersion("path", Json.obj(
      "p" -> pos,
      "w" -> who(uid).map(whoWriter.writes)
    ), Messadata())

    case AddNode(pos, node, uid) =>
      val dests = destCache.get(AnaDests.Ref(chess.variant.Standard, node.fen.value, pos.path.toString))
      notifyVersion("addNode", Json.obj(
        "n" -> TreeBuilder.toBranch(node),
        "p" -> pos,
        "w" -> who(uid),
        "d" -> dests.dests,
        "o" -> dests.opening
      ), Messadata())

    case DeleteNode(pos, uid) => notifyVersion("deleteNode", Json.obj(
      "p" -> pos,
      "w" -> who(uid)
    ), Messadata())

    case PromoteNode(pos, uid) => notifyVersion("promoteNode", Json.obj(
      "p" -> pos,
      "w" -> who(uid)
    ), Messadata())

    case ReloadMembers(members)   => notifyVersion("members", members, Messadata())

    case ReloadChapters(chapters) => notifyVersion("chapters", chapters, Messadata())

    case ReloadAll                => notifyVersion("reload", JsNull, Messadata())
    case ChangeChapter(uid) => notifyVersion("changeChapter", Json.obj(
      "w" -> who(uid)
    ), Messadata())

    case SetShapes(pos, shapes, uid) => notifyVersion("shapes", Json.obj(
      "p" -> pos,
      "s" -> shapes,
      "w" -> who(uid)
    ), Messadata())

    case SetComment(pos, comment, uid) => notifyVersion("setComment", Json.obj(
      "p" -> pos,
      "c" -> comment,
      "w" -> who(uid)
    ), Messadata())

    case DeleteComment(pos, commentId, uid) => notifyVersion("deleteComment", Json.obj(
      "p" -> pos,
      "id" -> commentId,
      "w" -> who(uid)
    ), Messadata())

    case SetGlyphs(pos, glyphs, uid) => notifyVersion("glyphs", Json.obj(
      "p" -> pos,
      "g" -> glyphs,
      "w" -> who(uid)
    ), Messadata())

    case SetConceal(pos, ply) => notifyVersion("conceal", Json.obj(
      "p" -> pos,
      "ply" -> ply.map(_.value)
    ), Messadata())

    case lila.chat.actorApi.ChatLine(_, line) => line match {
      case line: lila.chat.UserLine =>
        notifyVersion("message", lila.chat.JsonView(line), Messadata(trollish = line.troll))
      case _ =>
    }

    case ReloadUid(uid) => notifyUid("reload", JsNull)(uid)

    case PingVersion(uid, v) =>
      ping(uid)
      timeBomb.delay
      withMember(uid) { m =>
        history.since(v).fold(resync(m))(_ foreach sendMessage(m))
      }

    case Broom =>
      broom
      if (timeBomb.boom) self ! PoisonPill

    case GetVersion => sender ! history.version

    case Socket.Join(uid, userId, troll, owner) =>
      import play.api.libs.iteratee.Concurrent
      val (enumerator, channel) = Concurrent.broadcast[JsValue]
      val member = Socket.Member(channel, userId, troll = troll, owner = owner)
      addMember(uid.value, member)
      notifyCrowd
      sender ! Socket.Connected(enumerator, member)

    case Quit(uid) =>
      members get uid foreach { member =>
        quit(uid)
        notifyCrowd
      }

    case NotifyCrowd =>
      delayedCrowdNotification = false
      val json =
        if (members.size <= maxSpectatorUsers) fuccess(showSpectators(lightUser)(members.values))
        else studyRepo.uids(studyId) map { memberIds =>
          showSpectatorsAndMembers(lightUser)(memberIds, members.values)
        }
      json foreach { notifyAll("crowd", _) }
  }

  def notifyCrowd {
    if (!delayedCrowdNotification) {
      delayedCrowdNotification = true
      context.system.scheduler.scheduleOnce(500 millis, self, NotifyCrowd)
    }
  }

  override val maxSpectatorUsers = 15

  // always show study members
  // since that's how the client knows if they're online
  def showSpectatorsAndMembers(lightUser: String => Option[LightUser])(memberIds: Set[User.ID], watchers: Iterable[Member]): JsValue = {

    val (total, anons, userIds) = watchers.foldLeft((0, 0, Set.empty[String])) {
      case ((total, anons, userIds), member) => member.userId match {
        case Some(userId) if !userIds(userId) && (memberIds(userId) || userIds.size < maxSpectatorUsers) => (total + 1, anons, userIds + userId)
        case Some(_) => (total + 1, anons, userIds)
        case _ => (total + 1, anons + 1, userIds)
      }
    }

    if (total == 0) JsNull
    else {
      val selectedUids =
        if (userIds.size >= maxSpectatorUsers) userIds.partition(memberIds.contains) match {
          case (members, others) => members ++ others.take(maxSpectatorUsers - members.size)
        }
        else userIds
      Json.obj(
        "nb" -> total,
        "users" -> selectedUids.flatMap { lightUser(_) }.map(_.titleName),
        "anons" -> anons)
    }
  }

  protected def shouldSkipMessageFor(message: Message, member: Member) =
    (message.metadata.trollish && !member.troll)

  private def who(uid: Uid) = uidToUserId(uid) map { Who(_, uid) }
}

private object Socket {

  case class Member(
    channel: JsChannel,
    userId: Option[String],
    troll: Boolean,
    owner: Boolean) extends lila.socket.SocketMember

  case class Who(u: String, s: Uid)
  import JsonView.uidWriter
  implicit private val whoWriter = Json.writes[Who]

  case class Join(uid: Uid, userId: Option[User.ID], troll: Boolean, owner: Boolean)
  case class Connected(enumerator: JsEnumerator, member: Member)

  case class ReloadUid(uid: Uid)

  case class AddNode(position: Position.Ref, node: Node, uid: Uid)
  case class DeleteNode(position: Position.Ref, uid: Uid)
  case class PromoteNode(position: Position.Ref, uid: Uid)
  case class SetPath(position: Position.Ref, uid: Uid)
  case class ReloadMembers(members: StudyMembers)
  case class SetShapes(position: Position.Ref, shapes: Shapes, uid: Uid)
  case class SetComment(position: Position.Ref, comment: Comment, uid: Uid)
  case class DeleteComment(position: Position.Ref, commentId: Comment.Id, uid: Uid)
  case class SetGlyphs(position: Position.Ref, glyphs: Glyphs, uid: Uid)
  case class ReloadChapters(chapters: List[Chapter.Metadata])
  case object ReloadAll
  case class ChangeChapter(uid: Uid)
  case class SetConceal(position: Position.Ref, ply: Option[Chapter.Ply])

  case class Messadata(trollish: Boolean = false)
  case object NotifyCrowd
}
