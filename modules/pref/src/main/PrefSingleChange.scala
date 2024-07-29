package lila.pref

import monocle.syntax.all.*
import play.api.data.*
import play.api.data.Forms.*

object PrefSingleChange:

  case class Change[A](field: String, mapping: Mapping[A], update: A => Pref => Pref):
    def form: Form[A] = Form(single(field -> mapping))

  private def changing[A](field: PrefForm.fields.type => (String, Mapping[A]))(
      f: A => Pref => Pref
  ): Change[A] =
    Change(field(PrefForm.fields)._1, field(PrefForm.fields)._2, f)

  val changes: Map[String, Change[?]] = List[Change[?]](
    changing(_.bg): v =>
      Pref.Bg.fromString.get(v).fold[Pref => Pref](identity)(bg => _.copy(bg = bg)),
    changing(_.bgImg): v =>
      _.copy(bgImg = v.some.filterNot(_.isBlank)),
    changing(_.theme): v =>
      _.copy(theme = v),
    changing(_.pieceSet): v =>
      _.copy(pieceSet = v),
    changing(_.theme3d): v =>
      _.copy(theme3d = v),
    changing(_.pieceSet3d): v =>
      _.copy(pieceSet3d = v),
    changing(_.is3d): v =>
      _.copy(is3d = v),
    changing(_.soundSet): v =>
      _.copy(soundSet = v),
    changing(_.zen): v =>
      _.copy(zen = v),
    changing(_.voice): v =>
      _.copy(voice = v.some),
    changing(_.keyboardMove): v =>
      _.copy(keyboardMove = v | Pref.KeyboardMove.NO),
    changing(_.autoQueen): v =>
      _.copy(autoQueen = v),
    changing(_.premove): v =>
      _.copy(premove = v == 1),
    changing(_.takeback): v =>
      _.copy(takeback = v),
    changing(_.autoThreefold): v =>
      _.copy(autoThreefold = v),
    changing(_.submitMove): v =>
      _.copy(submitMove = v),
    changing(_.confirmResign): v =>
      _.copy(confirmResign = v),
    changing(_.moretime): v =>
      _.copy(moretime = v),
    changing(_.clockSound): v =>
      _.copy(clockSound = v == 1),
    changing(_.pieceNotation): v =>
      _.copy(pieceNotation = v),
    changing(_.ratings): v =>
      _.copy(ratings = v),
    changing(_.follow): v =>
      _.copy(follow = v == 1),
    changing(_.board.brightness): v =>
      _.focus(_.board.brightness).replace(v),
    changing(_.board.opacity): v =>
      _.focus(_.board.opacity).replace(v),
    changing(_.board.hue): v =>
      _.focus(_.board.hue).replace(v)
  ).map: change =>
    change.field -> change
  .toMap
