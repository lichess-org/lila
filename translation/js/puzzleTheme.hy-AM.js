"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.puzzleTheme)window.i18n.puzzleTheme={};let i=window.i18n.puzzleTheme;i['advancedPawn']="Առաջ գնացած զինվոր";i['advancedPawnDescription']="Զինվորը վերածվելու կամ զինվորը վերածվելու սպառնալիքի հետ կապված մարտավարություն։";i['advantage']="Առավելություն";i['advantageDescription']="Օգտագործեք որոշիչ առավելություն ստանալու Ձեր հնարավորությունը (200-ից 600 սանտիզինվոր)";i['anastasiaMate']="Անաստասիայի մատ";i['anastasiaMateDescription']="Ձին և նավակը (կամ թագուհին) մրցակցի արքային մատ են անում խաղատախտակի եզրի և մրցակցի այլ խաղաքարի միջև։";i['arabianMate']="Արաբական մատ";i['arabianMateDescription']="Ձին և նավակը մրցակցի արքային մատ են անում խաղատախտակի անկյունում։";i['attackingF2F7']="Գրոհ f2-ի կամ f7-ի վրա";i['attackingF2F7Description']="f2 կամ f7 զինվորների վրա ուղղված գրոհ, օրինակ, Ֆեգատելլոյի գրոհում (տապակած լյարդի սկզբնախաղում)։";i['attraction']="Հրապուրում";i['attractionDescription']="Փոխանակում կամ զոհաբերություն, որը ստիպում կամ մղում է մրցակցի խաղաքարին զբաղեցնել դաշտը, որից հետո հնարավոր է դառնում հետագա մարտավարական հնարքը։";i['backRankMate']="Մատ վերջին հորիզոնականում";i['backRankMateDescription']="Մատ արքային նրա իսկ հորիզոնականում, երբ նա շրջափակված է իր իսկ խաղաքարերով։";i['bishopEndgame']="Փղային վերջնախաղ";i['bishopEndgameDescription']="Միայն փղերով և զինվորներով վերջնախաղ։";i['bodenMate']="Բոդենի մատ";i['bodenMateDescription']="Խաչվող անկյունագծերում գտնվող երկու փղերը մատ են հայտարարում մրցակցի արքային, որը շրջափակված է սեփական խաղաքարերով։";i['capturingDefender']="Պաշտպանի վերացում";i['capturingDefenderDescription']="Այլ խաղաքարը պաշտպանող խաղաքարի շահում կամ փոխանակում՝ հետագայում անպաշտպան մնացած խաղաքարի շահումով։";i['castling']="Փոխատեղում";i['castlingDescription']="Արքայի տեղափոխումն ապահով տեղ և նավակի դուրսբերումը մարտի։";i['clearance']="Գծի կամ դաշտի ազատում";i['clearanceDescription']="Որպես կանոն, տեմպով կատարվող քայլ, որն ազատում է դաշտը, գիծը կամ անկյունագիծը՝ մարտավարական մտահղացումն իրագործելու նպատակով։";i['crushing']="Ջախջախում";i['crushingDescription']="Օգտագործեք մրցակցի վրիպումը՝ ջախջախիչ առավելություն (600 և ավելի սանտիզինվոր) ստանալու համար";i['defensiveMove']="Պաշտպանական քայլ";i['defensiveMoveDescription']="Ճշգրիտ քայլ կամ քայլերի հաջորդականություն, որոնք անհրաժեշտ են նյութական կամ առավելության կորստից խուսափելու համար։";i['deflection']="Շեղում";i['deflectionDescription']="Քայլ, որը մրցակցի խաղաքարը շեղում է կարևոր խնդրից, օրինակ, հանգուցային դաշտի պաշտպանությունից։";i['discoveredAttack']="Բացված հարձակում";i['discoveredAttackDescription']="Քայլ խաղաքարով, որ ծածկում է հեռահար խաղաքարի գրոհի գիծը։ Օրինակ, քայլ ձիով, որով բացվում է գիծը նրա հետևում կանգնած նավակի համար։";i['doubleBishopMate']="Մատ երկու փղով";i['doubleBishopMateDescription']="Հարակից անկյունագծերում գտնվող երկու փղերը մատ են հայտարարում մրցակցի արքային, որը շրջափակված է սեփական խաղաքարերով։";i['doubleCheck']="Կրկնակի շախ";i['doubleCheckDescription']="Շախ միաժամանակ երկու խաղաքարով՝ բաց հարձակման միջոցով։ Հնարավոր չէ վերցնել երկու գրոհող խաղաքարերը և հնարավոր չէ ծածկվել դրանցից, հետևաբար արքան կարող է միայն հեռանալ շախից։";i['dovetailMate']="«Ծիծեռնակի պոչ» մատ";i['dovetailMateDescription']="Մատ թագուհով կողքին կանգնած արքային, որի նահանջի միակ երկու դաշտերը զբաղեցնում են սեփական խաղաքարերը։";i['endgame']="Վերջնախաղ";i['endgameDescription']="Մարտավարություն խաղի վերջնամասում։";i['enPassantDescription']="Մարտավարություն «կողանցիկ հարված» կանոնի կիրառմամբ, որտեղ զինվորը կարող է հարվածել մրցակցի զինվորը, որը առաջին քայլն է կատարել՝ տեղաշարժվելով երկու դաշտ, ընդ որում՝ հատվող դաշտը գտնվում է մրցակցի զինվորի հարվածի տակ, որը կարող է վերցնել այդ զինվորը։";i['equality']="Հավասարեցում";i['equalityDescription']="Պարտված դիրքից հավասարեցրեք խաղը. պարտիան ավարտեք ոչ-ոքի կամ ստացեք նյութական հավասարություն (200 սանտիզինվորից պակաս)";i['exposedKing']="Մերկ արքա";i['exposedKingDescription']="Անպաշտպան կամ թույլ պաշտպանված արքան հաճախ դառնում է մատային գրոհի զոհը։";i['fork']="Պատառաքաղ";i['forkDescription']="Քայլ, որի դեպքում հարվածի տակ է հայտնվում մրցակցի երկու խաղաքար։";i['hangingPiece']="Անպաշտպան խաղաքար";i['hangingPieceDescription']="Մարտավարություն, որի ժամանակ մրցակցի խաղաքարը պաշտպանված չէ կամ լավ պաշտպանված չէ և կարող է վերցվել։";i['healthyMix']="Խառը խնդիրներ";i['healthyMixDescription']="Ամեն ինչից` քիչ-քիչ։ Դուք չգիտեք` ինչ է սպասվում, այնպես որ, պատրաստ եղեք ամեն ինչի։ Ինչպես իսկական պարտիայում։";i['hookMate']="Հուք մատ";i['hookMateDescription']="Մատ զինվորով պաշտպանված ձիով և նավակով, ընդ որում` մրցակցի զինվորներից մեկը զբաղեցնում է նրա արքայի նահանջի միակ դաշտը։";i['interference']="Ծածկում";i['interferenceDescription']="Քայլ, որով ծածկվում է մրցակցի հեռահար խաղաքարերի համագործակցության գիծը, որի արդյունքում այդ խաղաքարերը կամ նրանցից մեկը դառնում են անպաշտպան։ Օրինակ, ձին կանգնում է նավակների միջև գտնվող պաշտպանված վանդակին։";i['intermezzo']="Միջանկյալ քայլ";i['intermezzoDescription']="Սպասելի քայլ կատարելու փոխարեն, սկզբում կատարվում է այլ, անմիջական սպառնալիք ստեղծող քայլ, որին մրցակիցը պետք է պատասխանի։ Հայտնի է նաև  «Zwischenzug» կամ «Intermezzo» անուններով։";i['kingsideAttack']="Գրոհ արքայական թևում";i['kingsideAttackDescription']="Գրոհ մրցակցի՝ կարճ կողմում փոխատեղում կատարած արքայի վրա։";i['knightEndgame']="Ձիու վերջնախաղ";i['knightEndgameDescription']="Միայն ձիերով և զինվորներով վերջնախաղ։";i['long']="Եռաքայլ խնդիր";i['longDescription']="Երեք քայլ մինչև հաղթանակ։";i['master']="Վարպետների պարտիաներ";i['masterDescription']="Խնդիրներ տիտղոսակիր խաղացողների մասնակցությամբ պարտիաներից։";i['masterVsMaster']="Երկու վարպետների պարտիաներ";i['masterVsMasterDescription']="Խնդիրներ երկու տիտղոսակիր խաղացողների մասնակցությամբ պարտիաներից։";i['mate']="Մատ";i['mateDescription']="Ավարտեք խաղը գեղեցիկ";i['mateIn1']="Մատ 1 քայլից";i['mateIn1Description']="Արեք մատ մեկ քայլից։";i['mateIn2']="Մատ 2 քայլից";i['mateIn2Description']="Արեք մատ երկու քայլից։";i['mateIn3']="Մատ 3 քայլից";i['mateIn3Description']="Արեք մատ երեք քայլից։";i['mateIn4']="Մատ 4 քայլից";i['mateIn4Description']="Արեք մատ չորս քայլից։";i['mateIn5']="Մատ 5 և ավելի քայլից";i['mateIn5Description']="Գտեք դեպի մատը տանող քայլերի հաջորդականությունը։";i['middlegame']="Միջնախաղ";i['middlegameDescription']="Մարտավարություն խաղի երկրորդ փուլում։";i['oneMove']="Մեկքայլանի խնդիր";i['oneMoveDescription']="Խնդիր, որտեղ պետք է անել միայն մեկ հաղթող քայլ։";i['opening']="Սկզբնախաղ";i['openingDescription']="Մարտավարություն խաղի առաջին փուլում։";i['pawnEndgame']="Զինվորային վերջնախաղ";i['pawnEndgameDescription']="Վերջնախաղ զինվորներով։";i['pin']="Կապ";i['pinDescription']="Կապի օգտագործումով մարտավարություն, երբ խաղաքարը չի կարող քայլել, այլապես գրոհի տակ կհայտնվի նրա հետևում գտնվող ավելի արժեքավոր խաղաքարը։";i['playerGames']="Խաղացողի պարտիաները";i['playerGamesDescription']="Գտնել խնդիրներ, որոնք ստեղծվել են Ձեր պարտիաներից, կամ այլ խաղացողների պարտիաներից։";i['promotion']="Վերածում";i['promotionDescription']="Քայլ, որի ժամանակ զինվորը հասնում է վերջին հորիզոնականին և վերածվում նույն գույնի ցանկացած խաղաքարի, բացի արքայից։";i['puzzleDownloadInformation']=s("Այս խնդիրները հանրության սեփականությունն են, և Դուք կարող եք ներբեռնել դրանք՝ %s։");i['queenEndgame']="Թագուհու վերջնախաղ";i['queenEndgameDescription']="Միայն թագուհիներով և զինվորներով վերջնախաղ։";i['queenRookEndgame']="Թագուհով և նավակով վերջնախաղ";i['queenRookEndgameDescription']="Միայն թագուհիներով, նավակներով և զինվորներով վերջնախաղ։";i['queensideAttack']="Գրոհ թագուհու թևում";i['queensideAttackDescription']="Գրոհ մրցակցի՝ երկար կողմում փոխատեղում կատարած արքայի վրա։";i['quietMove']="Հանգիստ քայլ";i['quietMoveDescription']="Քայլ առանց շախի կամ խաղաքար վերցնելու, որն այնուամենայնիվ նախապատրաստում է անխուսափելի սպառնալիք։";i['rookEndgame']="Նավակային վերջնախաղ";i['rookEndgameDescription']="Միայն նավակներով և զինվորներով վերջնախաղ։";i['sacrifice']="Զոհաբերություն";i['sacrificeDescription']="Մարտավարություն, որի ժամանակ տրվում է որևէ խաղաքար` առավելություն ստանալու, մատ հայտարարելու կամ պարտիան ոչ-ոքի ավարտելու նպատակով։";i['short']="Երկքայլանի խնդիր";i['shortDescription']="Երկու քայլ մինչև հաղթանակ։";i['skewer']="Գծային հարձակում";i['skewerDescription']="Կապի տեսակ է, բայց այս դեպքում հակառակն է՝ ավելի թանկ խաղաքարը հայտնվում է պակաս արժեքավոր կամ համարժեք խաղաքարի գրոհի գծում։";i['smotheredMate']="Խեղդուկ մատ";i['smotheredMateDescription']="Մատ ձիով արքային, որը չի կարող փախչել, որովհետև շրջափակված է (խեղդված է) սեփական խաղաքարերով։";i['superGM']="Սուպերգրոսմայստերների պարտիաներ";i['superGMDescription']="Խնդիրներ աշխարհի լավագույն շախմատիստների պարտիաներից։";i['trappedPiece']="Խաղաքարի որսում";i['trappedPieceDescription']="Խաղաքարը չի կարող հեռանալ հարձակումից, քանի որ չունի նահանջի ազատ դաշտեր, կամ այդ դաշտերը ևս հարվածի տակ են։";i['underPromotion']="Թույլ վերածում";i['underPromotionDescription']="Զինվորի վերածում ոչ թե թագուհու, այլ ձիու, փղի կամ նավակի։";i['veryLong']="Բազմաքայլ խնդիր";i['veryLongDescription']="Չորս կամ ավելի քայլ հաղթելու համար։";i['xRayAttack']="Ռենտգեն";i['xRayAttackDescription']="Իրավիճակ, երբ հեռահար խաղաքարի հարձակման կամ պաշտպանության գծին կանգնած է մրցակցի խաղաքարը։";i['zugzwang']="Ցուգցվանգ";i['zugzwangDescription']="Մրցակիցը ստիպված է անել հնարավոր փոքրաթիվ քայլերից մեկը,  բայց քայլերից ցանկացածը տանում է դիրքի վատացման։"})()