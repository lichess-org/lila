"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.patron)window.i18n.patron={};let i=window.i18n.patron;i['actOfCreation']="Da, iată actul de creare (în franceză)";i['amount']="Suma";i['bankTransfers']="Acceptăm și transferuri bancare";i['becomePatron']="Devino un Patron Lichess";i['cancelSupport']="anuleaza-ți susținerea";i['celebratedPatrons']="Patronii renumiți care fac Lichess posibil";i['changeCurrency']="Schimbă moneda";i['changeMonthlyAmount']=s("Modificați suma lunară (%s)");i['changeMonthlySupport']="Pot schimba/anula susținerea mea lunară?";i['changeOrContact']=s("Da, oricând, de pe această pagină.\nSau poți %s.");i['checkOutProfile']="Verifică-ți pagina de profil!";i['contactSupport']="cere asistență de la Lichess";i['costBreakdown']="Vezi analiza detaliată a costurilor";i['currentStatus']="Status actual";i['date']="Dată";i['decideHowMuch']="Decide cât valorează Lichess pentru tine:";i['donate']="Donează";i['donateAsX']=s("Donează ca %s");i['downgradeNextMonth']="Într-o lună, NU vei fi taxat din nou, iar contul tău de Lichess va fi înapoi un cont normal.";i['featuresComparison']="Vezi compararea detaliată a caracteristicilor";i['freeAccount']="Cont gratuit";i['freeChess']="Șah gratuit pentru toată lumea, pentru totdeauna!";i['giftPatronWings']="Dăruiește aripi de Patron unui jucător";i['giftPatronWingsShort']="Dăruiește aripi Patron";i['ifNotRenewedThenAccountWillRevert']="Dacă nu se reînnoiește, contul tău va deveni din nou un cont obișnuit.";i['lichessIsRegisteredWith']=s("Lichess este înregistrat cu %s.");i['lichessPatron']="Patron Lichess";i['lifetime']="Pe viață";i['lifetimePatron']="Patron Lichess pe viață";i['logInToDonate']="Autentifică-te pentru a dona";i['makeAdditionalDonation']="Faceți o donație în plus acum";i['monthly']="Lunar";i['newPatrons']="Patroni noi";i['nextPayment']="Următoarea plată";i['noAdsNoSubs']="Fără reclame, fără abonamente; dar open-source și pasiune.";i['noLongerSupport']="Nu mai susține Lichess";i['noPatronFeatures']="Nu, deoarece Lichess e complet gratuit, pentru totdeauna, și pentru toată lumea. Asta e o promisiune.\nTotuși, Patronii primesc o nouă pictogramă faină lângă profil.";i['nowLifetime']="Acum ești Patron Lichess pe viață!";i['nowOneMonth']="Acum ești Patron Lichess pentru o lună!";i['officialNonProfit']="Este Lichess un non-profit în mod oficial?";i['onetime']="O dată";i['onlyDonationFromAbove']="Te rugăm să iei aminte că doar formularul de donație de mai sus îți va acorda statutul de Patron.";i['otherAmount']="Altă sumă";i['otherMethods']="Alte metode de donare?";i['patronFeatures']="Sunt unele funcții rezervate Patronilor?";i['patronForMonths']=p({"one":"Patron Lichess timp de o lună","few":"Patron Lichess timp de %s luni","other":"Patron Lichess timp de %s luni"});i['patronUntil']=s("Ai un cont de Patron până pe %s.");i['payLifetimeOnce']=s("Plătește %s o dată. Fii un Patron Lichess pentru totdeauna!");i['paymentDetails']="Detalii de plată";i['permanentPatron']="Acum ai un cont permanent de Patron.";i['pleaseEnterAmountInX']=s("Te rugăm să introduci o sumă în %s");i['recurringBilling']="Plată recurentă, reînnoindu-ți aripile de Patron în fiecare lună.";i['serversAndDeveloper']=s("În primul rând, la servere puternice.\nApoi plătim un dezvoltator cu normă întreagă: %s, fondatorul Lichess.");i['singleDonation']="O singură donație care îți oferă aripile de Patron pentru o lună.";i['stopPayments']="Retrage-ți cardul de credit și oprește plățile:";i['stopPaymentsPayPal']="Anulează abonamentul PayPal și oprește plățile:";i['stripeManageSub']="Gestionează-ți abonamentul și descarcă facturile și bonurile tale";i['thankYou']="Îți mulțumim pentru donația ta!";i['transactionCompleted']="Tranzacția a fost completată, și o chitanță pentru donația ta ți-a fost trimisă pe email.";i['tyvm']="Mulțumim foarte mult pentru ajutor. Ești tare!";i['update']="Actualizare";i['updatePaymentMethod']="Actualizați metoda de plată";i['viewOthers']="Vezi alți Patroni Lichess";i['weAreNonProfit']="Suntem o asociație non-profit deoarece credem că toată lumea ar trebui să aibă acces la o platformă de șah gratuită, de talie mondială.";i['weAreSmallTeam']="Suntem o echipă mică, deci susținerea ta face o diferență uriașă!";i['weRelyOnSupport']="Ne bazăm pe sprijinul oamenilor ca tine pentru a face acest lucru posibil. Dacă îți place să folosești Lichess, te rugăm să ne susții donând și devenind un Patron!";i['whereMoneyGoes']="Unde se duc banii?";i['withCreditCard']="Card de credit";i['xBecamePatron']=s("%s a devenit un Patron Lichess");i['xIsPatronForNbMonths']=p({"one":"%1$s este un Patron Lichess de %2$s lună","few":"%1$s este un Patron Lichess timp de %2$s luni","other":"%1$s este un Patron Lichess de %2$s luni"});i['xOrY']=s("%1$s sau %2$s");i['youHaveLifetime']="Ai un cont de Patron pe viață. Asta e minunat!";i['youSupportWith']=s("Susțineți lichess.org cu %s pe lună.");i['youWillBeChargedXOnY']=s("Veți plăti %1$s pe %2$s.")})()