package lila.memo

object Snooze:

  enum Duration(val value: FiniteDuration, val name: String):
    case TwentyMinutes extends Duration(20.minutes, "20 minutes")
    case OneHour extends Duration(1.hour, "one hour")
    case ThreeHours extends Duration(3.hours, "three hours")
    case OneDay extends Duration(1.day, "one day")
    case NextDeploy extends Duration(30.days, "until next deploy")
  object Duration:
    def apply(key: String) = Duration.values.find(_.toString == key)

final class Snoozer[Key](cacheApi: CacheApi)(using userIdOf: UserIdOf[Key]):

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
    Snooze.Duration(duration).foreach { set(key, _) }

  def snoozedKeysOf(snoozerId: UserId): Iterable[Key] =
    store.asMap().keys.collect { case key if userIdOf(key) == snoozerId => key }
