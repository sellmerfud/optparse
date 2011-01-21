package org.fud.optparse_test

import _root_.org.scalatest.FlatSpec
import _root_.org.scalatest.matchers.ShouldMatchers

import org.fud.optparse._

class OptionParserSpec extends FlatSpec with ShouldMatchers {
  val ARGUMENT_MISSING = "argument missing:"
  val INVALID_ARGUMENT = "invalid argument:"
  val INVALID_OPTION   = "invalid option:"
  val AMBIGUOUS_OPTION = "abmiguous option:"
  
  var results: Map[String, Any] = Map.empty
  val opts = new OptionParser
  opts.noArg("-x", "--expert", "Expert option") { () => results += "expert" -> true }
  opts.noArg("-h", "--help", "Display Help") { () => results += "help" -> true }
  opts.reqArg("-n", "--name", "Set Name") { name: String => results += "name" -> name }
  opts.reqArg("-s", "--size", "Set Size") { size: Int => results += "size" -> size }
  opts.optArg("-r", "--reset", "Reset..") { dir: Option[String] => results += "dir" -> dir.getOrElse("top")}
  opts.optArg("-a", "--at", "At..") { at: Option[Int] => results += "at" -> at.getOrElse(100)}
  case class Foo(s: String)
  opts.addConverter { s: String => Foo(s) }
  opts.reqArg("-f", "--foo", "Set Foo") { foo : Foo => results += "foo" -> foo}
  
  
  // ====================================================================================
  // ====================================================================================
  "OptionParser"  should "detect attempts to add an option for a type with no coverter" in {
    results = Map.empty
    val args = opts.parse(List())
    args should be ('empty)
    results should be ('empty)
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
  it should "detect attempts to add an option for a type with no converter" in {
    class SomeClass
    evaluating {
      opts.reqArg("-z", "--zoo", "Set Zoo") { zoo : SomeClass => results += "zoo" -> zoo}
    } should produce [RuntimeException]
  }    
  
}