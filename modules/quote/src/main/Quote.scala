package lidraughts.quote

import play.api.libs.json._
import scala.util.Random

final class Quote(val text: String, val author: String)

object Quote {

  def one = all(Random.nextInt(all.size))

  def one(seed: String) = all(new Random(seed.hashCode).nextInt(all.size))

  val all = Vector(
    new Quote("Draughts is fun.", "Ton Sijbrands"),
    new Quote("Let's play draughts!", "Klaas Leijenaar"),

    // lidraughts facts
    new Quote("All features for free; for everyone; forever.", "lidraughts.org"),
    new Quote("We will never display ads.", "lidraughts.org"),
    new Quote("We do not track you. It's a rare feature, nowadays.", "lidraughts.org"),
    new Quote("Everyone is a premium user.", "lidraughts.org")
  )

  implicit def quoteWriter: OWrites[Quote] = OWrites { q =>
    Json.obj(
      "text" -> q.text,
      "author" -> q.author
    )
  }
}
