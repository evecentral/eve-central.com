package com.evecentral.datainput

import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers

class UnifiedUploadMessageTest extends FunSuite with ShouldMatchers {

  val message = """
  {
  "resultType" : "orders",
  "version" : "0.1alpha",
  "uploadKeys" : [
    { "name" : "emk", "key" : "abc" },
    { "name" : "ec" , "key" : "def" }
  ],
  "generator" : { "name" : "Yapeal", "version" : "11.335.1737" },
  "currentTime" : "2011-10-22T15:46:00+00:00",
  "columns" : ["price","volRemaining","range","orderID","volEntered","minVolume","bid","issueDate","duration","stationID","solarSystemID"],
  "rowsets" : [
    {
      "generatedAt" : "2011-10-22T15:43:00+00:00",
      "regionID" : 10000065,
      "typeID" : 11134,
      "rows" : [
        [8999,1,32767,2363806077,1,1,false,"2011-12-03T08:10:59+00:00",90,60008692,30005038],
        [11499.99,10,32767,2363915657,10,1,false,"2011-12-03T10:53:26+00:00",90,60006970,null],
        [11500,48,32767,2363413004,50,1,false,"2011-12-02T22:44:01+00:00",90,60006967,30005039]
      ]
    },
    {
      "generatedAt" : "2011-10-22T15:42:00+00:00",
      "regionID" : 10000065,
      "typeID" : 11135,
      "rows" : [
        [8999,1,32767,2363806077,1,1,false,"2011-12-03T08:10:59+00:00",90,60008692,30005038],
        [11499.99,10,32767,2363915657,10,1,false,"2011-12-03T10:53:26+00:00",90,60006970,null],
        [11500,48,32767,2363413004,50,1,false,"2011-12-02T22:44:01+00:00",90,60006967,30005039]
      ]
    }
  ]
}"""

  test("Simple parse") {
    val i = new UnifiedUploadMessage(message)
    i.resultType should equal ("orders")
    i.columns should equal (List("price","volRemaining","range","orderID","volEntered","minVolume","bid","issueDate","duration","stationID","solarSystemID"))
  }

}
