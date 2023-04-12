package lila.security

import com.github.blemale.scaffeine.Cache
import java.time.Instant

import lila.common.base.Levenshtein.isLevenshteinDistanceLessThan

final class Flood(duration: FiniteDuration):

  import Flood.*

  private val floodNumber = 4

  private val cache: Cache[Source, Messages] = lila.memo.CacheApi.scaffeineNoScheduler
    .expireAfterAccess(duration)
    .build[Source, Messages]()

  def allowMessage(source: Source, text: String): Boolean =
    val msg  = Message(text, Instant.now)
    val msgs = ~cache.getIfPresent(source)
    !duplicateMessage(msg, msgs) && !quickPost(msg, msgs) ~ {
      _ ?? cache.put(source, msg :: msgs)
    }

  private def quickPost(msg: Message, msgs: Messages): Boolean =
    msgs.lift(floodNumber) ?? (_.date isAfter msg.date.minusSeconds(10))

object Flood:

  opaque type Source = String
  object Source extends OpaqueString[Source]

  // ui/chat/src/preset.ts
  private val passList = Set(
    "Hello",
    "Good luck",
    "Have fun!",
    "You too!",
    "Good game",
    "Well played",
    "Thank you",
    "I've got to go",
    "Bye!"
  )

  private[security] case class Message(text: String, date: Instant)

  private type Messages = List[Message]

  private[security] def duplicateMessage(msg: Message, msgs: Messages): Boolean =
    !passList.contains(msg.text) && msgs.headOption.?? { m =>
      similar(m.text, msg.text) || msgs.tail.headOption.?? { m2 =>
        similar(m2.text, msg.text)
      }
    }

  private def similar(s1: String, s2: String): Boolean =
    isLevenshteinDistanceLessThan(s1, s2, (s1.length.min(s2.length) >> 3) atLeast 2)
