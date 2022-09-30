package com.nulabinc.backlog.migration.common.messages

import java.nio.file.Path
import java.util.Locale

import com.nulabinc.backlog.migration.common.domain.mappings.{
  Mapping,
  MappingType,
  PriorityMapping,
  StatusMapping,
  UserMapping
}
import com.nulabinc.backlog.migration.common.errors._
import com.nulabinc.backlog.migration.common.formatters.Formatter
import com.osinka.i18n.{Lang, Messages}

object ConsoleMessages {
  private implicit val userLang: Lang =
    if (Locale.getDefault.equals(Locale.JAPAN)) Lang("ja") else Lang("en")

  lazy val empty: String      = Messages("common.empty")
  lazy val srcProduct: String = Messages("common.src")
  lazy val dstProduct: String = Messages("common.dst")
  lazy val statuses: String   = Messages("common.statuses")
  lazy val confirm: String    = Messages("cli.confirm")
  lazy val converted: String  = Messages("message.converted")

  def notLatestVersion(latest: String, current: String): String =
    s"""
       |--------------------------------------------------
       |${Messages("cli.warn.not.latest", latest, current)}
       |--------------------------------------------------
       """.stripMargin

  def confirmCanceled: String =
    s"""
       |--------------------------------------------------
       |${Messages("cli.cancel")}""".stripMargin

  def cliUnknownError(e: Throwable): String =
    s"${Messages("cli.error.unknown")}:${e.getMessage}"

  def cliRateLimitWarn: String =
    s"${Messages("cli.warn.rate_limit")}"

  def executed(key: String, value: String): String =
    Messages("message.executed", key, value)

  object Mappings {
    lazy val statusItem: String   = statuses
    lazy val priorityItem: String = Messages("common.priorities")
    lazy val userItem: String     = Messages("common.users")

    def needsSetup: String =
      Messages("cli.mapping.error.setup")

    def statusMappingMerged[A](filePath: Path, items: Seq[StatusMapping[A]])(implicit
        formatter: Formatter[StatusMapping[A]]
    ): String =
      mappingMerged(statusItem, filePath, items.map(formatter.format))

    def statusMappingNoChanges: String =
      mappingNoChanges(statusItem)

    def statusMappingCreated(filePath: Path): String =
      mappingFileCreated(statusItem, filePath)

    def mappingFileNotFound(): String =
      s"""
         |--------------------------------------------------
         |${Messages("cli.invalid_setup")}
       """.stripMargin

    def priorityMappingMerged[A](
        filePath: Path,
        items: Seq[PriorityMapping[A]]
    )(implicit formatter: Formatter[PriorityMapping[A]]): String =
      mappingMerged(priorityItem, filePath, items.map(formatter.format))

    def priorityMappingNoChanges: String =
      mappingNoChanges(priorityItem)

    def priorityMappingCreated(filePath: Path): String =
      mappingFileCreated(priorityItem, filePath)

    def userMappingMerged[A](filePath: Path, items: Seq[UserMapping[A]])(implicit
        formatter: Formatter[UserMapping[A]]
    ): String =
      mappingMerged(priorityItem, filePath, items.map(formatter.format))

    def userMappingNoChanges: String =
      mappingNoChanges(userItem)

    def userMappingCreated(filePath: Path): String =
      mappingFileCreated(userItem, filePath)

    def mappingFileIsBroken(itemName: String): String =
      s"""
         |--------------------------------------------------
         |${Messages("cli.mapping.error.broken_file", itemName)}
         |--------------------------------------------------
        """.stripMargin

    def validationError[A](error: MappingValidationError[A]): String = {
      val itemName = error.mappingType match {
        case MappingType.Priority => priorityItem
        case MappingType.Status   => statusItem
        case MappingType.User     => userItem
      }
      val errorStr = error.errors.map {
        case MappingValueIsEmpty(mapping) =>
          mappingItemIsEmpty(itemName, mapping)
        case MappingValueIsNotSpecified(mapping) =>
          mappingItemIsEmpty(itemName, mapping)
        case DestinationItemNotFound(value) =>
          mappingItemNotExist(itemName, value)
        case InvalidItemValue(required, input) =>
          mappingInvalidChoice(required, input)
      }

      s"""
         |${Messages("cli.mapping.error", itemName)}
         |--------------------------------------------------
         |${errorStr.mkString("\n")}
         |--------------------------------------------------""".stripMargin
    }

    def mappingFileNeedsFix(path: Path): String =
      s"""|--------------------------------------------------
          |${Messages("cli.mapping.fix_file", path)}""".stripMargin

    private def mappingItemIsEmpty[A](
        itemName: String,
        mapping: Mapping[A]
    ): String =
      s"- ${Messages("cli.mapping.error.empty.item", srcProduct, itemName, mapping.srcDisplayValue)}"

    private def mappingItemNotExist(itemName: String, value: String): String =
      s"- ${Messages("cli.mapping.error.not_exist.item", itemName, value, dstProduct)}"

    private def mappingInvalidChoice(required: String, input: String): String =
      s"- ${Messages("cli.mapping.error.invalid_choice", required, input)}"

    private def mappingMerged(
        itemName: String,
        filePath: Path,
        mappingStrings: Seq[(String, String)]
    ): String = {
      val formatted = mappingStrings.map {
        case (src, dst) if dst.isEmpty => s"- $src => $empty"
        case (src, dst)                => s"- $src => $dst"
      }

      s"""
         |--------------------------------------------------
         |${Messages("cli.mapping.merge_file", itemName, filePath)}
         |[${filePath.toAbsolutePath}]
         |${formatted.mkString("\n")}
         |--------------------------------------------------""".stripMargin
    }

    private def mappingNoChanges(itemName: String): String =
      s"""
         |--------------------------------------------------
         |${Messages("cli.mapping.no_change", itemName)}
         |--------------------------------------------------""".stripMargin

    private def mappingFileCreated(itemName: String, filePath: Path): String =
      s"""
         |--------------------------------------------------
         |${Messages("cli.mapping.output_file", itemName)}
         |[${filePath.toAbsolutePath}]
         |--------------------------------------------------""".stripMargin

  }

  object Validations {
    lazy val srcAccessError: String = accessError(srcProduct)
    lazy val dstAccessError: String = accessError(dstProduct)

    def accessError(itemName: String): String =
      Messages("cli.param.check.access", itemName)

    def disabledHost(itemName: String, url: String): String =
      Messages("cli.param.error.disable.host", itemName, url)

    def accessBlocked(itemName: String): String =
      Messages("cli.param.error.disable.access", itemName)

  }

  object Exports {
    lazy val start: String =
      s"""
         |${Messages("export.start")}
         |--------------------------------------------------""".stripMargin

    lazy val end: String =
      s"""--------------------------------------------------
         |${Messages("export.finish")}""".stripMargin
  }

  object Converts {
    lazy val start: String =
      s"""
         |${Messages("convert.start")}
         |--------------------------------------------------""".stripMargin

    lazy val end: String =
      s"""--------------------------------------------------
         |${Messages("convert.finish")}""".stripMargin

    lazy val projectUser: String = executed(Messages("common.project_user"), converted)
    lazy val projectKey: String  = executed(Messages("common.project_key"), converted)
  }

  object Imports {
    lazy val start: String =
      s"""
          |${Messages("import.start")}
          |--------------------------------------------------""".stripMargin

    lazy val finish: String =
      s"""|--------------------------------------------------
          |${Messages("import.finish")}""".stripMargin

    object Errors {
      lazy val suspend: String =
        s"""|--------------------------------------------------
            |${Messages("import.suspend")}""".stripMargin

      def failed(projectKey: String, errorMessage: String): String =
        Messages("import.error.failed.import", projectKey, errorMessage)

      def limitProject(projectKey: String): String =
        Messages("import.error.limit.project", projectKey)

      def projectNotJoin(projectKey: String): String =
        Messages("import.error.project.not.join", projectKey)

    }
  }
}
