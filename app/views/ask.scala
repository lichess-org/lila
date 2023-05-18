package views.html

import scala.collection.mutable
import scala.util.Random.shuffle

import controllers.routes
import lila.api.Context
import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.ask.Ask
import lila.ask.AskApi
import lila.security.{ Granter, Permission }

object ask:

  def renderMany(frag: Frag, asks: Iterable[Option[Ask]])(using Context): Frag =
    if asks.isEmpty then frag
    else
      RawFrag:
        AskApi.bake(
          frag.render,
          asks.map:
            case Some(ask) =>
              div(cls := s"ask-container${ask.isStretch ?? " stretch"}", renderOne(ask)).render
            case None =>
              AskApi.askNotFoundFrag
        )

  def renderOne(ask: Ask, prevView: Option[List[Int]] = None, tallyView: Boolean = false)(using
      ctx: Context
  ): Frag =
    RenderAsk(ask, prevView, tallyView, ctx).render

  def renderGraph(ask: Ask)(using ctx: Context): Frag =
    if ask.isRanked then RenderAsk(ask, None, true, ctx).rankGraphBody
    else RenderAsk(ask, None, true, ctx).pollGraphBody

private case class RenderAsk(
    ask: Ask,
    prevView: Option[List[Int]],
    tallyView: Boolean,
    ctx: Context
):
  // this.me is what AskApi cares about. It's either Some(user id), Some(anonymous hash), or None
  val me = ctx.me.fold(ask.toAnon(ctx.ip))(u => ask.toAnon(u.id))

  val view = prevView getOrElse:
    if ask.isRandom then shuffle(ask.choices.indices.toList)
    else ask.choices.indices.toList

  def render =
    fieldset(
      cls                              := s"ask${ask.isAnon ?? " anon"}",
      id                               := ask._id,
      ask.hasPickFor(me) option (value := "")
    )(
      header,
      ask.isConcluded option label(s"${ask.feedback.??(_ size) max ask.picks.??(_ size)} responses"),
      ask.choices.nonEmpty option (
        if ask.isRanked then
          if ask.isConcluded || tallyView then rankGraphBody
          else rankBody
        else if ask.isConcluded || tallyView then pollGraphBody
        else pollBody
      ),
      footer
    )

  def header =
    val viewParam = view.mkString("-")
    legend(
      span(cls := "ask__header")(
        label(
          ask.question,
          !tallyView option (
            if ask.isRanked then span("(Drag to sort)")
            else if ask.isMulti then span("(Choose all that apply)")
            else span("(Choose one)")
          )
        ),
        maybeDiv(
          "url-actions",
          ask.isTally option button(
            cls        := (if tallyView then "view" else "tally"),
            formmethod := "GET",
            formaction := routes.Ask.view(ask._id, viewParam.some, !tallyView)
          ),
          ctx.me.exists(_ is ask.creator) || ctx.me ?? Granter(Permission.Shusher) option button(
            cls        := "admin",
            formmethod := "GET",
            formaction := routes.Ask.admin(ask._id),
            title      := trans.edit.txt()(using ctx.lang)
          ),
          ask.hasPickFor(me) && !ask.isConcluded option button(
            cls        := "unset",
            formaction := routes.Ask.unset(ask._id, viewParam.some, ask.isAnon),
            title      := trans.delete.txt()(using ctx.lang)
          )
        ),
        maybeDiv(
          "properties",
          ask.isTraceable option button(
            cls   := "property trace",
            title := "Participants can see who voted for what"
          ),
          ask.isAnon option button(
            cls   := "property anon",
            title := "Your identity is anonymized and secure"
          ),
          ask.isOpen option button(cls := "property open", title := "Anyone can participate")
        )
      )
    )

  def footer =
    div(cls := "ask__footer")(
      ask.footer map (label(_)),
      ask.isFeedback && !ask.isConcluded && me.nonEmpty option Seq(
        input(
          cls         := "feedback-text",
          tpe         := "text",
          maxlength   := 80,
          placeholder := "80 characters max",
          value       := ~ask.feedbackFor(me)
        ),
        div(cls := "feedback-submit")(input(cls := "button", tpe := "button", value := "Submit"))
      ),
      ask.isConcluded && ask.feedback.exists(_.size > 0) option frag:
        ask.feedback.map: fbmap =>
          div(cls := "feedback-results")(
            ask.footer map (label(_)),
            fbmap.toSeq flatMap:
              case (feedbacker, fb) => Seq(div(ask.isTraceable ?? s"$feedbacker:"), div(fb))
          )
    )

  def pollBody = choiceContainer:
    val picks = ask.picksFor(me)
    val sb    = new mutable.StringBuilder("choice ")
    if ask.isCheckbox then sb ++= "cbx " else sb ++= "btn "
    if ask.isMulti then sb ++= "multiple " else sb ++= "exclusive "
    if ask.isStretch then sb ++= "stretch "
    (view map ask.choices).zipWithIndex map:
      case (choiceText, choice) =>
        val selected = picks.exists(_ contains choice)
        label(
          cls   := sb.toString + (if selected then "selected" else "enabled"),
          role  := "button",
          title := tooltip(choice),
          value := choice
        )(ask.isCheckbox option input(tpe := "checkbox", selected option checked), choiceText)

  def rankBody = choiceContainer:
    validRanking.zipWithIndex map:
      case (choice, index) =>
        val sb = new mutable.StringBuilder("choice btn rank")
        if ask.isStretch then sb ++= " stretch"
        if ask.hasPickFor(me) then sb ++= " submitted"
        label(cls := sb.toString, value := choice, draggable := true)(
          div(s"${index + 1}"),
          label(ask.choices(choice)),
          i
        )

  def pollGraphBody =
    div(cls := "ask__graph")(frag:
      val totals = ask.totals
      val max    = totals.max
      totals.zipWithIndex flatMap:
        case (total, choice) =>
          val pct  = if max == 0 then 0 else total * 100 / max
          val hint = tooltip(choice)
          Seq(
            div(title := hint)(ask.choices(choice)),
            div(cls := "votes-text", title := hint)(pluralize("vote", total)),
            div(cls := "set-width", title := hint, css("width") := s"$pct%")(nbsp)
          )
    )

  def rankGraphBody =
    div(cls := "ask__rank-graph")(frag:
      val tooltipVec = rankedTooltips
      ask.averageRank.zipWithIndex
        .sortWith((i, j) => i._1 < j._1) flatMap:
          case (avgIndex, choice) =>
            val lastIndex = ask.choices.size - 1
            val pct       = (lastIndex - avgIndex) / lastIndex * 100
            val hint      = tooltipVec(choice)
            Seq(
              div(title := hint)(ask.choices(choice)),
              div(cls := "set-width", title := hint, style := s"width: $pct%")(nbsp)
            )
    )

  def maybeDiv(clz: String, tags: Option[Frag]*) =
    if tags.toList.flatten.nonEmpty then div(cls := clz, tags) else emptyFrag

  def choiceContainer =
    val sb = new mutable.StringBuilder("ask__choices")
    if ask.isVertical then sb ++= " vertical"
    if ask.isStretch then sb ++= " stretch"
    // else if ask.isCenter then sb ++= " center" // stretch overrides center
    div(cls := sb.toString)

  def tooltip(choice: Int) =
    val sb         = new mutable.StringBuilder(256)
    val choiceText = ask.choices(choice)
    val hasPick    = ask.hasPickFor(me)

    val count     = ask.count(choiceText)
    val isAuthor  = ctx.me.exists(_.id == ask.creator)
    val isShusher = ctx.me ?? Granter(Permission.Shusher)

    if !ask.isRanked then
      if ask.isConcluded || tallyView then
        sb ++= pluralize("vote", count)
        if ask.isTraceable || isShusher then sb ++= s"\n\n${whoPicked(choice)}"
      else
        if isAuthor || ask.isTally then sb ++= pluralize("vote", count)
        if ask.isTraceable && ask.isTally || isShusher then sb ++= s"\n\n${whoPicked(choice)}"

    if sb.isEmpty then choiceText else sb.toString

  def rankedTooltips =
    val respondents = ask.picks ?? (picks => picks.size)
    val rankM       = ask.rankMatrix
    val notables = List(
      0 -> "ranked this first",
      2 -> "chose this in their top three",
      4 -> "chose this in their top five"
    )
    ask.choices.zipWithIndex map { case (choiceText, choice) =>
      val sb = new mutable.StringBuilder(s"$choiceText:\n\n")
      notables filter (_._1 < rankM.length - 2) map:
        case (i, text) => sb ++= s"  ${rankM(choice)(i)} $text\n"
      sb.toString
    }

  def pluralize(item: String, n: Int) =
    s"${if n == 0 then "No" else n} ${item}${if n != 1 then "s" else ""}"

  def whoPicked(choice: Int, max: Int = 100) =
    val who = ask.whoPicked(choice)
    if ask.isAnon then s"${who.size} votes"
    else who.take(max).mkString("", ", ", (who.length > max) ?? ", and others...")

  def validRanking =
    val initialOrder =
      if ask.isRandom then shuffle((0 until ask.choices.size).toVector)
      else (0 until ask.choices.size).toVector
    ask.picksFor(me).fold(initialOrder) { r =>
      if r == Nil || r.distinct.sorted != initialOrder.sorted then
        // it's late to be doing this but i think it beats counting the choices in an
        // aggregation stage in every db update or storing choices.size in a redundant field
        me ?? (id => env.ask.api.setPicks(ask._id, id, Some(Nil))) // blow away the bad
        initialOrder
      else r
    }
