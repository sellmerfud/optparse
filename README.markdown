## Overview##
OptionParser is a class that handles the parsing of switches and arguments on the command line.
It is based on the Ruby OptionParser class that is part of the standard Ruby library. It supports
POSIX style short option switches as well as GNU style long option switches.
By using closures when defining command line switches your code becomes much easier to write and maintain.

## Maven Information ##
    groupId:     org.sellmerfud
    artifactId:  optparse_2.10, optparse_2.9.2
    version:     2.0
    
## Sbt Configuration ##
    libraryDependencies += "org.sellmerfud" % "optparse_2.10" % "2.0"
    -- or --
    libraryDependencies += "org.sellmerfud" % "optparse_2.9.2" % "2.0"

    // And if Sonatype isn't in your list of resolvers, for some reason:
    resolvers += "Sonatype Nexus releases" at "https://oss.sonatype.org/content/repositories/releases"

    
## Earlier Versions ##
If you are using version 1.0 of this library [click here to see the README][README 1.0] for that version.
The api in version 2.0 has changed and is not code compatible with version 1.0.  Also the master branch
is built against Scala 2.10 and is not compatible with earlier versions of Scala. 
If you are using a Scala version prior to 2.9.2, then use the 1.0 version of this library.

[README 1.0]: https://github.com/sellmerfud/optparse/blob/master/README-1.0.markdown
    
## Features ##
* The switch specification and the code to handle it are written in the same place.
* Automatically formats a help summary.
* Supports both short (-q) and long (--quiet) switches.
* Long switch names can be abbreviated on the command line.
* Switch arguments are fully typed so your code does not have to parse and convert them.
* Switch arguments can be restricted to a set of valid values.
* You can easily define your own argument parsers and/or replace the default ones.
* Complete ScalaDoc documentation.
* Full set of unit tests.

## Configuration Class ##
The OptionParser class has a single type parameter.  This type parameter specifies the
configuration class that your code will use to aggregate the command line switches and 
arguments.  Each time a command line switch or argument is processed a function is called
with your configuration class.  The function should modify the configuration class or 
preferably construct a new immutable instance of the class and return it.
The command line parser will return the configuration class upon successfully parsing the
command line.

## Defining Switches ##
You define a switch by supplying its name(s), description, and a function that will be 
called each time the switch is detected in the list of command line arguments.
Your function has the type `{ (value: T, cfg: C) => C }`.  You supply the type for `T` and the framework
will select the appropriate parser and call your function with the value converted to your
expected type. The type `C` is your configuration class that was specified when the OptionParser
was instantiated.

```scala
case class Config(revision: Int = 0, args: List[String] = Nil)
val cli = new OptionParser[Config] {
  reqd[Int]("-r", "--revision NUM", "Choose revision") { (value, cfg) => cfg.copy(revision = value) }
  args[String] { (args, cfg) => cfg.copy(args = args) }
}
val config = cli.parse(List("-r", "9"), Config())
```
The `reqd()` function defines a switch that takes a required argument.  In this case we have
specified that we expect the argument value to be an `Int`.  If the user enters a value that
is not a valid integer then an [[org.sellmerfud.optparse.OptionParserException]] is thrown with an appropriate error 
message.  If the value is valid then our supplied function is called with the integer value.
Here we return a copy of our configuration class with the `revision` field updated. 

## Non-Switch Arguments ##
Anything encountered on the command line that is not a switch or an argument to a switch is 
passed to the function supplied to the `args()` method.  Like switch arguments, the non-switch
arguments can be of any type for which you have a defined argument parser.  In the example
above we have specified that non-switch arguments are of type `String`.

## Switch Names ##
A switch may have a short name, a long name, or both.

Short names may be specified as a single character preceded by a single dash.  You may optionally
append an argument name separated by a space.  The argument name is only for documentation
purposes and is displayed with the help text.

    -t           <== no argument
    -t ARG       <== required argument
    -t [VAL]     <== optional argument


Long names may be any number of characters and are preceded by two dashes. You may optionally
append an argument name.  The argument name may be separated by a space or by an equals sign.
This will affect how the name is displayed in the help text.

    --quiet
    --revision REV
    --revision=REV
    --start-date [TODAY]
    --start-date=[TODAY]
    --start-date[=TODAY]

Notice that in the case of an optional parameter you may put the equal sign inside or outside
the bracket.  Again this only affects how it is dispalyed in the help message.  If you specify
an argument name with both the short name and the long name, the one specified with the long
name is used.

There is a boolean switch that does not accept a command line argument but may be negated
when using the long name by preceding the name with `no-`.  For example:

    cli.bool("-t", "--timestamp", "Generate a timestamp") { (v, c) => c.copy(genTimestamp = v) }

    can be specified on the command line as:
      -t                <== function called with v == true
      --timestamp       <== function called with v == true
      --no-timestamp    <== function called with v == false
      --no-t            <== function called with v == false  (using partial name)

Notice that you only specify the positive form of the name when defining the switch. The help
text for this switch looks like this:

    -t, --[no-]timestamp            Generate a timestamp

## Special Tokens ##

* `--` is interpreted as the ''end of switches''.  When encountered no following arguments on the
command line will be treated as switches.
* `-`  is interpreted as a normal argument and not a switch.  It is commonly used to indicate `stdin`.

## Switch Types ##
You can define switches that take no arguments, an optional argument, or a required argument.

    Flag
        cli.flag("-x", "--expert", "Description") { (cfg) => ... }

    Boolean
        cli.bool("-t", "--timestamp", "Description") { (v, cfg) => ... }

    Required Argument
        cli.reqd[String]("-n", "--name=NAME", "Description") { (v, cfg) => ... }

    Optional Argument
        cli.optl[String]("-d", "--start-date[=TODAY]", "Description") { (v, cfg) => ... }

    Comma Separated List
          cli.list[String]("-b", "--branches=B1,B2,B3", "Description") { (v, cfg) => ... }

## Limiting Values ##
For switches that take arguments, either required or optional, you can specify a list of
acceptable values.

```scala
case class Config(color: String = "red")
val cli = new OptionParser[Config] {
  reqd[String]("", "--color COLOR", List("red", "green", "blue")) { (v, c) => c.copy(color = v) }
}
```
Here if the user enters `--color purple` on the command line an `OptionParserException` is
thrown.  The exception message will display the accepable values.  Also the user can enter
partial values.

    coolapp --color r     // <==  Will be interpreted as red

If the value entered matches two or more of the acceptable values then an `OptionParserException` is
thrown with a message indicating that the value was ambiguous and displays the acceptable values.
Also note that you can pass a `Map` instead of a `List` if you need to map string values to
some other type.
```scala
class Color(rgb: String)
val red   = new Color("#FF0000")
val green = new Color("#00FF00")
val blue  = new Color("#0000FF")
case class Config(color: Color = red)

val cli = new OptionParser[Config] {
  optl[Color]("", "--color [ARG]", Map("red" -> red, "green" -> green, "blue" -> blue)) { 
    (v, c) => 
    c.copy(color = v getOrElse red)
  }
}
```
Since we are defining a switch with an optional argument, the type of `v` is `Option[Color]`.

## Banner, Separators and Help Text ##
You can specify a banner which will be the first line displayed in the help text. You can also
define separators that display information between the switches in the help text.
```scala
case class Config(...)
val cli = new OptionParser[Config] {
  banner = "coolapp [Options] file..."
  separator("")
  separator("Main Options:")
  flag("-f", "--force", "Force file creation") { (cfg) => ... }
  reqd[String]("-r REV", "", "Specify a revision") { (v, cfg) => ... }
  separator("")
  separator("Other Options:")
  optl[String]("", "--backup[=NAME]", "Make a backup", "--> NAME defaults to 'backup'")
    { (v, cfg) => ... }
  bool("-t", "--timestamp", "Create a timestamp") { (v, cfg) => ... }
}
println(cli)  // or println(cli.help)
```

    The above would print the following:
    coolapp [Options] file...

    Main Options:
        -f, --force                  Force file creation
        -n REV                       Specify a revision

    Other Options:
            --backup[=NAME]          Make a backup
                                     --> NAME defaults to 'backup'
        -t, --[no-]timestamp         Create a timestamp
        -h, --help                   Show this message

Where did the **`-h, --help`** entry come from?  By default the `help` switch is added automatically.
The function associated with it will print the help text to `stdout` and call `exit(0)`.
You can define your own help switch by simply defining a switch with either or both of the
names `-h`, `--help`.  You can also turn off the auto help altogether.

## How Short Switches Are Parsed ##
Short switches encountered on the command line are interpreted as follows:

    Assume that the following switches have been defined:
       -t, --text   (Takes no argument)
       -v           (Takes no argument)
       -f FILE      (Requires an argument)
       -b [OPT]     (Takes an optional argument)

    Switches that do not accept arguments may be specified separately or may be concatenated together:
       -tv  ==  -t -v

    A switch that takes an argument may be concatenated to one or more switches that do not take
    arguments as long as it is the last switch in the group:
        -tvf foo.tar  ==  -t -v -f foo.tar
        -tfv foo.tar  ==  -t -f v foo.tar  (v is the argument value for the -f switch)

    The argument for a switch may be specified with or without intervening spaces:
        -ffoo.tar  == -f foo.tar

    For arguments separated by space, switches with required arguments are greedy while those that
    take optional arguments are not. They will ignore anything that looks like a another switch.
       -v -f -t       <-- The -f option is assigned the value "-t"
       -v -f -text    <-- The -f option is assigned the value "-text"
       -v -f --text   <-- The -f option is assigned the value "--text"

       -v -b t        <-- The -b option is assigned the value "t"
       -v -b -t       <-- The -b option is interpreted without an argument
       -v -b -text    <-- The -b option is interpreted without an argument
       -v -b --text   <-- The -b option is interpreted without an argument
       -v -b-text     <-- The -b option is assigned the value "-text" (no intervening space)

## How Long Switches Are Parsed ##
Long switches encountered on the command line are interpreted as follows:

    Assume that the following switches have been defined:
       --timestamp       (Boolean - takes no argument)
       --file FILE       (Requires an argument)
       --backup[=BACKUP] (Takes an optional argument)

    The argument for a switch may be joined by an equals sign or may be separated by space:
        --file=foo.tar == --file foo.tar
        --backup=data.bak == --backup data.bak

    For arguments separated by space, switches with required arguments are greedy while those that take
    optional arguments are not. They will ignore anything that looks like a another switch. See the
    discussion of short switches above for an example.  The behavior for long switches is identical.

    Boolean switches may be negated.
        --timestamp      <-- The option is assigned a true value
        --no-timestamp   <-- The option is assigned a false value

## Full Example ##
```scala
import java.util.Date
import java.io.File
import java.text.{SimpleDateFormat, ParseException}
import org.sellmerfud.optparse._

object Sample {
  val dateFormat = new SimpleDateFormat("MM-dd-yyyy")
  case class Config(
    quiet:    Boolean        = false,
    expert:   Boolean        = false,
    name:     Option[String] = None,
    fileType: String         = "binary",
    base:     String         = "HEAD",
    date:     Date           = new Date(),
    libs:     List[File]     = Nil,
    fileArgs: List[String]   = Nil)

  def main(args: Array[String]) {
    val config = try {
      new OptionParser[Config] {
        // Add an argument parser to handle date values
        addArgumentParser[Date] { arg =>
          try   { dateFormat.parse(arg) }
          catch { case e: ParseException => throw new InvalidArgumentException("Expected date in mm-dd-yyyy format") }
        }
        banner = "coolapp [options] file..."
        separator("")
        separator("Options:")
        bool("-q", "--quiet", "Do not write to stdout.")
          { (v, cfg) => cfg.copy(quiet = v) }

        flag("-x", "", "Use expert mode")
          { (c) => c.copy(expert = true) }

        reqd[String]("-n <name>", "", "Enter you name.")
          { (v, c) => c.copy(name = Some(v)) }

        reqd[File]("-l", "--lib=<lib>", "Specify a library. Can be used mutiple times.")
          { (v, c) => c.copy(libs = c.libs :+ v) }

        reqd[Date]("-d", "--date <date>", "Enter date in mm-dd-yyyy format.")
          { (v, c) => c.copy(date = v) }

        reqd[String]("-t", "--type=<type>", List("ascii", "binary"), "Set the data type. (ascii, binary)")
          { (v, c) => c.copy(fileType = v) }

        optl[String]("-b", "--base[=<commit>]", "Set the base commit. Default is HEAD.")
          { (v, c) => c.copy(base = v getOrElse "HEAD") }
          
        args[String] { (v, c) => c.copy(fileArgs = v) }  
      }.parse(args, Config())
    }
    catch { case e: OptionParserException => println(e.getMessage); sys.exit(1) }

    println("config: " + config)
  }
}
```

    Command Line: -l /etc/foo --lib=/tmp/bar -x .profile -n Bob -d09-11-2001
    -------------------------------------------------------------------------------
    config: Config(false,true,Some(Bob),binary,HEAD,Tue Sep 11 00:00:00 CDT 2001,
                   List(/etc/foo, /tmp/bar), List(.profile))
    

    Command Line: --date=04/01/2011
    -------------------------------------------------------------------------------
    invalid argument: --date=04/01/2011   (Expected date in mm-dd-yyyy format)

    Command Line: --ty=ebcdic
    -------------------------------------------------------------------------------
    invalid argument: --ty ebcdic    (ascii, binary)

    Command Line: --typ=a
    -------------------------------------------------------------------------------
    config: Config(false,false,None,ascii,HEAD,Mon Feb 11 21:52:12 CST 2013,
                  List(), List())

## Unit Tests ##
A full set of unit tests using _ScalaTest_ is included.

## Dependencies ##
There are no external dependencies.

## License ##
MIT License -- You can use it as is or modify it however you like.
