package controllers

import javax.inject.{Inject, Singleton}
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}

@Singleton
class CustomRouteController @Inject()(cc: ControllerComponents) extends AbstractController(cc) {

  def get(ageRange: AgeRange, hoge: String): Action[AnyContent] = Action {
    Ok(ageRange.toString)
  }
}
