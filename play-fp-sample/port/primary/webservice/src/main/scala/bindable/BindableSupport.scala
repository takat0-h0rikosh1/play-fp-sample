package bindable

import cats.data.EitherT
import cats.implicits._
import play.api.mvc.QueryStringBindable

trait BindableSupport {

  private type Bindable[A] = QueryStringBindable[A]
  private type Params = Map[String, Seq[String]]
  private type Bound[A] = Option[Either[String, A]]

  // bindable#bind(...): Option[Either[String, Option[A]]] の結果 が None の場合は Right(None) にする
  // これやりたいユースケースが意外と多い...
  def bindToRightOption[A](
      key: String
  )(implicit params: Params, bindable: Bindable[A]): Either[String, Option[A]] =
    EitherT(bindable.bind(key, params))
      .map(Option(_))
      .value
      .getOrElse(Right(None))
}
