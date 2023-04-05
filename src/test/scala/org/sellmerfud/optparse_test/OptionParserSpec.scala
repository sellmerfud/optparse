/* 
Copyright (c) 2011 Curt Sellmer

Permission is hereby granted, free of charge, to any person obtaining
a copy of this software and associated documentation files (the
"Software"), to deal in the Software without restriction, including
without limitation the rights to use, copy, modify, merge, publish,
distribute, sublicense, and/or sell copies of the Software, and to
permit persons to whom the Software is furnished to do so, subject to
the following conditions:

The above copyright notice and this permission notice shall be
included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/
package org.sellmerfud.optparse_test

import org.scalatest.FlatSpec
import org.scalatest.Matchers

import org.sellmerfud.optparse._

class OptionParserSpec extends FlatSpec with Matchers {
  val ARGUMENT_MISSING   = "argument missing:"
  val INVALID_ARGUMENT   = "invalid argument:"
  val AMBIGUOUS_ARGUMENT = "ambiguous argument:"
  val NEEDLESS_ARGUMENT  = "needless argument:"
  val INVALID_OPTION     = "invalid option:"
  val AMBIGUOUS_OPTION   = "ambiguous option:"
  val BOTH_BLANK         = "Both long and short name specifications cannot be blank";
  val INVALID_SHORT_NAME = "Invalid short name specification:"
  val INVALID_LONG_NAME  = "Invalid long name specification:"

  // ====================================================================================
  // ================== Definging Command Line Switches =================================
  // ====================================================================================

  "Defining switches" should "fail if both names are empty" in {
    case class Config()
    val cli = new OptionParser[Config]
    the [OptionParserException] thrownBy {
      cli.flag("", "  ") { (cfg) => cfg }
    } should have message BOTH_BLANK
  }
  
  // ====================================================================================

  it should "fail if the short name is not specified correctly" in {
    case class Config()
    val cli = new OptionParser[Config]
    // Missing leading dash
    val t1 = the [OptionParserException] thrownBy cli.flag("a", "") { (cfg) => cfg }
    t1.getMessage should startWith (INVALID_SHORT_NAME)
  
    // Must be only one character
    val t2 = the [OptionParserException] thrownBy cli.flag("-ab", "") { (cfg) => cfg }
    t2.getMessage should startWith (INVALID_SHORT_NAME)
    
    val t3 = the [OptionParserException] thrownBy cli.flag("-ab ARG", "") { (cfg) => cfg }
    t3.getMessage should startWith (INVALID_SHORT_NAME)
  }
  
  // ====================================================================================

  it should "fail if the long name is not specified correctly" in {
    case class Config()
    val cli = new OptionParser[Config]
    val t1 = the [OptionParserException] thrownBy cli.flag("", "name") { (cfg) => cfg }
    t1.getMessage should startWith (INVALID_LONG_NAME)
    
    val t2 = the [OptionParserException] thrownBy cli.flag("", "-name") { (cfg) => cfg }
    t2.getMessage should startWith (INVALID_LONG_NAME)

    val t3 = the [OptionParserException] thrownBy cli.flag("", "name ARG") { (cfg) => cfg }
    t3.getMessage should startWith (INVALID_LONG_NAME)
    
    val t4 = the [OptionParserException] thrownBy cli.flag("", "-name ARG") { (cfg) => cfg }
    t4.getMessage should startWith (INVALID_LONG_NAME)

    val t5 = the [OptionParserException] thrownBy cli.flag("", "name=ARG") { (cfg) => cfg }
    t5.getMessage should startWith (INVALID_LONG_NAME)
    
    val t6 = the [OptionParserException] thrownBy cli.flag("", "-name=ARG") { (cfg) => cfg }
    t6.getMessage should startWith (INVALID_LONG_NAME)
  }
  
  // ====================================================================================

  it should "accept the ARG to long switches in four different forms" in {
    case class Config()
    val cli = new OptionParser[Config] {
      flag("", "--name ARG") { (cfg) => cfg }
      flag("", "--name [ARG]") { (cfg) => cfg }
      flag("", "--name=ARG") { (cfg) => cfg }
      flag("", "--name[=ARG]") { (cfg) => cfg }
    }
  }
  
  // ====================================================================================

  it should "pass as long as either name is specified" in {
    case class Config()
    val cli = new OptionParser[Config] {
      flag("-a", "") { (cfg) => cfg }
      flag("-b ARG", "") { (cfg) => cfg }
      flag("", "--bcd") { (cfg) => cfg }
      flag("", "--cde ARG") { (cfg) => cfg }
      flag("-c", "--cde") { (cfg) => cfg }
    }
  }
  
  it should "allow non-boolean switches to start with --no-" in {
    case class Config()
    val cli = new OptionParser[Config] {
      flag("", "--no-updates") { (cfg) => cfg }
    }
  }

  // ====================================================================================

  it should "remove switches with identical names when adding a new switch" in {
    case class Config(notUsed: Boolean = false, notUsed2: Boolean = false, expert: Boolean = false, args: Vector[String] = Vector.empty)
    val cli = new OptionParser[Config] {
      flag("-x", "--notUsed") { (cfg) => cfg.copy(notUsed = true) }
      flag("-X", "--expert")  { (cfg) => cfg.copy(notUsed2 = true) }
      flag("-x", "--expert")  { (cfg) => cfg.copy(expert = true) }
      arg[String] { (arg, cfg) => cfg.copy(args = cfg.args :+ arg ) }
    }

    val config = cli.parse(Seq("-x"), Config())
    config.notUsed  shouldBe (false)
    config.notUsed2 shouldBe (false)
    config.expert   shouldBe (true)
    config.args     should be (empty)

    val config2 = cli.parse(Seq("--expert"), Config())
    config2.notUsed  shouldBe (false)
    config2.notUsed2 shouldBe (false)
    config2.expert   shouldBe (true)
    config2.args     should be (empty)
  }
  
  // ====================================================================================

  it should "remove switches with identical names negated names when adding a new switch" in {
    case class Config(ignore: Option[Boolean] = None, noIgnore: Boolean = false, noUpdate: Boolean = false, update: Option[Boolean] = None)
    val cli = new OptionParser[Config] {
      bool("", "--ignore")    { (v, cfg) => cfg.copy(ignore = Some(v)) }
      flag("", "--no-ignore") { (cfg)    => cfg.copy(noIgnore = true) }
      flag("", "--no-update") { (cfg)    => cfg.copy(noUpdate = true) }
      bool("", "--update")    { (v, cfg) => cfg.copy(update = Some(v)) }
    }

    val config = cli.parse(Seq("--no-ignore"), Config())
    config.ignore   shouldBe (None)
    config.noIgnore shouldBe (true)

    val config2 = cli.parse(Seq("--update"), Config())
    config2.noUpdate shouldBe (false)
    config2.update   shouldBe (Some(true))
    
    val config3 = cli.parse(Seq("--no-update"), Config())
    config3.noUpdate shouldBe (false)
    config3.update   shouldBe (Some(false))
  }
  
  // ====================================================================================
  it should "detect attempts to add an option for a type with no argument parser" in {
    class SomeClass(name: String)
    case class Config(zoo: Option[SomeClass] = None)
    val cli = new OptionParser[Config]
    
    a [RuntimeException] shouldBe thrownBy {
      cli.reqd[SomeClass]("-z", "--zoo") { (zoo, cfg)  => cfg.copy(zoo = Some(zoo)) }
    }
  }    
  
  it should "remove switches with integer short names when and int() swith is defined" in {
    case class Config(four: Boolean = false, intValue: Option[Int] = None)
    val cli = new OptionParser[Config] {
      flag("-4", "")    { cfg => cfg.copy(four = true) }
      int("-<number>")  { (v, cfg) => cfg.copy(intValue = Some(v)) }
    }
    
    val config = cli.parse(Seq("-4"), Config())
    config.four     shouldBe (false)
    config.intValue shouldBe (Some(4))
  }    
  
  it should "remove int() switches when a switch with an integer short name is defined" in {
    case class Config(four: Boolean = false, intValue: Option[Int] = None)
    val cli = new OptionParser[Config] {
      int("-<number>")  { (v, cfg) => cfg.copy(intValue = Some(v)) }
      flag("-4", "")    { cfg => cfg.copy(four = true) }
    }
    
    val config = cli.parse(Seq("-4"), Config())
    config.four     shouldBe (true)
    config.intValue shouldBe (None)
  }    
  
  
  // ====================================================================================
  // ================== Command Line Parsing ============================================
  // ====================================================================================

  "Commmand line parsing" should "handle empty argv" in {
    case class Config(name: Option[String] = None, expert: Boolean = false, args: Vector[String] = Vector.empty)
    val cli = new OptionParser[Config] {
      flag("-x", "--expert") { (cfg) => cfg.copy(expert = true) }
      reqd[String]("-n", "--name NAME") { (name, cfg) => cfg.copy(name = Some(name)) }
    }
    
    val config = cli.parse(Seq(), Config())
    config.args   should be (empty)
    config.name   should be (None)
    config.expert should be (false)
  }    

  // ====================================================================================
  
  it should "handle arg list without any switches" in {
    import java.io.File
    case class Config(name: Option[String] = None, expert: Boolean = false, args: Vector[File] = Vector.empty)
    val cli = new OptionParser[Config] {
      flag("-x", "--expert") { (cfg) => cfg.copy(expert = true) }
      reqd[String]("-n", "--name NAME") { (name, cfg) => cfg.copy(name = Some(name)) }
      arg[File] { (arg, cfg) => cfg.copy(args = cfg.args :+ arg ) }
    }

    val c1 = cli.parse(Seq("foo"), Config())
    c1.args   shouldBe (Vector(new File("foo")))
    c1.name   shouldBe (None)
    c1.expert shouldBe (false)
  
    val c2 = cli.parse(Seq("foo", "bar"), Config())
    c2.args   shouldBe (Vector(new File("foo"), new File("bar")))
    c2.name   shouldBe (None)
    c2.expert shouldBe (false)
  }    
  
  // ====================================================================================

  it should "handle switches that do not take arguments" in {
    case class Config(expert: Boolean = false, timestamp: Option[Boolean] = None)
    val cli = new OptionParser[Config] {
      flag("-x", "--expert") { (cfg) => cfg.copy(expert = true) }
      bool("-t", "--timestamp") { (v, cfg) => cfg.copy(timestamp = Some(v)) }
      arg[String] { (_, _) => throw new Exception("arg() should not be called!") }
    }

    // Short
    val c1 = cli.parse(Seq("-x"), Config())
    c1.expert    shouldBe (true)
    c1.timestamp shouldBe (None)
  
    val c2 = cli.parse(Seq("-x", "-t"), Config())
    c2.expert    shouldBe (true)
    c2.timestamp shouldBe (Some(true))

    val c3 = cli.parse(Seq("-xt"), Config())
    c3.expert    shouldBe (true)
    c3.timestamp shouldBe (Some(true))

    // Long
    val c4 = cli.parse(Seq("--expert"), Config())
    c4.expert    shouldBe (true)
    c4.timestamp shouldBe (None)
  
    val c5 = cli.parse(Seq("--expert", "--timestamp"), Config())
    c5.expert    shouldBe (true)
    c5.timestamp shouldBe (Some(true))

    val c6 = cli.parse(Seq("--expert", "--no-timestamp"), Config())
    c6.expert    shouldBe (true)
    c6.timestamp shouldBe (Some(false))
  }
  
  
  // ====================================================================================

  it should "detect invalid switches" in {
    case class Config(test: Boolean = false)
    val cli = new OptionParser[Config] {
      flag("-t", "--test") { (cfg) => cfg.copy(test = true) }
      arg[String] { (_, _) => throw new Exception("arg() should not be called!") }
    }
    
    val t1 = the [OptionParserException] thrownBy cli.parse(Seq("-x"), Config())
    t1.getMessage should startWith (INVALID_OPTION)
    
    val t2 = the [OptionParserException] thrownBy cli.parse(Seq("--expert"), Config())
    t2.getMessage should startWith (INVALID_OPTION)
  }
  
  
  // ====================================================================================

  it should "handle partial names of long switches" in {
    case class Config(
      text: Boolean = false,
      test: Boolean = false,
      list: Boolean = false,
      nice: Boolean = false,
      quiet: Option[Boolean] = None)
    val cli = new OptionParser[Config] {
      flag("", "--text") { (cfg) => cfg.copy(text = true) }
      flag("", "--test") { (cfg) => cfg.copy(test = true) }
      flag("", "--list") { (cfg) => cfg.copy(list = true) }
      flag("", "--nice") { (cfg) => cfg.copy(nice = true) }
      bool("", "--quiet") { (v, cfg) => cfg.copy(quiet = Some(v)) }
      arg[String] { (_, _) => throw new Exception("arg() should not be called!") }
    }
    
    val c1 = cli.parse(Seq("--l", "--tex"), Config())
    
    c1.text  shouldBe (true)
    c1.test  shouldBe (false)
    c1.list  shouldBe (true)
    c1.nice  shouldBe (false)
    c1.quiet shouldBe (None)
    
    val t1 = the [OptionParserException] thrownBy cli.parse(Seq("--te"), Config())
    t1.getMessage should startWith (AMBIGUOUS_OPTION)

    // options that start with the letter n shouldBe ambiguous with boolean args 
    // because of the --no-... variant.
    val t2 = the [OptionParserException] thrownBy cli.parse(Seq("--n"), Config())
    t2.getMessage should startWith (AMBIGUOUS_OPTION)
    
    val c2 = cli.parse(Seq("--ni"), Config())
    c2.text  shouldBe (false)
    c2.test  shouldBe (false)
    c2.list  shouldBe (false)
    c2.nice  shouldBe (true)
    c2.quiet shouldBe (None)
    
    // Should match --no-quiet
    val c3 = cli.parse(Seq("--no"), Config())
    c3.text  shouldBe (false)
    c3.test  shouldBe (false)
    c3.list  shouldBe (false)
    c3.nice  shouldBe (false)
    c3.quiet shouldBe (Some(false))
  }
  
  // ====================================================================================

  it should "handle boolean switches" in {
    case class Config(timestamp: Boolean = false)
    val cli = new OptionParser[Config] {
      bool("-t", "--timestamp") { (v, cfg) => cfg.copy(timestamp = v) }
      arg[String] { (_, _) => throw new Exception("arg() should not be called!") }
    }
    
    val c1 = cli.parse(Seq("-t"), Config())
    c1.timestamp shouldBe (true)

    val c2 = cli.parse(Seq("--timestamp"), Config())
    c2.timestamp shouldBe (true)

    val c3 = cli.parse(Seq("--no-timestamp"), Config())
    c3.timestamp shouldBe (false)
  }
  
  // ====================================================================================

  it should "handle switches where the argument is a comma separated list" in {
    case class Config(names: List[String] = Nil, sizes: List[Int] = Nil)
    val cli = new OptionParser[Config] {
      list[String]("-n", "--names") { (names, cfg) => cfg.copy(names = names) }
      list[Int]("-s", "--sizes")    { (sizes, cfg) => cfg.copy(sizes = sizes) }
      arg[String] { (_, _) => throw new Exception("arg() should not be called!") }
    }
    
    val c1 = cli.parse(Seq("-nlarry,moe,curly"), Config())
    c1.names shouldBe (List("larry", "moe", "curly"))
    c1.sizes shouldBe (Nil)

    val c2 = cli.parse(Seq("-n", "larry,moe,curly"), Config())
    c2.names shouldBe (List("larry", "moe", "curly"))
    c2.sizes shouldBe (Nil)

    val c3 = cli.parse(Seq("--names=larry,moe,curly"), Config())
    c3.names shouldBe (List("larry", "moe", "curly"))
    c3.sizes shouldBe (Nil)

    val c4 = cli.parse(Seq("--names", "larry,moe,curly"), Config())
    c4.names shouldBe (List("larry", "moe", "curly"))
    c4.sizes shouldBe (Nil)

    // Should work for other known types
    val c5 = cli.parse(Seq("--sizes", "36,24,36"), Config())
    c5.names shouldBe (Nil)
    c5.sizes shouldBe (List(36,24,36))
  }

  // ====================================================================================
  
  it should "reject needless arguments for long switches that don't take them" in {
    case class Config(expert: Boolean = false, timestamp: Boolean = false)
    val cli = new OptionParser[Config] {
      flag("-x", "--expert") { (cfg) => cfg.copy(expert = true) }
      bool("-t", "--timestamp") { (v, cfg) => cfg.copy(timestamp = v) }
      arg[String] { (_, _) => throw new Exception("arg() should not be called!") }
    }

    val t1 = the [OptionParserException] thrownBy cli.parse(Seq("--expert=yes"), Config())
    t1.getMessage should startWith (NEEDLESS_ARGUMENT)

    val t2 = the [OptionParserException] thrownBy cli.parse(Seq("--timestamp=yes"), Config())
    t2.getMessage should startWith (NEEDLESS_ARGUMENT)
  }

  // ====================================================================================

  it should "handle short switches with required arguments" in {
    case class Config(
      name: Option[String] = None,
      expert: Boolean = false,
      text: Boolean = false,
      args: Vector[String] = Vector.empty)
    val cli = new OptionParser[Config] {
      reqd[String]("-n NAME", "") { (name, cfg) => cfg.copy(name = Some(name)) }
      flag("-x", "") { (cfg) => cfg.copy(expert = true) }
      flag("-t", "") { (cfg) => cfg.copy(text = true) }
      arg[String] { (arg, cfg) => cfg.copy(args = cfg.args :+ arg) }
    }
    
    val c1 = cli.parse(Seq("-n", "curt", "foo"), Config())
    c1.args shouldBe (Vector("foo"))
    c1.name shouldBe (Some("curt"))

    val c2 = cli.parse(Seq("-ncurt", "foo"), Config())
    c2.args shouldBe (Vector("foo"))
    c2.name shouldBe (Some("curt"))

    val c3 = cli.parse(Seq("-xtn", "curt", "foo"), Config())
    c3.args shouldBe (Vector("foo"))
    c3.name shouldBe (Some("curt"))
    c3.expert shouldBe (true)
    c3.text shouldBe (true)
    
    val c4 = cli.parse(Seq("-xtncurt", "foo"), Config())
    c4.args shouldBe (Vector("foo"))
    c4.name shouldBe (Some("curt"))
    c4.expert shouldBe (true)
    c4.text shouldBe (true)
    
    val t1 = the [OptionParserException] thrownBy cli.parse(Seq("-n"), Config())
    t1.getMessage should startWith (ARGUMENT_MISSING)
    
    val t2 = the [OptionParserException] thrownBy cli.parse(Seq("-xtn"), Config())
    t2.getMessage should startWith (ARGUMENT_MISSING)
  }

  // ====================================================================================

  it should "allow short switches without argument and int() switches to be combined" in {
    case class Config(
      name: Option[String] = None,
      expert: Boolean = false,
      text: Boolean = false,
      intValue: Option[Int] = None,
      args: Vector[String] = Vector.empty)
      
    val cli = new OptionParser[Config] {
      reqd[String]("-n NAME", "") { (name, cfg) => cfg.copy(name = Some(name)) }
      int("-<number") { (v, cfg) => cfg.copy(intValue = Some(v)) }
      flag("-x", "") { (cfg) => cfg.copy(expert = true) }
      flag("-t", "") { (cfg) => cfg.copy(text = true) }
      arg[String] { (arg, cfg) => cfg.copy(args = cfg.args :+ arg) }
    }
    
    val c1 = cli.parse(Seq("-10n", "curt", "foo"), Config())
    c1.args shouldBe (Vector("foo"))
    c1.name shouldBe (Some("curt"))
    c1.intValue shouldBe (Some(10))

    val c2 = cli.parse(Seq("-x10", "-ncurt", "foo"), Config())
    c2.args shouldBe (Vector("foo"))
    c2.name shouldBe (Some("curt"))
    c2.expert shouldBe (true)
    c2.intValue shouldBe (Some(10))

    val c3 = cli.parse(Seq("-x10tn", "curt", "foo"), Config())
    c3.args shouldBe (Vector("foo"))
    c3.name shouldBe (Some("curt"))
    c3.expert shouldBe (true)
    c3.text shouldBe (true)
    c3.intValue shouldBe (Some(10))
    
    val c4 = cli.parse(Seq("-xtncurt", "foo"), Config())
    c4.args shouldBe (Vector("foo"))
    c4.name shouldBe (Some("curt"))
    c4.expert shouldBe (true)
    c4.text shouldBe (true)
    c4.intValue shouldBe (None)
  }

  // ====================================================================================

  it should "handle long switches with required arguments" in {
    case class Config(name: Option[String] = None, args: Vector[String] = Vector.empty)
    val cli = new OptionParser[Config] {
      reqd[String]("", "--name NAME") { (name, cfg) => cfg.copy(name = Some(name)) }
      arg[String] { (arg, cfg) => cfg.copy(args = cfg.args :+ arg) }
    }
    
    val c1 = cli.parse(Seq("--name", "curt", "foo"), Config())
    c1.name shouldBe Some("curt")
    c1.args shouldBe Vector("foo")

    val c2 = cli.parse(Seq("--name=curt", "foo"), Config())
    c2.name shouldBe Some("curt")
    c2.args shouldBe Vector("foo")
    
    val t1 = the [OptionParserException] thrownBy cli.parse(Seq("--name"), Config())
    t1.getMessage should startWith (ARGUMENT_MISSING)
  }

  // ====================================================================================

  it should "handle switches with String and File arguments" in {
    import java.io.File
    case class Config(string: Option[String] = None, file: Option[File] = None, args: Vector[String] = Vector.empty)
    val cli = new OptionParser[Config] {
      reqd[String]("-s", "--string ARG") { (s, cfg) => cfg.copy(string = Some(s)) }
      reqd[File]  ("-f", "--file FILE")  { (f, cfg) => cfg.copy(file = Some(f)) }
      arg[String] { (arg, cfg) => cfg.copy(args = cfg.args :+ arg) }
    }

    val c1 = cli.parse(Seq("-s", "hello", "foo", "-f", "/etc/passwd"), Config())
    c1.args shouldBe Vector("foo")
    c1.string shouldBe Some("hello")
    c1.file shouldBe Some(new File("/etc/passwd"))

    val c2 = cli.parse(Seq("-f/etc/passwd"), Config())
    c2.args shouldBe (Vector.empty)
    c2.string shouldBe None
    c2.file shouldBe Some(new File("/etc/passwd"))
  }

  // ====================================================================================

  it should "handle switches with Char arguments" in {
    case class Config(char: Char = 0.toChar)
    val cli = new OptionParser[Config] {
      reqd[Char]("-c", "--char ARG")   { (c, cfg) => cfg.copy(char = c) }
      arg[String] { (_, _) => throw new Exception("arg() should not be called!") }
    }
    
    // Single char
    val c1 = cli.parse(Seq("-c", "%"), Config())
    c1.char shouldBe ('%')

    // Control characters
    val c2 = cli.parse(Seq("-c", "\\b"), Config())
    c2.char shouldBe (8.toChar)

    val c3 = cli.parse(Seq("-c", "\\t"), Config())
    c3.char shouldBe (9.toChar)

    val c4 = cli.parse(Seq("-c", "\\n"), Config())
    c4.char shouldBe (10.toChar)

    val c5 = cli.parse(Seq("-c", "\\f"), Config())
    c5.char shouldBe (12.toChar)

    val c6 = cli.parse(Seq("-c", "\\r"), Config())
    c6.char shouldBe (13.toChar)

    // Octal codes
    val c7 = cli.parse(Seq("-c", "\\033"), Config())
    c7.char shouldBe (27.toChar)

    val c8 = cli.parse(Seq("-c", "\\33"), Config())
    c8.char shouldBe (27.toChar)

    val c9 = cli.parse(Seq("-c", "\\377"), Config())
    c9.char shouldBe (255.toChar)

    // Hex codes
    val c10 = cli.parse(Seq("-c", "\\xA"), Config())
    c10.char shouldBe ('\n')

    val c11 = cli.parse(Seq("-c", "\\X0A"), Config())
    c11.char shouldBe ('\n')

    val c12 = cli.parse(Seq("-c", "é"), Config())
    c12.char shouldBe ('é')

    val c13 = cli.parse(Seq("-c", "\\xe9"), Config())
    c13.char shouldBe ('é')

    val c14 = cli.parse(Seq("-c", "\\u00e9"), Config())
    c14.char shouldBe ('é')

    // Unicode
    val c15 = cli.parse(Seq("-c", "\\u001B"), Config())
    c15.char shouldBe (27.toChar)

    val c16 = cli.parse(Seq("-c", "\\u001b"), Config())
    c16.char shouldBe (27.toChar)
    
    val t1 = the [OptionParserException] thrownBy cli.parse(Seq("-c", "abc"), Config())
    t1.getMessage should startWith (INVALID_ARGUMENT)
    
    val t2 = the [OptionParserException] thrownBy cli.parse(Seq("-c", "\\g"), Config())
    t2.getMessage should startWith (INVALID_ARGUMENT)

    // Octal must be 1 two or three digits
    val t3 = the [OptionParserException] thrownBy cli.parse(Seq("-c", "\\01777"), Config())
    t3.getMessage should startWith (INVALID_ARGUMENT)

    // Octal, if three digits first cannot be greater then 3
    val t4 = the [OptionParserException] thrownBy cli.parse(Seq("-c", "\\777"), Config())
    t4.getMessage should startWith (INVALID_ARGUMENT)

    // Hex, cannot be more than 2 chars
    val t5 = the [OptionParserException] thrownBy cli.parse(Seq("-c", "\\x1FF"), Config())
    t5.getMessage should startWith (INVALID_ARGUMENT)

    // Unicode must be four hex digits
    val t6 = the [OptionParserException] thrownBy cli.parse(Seq("-c", "\\uFF"), Config())
    t6.getMessage should startWith (INVALID_ARGUMENT)
  }
  
  // ====================================================================================

  it should "handle switches with numeric argument types" in {
    case class Config(int: Int = -1, short: Short = -1, long: Long = -1L, float: Float = -1.0F, double: Double = -1.0)
    val cli = new OptionParser[Config] {
      reqd[Int]   ("-i", "--int ARG")    { (v, cfg) => cfg.copy(int    = v) }
      reqd[Short] ("-h", "--short ARG")  { (v, cfg) => cfg.copy(short  = v) }
      reqd[Long]  ("-l", "--long ARG")   { (v, cfg) => cfg.copy(long   = v) }
      reqd[Float] ("-f", "--float ARG")  { (v, cfg) => cfg.copy(float  = v) }
      reqd[Double]("-d", "--double ARG") { (v, cfg) => cfg.copy(double = v) }
      arg[String] { (_, _) => throw new Exception("arg() should not be called!") }
    }


    val c1 = cli.parse(Seq("-i", "2147483647", "-h", "32767", "-l", "9223372036854775807", "-f", "3.4028235E38", "-d", "1.7976931348623157E308"), Config())
    c1.int    shouldBe (2147483647)
    c1.short  shouldBe (32767.asInstanceOf[Short])
    c1.long   shouldBe (9223372036854775807L)
    c1.float  shouldBe (3.4028235E38.asInstanceOf[Float])
    c1.double shouldBe (1.7976931348623157E308)

    // Negatives
    val c2 = cli.parse(Seq("-i-2147483648", "-h-32768", "-l-9223372036854775808", "-f-3.4028235E38", "-d-1.7976931348623157E308"), Config())
    c2.int    shouldBe (-2147483648)
    c2.short  shouldBe ((-32768).asInstanceOf[Short])
    c2.long   shouldBe (-9223372036854775808L)
    c2.float  shouldBe (-3.4028235E38.asInstanceOf[Float])
    c2.double shouldBe (-1.7976931348623157E308)
    
    // Hex values
    val c3 = cli.parse(Seq("-i", "0xFFFF", "-h", "0XFFF", "-l", "0x12345FFFFF"), Config())
    c3.int   shouldBe (0xFFFF)
    c3.short shouldBe (0xFFF.asInstanceOf[Short])
    c3.long  shouldBe (0x12345FFFFFL)

    // Negative Hex values
    val c4 = cli.parse(Seq("-i", "-0xFFFF", "-h", "-0XFFF", "-l", "-0x12345FFFFF"), Config())
    c4.int   shouldBe (-0xFFFF)
    c4.short shouldBe ((-0xFFF).asInstanceOf[Short])
    c4.long  shouldBe (-0x12345FFFFFL)

    // Octal values
    val c5 = cli.parse(Seq("-i", "01777", "-h", "035", "-l", "024777444"), Config())
    c5.int   shouldBe (1023)
    c5.short shouldBe (29.asInstanceOf[Short])
    c5.long  shouldBe (5504804L)

    // Negative Octal values
    val c6 = cli.parse(Seq("-i", "-01777", "-h", "-035", "-l", "-024777444"), Config())
    c6.int   shouldBe (-1023)
    c6.short shouldBe ((-29).asInstanceOf[Short])
    c6.long  shouldBe (-5504804L)

    // Invalid values
    
    val t1 = the [OptionParserException] thrownBy cli.parse(Seq("-i", "3.14"), Config())
    t1.getMessage should startWith (INVALID_ARGUMENT)
    val t2 = the [OptionParserException] thrownBy cli.parse(Seq("-h", "abc"), Config())
    t2.getMessage should startWith (INVALID_ARGUMENT)
    val t3 = the [OptionParserException] thrownBy cli.parse(Seq("-l", "abc"), Config())
    t3.getMessage should startWith (INVALID_ARGUMENT)
    val t4 = the [OptionParserException] thrownBy cli.parse(Seq("-f", "abc"), Config())
    t4.getMessage should startWith (INVALID_ARGUMENT)
    val t5 = the [OptionParserException] thrownBy cli.parse(Seq("-d", "abc"), Config())
    t5.getMessage should startWith (INVALID_ARGUMENT)
    
    // Values out of range
    val t6 = the [OptionParserException] thrownBy cli.parse(Seq("-i", "9223372036854775807"), Config())
    t6.getMessage should startWith (INVALID_ARGUMENT)
    val t7 = the [OptionParserException] thrownBy cli.parse(Seq("-h", "9223372036854775807"), Config())
    t7.getMessage should startWith (INVALID_ARGUMENT)
    val t8 = the [OptionParserException] thrownBy cli.parse(Seq("-l", "92233720368547758078"), Config())
    t8.getMessage should startWith (INVALID_ARGUMENT)
    val t9 = the [OptionParserException] thrownBy cli.parse(Seq("-f", "1.7976931348623157E308"), Config())
    t9.getMessage should startWith (INVALID_ARGUMENT)
    val t10 = the [OptionParserException] thrownBy cli.parse(Seq("-d", "1.7976931348623157E309"), Config())
    t10.getMessage should startWith (INVALID_ARGUMENT)

    val t11 = the [OptionParserException] thrownBy cli.parse(Seq("-i", "0x1FFFFFFFF"), Config())
    t11.getMessage should startWith (INVALID_ARGUMENT)
    val t12 = the [OptionParserException] thrownBy cli.parse(Seq("-h", "0x1FFFFFFFF"), Config())
    t12.getMessage should startWith (INVALID_ARGUMENT)
    val t13 = the [OptionParserException] thrownBy cli.parse(Seq("-l", "0x1FFFFFFFFFFFFFFFF"), Config())
    t13.getMessage should startWith (INVALID_ARGUMENT)
  }
  
  // ====================================================================================
  
  it should "terminate switch processing with --" in {
    case class Config(incr: Int = 1, args: Vector[String] = Vector.empty)
    val cli = new OptionParser[Config] {
      reqd[Int]("-i", "--incr ARG")    { (v, cfg) => cfg.copy(incr = v) }
      arg[String] { (arg, cfg) => cfg.copy(args = cfg.args :+ arg) }
    }

    val c1 = cli.parse(Seq("-i", "9", "foo", "--", "-i", "10"), Config())
    c1.incr shouldBe (9)
    c1.args shouldBe Vector("foo", "-i", "10")
    
    val c2 = cli.parse(Seq("--incr", "9", "foo", "--", "--incr", "10"), Config())
    c2.incr shouldBe (9)
    c2.args shouldBe Vector("foo", "--incr", "10")
  }
  
  // ====================================================================================
  
  it should "allow '-' as an argument (ie. stdin)" in {
    case class Config(file: String = "", args: Vector[String] = Vector.empty)
    val cli = new OptionParser[Config] {
      reqd[String]("-f", "--file ARG")    { (v, cfg) => cfg.copy(file = v) }
      arg[String] { (arg, cfg) => cfg.copy(args = cfg.args :+ arg) }
    }

    val c1 = cli.parse(Seq("-"), Config())
    c1.file shouldBe ("")
    c1.args shouldBe Vector("-")
    
    val c2 = cli.parse(Seq("-f", "-"), Config())
    c2.file shouldBe ("-")
    c2.args shouldBe (Vector.empty)
    
    val c3 = cli.parse(Seq("-f-"), Config())
    c3.file shouldBe ("-")
    c3.args shouldBe (Vector.empty)

    val c4 = cli.parse(Seq("--file", "-"), Config())
    c4.file shouldBe ("-")
    c4.args shouldBe (Vector.empty)

    val c5 = cli.parse(Seq("--file=-"), Config())
    c5.file shouldBe ("-")
    c5.args shouldBe (Vector.empty)
  }
  
  // ====================================================================================
  
  it should "handle short switches with optional arguments" in {
    case class Config(expert: Boolean = false, dir: Option[String] = None, args: Vector[String] = Vector.empty)
    val cli = new OptionParser[Config] {
      flag("-x", "")               { (cfg) => cfg.copy(expert = true) }
      optl[String]("-d [VAL]", "") { (dir, cfg) => cfg.copy(dir = dir orElse Some("/tmp")) }
      arg[String] { (arg, cfg) => cfg.copy(args = cfg.args :+ arg) }
    }

    val c1 = cli.parse(Seq("-d", "/etc", "foo"), Config())
    c1.expert shouldBe (false)
    c1.dir    shouldBe Some("/etc")
    c1.args   shouldBe Vector("foo")

    val c2 = cli.parse(Seq("-d/etc", "foo"), Config())
    c2.expert shouldBe (false)
    c2.dir    shouldBe Some("/etc")
    c2.args   shouldBe Vector("foo")
    
    val c3 = cli.parse(Seq("-xd", "/etc", "foo"), Config())
    c3.expert shouldBe (true)
    c3.dir    shouldBe Some("/etc")
    c3.args   shouldBe Vector("foo")

    val c4 = cli.parse(Seq("-xd/etc", "foo"), Config())
    c4.expert shouldBe (true)
    c4.dir    shouldBe Some("/etc")
    c4.args   shouldBe Vector("foo")

    // -- marks end of options (it would be eaten if the switch REQUIRED and argument!)
    val c5 = cli.parse(Seq("-xd", "--", "foo"), Config())
    c5.expert shouldBe (true)
    c5.dir    shouldBe Some("/tmp")  // <<--- Default value
    c5.args   shouldBe Vector("foo")

    // Optional args should not eat an arg that looks like a switch
    val c6 = cli.parse(Seq("-d", "-x", "foo"), Config())
    c6.expert shouldBe (true)
    c6.dir    shouldBe Some("/tmp")  // <<--- Default value
    c6.args   shouldBe Vector("foo")

    // Optional args should use '-' as an argument.
    val c7 = cli.parse(Seq("-d", "-", "foo"), Config())
    c7.expert shouldBe (false)
    c7.dir    shouldBe Some("-")
    c7.args   shouldBe Vector("foo")

    val c8 = cli.parse(Seq("-d"), Config())
    c8.expert shouldBe (false)
    c8.dir    shouldBe Some("/tmp")  // <<--- Default value
    c8.args   shouldBe (Vector.empty)
  }

  // ====================================================================================

  it should "handle long switches with optional arguments" in {
    case class Config(expert: Boolean = false, at: Option[String] = None, args: Vector[String] = Vector.empty)
    val cli = new OptionParser[Config] {
      flag("-x",  "--expert")  { (cfg) => cfg.copy(expert = true) }
      optl[String]("", "--at [AT]") { (at, cfg) => cfg.copy(at = at orElse(Some("00:00"))) }
      arg[String] { (arg, cfg) => cfg.copy(args = cfg.args :+ arg) }
    }

    val c1 = cli.parse(Seq("--at", "1:30", "foo"), Config())
    c1.expert shouldBe (false)
    c1.at     shouldBe Some("1:30")
    c1.args   shouldBe Vector("foo")

    val c2 = cli.parse(Seq("--at=1:30", "foo"), Config())
    c2.expert shouldBe (false)
    c2.at     shouldBe Some("1:30")
    c2.args   shouldBe Vector("foo")

    // Optional args shouldn't eat an option name looking arg
    val c3 = cli.parse(Seq("--at", "-x", "foo"), Config())
    c3.expert shouldBe (true)
    c3.at     shouldBe Some("00:00")
    c3.args   shouldBe Vector("foo")
        
    // Optional args should eat an option name looking arg if it is attached
    val c4 = cli.parse(Seq("--at=-x", "foo"), Config())
    c4.expert shouldBe (false)
    c4.at     shouldBe Some("-x")
    c4.args   shouldBe Vector("foo")

    val c5 = cli.parse(Seq("--at", "--", "foo"), Config())
    c5.expert shouldBe (false)
    c5.at     shouldBe Some("00:00")
    c5.args   shouldBe Vector("foo")

    val c6 = cli.parse(Seq("--at"), Config())
    c6.expert shouldBe (false)
    c6.at     shouldBe Some("00:00")
    c6.args   shouldBe (Vector.empty)
  }
  
  // ====================================================================================
  
  it should "interpret --name= to have a empty string argument" in {
    case class Config(name: Option[String] = None)
    val cli = new OptionParser[Config] {
      reqd[String]("-n",  "--name NAME")  { (s, cfg) => cfg.copy(name = Some(s)) }
      arg[String] { (_, _) => throw new Exception("arg() should not be called!") }
    }

    val c1 = cli.parse(Seq("--name="), Config())
    c1.name shouldBe Some("")
  }
  
  // ====================================================================================
  
  it should "interpret following args that begin with a '-' correctly (short)" in {
    case class Config(expert: Boolean = false, gist: Option[String] = None, jazz: Option[String] = None)
    val cli = new OptionParser[Config] {
      flag        ("-x",  "--expert")  { (cfg) => cfg.copy(expert = true) }
      reqd[String]("-g", "--gist TEXT", "Set Gist") { (gist, cfg) => cfg.copy(gist = Some(gist)) }
      optl[String]("-j", "--jazz [TEXT]", "Set Jazz") { (jazz, cfg) => cfg.copy(jazz = jazz orElse Some("bebop")) }
      arg[String] { (_, _) => throw new Exception("arg() should not be called!") }
    }

    // Required argument
    val c1 = cli.parse(Seq("-g-x"), Config())
    c1.expert shouldBe (false)
    c1.gist   shouldBe Some("-x")
    c1.jazz   shouldBe None
    
    val c2 = cli.parse(Seq("-g", "-x"), Config()) // Required args are greedy!
    c2.expert shouldBe (false)
    c2.gist   shouldBe Some("-x")
    c2.jazz   shouldBe None

    // Optional argument
    val c3 = cli.parse(Seq("-j-x"), Config()) // Attached, so should use as arg.
    c3.expert shouldBe (false)
    c3.gist   shouldBe None
    c3.jazz   shouldBe Some("-x")

    val c4 = cli.parse(Seq("-j", "-x"), Config()) // Optional arg, should not pick up the -x
    c4.expert shouldBe (true)
    c4.gist   shouldBe None
    c4.jazz   shouldBe Some("bebop")
  }

  // ====================================================================================

  it should "interpret following args that begin with a '-' correctly (long)" in {
    case class Config(expert: Boolean = false, gist: Option[String] = None, jazz: Option[String] = None)
    val cli = new OptionParser[Config] {
      flag        ("-x",  "--expert")  { (cfg) => cfg.copy(expert = true) }
      reqd[String]("-g", "--gist TEXT", "Set Gist") { (gist, cfg) => cfg.copy(gist = Some(gist)) }
      optl[String]("-j", "--jazz [TEXT]", "Set Jazz") { (jazz, cfg) => cfg.copy(jazz = jazz orElse Some("bebop")) }
      arg[String] { (_, _) => throw new Exception("arg() should not be called!") }
    }

    // Required argument
    val c1 = cli.parse(Seq("--gist=-x"), Config())
    c1.expert shouldBe (false)
    c1.gist   shouldBe Some("-x")
    c1.jazz   shouldBe None


    val c2 = cli.parse(Seq("--gist", "-x"), Config()) // Required args are greedy!
    c2.expert shouldBe (false)
    c2.gist   shouldBe Some("-x")
    c2.jazz   shouldBe None

    // Optional argument
    val c3 = cli.parse(Seq("--jazz=-x"), Config()) // Attached, so should use as arg.
    c3.expert shouldBe (false)
    c3.gist   shouldBe None
    c3.jazz   shouldBe Some("-x")

    val c4 = cli.parse(Seq("--jazz", "-x"), Config()) // Optional arg, should not pick up the -x
    c4.expert shouldBe (true)
    c4.gist   shouldBe None
    c4.jazz   shouldBe Some("bebop")
  }
  
  // ====================================================================================
  it should "handle custom argument parsers" in {
    case class Foo(s: String)
    case class Config(foo1: Option[Foo] = None, foo2: Option[Foo] = None)
    val cli = new OptionParser[Config] {
      addArgumentParser[Foo] { s: String => Foo(s) }
      reqd[Foo]("", "--foo1") { (foo, cfg) => cfg.copy(foo1 = Some(foo)) }
      optl[Foo]("", "--foo2") { (foo, cfg) => cfg.copy(foo2 = foo orElse Some(Foo("Default"))) }
      arg[String] { (_, _) => throw new Exception("arg() should not be called!") }
    }

    val c1 = cli.parse(Seq("--foo1", "46"), Config())
    c1.foo1 shouldBe Some(Foo("46"))
    c1.foo2 shouldBe None

    val c2 = cli.parse(Seq("--foo2", "46"), Config())
    c2.foo1 shouldBe None
    c2.foo2 shouldBe Some(Foo("46"))

    val c3 = cli.parse(Seq("--foo2"), Config())
    c3.foo1 shouldBe None
    c3.foo2 shouldBe Some(Foo("Default"))
  }

  // ====================================================================================
  
  it should "handle switches with a required value limited by a List of values" in {
    case class Config(theType: Option[String] = None)
    val cli = new OptionParser[Config] {
      reqd[String]("-t", "--type (binary, ascii)", List("binary", "ascii", "auto")) { (t, cfg) => cfg.copy(theType = Some(t)) }
      arg[String] { (_, _) => throw new Exception("arg() should not be called!") }
    }

    val c1 = cli.parse(Seq("-t", "binary"), Config())
    c1.theType shouldBe Some("binary")

    // Matching of argument prefix
    val c2 = cli.parse(Seq("--type", "as"), Config())
    c2.theType shouldBe Some("ascii")
  
    val t1 = the [OptionParserException] thrownBy cli.parse(Seq("--type=a"), Config())
    t1.getMessage should startWith (AMBIGUOUS_ARGUMENT)
    
    val t2 = the [OptionParserException] thrownBy cli.parse(Seq("--type=ebcdic"), Config())
    t2.getMessage should startWith (INVALID_ARGUMENT)
  }

  // ====================================================================================
  
  it should "handle switches with an optional value limmited by a Map of values" in {
    case class Config(zone: Int = 0)
    val cli = new OptionParser[Config] {
      optl[Int]("-z", "--zone", Map("one" -> 1, "two" -> 2, "three" -> 3)) { (z, cfg) => cfg.copy(zone = z getOrElse 1) }
      arg[String] { (_, _) => throw new Exception("arg() should not be called!") }
    }

    val c1 = cli.parse(Seq("-z"), Config())
    c1.zone shouldBe (1)

    val t1 = the [OptionParserException] thrownBy cli.parse(Seq("-z", "four"), Config())
    t1.getMessage should startWith (INVALID_ARGUMENT)

    val c2 = cli.parse(Seq("-z", "tw"), Config())
    c2.zone shouldBe (2)

    val t2 = the [OptionParserException] thrownBy cli.parse(Seq("-ztx"), Config())
    t2.getMessage should startWith (INVALID_ARGUMENT)
  
    val t3 = the [OptionParserException] thrownBy cli.parse(Seq("--zone", "t"), Config())
    t3.getMessage should startWith (AMBIGUOUS_ARGUMENT)
  }
  
  // ====================================================================================
  // ================== Help Info=== ====================================================
  // ====================================================================================

  "Help Info" should "include a -h, --help option by default" in {
    case class Config()
    val cli = new OptionParser[Config]
    cli.help shouldBe ("    -h, --help                       Show this message")
  }    
  
  // ====================================================================================

  it should "allow auto_help to be disabled" in {
    case class Config()
    val cli = new OptionParser[Config]
    cli.auto_help = false
    cli.help shouldBe ("")
  }
  
  // ====================================================================================

  it should "work for switches without args." in {
    case class Config()
    val cli = new OptionParser[Config] {
      auto_help = false
      flag("-a", "")(identity)
      flag("-b", "", "Help for b")(identity)
      flag("-c", "", "Line 1 for c", "Line 2 for c")(identity)
      flag("", "--dd")(identity)
      flag("", "--eee", "Help for eee")(identity)
      flag("", "--ffff", "Line 1 for ffff", "Line 2 for ffff")(identity)
      flag("-g", "--ggggg")(identity)
      flag("-h", "--hhhhhh", "Help for hhhhhh")(identity)
      flag("-i", "--iiiiiii", "Line 1 for iiiiiii", "Line 2 for iiiiiii")(identity)
    }

    cli.help shouldBe (
    """    -a
      |    -b                               Help for b
      |    -c                               Line 1 for c
      |                                     Line 2 for c
      |        --dd
      |        --eee                        Help for eee
      |        --ffff                       Line 1 for ffff
      |                                     Line 2 for ffff
      |    -g, --ggggg
      |    -h, --hhhhhh                     Help for hhhhhh
      |    -i, --iiiiiii                    Line 1 for iiiiiii
      |                                     Line 2 for iiiiiii""".stripMargin('|')
    )
  }

  // ====================================================================================
  
  it should "work for boolean switches." in {
    case class Config()
    val cli = new OptionParser[Config] {
      auto_help = false
      bool("-a", "") { (_, c) => c }
      bool("-b", "", "Help for b") { (_, c) => c }
      bool("-c", "", "Line 1 for c", "Line 2 for c") { (_, c) => c }
      bool("", "--dd") { (_, c) => c }
      bool("", "--eee", "Help for eee") { (_, c) => c }
      bool("", "--ffff", "Line 1 for ffff", "Line 2 for ffff") { (_, c) => c }
      bool("-g", "--ggggg") { (_, c) => c }
      bool("-h", "--hhhhhh", "Help for hhhhhh") { (_, c) => c }
      bool("-i", "--iiiiiii", "Line 1 for iiiiiii", "Line 2 for iiiiiii") { (_, c) => c }
    }

    cli.help shouldBe (
    """    -a
      |    -b                               Help for b
      |    -c                               Line 1 for c
      |                                     Line 2 for c
      |        --[no-]dd
      |        --[no-]eee                   Help for eee
      |        --[no-]ffff                  Line 1 for ffff
      |                                     Line 2 for ffff
      |    -g, --[no-]ggggg
      |    -h, --[no-]hhhhhh                Help for hhhhhh
      |    -i, --[no-]iiiiiii               Line 1 for iiiiiii
      |                                     Line 2 for iiiiiii""".stripMargin('|')
    )
  }
  
  // ====================================================================================

  it should "work for switches with required args." in {
    case class Config()
    val cli = new OptionParser[Config] {
      auto_help = false
      reqd[Int]("-a ARG", "") { (_, c) => c }
      reqd[Int]("-b VAL", "", "Help for b") { (_, c) => c }
      reqd[Int]("-c VALUE", "", "Line 1 for c", "Line 2 for c") { (_, c) => c }
      reqd[Int]("", "--dd ARG") { (_, c) => c }
      reqd[Int]("", "--eee VAL", "Help for eee") { (_, c) => c }
      reqd[Int]("", "--ffff VALUE", "Line 1 for ffff", "Line 2 for ffff") { (_, c) => c }
      reqd[Int]("-g", "--ggggg=ARG") { (_, c) => c }
      reqd[Int]("-h", "--hhhhhh=VAL", "Help for hhhhhh") { (_, c) => c }
      reqd[Int]("-i", "--iiiiiii=VALUE", "Line 1 for iiiiiii", "Line 2 for iiiiiii") { (_, c) => c }
    }
  
    cli.help shouldBe (
    """    -a ARG
      |    -b VAL                           Help for b
      |    -c VALUE                         Line 1 for c
      |                                     Line 2 for c
      |        --dd ARG
      |        --eee VAL                    Help for eee
      |        --ffff VALUE                 Line 1 for ffff
      |                                     Line 2 for ffff
      |    -g, --ggggg=ARG
      |    -h, --hhhhhh=VAL                 Help for hhhhhh
      |    -i, --iiiiiii=VALUE              Line 1 for iiiiiii
      |                                     Line 2 for iiiiiii""".stripMargin('|')
    )
  }
  
  // ====================================================================================

  it should "work for switches with optional args." in {
    case class Config()
    val cli = new OptionParser[Config] {
      auto_help = false
      optl[Int]("-a [ARG]", "") { (_, c) => c }
      optl[Int]("-b [VAL]", "", "Help for b") { (_, c) => c }
      optl[Int]("-c [VALUE]", "", "Line 1 for c", "Line 2 for c") { (_, c) => c }
      optl[Int]("", "--dd [ARG]") { (_, c) => c }
      optl[Int]("", "--eee [VAL]", "Help for eee") { (_, c) => c }
      optl[Int]("", "--ffff [VALUE]", "Line 1 for ffff", "Line 2 for ffff") { (_, c) => c }
      optl[Int]("-g", "--ggggg=[ARG]") { (_, c) => c }
      optl[Int]("-h", "--hhhhhh=[VAL]", "Help for hhhhhh") { (_, c) => c }
      optl[Int]("-i", "--iiiiiii=[VALUE]", "Line 1 for iiiiiii", "Line 2 for iiiiiii") { (_, c) => c }
      optl[Int]("-j", "--jjjjj[=ARG]") { (_, c) => c }
      optl[Int]("-k", "--kkkkkk[=VAL]", "Help for kkkkkk") { (_, c) => c }
      optl[Int]("-l", "--lllllll[=VALUE]", "Line 1 for lllllll", "Line 2 for lllllll") { (_, c) => c }
    }
  
    cli.help shouldBe (
    """    -a [ARG]
      |    -b [VAL]                         Help for b
      |    -c [VALUE]                       Line 1 for c
      |                                     Line 2 for c
      |        --dd [ARG]
      |        --eee [VAL]                  Help for eee
      |        --ffff [VALUE]               Line 1 for ffff
      |                                     Line 2 for ffff
      |    -g, --ggggg=[ARG]
      |    -h, --hhhhhh=[VAL]               Help for hhhhhh
      |    -i, --iiiiiii=[VALUE]            Line 1 for iiiiiii
      |                                     Line 2 for iiiiiii
      |    -j, --jjjjj[=ARG]
      |    -k, --kkkkkk[=VAL]               Help for kkkkkk
      |    -l, --lllllll[=VALUE]            Line 1 for lllllll
      |                                     Line 2 for lllllll""".stripMargin('|')
    )
  }

  // ====================================================================================

  it should "use the ARG info from the long name if it has been specified for both names." in {
    case class Config()
    val cli = new OptionParser[Config] {
      auto_help = false
      reqd[Int]("-a ARG", "--apple VALUE") { (_, c) => c }
      optl[Int]("-b [ARG]", "--box [VALUE]") { (_, c) => c }
    }
    
    cli.help shouldBe (
    """    -a, --apple VALUE
      |    -b, --box [VALUE]""".stripMargin('|')
    )
  }
  
  // ====================================================================================
  
  it should "support the banner and separators." in {
    import java.io.File
    case class Config()
    val cli = new OptionParser[Config] {
      banner = "testapp [options]"
      separator("")
      flag("-a", "--apple")(identity)
      flag("-b", "--best", "Pick best option")(identity)
      flag("-c", "--cat", "Cat option", "  more cat info")(identity)
      reqd[String]("-d ARG", "", "dddd dddd") { (_, c) => c }
      separator("")
      separator("More options:")
        // Arg for long name overrides that from short!
      reqd[String]("-e EEE", "--extra ARG", "Extra stuff") { (_, c) => c }
      reqd[File]("-f", "--file=FILE", "File name") { (_, c) => c }
      reqd[String]("", "--goo=ARG", "Goo help") { (_, c) => c }
    }
    
    cli.help shouldBe (
    """testapp [options]
      |
      |    -a, --apple
      |    -b, --best                       Pick best option
      |    -c, --cat                        Cat option
      |                                       more cat info
      |    -d ARG                           dddd dddd
      |
      |More options:
      |    -e, --extra ARG                  Extra stuff
      |    -f, --file=FILE                  File name
      |        --goo=ARG                    Goo help
      |    -h, --help                       Show this message""".stripMargin('|')
    )
  }
}
