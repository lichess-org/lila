"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.patron)window.i18n.patron={};let i=window.i18n.patron;i['actOfCreation']="Да, ево и решења о регистрацији (на француском)";i['amount']="Износ";i['bankTransfers']="Такође прихватамо банковне трансфере";i['becomePatron']="Постани Личес Патрон";i['cancelSupport']="откажи твоју пордшку";i['changeMonthlyAmount']=s("Промени месечну вредност (%s)");i['changeMonthlySupport']="Могу ли изменити/укинти моју месечну донацију?";i['changeOrContact']=s("Да, у било ком тренутку, на овој страници.\nИли можеш %s.");i['contactSupport']="контактирати Личес подршку";i['costBreakdown']="Погледај детаљну поделу цена";i['currentStatus']="Тренутни статус";i['date']="Датум";i['decideHowMuch']="Одлучи колико Личес теби вреди:";i['donate']="Донирај";i['downgradeNextMonth']="За месец дана, нећете бити поново наплаћени, а ваш Личес налог ће се вратити на обичан налог.";i['featuresComparison']="Погледај детаљно поређење могућности";i['freeAccount']="Бесплатан налог";i['freeChess']="Бесплатан шах за све, заувек!";i['lichessPatron']="Личес Патрон";i['lifetime']="Доживотно";i['lifetimePatron']="Доживотни Личес Патрон";i['makeAdditionalDonation']="Направи додатну донацију сада";i['monthly']="Месечно";i['newPatrons']="Нови Патрони";i['nextPayment']="Следећа уплата";i['noPatronFeatures']="Не, јер је Lichess потпуно бесплатан, заувек и за све. То је обећање.\nИпак, Патрони добијају права да се хвале својом новом кул иконом на профилу.";i['nowLifetime']="Сада си доживотни Личес Патрон!";i['nowOneMonth']="Сада си Личес Патрон један месец!";i['officialNonProfit']="Да ли је Личес званично непрофитно удружење?";i['onetime']="Једноктратно";i['otherAmount']="Остало";i['otherMethods']="Остали методи донација?";i['patronFeatures']="Да ли су неке могућности дозвољене само Патронима?";i['patronForMonths']=p({"one":"Личес Патрон један месец","few":"Личес Патрон %s месеца","other":"Личес Патрон %s месеци"});i['patronUntil']=s("Имаш Патрон налог до %s.");i['payLifetimeOnce']=s("Плати %s једном. Буди Личес Патрон заувек!");i['permanentPatron']="Сада имаш трајни Патрон налог.";i['recurringBilling']="Поновно наплаћивање, обнављајући твоја Патрон крила сваког месеца.";i['serversAndDeveloper']=s("Пре свега, на моћне сервере.\nНакон тога, плаћамо програмера: %s, оснивача Личеса.");i['singleDonation']="Једнократна донација која ти даје Патрон крила један месец.";i['stopPayments']="Повуци своју кредитну картицу и обустави наплате:";i['thankYou']="Хвала на донацији!";i['transactionCompleted']="Твоја трансакција је завршена и рачун за твоју донацију је послат на твоју е-пошту.";i['viewOthers']="Погледај остале Личес Патроне";i['weAreNonProfit']="Ми смо непрофитно удружење јер верујемо да сви требају имати приступ бесплатној шаховској платформи светске класе.";i['weAreSmallTeam']="Ми смо мали тим, па твоја подршка прави велику разлику!";i['weRelyOnSupport']="За наш успех нам је неопходна ваша помоћ. Ако уживате у Личесу, молимо размислите о помагању путем донација којим постајете Патрон!";i['whereMoneyGoes']="Куда сав новац иде?";i['withCreditCard']="Кредитна Картица";i['xBecamePatron']=s("%s је постао Личес Патрон");i['xOrY']=s("%1$s или %2$s");i['youHaveLifetime']="Имаш доживотни Патрон налог. То је врло страва!";i['youSupportWith']=s("Ти подржаваш lichess.org са %s месечно.")})()