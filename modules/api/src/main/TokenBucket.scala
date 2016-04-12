package lila.api

import java.util.concurrent.TimeUnit

import akka.actor._
import akka.event.LoggingReceive
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.{ ExecutionContext, Future }

/**
 * Token Bucket implementation as described here http://en.wikipedia.org/wiki/Token_bucket
 *
 * Implementation from https://github.com/sief/play-guard/blob/master/module/app/com/sief/ratelimit/TokenBucketGroup.scala
 * Modifications:
 * - No token count, always use 1
 * - Return a consumer, not an actor ref
 * - Removed overflow protection
 */

/**
 * For mocking the current time.
 */
trait Clock {
  def now: Long
}

/**
 * Actor message for consuming tokens
 * @param key bucket key
 */
private case class TokenRequest(key: Any)

/**
 * Actor which synchronizes the bucket token requests
 * @param size bucket size
 * @param rate refill rate in tokens per second
 * @param clock for mocking the current time.
 */
private class TokenBucket(size: Int, rate: Float, clock: Clock) extends Actor {

  private val intervalMillis: Int = (1000 / rate).toInt

  private val ratePerMilli: Double = rate / 1000

  private var lastRefill: Long = clock.now

  private var buckets = Map.empty[Any, Int]

  def receive = {

    /**
     * First refills all buckets at the given rate, then tries to consume 1.
     * If no bucket exists for the given key, a new full one is created.
     */
    case TokenRequest(key) =>
      refillAll()
      val newLevel = buckets.getOrElse(key, size) - 1
      if (newLevel >= 0) buckets = buckets + (key -> newLevel)
      sender ! newLevel
  }

  /**
   * Refills all buckets at the given rate. Full buckets are removed.
   */
  private def refillAll() {
    val now: Long = clock.now
    val diff: Long = now - lastRefill
    val tokensToAdd: Int = (diff * ratePerMilli).toInt
    if (tokensToAdd > 0) {
      buckets = buckets.mapValues(tokensToAdd +).filterNot(_._2 >= size)
      lastRefill = now - diff % intervalMillis
    }
  }
}

object TokenBucket {

  private val defaultTimeout = Timeout(100, TimeUnit.MILLISECONDS)

  final class Consumer(actor: ActorRef) {

    def apply(key: Any)(implicit timeout: Timeout = defaultTimeout): Future[Int] =
      consume(actor, key)
  }

  /**
   * Creates the actor and bucket group.
   * @param system actor system
   * @param size bucket size. Has to be in the range 0 to 1000.
   * @param rate refill rate in tokens per second. Has to be in the range 0.000001f to 1000.
   * @param clock for mocking the current time.
   * @param context akka execution context
   * @return actorRef, needed to call consume later.
   */
  def create(system: ActorSystem, size: Int, rate: Float, clock: Clock = new Clock {
    override def now: Long = System.currentTimeMillis
  })(implicit context: ExecutionContext): Consumer = {
    require(size > 0)
    require(size <= 1000)
    require(rate >= 0.000001f)
    require(rate <= 1000)
    new Consumer(system.actorOf(Props(new TokenBucket(size, rate, clock))))
  }

  /**
   * Try to consume 1 token. If the returned value is negative, no tokens are consumed.
   * @param actor actorRef returned by create
   * @param key bucket key
   * @param timeout akka timeout
   * @return (remainingTokens - 1), if negative no tokens are consumed.
   */
  def consume(actor: ActorRef, key: Any)(implicit timeout: Timeout = defaultTimeout): Future[Int] =
    (actor ? TokenRequest(key)).mapTo[Int]
}
