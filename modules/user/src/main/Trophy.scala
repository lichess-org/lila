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
    val iconChar: Option[Char],
    val url: Option[String])

  object Kind {

    object ZugMiracle extends Kind(
      key = "zugMiracle",
      name = "Zug miracle",
      iconChar = none,
      url = "http://lichess.org/qa/259/how-do-you-get-a-zug-miracle-trophy".some)

    object WayOfBerserk extends Kind(
      key = "wayOfBerserk",
      name = "The way of Berserk",
      iconChar = '`'.some,
      url = "http://lichess.org/qa/340/way-of-berserk-trophy".some)

    object MarathonWinner extends Kind(
      key = "marathonWinner",
      name = "Marathon Winner",
      iconChar = '\\'.some,
      url = "http://lichess.org/blog/VXF45yYAAPQgLH4d/chess-marathon-1".some)

    object MarathonTopTen extends Kind(
      key = "marathonTopTen",
      name = "Marathon Top 10",
      iconChar = '\\'.some,
      url = "http://lichess.org/blog/VXF45yYAAPQgLH4d/chess-marathon-1".some)

    object MarathonTopFifty extends Kind(
      key = "marathonTopFifty",
      name = "Marathon Top 50",
      iconChar = '\\'.some,
      url = "http://lichess.org/blog/VXF45yYAAPQgLH4d/chess-marathon-1".some)

    object MarathonSurvivor extends Kind(
      key = "marathonSurvivor",
      name = "Marathon #1 survivor",
      iconChar = ','.some,
      url = "http://lichess.org/blog/VXF45yYAAPQgLH4d/chess-marathon-1".some)

    object BongcloudWarrior extends Kind(
      key = "bongcloudWarrior",
      name = "Bongcloud Warrior",
      iconChar = '~'.some,
      url = "http://lichess.org/forum/lichess-feedback/bongcloud-trophy".some)

    val all = List(ZugMiracle, WayOfBerserk, MarathonWinner, MarathonTopTen, MarathonTopFifty, MarathonSurvivor, BongcloudWarrior)
    def byKey(key: String) = all find (_.key == key)
  }

  def make(userId: String, kind: Trophy.Kind) = Trophy(
    _id = ornicar.scalalib.Random nextStringUppercase 8,
    user = userId,
    kind = kind,
    date = DateTime.now)
}
