"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.class)window.i18n.class={};let i=window.i18n.class;i['addLichessUsernames']="Добавете Lichess потребителските имена, за да ги поканите като учители. Едно за всеки ред.";i['addStudent']="Добави ученик";i['aLinkToTheClassWillBeAdded']="Адресът на класа ще бъде добавен автоматично към края на съобщението и няма нужда да го добавяте ръчно.";i['anInvitationHasBeenSentToX']=s("Изпратена е покана до %s");i['applyToBeLichessTeacher']="Кандидатствай за Lichess учител";i['classDescription']="Описание на класа";i['className']="Име на класа";i['classNews']="Новини за класа";i['clickToViewInvitation']="Натиснете тук, за да приемете поканата:";i['closeClass']="Затвори класа";i['closeDesc1']="Ученикът няма никога вече да може да използва тази регистрация. Затварянето е постоянно. Уверете се че ученикът разбира и се съгласява с това.";i['closeDesc2']="Вместо това, можете да предадете контрола върху регистрацията на ученика, така че тя/той да продължат да я използват.";i['closeStudent']="Закриване на регистрацията";i['closeTheAccount']="Постоянно закриване на регистрацията на ученика.";i['createANewLichessAccount']="Създадете нов Lichess акаунт";i['createDesc1']="Ако ученикът няма Lichess акаунт, можете да създадете негов тук.";i['createDesc2']="Няма нужда от e-mail адрес. Паролата ще се генерира автоматично, а Вие трябва да я предадете на ученика, за да могат да влязат.";i['createDesc3']="Важно: учениците не трябва да имат няколко акаунта.";i['createDesc4']="Ако вече имат акаунт, използвайте тази форма.";i['createMoreClasses']="създайте повече класове";i['createMultipleAccounts']="Създайте няколко Lichess акаунта наведнъж";i['createStudentWarning']="Можете да създавате регистрации само за истински ученици. Не използвайте тази възможност да правите допълнителни профили за себе си, защото ще бъдете блокирани.";i['editNews']="Редактирай новини";i['features']="Функционалности";i['freeForAllForever']="100% безплатно, безкрайно, без реклами и следене";i['generateANewPassword']="Генерирайте нова парола за ученика";i['generateANewUsername']="Генерирайте ново потребителско име";i['invitationToClass']=s("Вие сте поканен да влезете в класа \\\"%s\\\" като ученик.");i['invite']="Покани";i['inviteALichessAccount']="Поканете Lichess акаунт";i['inviteDesc1']="Ако ученикът вече има Lichess акаунт, може да го добавите в класа.";i['inviteDesc2']="Те ще получат съобщение в Lichess с линк, с който могат да влязат в класа.";i['inviteDesc3']="Важно: канете само ученици, които познавате и които активно ще се включват в час.";i['inviteDesc4']="Никога не изпращайте нежелани покани към други потребители.";i['invitedToXByY']=s("Поканен в %1$s от %2$s");i['inviteTheStudentBack']="Покани ученика отново";i['lastActiveDate']="Активен";i['lichessClasses']="Класове";i['lichessProfileXCreatedForY']=s("Lichess профил %1$s създаден за %2$s.");i['lichessUsername']="Lichess потребителско име";i['makeSureToCopy']="Копирайте генерираната парола. Няма да може да я видите след това!";i['managed']="Управляван";i['maxStudentsNote']=s("Имайте предвид, че един клас може да има до %1$s ученици. За да управлявате повече ученици, %2$s.");i['messageAllStudents']="Изпрати съобщение на всички ученици за нов учебен материал";i['multipleAccsFormDescription']=s("Можете също %s да създадете множество Lichess акаунти от списък с имена на ученици.");i['na']="Неналично";i['nbPendingInvitations']=p({"one":"Една покана в изчакване","other":"%s покани в изчакване"});i['nbStudents']=p({"one":"Ученик","other":"%s ученици"});i['nbTeachers']=p({"one":"Учител","other":"%s учители"});i['newClass']="Нов клас";i['news']="Новини";i['newsEdit1']="Всички новини за класа в едно поле.";i['newsEdit2']="Добавете последните новини в горната част. Не изтривайте предишни новини.";i['newsEdit3']="Отделни новини с ---\nЩе покаже хоризонтална линия.";i['noClassesYet']="Все още няма часове.";i['noRemovedStudents']="Няма премахнати ученици.";i['noStudents']="В този клас все още няма ученици.";i['nothingHere']="Нищо тук, засега.";i['notifyAllStudents']="Извести всички ученици";i['onlyVisibleToTeachers']="Да се вижда само от учители на класа";i['orSeparator']="или";i['overDays']="За дни";i['overview']="Преглед";i['passwordX']=s("Парола: %s");i['privateWillNeverBeShown']="Поверително. Това няма да бъде показано извън класа. Помага да запомните кой е ученикът.";i['progress']="Напредък";i['quicklyGenerateSafeUsernames']="Бързо създаване на безопасни потребителски имена и пароли за ученици";i['realName']="Истинско име";i['realUniqueEmail']="Истински, уникален имейл адрес на ученика. Ще му изпратим имейл за потвърждение с линк за дипломиране на акаунта.";i['release']="Дипломирай";i['releaseDesc1']="Дипломиран акаунт не може да бъде управляван отново. Ученикът ще може да управлява режима за деца и да смени паролата си.";i['releaseDesc2']="Ученикът ще остане в класа след дипломирането на акаунта.";i['releaseTheAccount']="Дипломирай акаунта, за да може ученикът да го управлява автономно.";i['removedByX']=s("Премахнати от %s");i['removedStudents']="Премахнат";i['removeStudent']="Премахни ученик";i['reopen']="Повторно отваряне";i['resetPassword']="Възстанови парола";i['sendAMessage']="Изпратете съобщение до всички ученици.";i['studentCredentials']=s("Ученик: %1$s\nПотребителско име: %2$s\nПарола: %3$s");i['students']="Ученици";i['studentsRealNamesOnePerLine']="Истински имена на учениците, по едно на ред";i['teachClassesOfChessStudents']="Води часове с ученици на шахмат с инструмента Lichess Часове.";i['teachers']="Учители";i['teachersOfTheClass']="Учители на класа";i['teachersX']=s("Учители: %s");i['thisStudentAccountIsManaged']="Този учебен акаунт е управляван";i['timePlaying']="Играно време";i['trackStudentProgress']="Следи прогреса на учениците в игри и пъзели";i['upgradeFromManaged']="Смени от управляван към автономен";i['useThisForm']="използвайте този формуляр";i['variantXOverLastY']=s("%1$s за последните %2$s");i['visibleByBothStudentsAndTeachers']="Видим от учители и ученици в класа";i['welcomeToClass']=s("Добре дошли във Вашият клас: %s.\nЕто линк за достъп към класа.");i['winrate']="Процент на победи";i['xAlreadyHasAPendingInvitation']=s("%s вече има чакаща покана");i['xIsAKidAccountWarning']=s("%1$s е на детски режим и не може да получи съобщението ви. Трябва да им предоставите URL адреса на поканата ръчно: %2$s");i['xisNowAStudentOfTheClass']=s("%s вече е ученик в класа");i['youAcceptedThisInvitation']="Вие приехте тази покана.";i['youDeclinedThisInvitation']="Вие отказахте тази покана.";i['youHaveBeenInvitedByX']=s("Поканени сте от %s.")})()