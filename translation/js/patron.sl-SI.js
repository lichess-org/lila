"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.patron)window.i18n.patron={};let i=window.i18n.patron;i['actOfCreation']="Da, tu je uradna objava o ustanovitvi (v francoščini)";i['amount']="Znesek";i['bankTransfers']="Sprejemamo tudi bančne transferje";i['becomePatron']="Postani Lichess pokrovitelj";i['cancelSupport']="Prekličite svojo podporo";i['celebratedPatrons']="Slavni pokrovitelji, ki so omogočili Lichess";i['changeCurrency']="Spremenite valuto";i['changeMonthlyAmount']=s("Spremeni mesečni znesek (%s)");i['changeMonthlySupport']="Ali lahko spremenim/prekličem svojo mesečno podporo?";i['changeOrContact']=s("Da, kadarkoli, na tej strani.\nAli pa %s.");i['checkOutProfile']="Preglejte svojo stran s profilom!";i['contactSupport']="kontaktirajte Lichess podporo";i['costBreakdown']="Oglejte si podrobno razčlenitev stroškov";i['currentStatus']="Trenutno stanje";i['date']="Datum";i['decideHowMuch']="Odločite se, koliko vam je Lichess vreden:";i['donate']="Donirajte";i['donateAsX']=s("Donirajte kot %s");i['downgradeNextMonth']="V enem mesecu vam NE bomo spet zaračunali in vaš Lichess račun se bo vrnil na običajni račun.";i['featuresComparison']="Poglejte podrobno primerjavo lastnosti";i['freeAccount']="Prost račun";i['freeChess']="Zastonj šah za vse, za vedno!";i['giftPatronWings']="Podari donatorska krila igralcu";i['giftPatronWingsShort']="Daruj donatorska krila";i['ifNotRenewedThenAccountWillRevert']="Če ga ne obnovite, se bo vaš račun vrnil na običajni račun.";i['lichessIsRegisteredWith']=s("Lichess je registriran s %s.");i['lichessPatron']="Lichess pokrovitelj";i['lifetime']="Doživljenjsko";i['lifetimePatron']="Doživljenski Lichess pokrovitelj";i['logInToDonate']="Za donacije se prijavite";i['makeAdditionalDonation']="Opravite dodatno donacijo";i['monthly']="Mesečno";i['newPatrons']="Novi pokrovitelji";i['nextPayment']="Naslednje plačilo";i['noAdsNoSubs']="Brez oglasov, brez naročnin; toda odprtokoden in strasten.";i['noLongerSupport']="Ni več podpore Lichess";i['noPatronFeatures']="Ne, ker je Lichess popolnoma zastonj, za vedno in za vsakogar. To je obljuba. Kakorkoli, pokrovitelji dobijo pravice hvalisanja s kul novo ikono profila.";i['nowLifetime']="Zdaj ste doživljenski Lichess pokrovitelj!";i['nowOneMonth']="Zdaj ste Lichess pokrovitelj za en mesec!";i['officialNonProfit']="Ali je Lichess uradno neprofiten?";i['onetime']="Enkratno";i['onlyDonationFromAbove']="Upoštevajte, da bo status pokrovitelja podeljen le z zgoraj navedenim obrazcem za donacijo.";i['otherAmount']="Drugo";i['otherMethods']="Druge metode donacije?";i['patronFeatures']="Ali so katere možnosti namenjene samo pokroviteljem?";i['patronForMonths']=p({"one":"Lichess Pokrovitelj za en mesec","two":"Lichess pokrovitelj za dva meseca","few":"Lichess pokrovitelj za %s mesece","other":"Lichess pokrovitelj za %s mesecev"});i['patronUntil']=s("Imate pokroviteljski račun od %s.");i['payLifetimeOnce']=s("Plačajte %s enkrat Bodite Lichess pokrovitelj za vedno!");i['paymentDetails']="Podrobnosti o plačilu";i['permanentPatron']="Zdaj imate stalen račun pokrovitelja.";i['pleaseEnterAmountInX']=s("Prosimo, vnesite znesek v %s");i['recurringBilling']="Ponavljajoči se računi, vsak mesec obnavljate svojo ikono Pokrovitelja.";i['serversAndDeveloper']=s("Najprej za zmogljive strežnike.\nNato za plačilo polno zaposlenega razvijalca: %s, ustanovitelja Lichessa.");i['singleDonation']="Enkratna donacija, ki vam podeli pokroviteljsko ikono za en mesec.";i['stopPayments']="Umaknite kreditno kartico in ustavite plačila:";i['stopPaymentsPayPal']="Prekličite naročnino na PayPal in ustavite plačila:";i['stripeManageSub']="Upravljajte svojo naročnino in prenesite svoje račune in potrdila";i['thankYou']="Hvala za vašo donacijo!";i['transactionCompleted']="Vaša transakcija je zaključena. Potrdilo o donaciji smo vam poslali po elektronski pošti.";i['tyvm']="Najlepša hvala za tvojo pomoč. Zažigaš!";i['update']="Posodobitev";i['updatePaymentMethod']="Način plačila posodobljen";i['viewOthers']="Ogled drugih Lichess pokroviteljev";i['weAreNonProfit']="Smo neprofitno združenje, saj verjamemo, da bi morali vsi imeti dostop do brezplačne šahovske platforme svetovnega razreda.";i['weAreSmallTeam']="Smo majhna ekipa, zato vaša podpora veliko pomeni!";i['weRelyOnSupport']="Da to omogočimo, se zanašamo na vašo podporo. Če radi uporabljate Lichess, vas prosimo, da nas podprete z donacijami in postanete pokrovitelj!";i['whereMoneyGoes']="Kam gre denar?";i['withCreditCard']="Kreditna kartica";i['xBecamePatron']=s("%s je postal Lichess pokrovitelj");i['xIsPatronForNbMonths']=p({"one":"%1$s je pokrovitelj lichess za %2$s mesec","two":"%1$s je pokrovitelj lichess za %2$s meseca","few":"%1$s je pokrovitelj lichess za %2$s mesece","other":"%1$s je pokrovitelj lichess za %2$s mesecev"});i['xOrY']=s("%1$s ali %2$s");i['youHaveLifetime']="Imate vseživljenski pokroviteljski račun. To je super!";i['youSupportWith']=s("Vaša podpora lichess.org z/s %s na mesec.");i['youWillBeChargedXOnY']=s("Dne %2$s vam bomo zaračunali %1$s.")})()