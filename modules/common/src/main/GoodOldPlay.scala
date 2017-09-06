package old.play

import java.util.concurrent.atomic.{ AtomicReference }

import akka.actor.{ ActorSystem, Scheduler }
import akka.stream.Materializer
import play.api._
import play.api.mvc.ControllerComponents
import play.api.inject.ApplicationLifecycle
import play.api.libs.ws.{ WSClient, StandaloneWSClient }
import play.api.Mode

import scala.concurrent.ExecutionContext

/*
 * Band aid for progressive migration to 2.6
 * https://gist.github.com/mathieuancelin/b50342227d0e686397cde6de33d5e356
 */
object Env {

  case class Deps(
      application: Application,
      lifecycle: ApplicationLifecycle,
      context: ExecutionContext,
      environment: Environment,
      controllerComponents: ControllerComponents
  )

  private val _ref: AtomicReference[Deps] = new AtomicReference[Deps]()

  def start(deps: Deps) = _ref.set(deps)

  lazy val deps: Deps = Option(_ref.get()).get

  lazy val application: Application = deps.application
  lazy val lifecycle: ApplicationLifecycle = deps.lifecycle
  implicit lazy val defaultContext: ExecutionContext = deps.context

  lazy val actorSystem: ActorSystem = application.actorSystem
  lazy val materializer: Materializer = application.materializer
  lazy val configuration: Configuration = application.configuration
  lazy val mode: Mode = application.mode
  lazy val scheduler: Scheduler = actorSystem.scheduler
  lazy val environment: Environment = deps.environment
  lazy val controllerComponents = deps.controllerComponents

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
  lazy val actionBuilder: ActionBuilder[Request, AnyContent] = controllerComponents.actionBuilder

  lazy val standaloneWSClient: StandaloneWSClient = new StandaloneWSClient {
    def underlying[T] = WS.asInstanceOf[T]
    def url(url: String) = WS.url(url)
    def close() = {}
  }
}
