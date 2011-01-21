
package org.fud.optparse


import collection.mutable.ListBuffer

class OptionParserException(m: String) extends RuntimeException(m)

/**
 * Option parser class based on Ruby class of same name.
 * Support command line parsing using POSIX style short and long arguments.
*/

class OptionParser {
  
  protected val argv = new ListBuffer[String]
  protected val switches = new ListBuffer[Switch]
  
  protected var _curr_arg_display = ""  // Used for error reporting
  def curr_arg_display = _curr_arg_display
  
  def help = switches.mkString("\n")
  override def toString = help
  
  
  class ArgumentMissing extends OptionParserException("argument missing: " + curr_arg_display)
  class InvalidArgument extends OptionParserException("invalid argument: " + curr_arg_display)
  class InvalidOption extends OptionParserException("invalid option: " + curr_arg_display)
  class AmbiguousOption extends OptionParserException("abmiguous option: " + curr_arg_display)
  
  abstract class Param
  
  case class Terminate() extends Param

  case class NonSwitch(arg: String) extends Param

  // Container for internal and display names of a switch.
  case class Names(short: String, long: String, display: String) {
    override val toString = display
  }
  
  // The short and long names are stored without leading '-' or '--'
  abstract class Switch(names: Names, val info: Seq[String] = List()) extends Param {
    val takesArg: Boolean = false
    val requiresArg: Boolean = false
    def short = names.short
    def long  = names.long
    
    // Called when this switch is detected on the command line.  Should handle the
    // invocation of the user's code to process this switch.
    def process: Unit = {}
    
    override lazy val toString = {
      val switches  = "    " + names
      val sep  = "\n" + " " * 37
      val sep1 = if (switches.length < 37) " " * (37 - switches.length) else sep
      switches + info.mkString(sep1, sep, "")
    }
  }
  
  
  class NoArgSwitch(n: Names, d: Seq[String], func: () => Unit) extends Switch(n, d) {
    override def process: Unit = func()
  }
  
  class ArgSwitch[T](n: Names, d: Seq[String], convert: String => T, func: T => Unit) extends Switch(n, d) {
    override val takesArg = true
    override val requiresArg = true
    
    override def process: Unit =
      if (argv.isEmpty) throw new ArgumentMissing else func(convert(argv.remove(0)))
  }

  class OptArgSwitch[T](n: Names, d: Seq[String], convert: String => T, func: Option[T] => Unit) extends Switch(n, d) {
    override val takesArg = true
    
    override def process: Unit = {
      if (argv.isEmpty || (argv(0).startsWith("-") && argv(0).length > 1)) // Single '-' is stdin argument
        func(None)
      else
        func(Some(convert(argv.remove(0))))
    }
  }

  val ShortSpec = """-(\S)(?:\s*(.+))?"""r
  val LongSpec  = """--([^\s=]+)(?:(=|\[=|\s+)(\S.*))?"""r
  
  // Parse the switch names and return the 'fixed' names.
  // Short name: 
  //   - must begin with a single '-'
  //   - may be followed by an argument name with or without separating space.
  // Long name:
  //   - must begin with two '--'
  //   - may be followed by a argument. There must be a single space or equals sign separating them.
  //
  // If an arg is specified for both the short and long, then the long one will take
  // precedence.
  // We also the display string for the names.
  protected def getNames(shortSpec: String, longSpec: String): Names = {
    var short = ""
    var long  = ""
    var arg   = ""
    var l_delim = " "
    shortSpec.trim match {
      case "" =>
      case ShortSpec(n, a) if a == null => short = n
      case ShortSpec(n, a) => short = n; arg = a
      case x => throw new OptionParserException("Invalid short name specification: " + x)
    }

    longSpec.trim match {
      case "" =>
      case LongSpec(n, _, a) if a == null => long = n
      case LongSpec(n, d, a) => long = n; l_delim = d.substring(0, 1); arg = a
      case x => throw new OptionParserException("Invalid long name specification: " + x)
    }
    
    val display = (short, long, arg) match {
      case ("", "",  _) => throw new OptionParserException("Both long and short name specifications cannot be blank")
      case (s,  "", "") => "-%s".format(s)
      case ("",  l, "") => "    --%s".format(l)
      case (s,  "",  a) => "-%s %s".format(s, a)
      case (s,   l, "") => "-%s, --%s".format(s, l)
      case ("",  l,  a) => "    --%s%s%s".format(l, l_delim, a)
      case (s,   l,  a) => "-%s, --%s%s%s".format(s, l, l_delim, a)
    }
    Names(short, long, display)
  }
    
  // A list of registered converters
  protected var _converters = List[(ClassManifest[_], String => _)]()
  
  // Look up a converter given a ClassManifest.
  // Throws an exception if not found.
  protected def converter[T](m: ClassManifest[T]): String => T = {
    // Check for exact match first then subclass
    _converters.find(m == _._1).map(_._2.asInstanceOf[String => T]).getOrElse {
      throw new OptionParserException("No Converter found for " + m)
    }
  }
  
  // Register a converter function for the given 
  def addConverter[T](f: String => T)(implicit m: ClassManifest[T]): Unit = {
    _converters = (m -> f) :: _converters
  }
  
  // Define default converters
  addConverter {s: String => s}
  addConverter { s: String => 
    try { s.toInt } catch { case _: NumberFormatException => throw new InvalidArgument }
  }
  addConverter { s: String => 
    try { s.toShort } catch { case _: NumberFormatException => throw new InvalidArgument }
  }
  addConverter { s: String => 
    try { s.toLong } catch { case _: NumberFormatException => throw new InvalidArgument }
  }
  addConverter { s: String => 
    try { s.toFloat } catch { case _: NumberFormatException => throw new InvalidArgument }
  }
  addConverter { s: String => 
    try { s.toDouble } catch { case _: NumberFormatException => throw new InvalidArgument }
  }
  addConverter { s: String => 
    s.toList match {
      case c :: Nil => c
      case _ => throw new InvalidArgument
    }
  }
  
  // Define a switch that takes no arguments
  def noArg(short: String, long: String, info: String*)(func: () => Unit): Unit = {
    val s = new NoArgSwitch(getNames(short, long), info, func)
    switches += s
    s
  }

  // Define a switch that takes a required argument
  def reqArg[T](short: String, long: String, info: String*)(func: T => Unit)(implicit m: ClassManifest[T]): Unit = {
    val s = new ArgSwitch(getNames(short, long), info, converter(m), func)
    switches += s
    s
  }
  
  // Define a switch that takes an optional argument
  def optArg[T](short: String, long: String, info: String*)(func: Option[T] => Unit)(implicit m: ClassManifest[T]): Unit = {
    val s = new OptArgSwitch(getNames(short, long), info, converter(m), func)
    switches += s
    s
  }
  
  // Look up a switch by the given long name.
  // Partial name lookup is performed. If more than one match is found then if one is an
  // exact match it wins, otherwise and AmbiguousOption exception is thrown
  // Throws InvalidOption if the switch cannot be found
  def longSwitch(name: String): Switch = {
    switches.toList.filter(_.long.startsWith(name)).sortWith(_.long.length < _.long.length) match {
      case x :: Nil => x
      case x :: xs  => if (x.long == name) x else throw new AmbiguousOption
      case Nil => throw new InvalidOption
    }
  }
  
  def shortSwitch(name: String): Switch = {
    switches.find(_.short == name).getOrElse { throw new InvalidOption }
  }
  
  val TerminationToken   = "--"r
  val StdinToken         = "(-)"r
  val LongSwitchWithArg  = "--([^=]+)=(.*)"r
  val LongSwitch         = "--(.*)"r
  val ShortSwitch        = "-(.)(.*)"r
  
  // Get the next parameter from the argv buffer.
  // It is possible that this routine will prepend switches back onto the argv
  // where there is a short switch that does not take an argument with other
  // characters appended to it.
  //    -cvf  :Where -c is a switch that does not take arguments
  // In this case we would prepend -vf back onto the argv buffer.
  // If there is an argument appended to a switch then it is returned rather
  // than prepended to the argv buffer.  This is necessary because the argument may start
  // with a dash and if it were prepened to argv, we would erroneously treat it as a switch.
  def nextParam: Param = {
    if (argv.isEmpty)
      Terminate()
    else {
      _curr_arg_display = argv(0)
      argv.remove(0) match {
        // The order of the cases here is important!
        case TerminationToken()           => Terminate()
        case StdinToken(arg)              => NonSwitch(arg)
        case LongSwitchWithArg(name, arg) => longSwitch(name) match {
          case switch if switch.takesArg => arg +=: argv; switch
          case switch => ("-" + arg) +=: argv; switch
        }
        case LongSwitch(name)             => longSwitch(name)
        case ShortSwitch(name, "")        => shortSwitch(name)
        case ShortSwitch(name, rest)      => shortSwitch(name) match {
          case switch if switch.takesArg => rest +=: argv; switch
          case switch => ("-" + rest) +=: argv; switch
        }
        case arg                          => NonSwitch(arg)
      }
    }
  }
  
  // Parse the given command line.  Each token from the command line should be
  // in a separate entry in the given sequence such as the array of strings passed
  // to def main(args: Array[String]) {}
  // The options are processed using the code that has been previously specified when
  // setting up the parser.  A List[String] of all non-option arguments is returned.
  def parse(args: Seq[String]): List[String] = {
    val non_switch_args = new ListBuffer[String]
    argv.clear // Clear any remnants
    argv ++= args
    
    var terminate = false
    while (!terminate) {
      nextParam match {
        case Terminate()    => non_switch_args ++= argv; terminate = true
        case NonSwitch(arg) => non_switch_args += arg
        case switch: Switch => switch.process
      }
    }
    
    non_switch_args.toList
  }
}

object Foo {
  def main(args: Array[String]): Unit = {
    var results: Map[String, Any] = Map.empty
    val opts = new OptionParser
    opts.noArg("-x", "--expert", "Expert option") { () => results += "expert" -> true }
    // opts.noArg("-h", "--help", "Display Help") { () => results += "help" -> true }
    // opts.reqArg("-n", "--name", "Set Name") { name: String => results += "name" -> name }
    // opts.reqArg("-s", "--size", "Set Size") { size: Int => results += "size" -> size }
    // opts.optArg("-r", "--reset", "Reset..") { dir: Option[String] => results += "dir" -> dir.getOrElse("top")}
    // opts.optArg("-a", "--at", "At..") { at: Option[Int] => results += "at" -> at.getOrElse(100)}
    opts.parse(List("-x", "foo"))
  }
}
