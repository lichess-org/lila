package lila.common

import play.api.ConfigLoader

import scala.jdk.CollectionConverters.*

import lila.common.autoconfig.*

object config:

  opaque type Every = FiniteDuration
  object Every extends OpaqueDuration[Every]
  opaque type AtMost = FiniteDuration
  object AtMost extends OpaqueDuration[AtMost]
  opaque type Delay = FiniteDuration
  object Delay extends OpaqueDuration[Delay]

  opaque type CollName = String
  object CollName extends OpaqueString[CollName]

  case class Secret(value: String) extends AnyVal:
    override def toString = "Secret(****)"

  opaque type BaseUrl = String
  object BaseUrl extends OpaqueString[BaseUrl]

  opaque type NetDomain = String
  object NetDomain extends OpaqueString[NetDomain]

  opaque type AssetDomain = String
  object AssetDomain extends OpaqueString[AssetDomain]

  opaque type AssetBaseUrl = String
  object AssetBaseUrl extends OpaqueString[AssetBaseUrl]

  opaque type AssetBaseUrlInternal = String
  object AssetBaseUrlInternal extends OpaqueString[AssetBaseUrlInternal]

  opaque type RateLimit = Boolean
  object RateLimit extends YesNo[RateLimit]

  opaque type EndpointUrl = String
  object EndpointUrl extends OpaqueString[EndpointUrl]

  case class Credentials(user: String, password: Secret):
    def show = s"$user:${password.value}"
  object Credentials:
    def read(str: String): Option[Credentials] = str.split(":") match
      case Array(user, password) => Credentials(user, Secret(password)).some
      case _                     => none

  case class HostPort(host: String, port: Int):
    def show = s"$host:$port"
  object HostPort:
    def read(str: String): Option[HostPort] = str.split(":") match
      case Array(host, port) => port.toIntOption.map(HostPort(host, _))
      case _                 => none

  given ConfigLoader[Secret]       = strLoader(Secret.apply)
  given ConfigLoader[List[String]] = ConfigLoader.seqStringLoader.map(_.toList)

  given [A](using l: ConfigLoader[A]): ConfigLoader[List[A]] =
    ConfigLoader { c => k =>
      c.getConfigList(k).asScala.toList.map { l.load(_) }
    }

  given [A](using loader: ConfigLoader[A]): ConfigLoader[Option[A]] =
    ConfigLoader[Option[A]](c => k => if c.hasPath(k) then Some(loader.load(c, k)) else None)

  def strLoader[A](f: String => A): ConfigLoader[A]   = ConfigLoader.stringLoader.map(f)
  def intLoader[A](f: Int => A): ConfigLoader[A]      = ConfigLoader.intLoader.map(f)
  def boolLoader[A](f: Boolean => A): ConfigLoader[A] = ConfigLoader.booleanLoader.map(f)

  def strLoader[A](using sr: SameRuntime[String, A]): ConfigLoader[A] =
    ConfigLoader.stringLoader.map(sr.apply)
  def intLoader[A](using sr: SameRuntime[Int, A]): ConfigLoader[A] =
    ConfigLoader.intLoader.map(sr.apply)
  def boolLoader[A](using sr: SameRuntime[Boolean, A]): ConfigLoader[A] =
    ConfigLoader.booleanLoader.map(sr.apply)
