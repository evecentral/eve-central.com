package com.evecentral.frontend

import com.evecentral.frontend.Formatter
import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers

class TestFormatter extends FunSuite with ShouldMatchers {

  test("Passthrough") {
    Formatter.price(10) should equal("10.00")
  }
  
  test("Millions") {
    Formatter.price(1000000) should equal ("1.00M")
  }
  
  test("Thousands") {
    Formatter.price(10000) should equal ("10.00K")
  }
}
