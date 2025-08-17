package lila.ui

import chess.format.Uci
import play.api.mvc.{ PathBindable, QueryStringBindable }
import scalalib.newtypes.SameRuntime

import lila.core.id.*
import lila.core.study.Order as StudyOrder
import lila.core.ublog.{ BlogsBy, QualityFilter as BlogQualityFilter }

object LilaRouter:

  given opaquePathBindable[T, A](using
      sr: SameRuntime[A, T],
      rs: SameRuntime[T, A],
      bindable: PathBindable[A]
  ): PathBindable[T] =
    bindable.transform(sr.apply, rs.apply)

  given opaqueQueryStringBindable[T, A](using
      sr: SameRuntime[A, T],
      rs: SameRuntime[T, A],
      bindable: QueryStringBindable[A]
  ): QueryStringBindable[T] =
    bindable.transform(sr.apply, rs.apply)

  private def strPath[A](
      parse: String => Option[A],
      error: => String,
      write: A => String = (_: A).toString
  ): PathBindable[A] = new:
    def bind(_key: String, value: String) = parse(value).toRight(error)
    def unbind(_key: String, value: A) = write(value)

  given PathBindable[UserStr] = strPath[UserStr](UserStr.read, "Invalid Lichess username")
  given PathBindable[PerfKey] = strPath[PerfKey](PerfKey.apply, "Invalid Lichess performance key")
  given PathBindable[GameId] = summon[PathBindable[GameAnyId]].transform(_.gameId, _.into(GameAnyId))
  given PathBindable[Color] =
    strPath[Color](Color.fromName, "Invalid chess color, should be white or black", _.name)
  given PathBindable[Uci] = strPath[Uci](Uci.apply, "Invalid UCI move", _.uci)
  given PathBindable[StudyOrder] = strPath[StudyOrder](
    s => scala.util.Try(StudyOrder.valueOf(s).some).getOrElse(None),
    "Invalid study order"
  )

  private def urlEncode(str: String) = java.net.URLEncoder.encode(str, "utf-8")

  private def strQueryString[A](
      parse: String => Option[A],
      error: => String,
      write: A => String
  ): QueryStringBindable[A] = new:
    def bind(key: String, params: Map[String, Seq[String]]) =
      params
        .get(key)
        .flatMap(_.headOption)
        .map: value =>
          parse(value).toRight(error)
    def unbind(key: String, value: A) = s"${urlEncode(key)}=${urlEncode(write(value))}"

  given QueryStringBindable[Color] =
    strQueryString[Color](Color.fromName, "Invalid chess color, should be white or black", _.name)
  given QueryStringBindable[Uci] = strQueryString[Uci](Uci.apply, "Invalid UCI move", _.uci)
  given QueryStringBindable[BlogsBy] = strQueryString[BlogsBy](BlogsBy.fromName, "Invalid order", _.toString)
  given QueryStringBindable[BlogQualityFilter] =
    strQueryString[BlogQualityFilter](BlogQualityFilter.fromName, "Invalid quality", _.name)
