
package org.fud.optparse


import collection.mutable.ListBuffer

/**
 * Option parser class based on Ruby class of same name.
 * Support command line parsing using POSIX style short and long arguments.
*/

class OptionParser {
  
  protected val argv = new ListBuffer[String]
  protected val switches = new ListBuffer[Switch]
  
  protected var curr_arg_display = ""  // Used for error reporting
  
  class OptParseException(m: String) extends RuntimeException(m)
  class ArgumentMissing extends OptParseException("argument missing: " + curr_arg_display)
  class InvalidArgument extends OptParseException("invalid argument: " + curr_arg_display)
  class InvalidOption extends RuntimeException("invalid option: " + curr_arg_display)
  class AmbiguousOption extends RuntimeException("abmiguous option: " + curr_arg_display)
  

  abstract class Param
  
  case class Terminate() extends Param

  case class NonSwitch() extends Param
  
  // The short and long names are stored without leading '-' or '--'
  abstract class Switch(val short: String = "", val long: String = "", val display: Seq[String] = List()) extends Param {
    val takesArg: Boolean = false
    val requiresArg: Boolean = false
    // Called when this switch is detected on the command line.  Should handle the
    // invocation of the user's code to process this switch.
    def process(arg: Option[String]): Unit = {}
  }
  
  
  class NoArgSwitch(s: String, l: String, d: Seq[String], func: () => Unit) extends Switch(s, l, d) {
    override def process(arg: Option[String]): Unit = func()
  }
  
  class ArgSwitch[T](s: String, l: String, d: Seq[String], convert: String => T, func: T => Unit) extends Switch(s, l, d) {
    override val takesArg = true
    override val requiresArg = true
    
    override def process(arg: Option[String]): Unit = {
      arg match {
        case Some(a) => func(convert(a))
        case None    => if (argv.isEmpty) throw new ArgumentMissing else func(convert(argv.remove(0)))
      }
    }
  }

  class OptArgSwitch[T](s: String, l: String, d: Seq[String], convert: String => T, func: Option[T] => Unit) extends Switch(s, l, d) {
    override val takesArg = true
    
    override def process(arg: Option[String]): Unit = {
      if (argv.isEmpty || argv(0).startsWith("-"))
        func(None)
      else
        func(Some(convert(argv.remove(0))))
    }
  }
  
  
  private def withVal[T](v: T)(f: T => Unit): T = { f(v); v }
  private def cleanName(name: String) = name.dropWhile(_ == '-').mkString
  
  def noArg(short: String, long: String, display: String*)(func: () => Unit): Unit = {
    val s = new NoArgSwitch(cleanName(short), cleanName(long), display, func)
    switches += s
    s
  }
  
  def string(short: String, long: String, display: String*)(func: String => Unit): Unit = {
    val convert = { s: String => s}
    val s = new ArgSwitch(cleanName(short), cleanName(long), display, convert, func)
    switches += s
    s
  }
  
  def int(short: String, long: String, display: String*)(func: Int => Unit): Unit = {
    val convert = { s: String => 
      try { s.toInt } catch { case _: NumberFormatException => throw new InvalidArgument }
    }
    val s = new ArgSwitch(cleanName(short), cleanName(long), display, convert, func)
    switches += s
    s
  }

  
  def optString(short: String, long: String, display: String*)(func: Option[String] => Unit): Unit = {
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
  def nextParam: (Param, Option[String]) = {
    if (argv.isEmpty)
      (Terminate(), None)
    else {
      curr_arg_display = argv(0)
      argv.remove(0) match {
        case TerminationToken()           => (Terminate(), None)
        case StdinToken(arg)              => (NonSwitch(), Some(arg))
        case LongSwitchWithArg(name, arg) => (longSwitch(name), Some(arg))
        case LongSwitch(name)             => (longSwitch(name), None)
        case ShortSwitch(name, "")        => (shortSwitch(name), None)
        case ShortSwitch(name, rest)      =>
          shortSwitch(name) match {
            case switch if switch.takesArg => (switch, Some(rest))
            case switch => ("-" + rest) +=: argv; (switch, None)
          }
        case arg                          => (NonSwitch(), Some(arg))
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
        case (Terminate(), _)      => non_switch_args ++= argv; terminate = true
        case (NonSwitch(), arg)    => non_switch_args += arg.get
        case (switch: Switch, arg) => switch.process(arg)
      }
    }
    
    non_switch_args.toList
  }
}
