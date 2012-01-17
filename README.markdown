## Overview ##
OptionParser is a class that handles the parsing of switches and arguments on the command line.
It is based on the Ruby OptionParser class that is part of the standard Ruby library. It supports
POSIX style short option switches as well as GNU style long option switches.
By using closures when defining command line switches your code becomes much easier to write and maintain.

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

## Defining Switches ##
You define a switch by supplying its name(s), description, and a function that will be
called each time the switch is detected in the list of command line arguments.
Your function has the type `{ value: T => Unit }`.  You supply the type for `T` and the framework
will select the appropriate parser and call your function with the value converted to your
expected type.

    var revision = 0
    val cli = new OptionParser
    cli.reqd("-r", "--revision NUM", "Choose revision") { v: Int => revision = v }
    val args = cli.parse(List("-r", "9"))

The `reqd()` function defines a switch that takes a required argument.  In this case we have
specified that we expect the argument value to be an `Int`.  If the user enters a value that
is not a valid integer then an `OptionParserException` is thrown with an appropriate error
message.  If the value is valid then our supplied function is called with the integer value.
Here we simply save the value in a variable.  Anything encountered on the command line that
is not a switch or an argument to a switch is returned in a list by the parse function.

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

    cli.bool("-t", "--timestamp", "Generate a timestamp") { v => if (v) generateTimestamp() }

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
        cli.flag("-x", "--expert", "Description") { () => ... }

    Boolean
        cli.bool("-t", "--timestamp", "Description") { v: Boolean => ... }

    Required Argument
        cli.reqd("-n", "--name=NAME", "Description") { v: String => ... }

    Optional Argument
        cli.optl("-d", "--start-date[=TODAY]", "Description") { v: Option[String] => ... }

    Comma Separated List
          cli.list("-b", "--branches=B1,B2,B3", "Description") { v: List[String] => ... }

## Limiting Values ##
For switches that take arguments, either required or optional, you can specify a list of
acceptable values.

    def setColor(c: String) = ...
    cli.reqd("", "--color COLOR", List("red", "green", "blue")) { v => setColor(v) }

Here if the user enters `--color purple` on the command line an `OptionParserException` is
thrown.  The exception message will display the accepable values.  Also the user can enter
partial values.

    coolapp --color r     // <==  Will be interpreted as red

If the value entered matches two or more of the acceptable values then an `OptionParserException` is
thrown with a message indicating that the value was ambiguous and displays the acceptable values.
Also note that you can pass a `Map` instead of a `List` if you need to map string values to
some other type.

    class Color(rgb: String)
    val red   = new Color("#FF0000")
    val green = new Color("#00FF00")
    val blue  = new Color("#0000FF")
    def setColor(c: Color) = ...

    cli.optl("", "--color [ARG]", Map("red" -> red, "green" -> green, "blue" -> blue)) { v =>
      setColor(v.getOrElse(red))
    }

Notice here that we did not have to specify the type of the function parameter `v` because the
compiler can infer the type from the `Map[String, Color]` parameter. Since we are defining a
switch with an optional argument, the type of `v` is `Option[Color]`.

## Banner, Separators and Help Text ##
You can specify a banner which will be the first line displayed in the help text. You can also
define separators that display information between the switches in the help text.

    val cli = new OptionParser
    cli.banner = "coolapp [Options] file..."
    cli separator ""
    cli separator "Main Options:"
    cli.flag("-f", "--force", "Force file creation") { () => ... }
    cli.reqd("-r REV", "", "Specify a revision") { v: String => ... }
    cli separator ""
    cli separator "Other Options:"
    cli.optl("", "--backup[=NAME]", "Make a backup", "--> NAME defaults to 'backup'")
      { v: Option[String] => ... }
    cli.bool("-t", "--timestamp", "Create a timestamp") { v => ... }
    println(cli)  // or println(cli.help)

    Would print the following:
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

    import java.util.Date
    import java.io.File
    import java.text.{SimpleDateFormat, ParseException}
    import org.fud.optparse._

    object Sample {
      val dateFormat = new SimpleDateFormat("MM-dd-yyyy")

      def main(args: Array[String]) {
        var options = Map[Symbol, Any]('quiet -> false, 'expert -> false, 'base -> "HEAD")
        var libs = List[File]()
        val file_args = try {
          new OptionParser {
            // Add an argument parser to handle date values
            addArgumentParser[Date] { arg =>
              try   { dateFormat.parse(arg) }
              catch { case e: ParseException => throw new InvalidArgumentException("Expected date in mm-dd-yyyy format") }
            }
            banner = "coolapp [options] file..."
            separator("")
            separator("Options:")
            bool("-q", "--quiet", "Do not write to stdout.")
              { v => options += 'quiet -> v }

            flag("-x", "", "Use expert mode")
              { () => options += 'expert -> true }

            reqd[String]("-n <name>", "", "Enter you name.")
              { v => options += 'name -> v }

            reqd[File]("-l", "--lib=<lib>", "Specify a library. Can be used mutiple times.")
              { v => libs = v :: libs }

            reqd[Date]("-d", "--date <date>", "Enter date in mm-dd-yyyy format.")
              { v => options += 'date -> v }

            reqd[String]("-t", "--type=<type>", List("ascii", "binary"), "Set the data type. (ascii, binary)")
              { v => options += 'type -> v }

            optl[String]("-b", "--base[=<commit>]", "Set the base commit. Default is HEAD.")
              { v: Option[String] => options += 'base -> v.getOrElse("HEAD") }
          }.parse(args)
        }
        catch { case e: OptionParserException => println(e.getMessage); exit(1) }

        println("Options: " + options)
        println("Libraries: " + libs.reverse)
        println("File Args: " + file_args)
      }
    }

    Command Line: -l /etc/foo --lib=/tmp/bar -x .profile -n Bob -d09-11-2001
    -------------------------------------------------------------------------------
    Options: Map('name -> Bob, 'quiet -> false, 'base -> HEAD,
                 'date -> Tue Sep 11 00:00:00 CDT 2001, 'expert -> true)
    Libraries: List(/etc/foo, /tmp/bar)
    File Args: List(.profile)

    Command Line: --date=04/01/2011
    -------------------------------------------------------------------------------
    invalid argument: --date=04/01/2011   (Expected date in mm-dd-yyyy format)

    Command Line: --ty=ebcdic
    -------------------------------------------------------------------------------
    invalid argument: --ty ebcdic    (ascii, binary)

    Command Line: --typ=a
    -------------------------------------------------------------------------------
    Options: Map('quiet -> false, 'expert -> false, 'base -> HEAD, 'type -> ascii)
    Libraries: List()
    File Args: List()

## Unit Tests ##
A full set of unit tests using _ScalaTest_ is included.

## Building ##
I'm using a Rakefile for development.  There is also a build.sbt file.  However, the source is in
a single file: `src/main/scala/org/fud/optparse/OptionParser.scala`. Include it in your project
and build it however you like.

## Dependencies ##
This code requires Scala 2.8 or later as it relies on `ClassManifest`.
There are no external dependencies.

## License ##
MIT License -- You can use it as is or modify it however you like.




