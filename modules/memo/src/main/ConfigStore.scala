package lila.memo

import com.typesafe.config.ConfigFactory
import configs.syntax._
import configs.{ Configs, ConfigError }
import play.api.data._
import play.api.data.Forms._
import play.api.data.validation._
import scala.concurrent.duration._
import scala.util.Try

import lila.db.dsl._

final class ConfigStore[A: Configs](
    coll: Coll,
    id: String,
    ttl: FiniteDuration,
    logger: lila.log.Logger) {

  private val mongoDocKey = "config"

  private val cache = AsyncCache.single[Option[A]](
    "db.config_store",
    f = rawText.map {
      _.flatMap { text =>
        parse(text).fold(
          errs => {
            errs foreach { logger.warn(_) }
            none
          },
          res => res.some)
      }
    },
    timeToLive = ttl)

  def parse(text: String): Either[List[String], A] = try {
    ConfigFactory.parseString(text).extract[A].toEither.left.map(_.messages.toList.map(_.toString))
  }
  catch {
    case e: com.typesafe.config.ConfigException => Left(List(e.getMessage))
  }

  def get: Fu[Option[A]] = cache(true)

  def rawText: Fu[Option[String]] = coll.primitiveOne[String]($id(id), mongoDocKey)

  def set(text: String): Either[List[String], Funit] = parse(text).right map { _ =>
    coll.update($id(id), $doc(mongoDocKey -> text), upsert = true) >> cache.clear
  }

  def makeForm: Fu[Form[String]] = {
    val form = Form(single(
      "text" -> text.verifying(Constraint[String]("constraint.text_parsable") { t =>
        parse(t) match {
          case Left(errs) => Invalid(ValidationError(errs mkString ","))
          case _          => Valid
        }
      })
    ))
    rawText map {
      _.fold(form)(form.fill)
    }
  }
}

object ConfigStore {

  final class Builder(coll: Coll) {
    def apply[A: Configs](
      id: String,
      ttl: FiniteDuration,
      logger: lila.log.Logger) = new ConfigStore[A](coll, id, ttl, logger branch "config_store")
  }

  def apply(coll: Coll) = new Builder(coll)
}
