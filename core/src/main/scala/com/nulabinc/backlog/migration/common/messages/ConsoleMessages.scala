package com.nulabinc.backlog.migration.common.messages

import java.nio.file.Path
import java.util.Locale

import com.nulabinc.backlog.migration.common.domain.mappings.{Formatter, PriorityMapping, StatusMapping}
import com.osinka.i18n.{Lang, Messages}

object ConsoleMessages {
  private implicit val userLang: Lang = if (Locale.getDefault.equals(Locale.JAPAN)) Lang("ja") else Lang("en")

  val empty: String = Messages("common.empty")

  object Mappings {
    private lazy val statusItem = Messages("common.statuses")
    private lazy val priorityItem = Messages("common.priorities")

    def statusMappingMerged[A](filePath: Path, items: Seq[StatusMapping[A]])(implicit formatter: Formatter[StatusMapping[A]]): String =
      mappingMerged(statusItem, filePath, items.map(formatter.format))

    def statusMappingNoChanges: String =
      mappingNoChanges(statusItem)

    def statusMappingCreated(filePath: Path): String =
      mappingFileCreated(statusItem, filePath)

    def priorityMappingMerged[A](filePath: Path, items: Seq[PriorityMapping[A]])(implicit formatter: Formatter[PriorityMapping[A]]): String =
      mappingMerged(priorityItem, filePath, items.map(formatter.format))

    def priorityMappingNoChanges: String =
      mappingNoChanges(priorityItem)

    def priorityMappingCreated(filePath: Path): String =
      mappingFileCreated(priorityItem, filePath)

    private def mappingMerged(itemName: String, filePath: Path, mappingStrings: Seq[(String, String)]): String = {
      val formatted = mappingStrings.map {
        case (src, dst) if dst.isEmpty => s"- $src => $empty"
        case (src, dst) => s"- $src => $dst"
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
}
