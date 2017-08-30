package old.play

import java.util.concurrent.atomic.{ AtomicReference }
// import java.util.concurrent.{Executors, ThreadFactory}

import lila.common.LilaComponents

import akka.actor.{ ActorSystem, Scheduler }
import akka.stream.Materializer
import play.api._
import play.api.ApplicationLoader.Context
import play.api.inject.ApplicationLifecycle
import play.api.libs.ws.{ WSClient, StandaloneWSClient }
import play.api.Mode

import scala.concurrent.ExecutionContext

/*
 * Band aid for progressive migration to 2.6
 * https://gist.github.com/mathieuancelin/b50342227d0e686397cde6de33d5e356
 */
object Env {

  private val _ref: AtomicReference[LilaComponents] = new AtomicReference[LilaComponents]()

  def start(components: LilaComponents) = _ref.set(components)

  lazy val components: LilaComponents = Option(_ref.get()).get

  lazy val application: Application = components.application
  lazy val lifecycle: ApplicationLifecycle = components.applicationLifecycle
  implicit lazy val defaultContext: ExecutionContext = components.executionContext

  lazy val actorSystem: ActorSystem = application.actorSystem
  lazy val materializer: Materializer = application.materializer
  lazy val configuration: Configuration = application.configuration
  lazy val mode: Mode = application.mode
  lazy val scheduler: Scheduler = actorSystem.scheduler
  lazy val environment: Environment = components.environment

  lazy val WS: WSClient = {
    import play.api.libs.ws.ahc.{ AsyncHttpClientProvider, AhcWSClientProvider }
    implicit val mat = materializer
    val asyncHttpClient = new AsyncHttpClientProvider(environment, configuration, lifecycle).get
    new AhcWSClientProvider(asyncHttpClient).get
  }
  import play.api.http.HttpConfiguration
  import play.api.mvc.{ SessionCookieBaker, DefaultSessionCookieBaker }
  import play.api.libs.crypto.{ CookieSigner, DefaultCookieSigner }
  lazy val httpConf = HttpConfiguration.fromConfiguration(configuration, environment)
  lazy val cookieSigner: CookieSigner = new DefaultCookieSigner(httpConf.secret)
  lazy val cookieBaker: SessionCookieBaker = {
    new DefaultSessionCookieBaker(httpConf.session, httpConf.secret, cookieSigner)
  }

  import play.api.mvc.{ ActionBuilder, Request, AnyContent }
  lazy val actionBuilder: ActionBuilder[Request, AnyContent] = components.controllerComponents.actionBuilder

  lazy val standaloneWSClient: StandaloneWSClient = new StandaloneWSClient {
    def underlying[T] = WS.asInstanceOf[T]
    def url(url: String) = WS.url(url)
    def close() = {}
  }
}
