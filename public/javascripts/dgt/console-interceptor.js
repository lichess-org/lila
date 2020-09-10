rewireLoggingToElement(
    () => document.getElementById("dgt-play-zone-log"),
    () => document.getElementById("dgt-play-zone"), true);

function rewireLoggingToElement(eleLocator, eleOverflowLocator, autoScroll) {
    fixLoggingFunc('log');
    fixLoggingFunc('debug');
    fixLoggingFunc('warn');
    fixLoggingFunc('error');
    fixLoggingFunc('info');
    fixLoggingFunc('table');

    function fixLoggingFunc(name) {
        console['old' + name] = console[name];
        console[name] = function(...arguments) {
        var output = produceOutput(name, arguments);
            if (output != "*" && output != ":")
                output += "<br>";               
            const eleLog = eleLocator();
            if (autoScroll) {
                const eleContainerLog = eleOverflowLocator();
                const isScrolledToBottom = eleContainerLog.scrollHeight - eleContainerLog.clientHeight <= eleContainerLog.scrollTop + 1;
                eleLog.innerHTML += output;
                if (isScrolledToBottom) {
                    eleContainerLog.scrollTop = eleContainerLog.scrollHeight - eleContainerLog.clientHeight;
                }
            } else {
                eleLog.innerHTML += output;
            }

            console['old' + name].apply(undefined, arguments);
        };
    }

    function produceOutput(name, args) {
        return args.reduce((output, arg) => {
            if (String(arg).length < 2)
                return arg;
            else
                return output +
                    "<span class=\"log-" + (typeof arg) + " log-" + name + "\">" +
                        (typeof arg === "object" && (JSON || {}).stringify ? JSON.stringify(arg) : arg) +
                    "</span>&nbsp;";
        }, '');
    }
}