"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.emails)window.i18n.emails={};let i=window.i18n.emails;i['common_contact']=s("За да се свържете с нас, посетете %s.");i['common_note']=s("Това е служебен имейл, свързан с използването Ви на %s.");i['common_orPaste']="(Щракването не работи? Опитайте да поставите линка в браузъра!)";i['emailChange_click']="За да потвърдите, че сте собственик на този имейл, моля, щракнете на:";i['emailChange_intro']="Поискахте да промените Вашия имейл адрес.";i['emailChange_subject']=s("Потвърдете Вашия нов имейл адрес, %s");i['emailConfirm_click']="Щракнете върху връзката, за да активирате Lichess профила си:";i['emailConfirm_ignore']="Ако не сте се регистрирали в Lichess, може спокойно да игнорирате това съобщение.";i['emailConfirm_subject']=s("Потвърдете регистрацията си в lichess.org, %s");i['logInToLichess']=s("Влезте в lichess.org, %s");i['passwordReset_clickOrIgnore']="Ако сте направили това искане, щракнете върху връзката по-долу. В противен случай може да игнорирате това съобщение.";i['passwordReset_intro']="Получихме заявка за възстановяване на паролата за Вашия акаунт.";i['passwordReset_subject']=s("Възстановете паролата си в lichess.org, %s");i['welcome_subject']=s("Добре дошли в lichess.org, %s");i['welcome_text']=s("Успешно създадохте профила си в https://lichess.org.\nВашият профил е: %1$s. Може да го персонализирате по ваш вкус на %2$s.\nЗабавлявайте се, и нека фигурите ви винаги да достигат до противниковия цар!")})()