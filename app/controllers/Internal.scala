package controllers

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Instant
import java.util.HexFormat
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

import play.api.libs.json.*
import play.api.mvc.*

import lila.app.{ *, given }
import lila.identity.{ IdentityEvent, IdentityStatus }

final class Internal(env: Env) extends LilaController(env):

  private val signatureHeader = "X-Identity-Signature"

  def identityEvent = Action.async(parse.tolerantText(4096)) { req =>
    env.identity.webhookSecret match
      case None => fuccess(NotFound)
      case Some(secret) =>
        req.headers.get(signatureHeader) match
          case Some(signature) if verify(secret.value, req.body, signature) =>
            scala.util.Try(Json.parse(req.body).validate[IdentityEvent]).toOption match
              case Some(JsSuccess(event, _)) if event.issuer == env.oidc.api.issuer =>
                env.identity.statusService.apply(event).map(_ => Ok)
              case _ => fuccess(BadRequest)
          case _ => fuccess(Unauthorized)
  }

  def backchannelLogout = Action.async(parse.formUrlEncoded) { req =>
    req.body.get("logout_token").flatMap(_.headOption).filter(_.nonEmpty) match
      case None => fuccess(BadRequest)
      case Some(token) =>
        env.oidc.api
          .validateLogoutToken(token)
          .flatMap { claims =>
            val sid = Option(claims.getSessionID).map(_.getValue)
            val sub = Option(claims.getSubject)
              .map(_.getValue)
              .filter(UserId.regex.matches)
              .map(UserId.apply)
            for
              ids <- sid.fold(fuccess(Nil))(env.security.store.sessionIdsBySid)
              _   <- env.security.store.closeSessions(ids)
              _ = sub.foreach: id =>
                lila.common.Bus.publish(lila.core.security.CloseAccount(id), "accountClose")
                if sid.isEmpty then
                  lila.common.Bus.publish(lila.core.security.RevokeAllCredentials(id), "identityStatus")
              _ <- sub.fold(funit)(id => env.security.api.appeal.closeAllOf(id))
              _ = lila.log("identity").info(s"backchannel logout sid=${sid | "-"} sub=${sub | "-"}")
            yield Ok
          }
          .recover { case _ => BadRequest }
  }

  private def verify(secret: String, body: String, signature: String): Boolean =
    try
      val mac = Mac.getInstance("HmacSHA256")
      mac.init(SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"))
      val expected = mac.doFinal(body.getBytes(StandardCharsets.UTF_8))
      MessageDigest.isEqual(expected, HexFormat.of().parseHex(signature.trim))
    catch case _: Throwable => false

  private given Reads[IdentityEvent] = Reads: json =>
    for
      eventId   <- nonEmpty("eventId")(json)
      issuer    <- nonEmpty("issuer")(json)
      sub       <- nonEmpty("sub")(json)
      statusKey <- (json \ "status").validate[String]
      status <- IdentityStatus
        .fromKey(statusKey)
        .fold[JsResult[IdentityStatus]](JsError(s"unknown status: $statusKey"))(JsSuccess(_))
      version <- (json \ "version")
        .validate[Long]
        .flatMap: v =>
          if v > 0 then JsSuccess(v) else JsError("version must be positive")
      occurredAtRaw <- (json \ "occurredAt").validate[String]
      occurredAt <- scala.util
        .Try(Instant.parse(occurredAtRaw))
        .toOption
        .fold[JsResult[Instant]](JsError("occurredAt must be ISO-8601"))(JsSuccess(_))
      reasonCode = (json \ "reasonCode").asOpt[String]
    yield IdentityEvent(
      eventId = eventId,
      issuer = issuer,
      subject = UserId(sub),
      status = status,
      version = version,
      occurredAt = occurredAt,
      reasonCode = reasonCode
    )

  private def nonEmpty(field: String)(json: JsValue): JsResult[String] =
    (json \ field)
      .validate[String]
      .flatMap: value =>
        if value.nonEmpty then JsSuccess(value) else JsError(s"$field is required")
