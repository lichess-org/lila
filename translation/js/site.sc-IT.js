"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.site)window.i18n.site={};let i=window.i18n.site;i['aiNameLevelAiLevel']=s("%1$s difficultade %2$s");i['chat']="Chat";i['checkmate']="Scaccu maccu";i['gameOver']="As perdiu";i['level']="Difficultade";i['nbMinutes']=p({"one":"%s minutu","other":"%s minutos"});i['playWithAFriend']="Gioga con unu amigu";i['playWithTheMachine']="Gioga chin su elaboradore";i['resign']="Arrendidi";i['strength']="Fortza";i['toInviteSomeoneToPlayGiveThisUrl']="Para inbidai calicunu dona issu custu URL";i['waiting']="Ispettende";i['waitingForOpponent']="Ispettende unu enemigu";i['yourTurn']="A tue"})()