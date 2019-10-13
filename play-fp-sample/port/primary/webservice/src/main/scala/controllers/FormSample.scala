package controllers

import com.google.inject.Inject
import javax.inject.Singleton
import play.api.data.Forms._
import play.api.data.format.{Formats, Formatter}
import play.api.data.{Form, FormError}
import play.api.mvc.{
  AbstractController,
  Action,
  AnyContent,
  ControllerComponents
}

@Singleton
class FormSample @Inject()(cc: ControllerComponents)
    extends AbstractController(cc) {

  import SampleMapper._

  def post(): Action[AnyContent] = Action { implicit request =>
    sampleForm
      .bindFromRequest()
      .fold(e => BadRequest(e.errors.toString), whoami => Ok(whoami.toString))
  }

}

object SampleMapper extends CustomFormatter {

  val sampleForm = Form(
    mapping(
      "name" -> text.transform[Iam](Iam, _.value),
      "emails" -> seq(text.transform[Email](Email, _.value))
        .transform[Emails](Emails, _.values)
    )(Whoami.apply)(Whoami.unapply)
  )
}

trait CustomFormatter {

  implicit def userFormat: Formatter[Iam] = new Formatter[Iam] {

    override def bind(
      key: String,
      data: Map[String, String]
    ): Either[Seq[FormError], Iam] = {
      Formats.stringFormat.bind(key, data).right.map(Iam)
    }

    override def unbind(key: String, value: Iam): Map[String, String] =
      Map(key -> value.value)
  }
}

case class Whoami(iam: Iam, emails: Emails)

case class Iam(value: String)

case class Email(value: String)

case class Emails(values: Seq[Email])
