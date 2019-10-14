package controllers

import application.WordResolveService
import monix.eval.Task
import monix.execution.Scheduler
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}

class WordController(
    resolveService: WordResolveService[Task],
    cc: ControllerComponents
) extends AbstractController(cc) {

  implicit val scheduler: Scheduler = Scheduler(controllerComponents.executionContext)

  def getAll: Action[AnyContent] = Action.async {
    resolveService.resolveAll.runToFuture.map { words =>
      Ok(words.toString())
    }
  }
}
