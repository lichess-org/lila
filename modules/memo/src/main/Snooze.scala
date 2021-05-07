package lila.memo

import scala.concurrent.duration._

object Snooze {

  sealed abstract class Duration(val value: FiniteDuration, val name: String)

  object Duration {
    case object TwentyMinutes extends Duration(20 minutes, "20 minutes")
    case object OneHour       extends Duration(1 hour, "one hour")
    case object ThreeHours    extends Duration(3 hours, "three hours")
    case object OneDay        extends Duration(1 day, "one day")
    case object NextDeploy    extends Duration(30 days, "until next deploy")
    val all                = List(TwentyMinutes, OneHour, ThreeHours, OneDay, NextDeploy)
    def apply(key: String) = all.find(_.toString == key)
  }

  trait Key {
    val snoozerId: String
  }
}

final class Snoozer[Key <: Snooze.Key](cacheApi: CacheApi) {

  private val store = cacheApi.notLoadingSync[Key, Snooze.Duration](256, "appeal.snooze")(
    _.expireAfter[Key, Snooze.Duration](
      create = (_, duration) => duration.value,
      update = (_, duration, _) => duration.value,
      read = (_, _, current) => current
    ).build()
  )

  def set(key: Key, duration: Snooze.Duration): Unit =
    store.put(key, duration)

  def set(key: Key, duration: String): Unit =
    Snooze.Duration(duration) foreach { set(key, _) }

  def snoozedKeysOf(snoozerId: String): Iterable[Key] =
    store.asMap().keys.collect { case key if key.snoozerId == snoozerId => key }
}
