"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.oauthScope)window.i18n.oauthScope={};let i=window.i18n.oauthScope;i['alreadyHavePlayedGames']="Вече сте играли игри!";i['apiDocumentation']="Документацията за API";i['carefullySelect']="Внимателно изберете какво е позволено да се прави от ваша страна.";i['challengeBulk']="Създай много игри наведнъж за други играчи";i['challengeRead']="Прочети предстоящите промени";i['challengeWrite']="Изпрати, приеми, откажи предизвикателства";i['copyTokenNow']="Не забравяйте да запазите личния си токен. Няма да имате възможност да го видите отново!";i['doNotShareIt']="Токенът ще има достъп до вашият акаунт. НЕ го споделяйте с никого!";i['emailRead']="Прочети имейл адреса";i['followWrite']="Следвай и отследвай други играчи";i['forExample']=s("Например: %s");i['lastUsed']=s("Последно употребено %s");i['msgWrite']="Изпращане на лично съобщение до други играчи";i['newAccessToken']="Нов личен API токен за достъп";i['preferenceRead']="Прочетете предпочитанията";i['preferenceWrite']="Напишете предпочитания";i['puzzleRead']="Виж пъзел активност";i['racerWrite']="Създай и се присъедини към пъзел състезания";i['rememberTokenUse']="За да запомните за какво се използва токена";i['studyRead']="Чети лични проучвания и излъчвания";i['studyWrite']="Създай, обнови, изтрии проучвания и излъчвания";i['teamLead']="Управление на клубовете които ръководите: изпращане на лични съобщения, изключване на членове";i['teamRead']="Прочети личната информация на отбора";i['teamWrite']="Присъедини се и напусни отбори";i['tokenDescription']="Описание на токена";i['tokenGrantsPermission']="Токенът разрешава на други хора да използват вашия акаунт.";i['tournamentWrite']="Създай, обновни, присъедини се към турнири";i['whatTheTokenCanDo']="Какво може да прави от ваше име токенът:"})()