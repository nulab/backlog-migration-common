package com.nulabinc.backlog.migration.common.utils

import java.util.Date

import com.mixpanel.mixpanelapi.{ClientDelivery, MessageBuilder, MixpanelAPI}
import org.json.JSONObject

/**
  * @author uchida
  */
object MixpanelUtil {

  def track(token: String, data: TrackingData) = {
    val props = new JSONObject()
    props.put("Source URL", data.srcUrl)
    props.put("Destination URL", data.dstUrl)
    props.put("Source Project Key", data.srcProjectKey)
    props.put("Destination Project Key", data.dstProjectKey)
    props.put("Source Space Created Date", data.srcSpaceCreated)
    props.put("Destination Space Created Date", data.dstSpaceCreated)
    props.put("Between Created Date And Migrated Date", diff(data.dstSpaceCreated))
    props.put("Product", data.product)

    val messageBuilder = new MessageBuilder(token)

    val distinctId = s"${data.envname}-${data.spaceId}-${data.userId}"
    val event      = messageBuilder.event(distinctId, "Project Migrated", props)

    val delivery = new ClientDelivery()
    delivery.addMessage(event)

    val mixpanel = new MixpanelAPI()
    mixpanel.deliver(delivery)

  }

  private[this] def diff(dstSpaceCreated: String): Long = {
    val dstSpaceCreatedTime = DateUtil.tryIsoParse(Some(dstSpaceCreated)).getTime()
    val nowTime             = (new Date()).getTime()
    (nowTime - dstSpaceCreatedTime) / (1000 * 60 * 60 * 24)
  }

}
case class TrackingData(product: String,
                        envname: String,
                        spaceId: Long,
                        userId: Long,
                        srcUrl: String,
                        dstUrl: String,
                        srcProjectKey: String,
                        dstProjectKey: String,
                        srcSpaceCreated: String,
                        dstSpaceCreated: String)
