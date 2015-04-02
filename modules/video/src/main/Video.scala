package lila.video

import org.joda.time.DateTime

case class Video(
    _id: Video.ID, // youtube ID
    title: String,
    author: String,
    targets: List[Target],
    tags: List[Tag],
    lang: Lang,
    ads: Boolean,
    metadata: Youtube.Metadata,
    createdAt: DateTime) {

  def id = _id

  def thumbnail = s"http://img.youtube.com/vi/$id/0.jpg"

  def similarity(other: Video) =
    (tags intersect other.tags).size +
      (targets intersect other.targets).size +
      (if (author == other.author) 1 else 0)

  override def toString = s"[$id] $title ($author)"
}

object Target {
  val BEGINNER = 1
  val INTERMEDIATE = 2
  val ADVANCED = 3
  val EXPERT = 4

  def name(target: Int) = target match {
    case BEGINNER     => "beginner"
    case INTERMEDIATE => "intermediate"
    case ADVANCED     => "advanced"
    case EXPERT       => "expert"
    case _            => ""
  }
}

object Video {

  type ID = String
}
