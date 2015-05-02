package lila.shutup

import org.joda.time.DateTime
import reactivemongo.bson._
import reactivemongo.bson.Macros
import reactivemongo.core.commands._
import scala.concurrent.duration._

import lila.db.BSON._
import lila.db.Types.Coll
import lila.game.GameRepo
import lila.user.UserRepo

final class ShutupApi(
    coll: Coll,
    follows: (String, String) => Fu[Boolean],
    reporter: akka.actor.ActorSelection) {

  private implicit val doubleListHandler = bsonArrayToListHandler[Double]
  private implicit val UserRecordBSONHandler = Macros.handler[UserRecord]

  def publicForumMessage(userId: String, text: String) = record(userId, text, TextType.PublicForumMessage)
  def teamForumMessage(userId: String, text: String) = record(userId, text, TextType.TeamForumMessage)
  def publicChat(chatId: String, userId: String, text: String) = record(userId, text, TextType.PublicChat)

  def privateChat(chatId: String, userId: String, text: String) =
    GameRepo.getUserIds(chatId) map {
      _ find (userId !=)
    } flatMap {
      record(userId, text, TextType.PrivateChat, _)
    }

  def privateMessage(userId: String, toUserId: String, text: String) =
    record(userId, text, TextType.PrivateMessage, toUserId.some)

  private def record(userId: String, text: String, textType: TextType, toUserId: Option[String] = None): Funit =
    UserRepo isTroll userId flatMap {
      case true => funit
      case false => toUserId ?? { follows(userId, _) } flatMap {
        case true => funit
        case false => coll.db.command {
          FindAndModify(
            collection = coll.name,
            query = BSONDocument("_id" -> userId),
            modify = Update(
              update = BSONDocument("$push" -> BSONDocument(
                textType.key -> BSONDocument(
                  "$each" -> List(BSONDouble(Analyser(text).ratio)),
                  "$slice" -> -textType.rotation)
              )),
              fetchNewObject = true),
            upsert = true
          )
        } map2 UserRecordBSONHandler.read flatMap {
          case None             => fufail(s"can't find user record for $userId")
          case Some(userRecord) => legiferate(userRecord)
        } logFailure "ShutupApi"
      }
    }

  private def legiferate(userRecord: UserRecord): Funit =
    userRecord.reports.exists(_.unacceptable) ?? {
      reporter ! lila.hub.actorApi.report.Shutup(userRecord.userId, reportText(userRecord))
      coll.remove(BSONDocument("_id" -> userRecord.userId)).void
    }

  private def reportText(userRecord: UserRecord) =
    "[AUTOREPORT]\n" + userRecord.reports.collect {
      case r if r.unacceptable =>
        s"${r.textType.name}: ${r.nbBad} dubious (out of ${r.ratios.size})"
    }.mkString("\n")
}
