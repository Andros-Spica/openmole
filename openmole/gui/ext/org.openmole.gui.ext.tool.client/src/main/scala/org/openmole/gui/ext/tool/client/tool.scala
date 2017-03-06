package org.openmole.gui.ext.tool

/*
 * Copyright (C) 01/04/16 // mathieu.leclaire@openmole.org
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY, without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import fr.iscpif.scaladget.stylesheet.all._
import fr.iscpif.scaladget.stylesheet.{ all ⇒ sheet }
import org.scalajs.dom

import scalatags.JsDom.all._
import scalatags.JsDom.{ styles, svgAttrs }

package object client {

  lazy val omsheet = this

  //GENERAL
  lazy val grey: ModifierSeq = Seq(
    color("grey"),
    opacity := 0.8
  )

  lazy val BLUE = "#3086b5"

  lazy val RED = "#c83737"

  lazy val DARK_GREY = "#222"

  lazy val LIGHT_GREY = "#9d9d9d"

  lazy val VERY_LIGHT_GREY = "#e7e7e7"

  lazy val FUN_GREY = "#cccccc"

  lazy val WHITE = "white"

  lazy val textCenter = ms("text-center")
  lazy val textLeft = ms("text-left")
  lazy val textRight = ms("text-right")
  lazy val textJustify = ms("text-justify")
  lazy val textNoWrap = ms("text-nowrap")

  lazy val tableTag: ModifierSeq = Seq(
    sheet.marginLeft(3),
    fontSize := 14,
    relativePosition,
    top := 4
  )

  def color(col: String): ModifierSeq = styles.color := col

  def bold(col: String): ModifierSeq = bold +++ (color(col))

  val bold: ModifierSeq = Seq(
    fontWeight := "bold"
  )

  lazy val relativePosition: ModifierSeq = position := "relative"

  lazy val absolutePosition: ModifierSeq = position := "absolute"

  lazy val fixedPosition: ModifierSeq = position := "fixed"

  lazy val centerElement: ModifierSeq = Seq(
    display := "table",
    margin := "0 auto",
    color(LIGHT_GREY)
  )

  lazy val centerFileToolBar: ModifierSeq = Seq(
    display := "table",
    margin := "0 auto",
    color(LIGHT_GREY)
  )

  lazy val shutdown: ModifierSeq = Seq(
    color(DARK_GREY),
    fontStyle := "italic",
    sheet.paddingTop(40),
    sheet.marginLeft(-25)
  )
  // SCRIPT CLIENT
  lazy val mainNav0: ModifierSeq = Seq(
    sheet.paddingLeft(0),
    borderColor := DARK_GREY,
    zIndex := 10
  )

  lazy val mainNav370: ModifierSeq = Seq(
    sheet.paddingLeft(370),
    borderColor := DARK_GREY,
    zIndex := 10
  )

  lazy val fileChevronStyle: ModifierSeq = Seq(
    lineHeight := "10px",
    top := 10,
    left := -30,
    sheet.paddingRight(20)
  )

  lazy val mainNav: ModifierSeq = Seq(
    left := 370,
    fontSize := 20
  )
  lazy val connectionTabOverlay: ModifierSeq = Seq(
    background := "white none repeat scroll 0 0",
    color(WHITE),
    height := "100%",
    styles.left := 0,
    absolutePosition,
    width := "100%",
    zIndex := 1100,
    top := 0
  )

  lazy val openmoleLogo: ModifierSeq = Seq(
    fixedPosition,
    width := 600,
    top := -30,
    zIndex := "-1",
    styles.right := "calc(50% - 250px)"
  )

  lazy val displayOff: ModifierSeq = Seq(
    display := "none"
  )

  lazy val fullpanel: ModifierSeq = Seq(
    height := "100%",
    width := "100%"
  )

  lazy val panelReduce: ModifierSeq = Seq(
    styles.left := 330,
    sheet.paddingRight(340),
    fixedPosition,
    top := 60,
    transition := "all 0.1 s ease - out 0 s",
    width := "99%",
    height := "calc (100 % -140 px)"
  )

  lazy val panelOpen: ModifierSeq = Seq(
    styles.left := 0
  )

  lazy val centerpanel: ModifierSeq = Seq(
    height := "89%",
    sheet.paddingLeft(15),
    sheet.paddingRight(15),
    relativePosition,
    top := 30,
    width := "100%"
  )

  lazy val leftpanel: ModifierSeq = Seq(
    background := s"$DARK_GREY none repeat scroll 0 0",
    height := "100%",
    styles.left := -320,
    opacity := 1,
    overflowY := "auto",
    sheet.paddingLeft(7),
    absolutePosition,
    top := 37,
    transition := "all 0.1 s ease - out 0 s",
    width := 320
  )

  lazy val logoVersion: ModifierSeq = Seq(
    width := 200,
    position := "fixed",
    bottom := 0,
    styles.right := 50,
    zIndex := -2
  )

  lazy val resetBlock: ModifierSeq = Seq(
    relativePosition,
    pointer,
    floatRight,
    top := 20,
    zIndex := 10
  )

  lazy val closeBanner: ModifierSeq = Seq(
    floatRight,
    pointer,
    relativePosition,
    top := 18,
    zIndex := 10,
    sheet.paddingRight(15),
    fontSize := 20
  )

  lazy val fixed: ModifierSeq = Seq(
    fixedPosition,
    width := "100%"
  )

  lazy val absoluteFullWidth: ModifierSeq = Seq(
    absolutePosition,
    width := "100%"
  )

  lazy val shutdownButton: ModifierSeq = Seq(
    fontSize := 20,
    verticalAlign := "middle",
    sheet.marginLeft(10),
    zIndex := 1101
  )

  lazy val resetPassword: ModifierSeq = Seq(
    fontSize := 12,
    color(LIGHT_GREY),
    cursor := "pointer"
  )

  lazy val connectionBlock: ModifierSeq = Seq(
    display := "inline-block",
    textAlign := "right",
    float := "right",
    sheet.marginRight(15)
  )

  lazy val textVersion: ModifierSeq = Seq(
    color("grey"),
    position := "fixed",
    textAlign := "center",
    right := 27,
    zIndex := -1,
    bottom := 5
  )

  //TREENODE PANEL
  lazy val fileInfo: ModifierSeq = Seq(
    sheet.floatRight,
    absolutePosition,
    width := 100,
    right := 2,
    sheet.marginTop(6),
    textAlign := "right"
  )

  lazy val fileSize: ModifierSeq = Seq(
    color("lightgray"),
    fontSize := 10
  )

  lazy val file: ModifierSeq = Seq(
    color(WHITE),
    display := "inline-block",
    height := 20,
    textDecoration := "none"
  )

  lazy val dir: ModifierSeq = Seq(
    backgroundColor := BLUE,
    color(WHITE),
    display := "inline-block",
    height := 20,
    sheet.marginBottom(3),
    sheet.marginTop(2),
    sheet.marginLeft(1),
    padding := 2,
    width := 20,
    borderRadius := "4px"
  )

  lazy val fileNameOverflow: ModifierSeq = Seq(
    color(WHITE),
    whiteSpace := "nowrap",
    overflow := "hidden",
    pointer,
    width := 245,
    textOverflow := "ellipsis",
    sheet.paddingBottom(3)
  )

  lazy val fileIcon: ModifierSeq = Seq(
    sheet.paddingLeft(5),
    sheet.paddingTop(3),
    fontSize := 8,
    sheet.marginBottom(-16),
    zIndex := 2
  )

  lazy val divAlertPosition: ModifierSeq = Seq(
    floatRight,
    sheet.marginRight(70),
    sheet.marginTop(20)
  )

  lazy val treeprogress: ModifierSeq = Seq(
    sheet.marginTop(20),
    width := "100%"
  )

  lazy val message: ModifierSeq = Seq(
    color("#999"),
    fontStyle := "italic",
    sheet.marginLeft(25),
    sheet.marginTop(38)
  )

  lazy val fileSelectionOverlay: ModifierSeq = Seq(
    right := 0,
    pointer,
    width := 360,
    height := 24,
    sheet.marginLeft(-5),
    sheet.marginBottom(1),
    borderRadius := "2px"
  )

  lazy val fileSelected: ModifierSeq = Seq(
    backgroundColor := "#a6bf26"
  ) +++ fileSelectionOverlay

  lazy val fileSelectedForDeletion: ModifierSeq = Seq(
    backgroundColor := "#d9534f"
  ) +++ fileSelectionOverlay

  lazy val fileSelectionMessage: ModifierSeq = Seq(
    floatRight,
    padding := 4,
    fontSize := 13,
    right := 6,
    color(WHITE)
  )

  lazy val pasteLabel: ModifierSeq = Seq(
    relativePosition,
    top := 25,
    pointer,
    fontSize := 12,
    width := 50
  )

  lazy val moreEntries: ModifierSeq = Seq(
    backgroundColor := WHITE,
    margin := 10,
    borderRadius := "3px",
    textAlign := "center"
  )

  lazy val moreEntriesText: ModifierSeq = Seq(
    color("#444"),
    fontSize := 12,
    fontStyle := "italic",
    padding := 7
  )

  //TREENODE TABS
  lazy val editingElement: ModifierSeq = Seq(
    fixedPosition,
    styles.right := 200,
    top := 15,
    zIndex := 10
  )

  lazy val tabContent: ModifierSeq = Seq(
    sheet.marginTop(-1),
    relativePosition,
    width := "100%"
  )

  lazy val playTabOverlay: ModifierSeq = Seq(
    color(WHITE),
    height := "100%",
    absolutePosition,
    width := "100%",
    background := BLUE,
    opacity := 0.9,
    width := "100%",
    zIndex := 10,
    bottom := 3,
    borderBottomLeftRadius := "5px",
    borderBottomRightRadius := "5px",
    borderTopRightRadius := "5px"
  )

  lazy val overlayElement: ModifierSeq = Seq(
    absolutePosition,
    width := "100%",
    color(WHITE),
    fontWeight := "bold",
    sheet.paddingTop(100),
    fontSize := 25,
    zIndex := 25,
    textAlign := "center"
  )

  lazy val executionElement: ModifierSeq = Seq(
    relativePosition,
    floatRight,
    right := 200,
    padding := 15,
    margin := "auto",
    width := 50,
    zIndex := 18
  )

  lazy val executionTable: ModifierSeq = Seq(
    backgroundColor := DARK_GREY,
    color(WHITE)
  )

  lazy val monospace: ModifierSeq = fontFamily := "monospace"

  def fileList: ModifierSeq = {
    Seq(
      height := dom.window.innerHeight - 195, //nbElements * 21, //  <-- Select the height of the body
      absolutePosition,
      fontSize := 14,
      listStyleType := "none",
      sheet.marginTop(50),
      sheet.paddingBottom(30),
      sheet.marginLeft(-7),
      sheet.paddingLeft(10),
      width := 370,
      overflowY := "auto",
      zIndex := -1
    )
  }

  lazy val tabClose: ModifierSeq = Seq(
    relativePosition,
    fontSize := 17,
    right := -7,
    top := 4
  )

  //EDITOR
  lazy val editorContainer: ModifierSeq = Seq(
    padding := 0,
    relativePosition,
    height := "100%",
    width := "100%"
  )

  lazy val activeTab: ModifierSeq = Seq(
    backgroundColor := FUN_GREY,
    bold
  )

  lazy val unActiveTab: ModifierSeq = Seq(
    border := s"1px solid $VERY_LIGHT_GREY"
  )

  //PANELS
  def panelWidth(ratio: Int): ModifierSeq = Seq(
    width := s"${ratio.toString}%",
    maxWidth := 1250
  )

  //MARKET PANEL
  lazy val docEntry: ModifierSeq = Seq(
    color(WHITE),
    backgroundColor := DARK_GREY,
    borderRadius := "4px",
    sheet.marginTop(3),
    sheet.paddingTop(6),
    verticalAlign := "middle",
    sheet.paddingBottom(4),
    sheet.paddingLeft(15),
    minHeight := 46
  )

  lazy val docTitleEntry: ModifierSeq = Seq(
    fontWeight := "bold",
    cursor := "pointer",
    sheet.paddingTop(6)
  )

  //EXECUTION PANEL
  lazy val panelHeaderSettings: ModifierSeq = Seq(
    floatRight,
    sheet.marginRight(200),
    sheet.marginTop(-25)
  )

  lazy val execOutput: ModifierSeq = Seq(
    sheet.marginRight(-10),
    sheet.marginTop(5)
  )

  lazy val execLevel: ModifierSeq = Seq(
    sheet.marginRight(-25),
    sheet.marginTop(-3)
  )

  lazy val errorTable: ModifierSeq = Seq(
    lineHeight := "30px",
    borderWidth := "0.1em",
    borderStyle := "solid",
    borderColor := "#ccc",
    borderLeft := 0,
    borderRight := 0
  )

  lazy val executionVisible: ModifierSeq = Seq(
    color(BLUE),
    fontWeight := "bold"
  )

  def executionState(state: String): ModifierSeq = Seq(
    state match {
      case "failed"   ⇒ color("#CC3A36")
      case "running"  ⇒ color("yellow")
      case "finished" ⇒ color("#a6bf26")
      case "canceled" ⇒ color("orange")
      case _          ⇒ color(WHITE)
    },
    fontWeight := "bold"
  )

  lazy val settingsButton: ModifierSeq = Seq(
    width := 45,
    height := 32,
    top := 20,
    padding := 5
  )

  lazy val closeDetails: ModifierSeq = Seq(
    display := "inline-block",
    floatRight,
    sheet.paddingRight(5),
    pointer,
    sheet.marginTop(-28),
    color(DARK_GREY),
    fontSize := 22,
    fontWeight := "bold",
    opacity := 0.4
  )

  lazy val environmentErrorBadge: ModifierSeq = Seq(
    color(DARK_GREY),
    backgroundColor := FUN_GREY
  )

  //OPTON DIVS
  lazy val optionsdiv: ModifierSeq = Seq(
    relativePosition,
    sheet.marginRight(10),
    top := -3
  )

  // SELECTS
  lazy val selectFilter: ModifierSeq = Seq(
    sheet.marginTop(6),
    fontSize := 14,
    sheet.paddingLeft(5),
    borderBottomRightRadius := 0,
    borderBottomLeftRadius := 0
  )

  // ALERT PANELS

  lazy val alertOverlay: ModifierSeq = Seq(
    background := s"$DARK_GREY none repeat scroll 0 0",
    opacity := 0.95,
    color(WHITE),
    height := "100%",
    absolutePosition,
    width := "100%",
    zIndex := 1200,
    textAlign := "right",
    top := 0
  )

  // POSTIONING
  lazy val fullPageZone: ModifierSeq = Seq(
    top := 0,
    height := "100%",
    width := "100%"
  )

  lazy val fileZone: ModifierSeq = Seq(
    top := 10,
    height := "100%",
    width := 370
  )

  lazy val topZone: ModifierSeq = Seq(
    top := 0,
    height := 100,
    width := 100
  )

  lazy val centerPage: ModifierSeq = Seq(
    position := "fixed",
    top := "45%",
    styles.left := "50%",
    minWidth := 250,
    svgAttrs.transform := "translate (-50%,-50%)"
  )

  lazy val relativeCenter: ModifierSeq = Seq(
    relativePosition,
    top := "50%",
    textAlign := "center"
  )

  lazy val rightPage: ModifierSeq = Seq(
    styles.right := 0,
    margin := "10 20"
  )

  // AUTHENTICATION PANEL
  lazy val certificate: ModifierSeq = Seq(
    width := 130,
    textAlign := "center",
    sheet.marginTop(40)
  )

  // ENVIRONMENT ERROR PANEL
  lazy val environmentPanelError: ModifierSeq = Seq(
    backgroundColor := WHITE,
    margin := "10 10 0",
    padding := 10
  )

  //MODEL WIZARD PANEL
  lazy val modelNameInput: ModifierSeq = Seq(
    height := 34,
    width := 150,
    borderRadius := "1px"
  )

  lazy val rightBlock: ModifierSeq = Seq(
    width := "70%",
    floatRight
  )

  lazy val onecolumn: ModifierSeq = Seq(
    floatLeft,
    width := "100%"
  )

  lazy val twocolumns: ModifierSeq = Seq(
    floatLeft,
    width := "50%"
  )

  lazy val modelIO: ModifierSeq = Seq(
    relativePosition,
    left := "50%",
    sheet.marginLeft(-60),
    sheet.paddingBottom(20)
  )

  // PLUGIN PANEL
  lazy val uploadPlugin: ModifierSeq = Seq(
    sheet.marginRight(50),
    sheet.paddingBottom(30),
    width := 45,
    height := 30
  )

  lazy val pluginRight: ModifierSeq = Seq(
    right := -5,
    sheet.marginTop(-9)
  )

  lazy val spinnerStyle: ModifierSeq = Seq(
    backgroundColor := DARK_GREY,
    textAlign := "center",
    borderRadius := "4px"
  )

  lazy val dateStyle: ModifierSeq = Seq(
    absolutePosition,
    fontStyle := "italic",
    sheet.paddingTop(6),
    floatRight,
    right := 50,
    color("lightgrey")
  )

  lazy val dropdownError: ModifierSeq = Seq(
    // margin := 10,
    color(DARK_GREY),
    relativePosition,
    borderRadius := 5,
    fontSize := 11,
    border := "1px solid #ccc",
    padding := 10,
    lineHeight := "0.5cm",
    wordWrap := "break-word"
  )

  lazy val fixedTable: ModifierSeq = Seq(
    tableLayout := "fixed",
    width := "100%"
  )

  //FILE TOOL BAR
  lazy val selectedTool: ModifierSeq = Seq(
    opacity := 1,
    svgAttrs.transform := "scale(1.2)",
    color(BLUE)
  )

  lazy val borderRightFlat: ModifierSeq = Seq(
    borderBottomRightRadius := 0,
    borderTopRightRadius := 0
  )

  lazy val smallInput: ModifierSeq = Seq(
    width := 60,
    sheet.paddingTop(5),
    sheet.paddingLeft(3),
    textAlign := "center",
    height := (28),
    fontSize := 14
  )

  lazy val filterElement: ModifierSeq = Seq(
    sheet.floatLeft,
    height := 30,
    sheet.paddingLeft(4),
    fontSize := 12
  )

  lazy val selectedButton: ModifierSeq = Seq(
    sheet.btn,
    backgroundColor := "#e3dbdb"
  )

  lazy val sortingBar: ModifierSeq = Seq(
    relativePosition,
    height := 20,
    right := -240,
    top := -120
  )

  lazy val labelStyle: ModifierSeq = Seq(
    sheet.marginTop(4),
    color(WHITE),
    width := "auto",
    fontSize := 14
  )

  lazy val tdStyle: ModifierSeq = Seq(
    colMD(2),
    sheet.marginBottom(8),
    sheet.paddingLeft(5),
    width := "auto",
    height := 25
  )

  //TOOLTIP
  lazy val warningTooltip: ModifierSeq = Seq(
    absolutePosition,
    display := "inline-block",
    width := "auto",
    maxWidth := 200,
    height := "auto",
    padding := 8,
    borderRadius := "4px",
    backgroundColor := "pink",
    color("red"),
    boxShadow := "0 8px 6px -6px black"
  )

  //DOC 
  lazy val greenBold: ModifierSeq = Seq(
    color("#a6bf26"),
    fontWeight := "bold"
  )

  lazy val labelInLine: ModifierSeq = Seq(
    relativePosition,
    display := "inline"
  )

  lazy val glyphText: ModifierSeq = Seq(
    color("#a6bf26"),
    fontSize := 22,
    sheet.paddingRight(5)
  )

  lazy val docContent: ModifierSeq = Seq(
    opacity := 0.95,
    padding := "5 50"
  )

  //BANNER ALERT
  lazy val bannerAlert: ModifierSeq = Seq(
    width := "100%",
    absolutePosition,
    height := 60,
    zIndex := 5
  )

  lazy val bannerAlertInner: ModifierSeq = Seq(
    width := "100%",
    relativePosition,
    color(WHITE),
    opacity := 0.9,
    paddingAll(top = 10, left = 15, bottom = 15)

  )

}
