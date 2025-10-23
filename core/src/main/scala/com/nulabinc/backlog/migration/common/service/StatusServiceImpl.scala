package com.nulabinc.backlog.migration.common.service

import java.util.Locale
import javax.inject.Inject

import com.nulabinc.backlog.migration.common.client.BacklogAPIClient
import com.nulabinc.backlog.migration.common.domain.{
  BacklogCustomStatus,
  BacklogDefaultStatus,
  BacklogProjectKey,
  BacklogStatus,
  BacklogStatusName,
  BacklogStatuses,
  Id
}
import com.nulabinc.backlog.migration.common.utils.Logging
import com.nulabinc.backlog4j.BacklogAPIException
import com.nulabinc.backlog4j.Project.CustomStatusColor
import com.nulabinc.backlog4j.api.option.{AddStatusParams, UpdateOrderOfStatusParams}

import scala.jdk.CollectionConverters._
import scala.util.Try

/**
 * @author
 *   uchida
 */
class StatusServiceImpl @Inject() (
    backlog: BacklogAPIClient,
    projectKey: BacklogProjectKey
) extends StatusService
    with Logging {

  override def allStatuses(): BacklogStatuses =
    BacklogStatuses(
      allStatusesForExport()
        .append(defaultStatusesJa.values)
        .append(defaultStatusesEn.values)
        .values
        .distinctBy(_.name)
    )

  override def allStatusesForExport(): BacklogStatuses =
    Try {
      BacklogStatuses(
        backlog.getStatuses(projectKey).asScala.toSeq.map(BacklogStatus.from)
      )
    }.recover {
      case ex: BacklogAPIException if ex.getMessage.contains("No such project") =>
        defaultStatuses()
      case ex =>
        throw ex
    }.getOrElse(defaultStatuses())

  override def add(status: BacklogCustomStatus): BacklogCustomStatus = {
    val added = backlog.addStatus(
      new AddStatusParams(
        projectKey.value,
        status.name.trimmed,
        CustomStatusColor.strValueOf(status.color)
      )
    )

    BacklogCustomStatus.from(added)
  }

  override def updateOrder(ids: Seq[Id[BacklogStatus]]): Unit =
    Try {
      backlog.updateOrderOfStatus(
        new UpdateOrderOfStatusParams(projectKey.value, ids.map(_.value).asJava)
      )
    }.recover {
      case ex: BacklogAPIException if ex.getMessage.contains("Undefined resource") =>
        logger.warn("Your backlog doesn't support the updateOrder API", ex)
      case ex =>
        logger.error(s"UpdateOrder API error. Message: ${ex.getMessage}", ex)
        throw ex
    }.getOrElse(())

  override def remove(id: Id[BacklogStatus]): Unit =
    backlog.removeStatus(projectKey.value, id.value, 1) // Any status id is OK

  private val defaultStatusesJa: BacklogStatuses = BacklogStatuses(
    Seq(
      BacklogDefaultStatus(Id(1), BacklogStatusName("未対応"), 1000),
      BacklogDefaultStatus(Id(2), BacklogStatusName("処理中"), 2000),
      BacklogDefaultStatus(Id(3), BacklogStatusName("処理済み"), 3000),
      BacklogDefaultStatus(Id(4), BacklogStatusName("完了"), 4000)
    )
  )

  private val defaultStatusesEn: BacklogStatuses = BacklogStatuses(
    Seq(
      BacklogDefaultStatus(Id(1), BacklogStatusName("Open"), 1000),
      BacklogDefaultStatus(Id(2), BacklogStatusName("In Progress"), 2000),
      BacklogDefaultStatus(Id(3), BacklogStatusName("Resolved"), 3000),
      BacklogDefaultStatus(Id(4), BacklogStatusName("Closed"), 4000)
    )
  )

  private def defaultStatuses(): BacklogStatuses = {
    // アプリケーション起動時にconfファイルを参照してLocale.setDefaultが呼び出されている
    Locale.getDefault match {
      case Locale.JAPAN => defaultStatusesJa
      case _            => defaultStatusesEn
    }
  }

}
