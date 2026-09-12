package lila.forum

import reactivemongo.api.bson.*
import scalalib.Iso

import lila.core.forum.{ ForumPostMini, ForumTopicMini }
import lila.db.dsl.{ *, given }

private object BSONHandlers:

  given BSONDocumentHandler[ForumCateg] = Macros.handler

  given BSONDocumentHandler[OldVersion] = Macros.handler

  private given reactionIso: Iso.StringIso[ForumPost.Reaction] =
    Iso.string(key => ForumPost.Reaction(key).err(s"Unknown reaction $key"), _.key)

  given BSONHandler[ForumPost.Reaction] = quickHandler[ForumPost.Reaction](
    { case BSONString(key) => reactionIso.from(key) },
    reaction => BSONString(reaction.key)
  )

  private given BSONHandler[ForumPost.Reactions] = typedMapHandlerIso[ForumPost.Reaction, Set[UserId]]

  given BSONDocumentHandler[ForumPost] = Macros.handler
  given BSONDocumentHandler[ForumTopic] = Macros.handler

  given BSONDocumentHandler[ForumPostMini] = Macros.handler
  given BSONDocumentHandler[ForumTopicMini] = Macros.handler

  given BSONDocumentHandler[Usermod] =
    given BSONHandler[Usermod.Reason] = quickHandler[Usermod.Reason](
      { case BSONString(key) => Usermod.Reason(key).err(s"Unknown usermod reason $key") },
      reason => BSONString(reason.key)
    )
    given complaints: BSONHandler[Map[UserId, Usermod.Reason]] =
      typedMapHandler[UserId, Usermod.Reason]
    given BSONDocumentHandler[Usermod.Report] = Macros.handler
    given negative: BSONHandler[Map[ForumPostId, Usermod.Report]] =
      typedMapHandler[ForumPostId, Usermod.Report]
    given positiveReactions: BSONHandler[Map[UserId, Instant]] =
      typedMapHandler[UserId, Instant]
    given positive: BSONHandler[Map[ForumPostId, Map[UserId, Instant]]] =
      typedMapHandler[ForumPostId, Map[UserId, Instant]]
    Macros.handler
