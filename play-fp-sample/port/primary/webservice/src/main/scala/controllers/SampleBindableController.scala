package controllers

import bindable.AgeRange
import javax.inject.{Inject, Singleton}
import play.api.mvc.{
  AbstractController,
  Action,
  AnyContent,
  ControllerComponents
}

@Singleton
class SampleBindableController @Inject()(cc: ControllerComponents)
    extends AbstractController(cc) {

  def get(ageRange: AgeRange, hoge: String): Action[AnyContent] = Action {
    Ok(ageRange.toString)
  }
}
