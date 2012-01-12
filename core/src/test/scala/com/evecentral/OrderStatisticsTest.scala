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
    val os = OrderStatistics(orders)
    os.avg should equal(1)
    os.volume should equal(1)
    os.wavg should equal(1)
    os.stdDev should equal(0)
    os.variance should equal(0)
    os.fivePercent should equal(1)
    os.median should equal(1)
  }
  
  test("Zero stats") {
    val orders = List()
    val os = OrderStatistics(orders)
    os.avg should equal(0)
    os.volume should equal(0)
    os.stdDev should equal(0)
    os.variance should equal(0)
    os.median should equal(0)
    os.fivePercent should equal(0)
  }
  
  test("Two stat") {
    val orders = List(makeOrder(1,1), makeOrder(2,1))
    val os = OrderStatistics(orders)
    os.avg should equal(1.5)
    os.wavg should equal(1.5)
    os.median should equal(1.5)
  }

  test("Three stat") {
    val orders = List(makeOrder(1,1), makeOrder(2,1), makeOrder(3,1))
    val os = OrderStatistics(orders)
    os.avg should equal(2)
    os.wavg should equal(2)
    os.median should equal(2)
  }
  
  test("Two state uneven") {
    val orders = List(makeOrder(1,1), makeOrder(2,1000))
    val os = OrderStatistics(orders)
    os.median should equal(2)
    os.fivePercent should be (1.999 plusOrMinus 0.0001)
  }
  
  test("Lots of orders") {
    var i = 0
    val num = 150000
    var orders = List[MarketOrder]()
    while(i < num) {
      orders =List(makeOrder(1,1)) ++  orders
        i += 1
    }
    val os = OrderStatistics(orders)
    os.volume should equal (num)
    os.avg should equal (1)
    os.median should equal(1)
  }
}
