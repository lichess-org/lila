package lila.study

import akka.actor._
import play.api.libs.iteratee._
import play.api.libs.json._
import scala.concurrent.duration._
import scala.concurrent.Promise

import chess.Centis
import chess.format.pgn.Glyphs
import lila.hub.Trouper
import lila.socket.actorApi.{ Connected => _, _ }
import lila.socket.Socket.{ Sri, GetVersion, SocketVersion }
import lila.socket.{ SocketTrouper, History, Historical, AnaDests }
import lila.tree.Node.{ Shapes, Comment }
import lila.user.User
import lila.chat.Chat

final class StudySocket(
    system: ActorSystem,
    studyId: Study.Id,
    jsonView: JsonView,
    studyRepo: StudyRepo,
    chapterRepo: ChapterRepo,
    lightUserApi: lila.user.LightUserApi,
    protected val history: History[StudySocket.Messadata],
    sriTtl: Duration,
    lightStudyCache: LightStudyCache,
    keepMeAlive: () => Unit
) extends SocketTrouper[StudySocket.Member](system, sriTtl) with Historical[StudySocket.Member, StudySocket.Messadata] {

  import StudySocket._
  import JsonView._
  import jsonView.membersWrites
  import lila.tree.Node.{ openingWriter, commentWriter, glyphsWriter, shapesWrites, clockWrites }

  private var delayedCrowdNotification = false

  lilaBus.subscribe(this, chatClassifier)

  override def stop(): Unit = {
    super.stop()
    lilaBus.unsubscribe(this, chatClassifier)
  }

  private def chatClassifier = Chat classify Chat.Id(studyId.value)

  private def sendStudyDoor(enters: Boolean)(userId: User.ID) =
    lightStudyCache get studyId foreach {
      _ foreach { study =>
        lilaBus.publish(
          lila.hub.actorApi.study.StudyDoor(
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

    case SetPath(pos, sri) =>
      notifyVersion("path", Json.obj(
        "p" -> pos,
        "w" -> who(sri).map(whoWriter.writes)
      ), noMessadata)

    case AddNode(pos, node, variant, sri, sticky, relay) =>
      val dests = AnaDests(
        variant,
        node.fen,
        pos.path.toString,
        pos.chapterId.value.some
      )
      notifyVersion("addNode", Json.obj(
        "n" -> TreeBuilder.toBranch(node, variant),
        "p" -> pos,
        "w" -> who(sri),
        "d" -> dests.dests,
        "o" -> dests.opening,
        "s" -> sticky
      ).add("relay", relay), noMessadata)

    case DeleteNode(pos, sri) => notifyVersion("deleteNode", Json.obj(
      "p" -> pos,
      "w" -> who(sri)
    ), noMessadata)

    case Promote(pos, toMainline, sri) => notifyVersion("promote", Json.obj(
      "p" -> pos,
      "toMainline" -> toMainline,
      "w" -> who(sri)
    ), noMessadata)

    case ReloadMembers(studyMembers) =>
      notifyVersion("members", studyMembers, noMessadata)
      val ids = studyMembers.ids.toSet
      notifyIf(makeMessage("reload")) { m =>
        m.userId.exists(ids.contains)
      }

    case ReloadChapters(chapters) => notifyVersion("chapters", chapters, noMessadata)

    case ReloadAll => notifyVersion("reload", JsNull, noMessadata)

    case ChangeChapter(sri, pos) => notifyVersion("changeChapter", Json.obj(
      "p" -> pos,
      "w" -> who(sri)
    ), noMessadata)

    case UpdateChapter(sri, chapterId) => notifyVersion("updateChapter", Json.obj(
      "chapterId" -> chapterId,
      "w" -> who(sri)
    ), noMessadata)

    case DescChapter(sri, chapterId, description) => notifyVersion("descChapter", Json.obj(
      "chapterId" -> chapterId,
      "desc" -> description,
      "w" -> who(sri)
    ), noMessadata)

    case DescStudy(sri, description) => notifyVersion("descStudy", Json.obj(
      "desc" -> description,
      "w" -> who(sri)
    ), noMessadata)

    case AddChapter(sri, pos, sticky) => notifyVersion("addChapter", Json.obj(
      "p" -> pos,
      "w" -> who(sri),
      "s" -> sticky
    ), noMessadata)

    case SetShapes(pos, shapes, sri) => notifyVersion("shapes", Json.obj(
      "p" -> pos,
      "s" -> shapes,
      "w" -> who(sri)
    ), noMessadata)

    case SetComment(pos, comment, sri) => notifyVersion("setComment", Json.obj(
      "p" -> pos,
      "c" -> comment,
      "w" -> who(sri)
    ), noMessadata)

    case SetTags(chapterId, tags, sri) => notifyVersion("setTags", Json.obj(
      "chapterId" -> chapterId,
      "tags" -> tags,
      "w" -> who(sri)
    ), noMessadata)

    case DeleteComment(pos, commentId, sri) => notifyVersion("deleteComment", Json.obj(
      "p" -> pos,
      "id" -> commentId,
      "w" -> who(sri)
    ), noMessadata)

    case SetGlyphs(pos, glyphs, sri) => notifyVersion("glyphs", Json.obj(
      "p" -> pos,
      "g" -> glyphs,
      "w" -> who(sri)
    ), noMessadata)

    case SetClock(pos, clock, sri) => notifyVersion("clock", Json.obj(
      "p" -> pos,
      "c" -> clock,
      "w" -> who(sri)
    ), noMessadata)

    case ForceVariation(pos, force, sri) => notifyVersion("forceVariation", Json.obj(
      "p" -> pos,
      "force" -> force,
      "w" -> who(sri)
    ), noMessadata)

    case SetConceal(pos, ply) => notifyVersion("conceal", Json.obj(
      "p" -> pos,
      "ply" -> ply.map(_.value)
    ), noMessadata)

    case SetLiking(liking, sri) => notifyVersion("liking", Json.obj(
      "l" -> liking,
      "w" -> who(sri)
    ), noMessadata)

    case lila.chat.actorApi.ChatLine(_, line) => line match {
      case line: lila.chat.UserLine =>
        notifyVersion("message", lila.chat.JsonView(line), Messadata(trollish = line.troll))
      case _ =>
    }

    case ReloadSri(sri) => notifySri("reload", JsNull)(sri)

    case ReloadSriBecauseOf(sri, chapterId) => notifySri("reload", Json.obj(
      "chapterId" -> chapterId
    ))(sri)

    case GetVersion(promise) => promise success history.version

    case Join(sri, userId, troll, version, promise) =>
      import play.api.libs.iteratee.Concurrent
      val (enumerator, channel) = Concurrent.broadcast[JsValue]
      val member = Member(channel, userId, troll = troll)
      addMember(sri, member)
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
        else studyRepo sris studyId map showSpectatorsAndMembers map some
      json foreach { notifyAll("crowd", _) }

    case Broadcast(t, msg) => notifyAll(t, msg)

    case ServerEval.Progress(chapterId, tree, analysis, division) =>
      import lila.game.JsonView.divisionWriter
      notifyAll("analysisProgress", Json.obj(
        "analysis" -> analysis,
        "ch" -> chapterId,
        "tree" -> tree,
        "division" -> division
      ))
  }: Trouper.Receive) orElse lila.chat.Socket.out(
    send = (t, d, _) => notifyVersion(t, d, noMessadata)
  )

  override protected def broom: Unit = {
    super.broom
    if (members.nonEmpty) keepMeAlive()
  }

  override protected def afterQuit(sri: Sri, member: Member) = {
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

  private def who(sri: Sri) = sriToUserId(sri) map { Who(_, sri) }

  private val noMessadata = Messadata()
}

object StudySocket {

  case class Member(
      channel: JsChannel,
      userId: Option[String],
      troll: Boolean
  ) extends lila.socket.SocketMember

  case class Who(u: String, s: Sri)
  import JsonView.sriWriter
  implicit private val whoWriter = Json.writes[Who]

  case class Join(sri: Sri, userId: Option[User.ID], troll: Boolean, version: Option[SocketVersion], promise: Promise[Connected])
  case class Connected(enumerator: JsEnumerator, member: Member)

  case class ReloadSri(sri: Sri)
  case class ReloadSriBecauseOf(sri: Sri, chapterId: Chapter.Id)

  case class AddNode(
      position: Position.Ref,
      node: Node,
      variant: chess.variant.Variant,
      sri: Sri,
      sticky: Boolean,
      relay: Option[Chapter.Relay]
  )
  case class DeleteNode(position: Position.Ref, sri: Sri)
  case class Promote(position: Position.Ref, toMainline: Boolean, sri: Sri)
  case class SetPath(position: Position.Ref, sri: Sri)
  case class SetShapes(position: Position.Ref, shapes: Shapes, sri: Sri)
  case class ReloadMembers(members: StudyMembers)
  case class SetComment(position: Position.Ref, comment: Comment, sri: Sri)
  case class DeleteComment(position: Position.Ref, commentId: Comment.Id, sri: Sri)
  case class SetGlyphs(position: Position.Ref, glyphs: Glyphs, sri: Sri)
  case class SetClock(position: Position.Ref, clock: Option[Centis], sri: Sri)
  case class ForceVariation(position: Position.Ref, force: Boolean, sri: Sri)
  case class ReloadChapters(chapters: List[Chapter.Metadata])
  case object ReloadAll
  case class ChangeChapter(sri: Sri, position: Position.Ref)
  case class UpdateChapter(sri: Sri, chapterId: Chapter.Id)
  case class DescChapter(sri: Sri, chapterId: Chapter.Id, desc: Option[String])
  case class DescStudy(sri: Sri, desc: Option[String])
  case class AddChapter(sri: Sri, position: Position.Ref, sticky: Boolean)
  case class SetConceal(position: Position.Ref, ply: Option[Chapter.Ply])
  case class SetLiking(liking: Study.Liking, sri: Sri)
  case class SetTags(chapterId: Chapter.Id, tags: chess.format.pgn.Tags, sri: Sri)
  case class Broadcast(t: String, msg: JsObject)

  case class Messadata(trollish: Boolean = false)
  case object NotifyCrowd
}
