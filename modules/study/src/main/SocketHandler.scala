package lila.study

import scala.concurrent.duration._

import akka.actor._
import akka.pattern.ask
import play.api.libs.functional.syntax._
import play.api.libs.json._

import chess.format.pgn.Glyph
import lila.chat.Chat
import lila.common.ApiVersion
import lila.common.PimpedJson._
import lila.hub.actorApi.map._
import lila.socket.actorApi.{ Connected => _, _ }
import lila.socket.Socket.makeMessage
import lila.socket.Socket.{ Sri, SocketVersion }
import lila.socket.{ Handler, AnaMove, AnaDrop, AnaAny }
import lila.tree.Node.{ Shape, Shapes, Comment, Gamebook }
import lila.user.User
import makeTimeout.short
import actorApi.Who

final class SocketHandler(
    hub: lila.hub.Env,
    socketMap: SocketMap,
    chat: ActorSelection,
    api: StudyApi,
    evalCacheHandler: lila.evalCache.EvalCacheSocketHandler
) {

  import Handler.AnaRateLimit
  import JsonView.shapeReader

  private val InviteLimitPerUser = new lila.memo.RateLimit[User.ID](
    credits = 50,
    duration = 24 hour,
    name = "study invites per user",
    key = "study_invite.user"
  )

  def makeController(
    socket: StudySocket,
    studyId: Study.Id,
    sri: Sri,
    member: StudySocket.Member,
    user: Option[User]
  ): Handler.Controller = ({
    case ("invite", o) => for {
      byUserId <- member.userId
      username <- o str "d"
    } InviteLimitPerUser(byUserId, cost = 1) {
      api.invite(byUserId, studyId, username, socket,
        onError = err => member push makeMessage("error", err))
    }

  }: Handler.Controller) orElse evalCacheHandler(sri, member, user) orElse lila.chat.Socket.in(
    chatId = Chat.Id(studyId.value),
    member = member,
    chat = chat,
    canTimeout = Some { suspectId =>
      user.?? { u =>
        api.isContributor(studyId, u.id) >>& !api.isMember(studyId, suspectId)
      }
    },
    publicSource = none // the "talk" event is handled by the study API
  )

  private def reading[A](o: JsValue)(f: A => Unit)(implicit reader: Reads[A]): Unit =
    o obj "d" flatMap { d => reader.reads(d).asOpt } foreach f

  private case class AtPosition(path: String, chapterId: Chapter.Id) {
    def ref = Position.Ref(chapterId, Path(path))
  }
  private implicit val chapterIdReader = stringIsoReader(Chapter.idIso)
  private implicit val chapterNameReader = stringIsoReader(Chapter.nameIso)
  private implicit val atPositionReader = (
    (__ \ "path").read[String] and
    (__ \ "ch").read[Chapter.Id]
  )(AtPosition.apply _)
  private case class SetRole(userId: String, role: String)
  private implicit val SetRoleReader = Json.reads[SetRole]
  private implicit val ChapterDataReader = Json.reads[ChapterMaker.Data]
  private implicit val ChapterEditDataReader = Json.reads[ChapterMaker.EditData]
  private implicit val ChapterDescDataReader = Json.reads[ChapterMaker.DescData]
  private implicit val StudyDataReader = Json.reads[Study.Data]
  private implicit val setTagReader = Json.reads[actorApi.SetTag]
  private implicit val gamebookReader = Json.reads[Gamebook]
  private implicit val explorerGame = Json.reads[actorApi.ExplorerGame]

  def getSocket(studyId: Study.Id) = socketMap getOrMake studyId.value

  def join(
    studyId: Study.Id,
    sri: Sri,
    user: Option[User],
    version: Option[SocketVersion],
    apiVersion: ApiVersion
  ): Fu[JsSocketHandler] = {
    val socket = getSocket(studyId)
    join(studyId, sri, user, socket, member => makeController(socket, studyId, sri, member, user = user), version, apiVersion)
  }

  def join(
    studyId: Study.Id,
    sri: Sri,
    user: Option[User],
    socket: StudySocket,
    controller: StudySocket.Member => Handler.Controller,
    version: Option[SocketVersion],
    apiVersion: ApiVersion
  ): Fu[JsSocketHandler] =
    socket.ask[StudySocket.Connected](StudySocket.Join(sri, user.map(_.id), user.??(_.troll), version, _)) map {
      case StudySocket.Connected(enum, member) => Handler.iteratee(
        hub,
        controller(member),
        member,
        socket,
        sri,
        apiVersion
      ) -> enum
    }
}
