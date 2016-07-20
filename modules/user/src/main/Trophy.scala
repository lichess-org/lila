package lila.user

import org.joda.time.DateTime

case class Trophy(
  _id: String, // random
  user: String,
  kind: Trophy.Kind,
  date: DateTime)

object Trophy {

  sealed abstract class Kind(
    val key: String,
    val name: String,
    val icon: Option[String],
    val url: Option[String],
    val klass: Option[String])

  object Kind {

    object ZugMiracle extends Kind(
      key = "zugMiracle",
      name = "Zug miracle",
      icon = none,
      url = "//lichess.org/qa/259/how-do-you-get-a-zug-miracle-trophy".some,
      none)

    object WayOfBerserk extends Kind(
      key = "wayOfBerserk",
      name = "The way of Berserk",
      icon = "`".some,
      url = "//lichess.org/qa/340/way-of-berserk-trophy".some,
      "fire_trophy".some)

    object MarathonWinner extends Kind(
      key = "marathonWinner",
      name = "Marathon Winner",
      icon = "\\".some,
      url = none,
      "fire_trophy".some)

    object MarathonTopTen extends Kind(
      key = "marathonTopTen",
      name = "Marathon Top 10",
      icon = "\\".some,
      url = none,
      "fire_trophy".some)

    object MarathonTopFifty extends Kind(
      key = "marathonTopFifty",
      name = "Marathon Top 50",
      icon = "\\".some,
      url = none,
      "fire_trophy".some)

    object MarathonTopHundred extends Kind(
      key = "marathonTopHundred",
      name = "Marathon Top 100",
      icon = "\\".some,
      url = none,
      "fire_trophy".some)

    object MarathonSurvivor extends Kind(
      key = "marathonSurvivor",
      name = "Marathon #1 survivor",
      icon = ",".some,
      url = "//lichess.org/blog/VXF45yYAAPQgLH4d/chess-marathon-1".some,
      "fire_trophy".some)

    object BongcloudWarrior extends Kind(
      key = "bongcloudWarrior",
      name = "Bongcloud Warrior",
      icon = "~".some,
      url = "//lichess.org/forum/lichess-feedback/bongcloud-trophy".some,
      "fire_trophy".some)

    object Developer extends Kind(
      key = "developer",
      name = "Lichess developer",
      icon = "&#xe000;".some,
      url = "https://github.com/ornicar/lila/graphs/contributors".some,
      "icon3d".some)

    object Moderator extends Kind(
      key = "moderator",
      name = "Lichess moderator",
      icon = "&#xe002;".some,
      url = "//lichess.org/report".some,
      "icon3d".some)

    object Streamer extends Kind(
      key = "streamer",
      name = "Lichess streamer",
      icon = "&#xe003;".some,
      url = "//lichess.org/help/stream-on-lichess".some,
      "icon3d".some)

    val all = List(
      ZugMiracle,
      WayOfBerserk,
      MarathonSurvivor,
      MarathonWinner, MarathonTopTen, MarathonTopFifty, MarathonTopHundred,
      BongcloudWarrior,
      Developer, Moderator,
      Streamer)
    def byKey(key: String) = all find (_.key == key)
  }

  def make(userId: String, kind: Trophy.Kind) = Trophy(
    _id = ornicar.scalalib.Random nextStringUppercase 8,
    user = userId,
    kind = kind,
    date = DateTime.now)
}
