package com.evecentral

import com.evecentral.dataaccess.MarketOrder

import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers

class OrderStatisticsTest extends FunSuite with ShouldMatchers {
  
  def makeOrder(price: Double, vol: Int) = {
    MarketOrder(34, 1, price, false, null, null, null, 1, vol, vol, 1, null, null)
  }
  
  test("Single Stats") {
    val orders = List[MarketOrder](makeOrder(1.0, 1))
    val os = new OrderStatistics(orders)
    os.avg should equal(1)
    os.volume should equal(1)
    os.wavg should equal(1)
    os.stdDev should equal(0)
    os.variance should equal(0)
    os.fivePercent should equal(1)
    os.median should equal(1)
  }
}
