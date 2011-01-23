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
  
  var results: Map[String, Any] = Map.empty
  val opts = new OptionParser
  // The first two switches should be removed when the third switch is added since
  // the short and long names are duplicated. (See first test)
  opts.noArg("-x", "--notUsed") { () => results += "notUsed" -> true }
  opts.noArg("-X", "--expert") { () => results += "notUsed2" -> true }
  opts.noArg("-x", "--expert", "Expert option", "more..") { () => results += "expert" -> true }
  opts.reqArg("-n", "--name=NAME", "Set Name") { name: String => results += "name" -> name }
  opts.optArg("-r", "--reset [VAL]", "Reset..") { dir: Option[String] => results += "dir" -> dir.getOrElse("top")}
  opts.reqArg("-s SIZE", "--size", "Set Size") { size: Int => results += "size" -> size }
  opts.optArg("-a [AT]", "--at", "At..") { at: Option[Int] => results += "at" -> at.getOrElse(100)}

  opts.reqArg("-g", "--gist TEXT", "Set Gist") { gist: String => results += "gist" -> gist }
  opts.optArg("-j", "--jazz [TEXT]", "Set Jazz") { jazz: Option[String] => results += "jazz" -> jazz.getOrElse("bebop")}
  
  opts.reqArg("-t", "--type (binary, ascii)", List("binary", "ascii")) { t => results += "type" -> t }
  opts.optArg("-z", "--zone [one, two, three]", Map("one" -> 1, "two" -> 2, "three" -> 3)) { z => 
    results += "zone" -> z.getOrElse(1)
  }
  
  case class Foo(s: String)
  opts.addConverter { s: String => Foo(s) }
  opts.reqArg("-f", "--foo", "Set Foo") { foo : Foo => results += "foo" -> foo}
  
  opts.noArg("-h", "--help", "Display Help") { () => results += "help" -> true }
    
  "OptionParser" should "remove switches with identical names when adding a new switch" in {
    results = Map.empty
    var args = opts.parse(List("-x"))
    args should be ('empty)
    results should have size (1)
    results should not contain key ("notUsed")
    results should contain key "expert"
    results("expert") should be === (true)

    results = Map.empty
    args = opts.parse(List("--expert"))
    args should be ('empty)
    results should have size (1)
    results should not contain key ("notUsed2")
    results should contain key "expert"
    results("expert") should be === (true)
  }
  
  // ====================================================================================
  // ====================================================================================
  it should "handle empty argv" in {
    results = Map.empty
    val args = opts.parse(List())
    args should be ('empty)
    results should be ('empty)
  }    

  // ====================================================================================
  // ====================================================================================
  it should "handle arg list without any switches" in {
    results = Map.empty
    var args = opts.parse(List("foo"))
    args should have length (1)
    args(0) should be === "foo"
    results should be ('empty)
    
    args = opts.parse(List("foo", "bar"))
    args should have length (2)
    args(0) should be === "foo"
    args(1) should be === "bar"
    results should be ('empty)
  }    
  // ====================================================================================
  // ====================================================================================
  it should "handle arg list only short switches" in {
    results = Map.empty
    var args = opts.parse(List("-x"))
    args should be ('empty)
    results should have size (1)
    results should contain key "expert"
    results("expert") should be === (true)
    
    results = Map.empty
    args = opts.parse(List("-x", "-h"))
    args should be ('empty)
    results should have size (2)
    results should contain key "expert"
    results should contain key "help"
    results("expert") should be === (true)
    results("help") should be === (true)

    results = Map.empty
    args = opts.parse(List("-xh"))
    args should be ('empty)
    results should have size (2)
    results should contain key "expert"
    results should contain key "help"
    results("expert") should be === (true)
    results("help") should be === (true)
  }    
  
  it should "handle both args and short switches" in {
    results = Map.empty
    var args = opts.parse(List("-x", "foo", "-h"))
    args should have length (1)
    args(0) should be === "foo"
    results should contain key "expert"
    results should contain key "help"
    results("expert") should be === (true)
    results("help") should be === (true)
  }

  it should "handle long switches" in {
    results = Map.empty
    var args = opts.parse(List("--expert", "foo", "--help"))
    args should have length (1)
    args(0) should be === "foo"
    results should have size (2)
    results should contain key "expert"
    results should contain key "help"
    results("expert") should be === (true)
    results("help") should be === (true)
  }
  
  it should "handle long switches with partial names" in {
    results = Map.empty
    var args = opts.parse(List("--exp", "foo", "--h"))
    args should have length (1)
    args(0) should be === "foo"
    results should have size (2)
    results should contain key "expert"
    results should contain key "help"
    results("expert") should be === (true)
    results("help") should be === (true)
  }
  
  it should "reject needless arguments for long switches that don't take them" in {
    var thrown = evaluating {
      opts.parse(List("--expert=yes"))
    } should produce [OptionParserException]
    thrown.getMessage should startWith (NEEDLESS_ARGUMENT)
  }
  
  it should "handle short switches with required arguments" in {
    results = Map.empty
    var args = opts.parse(List("-n", "curt", "foo"))
    args should have length (1)
    args(0) should be === "foo"
    results should have size (1)
    results should contain key "name"
    results("name") should be === "curt"

    results = Map.empty
    args = opts.parse(List("-ncurt", "foo"))
    args should have length (1)
    args(0) should be === "foo"
    results should have size (1)
    results should contain key "name"
    results("name") should be === "curt"

    results = Map.empty
    args = opts.parse(List("-xhn", "curt", "foo"))
    args should have length (1)
    args(0) should be === "foo"
    results should have size (3)
    results should contain key "expert"
    results should contain key "name"
    results should contain key "help"
    results("name") should be === "curt"
    results("expert") should be === (true)
    results("help") should be === (true)

    results = Map.empty
    args = opts.parse(List("-xhncurt", "foo"))
    args should have length (1)
    args(0) should be === "foo"
    results should have size (3)
    results should contain key "expert"
    results should contain key "name"
    results should contain key "help"
    results("name") should be === "curt"
    results("expert") should be === (true)
    results("help") should be === (true)
  }
  
  it should "handle long switches with required arguments" in {
    results = Map.empty
    var args = opts.parse(List("--name", "curt", "foo"))
    args should have length (1)
    args(0) should be === "foo"
    results should have size (1)
    results should contain key "name"
    results("name") should be === "curt"
  
    results = Map.empty
    args = opts.parse(List("--name=curt", "foo"))
    args should have length (1)
    args(0) should be === "foo"
    results should have size (1)
    results should contain key "name"
    results("name") should be === "curt"
  }
  
  it should "throw an exception if a required argument is missing" in {
    var thrown = evaluating {
      opts.parse(List("foo", "--name"))
    } should produce [OptionParserException]
    thrown.getMessage should startWith (ARGUMENT_MISSING)
    
    thrown = evaluating {
      opts.parse(List("foo", "-n"))
    } should produce [OptionParserException]
    thrown.getMessage should startWith (ARGUMENT_MISSING)
  }
  
  it should "detect invalid switches" in {
    var thrown = evaluating {
      opts.parse(List("foo", "--asdf"))
    } should produce [OptionParserException]
    thrown.getMessage should startWith (INVALID_OPTION)
    
    thrown = evaluating {
      opts.parse(List("foo", "-X"))
    } should produce [OptionParserException]
    thrown.getMessage should startWith (INVALID_OPTION)
  }
  
  it should "handle switches with integer arguments" in {
    results = Map.empty
    var args = opts.parse(List("-s", "9", "foo"))
    args should have length (1)
    args(0) should be === "foo"
    results should have size (1)
    results should contain key "size"
    results("size") should be === (9)

    results = Map.empty
    args = opts.parse(List("--size=9", "foo"))
    args should have length (1)
    args(0) should be === "foo"
    results should have size (1)
    results should contain key "size"
    results("size") should be === (9)
  }
  
  it should "terminate switch processing with --" in {
    results = Map.empty
    var args = opts.parse(List("-s", "9", "foo", "--", "-x"))
    args should have length (2)
    args(0) should be === "foo"
    args(1) should be === "-x"
    results should have size (1)
    results should contain key "size"
    results("size") should be === (9)
  }
  
  
  // ====================================================================================
  // ====================================================================================
  it should "handle short switches with optional arguments" in {
    results = Map.empty
    var args = opts.parse(List("-r", "bottom", "foo"))
    args should have length (1)
    args(0) should be === "foo"
    results should have size (1)
    results should contain key "dir"
    results("dir") should be === "bottom"

    results = Map.empty
    args = opts.parse(List("-rbottom", "foo"))
    args should have length (1)
    args(0) should be === "foo"
    results should have size (1)
    results should contain key "dir"
    results("dir") should be === "bottom"

    results = Map.empty
    args = opts.parse(List("-xr", "bottom", "foo"))
    args should have length (1)
    args(0) should be === "foo"
    results should have size (2)
    results should contain key "expert"
    results should contain key "dir"
    results("expert") should be === (true)
    results("dir") should be === "bottom"

    results = Map.empty
    args = opts.parse(List("-xrbottom", "foo"))
    args should have length (1)
    args(0) should be === "foo"
    results should have size (2)
    results should contain key "expert"
    results should contain key "dir"
    results("expert") should be === (true)
    results("dir") should be === "bottom"

    results = Map.empty
    args = opts.parse(List("-xr", "--", "foo"))
    args should have length (1)
    args(0) should be === "foo"
    results should have size (2)
    results should contain key "expert"
    results should contain key "dir"
    results("expert") should be === (true)
    results("dir") should be === "top"

    results = Map.empty
    args = opts.parse(List("-r", "-x", "foo"))
    args should have length (1)
    args(0) should be === "foo"
    results should have size (2)
    results should contain key "expert"
    results should contain key "dir"
    results("expert") should be === (true)
    results("dir") should be === "top"

    results = Map.empty
    args = opts.parse(List("-r"))
    args should be ('empty)
    results should have size (1)
    results should contain key "dir"
    results("dir") should be === "top"
  }
  
  it should "handle long switches with optional arguments" in {
    results = Map.empty
    var args = opts.parse(List("--at", "10", "foo"))
    args should have length (1)
    args(0) should be === "foo"
    results should have size (1)
    results should contain key "at"
    results("at") should be === (10)

    results = Map.empty
    args = opts.parse(List("--at=10", "foo"))
    args should have length (1)
    args(0) should be === "foo"
    results should have size (1)
    results should contain key "at"
    results("at") should be === (10)

    results = Map.empty
    args = opts.parse(List("--at", "-x", "foo"))
    args should have length (1)
    args(0) should be === "foo"
    results should have size (2)
    results should contain key "at"
    results should contain key "expert"
    results("at") should be === (100)
    results("expert") should be === (true)

    results = Map.empty
    args = opts.parse(List("--at", "--", "foo"))
    args should have length (1)
    args(0) should be === "foo"
    results should have size (1)
    results should contain key "at"
    results("at") should be === (100)

    results = Map.empty
    args = opts.parse(List("--at"))
    args should be ('empty)
    results should have size (1)
    results should contain key "at"
    results("at") should be === (100)
  }

  // ====================================================================================
  // ====================================================================================
  it should "interpret --name= to have a empty string argument" in {
    results = Map.empty
    var args = opts.parse(List("--name="))
    args should be ('empty)
    results should have size (1)
    results should contain key "name"
    results("name") should be === ""
  }

  // ====================================================================================
  // opts.reqArg("-g", "--gist TEXT", "Set Gist") { gist: String => results += "gist" -> gist }
  // opts.optArg("-j", "--jazz [TEXT]", "Set Jazz") { jazz: Option[String] => results += "jazz" -> jazz.getOrElse("bebop")}
  // ====================================================================================
  it should "interpret following args that begin with a '-' correctly (short)" in {
    // Required argument
    results = Map.empty
    var args = opts.parse(List("-g-x"))
    args should be ('empty)
    results should have size (1)
    results should contain key "gist"
    results("gist") should be === "-x"

    results = Map.empty
    args = opts.parse(List("-g", "-x")) // Required args are greedy!
    args should be ('empty)
    results should have size (1)
    results should contain key "gist"
    results("gist") should be === "-x"

    // Optional argument
    results = Map.empty
    args = opts.parse(List("-j-x")) // Attached, so should use as arg.
    args should be ('empty)
    results should have size (1)
    results should contain key "jazz"
    results("jazz") should be === "-x"

    results = Map.empty
    args = opts.parse(List("-j", "-x")) // Optional arg, should not pick up the -x
    args should be ('empty)
    results should have size (2)
    results should contain key "jazz"
    results should contain key "expert"
    results("jazz") should be === ("bebop")
    results("expert") should be === (true)
  }
  
  // ====================================================================================
  // opts.reqArg("-g", "--gist TEXT", "Set Gist") { gist: String => results += "gist" -> gist }
  // opts.optArg("-j", "--jazz [TEXT]", "Set Jazz") { jazz: Option[String] => results += "jazz" -> jazz.getOrElse("bebop")}
  // ====================================================================================
  it should "interpret following args that begin with a '-' correctly (long)" in {
    // Required argument
    results = Map.empty
    var args = opts.parse(List("--gist=-x"))
    args should be ('empty)
    results should have size (1)
    results should contain key "gist"
    results("gist") should be === "-x"

    results = Map.empty
    args = opts.parse(List("--gist", "-x")) // Required args are greedy!
    args should be ('empty)
    results should have size (1)
    results should contain key "gist"
    results("gist") should be === "-x"

    // Optional argument
    results = Map.empty
    args = opts.parse(List("--jazz=-x")) // Attached, so should use as arg.
    args should be ('empty)
    results should have size (1)
    results should contain key "jazz"
    results("jazz") should be === "-x"

    results = Map.empty
    args = opts.parse(List("--jazz", "-x")) // Optional arg, should not pick up the -x
    args should be ('empty)
    results should have size (2)
    results should contain key "jazz"
    results should contain key "expert"
    results("jazz") should be === ("bebop")
    results("expert") should be === (true)
  }

  // ====================================================================================
  // ====================================================================================
  it should "handle new types" in {
    results = Map.empty
    var args = opts.parse(List("--foo", "46"))
    args should be ('empty)
    results should have size (1)
    results should contain key "foo"
    results("foo") should be === Foo("46")
  }
  
  // ====================================================================================
  // ====================================================================================
  it should "handle switches with a required value limmited by a List of values" in {
    results = Map.empty
    var args = opts.parse(List("-t", "binary"))
    args should be ('empty)
    results should have size (1)
    results should contain key "type"
    results("type") should be === "binary"
 
    results = Map.empty
    args = opts.parse(List("--type", "a"))
    args should be ('empty)
    results should have size (1)
    results should contain key "type"
    results("type") should be === "ascii"
    
    var thrown = evaluating {
      opts.parse(List("--type=ebcdic"))
    } should produce [OptionParserException]
    thrown.getMessage should startWith (INVALID_ARGUMENT)
  }
  
  // ====================================================================================
  // ====================================================================================
  it should "handle switches with an optional value limmited by a Map of values" in {
    results = Map.empty
    var args = opts.parse(List("-z"))
    args should be ('empty)
    results should have size (1)
    results should contain key "zone"
    results("zone") should be === (1)
 
    var thrown = evaluating {
      args = opts.parse(List("-z", "four"))
    } should produce [OptionParserException]
    thrown.getMessage should startWith (INVALID_ARGUMENT)
  
    results = Map.empty
    args = opts.parse(List("-z", "tw"))
    args should be ('empty)
    results should have size (1)
    results should contain key "zone"
    results("zone") should be === (2)
 
    results = Map.empty
    args = opts.parse(List("-zth"))
    args should be ('empty)
    results should have size (1)
    results should contain key "zone"
    results("zone") should be === (3)
 
    thrown = evaluating {
      args = opts.parse(List("-ztx"))
    } should produce [OptionParserException]
    thrown.getMessage should startWith (INVALID_ARGUMENT)
 
    
    thrown = evaluating {
      opts.parse(List("--zone", "t"))
    } should produce [OptionParserException]
    thrown.getMessage should startWith (AMBIGUOUS_ARGUMENT)
  }
  
  // ====================================================================================
  // ====================================================================================
  it should "detect attempts to add an option for a type with no converter" in {
    class SomeClass
    evaluating {
      opts.reqArg("-z", "--zoo", "Set Zoo") { zoo : SomeClass => results += "zoo" -> zoo}
    } should produce [RuntimeException]
  }    
  
}