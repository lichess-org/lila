package lila.forum

import reactivemongo.api.bson.*

import scalalib.Iso
import lila.db.dsl.{ *, given }
import lila.core.forum.{ ForumPostMini, ForumTopicMini }

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

  given BSONDocumentHandler[ForumPost]  = Macros.handler
  given BSONDocumentHandler[ForumTopic] = Macros.handler

  given BSONDocumentHandler[ForumPostMini]  = Macros.handler
  given BSONDocumentHandler[ForumTopicMini] = Macros.handler
