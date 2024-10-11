"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.arena)window.i18n.arena={};let i=window.i18n.arena;i['arenaTournaments']="Турнири у арени";i['berserk']="\\\"Berserk\\\" у арени";i['berserkAnswer']="Када играч кликне дугме \\\"Полуди\\\" на почетку партије, изгуби пола свог времена, али добитак вреди један више турнирски поен.\n\nАко \\\"Полудите\\\" у временским контролама са инкрементом такође вам се поништава инкремент. (1+2 је изузетак, даје вам 1+0)\n\nДугме \\\"Полуди\\\", није доступно у партијама са почетним временом нула (0+1, 0+2).\n\nДа би добили још један поен, такође морате одиграти бар првих 7 потеза у партији.";i['drawingWithinNbMoves']=p({"one":"Изједначење игре у првом %s потезу неће донети поене ни једном играчу.","few":"Изједначење у првих %s потеза неће донети поене ниједном играчу.","other":"Изједначење у првих %s потеза неће донети поене ниједном играчу."});i['history']="Прошле арене";i['howAreScoresCalculated']="Како су бодови рачунати?";i['howAreScoresCalculatedAnswer']="Добитак вам доноси 2 поена, реми вам доноси 1 поен, а губитак вам не доноси ниједан поен. \nАко добијете две партије узастопно добијаћете дупле поене, представљене иконом пламена.\nСледеће партије ће наставити да вам доносе дупле поене све док не добијете партију.\nТо јест, добитак ће вам донети 4 поена, реми 2 поена, а губитак вам не доноси ниједан поен.\n\nНа пример, два добитка па реми вреди 6 поена: 2 + 2 + (2 x 1)";i['howDoesItEnd']="Како се завршава?";i['howDoesItEndAnswer']="Турнир има сат који одбројава. Када сат стигне до нуле, сва места су замрзнута и победник је проглашен. Партије које још трају се морају завршити, али се не рачунају у турниру.";i['howDoesPairingWork']="Како ради упаривање?";i['howDoesPairingWorkAnswer']="На почетку турнира, играчи су упарени по њиховом рејтингу.\nЧим завршите партију, вратите се у турнир, тад ће те бити упарени са играчем близу вашег места у турниру. То осигурава минимално чекање, међутим можда нећете играти против свих играча у турниру.\nИграјте брзо и вратите се у турнир да играте још игара и освојите још поена.";i['howIsTheWinnerDecided']="Како је победник одлучен?";i['howIsTheWinnerDecidedAnswer']="Играч(и) са највише поена на крају турнира ће бити проглашен(и) победником/цима.";i['isItRated']="Да ли је рејтован?";i['isNotRated']="Овај турнир *није* рејтован и *неће* утицати на ваш рејтинг.";i['isRated']="Овај турнир је рејтован и утицаће на ваш рејтинг.";i['otherRules']="Остала важна правила";i['shareUrl']=s("Делите линк да се играчи прикључе: %s");i['someRated']="Неки турнири су рејтовани и утицаће на ваш рејтинг.";i['thereIsACountdown']="Постоји одбројавање за твој први потез. Не прављење потеза у овом периоду ће предати игру твом противнику.";i['thisIsPrivate']="Ово је приватан турнир";i['willBeNotified']="Бићете обавештени када турнир почне, тако да можете играти партију у другом прозору док чекате."})()