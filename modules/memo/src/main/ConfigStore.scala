package lila.memo

import com.github.blemale.scaffeine.{ AsyncLoadingCache, Scaffeine }
import com.typesafe.config.ConfigFactory
import configs.Configs
import configs.syntax._
import play.api.data.Form
import scala.util.Try

import lila.db.dsl._

final class ConfigStore[A: Configs](coll: Coll, id: String, logger: lila.log.Logger) {

  private val mongoDocKey = "config"

  private val cache: AsyncLoadingCache[Unit, Option[A]] = Scaffeine()
    .maximumSize(1)
    .buildAsyncFuture[Unit, Option[A]](_ => rawText.map {
      _.flatMap { text =>
        parse(text).fold(
          errs => {
            errs foreach { logger.warn(_) }
            none
          },
          res => res.some
        )
      }
    })

  def parse(text: String): Either[List[String], A] = try {
    ConfigFactory.parseString(text).extract[A].toEither.left.map(_.messages.toList.map(_.toString))
  } catch {
    case e: com.typesafe.config.ConfigException => Left(List(e.getMessage))
  }

  def get: Fu[Option[A]] = cache.get(())

  def rawText: Fu[Option[String]] = coll.primitiveOne[String]($id(id), mongoDocKey)

  def set(text: String): Either[List[String], Funit] = parse(text).right map { a =>
    coll.update($id(id), $doc(mongoDocKey -> text), upsert = true).void >>-
      cache.put((), fuccess(a.some))
  }

  def makeForm: Fu[Form[String]] = {
    import play.api.data.Forms._
    import play.api.data.validation._
    val form = Form(single(
      "text" -> text.verifying(Constraint[String]("constraint.text_parsable") { t =>
        parse(t) match {
          case Left(errs) => Invalid(ValidationError(errs mkString ","))
          case _ => Valid
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
    def apply[A: Configs](id: String, logger: lila.log.Logger) =
      new ConfigStore[A](coll, id, logger branch "config_store")
  }
}
