package lila.ask
package ui

import scala.collection.mutable.StringBuilder
import scala.util.Random.shuffle

import lila.ui.{ *, given }
import ScalatagsTemplate.{ *, given }
import lila.core.ask.*

final class AskUi():
  def renderEncodedFrag(fragment: Frag, askFrags: List[Frag]): Frag =
    RawFrag(AskApi.replaceMarkers(fragment.render, askFrags.map(_.render)))

  def render(ask: Option[Ask])(using Context): Frag = ask.fold[Frag](p("<not found>"))(renderContainer)

  def renderHtmlWithAsks(html: Html, asks: Iterable[Ask])(using Context): Frag =
    if asks.isEmpty then html
    else
      RawFrag:
        AskApi.replaceMarkers(
          html.value,
          asks.map: ask =>
            renderContainer(ask).render
        )

  def renderOne(ask: Ask, prevView: Option[Vector[Int]] = None, tallyView: Boolean = false)(using
      Context
  ): Frag =
    AskView(ask, prevView, tallyView).render()

  private def renderContainer(ask: Ask)(using Context): Frag =
    div(cls := s"ask-container${ask.isStretch.so(" stretch")}", renderOne(ask))

  def renderGraph(ask: Ask)(using Context): Frag =
    if ask.isRanked then AskView(ask, None, true).rankGraphBody()
    else AskView(ask, None, true).pollGraphBody()

private case class AskView(ask: Ask, order: Option[Vector[Int]], tally: Boolean)(using ctx: Context):
  val voterId = ctx.me.fold(ask.toAnon(ctx.ip))(me => ask.toAnon(me.userId))
  val tallyView = tally
    && (ask.isTally || ctx.me.exists(_.userId == ask.creator) || Granter.opt(_.ModerateForum))
  val viewOrder = order.getOrElse:
    if ask.isRandom then shuffle(ask.choices.indices.toList)
    else ask.choices.indices.toList
  val choiceContainer =
    div(cls := "ask__choices" + ~(ask.isVertical.option(" vertical")) + ~(ask.isStretch.option(" stretch")))

  def render() =
    fieldset(
      cls := s"ask${ask.isAnon.so(" anon")}",
      id := ask._id,
      attr("data-has-pick") := ask.hasPickFor(voterId)
    )(
      header(),
      ask.isConcluded.option(label(s"${ask.form.so(_ size).max(ask.picks.so(_ size))} responses")),
      ask.choices.nonEmpty.option(
        if ask.isRanked then
          if ask.isConcluded || tallyView then rankGraphBody()
          else rankBody()
        else if ask.isConcluded || tallyView then pollGraphBody()
        else pollBody()
      ),
      footer()
    )

  def header() =
    val viewOrderParam = viewOrder.mkString("-")
    legend(
      span(cls := "ask__header")(
        label(
          ask.question,
          (!tallyView).option(
            if ask.isConcluded then span("(Results)")
            else if ask.isRanked then span("(Drag to sort)")
            else if ask.isMulti then span("(Choose all that apply)")
            else span("(Choose one)")
          )
        ),
        maybeDiv(
          "url-actions",
          ask.isTally.option(
            button(
              cls := (if tallyView then "view" else "tally"),
              formmethod := "GET",
              formaction := routes.Ask.view(ask._id, viewOrderParam.some, !tallyView)
            )
          ),
          (ctx.me.exists(_.userId == ask.creator) || Granter.opt(_.ModerateForum)).option(
            button(
              cls := "admin",
              formmethod := "GET",
              formaction := routes.Ask.admin(ask._id),
              title := "Admin"
            )
          ),
          ((ask.hasPickFor(voterId) || ask.hasFormFor(voterId)) && !ask.isConcluded).option(
            button(
              cls := "unset",
              formaction := routes.Ask.unset(ask._id, viewOrderParam.some, ask.isAnon),
              title := "Unset your submission"
            )
          )
        ),
        maybeDiv(
          "properties",
          ask.isTraceable.option(
            button(cls := "property trace", title := "Participants can see who voted for what")
          ),
          ask.isAnon.option(
            button(cls := "property anon", title := "Your identity is anonymized and secure")
          ),
          ask.isOpen.option(button(cls := "property open", title := "Anyone can participate"))
        )
      )
    )

  def footer() =
    div(cls := "ask__footer")(
      ask.footer.map(label(_)),
      (ask.isForm && !ask.isConcluded && voterId.nonEmpty).option(
        frag(
          form(autocomplete := "off")(
            input(
              cls := "form-text",
              tpe := "text",
              maxlength := 80,
              placeholder := "80 characters max",
              value := ~ask.formFor(voterId)
            )
          ),
          div(cls := "form-submit")(
            input(cls := "button button-blue button-empty", tpe := "button", value := "Submit")
          )
        )
      ),
      (ask.isConcluded && ask.form.exists(_.size > 0)).option(frag:
        ask.form.map: fmap =>
          div(cls := "form-results")(
            ask.footer.map(label(_)),
            fmap.toSeq.flatMap:
              case (user, text) => Seq(div(ask.isTraceable.so(s"$user:")), div(text))
          ))
    )

  def pollBody() = choiceContainer:
    val picks = ask.picksFor(voterId)
    val sb = StringBuilder("choice")
    if ask.isCheckbox then sb ++= " cbx" else sb ++= " btn"
    if ask.isMulti then sb ++= " multiple" else sb ++= " exclusive"
    if ask.isStretch then sb ++= " stretch"
    viewOrder
      .map(choice => ask.choices(choice) -> choice)
      .map:
        case (choiceText, choice) =>
          val selected = picks.exists(_.contains(choice))
          val clz = sb.toString + ~selected.option(" selected")
          if ask.isCheckbox then
            label(cls := clz, title := tooltip(choice), value := choice)(
              input(tpe := "checkbox", selected.option(checked)),
              choiceText
            )
          else button(cls := clz, title := tooltip(choice), value := choice)(choiceText)

  def rankBody() = choiceContainer:
    val hasPick = ask.hasPickFor(voterId)
    val clz = "choice btn rank" + ~ask.isStretch.option(" stretch") + ~hasPick.option(" ranked")
    validRanking().zipWithIndex.map:
      case (choice, index) =>
        div(cls := clz, value := choice, draggable := true)(
          div(if hasPick then s"${index + 1}" else iconEl(Icon.move)),
          label(ask.choices(choice)),
          i()
        )

  def pollGraphBody() =
    val totals = ask.totals
    val max = totals.max
    div(cls := "ask__graph")(totals.zipWithIndex.map:
      case (total, choice) =>
        val pct = if max == 0 then 0 else total * 100 / max
        val hint = tooltip(choice)
        frag(
          div(title := hint)(ask.choices(choice)),
          div(cls := "votes-text", title := hint)(pluralize("vote", total)),
          div(cls := "set-width", title := hint, css("width") := s"$pct%")(nbsp)
        ))

  def rankGraphBody() =
    val tooltipVec = rankedTooltips()
    div(cls := "ask__rank-graph")(
      ask.averageRank.zipWithIndex
        .sortWith((i, j) => i._1 < j._1)
        .map:
          case (avgIndex, choice) =>
            val lastIndex = ask.choices.size - 1
            val pct = (lastIndex - avgIndex) / lastIndex * 100
            val hint = tooltipVec(choice)
            frag(
              div(title := hint)(ask.choices(choice)),
              div(cls := "set-width", title := hint, style := s"width: $pct%")(nbsp)
            )
    )

  def maybeDiv(clz: String, els: Option[Frag]*) =
    if els.toList.flatten.nonEmpty then div(cls := clz, els) else emptyFrag

  def tooltip(choice: Int) =
    val sb = StringBuilder(256)
    val choiceText = ask.choices(choice)
    val count = ask.count(choiceText)
    val isAuthor = ctx.me.exists(_.userId == ask.creator)
    val isMod = Granter.opt(_.ModerateForum)

    if !ask.isRanked then
      if ask.isConcluded || tallyView then
        sb ++= pluralize("vote", count)
        if ask.isTraceable || isMod then sb ++= s"\n\n${whoPicked(choice)}"
      else
        if isAuthor || ask.isTally then sb ++= pluralize("vote", count)
        if ask.isTraceable && ask.isTally || isMod then sb ++= s"\n\n${whoPicked(choice)}"

    if sb.isEmpty then choiceText else sb.toString

  def rankedTooltips() =
    val rankM = ask.rankMatrix
    val notables = List(
      0 -> "ranked this first",
      1 -> "chose this in their top two",
      2 -> "chose this in their top three",
      3 -> "chose this in their top four",
      4 -> "chose this in their top five"
    )
    ask.choices.zipWithIndex.map:
      case (choiceText, choice) =>
        val sb = StringBuilder(s"$choiceText:\n\n")
        notables
          .filter(_._1 < rankM.length - 1)
          .map:
            case (i, text) =>
              sb ++= s"  ${rankM(choice)(i)} $text\n"
        sb.toString

  def pluralize(item: String, n: Int) =
    s"${if n == 0 then "No" else n} ${item}${if n != 1 then "s" else ""}"

  def whoPicked(choice: Int, max: Int = 100) =
    val who = ask.whoPicked(choice)
    if ask.isAnon then s"${who.size} votes"
    else who.take(max).mkString("", ", ", (who.length > max).so(", and others..."))

  def validRanking() =
    ask
      .picksFor(voterId)
      .fold(viewOrder): order =>
        if order == Vector.empty || order.distinct.sorted != viewOrder.sorted then viewOrder
        else order
