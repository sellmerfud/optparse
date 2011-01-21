package org.fud.optparse_test

import _root_.org.scalatest.FlatSpec
import _root_.org.scalatest.matchers.ShouldMatchers

import org.fud.optparse.OptionParser

class OptionParserSpec extends FlatSpec with ShouldMatchers {

  val opts = new OptionParser
  var results: Map[String, Any] = Map.empty
  
  opts.noArg("-x", "--expert", "Expert option") { () => results += "expert" -> true }
  opts.noArg("-h", "--help", "Display Help") { () => results += "help" -> true }
  opts.string("-n", "--name", "Set Name") { name: String => results += "name" -> name }
  opts.int("-s", "--size", "Set Size") { size: Int => results += "size" -> size }
  
  // ====================================================================================
  // ====================================================================================
  "OptionParser" should "handle empty argv" in {
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
    evaluating {
      opts.parse(List("foo", "--name"))
    } should produce [Exception]
    
    evaluating {
      opts.parse(List("foo", "-n"))
    } should produce [Exception]
  }
  
  it should "detect invalid switches" in {
    evaluating {
      opts.parse(List("foo", "--asdf"))
    } should produce [Exception]
    
    evaluating {
      opts.parse(List("foo", "-X"))
    } should produce [Exception]
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
  
}