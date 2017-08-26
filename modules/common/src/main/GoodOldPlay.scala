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

  private lazy val components: LilaComponents = Option(_ref.get()).get

  lazy val application: Application = components.application
  lazy val lifecycle: ApplicationLifecycle = components.applicationLifecycle

  lazy val actorSystem: ActorSystem = application.actorSystem
  lazy val materializer: Materializer = application.materializer
  lazy val configuration: Configuration = application.configuration
  lazy val mode: Mode = application.mode
  lazy val scheduler: Scheduler = actorSystem.scheduler
  lazy val defaultContext: ExecutionContext = components.executionContext
  lazy val environment: Environment = components.environment

  lazy val WS: WSClient = {
    import play.api.libs.ws.ahc.{ AsyncHttpClientProvider, AhcWSClientProvider }
    implicit val mat = materializer
    implicit val ec = defaultContext
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
  // lazy val cache: CacheApi = injector.instanceOf(classOf[CacheApi])
  // lazy val procNbr = Runtime.getRuntime.availableProcessors()

  // private def factory(of: String) = new ThreadFactory {
  //   val counter = new AtomicInteger(0)
  //   override def newThread(r: Runnable): Thread = new Thread(r, s"$of-${counter.incrementAndGet()}")
  // }

  // lazy val httpRequestExecContext = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(procNbr * 2, factory("http-requests")))
  // lazy val httpCallsExecContext = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(procNbr * 10, factory("http-calls")))
  // lazy val dataStoreExecContext = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(procNbr * 5, factory("data-store")))

}

object api {
  object Play {
    def application = Env.application
    def maybeApplication = Option(Env.application)
    def classloader = Env.application.classloader
    def configuration = Env.configuration
    def current = Env.application
    def isDev = Env.mode == Mode.Dev
    def isProd = Env.mode == Mode.Prod
    def isTest = Env.mode == Mode.Test
    def mode = Env.mode
    // def getFile(relativePath: String) = Env.application.getFile(relativePath)
    // def getExistingFile(relativePath: String) = Env.application.getExistingFile(relativePath)
    // def resource(name: String) = Env.application.resource(name)
    // def resourceAsStream(name: String) = Env.application.resourceAsStream(name)
  }
  object libs {
    object ws {
      def WS = Env.WS
    }
    object concurrent {
      object Akka {
        object Implicits {
          implicit def defaultActorSystem: ActorSystem = Env.actorSystem
          implicit def defaultMaterializer: Materializer = Env.materializer
          implicit def defaultScheduler: Scheduler = Env.scheduler
        }
        def defaultScheduler: Scheduler = Env.scheduler
        def defaultActorSystem: ActorSystem = Env.actorSystem
        def defaultMaterializer: Materializer = Env.materializer
      }
      object Execution {
        object Implicits {
          implicit def defaultContext: ExecutionContext = Env.defaultContext
        }
        def defaultContext: ExecutionContext = Env.defaultContext
        // def httpRequestsContext = Env.httpRequestExecContext
        // def httpCallsContext = Env.httpCallsExecContext
        // def dataStoreContext = Env.dataStoreExecContext
      }
    }
  }
}
