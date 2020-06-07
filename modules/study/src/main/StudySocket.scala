package lidraughts.study

import akka.actor._
import play.api.libs.iteratee._
import play.api.libs.json._
import scala.concurrent.duration._
import scala.concurrent.Promise

import draughts.Centis
import draughts.format.pdn.Glyphs
import lidraughts.chat.Chat
import lidraughts.hub.Trouper
import lidraughts.socket.actorApi.{ Connected => _, _ }
import lidraughts.socket.Socket.{ Uid, GetVersion, SocketVersion }
import lidraughts.socket.{ SocketTrouper, History, Historical, AnaDests }
import lidraughts.tree.Node.{ Shapes, Comment }
import lidraughts.user.User

final class StudySocket(
    system: ActorSystem,
    studyId: Study.Id,
    jsonView: JsonView,
    studyRepo: StudyRepo,
    chapterRepo: ChapterRepo,
    lightUserApi: lidraughts.user.LightUserApi,
    protected val history: History[StudySocket.Messadata],
    uidTtl: Duration,
    lightStudyCache: LightStudyCache,
    keepMeAlive: () => Unit
) extends SocketTrouper[StudySocket.Member](system, uidTtl) with Historical[StudySocket.Member, StudySocket.Messadata] {

  import StudySocket._
  import JsonView._
  import jsonView.membersWrites
  import lidraughts.tree.Node.{ openingWriter, commentWriter, glyphsWriter, shapesWrites, clockWrites, fullUciNodeJsonWriter }

  private var delayedCrowdNotification = false

  lidraughtsBus.subscribe(this, chatClassifier)

  override def stop(): Unit = {
    super.stop()
    lidraughtsBus.unsubscribe(this, chatClassifier)
  }

  private def chatClassifier = Chat classify Chat.Id(studyId.value)

  private def sendStudyDoor(enters: Boolean)(userId: User.ID) =
    lightStudyCache get studyId foreach {
      _ foreach { study =>
        lidraughtsBus.publish(
          lidraughts.hub.actorApi.study.StudyDoor(
            userId = userId,
            studyId = studyId.value,
            contributor = study contributors userId,
            public = study.isPublic,
            enters = enters
          ),
          'study
        )
      }
    }

  def receiveSpecific = ({

    case SetPath(pos, uid) =>
      notifyVersion("path", Json.obj(
        "p" -> pos,
        "w" -> who(uid).map(whoWriter.writes)
      ), noMessadata)

    case AddNode(pos, node, variant, uid, sticky, relay) =>
      val dests = AnaDests(
        variant = variant,
        fen = node.fen,
        path = pos.path.toString,
        chapterId = pos.chapterId.value.some,
        lastUci = node.move.uci.uci.some
      )
      val destsFull = dests.captureLength > 1 option AnaDests(
        variant = variant,
        fen = node.fen,
        path = pos.path.toString,
        chapterId = pos.chapterId.value.some,
        lastUci = node.move.uci.uci.some,
        fullCapture = true.some
      )
      val jsonMsg = Json.obj(
        "n" -> fullUciNodeJsonWriter.writes(TreeBuilder.toBranch(node, variant)),
        "p" -> pos,
        "w" -> who(uid),
        "d" -> dests.dests,
        "s" -> sticky
      ).add("o", dests.opening)
        .add("f", destsFull.map(_.dests))
        .add("u", destsFull.flatMap(_.destsUci))
        .add("relay", relay)
      notifyVersion("addNode", jsonMsg, noMessadata)

    case DeleteNode(pos, uid) => notifyVersion("deleteNode", Json.obj(
      "p" -> pos,
      "w" -> who(uid)
    ), noMessadata)

    case Promote(pos, toMainline, uid) => notifyVersion("promote", Json.obj(
      "p" -> pos,
      "toMainline" -> toMainline,
      "w" -> who(uid)
    ), noMessadata)

    case ReloadMembers(studyMembers) =>
      notifyVersion("members", studyMembers, noMessadata)
      val ids = studyMembers.ids.toSet
      notifyIf(makeMessage("reload")) { m =>
        m.userId.exists(ids.contains)
      }

    case ReloadChapters(chapters) => notifyVersion("chapters", chapters, noMessadata)

    case ReloadAll => notifyVersion("reload", JsNull, noMessadata)

    case ChangeChapter(uid, pos) => notifyVersion("changeChapter", Json.obj(
      "p" -> pos,
      "w" -> who(uid)
    ), noMessadata)

    case UpdateChapter(uid, chapterId) => notifyVersion("updateChapter", Json.obj(
      "chapterId" -> chapterId,
      "w" -> who(uid)
    ), noMessadata)

    case DescChapter(uid, chapterId, description) => notifyVersion("descChapter", Json.obj(
      "chapterId" -> chapterId,
      "desc" -> description,
      "w" -> who(uid)
    ), noMessadata)

    case DescStudy(uid, description) => notifyVersion("descStudy", Json.obj(
      "desc" -> description,
      "w" -> who(uid)
    ), noMessadata)

    case AddChapter(uid, pos, sticky) => notifyVersion("addChapter", Json.obj(
      "p" -> pos,
      "w" -> who(uid),
      "s" -> sticky
    ), noMessadata)

    case ValidationError(uid, error) => notifyUid("validationError", Json.obj(
      "error" -> error
    ))(uid)

    case SetShapes(pos, shapes, uid) => notifyVersion("shapes", Json.obj(
      "p" -> pos,
      "s" -> shapes,
      "w" -> who(uid)
    ), noMessadata)

    case SetComment(pos, comment, uid) => notifyVersion("setComment", Json.obj(
      "p" -> pos,
      "c" -> comment,
      "w" -> who(uid)
    ), noMessadata)

    case SetTags(chapterId, tags, uid) => notifyVersion("setTags", Json.obj(
      "chapterId" -> chapterId,
      "tags" -> tags,
      "w" -> who(uid)
    ), noMessadata)

    case DeleteComment(pos, commentId, uid) => notifyVersion("deleteComment", Json.obj(
      "p" -> pos,
      "id" -> commentId,
      "w" -> who(uid)
    ), noMessadata)

    case SetGlyphs(pos, glyphs, uid) => notifyVersion("glyphs", Json.obj(
      "p" -> pos,
      "g" -> glyphs,
      "w" -> who(uid)
    ), noMessadata)

    case SetClock(pos, clock, uid, relay) => notifyVersion("clock", Json.obj(
      "p" -> pos,
      "c" -> clock,
      "w" -> who(uid)
    ).add("relay", relay), noMessadata)

    case SetRelay(chapterId, relay) => notifyVersion("relay", Json.obj(
      "chapterId" -> chapterId,
      "relay" -> relay
    ), noMessadata)

    case SetConceal(pos, ply) => notifyVersion("conceal", Json.obj(
      "p" -> pos,
      "ply" -> ply.map(_.value)
    ), noMessadata)

    case SetLiking(liking, uid) => notifyVersion("liking", Json.obj(
      "l" -> liking,
      "w" -> who(uid)
    ), noMessadata)

    case lidraughts.chat.actorApi.ChatLine(_, line) => line match {
      case line: lidraughts.chat.UserLine =>
        notifyVersion("message", lidraughts.chat.JsonView(line), Messadata(trollish = line.troll))
      case _ =>
    }

    case ReloadUid(uid) => notifyUid("reload", JsNull)(uid)

    case ReloadUidBecauseOf(uid, chapterId) => notifyUid("reload", Json.obj(
      "chapterId" -> chapterId
    ))(uid)

    case GetVersion(promise) => promise success history.version

    case Join(uid, userId, troll, version, promise) =>
      import play.api.libs.iteratee.Concurrent
      val (enumerator, channel) = Concurrent.broadcast[JsValue]
      val member = Member(channel, userId, troll = troll)
      addMember(uid, member)
      notifyCrowd
      promise success Connected(
        prependEventsSince(version, enumerator, member),
        member
      )
      userId foreach sendStudyDoor(true)

    case NotifyCrowd =>
      delayedCrowdNotification = false
      val json =
        if (members.size <= maxSpectatorUsers) showSpectators(lightUserApi.async)(members.values)
        else studyRepo uids studyId map showSpectatorsAndMembers map some
      json foreach { notifyAll("crowd", _) }

    case Broadcast(t, msg) => notifyAll(t, msg)

    case ServerEval.Progress(chapterId, tree, analysis, division) =>
      import lidraughts.game.JsonView.divisionWriter
      notifyAll("analysisProgress", Json.obj(
        "analysis" -> analysis,
        "ch" -> chapterId,
        "tree" -> tree,
        "division" -> division
      ))
  }: Trouper.Receive) orElse lidraughts.chat.Socket.out(
    send = (t, d, _) => notifyVersion(t, d, noMessadata)
  )

  override protected def broom: Unit = {
    super.broom
    if (members.nonEmpty) keepMeAlive()
  }

  override protected def afterQuit(uid: Uid, member: Member) = {
    member.userId foreach sendStudyDoor(false)
    notifyCrowd
  }

  private def notifyCrowd: Unit =
    if (!delayedCrowdNotification) {
      delayedCrowdNotification = true
      system.scheduler.scheduleOnce(1 second)(this ! NotifyCrowd)
    }

  // always show study members
  // since that's how the client knows if they're online
  // WCC has thousands of spectators. mutable implementation.
  private def showSpectatorsAndMembers(studyMemberIds: Set[User.ID]): JsValue = {
    var nb = 0
    var titleNames = List.empty[String]
    members foreachValue { w =>
      nb = nb + 1
      w.userId.filter(studyMemberIds.contains) foreach { userId =>
        titleNames = lightUserApi.sync(userId).fold(userId)(_.titleName) :: titleNames
      }
    }
    Json.obj("nb" -> nb, "users" -> titleNames)
  }

  protected def shouldSkipMessageFor(message: Message, member: Member) =
    (message.metadata.trollish && !member.troll)

  private def who(uid: Uid) = uidToUserId(uid) map { Who(_, uid) }

  private val noMessadata = Messadata()
}

object StudySocket {

  case class Member(
      channel: JsChannel,
      userId: Option[String],
      troll: Boolean
  ) extends lidraughts.socket.SocketMember

  case class Who(u: String, s: Uid)
  import JsonView.uidWriter
  implicit private val whoWriter = Json.writes[Who]

  case class Join(uid: Uid, userId: Option[User.ID], troll: Boolean, version: Option[SocketVersion], promise: Promise[Connected])
  case class Connected(enumerator: JsEnumerator, member: Member)

  case class ReloadUid(uid: Uid)
  case class ReloadUidBecauseOf(uid: Uid, chapterId: Chapter.Id)

  case class AddNode(
      position: Position.Ref,
      node: Node,
      variant: draughts.variant.Variant,
      uid: Uid,
      sticky: Boolean,
      relay: Option[Chapter.Relay]
  )
  case class DeleteNode(position: Position.Ref, uid: Uid)
  case class Promote(position: Position.Ref, toMainline: Boolean, uid: Uid)
  case class SetPath(position: Position.Ref, uid: Uid)
  case class SetShapes(position: Position.Ref, shapes: Shapes, uid: Uid)
  case class ReloadMembers(members: StudyMembers)
  case class SetComment(position: Position.Ref, comment: Comment, uid: Uid)
  case class DeleteComment(position: Position.Ref, commentId: Comment.Id, uid: Uid)
  case class SetGlyphs(position: Position.Ref, glyphs: Glyphs, uid: Uid)
  case class SetClock(position: Position.Ref, clock: Option[Centis], uid: Uid, relay: Option[Chapter.Relay])
  case class SetRelay(chapterId: Chapter.Id, relay: Chapter.Relay)
  case class ReloadChapters(chapters: List[Chapter.Metadata])
  case object ReloadAll
  case class ChangeChapter(uid: Uid, position: Position.Ref)
  case class UpdateChapter(uid: Uid, chapterId: Chapter.Id)
  case class DescChapter(uid: Uid, chapterId: Chapter.Id, desc: Option[String])
  case class DescStudy(uid: Uid, desc: Option[String])
  case class AddChapter(uid: Uid, position: Position.Ref, sticky: Boolean)
  case class ValidationError(uid: Uid, error: String)
  case class SetConceal(position: Position.Ref, ply: Option[Chapter.Ply])
  case class SetLiking(liking: Study.Liking, uid: Uid)
  case class SetTags(chapterId: Chapter.Id, tags: draughts.format.pdn.Tags, uid: Uid)
  case class Broadcast(t: String, msg: JsObject)

  case class Messadata(trollish: Boolean = false)
  case object NotifyCrowd
}
