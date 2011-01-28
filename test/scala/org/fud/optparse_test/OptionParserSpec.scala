package org.fud.optparse_test

import _root_.org.scalatest.FlatSpec
import _root_.org.scalatest.matchers.ShouldMatchers

import org.fud.optparse._

class OptionParserSpec extends FlatSpec with ShouldMatchers {
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
    val cli = new OptionParser
    var thrown = evaluating {
      cli.noArg("", "  ") { () => /* Empty */ }
    } should produce [OptionParserException]
    thrown.getMessage should be === (BOTH_BLANK)
  }
  
  // ====================================================================================

  it should "fail if the short name is not specified correctly" in {
    val cli = new OptionParser
    // Missing leading dash
    var thrown = evaluating { cli.noArg("a", "") { () => /* Empty */ } } should produce [OptionParserException]
    thrown.getMessage should startWith (INVALID_SHORT_NAME)
  
    // Must be only one character
    thrown = evaluating { cli.noArg("-ab", "") { () => /* Empty */ } } should produce [OptionParserException]
    thrown.getMessage should startWith (INVALID_SHORT_NAME)
    
    evaluating { cli.noArg("-ab ARG", "") { () => /* Empty */ } } should produce [OptionParserException]
    thrown.getMessage should startWith (INVALID_SHORT_NAME)
  }
  
  // ====================================================================================

  it should "fail if the long name is not specified correctly" in {
    val cli = new OptionParser
    var thrown = evaluating { cli.noArg("", "name") { () => /* Empty */ } } should produce [OptionParserException]
    thrown.getMessage should startWith (INVALID_LONG_NAME)
    
    thrown = evaluating { cli.noArg("", "-name") { () => /* Empty */ } } should produce [OptionParserException]
    thrown.getMessage should startWith (INVALID_LONG_NAME)

    thrown = evaluating { cli.noArg("", "name ARG") { () => /* Empty */ } } should produce [OptionParserException]
    thrown.getMessage should startWith (INVALID_LONG_NAME)
    
    thrown = evaluating { cli.noArg("", "-name ARG") { () => /* Empty */ } } should produce [OptionParserException]
    thrown.getMessage should startWith (INVALID_LONG_NAME)

    thrown = evaluating { cli.noArg("", "name=ARG") { () => /* Empty */ } } should produce [OptionParserException]
    thrown.getMessage should startWith (INVALID_LONG_NAME)
    
    thrown = evaluating { cli.noArg("", "-name=ARG") { () => /* Empty */ } } should produce [OptionParserException]
    thrown.getMessage should startWith (INVALID_LONG_NAME)
    
    // --no- prefix is reserved for boolean switches
    thrown = evaluating { cli.noArg("", "--no-name") { () => /* Empty */ } } should produce [OptionParserException]
    thrown.getMessage should startWith (INVALID_LONG_NAME)
  }
  
  // ====================================================================================

  it should "accept the ARG to long switches in four different forms" in {
    val cli = new OptionParser
    cli.noArg("", "--name ARG") { () => /* Empty */ }
    cli.noArg("", "--name [ARG]") { () => /* Empty */ }
    cli.noArg("", "--name=ARG") { () => /* Empty */ }
    cli.noArg("", "--name[=ARG]") { () => /* Empty */ }
  }
  
  // ====================================================================================

  it should "pass as long as either name is specified" in {
    val cli = new OptionParser
    cli.noArg("-a", "") { () => /* Empty */ }
    cli.noArg("-b ARG", "") { () => /* Empty */ }
    cli.noArg("", "--bcd") { () => /* Empty */ }
    cli.noArg("", "--cde ARG") { () => /* Empty */ }
    cli.noArg("-c", "--cde") { () => /* Empty */ }
  }

  // ====================================================================================

  it should "remove switches with identical names when adding a new switch" in {
    val cli = new OptionParser
    var results: Map[String, Any] = Map.empty

    cli.noArg("-x", "--notUsed") { () => results += "notUsed" -> true }
    cli.noArg("-X", "--expert") { () => results += "notUsed2" -> true }
    cli.noArg("-x", "--expert") { () => results += "expert" -> true }

    var args = cli.parse(List("-x"))
    args should be ('empty)
    results should have size (1)
    results should not contain key ("notUsed")
    results should contain key "expert"
    results("expert") should be === (true)

    results = Map.empty
    args = cli.parse(List("--expert"))
    args should be ('empty)
    results should have size (1)
    results should not contain key ("notUsed2")
    results should contain key "expert"
    results("expert") should be === (true)
  }
  
  // ====================================================================================
  it should "detect attempts to add an option for a type with no argument parser" in {
    val cli = new OptionParser
    var results: Map[String, Any] = Map.empty

    class SomeClass
    evaluating {
      cli.reqArg("-z", "--zoo") { zoo : SomeClass => results += "zoo" -> zoo}
    } should produce [RuntimeException]
  }    
  

  // ====================================================================================
  // ================== Command Line Parsing ============================================
  // ====================================================================================

  "Commmand line parsing" should "handle empty argv" in {
    val cli = new OptionParser
    var results: Map[String, Any] = Map.empty
    
    cli.noArg("-x", "--expert") { () => results += "expert" -> true }
    cli.reqArg("-n", "--name NAME") { name: String => results += "name" -> name }

    val args = cli.parse(List())
    args should be ('empty)
    results should be ('empty)
  }    

  // ====================================================================================
  
  it should "handle arg list without any switches" in {
    val cli = new OptionParser
    var results: Map[String, Any] = Map.empty
    
    cli.noArg("-x", "--expert") { () => results += "expert" -> true }
    cli.reqArg("-n", "--name NAME") { name: String => results += "name" -> name }

    var args = cli.parse(List("foo"))
    args should have length (1)
    args(0) should be === "foo"
    results should be ('empty)
  
    args = cli.parse(List("foo", "bar"))
    args should have length (2)
    args(0) should be === "foo"
    args(1) should be === "bar"
    results should be ('empty)
  }    
  
  // ====================================================================================

  it should "handle switches that do not take arguments" in {
    val cli = new OptionParser
    var results: Map[String, Any] = Map.empty
    
    cli.noArg("-x", "--expert") { () => results += "expert" -> true }
    cli.boolArg("-t", "--timestamp") { v: Boolean => results += "timestamp" -> v }

    // Short
    results = Map.empty
    var args = cli.parse(List("-x"))
    args should be ('empty)
    results should have size (1)
    results should contain key "expert"
    results("expert") should be === (true)
  
    results = Map.empty
    args = cli.parse(List("-x", "-t"))
    args should be ('empty)
    results should have size (2)
    results should contain key "expert"
    results should contain key "timestamp"
    results("expert") should be === (true)
    results("timestamp") should be === (true)

    results = Map.empty
    args = cli.parse(List("-xt"))
    args should be ('empty)
    results should have size (2)
    results should contain key "expert"
    results should contain key "timestamp"
    results("expert") should be === (true)
    results("timestamp") should be === (true)

    // Long
    results = Map.empty
    args = cli.parse(List("--expert"))
    args should be ('empty)
    results should have size (1)
    results should contain key "expert"
    results("expert") should be === (true)
  
    results = Map.empty
    args = cli.parse(List("--expert", "--timestamp"))
    args should be ('empty)
    results should have size (2)
    results should contain key "expert"
    results should contain key "timestamp"
    results("expert") should be === (true)
    results("timestamp") should be === (true)

    results = Map.empty
    args = cli.parse(List("--expert", "--no-timestamp"))
    args should be ('empty)
    results should have size (2)
    results should contain key "expert"
    results should contain key "timestamp"
    results("expert") should be === (true)
    results("timestamp") should be === (false)
  }
  
  // ====================================================================================

  it should "detect invalid switches" in {
    val cli = new OptionParser
    var results: Map[String, Any] = Map.empty
    
    cli.noArg("-t", "--test") { () => results += "test" -> true }
    
    var thrown = evaluating { cli.parse(List("-x")) } should produce [OptionParserException]
    thrown.getMessage should startWith (INVALID_OPTION)
    
    thrown = evaluating { cli.parse(List("--expert")) } should produce [OptionParserException]
    thrown.getMessage should startWith (INVALID_OPTION)
  }
  
  
  // ====================================================================================

  it should "handle partial names of long switches" in {
    val cli = new OptionParser
    var results: Map[String, Any] = Map.empty
    
    cli.noArg("", "--text") { () => results += "text" -> true }
    cli.noArg("", "--test") { () => results += "test" -> true }
    cli.noArg("", "--list") { () => results += "list" -> true }
    cli.noArg("", "--nice") { () => results += "nice" -> true }
    cli.boolArg("", "--quiet") { v => results += "quiet" -> v }
    
    results = Map.empty
    var args = cli.parse(List("--l", "--tex"))
    args should be ('empty)
    results should have size (2)
    results should contain key "list"
    results should contain key "text"
    results("list") should be === (true)
    results("text") should be === (true)
    
    var thrown = evaluating { cli.parse(List("--te")) } should produce [OptionParserException]
    thrown.getMessage should startWith (AMBIGUOUS_OPTION)

    // options that start with the letter n should be ambiguous with boolean args 
    // because of the --no-... variant.
    thrown = evaluating { cli.parse(List("--n")) } should produce [OptionParserException]
    thrown.getMessage should startWith (AMBIGUOUS_OPTION)
    
    results = Map.empty
    args = cli.parse(List("--ni"))
    args should be ('empty)
    results should have size (1)
    results should contain key "nice"
    results("nice") should be === (true)
    
    results = Map.empty
    args = cli.parse(List("--no"))
    args should be ('empty)
    results should have size (1)
    results should contain key "quiet"
    results("quiet") should be === (false)
  }
  
  // ====================================================================================

  it should "handle boolean switches" in {
    val cli = new OptionParser
    var results: Map[String, Any] = Map.empty
    
    cli.boolArg("-t", "--timestamp") { v: Boolean => results += "timestamp" -> v }
    
    results = Map.empty
    var args = cli.parse(List("-t"))
    args should be ('empty)
    results should have size (1)
    results should contain key "timestamp"
    results("timestamp") should be === (true)

    results = Map.empty
    args = cli.parse(List("--timestamp"))
    args should be ('empty)
    results should have size (1)
    results should contain key "timestamp"
    results("timestamp") should be === (true)

    results = Map.empty
    args = cli.parse(List("--no-timestamp"))
    args should be ('empty)
    results should have size (1)
    results should contain key "timestamp"
    results("timestamp") should be === (false)
  }
  
  // ====================================================================================

  it should "handle switches where the argument is a comma separated list" in {
    val cli = new OptionParser
    var results: Map[String, Any] = Map.empty
    
    cli.listArg[String]("-n", "--names") { names => results += "names" -> names }
    cli.listArg[Int]("-s", "--sizes")    { sizes => results += "sizes" -> sizes }
    
    results = Map.empty
    var args = cli.parse(List("-nlarry,moe,curly"))
    args should be ('empty)
    results should have size (1)
    results should contain key "names"
    results("names") should be === (List("larry", "moe", "curly"))

    results = Map.empty
    args = cli.parse(List("-n", "larry,moe,curly"))
    args should be ('empty)
    results should have size (1)
    results should contain key "names"
    results("names") should be === (List("larry", "moe", "curly"))

    results = Map.empty
    args = cli.parse(List("--names=larry,moe,curly"))
    args should be ('empty)
    results should have size (1)
    results should contain key "names"
    results("names") should be === (List("larry", "moe", "curly"))

    results = Map.empty
    args = cli.parse(List("--names", "larry,moe,curly"))
    args should be ('empty)
    results should have size (1)
    results should contain key "names"
    results("names") should be === (List("larry", "moe", "curly"))

    // Should work for other known types
    results = Map.empty
    args = cli.parse(List("--sizes", "36,24,36"))
    args should be ('empty)
    results should have size (1)
    results should contain key "sizes"
    results("sizes") should be === (List(36,24,36))
  }

  // ====================================================================================
  
  it should "reject needless arguments for long switches that don't take them" in {
    val cli = new OptionParser
    var results: Map[String, Any] = Map.empty

    cli.noArg("-x", "--expert") { () => results += "expert" -> true }
    cli.boolArg("-t", "--timestamp") { v: Boolean => results += "timestamp" -> v }

    var thrown = evaluating { cli.parse(List("--expert=yes")) } should produce [OptionParserException]
    thrown.getMessage should startWith (NEEDLESS_ARGUMENT)

    thrown = evaluating { cli.parse(List("--timestamp=yes")) } should produce [OptionParserException]
    thrown.getMessage should startWith (NEEDLESS_ARGUMENT)
  }

  // ====================================================================================

  it should "handle short switches with required arguments" in {
    val cli = new OptionParser
    var results: Map[String, Any] = Map.empty
    
    cli.reqArg[String]("-n NAME", "") { name => results += "name" -> name }
    cli.noArg("-x", "") { () => results += "expert" -> true }
    cli.noArg("-t", "") { () => results += "text" -> true }
    
    results = Map.empty
    var args = cli.parse(List("-n", "curt", "foo"))
    args should have length (1)
    args(0) should be === "foo"
    results should have size (1)
    results should contain key "name"
    results("name") should be === "curt"

    results = Map.empty
    args = cli.parse(List("-ncurt", "foo"))
    args should have length (1)
    args(0) should be === "foo"
    results should have size (1)
    results should contain key "name"
    results("name") should be === "curt"

    results = Map.empty
    args = cli.parse(List("-xtn", "curt", "foo"))
    args should have length (1)
    args(0) should be === "foo"
    results should have size (3)
    results should contain key "expert"
    results should contain key "name"
    results should contain key "text"
    results("name") should be === "curt"
    results("expert") should be === (true)
    results("text") should be === (true)

    results = Map.empty
    args = cli.parse(List("-xtncurt", "foo"))
    args should have length (1)
    args(0) should be === "foo"
    results should have size (3)
    results should contain key "expert"
    results should contain key "name"
    results should contain key "text"
    results("name") should be === "curt"
    results("expert") should be === (true)
    results("text") should be === (true)
    
    var thrown = evaluating { cli.parse(List("-n")) } should produce [OptionParserException]
    thrown.getMessage should startWith (ARGUMENT_MISSING)
    
    thrown = evaluating { cli.parse(List("-xtn")) } should produce [OptionParserException]
    thrown.getMessage should startWith (ARGUMENT_MISSING)
  }

  // ====================================================================================

  it should "handle long switches with required arguments" in {
    val cli = new OptionParser
    var results: Map[String, Any] = Map.empty
    
    cli.reqArg("", "--name NAME") { name: String => results += "name" -> name }
    
    results = Map.empty
    var args = cli.parse(List("--name", "curt", "foo"))
    args should have length (1)
    args(0) should be === "foo"
    results should have size (1)
    results should contain key "name"
    results("name") should be === "curt"

    results = Map.empty
    args = cli.parse(List("--name=curt", "foo"))
    args should have length (1)
    args(0) should be === "foo"
    results should have size (1)
    results should contain key "name"
    results("name") should be === "curt"
    
    var thrown = evaluating { cli.parse(List("--name")) } should produce [OptionParserException]
    thrown.getMessage should startWith (ARGUMENT_MISSING)
  }

  // ====================================================================================

  it should "handle switches with String and File arguments" in {
    val cli = new OptionParser
    var results: Map[String, Any] = Map.empty
    
    cli.reqArg("-s", "--string ARG") { v: String => results += "string" -> v }
    cli.reqArg("-f", "--file FILE")  { v: java.io.File => results += "file" -> v }

    results = Map.empty
    var args = cli.parse(List("-s", "hello", "foo", "-f", "/etc/passwd"))
    args should have size (1)
    args(0) should be === "foo"
    results should have size (2)
    results should contain key "string"
    results should contain key "file"
    results("string") should be === ("hello")
    results("file") should be === (new java.io.File("/etc/passwd"))

    results = Map.empty
    args = cli.parse(List("-f/etc/passwd"))
    args should be ('empty)
    results should have size (1)
    results should contain key "file"
    results("file") should be === (new java.io.File("/etc/passwd"))
  }

  // ====================================================================================

  it should "handle switches with Char arguments" in {
    val cli = new OptionParser
    var results: Map[String, Any] = Map.empty
    
    cli.reqArg("-c", "--char ARG")   { v: Char   => results += "char"   -> v }
    
    // Single char
    results = Map.empty
    var args = cli.parse(List("-c", "%"))
    args should be ('empty)
    results should contain key "char"
    results("char") should be === ('%')

    // Control characters
    results = Map.empty
    args = cli.parse(List("-c", "\\b"))
    args should be ('empty)
    results should contain key "char"
    results("char") should be === (8.toChar)

    results = Map.empty
    args = cli.parse(List("-c", "\\t"))
    args should be ('empty)
    results should contain key "char"
    results("char") should be === (9.toChar)

    results = Map.empty
    args = cli.parse(List("-c", "\\n"))
    args should be ('empty)
    results should contain key "char"
    results("char") should be === (10.toChar)

    results = Map.empty
    args = cli.parse(List("-c", "\\f"))
    args should be ('empty)
    results should contain key "char"
    results("char") should be === (12.toChar)

    results = Map.empty
    args = cli.parse(List("-c", "\\r"))
    args should be ('empty)
    results should contain key "char"
    results("char") should be === (13.toChar)

    // Octal codes
    results = Map.empty
    args = cli.parse(List("-c", "\\033"))
    args should be ('empty)
    results should contain key "char"
    results("char") should be === (27.toChar)

    results = Map.empty
    args = cli.parse(List("-c", "\\33"))
    args should be ('empty)
    results should contain key "char"
    results("char") should be === (27.toChar)

    results = Map.empty
    args = cli.parse(List("-c", "\\377"))
    args should be ('empty)
    results should contain key "char"
    results("char") should be === (255.toChar)

    // Hex codes
    results = Map.empty
    args = cli.parse(List("-c", "\\xA"))
    args should be ('empty)
    results should contain key "char"
    results("char") should be === ('\n')

    results = Map.empty
    args = cli.parse(List("-c", "\\X0A"))
    args should be ('empty)
    results should contain key "char"
    results("char") should be === ('\n')

    results = Map.empty
    args = cli.parse(List("-c", "é"))
    args should be ('empty)
    results should contain key "char"
    results("char") should be === ('é')

    results = Map.empty
    args = cli.parse(List("-c", "\\xe9"))
    args should be ('empty)
    results should contain key "char"
    results("char") should be === ('é')

    results = Map.empty
    args = cli.parse(List("-c", "\\u00e9"))
    args should be ('empty)
    results should contain key "char"
    results("char") should be === ('é')

    // Unicode
    results = Map.empty
    args = cli.parse(List("-c", "\\u001B"))
    args should be ('empty)
    results should contain key "char"
    results("char") should be === (27.toChar)

    results = Map.empty
    args = cli.parse(List("-c", "\\u001b"))
    args should be ('empty)
    results should contain key "char"
    results("char") should be === (27.toChar)
    
    var thrown = evaluating { cli.parse(List("-c", "abc")) } should produce [OptionParserException]
    thrown.getMessage should startWith (INVALID_ARGUMENT)
    
    thrown = evaluating { cli.parse(List("-c", "\\g")) } should produce [OptionParserException]
    thrown.getMessage should startWith (INVALID_ARGUMENT)

    // Octal must be 1 two or three digits
    thrown = evaluating { cli.parse(List("-c", "\\01777")) } should produce [OptionParserException]
    thrown.getMessage should startWith (INVALID_ARGUMENT)

    // Octal, if three digits first cannot be greater then 3
    thrown = evaluating { cli.parse(List("-c", "\\777")) } should produce [OptionParserException]
    thrown.getMessage should startWith (INVALID_ARGUMENT)

    // Hex, cannot be more than 2 chars
    thrown = evaluating { cli.parse(List("-c", "\\x1FF")) } should produce [OptionParserException]
    thrown.getMessage should startWith (INVALID_ARGUMENT)

    // Unicode must be four hex digits
    thrown = evaluating { cli.parse(List("-c", "\\uFF")) } should produce [OptionParserException]
    thrown.getMessage should startWith (INVALID_ARGUMENT)
  }
  
  // ====================================================================================

  it should "handle switches with numeric argument types" in {
    val cli = new OptionParser
    var results: Map[String, Any] = Map.empty

    cli.reqArg("-i", "--int ARG")    { v: Int    => results += "int"    -> v }
    cli.reqArg("-h", "--short ARG")  { v: Short  => results += "short"  -> v }
    cli.reqArg("-l", "--long ARG")   { v: Long   => results += "long"   -> v }
    cli.reqArg("-f", "--float ARG")  { v: Float  => results += "float"  -> v }
    cli.reqArg("-d", "--double ARG") { v: Double => results += "double" -> v }

    results = Map.empty
    var args = cli.parse(List("-i", "2147483647", "-h", "32767", "-l", "9223372036854775807", "-f", "3.4028235E38", "-d", "1.7976931348623157E308"))
    args should be ('empty)
    results should have size (5)
    results should contain key "int"
    results should contain key "short"
    results should contain key "long"
    results should contain key "float"
    results should contain key "double"
    results("int") should be === (2147483647)
    results("short") should be === (32767.asInstanceOf[Short])
    results("long") should be === (9223372036854775807L)
    results("float") should be === (3.4028235E38.asInstanceOf[Float])
    results("double") should be === (1.7976931348623157E308)

    // Negatives
    results = Map.empty
    args = cli.parse(List("-i-2147483648", "-h-32768", "-l-9223372036854775808", "-f-3.4028235E38", "-d-1.7976931348623157E308"))
    args should be ('empty)
    results should have size (5)
    results should contain key "int"
    results should contain key "short"
    results should contain key "long"
    results should contain key "float"
    results should contain key "double"
    results("int") should be === (-2147483648)
    results("short") should be === ((-32768).asInstanceOf[Short])
    results("long") should be === (-9223372036854775808L)
    results("float") should be === (-3.4028235E38.asInstanceOf[Float])
    results("double") should be === (-1.7976931348623157E308)
    
    // Hex values
    results = Map.empty
    args = cli.parse(List("-i", "0xFFFF", "-h", "0XFFF", "-l", "0x12345FFFFF"))
    args should be ('empty)
    results should have size (3)
    results should contain key "int"
    results should contain key "short"
    results should contain key "long"
    results("int") should be === (0xFFFF)
    results("short") should be === (0xFFF.asInstanceOf[Short])
    results("long") should be === (0x12345FFFFFL)

    // Negative Hex values
    results = Map.empty
    args = cli.parse(List("-i", "-0xFFFF", "-h", "-0XFFF", "-l", "-0x12345FFFFF"))
    args should be ('empty)
    results should have size (3)
    results should contain key "int"
    results should contain key "short"
    results should contain key "long"
    results("int") should be === (-0xFFFF)
    results("short") should be === ((-0xFFF).asInstanceOf[Short])
    results("long") should be === (-0x12345FFFFFL)

    // Octal values
    results = Map.empty
    args = cli.parse(List("-i", "01777", "-h", "035", "-l", "024777444"))
    args should be ('empty)
    results should have size (3)
    results should contain key "int"
    results should contain key "short"
    results should contain key "long"
    results("int") should be === (01777)
    results("short") should be === (035.asInstanceOf[Short])
    results("long") should be === (024777444L)

    // Negative Octal values
    results = Map.empty
    args = cli.parse(List("-i", "-01777", "-h", "-035", "-l", "-024777444"))
    args should be ('empty)
    results should have size (3)
    results should contain key "int"
    results should contain key "short"
    results should contain key "long"
    results("int") should be === (-01777)
    results("short") should be === ((-035).asInstanceOf[Short])
    results("long") should be === (-024777444L)

    // Invalid values
    
    var thrown = evaluating { cli.parse(List("-i", "3.14")) } should produce [OptionParserException]
    thrown.getMessage should startWith (INVALID_ARGUMENT)
    thrown = evaluating { cli.parse(List("-h", "abc")) } should produce [OptionParserException]
    thrown.getMessage should startWith (INVALID_ARGUMENT)
    thrown = evaluating { cli.parse(List("-l", "abc")) } should produce [OptionParserException]
    thrown.getMessage should startWith (INVALID_ARGUMENT)
    thrown = evaluating { cli.parse(List("-f", "abc")) } should produce [OptionParserException]
    thrown.getMessage should startWith (INVALID_ARGUMENT)
    thrown = evaluating { cli.parse(List("-d", "abc")) } should produce [OptionParserException]
    thrown.getMessage should startWith (INVALID_ARGUMENT)
    
    // Values out of range
    thrown = evaluating { cli.parse(List("-i", "9223372036854775807")) } should produce [OptionParserException]
    thrown.getMessage should startWith (INVALID_ARGUMENT)
    thrown = evaluating { cli.parse(List("-h", "9223372036854775807")) } should produce [OptionParserException]
    thrown.getMessage should startWith (INVALID_ARGUMENT)
    thrown = evaluating { cli.parse(List("-l", "92233720368547758078")) } should produce [OptionParserException]
    thrown.getMessage should startWith (INVALID_ARGUMENT)
    thrown = evaluating { cli.parse(List("-f", "1.7976931348623157E308")) } should produce [OptionParserException]
    thrown.getMessage should startWith (INVALID_ARGUMENT)
    thrown = evaluating { cli.parse(List("-d", "1.7976931348623157E309")) } should produce [OptionParserException]
    thrown.getMessage should startWith (INVALID_ARGUMENT)

    thrown = evaluating { cli.parse(List("-i", "0x1FFFFFFFF")) } should produce [OptionParserException]
    thrown.getMessage should startWith (INVALID_ARGUMENT)
    thrown = evaluating { cli.parse(List("-h", "0x1FFFFFFFF")) } should produce [OptionParserException]
    thrown.getMessage should startWith (INVALID_ARGUMENT)
    thrown = evaluating { cli.parse(List("-l", "0x1FFFFFFFFFFFFFFFF")) } should produce [OptionParserException]
    thrown.getMessage should startWith (INVALID_ARGUMENT)
    
  }
  
  // ====================================================================================
  
  it should "terminate switch processing with --" in {
    val cli = new OptionParser
    var results: Map[String, Any] = Map.empty

    cli.reqArg("-i", "--int ARG")    { v: Int    => results += "int"    -> v }

    var args = cli.parse(List("-i", "9", "--", "-i", "10"))
    args should have length (2)
    args(0) should be === "-i"
    args(1) should be === "10"
    results should have size (1)
    results should contain key "int"
    results("int") should be === (9)
    
    results = Map.empty
    args = cli.parse(List("--int", "9", "foo", "--", "--int", "10"))
    args should have length (3)
    args(0) should be === "foo"
    args(1) should be === "--int"
    args(2) should be === "10"
    results should have size (1)
    results should contain key "int"
    results("int") should be === (9)
  }
  
  // ====================================================================================
  
  it should "allow '-' as an argument (ie. stdin)" in {
    val cli = new OptionParser
    var results: Map[String, Any] = Map.empty

    cli.reqArg("-f", "--file ARG")    { v: String  => results += "file" -> v }

    var args = cli.parse(List("-"))
    args should have length (1)
    args(0) should be === "-"
    results should be ('empty)
    
    results = Map.empty
    args = cli.parse(List("-f", "-"))
    args should be ('empty)
    results should have size (1)
    results should contain key "file"
    results("file") should be === ("-")

    results = Map.empty
    args = cli.parse(List("-f-"))
    args should be ('empty)
    results should have size (1)
    results should contain key "file"
    results("file") should be === ("-")

    results = Map.empty
    args = cli.parse(List("--file", "-"))
    args should be ('empty)
    results should have size (1)
    results should contain key "file"
    results("file") should be === ("-")

    results = Map.empty
    args = cli.parse(List("--file=-"))
    args should be ('empty)
    results should have size (1)
    results should contain key "file"
    results("file") should be === ("-")
  }
  
  // ====================================================================================
  
  it should "handle short switches with optional arguments" in {
    val cli = new OptionParser
    var results: Map[String, Any] = Map.empty

    cli.noArg("-x", "")        { () => results += "expert" -> true }
    cli.optArg("-d [VAL]", "") { dir: Option[String] => results += "dir" -> dir.getOrElse("/tmp")}
    
    results = Map.empty
    var args = cli.parse(List("-d", "/etc", "foo"))
    args should have length (1)
    args(0) should be === "foo"
    results should have size (1)
    results should contain key "dir"
    results("dir") should be === "/etc"

    results = Map.empty
    args = cli.parse(List("-d/etc", "foo"))
    args should have length (1)
    args(0) should be === "foo"
    results should have size (1)
    results should contain key "dir"
    results("dir") should be === "/etc"

    results = Map.empty
    args = cli.parse(List("-xd", "/etc", "foo"))
    args should have length (1)
    args(0) should be === "foo"
    results should have size (2)
    results should contain key "expert"
    results should contain key "dir"
    results("expert") should be === (true)
    results("dir") should be === "/etc"

    results = Map.empty
    args = cli.parse(List("-xd/etc", "foo"))
    args should have length (1)
    args(0) should be === "foo"
    results should have size (2)
    results should contain key "expert"
    results should contain key "dir"
    results("expert") should be === (true)
    results("dir") should be === "/etc"

    results = Map.empty
    // -- marks end of options (it would be eaten if the swith REQUIRED and argument!)
    args = cli.parse(List("-xd", "--", "foo"))
    args should have length (1)
    args(0) should be === "foo"
    results should have size (2)
    results should contain key "expert"
    results should contain key "dir"
    results("expert") should be === (true)
    results("dir") should be === "/tmp"  // <<--- Default value

    results = Map.empty
    // Optional args should not eat an arg that looks like a switch
    args = cli.parse(List("-d", "-x", "foo"))
    args should have length (1)
    args(0) should be === "foo"
    results should have size (2)
    results should contain key "expert"
    results should contain key "dir"
    results("expert") should be === (true)
    results("dir") should be === "/tmp" // <<--- Default value

    results = Map.empty
    // Optional args should used '-' as an argument.
    args = cli.parse(List("-d", "-", "foo"))
    args should have length (1)
    args(0) should be === "foo"
    results should have size (1)
    results should contain key "dir"
    results("dir") should be === "-"

    results = Map.empty
    args = cli.parse(List("-d"))
    args should be ('empty)
    results should have size (1)
    results should contain key "dir"
    results("dir") should be === "/tmp"
  }

  // ====================================================================================

  it should "handle long switches with optional arguments" in {
    val cli = new OptionParser
    var results: Map[String, Any] = Map.empty

    cli.noArg("-x",  "--expert")  { () => results += "expert" -> true }
    cli.optArg("", "--at [AT]") { at: Option[String] => results += "at" -> at.getOrElse("00:00")}

    results = Map.empty
    var args = cli.parse(List("--at", "1:30", "foo"))
    args should have length (1)
    args(0) should be === "foo"
    results should have size (1)
    results should contain key "at"
    results("at") should be === ("1:30")

    results = Map.empty
    args = cli.parse(List("--at=1:30", "foo"))
    args should have length (1)
    args(0) should be === "foo"
    results should have size (1)
    results should contain key "at"
    results("at") should be === ("1:30")

    // Optional args shouldn't eat an option name looking arg
    results = Map.empty
    args = cli.parse(List("--at", "-x", "foo"))
    args should have length (1)
    args(0) should be === "foo"
    results should have size (2)
    results should contain key "at"
    results should contain key "expert"
    results("at") should be === ("00:00")
    results("expert") should be === (true)
    
    // Optional args should eat an option name looking arg if it is attached
    results = Map.empty
    args = cli.parse(List("--at=-x", "foo"))
    args should have length (1)
    args(0) should be === "foo"
    results should have size (1)
    results should contain key "at"
    results("at") should be === ("-x")

    results = Map.empty
    args = cli.parse(List("--at", "--", "foo"))
    args should have length (1)
    args(0) should be === "foo"
    results should have size (1)
    results should contain key "at"
    results("at") should be === ("00:00")

    results = Map.empty
    args = cli.parse(List("--at"))
    args should be ('empty)
    results should have size (1)
    results should contain key "at"
    results("at") should be === ("00:00")
  }
  
  // ====================================================================================
  
  it should "interpret --name= to have a empty string argument" in {
    val cli = new OptionParser
    var results: Map[String, Any] = Map.empty

    cli.reqArg("-n",  "--name NAME")  { s: String => results += "name" -> s }

    results = Map.empty
    var args = cli.parse(List("--name="))
    args should be ('empty)
    results should have size (1)
    results should contain key "name"
    results("name") should be === ""
  }
  
  // ====================================================================================
  
  it should "interpret following args that begin with a '-' correctly (short)" in {
    val cli = new OptionParser
    var results: Map[String, Any] = Map.empty

    cli.noArg("-x",  "--expert")  { () => results += "expert" -> true }
    cli.reqArg("-g", "--gist TEXT", "Set Gist") { gist: String => results += "gist" -> gist }
    cli.optArg("-j", "--jazz [TEXT]", "Set Jazz") { jazz: Option[String] => results += "jazz" -> jazz.getOrElse("bebop")}

    // Required argument
    results = Map.empty
    var args = cli.parse(List("-g-x"))
    args should be ('empty)
    results should have size (1)
    results should contain key "gist"
    results("gist") should be === "-x"

    results = Map.empty
    args = cli.parse(List("-g", "-x")) // Required args are greedy!
    args should be ('empty)
    results should have size (1)
    results should contain key "gist"
    results("gist") should be === "-x"

    // Optional argument
    results = Map.empty
    args = cli.parse(List("-j-x")) // Attached, so should use as arg.
    args should be ('empty)
    results should have size (1)
    results should contain key "jazz"
    results("jazz") should be === "-x"

    results = Map.empty
    args = cli.parse(List("-j", "-x")) // Optional arg, should not pick up the -x
    args should be ('empty)
    results should have size (2)
    results should contain key "jazz"
    results should contain key "expert"
    results("jazz") should be === ("bebop")
    results("expert") should be === (true)
  }

  // ====================================================================================

  it should "interpret following args that begin with a '-' correctly (long)" in {
    val cli = new OptionParser
    var results: Map[String, Any] = Map.empty

    cli.noArg("-x",  "--expert")  { () => results += "expert" -> true }
    cli.reqArg("-g", "--gist TEXT", "Set Gist") { gist: String => results += "gist" -> gist }
    cli.optArg("-j", "--jazz [TEXT]", "Set Jazz") { jazz: Option[String] => results += "jazz" -> jazz.getOrElse("bebop")}

    // Required argument
    results = Map.empty
    var args = cli.parse(List("--gist=-x"))
    args should be ('empty)
    results should have size (1)
    results should contain key "gist"
    results("gist") should be === "-x"

    results = Map.empty
    args = cli.parse(List("--gist", "-x")) // Required args are greedy!
    args should be ('empty)
    results should have size (1)
    results should contain key "gist"
    results("gist") should be === "-x"

    // Optional argument
    results = Map.empty
    args = cli.parse(List("--jazz=-x")) // Attached, so should use as arg.
    args should be ('empty)
    results should have size (1)
    results should contain key "jazz"
    results("jazz") should be === "-x"

    results = Map.empty
    args = cli.parse(List("--jazz", "-x")) // Optional arg, should not pick up the -x
    args should be ('empty)
    results should have size (2)
    results should contain key "jazz"
    results should contain key "expert"
    results("jazz") should be === ("bebop")
    results("expert") should be === (true)
  }
  
  // ====================================================================================
  it should "handle custom argument parsers" in {
    val cli = new OptionParser
    var results: Map[String, Any] = Map.empty
    
    case class Foo(s: String)
    cli.addArgumentParser { s: String => Foo(s) }
    
    cli.reqArg("", "--foo1") { foo : Foo => results += "foo1" -> foo}
    cli.optArg("", "--foo2") { foo : Option[Foo] => results += "foo2" -> foo.getOrElse(Foo("Default"))}

    results = Map.empty
    var args = cli.parse(List("--foo1", "46"))
    args should be ('empty)
    results should have size (1)
    results should contain key "foo1"
    results("foo1") should be === Foo("46")

    results = Map.empty
    args = cli.parse(List("--foo2", "46"))
    args should be ('empty)
    results should have size (1)
    results should contain key "foo2"
    results("foo2") should be === Foo("46")

    results = Map.empty
    args = cli.parse(List("--foo2"))
    args should be ('empty)
    results should have size (1)
    results should contain key "foo2"
    results("foo2") should be === Foo("Default")
  }

  // ====================================================================================
  
  it should "handle switches with a required value limmited by a List of values" in {
    val cli = new OptionParser
    var results: Map[String, Any] = Map.empty

    cli.reqArg("-t", "--type (binary, ascii)", List("binary", "ascii", "auto")) { t => results += "type" -> t }

    results = Map.empty
    var args = cli.parse(List("-t", "binary"))
    args should be ('empty)
    results should have size (1)
    results should contain key "type"
    results("type") should be === "binary"

    // Matching of argument prefix
    results = Map.empty
    args = cli.parse(List("--type", "as"))
    args should be ('empty)
    results should have size (1)
    results should contain key "type"
    results("type") should be === "ascii"
  
    var thrown = evaluating { cli.parse(List("--type=a")) } should produce [OptionParserException]
    thrown.getMessage should startWith (AMBIGUOUS_ARGUMENT)
    
    thrown = evaluating { cli.parse(List("--type=ebcdic")) } should produce [OptionParserException]
    thrown.getMessage should startWith (INVALID_ARGUMENT)
  }

  // ====================================================================================
  
  it should "handle switches with an optional value limmited by a Map of values" in {
    val cli = new OptionParser
    var results: Map[String, Any] = Map.empty

    cli.optArg("-z", "--zone", Map("one" -> 1, "two" -> 2, "three" -> 3)) { z => results += "zone" -> z.getOrElse(1) }

    results = Map.empty
    var args = cli.parse(List("-z"))
    args should be ('empty)
    results should have size (1)
    results should contain key "zone"
    results("zone") should be === (1)

    var thrown = evaluating { args = cli.parse(List("-z", "four")) } should produce [OptionParserException]
    thrown.getMessage should startWith (INVALID_ARGUMENT)

    results = Map.empty
    args = cli.parse(List("-z", "tw"))
    args should be ('empty)
    results should have size (1)
    results should contain key "zone"
    results("zone") should be === (2)

    results = Map.empty
    args = cli.parse(List("-zth"))
    args should be ('empty)
    results should have size (1)
    results should contain key "zone"
    results("zone") should be === (3)

    thrown = evaluating { args = cli.parse(List("-ztx")) } should produce [OptionParserException]
    thrown.getMessage should startWith (INVALID_ARGUMENT)
  
    thrown = evaluating { cli.parse(List("--zone", "t")) } should produce [OptionParserException]
    thrown.getMessage should startWith (AMBIGUOUS_ARGUMENT)
  }
  
  // ====================================================================================
  // ================== Help Info=== ====================================================
  // ====================================================================================

  "Help Info" should "include a -h, --help option by default" in {
    val cli = new OptionParser
    cli.help should be === ("    -h, --help                       Show this message")
  }    
  
  // ====================================================================================

  it should "allow auto_help to be disabled" in {
    val cli = new OptionParser
    cli.auto_help = false
    cli.help should be === ("")
  }
  
  // ====================================================================================

  it should "work for switches without args." in {
    val cli = new OptionParser
    cli.auto_help = false
    cli.noArg("-a", "") { () => }
    cli.noArg("-b", "", "Help for b") { () => }
    cli.noArg("-c", "", "Line 1 for c", "Line 2 for c") { () => }
    cli.noArg("", "--dd") { () => }
    cli.noArg("", "--eee", "Help for eee") { () => }
    cli.noArg("", "--ffff", "Line 1 for ffff", "Line 2 for ffff") { () => }
    cli.noArg("-g", "--ggggg") { () => }
    cli.noArg("-h", "--hhhhhh", "Help for hhhhhh") { () => }
    cli.noArg("-i", "--iiiiiii", "Line 1 for iiiiiii", "Line 2 for iiiiiii") { () => }

    cli.help should be === (
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
    val cli = new OptionParser
    cli.auto_help = false
    cli.boolArg("-a", "") { v => }
    cli.boolArg("-b", "", "Help for b") { v => }
    cli.boolArg("-c", "", "Line 1 for c", "Line 2 for c") { v => }
    cli.boolArg("", "--dd") { v => }
    cli.boolArg("", "--eee", "Help for eee") { v => }
    cli.boolArg("", "--ffff", "Line 1 for ffff", "Line 2 for ffff") { v => }
    cli.boolArg("-g", "--ggggg") { v => }
    cli.boolArg("-h", "--hhhhhh", "Help for hhhhhh") { v => }
    cli.boolArg("-i", "--iiiiiii", "Line 1 for iiiiiii", "Line 2 for iiiiiii") { v => }

    cli.help should be === (
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
    val cli = new OptionParser
    cli.auto_help = false
    cli.reqArg[Int]("-a ARG", "") { v => }
    cli.reqArg[Int]("-b VAL", "", "Help for b") { v => }
    cli.reqArg[Int]("-c VALUE", "", "Line 1 for c", "Line 2 for c") { v => }
    cli.reqArg[Int]("", "--dd ARG") { v => }
    cli.reqArg[Int]("", "--eee VAL", "Help for eee") { v => }
    cli.reqArg[Int]("", "--ffff VALUE", "Line 1 for ffff", "Line 2 for ffff") { v => }
    cli.reqArg[Int]("-g", "--ggggg=ARG") { v => }
    cli.reqArg[Int]("-h", "--hhhhhh=VAL", "Help for hhhhhh") { v => }
    cli.reqArg[Int]("-i", "--iiiiiii=VALUE", "Line 1 for iiiiiii", "Line 2 for iiiiiii") { v => }
  
    cli.help should be === (
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
    val cli = new OptionParser
    cli.auto_help = false
    cli.optArg[Int]("-a [ARG]", "") { v => }
    cli.optArg[Int]("-b [VAL]", "", "Help for b") { v => }
    cli.optArg[Int]("-c [VALUE]", "", "Line 1 for c", "Line 2 for c") { v => }
    cli.optArg[Int]("", "--dd [ARG]") { v => }
    cli.optArg[Int]("", "--eee [VAL]", "Help for eee") { v => }
    cli.optArg[Int]("", "--ffff [VALUE]", "Line 1 for ffff", "Line 2 for ffff") { v => }
    cli.optArg[Int]("-g", "--ggggg=[ARG]") { v => }
    cli.optArg[Int]("-h", "--hhhhhh=[VAL]", "Help for hhhhhh") { v => }
    cli.optArg[Int]("-i", "--iiiiiii=[VALUE]", "Line 1 for iiiiiii", "Line 2 for iiiiiii") { v => }
    cli.optArg[Int]("-j", "--jjjjj[=ARG]") { v => }
    cli.optArg[Int]("-k", "--kkkkkk[=VAL]", "Help for kkkkkk") { v => }
    cli.optArg[Int]("-l", "--lllllll[=VALUE]", "Line 1 for lllllll", "Line 2 for lllllll") { v => }
  
    cli.help should be === (
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
    val cli = new OptionParser
    cli.auto_help = false
    cli.reqArg[Int]("-a ARG", "--apple VALUE") { v => }
    cli.optArg[Int]("-b [ARG]", "--box [VALUE]") { v => }
    
    cli.help should be === (
    """    -a, --apple VALUE
      |    -b, --box [VALUE]""".stripMargin('|')
    )
    
  }
  
  // ====================================================================================
  
  it should "support the banner and separators." in {
    val cli = new OptionParser
    
    cli.banner = "testapp [options]"
    cli separator ""
    cli.noArg("-a", "--apple") { () => }
    cli.noArg("-b", "--best", "Pick best option") { () => }
    cli.noArg("-c", "--cat", "Cat option", "  more cat info") { () => }
    cli.reqArg("-d ARG", "", "dddd dddd") { v: String => }
    cli separator ""
    cli separator "More options:"
      // Arg for long name overrides that from short!
    cli.reqArg("-e EEE", "--extra ARG", "Extra stuff") { v: String => }
    cli.reqArg("-f", "--file=FILE", "File name") { file: java.io.File => }
    cli.reqArg[String]("", "--goo=ARG", "Goo help") { v => }
    
    
    cli.help should be === (
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