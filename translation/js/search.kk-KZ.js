"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.search)window.i18n.search={};let i=window.i18n.search;i['advancedSearch']="Терең іздеу";i['aiLevel']="Компьютер деңгейі";i['analysis']="Талдау";i['ascending']="Өсу бойымен";i['color']="Түсі";i['date']="Күні";i['descending']="Кему бойымен";i['evaluation']="Бағалау";i['from']="Бастап";i['gamesFound']=p({"one":"%s ойын табылды","other":"%s ойын табылды"});i['humanOrComputer']="Қарсыласты таңдаңыз: адам не компьютер";i['include']="Құрамы";i['loser']="Жеңілген";i['maxNumber']="Ойын саны";i['maxNumberExplanation']="Іздеу нәтижесінде қанша ойын көрсету";i['nbTurns']="Жүрістер саны";i['onlyAnalysed']="Тек компьютерлік талдауға бейім ойындар";i['opponentName']="Қарсыластың аты";i['ratingExplanation']="Екі ойыншының орташа рейтингі";i['result']="Нәтиже";i['search']="Іздеу";i['searchInXGames']=p({"one":"%s ойында іздеу","other":"%s ойында іздеу"});i['sortBy']="Сұрыптау шарты";i['source']="Іздеу жері";i['to']="Дейін";i['winnerColor']="Жеңімпаз түсі";i['xGamesFound']=p({"one":"Бір ойын табылды","other":"%s ойын табылды"})})()