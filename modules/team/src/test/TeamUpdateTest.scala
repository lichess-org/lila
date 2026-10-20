package lila.team

import lila.core.data.Markdown

class TeamUpdateTest extends munit.FunSuite:

  test("notification headline keeps hyphens between numbers"):
    assertEquals(
      TeamUpdateApi.markdownToHeadline(Markdown("20-21 И 23-24 октября")),
      "20-21 И 23-24 октября"
    )

  test("notification headline keeps the date ranges from the reported message"):
    assertEquals(
      TeamUpdateApi.markdownToHeadline(
        Markdown("20-21 И 23-24 октября. Обычные даты? (ну, кроме того что каникулы скоро начнутся)")
      ),
      "20-21 И 23-24 октября. Обычные даты? (ну…"
    )

  test("notification headline strips markdown"):
    assertEquals(
      TeamUpdateApi.markdownToHeadline(Markdown("#### Prize Arena at 18:30 Tonight! **Hi** all")),
      "Prize Arena at 18:30 Tonight! Hi all"
    )

  test("notification headline keeps literal asterisks"):
    assertEquals(
      TeamUpdateApi.markdownToHeadline(Markdown("2*3 = 6")),
      "2*3 = 6"
    )
