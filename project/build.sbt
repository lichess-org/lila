// a 'compileonly' configuation
ivyConfigurations += config("compileonly").hide

libraryDependencies += "org.apache.commons" % "commons-lang3" % "3.5" % "compileonly"

// appending everything from 'compileonly' to unmanagedClasspath
unmanagedClasspath in Compile ++=
  update.value.select(configurationFilter("compileonly"))
