package lila.shutup

import reactivemongo.api.bson._

import lila.db.dsl._
import lila.game.GameRepo
import lila.hub.actorApi.shutup.PublicSource
import lila.user.{ User, UserRepo }

final class ShutupApi(
    coll: Coll,
    gameRepo: GameRepo,
    userRepo: UserRepo,
    relationApi: lila.relation.RelationApi,
    reporter: lila.hub.actors.Report
)(implicit ec: scala.concurrent.ExecutionContext) {

  implicit private val UserRecordBSONHandler = Macros.handler[UserRecord]
  import PublicLine.PublicLineBSONHandler

  def getPublicLines(userId: User.ID): Fu[List[PublicLine]] =
    coll
      .find($doc("_id" -> userId), $doc("pub" -> 1).some)
      .one[Bdoc]
      .map {
        ~_.flatMap(_.getAsOpt[List[PublicLine]]("pub"))
      }

  def publicForumMessage(userId: User.ID, text: String) = record(userId, text, TextType.PublicForumMessage)
  def teamForumMessage(userId: User.ID, text: String)   = record(userId, text, TextType.TeamForumMessage)
  def publicChat(userId: User.ID, text: String, source: PublicSource) =
    record(userId, text, TextType.PublicChat, source.some)

  def privateChat(chatId: String, userId: User.ID, text: String) =
    gameRepo.getSourceAndUserIds(chatId) flatMap {
      case (source, _) if source.has(lila.game.Source.Friend) => funit // ignore challenges
      case (_, userIds) =>
        record(userId, text, TextType.PrivateChat, none, userIds find (userId !=))
    }

  def privateMessage(userId: User.ID, toUserId: User.ID, text: String) =
    record(userId, text, TextType.PrivateMessage, none, toUserId.some)

  private def record(
      userId: User.ID,
      text: String,
      textType: TextType,
      source: Option[PublicSource] = None,
      toUserId: Option[User.ID] = None
  ): Funit =
    userRepo isTroll userId flatMap {
      case true => funit
      case false =>
        toUserId ?? { relationApi.fetchFollows(_, userId) } flatMap {
          case true => funit
          case false =>
            val analysed = Analyser(text)
            val pushPublicLine = source.ifTrue(analysed.nbBadWords > 0) ?? { source =>
              $doc(
                "pub" -> $doc(
                  "$each"  -> List(PublicLine.make(text, source)),
                  "$slice" -> -20
                )
              )
            }
            val push = $doc(
              textType.key -> $doc(
                "$each"  -> List(BSONDouble(analysed.ratio)),
                "$slice" -> -textType.rotation
              )
            ) ++ pushPublicLine
            coll.ext
              .findAndUpdate[UserRecord](
                selector = $id(userId),
                update = $push(push),
                fetchNewObject = true,
                upsert = true
              )
              .flatMap {
                case None             => fufail(s"can't find user record for $userId")
                case Some(userRecord) => legiferate(userRecord)
              }
              .recover(lila.db.ignoreDuplicateKey)
        }
    }

  private def legiferate(userRecord: UserRecord): Funit =
    userRecord.reports.exists(_.unacceptable) ?? {
      reporter ! lila.hub.actorApi.report.Shutup(userRecord.userId, reportText(userRecord))
      coll.update
        .one(
          $id(userRecord.userId),
          $unset(
            TextType.PublicForumMessage.key,
            TextType.TeamForumMessage.key,
            TextType.PrivateMessage.key,
            TextType.PrivateChat.key,
            TextType.PublicChat.key
          )
        )
        .void
    }

  private def reportText(userRecord: UserRecord) =
    userRecord.reports
      .collect {
        case r if r.unacceptable =>
          s"${r.textType.name}: ${r.nbBad} dubious (out of ${r.ratios.size})"
      }
      .mkString("\n")
}
