package lila.mod

import akka.stream.scaladsl.*
import reactivemongo.akkastream.cursorProducer
import play.api.libs.json.*

import lila.common.Json.given
import lila.common.{ Bus, HTTPRequest }
import lila.core.security.UserSignup
import lila.user.UserRepo
import lila.db.dsl.{ *, given }

final class ModStream(logRepo: ModlogRepo, userRepo: UserRepo)(using Executor, akka.stream.Materializer):

  def markedSince(since: Instant): Source[UserId, ?] =
    logRepo.coll
      .find(
        $doc("action".$in(List("engine", "booster", "cheatDetected")), "date" -> $doc("$gt" -> since)),
        $doc("user" -> true).some
      )
      .sort($doc("date" -> 1))
      .batchSize(100)
      .cursor[Bdoc]()
      .documentSource()
      .grouped(100)
      .throttle(10, 1.second)
      .map(_.flatMap(_.getAsOpt[UserId]("user")))
      .mapAsync(1)(userRepo.filterLame)
      .mapConcat(_.toList)

  object events:

    private val classifier = "userSignup"

    private val blueprint =
      Source
        .queue[UserSignup](32, akka.stream.OverflowStrategy.dropHead)
        .map { case UserSignup(user, email, req, fp, suspIp) =>
          Json.obj(
            "t"           -> "signup",
            "username"    -> user.username,
            "email"       -> email.value,
            "ip"          -> HTTPRequest.ipAddress(req).value,
            "suspIp"      -> suspIp,
            "userAgent"   -> HTTPRequest.userAgent(req),
            "fingerPrint" -> fp
          )
        }
        .map: js =>
          s"${Json.stringify(js)}\n"

    def apply()(using Executor): Source[String, ?] =
      blueprint.mapMaterializedValue: queue =>
        val sub = Bus.subscribeFun(classifier):
          case signup: UserSignup => queue.offer(signup)
        queue
          .watchCompletion()
          .addEffectAnyway:
            Bus.unsubscribe(sub, classifier)
