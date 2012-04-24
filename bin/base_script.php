<?php

$play = "/home/Play20/play";

function show_run($text, $command, $catch = false)
{
    echo "\n* $text\n$command\n";
    passthru($command, $return);
    if (0 !== $return && !$catch) {
        echo "\n/!\\ The command returned $return\n";
        exit(1);
    }
}

function show_run_catch($text, $command)
{
    show_run($text, $command, true);
}

function read_arg(array $argv, $index, $default = null)
{
    return isset($argv[$index+1]) ? $argv[$index+1] : $default;
}
