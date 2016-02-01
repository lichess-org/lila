package lila.message

private object Automute {

  def apply(text: String, by: lila.user.User) = by.troll || scan(text)

  private def scan(text: String) = blacklist exists text.contains

  private val blacklist = List(
    /* While links to other chess websites are welcome,
     * refer links grant the referrer money,
     * effectively inducing spam */
    "velocitychess.com/ref/",
    "chess24.com?ref="
  )
}
