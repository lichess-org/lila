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
    val iconChar: Option[Char])

  object Kind {

    object ZugMiracle extends Kind(
      key = "zugMiracle",
      name = "Zug miracle",
      iconChar = none)

    object WayOfBerserk extends Kind(
      key = "wayOfBerserk",
      name = "The way of Berserk",
      iconChar = '`'.some)

    object MarathonWinner extends Kind(
      key = "marathonWinner",
      name = "Marathon Winner",
      iconChar = '\\'.some)

    val all = List(ZugMiracle, WayOfBerserk, MarathonWinner)
    def byKey(key: String) = all find (_.key == key)
  }

  def make(user: User, kind: Trophy.Kind) = Trophy(
    _id = ornicar.scalalib.Random nextStringUppercase 8,
    user = user.id,
    kind = kind,
    date = DateTime.now)
}
