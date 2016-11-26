package org.openmole.gui.client.core

/*
 * Copyright (C) 07/10/15 // mathieu.leclaire@openmole.org
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import fr.iscpif.scaladget.api.Select.SelectElement
import org.openmole.gui.client.core.alert.AlertPanel
import org.openmole.gui.client.core.files._
import org.openmole.gui.ext.data._
import org.openmole.gui.misc.js.{ OptionsDiv, OMTags }
import autowire._
import org.scalajs.dom.html.TextArea
import org.openmole.gui.client.core.files.TreeNode._
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import org.openmole.gui.client.core.files.treenodemanager.{ instance ⇒ manager }
import org.scalajs.dom.raw.{ HTMLDivElement, HTMLInputElement }
import org.openmole.gui.misc.js.JsRxTags._
import rx._
import org.openmole.gui.shared.Api
import scalatags.JsDom.{ TypedTag, tags ⇒ tags }
import scalatags.JsDom.all._
import fr.iscpif.scaladget.api.{ BootstrapTags ⇒ bs }
import fr.iscpif.scaladget.api.Select
import fr.iscpif.scaladget.api.Select._
import Waiter._
import org.openmole.gui.ext.data.DataUtils._
import fr.iscpif.scaladget.stylesheet.{ all ⇒ sheet }
import org.openmole.gui.misc.utils.stylesheet._
import sheet._
import bs._

class ModelWizardPanel extends ModalPanel {
  implicit val ctx: Ctx.Owner = Ctx.Owner.safe()
  lazy val modalID = "modelWizardPanelID"

  def onOpen() = {}

  def onClose() = {}

  sealed trait VariableRole[T] {
    def content: T

    def clone(t: T): VariableRole[T]

    def switch: VariableRole[T]
  }

  case class Input[T](content: T) extends VariableRole[T] {
    def clone(otherT: T) = Input(otherT)

    def switch = Output(content)
  }

  case class Output[T](content: T) extends VariableRole[T] {
    def clone(otherT: T): VariableRole[T] = Output(otherT)

    def switch = Input(content)
  }

  case class CommandInput[T](content: T) extends VariableRole[T] {
    def clone(otherT: T) = CommandInput(otherT)

    def switch = CommandOutput(content)
  }

  case class CommandOutput[T](content: T) extends VariableRole[T] {
    def clone(otherT: T): VariableRole[T] = CommandOutput(otherT)

    def switch = CommandInput(content)
  }

  implicit def pairToLine(variableElement: VariableRole[VariableElement]): Reactive = buildReactive(variableElement)

  implicit def stringToOptionString(s: String): Option[String] = if (s.isEmpty) None else Some(s)

  val filePath: Var[Option[SafePath]] = Var(None)
  val transferring: Var[ProcessState] = Var(Processed())
  val labelName: Var[Option[String]] = Var(None)
  val launchingCommand: Var[Option[LaunchingCommand]] = Var(None)
  val currentReactives: Var[Seq[Reactive]] = Var(Seq())
  val updatableTable: Var[Boolean] = Var(true)
  val bodyContent: Var[Option[TypedTag[HTMLDivElement]]] = Var(None)
  val resources: Var[Resources] = Var(Resources.empty)
  val currentTab: Var[Int] = Var(0)
  val autoMode = Var(true)
  val upButton: Var[HTMLDivElement] = Var(tags.div().render)
  val fileToUploadPath: Var[Option[SafePath]] = Var(None)
  val targetPath: Var[Option[SafePath]] = Var(None)
  val fromArchive: Var[Boolean] = Var(false)

  val modelSelector: Select[SafePath] = Seq[SafePath]().select(
    None, SafePath.naming, btn_default, () ⇒ {
    fileToUploadPath() = modelSelector.content.now
    onModelChange
  }
  )

  val methodSelector: Select[JarMethod] = Seq[JarMethod]().select(
    None, (jm: JarMethod) ⇒ jm.name, btn_default, () ⇒ {
    methodSelector.content.now.foreach { s ⇒
      setJavaLaunchingCommand(s.value)
    }
  }
  )

  val classSelector: Select[FullClass] = Seq[FullClass]().select(
    None, (fc: FullClass) ⇒ fc.name, btn_default, () ⇒ {
    classSelector.content.now.foreach { c ⇒
      setMethodSelector(c.value)
    }
  }
  )

  def onModelChange = {
    fileToUploadPath.now.foreach {
      m ⇒
        val fileType: FileType = m
        fileType match {
          case _: CodeFile ⇒ setLaunchingCommand(m)
          case a: Archive ⇒
            if (a.language == JavaLikeLanguage()) getJarClasses(m)
          case _ ⇒
        }
    }
  }

  def setMethodSelector(classContent: FullClass) = {
    fileToUploadPath.now.map {
      fn ⇒
        OMPost[Api].methods(fn, classContent.name).call().foreach {
          b ⇒
            methodSelector.setContents(b)
            b.headOption.map {
              setJavaLaunchingCommand
            }
        }
    }
  }

  def setScritpName = scriptNameInput.value = filePath.now.map {
    _.name.split('.').head
  }.getOrElse("model")

  def setJavaLaunchingCommand(method: JarMethod) = {
    val lc = JavaLaunchingCommand(method, method.args, method.ret.map {
      Seq(_)
    }.getOrElse(Seq()))
    setReactives(lc)
    launchingCommand() = Some(lc)
    setScritpName
  }

  val commandArea: TextArea = textArea(3).render
  val autoModeCheckBox = checkbox(autoMode.now)(onchange := {
    () ⇒
      autoMode() = !autoMode.now
  })

  val scriptNameInput = bs.input()(modelNameInput, placeholder := "Script name").render
  val languages: Seq[Language] = Seq(Binary(), JavaLikeLanguage(), PythonLanguage(), NetLogoLanguage(), RLanguage())
  val codeSelector: Select[Language] = languages.select(Some(Binary()), (l: Language) ⇒ l.name, btn_default)

  launchingCommand.triggerLater {
    if (autoMode.now) {
      commandArea.value = launchingCommand.now.map {
        _.fullCommand
      }.getOrElse("")
    }
  }

  currentReactives.triggerLater {
    if (updatableTable.now) setBodyContent
  }

  def buttonStyle(i: Int): ModifierSeq = {
    if (i == currentTab.now) btn_primary
    else btn_default
  } +++ sheet.marginRight(20)

  def nbInputs = inputs(currentReactives.now).size

  def nbOutputs = currentReactives.now.size - nbInputs

  def inputs(reactives: Seq[Reactive]): Seq[VariableRole[VariableElement]] = {
    reactives.map {
      _.role
    }.collect {
      case x: Input[VariableElement]        ⇒ x
      case x: CommandInput[VariableElement] ⇒ x
    }
  }

  def outputs(reactives: Seq[Reactive]): Seq[VariableRole[VariableElement]] =
    reactives.map {
      _.role
    }.collect {
      case x: Output[VariableElement]        ⇒ x
      case x: CommandOutput[VariableElement] ⇒ x
    }

  def getReactive(index: Int): Option[Reactive] = currentReactives.now.filter {
    _.index == index
  }.headOption

  def setUpButton = upButton() =
    div(ms("modelWizardDivs"))(
      div(maxWidth := 250)(
        label(
          sheet.paddingTop(5) +++ certificate +++ "inputFileStyle",
          transferring.withTransferWaiter {
            _ ⇒
              div(
                fileInput((fInput: HTMLInputElement) ⇒ {
                  if (fInput.files.length > 0) {
                    emptyJARSelectors
                    fileToUploadPath() = None
                    resources() = Resources.empty
                    val fileName = fInput.files.item(0).name
                    labelName() = Some(fileName)
                    filePath() = Some(manager.current.now ++ fileName)
                    filePath.now.map {
                      fp ⇒
                        moveFilesAndBuildForm(fInput, fileName, fp)
                    }
                  }
                }), labelName.now match {
                  case Some(s: String) ⇒ s
                  case _               ⇒ "Your Model"
                }
              )
          }
        ), {
          fileToUploadPath.now.map {
            _ ⇒ span(grey +++ floatRight)(codeSelector.selector)
          }.getOrElse(tags.div())
        }
      ), {
        span(grey)(
          if (modelSelector.isContentsEmpty) div() else modelSelector.selector,
          if (classSelector.isContentsEmpty) div() else classSelector.selectorWithFilter,
          if (methodSelector.isContentsEmpty) div() else methodSelector.selectorWithFilter,
          codeSelector.content.now.value match {
            case Some(se: SelectElement[_]) ⇒
              se.value match {
                case NetLogoLanguage() ⇒ div("If your Netlogo sript depends on plugins, you should upload an archive (tar.gz, tgz) containing the root workspace.")
                case _                 ⇒ div
              }
            case _ ⇒ div()
          }
        )
      }
    ).render

  def moveFilesAndBuildForm(fInput: HTMLInputElement, fileName: String, uploadPath: SafePath) =
    CoreUtils.withTmpFile {
      tempFile ⇒
        FileManager.upload(
          fInput,
          tempFile,
          (p: ProcessState) ⇒ {
            transferring() = p
          },
          UploadAbsolute(),
          () ⇒ {
            OMPost[Api].extractAndTestExistence(tempFile ++ fileName, uploadPath.parent).call().foreach {
              existing ⇒
                val fileType: FileType = uploadPath

                targetPath() = Some(fileType match {
                  case a: Archive ⇒
                    a.language match {
                      case j: JavaLikeLanguage ⇒ uploadPath
                      case _                   ⇒ uploadPath.toNoExtention
                    }
                  case codeFile: CodeFile ⇒ uploadPath
                  case _                  ⇒ uploadPath
                })

                // Move files from tmp to target path
                if (existing.isEmpty) {
                  targetPath.now.map {
                    tp ⇒
                      OMPost[Api].copyAllTmpTo(tempFile, tp).call().foreach {
                        b ⇒
                          buildForm(uploadPath, fileType)
                          OMPost[Api].deleteFile(tempFile, ServerFileSytemContext.absolute).call()
                      }
                  }
                }
                else {
                  val optionsDiv = OptionsDiv(existing, SafePath.naming)
                  AlertPanel.div(
                    tags.div(
                      "Some files already exist, overwrite ?",
                      optionsDiv.div
                    ),
                    () ⇒ {
                      OMPost[Api].copyFromTmp(tempFile, optionsDiv.result /*, fp ++ fileName*/ ).call().foreach {
                        b ⇒
                          buildForm(uploadPath, fileType)
                          OMPost[Api].deleteFile(tempFile, ServerFileSytemContext.absolute).call()
                      }
                    }, () ⇒ {
                    }, buttonGroupClass = "right"
                  )
                }
            }
          }
        )
    }

  def buildForm(uploadPath: SafePath, fileType: FileType) = {

    fileType match {
      case archive: Archive ⇒
        fromArchive() = true
        archive.language match {
          //Java case
          case JavaLikeLanguage() ⇒
            modelSelector.emptyContents
            fileToUploadPath() = Some(uploadPath)
            fileToUploadPath.now.foreach {
              getJarClasses
            }

          // Other archive: tgz, tar.gz
          case UndefinedLanguage ⇒
            OMPost[Api].models(uploadPath).call().foreach {
              models ⇒
                fileToUploadPath() = models.headOption
                modelSelector.setContents(models, () ⇒ {
                  TreeNodePanel.refreshAnd(() ⇒ onModelChange)
                })
                getResourceInfo
            }

          case _ ⇒
            fromArchive() = false

        }
      case codeFile: CodeFile ⇒
        modelSelector.emptyContents
        resources() = resources.now.withNoImplicit
        setLaunchingCommand(uploadPath)
      case _ ⇒
    }
  }

  def getResourceInfo = {
    fileToUploadPath.now.foreach {
      mp ⇒
        val modelName = mp.name
        val resourceDir = mp.parent
        OMPost[Api].listFiles(resourceDir).call().foreach {
          b ⇒
            val l = b.list.filterNot {
              _.name == modelName
            }.map { tn ⇒
              val sp = resourceDir ++ tn.name
              Resource(sp, 0L)
            }
            resources() = resources.now.copy(implicits = l, number = l.size)
            OMPost[Api].expandResources(resources.now).call().foreach {
              lf ⇒
                resources() = lf
            }
        }
    }
  }

  def getJarClasses(jarPath: SafePath) = {
    codeSelector.content() = Some(JavaLikeLanguage())
    OMPost[Api].classes(jarPath).call().foreach {
      b ⇒
        val classContents = b.flatMap {
          _.flatten
        }
        classSelector.setContents(classContents)
        classContents.headOption.foreach {
          setMethodSelector(_)
        }
    }
  }

  def setLaunchingCommand(filePath: SafePath) = {
    emptyJARSelectors
    OMPost[Api].launchingCommands(filePath).call().foreach {
      b ⇒
        TreeNodePanel.refreshAndDraw
        launchingCommand() = b.headOption
        fileToUploadPath() = Some(filePath)
        launchingCommand.now.foreach {
          lc ⇒
            codeSelector.content() = lc.value.language.map {
              SelectElement(_)
            }
            setScritpName
            setReactives(lc)
        }
    }
  }

  def emptyJARSelectors = {
    classSelector.emptyContents
    methodSelector.emptyContents
  }

  def setReactives(lc: LaunchingCommand) = {
    val nbArgs = lc.arguments.size
    val iReactives = lc.arguments.zipWithIndex.collect {
      case (ve: VariableElement, id: Int) ⇒ buildReactive(CommandInput(ve), id)
    }
    val oReactives = lc.outputs.zipWithIndex collect {
      case (ve: VariableElement, id: Int) ⇒ buildReactive(CommandOutput(ve), id + nbArgs)
    }
    currentReactives() = iReactives ++ oReactives
  }

  val step1 = tags.div(
    tags.h4("Step 1: Code import"),
    div(grey +++ rightBlock)(
      "Pick your code up among jar archive, netlogo scripts, or any code packaged on linux with Care ( like Python, C, C++ " +
        "R, etc). In the case of a Care archive, the packaging has to be done with the",
      tags.b(" -o yourmodel.tar.gz.bin."),
      " option."
    )
  )

  val step2 = div(
    div(grey)("The systems detects automatically the launching command and propose you the creation of some OpenMOLE Variables so that" +
      " your model will be able to be feeded with variable values coming from the workflow you will build afterwards. In the case of Java, Scala, Netlogo" +
      "(ie codes working on the JVM) the OpenMOLE variables can be set directly in the command line. Otherwise, they have to be set inside ${} statements." +
      " By default he systems detects automatically your Variable changes and update the launching command. However, this option can be desactivated.")
  )

  val autoModeTag = div(onecolumn +++ sheet.paddingTop(20))(
    tags.b("Launching Command"),
    div(sheet.paddingTop(4) +++ floatRight)(
      "Automatic ",
      autoModeCheckBox,
      span(grey)(" It is automatically updated (default), or it can be set manually")
    )
  )

  val buildModelTaskButton = {

    tags.button("Build", btn_primary, onclick := {
      () ⇒
        save
        close

        val codeType = codeSelector.content.now.map {
          _.value
        }.getOrElse(Binary())

        val targetSuffix = codeType match {
          case NetLogoLanguage() ⇒
            if (fromArchive.now) s"/${fileToUploadPath.now.map { _.name }.getOrElse("NetLogoMODEL")}"
            else ""
          case _ ⇒ ""
        }

        launchingCommand.now.foreach {
          lc ⇒
            val path = manager.current.now
            val scriptName = scriptNameInput.value.clean
            val target = targetPath.now.map {
              _.name
            }.getOrElse("executable")
            OMPost[Api].buildModelTask(
              target + targetSuffix,
              scriptName,
              commandArea.value,
              codeType,
              inputs(currentReactives.now).map {
                _.content.prototype
              },
              outputs(currentReactives.now).map {
                _.content.prototype
              },
              path, classSelector.content.now.map {
                _.value.name
              }, fileToUploadPath.now.map {
                _.name
              }, resources.now
            ).call().foreach {
                b ⇒
                  panels.treeNodePanel.fileDisplayer.tabs -- b
                  panels.treeNodePanel.displayNode(FileNode(Var(b.name), 0L, 0L))
                  TreeNodePanel.refreshAndDraw
              }
        }
    })
  }

  def save = {
    currentReactives.now.map {
      _.save
    }
  }

  def buildReactive(role: VariableRole[VariableElement], index: Int): Reactive = Reactive(role, index)

  def buildReactive(role: VariableRole[VariableElement]): Reactive =
    currentReactives.now.filter {
      _.role == role
    }.headOption.getOrElse(buildReactive(role, role.content.index))

  def addVariableElement(p: VariableRole[VariableElement]) = {
    save
    currentReactives() = currentReactives.now :+ buildReactive(p, -1)
  }

  def applyOnPrototypePair(p: VariableRole[VariableElement], todo: (VariableRole[VariableElement], Int) ⇒ Unit) =
    currentReactives.now.map {
      _.role
    }.zipWithIndex.filter {
      case (ptp, index) ⇒ ptp == p
    }.foreach {
      case (role, index) ⇒ todo(role, index)
    }

  def updatePrototypePair(p: VariableRole[VariableElement], variableElement: VariableElement) =
    applyOnPrototypePair(p, (role: VariableRole[VariableElement], index: Int) ⇒ currentReactives() = currentReactives.now.updated(index, buildReactive(role.clone(variableElement), index)))

  def switchPrototypePair(p: VariableRole[VariableElement]) = {
    save
    applyOnPrototypePair(p, (role: VariableRole[VariableElement], index: Int) ⇒ currentReactives() = currentReactives.now.updated(index, buildReactive(role.switch, index)))
  }

  def addSwitchedPrototypePair(p: VariableRole[VariableElement]) = {
    save
    currentReactives() = (currentReactives.now :+ buildReactive(p.switch, -1)) distinct
  }

  case class Reactive(role: VariableRole[VariableElement], index: Int) {
    val lineHovered: Var[Boolean] = Var(false)

    val switchGlyph = role match {
      case i: Input[_]         ⇒ glyph_arrow_right
      case ci: CommandInput[_] ⇒ glyph_arrow_right
      case _                   ⇒ glyph_arrow_left
    }

    def updateLaunchingCommand =
      role match {
        case CommandInput(_) | CommandOutput(_) ⇒
          launchingCommand() = launchingCommand.now.map { lc ⇒
            lc.updateVariables(currentReactives.now.map {
              _.role
            }.collect {
              case x: CommandInput[_]  ⇒ x
              case y: CommandOutput[_] ⇒ y
            }.map {
              _.content
            })
          }
        case _ ⇒
      }

    def save = getReactive(index).map { reactive ⇒ updatePrototypePair(reactive.role, reactive.role.content.clone(nameInput.value, role.content.prototype.`type`, mappingInput.value)) }

    def removePrototypePair = {
      currentReactives() = currentReactives.now.filterNot(_.role == role)
      ModelWizardPanel.this.save
    }

    def saveWithoutTableUpdate = {
      updatableTable() = false
      save
      updatableTable() = true
    }

    val mappingInput: HTMLInputElement = bs.input(role.content.prototype.mapping.getOrElse(""))(oninput := { () ⇒
      saveWithoutTableUpdate
      updateLaunchingCommand
    }).render

    val nameInput: HTMLInputElement = bs.input(role.content.prototype.name)(oninput := { () ⇒
      saveWithoutTableUpdate
      updateLaunchingCommand
    }).render

    val line = {
      val glyphModifier = grey +++ sheet.paddingTop(2) +++ pointer +++ (opacity := 0.5)
      tags.tr(
        td(colMD(3) +++ sheet.paddingTop(7))(nameInput),
        td(colMD(2))(label(role.content.prototype.`type`.name.split('.').last)(label_primary)),
        td(colMD(1) +++ grey)(role.content.prototype.default),
        td(colMD(3))(if (role.content.prototype.mapping.isDefined) mappingInput else tags.div()),
        td(colMD(2) +++ floatRight)(
          tags.span(onclick := { () ⇒ switchPrototypePair(role) })(glyphModifier +++ switchGlyph),
          tags.span(onclick := { () ⇒ addSwitchedPrototypePair(role) })(glyphModifier +++ glyph_arrow_right_and_left),
          tags.span(pointer, onclick := { () ⇒ removePrototypePair })(glyphModifier +++ glyph_trash)
        )
      )
    }
  }

  def setBodyContent: Unit = bodyContent() = Some({
    val reactives = currentReactives.now
    val topButtons = Rx {
      div(sheet.paddingTop(20))(
        badge("I/O", s"$nbInputs/$nbOutputs",
          buttonStyle(0), () ⇒ {
            currentTab() = 0
            setBodyContent
          }),
        badge("Resources", s"${
          resources().number
        }", buttonStyle(1), () ⇒ {
          currentTab() = 1
          setBodyContent
        })
      )
    }

    setUpButton

    tags.div(
      fileToUploadPath.now.map {
        _ ⇒ tags.div()
      }.getOrElse(step1),
      transferring.now match {
        case _: Processing ⇒ OMTags.waitingSpan(" Uploading ...", btn_danger + "certificate")
        case _: Processed  ⇒ upButton.now
        case _             ⇒ upButton.now
      },
      fileToUploadPath.now.map {
        _ ⇒
          div(sheet.paddingTop(20))(
            tags.h4("Step2: Task configuration"), step2,
            topButtons,
            if (currentTab.now == 0) {
              tags.div({

                val idiv = div(modelIO)(tags.h3("Inputs")).render

                val odiv = div(modelIO)(tags.h3("Outputs")).render

                val head = thead(tags.tr(
                  for (h ← Seq("Name", "Type", "Default", "Mapped with", "", "")) yield {
                    th(textAlign := "center", h)
                  }
                ))

                div(sheet.paddingTop(30))(
                  div(sheet.paddingRight(10) +++ twocolumns)(
                    idiv,
                    tags.table(striped)(
                      head,
                      tbody(
                        for (ip ← inputs(reactives)) yield {
                          ip.line
                        }
                      )
                    )
                  ),
                  div(twocolumns)(
                    odiv,
                    tags.table(striped)(
                      head,
                      tbody(
                        for (op ← outputs(reactives)) yield {
                          op.line
                        }
                      )
                    )
                  )
                )
              }, autoModeTag, commandArea)
            }
            else {
              val body = tbody.render
              for {
                i ← resources.now.implicits
              } yield {
                body.appendChild(tags.tr(
                  td(colMD(3))(i.safePath.name),
                  td(colMD(2))(CoreUtils.readableByteCount(i.size))
                ).render)
              }
              tags.table(striped +++ sheet.paddingTop(20))(body)
            }
          )
      }.getOrElse(tags.div())
    )
  })

  lazy val dialog = {
    setBodyContent
    bs.modalDialog(
      modalID,
      bs.headerDialog(
        tags.span(tags.b("Model import"))
      ),
      bs.bodyDialog(Rx {
        bodyContent().getOrElse(tags.div())
      }),
      bs.footerDialog(buttonGroup(Seq(width := 200, right := 100))(
        bs.inputGroupButton(closeButton()),
        bs.inputGroupButton(scriptNameInput),
        bs.inputGroupButton(buildModelTaskButton)
      ))
    )
  }

}