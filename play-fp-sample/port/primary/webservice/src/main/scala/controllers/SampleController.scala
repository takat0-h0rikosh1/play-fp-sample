package controllers

import javax.inject.{Inject, Singleton}
import play.api.mvc.{AbstractController, ControllerComponents}

@Singleton
class SampleController @Inject() (cc: ControllerComponents) extends AbstractController(cc){

  def index = Action {
    Ok("index")
  }
}