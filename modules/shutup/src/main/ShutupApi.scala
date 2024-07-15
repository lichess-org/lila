package lila.shutup

import reactivemongo.api.bson.*

import lila.core.shutup.PublicSource
import lila.db.dsl.{ *, given }

final class ShutupApi(
    coll: Coll,
    gameRepo: lila.core.game.GameRepo,
    userApi: lila.core.user.UserApi,
    relationApi: lila.core.relation.RelationApi,
    reportApi: lila.core.report.ReportApi
)(using Executor)
    extends lila.core.shutup.ShutupApi:

  private given BSONDocumentHandler[UserRecord] = Macros.handler
  import PublicLine.given

  def getPublicLines(userId: UserId): Fu[List[PublicLine]] =
    coll
      .find($doc("_id" -> userId), $doc("pub" -> 1).some)
      .one[Bdoc]
      .map {
        ~_.flatMap(_.getAsOpt[List[PublicLine]]("pub"))
      }

  def teamForumMessage(userId: UserId, text: String) = record(userId, text, TextType.TeamForumMessage)
  def publicText(userId: UserId, text: String, source: PublicSource) =
    record(userId, text, TextType.of(source), source.some)

  def privateChat(chatId: String, userId: UserId, text: String) =
    gameRepo.getSourceAndUserIds(GameId(chatId)).flatMap {
      case (source, _) if source.has(lila.core.game.Source.Friend) => funit // ignore challenges
      case (_, userIds) =>
        record(userId, text, TextType.PrivateChat, none, userIds.find(userId !=))
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
    userApi.isTroll(userId).flatMap {
      if _ then funit
      else
        toUserId.so { relationApi.fetchFollows(_, userId) }.flatMap {
          if _ then funit
          else
            Analyser(text)
              .removeEngineIfBot(userApi.isBot(userId))
              .flatMap: analysed =>
                val pushPublicLine = source.ifTrue(analysed.badWords.nonEmpty).so { source =>
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
                  .flatMap:
                    case None             => fufail(s"can't find user record for $userId")
                    case Some(userRecord) => legiferate(userRecord, analysed)
        }
    }

  private def legiferate(userRecord: UserRecord, analysed: TextAnalysis): Funit =
    (analysed.critical || userRecord.reports.exists(_.unacceptable)).so {
      val text = (analysed.critical.so("Critical comm alert\n")) ++ {
        val repText = reportText(userRecord)
        if repText.isEmpty then analysed.badWords.mkString(", ") else repText
      }
      reportApi.autoCommReport(userRecord.userId, text, analysed.critical) >>
        coll.update
          .one(
            $id(userRecord.userId),
            $unset(TextType.values.map(_.key))
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
