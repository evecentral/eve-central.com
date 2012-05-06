package com.evecentral.datainput

import cc.spray.RequestContext

case class OldUploadPayload(ctx: RequestContext, typename: Option[String], userid: Option[String],
                            data: String, typeid: Option[String], region: Option[String])
