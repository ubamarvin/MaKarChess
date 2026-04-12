package makarchess.view

import makarchess.controller.ChessController

import javafx.application.Platform as JfxPlatform
import scalafx.scene.Scene
import scalafx.stage.Stage

import java.util.logging.Level
import java.util.logging.Logger

object GuiLauncher:

  @volatile private var started: Boolean = false

  def start(controller: ChessController): Unit =
    Logger.getLogger("com.sun.javafx.application.PlatformImpl").setLevel(Level.SEVERE)

    if !started then
      started = true
      val t = new Thread(() =>
        JfxPlatform.startup(() =>
          val gui = GuiView(controller)
          val stage = new Stage()
          stage.setTitle("MaKarChess")
          stage.setScene(new Scene(gui.root, 630, 780))
          stage.show()
          controller.model.notifyObservers
        )
      )
      t.setDaemon(false)
      t.start()
    else
      JfxPlatform.runLater(() =>
        val gui = GuiView(controller)
        val stage = new Stage()
        stage.setTitle("MaKarChess")
        stage.setScene(new Scene(gui.root, 630, 780))
        stage.show()
        controller.model.notifyObservers
      )
