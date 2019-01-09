package lila.shutup

import reactivemongo.bson._

import lila.db.dsl._
import lila.game.GameRepo
import lila.hub.actorApi.shutup.PublicSource
import lila.user.UserRepo

final class ShutupApi(
    coll: Coll,
    follows: (String, String) => Fu[Boolean],
    reporter: akka.actor.ActorSelection
) {

  private implicit val doubleListHandler = bsonArrayToListHandler[Double]
  private implicit val UserRecordBSONHandler = Macros.handler[UserRecord]
  import PublicLine.PublicLineBSONHandler

  def getPublicLines(userId: String): Fu[List[PublicLine]] =
    coll.find($doc("_id" -> userId), $doc("pub" -> 1))
      .uno[Bdoc].map {
        ~_.flatMap(_.getAs[List[PublicLine]]("pub"))
      }

  def publicForumMessage(userId: String, text: String) = record(userId, text, TextType.PublicForumMessage)
  def teamForumMessage(userId: String, text: String) = record(userId, text, TextType.TeamForumMessage)
  def publicChat(userId: String, text: String, source: PublicSource) = record(userId, text, TextType.PublicChat, source.some)

  def privateChat(chatId: String, userId: String, text: String) =
    GameRepo.getSourceAndUserIds(chatId) flatMap {
      case (source, _) if source.has(lila.game.Source.Friend) => funit // ignore challenges
      case (_, userIds) =>
        record(userId, text, TextType.PrivateChat, none, userIds find (userId !=))
    }

  def privateMessage(userId: String, toUserId: String, text: String) =
    record(userId, text, TextType.PrivateMessage, none, toUserId.some)

  private def record(userId: String, text: String, textType: TextType, source: Option[PublicSource] = None, toUserId: Option[String] = None): Funit =
    UserRepo isTroll userId flatMap {
      case true => funit
      case false => toUserId ?? { follows(_, userId) } flatMap {
        case true => funit
        case false =>
          val analysed = Analyser(text)
          val pushPublicLine = source.ifTrue(analysed.nbBadWords > 0) ?? { source =>
            $doc(
              "pub" -> $doc(
                "$each" -> List(PublicLine.make(text, source)),
                "$slice" -> -20
              )
            )
          }
          val push = $doc(
            textType.key -> $doc(
              "$each" -> List(BSONDouble(analysed.ratio)),
              "$slice" -> -textType.rotation
            )
          ) ++ pushPublicLine
          coll.findAndUpdate(
            selector = $doc("_id" -> userId),
            update = $doc("$push" -> push),
            fetchNewObject = true,
            upsert = true
          ).map(_.value) map2 UserRecordBSONHandler.read flatMap {
              case None => fufail(s"can't find user record for $userId")
              case Some(userRecord) => legiferate(userRecord)
            } logFailure lila.log("shutup")
      }
    }

  private def legiferate(userRecord: UserRecord): Funit =
    userRecord.reports.exists(_.unacceptable) ?? {
      reporter ! lila.hub.actorApi.report.Shutup(userRecord.userId, reportText(userRecord))
      coll.update(
        $doc("_id" -> userRecord.userId),
        $doc("$unset" -> $doc(
          TextType.PublicForumMessage.key -> true,
          TextType.TeamForumMessage.key -> true,
          TextType.PrivateMessage.key -> true,
          TextType.PrivateChat.key -> true,
          TextType.PublicChat.key -> true
        ))
      ).void
    }

  private def reportText(userRecord: UserRecord) =
    userRecord.reports.collect {
      case r if r.unacceptable =>
        s"${r.textType.name}: ${r.nbBad} dubious (out of ${r.ratios.size})"
    }.mkString("\n")
}
