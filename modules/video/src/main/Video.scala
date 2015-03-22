package lila.video

import org.joda.time.DateTime

case class Video(
    id: Video.ID, // youtube ID
    title: String,
    author: String,
    targets: List[Target],
    tags: List[Tag],
    lang: Lang,
    lichess: Boolean,
    ads: Boolean,
    createdAt: DateTime) {
}

object Target {
  val BEGINNER = 1
  val INTERMEDIATE = 2
  val ADVANCED = 3
  val EXPERT = 4
}

object Video {

  type ID = String
}
