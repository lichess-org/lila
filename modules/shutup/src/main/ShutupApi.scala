package lila.shutup

import reactivemongo.api.bson.*

import lila.core.shutup.{ PublicLine, PublicSource }
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
  import lila.shutup.PublicLine.given

  lila.common.Bus.sub[lila.core.user.UserDelete]: del =>
    coll.delete.one($id(del.id))

  def getPublicLines(userId: UserId): Fu[List[PublicLine]] =
    coll
      .find($id(userId), $doc("pub" -> 1).some)
      .one[Bdoc]
      .map:
        ~_.flatMap(_.getAsOpt[List[PublicLine]]("pub"))

  def teamForumMessage(userId: UserId, text: String) = record(userId, text, TextType.TeamForumMessage)
  def publicText(userId: UserId, text: String, source: PublicSource) =
    record(userId, text, TextType.of(source), source.some)

  def privateChat(chatId: String, userId: UserId, text: String) =
    gameRepo
      .getSourceAndUserIds(GameId(chatId))
      .flatMap:
        case (source, _) if source.has(lila.core.game.Source.Friend) => funit // ignore challenges
        case (_, userIds) =>
          record(userId, text, TextType.PrivateChat, none, userIds.find(userId !=))

  def privateMessage(userId: UserId, toUserId: UserId, text: String) =
    record(userId, text, TextType.PrivateMessage, none, toUserId.some)

  private def record(
      userId: UserId,
      text: String,
      textType: TextType,
      source: Option[PublicSource] = None,
      toUserId: Option[UserId] = None
  ): Funit =
    userApi
      .isTroll(userId)
      .not
      .flatMapz:
        toUserId
          .so(relationApi.fetchFollows(_, userId))
          .not
          .flatMapz:
            for
              analysed <- Analyser(text).removeEngineIfBot(userApi.isBot(userId))
              pushPublicLine = source.ifTrue(analysed.badWords.nonEmpty).so { source =>
                $doc(
                  "pub" -> $doc(
                    "$each" -> List(lila.shutup.PublicLine.make(text, source)),
                    "$slice" -> -20
                  )
                )
              }
              push = $doc(
                textType.key -> $doc(
                  "$each" -> List(BSONDouble(analysed.ratio)),
                  "$slice" -> -textType.rotation
                )
              ) ++ pushPublicLine
              res <- coll.findAndUpdateSimplified[UserRecord](
                selector = $id(userId),
                update = $push(push),
                fetchNewObject = true,
                upsert = true
              )
              _ <- res match
                case None => fufail(s"can't find user record for $userId")
                case Some(userRecord) => legiferate(userRecord, analysed)
            yield ()

  private def legiferate(userRecord: UserRecord, analysed: TextAnalysis): Funit =
    (analysed.critical || userRecord.reports.exists(_.unacceptable)).so:
      val text = analysed.critical.so("Critical comm alert\n") ++ {
        val repText = reportText(userRecord)
        if repText.isEmpty then analysed.badWords.mkString(", ") else repText
      }
      for
        _ <- reportApi.autoCommReport(userRecord.userId, text, analysed.critical)
        _ <- coll.update.one(
          $id(userRecord.userId),
          $unset(TextType.values.map(_.key))
        )
      yield ()

  private def reportText(userRecord: UserRecord) =
    userRecord.reports
      .collect:
        case r if r.unacceptable =>
          s"${r.textType.name}: ${r.nbBad} dubious (out of ${r.ratios.size})"
      .mkString("\n")
