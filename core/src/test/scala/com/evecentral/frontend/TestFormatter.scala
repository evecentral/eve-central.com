package com.evecentral.frontend

import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers

class TestFormatter extends FunSuite with ShouldMatchers {

  test("Passthrough") {
    Formatter.price(10) should equal("10.00")
  }
  
  test("Millions") {
    Formatter.price(1000000) should equal ("1.00m")
    Formatter.price(990999) should equal ("0.99m")
  }
  
  test("Thousands") {
    Formatter.price(10000) should equal ("10.00k")
  }

  test("Billions") {
    Formatter.price(10000000000.0) should equal ("10.00b")
  }
}
