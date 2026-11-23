package lila.i18n

import play.api.i18n.Lang

import lila.core.net.CountryCode

class LangPickerTest extends munit.FunSuite:

  test("firefox anon language picker"):
    val fromReq = List(Lang.get("en"), Lang.get("en-US"), Lang.get("fr")).flatten // en,en-US;q=0.9,fr;q=0.8
    val choices = List(Lang.get("en-US"), Lang.get("fr-FR")).flatten
    assertEquals(LangPicker.allFromRequestHeaders(fromReq, none), choices)

  test("firefox anon language picker, fr-FR"):
    val fromReq =
      List(Lang.get("en"), Lang.get("en-US"), Lang.get("fr-FR")).flatten // en,en-US;q=0.9,fr-FR;q=0.8
    val choices = List(Lang.get("en-US"), Lang.get("fr-FR"), Lang.get("br-FR"), Lang.get("co-FR")).flatten
    assertEquals(LangPicker.allFromRequestHeaders(fromReq, none), choices)

  test("firefox anon language picker, french IP"):
    val fromReq = List(Lang.get("en"), Lang.get("en-US"), Lang.get("fr")).flatten // en,en-US;q=0.9,fr;q=0.8
    val choices = List(Lang.get("en-US"), Lang.get("fr-FR"), Lang.get("br-FR"), Lang.get("co-FR")).flatten
    assertEquals(LangPicker.allFromRequestHeaders(fromReq, CountryCode("FR").some), choices)

  test("safari anon language picker"):
    val fromReq = List(Lang.get("en-GB"), Lang.get("en")).flatten // en-GB,en;q=0.9
    val choices = List(Lang.get("en-GB"), Lang.get("en-US"), Lang.get("cy-GB"), Lang.get("gd-GB")).flatten
    assertEquals(LangPicker.allFromRequestHeaders(fromReq, none), choices)
