package lila.common

import play.api.ConfigLoader

import scala.jdk.CollectionConverters.*

import lila.core.config.*

object config:

  given ConfigLoader[EmailAddress] = strLoader
  given ConfigLoader[NetDomain] = strLoader
  given ConfigLoader[BaseUrl] = strLoader
  given ConfigLoader[AssetDomain] = strLoader
  given ConfigLoader[AssetBaseUrl] = strLoader
  given ConfigLoader[RateLimit] = boolLoader
  given ConfigLoader[Secret] = strLoader(Secret.apply)
  given ConfigLoader[List[String]] = ConfigLoader.seqStringLoader.map(_.toList)

  given [A](using l: ConfigLoader[A]): ConfigLoader[List[A]] =
    ConfigLoader { c => k =>
      c.getConfigList(k).asScala.toList.map { l.load(_) }
    }

  given [A](using loader: ConfigLoader[A]): ConfigLoader[Option[A]] =
    ConfigLoader[Option[A]](c => k => c.hasPath(k).option(loader.load(c, k)))

  def strLoader[A](f: String => A): ConfigLoader[A] = ConfigLoader.stringLoader.map(f)
  def intLoader[A](f: Int => A): ConfigLoader[A] = ConfigLoader.intLoader.map(f)
  def boolLoader[A](f: Boolean => A): ConfigLoader[A] = ConfigLoader.booleanLoader.map(f)

  def strLoader[A](using sr: SameRuntime[String, A]): ConfigLoader[A] = strLoader(sr.apply)
  def intLoader[A](using sr: SameRuntime[Int, A]): ConfigLoader[A] = intLoader(sr.apply)
  def boolLoader[A](using sr: SameRuntime[Boolean, A]): ConfigLoader[A] = boolLoader(sr.apply)

  opaque type GetRelativeFile = String => java.io.File
  object GetRelativeFile extends FunctionWrapper[GetRelativeFile, String => java.io.File]
