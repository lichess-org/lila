package lila.report

import scala.concurrent.Future

import lila.core.LightUser
import lila.core.config.NetDomain
import lila.core.userId.{ MyId, UserName }

class ReportFormTest extends munit.FunSuite:

  given NetDomain = NetDomain("lichess.org")

  private val users = Map(
    "suspect" -> LightUser.fallback(UserName("Suspect")),
    "reporter" -> LightUser.fallback(UserName("Reporter"))
  )

  private val reporter = MyId("reporter")

  private val form =
    new ReportForm(lightUserAsync = LightUser.Getter(id => Future.successful(users.get(id.value))))

  private def bind(reason: String, text: String, username: String = "suspect") =
    form.create(using reporter).bind(Map("username" -> username, "reason" -> reason, "text" -> text))

  test("cheat report requires a game link"):
    val f = bind("cheat", "they cheat")
    assertEquals(f.globalErrors.map(_.message), List("error.provideOneCheatedGameLink"))

  test("stall report requires a game link"):
    val f = bind("stall", "they left the game")
    assertEquals(f.globalErrors.map(_.message), List("error.provideOneCheatedGameLink"))

  test("cheat report with a game link is valid"):
    val f = bind("cheat", "https://lichess.org/abcdefgh they cheat")
    assert(f.errors.isEmpty)
    assertEquals(f.value.map(_.reason), Some("cheat"))

  test("non-play reasons do not require a game link"):
    val f = bind("verbalabuse", "they insulted me")
    assert(f.errors.isEmpty)
    assertEquals(f.value.map(_.reason), Some("verbalabuse"))

  test("report text is capped at 3000 characters"):
    val f = bind("verbalabuse", "a" * 3001)
    assertEquals(f.globalErrors.map(_.message), List("Maximum report length is 3000 characters"))

  test("unknown reason is rejected"):
    val f = bind("notareason", "hello there")
    assertEquals(f.error("reason").map(_.message), Some("error.required"))

  test("unknown username is rejected"):
    val f = bind("verbalabuse", "hello there", username = "nobody")
    assertEquals(f.error("username").map(_.message), Some("Unknown username"))

  test("self-report is rejected"):
    val f = bind("verbalabuse", "hello there", username = "reporter")
    assertEquals(f.error("username").map(_.message), Some("You cannot report yourself"))

  test("the playbans reason cannot be filed by hand"):
    val f = bind("playbans", "this user is banned")
    assertEquals(f.error("reason").map(_.message), Some("error.required"))

  test("the altprint reason cannot be filed by hand"):
    val f = bind("altprint", "this user is a print")
    assertEquals(f.error("reason").map(_.message), Some("error.required"))

  test("every user selectable reason is accepted"):
    Reason.userSelectable.foreach: reason =>
      val f = bind(reason.key, "https://lichess.org/abcdefgh a valid explanation")
      assert(f.errors.isEmpty, s"${reason.key} should be accepted")
