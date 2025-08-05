package lila.video

case class Video(
    _id: Video.ID, // youtube ID
    title: String,
    author: String,
    targets: List[Target],
    tags: List[Tag],
    lang: Lang,
    ads: Boolean,
    startTime: Int, // in seconds
    metadata: Youtube.Metadata,
    createdAt: Instant
):

  inline def id = _id

  def thumbnail = s"https://img.youtube.com/vi/$id/0.jpg"

  def similarity(other: Video) =
    tags.intersect(other.tags).size +
      targets.intersect(other.targets).size +
      (author == other.author).so(1)

  def durationString =
    metadata.duration.map: seconds =>
      "%02d:%02d".format(seconds / 60, seconds % 60)

  override def toString = s"[$id] $title ($author)"

object Target:
  val BEGINNER = 1
  val INTERMEDIATE = 2
  val ADVANCED = 3
  val EXPERT = 4

  def name(target: Int) = target match
    case BEGINNER => "beginner"
    case INTERMEDIATE => "intermediate"
    case ADVANCED => "advanced"
    case EXPERT => "expert"
    case _ => ""

object Video:

  type ID = String
