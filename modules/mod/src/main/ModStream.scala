package lila.mod

import akka.stream.scaladsl.*
import reactivemongo.akkastream.cursorProducer
import play.api.libs.json.*

import lila.common.Json.given
import lila.common.{ Bus, HTTPRequest }
import lila.core.security.UserSignup
import lila.core.team.TeamCreate
import lila.user.UserRepo
import lila.db.dsl.{ *, given }

final class ModStream(logRepo: ModlogRepo, userRepo: UserRepo)(using akka.stream.Materializer):

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

    private val blueprint =
      Source
        .queue[UserSignup | TeamCreate](32, akka.stream.OverflowStrategy.dropHead)
        .map {
          case UserSignup(user, email, req, fp, suspIp) =>
            Json.obj(
              "t"           -> "signup",
              "username"    -> user.username,
              "email"       -> email.value,
              "ip"          -> HTTPRequest.ipAddress(req).value,
              "suspIp"      -> suspIp,
              "userAgent"   -> HTTPRequest.userAgent(req),
              "fingerPrint" -> fp
            )
          case TeamCreate(team) =>
            Json.obj(
              "t"           -> "teamCreate",
              "teamId"      -> team.id,
              "name"        -> team.name,
              "description" -> team.description,
              "creator"     -> team.userId
            )
        }
        .map: js =>
          s"${Json.stringify(js)}\n"

    def apply()(using Executor): Source[String, ?] =
      blueprint.mapMaterializedValue: queue =>
        val subUser = Bus.sub[UserSignup](queue.offer(_))
        val subTeam = Bus.sub[TeamCreate](queue.offer(_))
        queue
          .watchCompletion()
          .addEffectAnyway {
            Bus.unsub[UserSignup](subUser)
            Bus.unsub[TeamCreate](subTeam)
          }
