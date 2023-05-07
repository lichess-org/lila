package lila.shutup

import reactivemongo.api.bson.*

import lila.db.dsl.{ given, * }
import lila.game.GameRepo
import lila.hub.actorApi.shutup.PublicSource
import lila.user.UserRepo

final class ShutupApi(
    coll: Coll,
    gameRepo: GameRepo,
    userRepo: UserRepo,
    relationApi: lila.relation.RelationApi,
    reporter: lila.hub.actors.Report
)(using Executor):

  private given BSONDocumentHandler[UserRecord] = Macros.handler
  import PublicLine.given

  def getPublicLines(userId: UserId): Fu[List[PublicLine]] =
    coll
      .find($doc("_id" -> userId), $doc("pub" -> 1).some)
      .one[Bdoc]
      .map {
        ~_.flatMap(_.getAsOpt[List[PublicLine]]("pub"))
      }

  def publicForumMessage(userId: UserId, text: String) = record(userId, text, TextType.PublicForumMessage)
  def teamForumMessage(userId: UserId, text: String)   = record(userId, text, TextType.TeamForumMessage)
  def publicChat(userId: UserId, text: String, source: PublicSource) =
    record(userId, text, TextType.PublicChat, source.some)

  def privateChat(chatId: String, userId: UserId, text: String) =
    gameRepo.getSourceAndUserIds(GameId(chatId)) flatMap {
      case (source, _) if source.has(lila.game.Source.Friend) => funit // ignore challenges
      case (_, userIds) =>
        record(userId, text, TextType.PrivateChat, none, userIds find (userId !=))
    }

  def privateMessage(userId: UserId, toUserId: UserId, text: String) =
    record(userId, text, TextType.PrivateMessage, none, toUserId.some)

  private def record(
      userId: UserId,
      text: String,
      textType: TextType,
      source: Option[PublicSource] = None,
      toUserId: Option[UserId] = None
  ): Funit =
    userRepo isTroll userId flatMap {
      if _ then funit
      else
        toUserId ?? { relationApi.fetchFollows(_, userId) } flatMap {
          if _ then funit
          else
            val analysed = Analyser(text)
            val pushPublicLine = source.ifTrue(analysed.badWords.nonEmpty) ?? { source =>
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
            coll
              .findAndUpdateSimplified[UserRecord](
                selector = $id(userId),
                update = $push(push),
                fetchNewObject = true,
                upsert = true
              )
              .flatMap {
                case None             => fufail(s"can't find user record for $userId")
                case Some(userRecord) => legiferate(userRecord, analysed)
              }
        }
    }

  private def legiferate(userRecord: UserRecord, analysed: TextAnalysis): Funit =
    (analysed.critical || userRecord.reports.exists(_.unacceptable)) ?? {
      val text = (analysed.critical ?? "Critical comm alert\n") ++ {
        val repText = reportText(userRecord)
        if repText.isEmpty then analysed.badWords.mkString(", ") else repText
      }
      reporter ! lila.hub.actorApi.report.Shutup(userRecord.userId, text, analysed.critical)
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
