package lila.web

import play.api.mvc.RequestHeader

// https://developers.cloudflare.com/ruleset-engine/rules-language/fields/reference/cf.appsec.request.failed_detections/
object CfAppSec:

  case class ReqDetection(
      score: Option[Int],
      contentScan: Option[String],
      signature: Option[String],
      credential: Option[String]
  )

  def credentialPairLeaked(using req: RequestHeader): Boolean = parse.exists(_.credential.isDefined)

  def parse(using req: RequestHeader): Option[ReqDetection] =
    req.headers
      .get("x-cfsec")
      .map:
        header =>
          println(header)
          ReqDetection(none, none, none, none)
          // header.split(",").toList.map { detection =>
          //   val parts = detection.split(";").map(_.trim)
          //   val score = parts
          //     .find(_.startsWith("score="))
          //     .flatMap(_.split("=").lastOption)
          //     .flatMap(s => scala.util.Try(s.toInt).toOption)
          //   val contentScan = parts.find(_.startsWith("content_scan=")).flatMap(_.split("=").lastOption)
          //   val signature = parts.find(_.startsWith("signature=")).flatMap(_.split("=").lastOption)
          //   val credential = parts.find(_.startsWith("credential=")).flatMap(_.split("=").lastOption)
          //   ReqDetection(score, contentScan, signature, credential)
          // }
