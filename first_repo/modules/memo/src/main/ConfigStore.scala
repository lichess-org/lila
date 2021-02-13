package lila.memo

import com.typesafe.config.ConfigFactory
import play.api.ConfigLoader
import play.api.data.Form

import lila.db.dsl._

final class ConfigStore[A](coll: Coll, id: String, cacheApi: CacheApi, logger: lila.log.Logger)(implicit
    ec: scala.concurrent.ExecutionContext,
    loader: ConfigLoader[A]
) {

  private val mongoDocKey = "config"

  private val cache = cacheApi.unit[Option[A]] {
    _.buildAsyncFuture(_ =>
      rawText.map {
        _.flatMap { text =>
          parse(text).fold(
            errs => {
              errs foreach { logger.warn(_) }
              none
            },
            res => res.some
          )
        }
      }
    )
  }

  def parse(text: String): Either[List[String], A] =
    try {
      Right(loader.load(ConfigFactory.parseString(text)))
    } catch {
      case e: Exception => Left(List(e.getMessage))
    }

  def get: Fu[Option[A]] = cache.get(())

  def rawText: Fu[Option[String]] = coll.primitiveOne[String]($id(id), mongoDocKey)

  def set(text: String): Either[List[String], Funit] =
    parse(text) map { a =>
      coll.update.one($id(id), $doc(mongoDocKey -> text), upsert = true).void >>-
        cache.put((), fuccess(a.some))
    }

  def makeForm: Fu[Form[String]] = {
    import play.api.data.Forms._
    import play.api.data.validation._
    val form = Form(
      single(
        "text" -> text.verifying(Constraint[String]("constraint.text_parsable") { t =>
          parse(t) match {
            case Left(errs) => Invalid(ValidationError(errs mkString ","))
            case _          => Valid
          }
        })
      )
    )
    rawText map {
      _.fold(form)(form.fill)
    }
  }
}

object ConfigStore {

  final class Builder(db: lila.db.Db, config: MemoConfig, cacheApi: CacheApi)(implicit
      ec: scala.concurrent.ExecutionContext
  ) {
    private val coll = db(config.configColl)

    def apply[A: ConfigLoader](id: String, logger: lila.log.Logger) =
      new ConfigStore[A](coll, id, cacheApi, logger branch "configStore")
  }
}
