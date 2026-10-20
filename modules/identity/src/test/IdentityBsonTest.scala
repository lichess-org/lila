package lila.identity

import java.time.Instant

import reactivemongo.api.bson.*

import lila.core.userId.UserId

class IdentityBsonTest extends munit.FunSuite:

  import BsonHandlers.given

  private val at = Instant.parse("2026-10-20T06:00:00Z")

  test("status record round-trips with _id and status key") {
    val record = IdentityStatusRecord(
      id = UserId("1001"),
      globallyEnabled = false,
      status = IdentityStatus.Banned,
      version = 3L,
      occurredAt = at,
      reasonCode = Some("abuse"),
      updatedAt = at
    )
    val doc = BSON.writeDocument(record).get
    assertEquals(doc.getAsOpt[String]("_id"), Some("1001"))
    assertEquals(doc.getAsOpt[String]("status"), Some("banned"))
    assertEquals(BSON.readDocument[IdentityStatusRecord](doc).toOption, Some(record))
  }

  test("event round-trips with _id and status") {
    val event = IdentityEvent(
      eventId = "evt-1",
      issuer = "https://passport.example",
      subject = UserId("1001"),
      status = IdentityStatus.Deleted,
      version = 4L,
      occurredAt = at,
      reasonCode = None
    )
    val doc = BSON.writeDocument(event).get
    assertEquals(doc.getAsOpt[String]("_id"), Some("evt-1"))
    assertEquals(doc.getAsOpt[String]("status"), Some("deleted"))
    assertEquals(BSON.readDocument[IdentityEvent](doc).toOption, Some(event))
  }

  test("legacy alias round-trips") {
    import IdentityAlias.given
    val alias = IdentityAlias(subject = "1001", userId = UserId("legacy-user"))
    val doc   = BSON.writeDocument(alias).get
    assertEquals(doc.getAsOpt[String]("_id"), Some("1001"))
    assertEquals(BSON.readDocument[IdentityAlias](doc).toOption, Some(alias))
  }

  test("status keys are parsed strictly") {
    assertEquals(IdentityStatus.fromKey("ACTIVE"), Some(IdentityStatus.Active))
    assertEquals(IdentityStatus.fromKey("banned"), Some(IdentityStatus.Banned))
    assertEquals(IdentityStatus.fromKey("nope"), None)
  }
