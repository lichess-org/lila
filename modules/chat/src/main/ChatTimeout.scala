package lila.chat

import play.api.data.Form
import play.api.data.Forms.*
import reactivemongo.api.bson.*
import scalalib.ThreadLocalRandom

import lila.db.dsl.{ *, given }

final class ChatTimeout(
    coll: Coll,
    duration: FiniteDuration
)(using Executor):

  import ChatTimeout.{ *, given }

  private val global = scalalib.cache.ExpireSetMemo[UserId](duration)

  def add(chat: UserChat, mod: User, user: User, reason: Reason, scope: Scope): Fu[Boolean] =
    isActive(chat.id, user.id).flatMap:
      if _ then fuccess(false)
      else
        if scope == Scope.Global then global.put(user.id)
        coll.insert
          .one(
            $doc(
              "_id"       -> ThreadLocalRandom.nextString(8),
              "chat"      -> chat.id,
              "mod"       -> mod.id,
              "user"      -> user.id,
              "reason"    -> reason,
              "createdAt" -> nowInstant,
              "expiresAt" -> nowInstant.plusSeconds(duration.toSeconds.toInt)
            )
          )
          .inject(true)

  def isActive(chatId: ChatId, userId: UserId): Fu[Boolean] =
    fuccess(global.get(userId)) >>| coll.exists:
      $doc(
        "chat" -> chatId,
        "user" -> userId,
        "expiresAt".$exists(true)
      )

  def history(user: User, nb: Int): Fu[List[UserEntry]] =
    coll.find($doc("user" -> user.id)).sort($sort.desc("createdAt")).cursor[UserEntry]().list(nb)

  def checkExpired: Fu[List[Reinstate]] =
    coll
      .list[Reinstate](
        $doc(
          "expiresAt".$lt(nowInstant)
        )
      )
      .flatMap:
        case Nil  => fuccess(Nil)
        case objs => coll.unsetField($inIds(objs.map(_._id)), "expiresAt", multi = true).inject(objs)

object ChatTimeout:

  export lila.core.chat.{ TimeoutReason as Reason, TimeoutScope as Scope }

  given BSONHandler[Reason] = tryHandler(
    { case BSONString(value) => Reason(value).toTry(s"Invalid reason $value") },
    x => BSONString(x.key)
  )

  case class Reinstate(_id: String, chat: ChatId, user: UserId)
  given BSONDocumentReader[Reinstate] = Macros.reader

  case class UserEntry(mod: UserId, reason: Reason, createdAt: Instant)
  given BSONDocumentReader[UserEntry] = Macros.reader

  import lila.common.Form.given
  val form = Form(
    mapping(
      "roomId" -> of[RoomId],
      "chan"   -> lila.common.Form.stringIn(Set("tournament", "swiss", "team", "study")),
      "userId" -> lila.common.Form.username.historicalField,
      "reason" -> nonEmptyText,
      "text"   -> nonEmptyText
    )(TimeoutFormData.apply)(unapply)
  )

  case class TimeoutFormData(roomId: RoomId, chan: String, userId: UserStr, reason: String, text: String)
