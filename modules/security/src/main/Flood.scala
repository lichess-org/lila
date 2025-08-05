package lila.security

import com.github.blemale.scaffeine.Cache

import lila.core.security.FloodSource as Source

final class Flood(using Executor) extends lila.core.security.FloodApi:

  import Flood.*

  private val floodNumber = 4

  private val cache: Cache[Source, Messages] = lila.memo.CacheApi.scaffeineNoScheduler
    .expireAfterAccess(1.minute)
    .build[Source, Messages]()

  def allowMessage(source: Source, text: String): Boolean =
    val msg = Message(text, nowInstant)
    val msgs = ~cache.getIfPresent(source)
    val ok = !duplicateMessage(msg, msgs) && !quickPost(msg, msgs)
    if ok then cache.put(source, msg :: msgs)
    ok

  private def quickPost(msg: Message, msgs: Messages): Boolean =
    msgs.lift(floodNumber).exists(_.date.isAfter(msg.date.minusSeconds(10)))

object Flood:

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
    !passList.contains(msg.text) && msgs.headOption.so: m =>
      similar(m.text, msg.text) || msgs.tail.headOption.so: m2 =>
        similar(m2.text, msg.text)

  private def similar(s1: String, s2: String): Boolean =
    scalalib.Levenshtein.isDistanceLessThan(s1, s2, (s1.length.min(s2.length) >> 3).atLeast(2))
