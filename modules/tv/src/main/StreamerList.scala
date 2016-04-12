package lila.tv

import com.typesafe.config.{ Config, ConfigFactory }
import scala.collection.JavaConversions._
import scala.util.{ Try, Success, Failure }

final class StreamerList(
    val store: {
      def get: Fu[String]
      def set(text: String): Funit
    }) {

  import StreamerList._

  def get: Fu[List[Streamer]] = store.get.map { text =>
    validate(text)._1
  }

  def find(id: String): Fu[Option[Streamer]] = get map (_ find (_.id == id))

  private[tv] def lichessIds: Fu[Set[String]] = get map {
    _.filter(_.featured).map(_.lichessName.toLowerCase).toSet
  }

  private[tv] def validate(text: String): (List[Streamer], List[Exception]) = Try {
    ConfigFactory.parseString(text).getConfigList("streamers").toList.map { c =>
      Try {
        Streamer(
          service = c getString "service" match {
            case s if s == "twitch"  => Twitch
            case s if s == "hitbox"  => Hitbox
            case s if s == "youtube" => Youtube
            case s                   => sys error s"Invalid service name: $s"
          },
          streamerName = c getString "streamer_name",
          streamerNameForDisplay = Try(c getString "streamer_name_for_display").toOption,
          lichessName = c getString "lichess_name",
          featured = c.getBoolean("featured"),
          chat = c.getBoolean("chat"))
      }
    }.foldLeft(List.empty[Streamer] -> List.empty[Exception]) {
      case ((res, err), Success(r)) => (r :: res, err)
      case ((res, err), Failure(e: Exception)) =>
        lila.log("tv").warn("streamer", e)
        (res, e :: err)
      case (_, Failure(e)) => throw e
    }
  } match {
    case Failure(e: Exception) => (Nil, List(e))
    case Failure(e)            => throw e
    case Success(v)            => v
  }

  def form = {
    import play.api.data._
    import play.api.data.Forms._
    import play.api.data.validation._
    Form(single(
      "text" -> text.verifying(Constraint[String]("constraint.text_parsable") { t =>
        validate(t) match {
          case (_, Nil)  => Valid
          case (_, errs) => Invalid(ValidationError(errs.map(_.getMessage) mkString ","))
        }
      })
    ))
  }
}

object StreamerList {

  sealed trait Service
  case object Twitch extends Service
  case object Hitbox extends Service
  case object Youtube extends Service

  def findTwitch = find(Twitch) _
  def findHitbox = find(Hitbox) _
  def findYoutube = find(Youtube) _
  def find(service: Service)(streamers: List[Streamer])(name: String) =
    streamers.find { s =>
      s.service == service && s.streamerName.toLowerCase == name.toLowerCase
    }

  case class Streamer(
      service: Service,
      streamerName: String,
      streamerNameForDisplay: Option[String],
      lichessName: String,
      featured: Boolean,
      chat: Boolean) {

    def showStreamerName = streamerNameForDisplay | streamerName

    def twitch = service == Twitch
    def hitbox = service == Hitbox
    def youtube = service == Youtube

    def id = s"$streamerName@${service.toString.toLowerCase}"
  }
}
