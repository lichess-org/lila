package old.play

import java.util.concurrent.atomic.{ AtomicReference }
// import java.util.concurrent.{Executors, ThreadFactory}

import akka.actor.{ ActorSystem, Scheduler }
import akka.stream.Materializer
import play.api._
import play.api.ApplicationLoader.Context
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.inject.Injector
import play.api.libs.ws.WSClient
import play.api.Mode

import scala.concurrent.ExecutionContext

/*
 * Band aid for progressive migration to 2.6
 * https://gist.github.com/mathieuancelin/b50342227d0e686397cde6de33d5e356
 */
object Env {

  // I know it's really bad ...
  private[play] val _ref: AtomicReference[Application] = new AtomicReference[Application]()

  lazy val application: Application = Option(_ref.get()).get
  lazy val actorSystem: ActorSystem = application.actorSystem
  lazy val materializer: Materializer = application.materializer
  lazy val configuration: Configuration = application.configuration
  lazy val mode: Mode = application.mode
  lazy val scheduler: Scheduler = actorSystem.scheduler
  lazy val injector: Injector = application.injector
  lazy val playExecutionContext: ExecutionContext = injector.instanceOf(classOf[ExecutionContext])
  lazy val environment: Environment = injector.instanceOf(classOf[Environment])
  lazy val WS: WSClient = injector.instanceOf(classOf[WSClient])
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

trait GoodOldPlay {

  object Implicits {
    implicit def defaultActorSystem: ActorSystem = Env.actorSystem
    implicit def defaultMaterializer: Materializer = Env.materializer
    implicit def defaultScheduler: Scheduler = Env.scheduler
    implicit def defaultContext: ExecutionContext = Env.playExecutionContext
  }

  def WS = Env.WS
  def Configuration = Env.configuration
  def Application = Env.configuration
  def Injector = Env.configuration
  def Mode = Env.configuration

  def currentApplication = Env.application
  def defaultContext = Env.playExecutionContext
  def defaultScheduler = Env.scheduler
  def defaultMaterializer = Env.scheduler
  def defaultActorSystem = Env.configuration
  // def httpRequestsContext = Env.httpRequestExecContext
  // def httpCallsContext = Env.httpCallsExecContext
  // def dataStoreContext = Env.dataStoreExecContext
}

object api {
  object Play {
    def application = Env.application
    def maybeApplication = Option(Env.application)
    def injector = Env.injector
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
          implicit def defaultContext: ExecutionContext = Env.playExecutionContext
        }
        def defaultContext: ExecutionContext = Env.playExecutionContext
        // def httpRequestsContext = Env.httpRequestExecContext
        // def httpCallsContext = Env.httpCallsExecContext
        // def dataStoreContext = Env.dataStoreExecContext
      }
    }
  }
}
