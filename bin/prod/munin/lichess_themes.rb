#!/usr/bin/env ruby

script = 'db.user2.group({cond:{"settings.theme":{$exists:true}},key:{"settings.theme":true},initial:{s:0},reduce:function(o,p){p.s++;}}).forEach(function(y) { print(y["settings.theme"] + ".value " + y.s);});'
command = 'mongo lichess --eval \'%s\'' % script

themes = ['green', 'brown', 'blue', 'grey', 'wood', 'canvas']

if ARGV.include? 'config'
  puts "graph_title Lichess themes popularity
graph_args --base 1000 -l 0
graph_vlabel users
graph_category lichess
"
themes.each do |theme|
  puts "#{theme}.label #{theme}
#{theme}.draw LINE
"
end
else
  puts IO.popen(command).readlines.to_a[2..-1].join
end

