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

  def validate(text: String): (List[Streamer], List[Exception]) = Try {
    ConfigFactory.parseString(text).getConfigList("streamers").toList.map { c =>
      Try {
        Streamer(
          service = c getString "service" match {
            case s if s == "twitch" => Twitch
            case s if s == "hitbox" => Hitbox
            case s                  => sys error s"Invalid service name: $s"
          },
          streamerName = c getString "streamer_name",
          lichessName = c getString "lichess_name",
          featured = c.getBoolean("featured"),
          chat = c.getBoolean("chat"))
      }
    }.foldLeft(List.empty[Streamer] -> List.empty[Exception]) {
      case ((res, err), Success(r))            => (r :: res, err)
      case ((res, err), Failure(e: Exception)) => (res, e :: err)
      case (_, Failure(e))                     => throw e
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

  case class Streamer(
      service: Service,
      streamerName: String,
      lichessName: String,
      featured: Boolean,
      chat: Boolean) {

    def twitch = service == Twitch
    def hitbox = service == Hitbox
  }
}
