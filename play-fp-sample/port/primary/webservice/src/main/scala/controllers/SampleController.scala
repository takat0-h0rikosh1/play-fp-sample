package controllers

import javax.inject.{Inject, Singleton}
import play.api.data.{Form, FormError, Forms}
import play.api.data.Forms._
import play.api.data.format.{Formats, Formatter}
import play.api.mvc.{AbstractController, ControllerComponents}

@Singleton
class SampleController @Inject()(cc: ControllerComponents) extends AbstractController(cc) {

  def index = Action {
    Ok("index")
  }

  import UserFormBinder._

  def post() = Action { implicit request =>
    userForm.bindFromRequest()
      .fold(e => BadRequest(e.errors.toString), user => Ok(user.toString))
  }
}

object UserFormBinder {

  import NameFormatter._

  val userForm = Form(
    mapping(
      "name" -> Forms.of[Name],
      "textValue" -> set(Forms.of[TextValue]).verifying(_.nonEmpty)
    )(User.apply)(User.unapply)
  )
}

object NameFormatter {

  implicit def userFormat: Formatter[Name] = new Formatter[Name] {
    override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], Name] = {
      Formats.stringFormat.bind(key, data).right.map(Name)
      //      data.get(key).map(x => Right(Name(x))).getOrElse(Left(Seq(FormError(key, "error: require.name"))))
    }

    override def unbind(key: String, value: Name): Map[String, String] =
      Map(key -> value.value)

  }

  implicit def textValue: Formatter[TextValue] = new Formatter[TextValue] {
    override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], TextValue] = {
      Formats.stringFormat.bind(key, data).right.map(TextValue)
      //      data.get(key).map(x => Right(TextValue(x))).getOrElse(Left(Seq(FormError(key, "error: require.textValue"))))
    }

    override def unbind(key: String, value: TextValue): Map[String, String] =
      Map(key -> value.value)
  }
}

case class User(name: Name, textValues: Set[TextValue])

case class Name(value: String)

case class TextValue(value: String)